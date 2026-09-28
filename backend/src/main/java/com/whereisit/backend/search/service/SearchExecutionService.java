package com.whereisit.backend.search.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.whereisit.backend.candidate.dto.CandidateResponse;
import com.whereisit.backend.candidate.entity.LostItemCandidate;
import com.whereisit.backend.candidate.ranking.CandidateRanker;
import com.whereisit.backend.candidate.ranking.RankedCandidate;
import com.whereisit.backend.candidate.repository.LostItemCandidateRepository;
import com.whereisit.backend.founditem.client.FoundItemLookupException;
import com.whereisit.backend.founditem.client.FoundItemSearchQuery;
import com.whereisit.backend.founditem.entity.FoundItem;
import com.whereisit.backend.founditem.entity.FoundItemSourceType;
import com.whereisit.backend.founditem.service.FoundItemCollectionService;
import com.whereisit.backend.global.error.BusinessException;
import com.whereisit.backend.global.error.CommonErrorCode;
import com.whereisit.backend.lostitem.dto.ChatMessageResponse;
import com.whereisit.backend.lostitem.dto.LostItemResponse;
import com.whereisit.backend.lostitem.dto.SearchConditionsPatch;
import com.whereisit.backend.lostitem.entity.ChatMessage;
import com.whereisit.backend.lostitem.entity.ChatRole;
import com.whereisit.backend.lostitem.entity.LostItem;
import com.whereisit.backend.lostitem.repository.ChatMessageRepository;
import com.whereisit.backend.lostitem.service.LostItemService;
import com.whereisit.backend.search.dto.RunSearchRequest;
import com.whereisit.backend.search.dto.SearchExecutionResponse;
import com.whereisit.backend.search.error.SearchErrorCode;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * API-11 검색 실행. AI 대신 규칙 기반 CandidateRanker를 쓴다(decision-log AI 제공자 미결정).
 * 두 출처(POLICE·PORTAL)를 모두 조회해 성공한 경우에만 현재 후보 캐시를 갱신하고,
 * 한쪽만 성공하면 PARTIAL로 표시하며 일시적 후보만 반환한다(저장하지 않음).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchExecutionService {

	private final LostItemService lostItemService;
	private final ChatMessageRepository chatMessageRepository;
	private final FoundItemCollectionService foundItemCollectionService;
	private final LostItemCandidateRepository candidateRepository;
	private final CandidateRanker candidateRanker;
	private final Clock clock;

	@Transactional
	public SearchExecutionResponse execute(Long memberId, Long lostItemId, RunSearchRequest request) {
		LostItem lostItem = lostItemService.findOwned(memberId, lostItemId);
		lostItemService.ensureNotExpired(lostItem);
		applyMode(lostItem, request);

		FoundItemSearchQuery query = buildQuery(lostItem);
		SourceRun policeRun = runSource(FoundItemSourceType.POLICE, query);
		SourceRun portalRun = runSource(FoundItemSourceType.PORTAL, query);

		if (!policeRun.success() && !portalRun.success()) {
			throw new BusinessException(SearchErrorCode.LOST_API_UNAVAILABLE);
		}
		boolean complete = policeRun.success() && portalRun.success();

		List<FoundItem> foundItems = new ArrayList<>(policeRun.items());
		foundItems.addAll(portalRun.items());
		List<RankedCandidate> ranked = candidateRanker.rank(lostItem.getDescription(), foundItems);

		List<CandidateResponse> candidateResponses;
		List<String> warnings = new ArrayList<>();
		if (complete) {
			candidateResponses = persistAsCurrentCandidates(lostItem, ranked);
		} else {
			warnings.add("PARTIAL_SOURCE_RESULT");
			candidateResponses = ranked.stream().map(CandidateResponse::transientOf).toList();
		}

		ChatMessage assistantMessage = chatMessageRepository.save(
				ChatMessage.of(lostItem, ChatRole.ASSISTANT, buildAssistantMessageContent(complete, candidateResponses.size())));

		return new SearchExecutionResponse(
				LostItemResponse.from(lostItem, clock, (int) candidateRepository.countByLostItemIdAndCurrentTrue(lostItem.getId())),
				ChatMessageResponse.from(assistantMessage),
				complete ? "COMPLETE" : "PARTIAL",
				warnings,
				candidateResponses);
	}

	private void applyMode(LostItem lostItem, RunSearchRequest request) {
		switch (request.mode()) {
			case INITIAL -> {
				// 저장된 설명·조건 그대로 재검색한다.
			}
			case TEXT -> {
				if (request.message() == null || request.message().isBlank()) {
					throw new BusinessException(CommonErrorCode.VALIDATION_ERROR);
				}
				// AI가 없어 추가 설명에서 새 조건을 추출하지는 못하고, 대화 기록으로만 남긴다(PR 설명 참고).
				chatMessageRepository.save(ChatMessage.of(lostItem, ChatRole.USER, request.message()));
			}
			case FILTER -> {
				SearchConditionsPatch patch = request.conditions();
				if (patch == null) {
					throw new BusinessException(CommonErrorCode.VALIDATION_ERROR);
				}
				lostItemService.applyConditions(lostItem, patch);
			}
		}
	}

	private FoundItemSearchQuery buildQuery(LostItem lostItem) {
		LocalDate endDate = LocalDate.now(clock);
		return new FoundItemSearchQuery(lostItem.getCategoryLargeCode(), lostItem.getCategoryMiddleCode(),
				lostItem.getColorCode(), lostItem.getRegionCode(), lostItem.getSearchStartDate(), endDate);
	}

	private SourceRun runSource(FoundItemSourceType sourceType, FoundItemSearchQuery query) {
		try {
			return new SourceRun(sourceType, true, foundItemCollectionService.collect(sourceType, query));
		} catch (FoundItemLookupException e) {
			log.warn("{} 조회 실패, PARTIAL로 처리합니다", sourceType, e);
			return new SourceRun(sourceType, false, List.of());
		}
	}

	/**
	 * 완전 조회 성공일 때만 부른다. 새 결과에 있는 습득물은 현재_결과_여부=1로 올리고,
	 * 이번에 빠진 기존 후보는 이력은 남긴 채 현재_결과_여부만 0으로 내린다.
	 */
	private List<CandidateResponse> persistAsCurrentCandidates(LostItem lostItem, List<RankedCandidate> ranked) {
		LocalDateTime now = LocalDateTime.now(clock);
		Set<Long> keepFoundItemIds = new HashSet<>();
		List<LostItemCandidate> updated = new ArrayList<>();

		for (RankedCandidate r : ranked) {
			LostItemCandidate candidate = candidateRepository
					.findByLostItemIdAndFoundItemId(lostItem.getId(), r.foundItem().getId())
					.orElseGet(() -> candidateRepository.save(LostItemCandidate.create(lostItem, r.foundItem(), now)));
			candidate.markSeenInCurrentResult(r.rank(), r.reason(), r.similar(), now);
			keepFoundItemIds.add(r.foundItem().getId());
			updated.add(candidate);
		}

		for (LostItemCandidate previous : candidateRepository.findByLostItemIdAndCurrentTrue(lostItem.getId())) {
			if (!keepFoundItemIds.contains(previous.getFoundItem().getId())) {
				previous.markNotInCurrentResult();
			}
		}

		return updated.stream().map(CandidateResponse::from).toList();
	}

	private String buildAssistantMessageContent(boolean complete, int candidateCount) {
		return complete
				? "습득물 %d건을 확인했습니다.".formatted(candidateCount)
				: "일부 출처 조회에 실패해 임시 후보 %d건만 보여드립니다.".formatted(candidateCount);
	}

	private record SourceRun(FoundItemSourceType sourceType, boolean success, List<FoundItem> items) {
	}
}
