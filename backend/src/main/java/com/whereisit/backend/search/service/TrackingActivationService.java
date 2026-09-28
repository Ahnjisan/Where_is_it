package com.whereisit.backend.search.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.whereisit.backend.global.error.BusinessException;
import com.whereisit.backend.lostitem.dto.LostItemResponse;
import com.whereisit.backend.search.dto.RunSearchRequest;
import com.whereisit.backend.search.dto.SearchExecutionResponse;
import com.whereisit.backend.search.dto.SearchMode;
import com.whereisit.backend.search.error.SearchErrorCode;
import com.whereisit.backend.search.service.TrackingActivationPersistenceService.ActivationPreparation;

import lombok.RequiredArgsConstructor;

/** API-14 7일 추적 활성화 오케스트레이터. 재검색 중에는 DB 트랜잭션을 유지하지 않는다. */
@Service
@RequiredArgsConstructor
public class TrackingActivationService {

	private final TrackingActivationPersistenceService persistenceService;
	private final SearchExecutionService searchExecutionService;

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public LostItemResponse activate(Long memberId, Long lostItemId, String notificationEmailOverride) {
		ActivationPreparation preparation = persistenceService.prepare(
				memberId, lostItemId, notificationEmailOverride);
		if (preparation.alreadyTracking()) {
			return preparation.existingResponse();
		}

		SearchExecutionResponse baseline = searchExecutionService.execute(
				memberId, lostItemId, new RunSearchRequest(SearchMode.INITIAL, null, null));
		if (!"COMPLETE".equals(baseline.lookupStatus())) {
			throw new BusinessException(SearchErrorCode.TRACKING_BASELINE_UNAVAILABLE);
		}

		return persistenceService.activateAfterBaseline(
				memberId, lostItemId, preparation.notificationEmail());
	}
}
