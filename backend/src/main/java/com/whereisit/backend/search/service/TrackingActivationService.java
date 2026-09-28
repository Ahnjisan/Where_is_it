package com.whereisit.backend.search.service;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.whereisit.backend.founditem.client.FoundItemListEntry;
import com.whereisit.backend.founditem.client.FoundItemLookupException;
import com.whereisit.backend.founditem.service.FoundItemCollectionService;
import com.whereisit.backend.global.error.BusinessException;
import com.whereisit.backend.lostitem.dto.LostItemResponse;
import com.whereisit.backend.search.error.SearchErrorCode;
import com.whereisit.backend.search.service.TrackingActivationPersistenceService.ActivationPreparation;
import com.whereisit.backend.tracking.TrackingCandidateMatcher;
import com.whereisit.backend.tracking.TrackingSearchWindow;

import lombok.RequiredArgsConstructor;

/**
 * API-17 7일 추적 활성화 오케스트레이터. 일일 재검색과 같은 방식(포털기관 목록 1번 API를 습득일 범위로 조회한 뒤
 * 저장된 검색어로 매칭)으로 기준 후보를 정한다. DB 접근은 모두 {@link TrackingActivationPersistenceService}의
 * 짧은 트랜잭션에서 하고, 포털기관 조회 중에는 DB 트랜잭션·커넥션을 붙잡지 않는다.
 */
@Service
@RequiredArgsConstructor
public class TrackingActivationService {

	private final TrackingActivationPersistenceService persistenceService;
	private final FoundItemCollectionService foundItemCollectionService;
	private final TrackingCandidateMatcher candidateMatcher;
	private final Clock clock;

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public LostItemResponse activate(Long memberId, Long lostItemId, String notificationEmailOverride) {
		ActivationPreparation preparation = persistenceService.prepare(
				memberId, lostItemId, notificationEmailOverride);
		if (preparation.alreadyTracking()) {
			return preparation.existingResponse();
		}

		LocalDate today = LocalDate.now(clock);
		LocalDate fetchStart = TrackingSearchWindow.effectiveStart(preparation.condition().searchStartDate(), today);
		List<FoundItemListEntry> entries;
		try {
			entries = foundItemCollectionService.lookupPortalByFoundDate(fetchStart, today);
		}
		catch (FoundItemLookupException e) {
			throw new BusinessException(SearchErrorCode.LOST_API_UNAVAILABLE);
		}
		List<FoundItemListEntry> matched = candidateMatcher.match(entries, preparation.condition());

		return persistenceService.activateAfterBaseline(
				memberId, lostItemId, preparation.notificationEmail(), preparation.condition(), matched);
	}
}
