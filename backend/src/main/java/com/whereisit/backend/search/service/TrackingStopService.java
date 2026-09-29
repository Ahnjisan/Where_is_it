package com.whereisit.backend.search.service;

import java.time.Clock;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.whereisit.backend.candidate.repository.LostItemCandidateRepository;
import com.whereisit.backend.global.error.BusinessException;
import com.whereisit.backend.global.error.CommonErrorCode;
import com.whereisit.backend.lostitem.dto.LostItemResponse;
import com.whereisit.backend.lostitem.entity.LostItem;
import com.whereisit.backend.lostitem.entity.LostItemStatus;
import com.whereisit.backend.lostitem.error.LostItemErrorCode;
import com.whereisit.backend.lostitem.repository.LostItemRepository;

import lombok.RequiredArgsConstructor;

/**
 * API-21 추적 종료. 상태 CHECK(SEARCHING·TRACKING·EXPIRED)와 추적 기간 CHECK(expires_at = started_at + 7일)를
 * 지키기 위해 새 상태를 만들지 않고 EXPIRED로 바꾸며, 시작·만료 시각은 그대로 둔다.
 * 일일 배치는 같은 행을 잠근 뒤 TRACKING인지 다시 확인하므로, 종료 이후에는 재검색·알림이 만들어지지 않는다.
 */
@Service
@RequiredArgsConstructor
public class TrackingStopService {

	private final LostItemRepository lostItemRepository;
	private final LostItemCandidateRepository candidateRepository;
	private final Clock clock;

	@Transactional
	public LostItemResponse stop(Long memberId, Long lostItemId) {
		LostItem lostItem = lostItemRepository.findOwnedActiveByIdForUpdate(lostItemId, memberId)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
		if (lostItem.getStatus() == LostItemStatus.SEARCHING) {
			throw new BusinessException(LostItemErrorCode.TRACKING_NOT_STARTED);
		}
		// 이미 EXPIRED면 아무것도 바꾸지 않아 updated_at도 그대로다(멱등). 만료 시각이 지난 TRACKING도 여기서 EXPIRED로 맞춘다.
		if (lostItem.getStatus() == LostItemStatus.TRACKING) {
			lostItem.expire();
		}
		return LostItemResponse.from(lostItem, clock,
				(int) candidateRepository.countByLostItemIdAndCurrentTrue(lostItem.getId()));
	}
}
