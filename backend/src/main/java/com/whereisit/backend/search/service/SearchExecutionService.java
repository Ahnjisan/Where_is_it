package com.whereisit.backend.search.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.whereisit.backend.founditem.client.FoundItemListEntry;
import com.whereisit.backend.founditem.client.FoundItemLookupException;
import com.whereisit.backend.founditem.client.FoundItemSearchQuery;
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

/** API-11 검색 실행 오케스트레이터. 모든 외부 호출은 활성 DB 트랜잭션 없이 실행한다. */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchExecutionService {

	private final SearchExecutionPersistenceService persistenceService;
	private final AiSearchConditionExtractor aiSearchConditionExtractor;
	private final FoundItemCollectionService foundItemCollectionService;
	private final Clock clock;

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public SearchExecutionResponse execute(Long memberId, Long lostItemId, RunSearchRequest request) {
		PreparedMode prepared = prepare(memberId, lostItemId, request);
		SearchSnapshot snapshot = prepared.snapshot();
		ChatMessageResponse assistantMessage = null;

		if (request.mode() != SearchMode.FILTER) {
			AiSearchConditionExtractionResult result = aiSearchConditionExtractor.extract(
					new AiSearchConditionExtractionRequest(
							prepared.naturalLanguage(),
							snapshot.description(),
							snapshot.languageCode(),
							LocalDate.now(clock),
							snapshot.lostDateFrom(),
							snapshot.lostDateTo(),
							snapshot.lostPlaceText()));
			AiAppliedResult applied = persistenceService.applyAiResult(memberId, lostItemId, result);
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
		return persistenceService.finalizeSearch(
				memberId, lostItemId, entries, complete, assistantMessage);
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
			log.warn("{} 조회 실패, PARTIAL로 처리합니다", sourceType);
			return new SourceRun(sourceType, false, List.of());
		}
	}

	private record PreparedMode(SearchSnapshot snapshot, String naturalLanguage) {
	}

	private record SourceRun(FoundItemSourceType sourceType, boolean success, List<FoundItemListEntry> entries) {
	}
}
