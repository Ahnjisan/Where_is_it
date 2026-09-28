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
import com.whereisit.backend.founditem.client.FoundItemListEntry;
import com.whereisit.backend.founditem.entity.FoundItem;
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
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractionResult;
import com.whereisit.backend.search.dto.SearchExecutionResponse;
import com.whereisit.backend.search.error.SearchErrorCode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class SearchExecutionPersistenceService {

	private final LostItemService lostItemService;
	private final ChatMessageRepository chatMessageRepository;
	private final FoundItemCollectionService foundItemCollectionService;
	private final LostItemCandidateRepository candidateRepository;
	private final CandidateRanker candidateRanker;
	private final Clock clock;

	@Transactional(readOnly = true)
	public SearchSnapshot prepareInitial(Long memberId, Long lostItemId) {
		return snapshot(findOwnedActive(memberId, lostItemId));
	}

	@Transactional
	public SearchSnapshot prepareText(Long memberId, Long lostItemId, String message) {
		if (message == null || message.isBlank()) {
			throw new BusinessException(CommonErrorCode.VALIDATION_ERROR);
		}
		LostItem lostItem = findOwnedActive(memberId, lostItemId);
		chatMessageRepository.save(ChatMessage.of(lostItem, ChatRole.USER, message));
		return snapshot(lostItem);
	}

	@Transactional
	public SearchSnapshot prepareFilter(Long memberId, Long lostItemId, SearchConditionsPatch conditions) {
		if (conditions == null) {
			throw new BusinessException(CommonErrorCode.VALIDATION_ERROR);
		}
		LostItem lostItem = findOwnedActive(memberId, lostItemId);
		lostItemService.applyConditions(lostItem, conditions);
		return snapshot(lostItem);
	}

	@Transactional
	public AiAppliedResult applyAiResult(Long memberId, Long lostItemId,
			AiSearchConditionExtractionResult result) {
		LostItem lostItem = findOwnedActive(memberId, lostItemId);
		try {
			lostItem.updateAiSearchConditions(
					result.lostDateFrom(), result.lostDateTo(), result.lostPlaceText());
		}
		catch (IllegalArgumentException e) {
			throw new BusinessException(SearchErrorCode.AI_CONDITION_UNAVAILABLE);
		}
		ChatMessage assistant = chatMessageRepository.save(
				ChatMessage.of(lostItem, ChatRole.ASSISTANT, result.assistantMessage()));
		return new AiAppliedResult(snapshot(lostItem), ChatMessageResponse.from(assistant));
	}

	@Transactional
	public SearchExecutionResponse finalizeSearch(Long memberId, Long lostItemId,
			List<FoundItemListEntry> entries, boolean complete, ChatMessageResponse aiAssistantMessage) {
		LostItem lostItem = findOwnedActive(memberId, lostItemId);
		List<FoundItem> foundItems = foundItemCollectionService.persist(entries);
		List<RankedCandidate> ranked = candidateRanker.rank(lostItem.getDescription(), foundItems);

		List<CandidateResponse> candidateResponses;
		List<String> warnings = new ArrayList<>();
		if (complete) {
			candidateResponses = persistAsCurrentCandidates(lostItem, ranked);
		}
		else {
			warnings.add("PARTIAL_SOURCE_RESULT");
			candidateResponses = ranked.stream().map(CandidateResponse::transientOf).toList();
		}

		ChatMessageResponse assistantMessage = aiAssistantMessage;
		if (assistantMessage == null) {
			ChatMessage fixed = chatMessageRepository.save(ChatMessage.of(lostItem, ChatRole.ASSISTANT,
					buildAssistantMessageContent(complete, candidateResponses.size())));
			assistantMessage = ChatMessageResponse.from(fixed);
		}

		return new SearchExecutionResponse(
				LostItemResponse.from(lostItem, clock, (int) candidateRepository.countByLostItemIdAndCurrentTrue(lostItem.getId())),
				assistantMessage,
				complete ? "COMPLETE" : "PARTIAL",
				warnings,
				candidateResponses);
	}

	private LostItem findOwnedActive(Long memberId, Long lostItemId) {
		LostItem lostItem = lostItemService.findOwned(memberId, lostItemId);
		lostItemService.ensureNotExpired(lostItem);
		return lostItem;
	}

	private SearchSnapshot snapshot(LostItem lostItem) {
		return new SearchSnapshot(
				lostItem.getDescription(), lostItem.getLanguageCode().getCode(),
				lostItem.getCategoryLargeCode(), lostItem.getCategoryMiddleCode(), lostItem.getColorCode(),
				lostItem.getRegionCode(), lostItem.getSearchStartDate(), lostItem.getLostDateFrom(),
				lostItem.getLostDateTo(), lostItem.getLostPlaceText());
	}

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

	public record SearchSnapshot(
			String description,
			String languageCode,
			String categoryLargeCode,
			String categoryMiddleCode,
			String colorCode,
			String regionCode,
			LocalDate searchStartDate,
			LocalDate lostDateFrom,
			LocalDate lostDateTo,
			String lostPlaceText) {
	}

	public record AiAppliedResult(SearchSnapshot snapshot, ChatMessageResponse assistantMessage) {
	}
}
