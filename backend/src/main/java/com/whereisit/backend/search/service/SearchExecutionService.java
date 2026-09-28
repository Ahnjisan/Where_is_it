package com.whereisit.backend.search.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.whereisit.backend.candidate.ranking.CandidateRanker;
import com.whereisit.backend.candidate.ranking.CandidateRankingInput;
import com.whereisit.backend.candidate.ranking.CandidateRankingRequest;
import com.whereisit.backend.candidate.ranking.CandidateRankingResult;
import com.whereisit.backend.candidate.ranking.SimpleTextSimilarityRanker;
import com.whereisit.backend.candidate.ranking.initial.InitialSearchCandidateRanker;
import com.whereisit.backend.founditem.client.FoundItemListEntry;
import com.whereisit.backend.founditem.client.FoundItemLookupException;
import com.whereisit.backend.founditem.client.FoundItemSearchQuery;
import com.whereisit.backend.founditem.client.PortalFoundItemSearchQuery;
import com.whereisit.backend.founditem.entity.FoundItemSourceType;
import com.whereisit.backend.founditem.service.FoundItemCollectionService;
import com.whereisit.backend.global.error.BusinessException;
import com.whereisit.backend.global.error.CommonErrorCode;
import com.whereisit.backend.lostitem.dto.ChatMessageResponse;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractionRequest;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractionResult;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractor;
import com.whereisit.backend.search.dto.RunSearchRequest;
import com.whereisit.backend.search.dto.SearchExecutionResponse;
import com.whereisit.backend.search.dto.SearchMode;
import com.whereisit.backend.search.error.SearchErrorCode;
import com.whereisit.backend.search.service.SearchExecutionPersistenceService.AiAppliedResult;
import com.whereisit.backend.search.service.SearchExecutionPersistenceService.SearchSnapshot;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/** API-05 내부 검색 실행 서비스. 모든 외부 호출과 후보 랭킹은 활성 DB 트랜잭션 없이 실행한다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchExecutionService {

	private static final int MAX_CANDIDATES = 100;

	private final SearchExecutionPersistenceService persistenceService;
	private final AiSearchConditionExtractor aiSearchConditionExtractor;
	private final FoundItemCollectionService foundItemCollectionService;
	private final CandidateRanker candidateRanker;
	private final SimpleTextSimilarityRanker simpleRanker;
	private final CandidatePreselector candidatePreselector;
	private final InitialSearchCandidateRanker initialSearchCandidateRanker;
	private final Clock clock;

	/**
	 * API-05 initial search. Only portal operation 2 is queried and no LostItemCandidate relationship is stored.
	 * Issue #80: API-05 alone uses the deterministic top-20 preselection and the score-based ranker whose failures
	 * always fall back to rule-based results. Legacy {@link #execute} keeps its 100-candidate ranker path unchanged.
	 */
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public SearchExecutionResponse executeInitial(Long memberId, Long lostItemId) {
		SearchSnapshot snapshot = persistenceService.prepareInitial(memberId, lostItemId);
		AiSearchConditionExtractionResult extracted = aiSearchConditionExtractor.extract(
				new AiSearchConditionExtractionRequest(
						snapshot.description(), snapshot.description(), snapshot.languageCode(),
						LocalDate.now(clock), snapshot.lostDateFrom(), snapshot.lostDateTo(), snapshot.lostPlaceText()));
		AiAppliedResult applied = persistenceService.applyAiResult(memberId, lostItemId, snapshot, extracted);
		snapshot = applied.snapshot();

		List<FoundItemListEntry> portalEntries;
		try {
			portalEntries = foundItemCollectionService.lookupPortalByNameAndStorage(
					new PortalFoundItemSearchQuery(
							extracted.productNameKeyword(), extracted.storagePlaceKeyword()));
		}
		catch (FoundItemLookupException e) {
			throw new BusinessException(SearchErrorCode.LOST_API_UNAVAILABLE);
		}

		CandidateSelection selection = toInitialSelection(candidatePreselector.select(
				snapshot.description(), portalEntries, CandidatePreselector.AI_EVALUATION_LIMIT));
		CandidateRankingResult rankingResult = selection.entriesByKey().isEmpty()
				? CandidateRankingResult.notRun()
				: initialSearchCandidateRanker.rank(new CandidateRankingRequest(
						snapshot.description(), snapshot.languageCode(), snapshot.lostDateFrom(), snapshot.lostDateTo(),
						snapshot.lostPlaceText(), selection.rankingInputs()));
		if (selection.limited()) {
			rankingResult = rankingResult.withWarning("RESULT_LIMIT_REACHED");
		}

		return persistenceService.finalizeInitialSearch(
				memberId, lostItemId, snapshot, selection.entriesByKey(), rankingResult, applied.assistantMessage());
	}

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public SearchExecutionResponse execute(Long memberId, Long lostItemId, RunSearchRequest request) {
		PreparedMode prepared = prepare(memberId, lostItemId, request);
		SearchSnapshot snapshot = prepared.snapshot();
		ChatMessageResponse assistantMessage = null;

		if (request.mode() != SearchMode.FILTER) {
			AiSearchConditionExtractionResult result = aiSearchConditionExtractor.extract(
					new AiSearchConditionExtractionRequest(
							prepared.naturalLanguage(), snapshot.description(), snapshot.languageCode(),
							LocalDate.now(clock), snapshot.lostDateFrom(), snapshot.lostDateTo(), snapshot.lostPlaceText()));
			AiAppliedResult applied = persistenceService.applyAiResult(memberId, lostItemId, snapshot, result);
			snapshot = applied.snapshot();
			assistantMessage = applied.assistantMessage();
		}

		FoundItemSearchQuery query = buildQuery(snapshot);
		SourceRun policeRun = runSource(FoundItemSourceType.POLICE, query);
		SourceRun portalRun = runSource(FoundItemSourceType.PORTAL, query);
		if (!policeRun.success() && !portalRun.success()) {
			throw new BusinessException(SearchErrorCode.LOST_API_UNAVAILABLE);
		}

		boolean complete = policeRun.success() && portalRun.success();
		List<FoundItemListEntry> entries = new ArrayList<>(policeRun.entries());
		entries.addAll(portalRun.entries());
		CandidateSelection selection = selectCandidates(snapshot.description(), entries);

		CandidateRankingResult rankingResult;
		if (selection.entriesByKey().isEmpty()) {
			rankingResult = CandidateRankingResult.notRun();
		}
		else {
			CandidateRankingRequest rankingRequest = new CandidateRankingRequest(
					snapshot.description(), snapshot.languageCode(), snapshot.lostDateFrom(), snapshot.lostDateTo(),
					snapshot.lostPlaceText(), selection.rankingInputs());
			rankingResult = candidateRanker.rank(rankingRequest);
		}
		if (selection.limited()) {
			rankingResult = rankingResult.withWarning("RESULT_LIMIT_REACHED");
		}

		return persistenceService.finalizeSearch(
				memberId, lostItemId, snapshot, selection.entriesByKey(), rankingResult,
				complete, assistantMessage);
	}

	private CandidateSelection selectCandidates(String description, List<FoundItemListEntry> entries) {
		Map<NaturalKey, FoundItemListEntry> unique = new LinkedHashMap<>();
		for (FoundItemListEntry entry : entries) {
			unique.putIfAbsent(new NaturalKey(entry.sourceType(), entry.atcId(), entry.fdSn()), entry);
		}
		List<FoundItemListEntry> selected = new ArrayList<>(unique.values());
		boolean limited = selected.size() > MAX_CANDIDATES;
		if (limited) {
			selected.sort(Comparator
					.<FoundItemListEntry>comparingInt(entry -> simpleRanker.similarityScore(description, toRankingInput("c0", entry)))
					.reversed()
					.thenComparing(FoundItemListEntry::foundDate, Comparator.nullsLast(Comparator.reverseOrder()))
					.thenComparing(FoundItemListEntry::sourceType, Comparator.nullsLast(Comparator.naturalOrder()))
					.thenComparing(FoundItemListEntry::atcId, Comparator.nullsFirst(Comparator.naturalOrder()))
					.thenComparing(FoundItemListEntry::fdSn, Comparator.nullsFirst(Comparator.naturalOrder())));
			selected = new ArrayList<>(selected.subList(0, MAX_CANDIDATES));
		}

		Map<String, FoundItemListEntry> entriesByKey = new LinkedHashMap<>();
		List<CandidateRankingInput> inputs = new ArrayList<>();
		for (int i = 0; i < selected.size(); i++) {
			String key = "c" + (i + 1);
			FoundItemListEntry entry = selected.get(i);
			entriesByKey.put(key, entry);
			inputs.add(toRankingInput(key, entry));
		}
		return new CandidateSelection(entriesByKey, inputs, limited);
	}

	private CandidateSelection toInitialSelection(CandidatePreselector.Preselection preselection) {
		Map<String, FoundItemListEntry> entriesByKey = new LinkedHashMap<>();
		List<CandidateRankingInput> inputs = new ArrayList<>();
		for (int i = 0; i < preselection.entries().size(); i++) {
			String key = "c" + (i + 1);
			FoundItemListEntry entry = preselection.entries().get(i);
			entriesByKey.put(key, entry);
			inputs.add(toRankingInput(key, entry));
		}
		return new CandidateSelection(entriesByKey, inputs, preselection.limited());
	}

	private CandidateRankingInput toRankingInput(String key, FoundItemListEntry entry) {
		return new CandidateRankingInput(key, entry.productName(), entry.subject(), entry.categoryName(),
				entry.colorName(), entry.foundDate(), entry.storagePlace());
	}

	private PreparedMode prepare(Long memberId, Long lostItemId, RunSearchRequest request) {
		return switch (request.mode()) {
			case INITIAL -> {
				SearchSnapshot snapshot = persistenceService.prepareInitial(memberId, lostItemId);
				yield new PreparedMode(snapshot, snapshot.description());
			}
			case TEXT -> {
				if (request.message() == null || request.message().isBlank()) {
					throw new BusinessException(CommonErrorCode.VALIDATION_ERROR);
				}
				yield new PreparedMode(
						persistenceService.prepareText(memberId, lostItemId, request.message()), request.message());
			}
			case FILTER -> new PreparedMode(
					persistenceService.prepareFilter(memberId, lostItemId, request.conditions()), null);
		};
	}

	private FoundItemSearchQuery buildQuery(SearchSnapshot snapshot) {
		return new FoundItemSearchQuery(
				snapshot.categoryLargeCode(), snapshot.categoryMiddleCode(), snapshot.colorCode(), snapshot.regionCode(),
				snapshot.searchStartDate(), LocalDate.now(clock));
	}

	private SourceRun runSource(FoundItemSourceType sourceType, FoundItemSearchQuery query) {
		try {
			return new SourceRun(sourceType, true, foundItemCollectionService.lookup(sourceType, query));
		}
		catch (FoundItemLookupException e) {
			log.warn("{} 조회 실패, PARTIAL로 처리합니다.", sourceType);
			return new SourceRun(sourceType, false, List.of());
		}
	}

	private record PreparedMode(SearchSnapshot snapshot, String naturalLanguage) {
	}

	private record SourceRun(FoundItemSourceType sourceType, boolean success, List<FoundItemListEntry> entries) {
	}

	private record NaturalKey(FoundItemSourceType sourceType, String atcId, String fdSn) {
	}

	private record CandidateSelection(
			Map<String, FoundItemListEntry> entriesByKey,
			List<CandidateRankingInput> rankingInputs,
			boolean limited) {
	}
}
