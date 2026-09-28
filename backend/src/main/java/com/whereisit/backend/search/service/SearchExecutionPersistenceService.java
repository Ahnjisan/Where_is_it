package com.whereisit.backend.search.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.whereisit.backend.candidate.dto.CandidateResponse;
import com.whereisit.backend.candidate.entity.LostItemCandidate;
import com.whereisit.backend.candidate.ranking.CandidateRankingResult;
import com.whereisit.backend.candidate.ranking.CandidateRankingResult.RankingStatus;
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
import com.whereisit.backend.lostitem.entity.LostItemStatus;
import com.whereisit.backend.lostitem.repository.ChatMessageRepository;
import com.whereisit.backend.lostitem.repository.LostItemRepository;
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
	private final LostItemRepository lostItemRepository;
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
	public AiAppliedResult applyAiResult(Long memberId, Long lostItemId, SearchSnapshot expectedSnapshot,
			AiSearchConditionExtractionResult result) {
		LostItem lostItem = findOwnedActiveForUpdate(memberId, lostItemId);
		if (!expectedSnapshot.equals(snapshot(lostItem))) {
			throw new BusinessException(SearchErrorCode.ITEM_BUSY);
		}
		try {
			lostItem.updateAiSearchConditions(result.lostDateFrom(), result.lostDateTo(), result.lostPlaceText());
			lostItem.updateSearchKeywords(result.productNameKeyword(), result.storagePlaceKeyword());
		}
		catch (IllegalArgumentException e) {
			throw new BusinessException(SearchErrorCode.AI_CONDITION_UNAVAILABLE);
		}
		ChatMessage assistant = chatMessageRepository.save(
				ChatMessage.of(lostItem, ChatRole.ASSISTANT, result.assistantMessage()));
		return new AiAppliedResult(snapshot(lostItem), ChatMessageResponse.from(assistant));
	}

	@Transactional
	public SearchExecutionResponse finalizeSearch(Long memberId, Long lostItemId, SearchSnapshot expectedSnapshot,
			Map<String, FoundItemListEntry> entriesByKey, CandidateRankingResult rankingResult,
			boolean complete, ChatMessageResponse aiAssistantMessage) {
		LostItem lostItem = findOwnedActiveForUpdate(memberId, lostItemId);
		if (!expectedSnapshot.equals(snapshot(lostItem))) {
			throw new BusinessException(SearchErrorCode.ITEM_BUSY);
		}
		validateRanking(entriesByKey.keySet(), rankingResult);

		List<FoundItem> foundItems = foundItemCollectionService.persist(new ArrayList<>(entriesByKey.values()));
		if (foundItems.size() != entriesByKey.size()) {
			throw new BusinessException(CommonErrorCode.INTERNAL_ERROR);
		}
		Map<String, FoundItem> foundItemsByKey = mapFoundItems(entriesByKey.keySet(), foundItems);

		boolean persisted = complete && (rankingResult.rankingStatus() == RankingStatus.SUCCESS
				|| rankingResult.rankingStatus() == RankingStatus.NOT_RUN);
		List<CandidateResponse> candidateResponses = persisted
				? persistAsCurrentCandidates(lostItem, rankingResult.candidates(), foundItemsByKey)
				: transientCandidates(rankingResult.candidates(), foundItemsByKey);

		List<String> warnings = new ArrayList<>();
		if (!complete) {
			warnings.add("PARTIAL_SOURCE_RESULT");
		}
		warnings.addAll(rankingResult.warnings());
		warnings = List.copyOf(new LinkedHashSet<>(warnings));

		ChatMessageResponse assistantMessage = aiAssistantMessage;
		if (assistantMessage == null) {
			ChatMessage fixed = chatMessageRepository.save(ChatMessage.of(lostItem, ChatRole.ASSISTANT,
					buildAssistantMessageContent(complete, candidateResponses.size())));
			assistantMessage = ChatMessageResponse.from(fixed);
		}

		return new SearchExecutionResponse(
				LostItemResponse.from(
						lostItem,
						clock,
						(int) candidateRepository.countByLostItemIdAndCurrentTrue(lostItem.getId())),
				assistantMessage,
				complete ? "COMPLETE" : "PARTIAL",
				rankingResult.rankingStatus().name(),
				persisted,
				warnings,
				candidateResponses,
				List.of());
	}

	private void validateRanking(Set<String> expectedKeys, CandidateRankingResult result) {
		if (result.rankingStatus() == RankingStatus.NOT_RUN) {
			if (!expectedKeys.isEmpty() || !result.candidates().isEmpty() || result.openAiResultUsed()) {
				throw new BusinessException(CommonErrorCode.INTERNAL_ERROR);
			}
			return;
		}
		if (result.rankingStatus() == RankingStatus.SUCCESS && !result.openAiResultUsed()) {
			throw new BusinessException(CommonErrorCode.INTERNAL_ERROR);
		}
		if (result.rankingStatus() == RankingStatus.UNAVAILABLE && result.openAiResultUsed()) {
			throw new BusinessException(CommonErrorCode.INTERNAL_ERROR);
		}
		Set<String> actualKeys = new HashSet<>();
		Set<Integer> ranks = new HashSet<>();
		for (RankedCandidate ranked : result.candidates()) {
			if (!expectedKeys.contains(ranked.candidateKey()) || !actualKeys.add(ranked.candidateKey())
					|| !ranks.add(ranked.rank()) || ranked.rank() < 1 || ranked.rank() > expectedKeys.size()
					|| ranked.reason() == null || ranked.reason().isBlank()
					|| ranked.reason().codePointCount(0, ranked.reason().length()) > 500) {
				throw new BusinessException(CommonErrorCode.INTERNAL_ERROR);
			}
		}
		if (!actualKeys.equals(expectedKeys) || ranks.size() != expectedKeys.size()) {
			throw new BusinessException(CommonErrorCode.INTERNAL_ERROR);
		}
	}

	private Map<String, FoundItem> mapFoundItems(Set<String> keys, List<FoundItem> foundItems) {
		Map<String, FoundItem> result = new LinkedHashMap<>();
		int index = 0;
		for (String key : keys) {
			result.put(key, foundItems.get(index++));
		}
		return result;
	}

	private List<CandidateResponse> transientCandidates(List<RankedCandidate> ranked,
			Map<String, FoundItem> foundItemsByKey) {
		return ranked.stream()
				.map(item -> CandidateResponse.transientOf(foundItemsByKey.get(item.candidateKey()), item))
				.toList();
	}

	private LostItem findOwnedActive(Long memberId, Long lostItemId) {
		LostItem lostItem = lostItemService.findOwned(memberId, lostItemId);
		lostItemService.ensureNotExpired(lostItem);
		return lostItem;
	}

	/**
	 * API-05 response finalization. FoundItem rows are an external-source cache only; no LostItemCandidate is created
	 * or updated, and persisted is therefore always false.
	 */
	@Transactional
	public SearchExecutionResponse finalizeInitialSearch(Long memberId, Long lostItemId,
			SearchSnapshot expectedSnapshot, Map<String, FoundItemListEntry> entriesByKey,
			CandidateRankingResult rankingResult, ChatMessageResponse assistantMessage) {
		LostItem lostItem = findOwnedActiveForUpdate(memberId, lostItemId);
		if (!expectedSnapshot.equals(snapshot(lostItem))) {
			throw new BusinessException(SearchErrorCode.ITEM_BUSY);
		}
		validateRanking(entriesByKey.keySet(), rankingResult);

		List<FoundItem> foundItems = foundItemCollectionService.persist(new ArrayList<>(entriesByKey.values()));
		if (foundItems.size() != entriesByKey.size()) {
			throw new BusinessException(CommonErrorCode.INTERNAL_ERROR);
		}
		Map<String, FoundItem> foundItemsByKey = mapFoundItems(entriesByKey.keySet(), foundItems);
		List<CandidateResponse> candidates = transientCandidates(rankingResult.candidates(), foundItemsByKey);

		return new SearchExecutionResponse(
				LostItemResponse.from(
						lostItem,
						clock,
						(int) candidateRepository.countByLostItemIdAndCurrentTrue(lostItem.getId())),
				assistantMessage,
				"COMPLETE",
				rankingResult.rankingStatus().name(),
				false,
				rankingResult.warnings(),
				candidates,
				List.of());
	}

	private LostItem findOwnedActiveForUpdate(Long memberId, Long lostItemId) {
		LostItem lostItem = lostItemRepository.findOwnedActiveByIdForUpdate(lostItemId, memberId)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
		lostItemService.ensureNotExpired(lostItem);
		return lostItem;
	}

	private SearchSnapshot snapshot(LostItem lostItem) {
		return new SearchSnapshot(
				lostItem.getId(), lostItem.getMember().getId(), lostItem.getDescription(), lostItem.getLanguageCode().getCode(),
				lostItem.getCategoryLargeCode(), lostItem.getCategoryMiddleCode(), lostItem.getColorCode(),
				lostItem.getRegionCode(), lostItem.getSearchStartDate(), lostItem.getLostDateFrom(),
				lostItem.getLostDateTo(), lostItem.getLostPlaceText(), lostItem.getStatus(), lostItem.getDeletedAt());
	}

	private List<CandidateResponse> persistAsCurrentCandidates(LostItem lostItem, List<RankedCandidate> ranked,
			Map<String, FoundItem> foundItemsByKey) {
		LocalDateTime now = LocalDateTime.now(clock);
		Set<Long> keepFoundItemIds = new HashSet<>();
		List<LostItemCandidate> updated = new ArrayList<>();

		for (RankedCandidate item : ranked) {
			FoundItem foundItem = foundItemsByKey.get(item.candidateKey());
			LostItemCandidate candidate = candidateRepository
					.findByLostItemIdAndFoundItemId(lostItem.getId(), foundItem.getId())
					.orElseGet(() -> candidateRepository.save(LostItemCandidate.create(lostItem, foundItem, now)));
			candidate.markSeenInCurrentResult(item.rank(), item.reason(), item.similar(), now);
			keepFoundItemIds.add(foundItem.getId());
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
			Long lostItemId,
			Long memberId,
			String description,
			String languageCode,
			String categoryLargeCode,
			String categoryMiddleCode,
			String colorCode,
			String regionCode,
			LocalDate searchStartDate,
			LocalDate lostDateFrom,
			LocalDate lostDateTo,
			String lostPlaceText,
			LostItemStatus status,
			LocalDateTime deletedAt) {
	}

	public record AiAppliedResult(SearchSnapshot snapshot, ChatMessageResponse assistantMessage) {
	}
}
