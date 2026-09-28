package com.whereisit.backend.search.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.whereisit.backend.candidate.repository.LostItemCandidateRepository;
import com.whereisit.backend.founditem.client.FoundItemListEntry;
import com.whereisit.backend.global.error.BusinessException;
import com.whereisit.backend.global.error.CommonErrorCode;
import com.whereisit.backend.lostitem.dto.LostItemResponse;
import com.whereisit.backend.lostitem.entity.LostItem;
import com.whereisit.backend.lostitem.entity.LostItemStatus;
import com.whereisit.backend.lostitem.error.LostItemErrorCode;
import com.whereisit.backend.lostitem.repository.LostItemRepository;
import com.whereisit.backend.lostitem.service.LostItemService;
import com.whereisit.backend.search.error.SearchErrorCode;
import com.whereisit.backend.tracking.TrackingCandidateRecorder;
import com.whereisit.backend.tracking.TrackingCondition;

import lombok.RequiredArgsConstructor;

/** API-17의 짧은 DB 트랜잭션 구간. 포털기관 조회는 {@link TrackingActivationService}가 트랜잭션 밖에서 한다. */
@Service
@RequiredArgsConstructor
public class TrackingActivationPersistenceService {

	private final LostItemService lostItemService;
	private final LostItemRepository lostItemRepository;
	private final LostItemCandidateRepository candidateRepository;
	private final TrackingCandidateRecorder candidateRecorder;
	private final Clock clock;

	@Transactional(readOnly = true)
	public ActivationPreparation prepare(Long memberId, Long lostItemId, String notificationEmailOverride) {
		LostItem lostItem = lostItemService.findOwned(memberId, lostItemId);
		if (lostItem.getStatus() == LostItemStatus.TRACKING) {
			return new ActivationPreparation(true, null, null, response(lostItem));
		}
		lostItemService.ensureNotExpired(lostItem);
		String notificationEmail = notificationEmailOverride != null
				? notificationEmailOverride
				: lostItem.getMember().getEmail();
		return new ActivationPreparation(false, notificationEmail, conditionOf(lostItem), null);
	}

	/**
	 * 트랜잭션 밖에서 매칭한 결과를 기준 후보로 저장하고 TRACKING으로 바꾼다. 조회하는 동안 검색 조건이 바뀌었으면
	 * 오래된 조건의 기준선을 저장하지 않도록 ITEM_BUSY로 거부한다.
	 */
	@Transactional
	public LostItemResponse activateAfterBaseline(Long memberId, Long lostItemId, String notificationEmail,
			TrackingCondition expectedCondition, List<FoundItemListEntry> matched) {
		LostItem lostItem = lostItemRepository.findOwnedActiveByIdForUpdate(lostItemId, memberId)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
		if (lostItem.getStatus() == LostItemStatus.TRACKING) {
			return response(lostItem);
		}
		lostItemService.ensureNotExpired(lostItem);
		if (!expectedCondition.equals(conditionOf(lostItem))) {
			throw new BusinessException(SearchErrorCode.ITEM_BUSY);
		}

		LocalDateTime now = LocalDateTime.now(clock);
		lostItem.fillSearchStartDateIfAbsent(expectedCondition.searchStartDate());
		candidateRecorder.record(lostItem, matched, now, true);
		lostItem.activateTracking(now, notificationEmail);
		// 기준선 조회가 오늘 재검색을 대신하므로, 같은 날 배치가 다시 알림 후보를 만들지 않게 한다.
		lostItem.markAutoSearchCompleted(now.toLocalDate());
		return response(lostItem);
	}

	/**
	 * 추적 매칭 조건. 물품명 검색어(API-05에서 AI가 구조화)가 없으면 추적할 수 없다.
	 * 습득물_조회_시작일이 비어 있으면 분실 기간 시작일, 그것도 없으면 검색 건 생성일을 쓴다.
	 */
	private TrackingCondition conditionOf(LostItem lostItem) {
		if (lostItem.getProductNameKeyword() == null) {
			throw new BusinessException(LostItemErrorCode.INVALID_SEARCH_CONDITION);
		}
		return new TrackingCondition(
				lostItem.getProductNameKeyword(), lostItem.getStoragePlaceKeyword(), searchStartDateOf(lostItem));
	}

	private LocalDate searchStartDateOf(LostItem lostItem) {
		if (lostItem.getSearchStartDate() != null) {
			return lostItem.getSearchStartDate();
		}
		if (lostItem.getLostDateFrom() != null) {
			return lostItem.getLostDateFrom();
		}
		return lostItem.getCreatedAt().toLocalDate();
	}

	private LostItemResponse response(LostItem lostItem) {
		return LostItemResponse.from(lostItem, clock, (int) candidateRepository.countByLostItemIdAndCurrentTrue(lostItem.getId()));
	}

	public record ActivationPreparation(
			boolean alreadyTracking, String notificationEmail, TrackingCondition condition,
			LostItemResponse existingResponse) {
	}
}
