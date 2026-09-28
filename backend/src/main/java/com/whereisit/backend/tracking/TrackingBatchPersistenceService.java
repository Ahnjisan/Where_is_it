package com.whereisit.backend.tracking;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.whereisit.backend.candidate.entity.LostItemCandidate;
import com.whereisit.backend.founditem.client.FoundItemListEntry;
import com.whereisit.backend.lostitem.entity.LostItem;
import com.whereisit.backend.lostitem.entity.LostItemStatus;
import com.whereisit.backend.lostitem.repository.LostItemRepository;
import com.whereisit.backend.notification.entity.EmailNotification;
import com.whereisit.backend.notification.repository.EmailNotificationRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 일일 추적 배치의 짧은 DB 트랜잭션 구간. 분실물 한 건마다 별도 트랜잭션으로 반영해,
 * 한 건의 실패가 다른 건의 결과를 되돌리지 않게 한다. 포털기관 조회와 메일 발송은 {@link TrackingBatchService}가 트랜잭션 밖에서 한다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingBatchPersistenceService {

	private final LostItemRepository lostItemRepository;
	private final EmailNotificationRepository notificationRepository;
	private final TrackingCandidateRecorder candidateRecorder;

	/** 추적_만료_시각이 지난 TRACKING 건을 검색 없이 EXPIRED로 바꾼다. */
	@Transactional
	public int expireOverdue(LocalDateTime now) {
		int expired = 0;
		for (LostItem lostItem : lostItemRepository.findByStatusAndDeletedAtIsNull(LostItemStatus.TRACKING)) {
			if (isOverdue(lostItem, now)) {
				lostItem.expire();
				expired++;
			}
		}
		return expired;
	}

	/** 오늘 아직 재검색하지 않은, 만료 전 TRACKING 건. 물품명 검색어가 없는 건은 매칭할 수 없어 건너뛴다. */
	@Transactional(readOnly = true)
	public List<TrackingTarget> findTargets(LocalDate today, LocalDateTime now) {
		List<TrackingTarget> targets = new ArrayList<>();
		for (LostItem lostItem : lostItemRepository.findByStatusAndDeletedAtIsNull(LostItemStatus.TRACKING)) {
			if (isOverdue(lostItem, now) || today.equals(lostItem.getLastAutoSearchDate())) {
				continue;
			}
			Optional<TrackingCondition> condition = conditionOf(lostItem);
			if (condition.isEmpty()) {
				log.warn("분실물 {}에 물품명 검색어가 없어 추적 재검색을 건너뜁니다", lostItem.getId());
				continue;
			}
			targets.add(new TrackingTarget(lostItem.getId(), condition.get()));
		}
		return targets;
	}

	/**
	 * 한 건의 재검색 결과를 반영한다. 조회하는 동안 상태·조건이 바뀌었거나 오늘 이미 처리됐으면 아무것도 하지 않는다.
	 * 새 후보가 있으면 오늘자 이메일_알림을 만들어 SENDING으로 두고, 트랜잭션이 끝난 뒤 보낼 내용을 돌려준다.
	 */
	@Transactional
	public Optional<PendingEmail> applyDailyResult(Long lostItemId, TrackingCondition expectedCondition,
			List<FoundItemListEntry> matched, LocalDateTime now) {
		LocalDate today = now.toLocalDate();
		Optional<LostItem> locked = lostItemRepository.findActiveByIdForUpdate(lostItemId);
		if (locked.isEmpty()) {
			return Optional.empty();
		}
		LostItem lostItem = locked.get();
		if (lostItem.getStatus() != LostItemStatus.TRACKING || isOverdue(lostItem, now)
				|| today.equals(lostItem.getLastAutoSearchDate())) {
			return Optional.empty();
		}
		if (!conditionOf(lostItem).equals(Optional.of(expectedCondition))) {
			log.info("분실물 {}의 추적 조건이 조회 중에 바뀌어 오늘 결과를 반영하지 않습니다", lostItemId);
			return Optional.empty();
		}

		List<LostItemCandidate> newCandidates = candidateRecorder.record(lostItem, matched, now, false);
		lostItem.markAutoSearchCompleted(today);
		if (newCandidates.isEmpty()
				|| notificationRepository.findByLostItemIdAndNotificationDate(lostItemId, today).isPresent()) {
			return Optional.empty();
		}

		TrackingEmailContent content = TrackingEmailContent.of(lostItem.getLanguageCode(), newCandidates);
		EmailNotification notification = notificationRepository.save(EmailNotification.create(
				lostItem, today, lostItem.getNotificationEmail(), lostItem.getLanguageCode(),
				content.subject(), content.body()));
		newCandidates.forEach(candidate -> candidate.linkNotification(notification));
		notification.markSending();
		return Optional.of(new PendingEmail(
				notification.getId(), notification.getRecipientEmail(), content.subject(), content.body()));
	}

	@Transactional
	public void markSent(Long notificationId, LocalDateTime sentAt) {
		notificationRepository.findById(notificationId).ifPresent(notification -> notification.markSent(sentAt));
	}

	@Transactional
	public void markFailed(Long notificationId, String errorCode) {
		notificationRepository.findById(notificationId).ifPresent(notification -> notification.markFailed(errorCode));
	}

	private boolean isOverdue(LostItem lostItem, LocalDateTime now) {
		return lostItem.getExpiresAt() != null && !lostItem.getExpiresAt().isAfter(now);
	}

	private Optional<TrackingCondition> conditionOf(LostItem lostItem) {
		if (lostItem.getProductNameKeyword() == null) {
			return Optional.empty();
		}
		return Optional.of(new TrackingCondition(
				lostItem.getProductNameKeyword(), lostItem.getStoragePlaceKeyword(), lostItem.getSearchStartDate()));
	}

	public record TrackingTarget(Long lostItemId, TrackingCondition condition) {
	}

	public record PendingEmail(Long notificationId, String recipientEmail, String subject, String body) {
	}
}
