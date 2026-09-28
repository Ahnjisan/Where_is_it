package com.whereisit.backend.tracking;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.whereisit.backend.founditem.client.FoundItemListEntry;
import com.whereisit.backend.founditem.client.FoundItemLookupException;
import com.whereisit.backend.founditem.service.FoundItemCollectionService;
import com.whereisit.backend.notification.service.EmailSendException;
import com.whereisit.backend.notification.service.EmailSender;
import com.whereisit.backend.tracking.TrackingBatchPersistenceService.PendingEmail;
import com.whereisit.backend.tracking.TrackingBatchPersistenceService.TrackingTarget;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * 하루 1회 추적 재검색. 만료 처리 → 대상 선정 → 포털기관 목록 1번 API를 습득일 범위로 한 번만 조회 →
 * 추적 건마다 저장된 검색어로 매칭해 새 후보 저장 → 새 후보가 있으면 이메일 발송 순서로 진행한다.
 * 조회 결과를 모든 추적 건이 공유하므로 외부 호출 수는 추적 건수와 무관하다.
 *
 * 이 클래스는 트랜잭션을 열지 않는다. DB 반영은 {@link TrackingBatchPersistenceService}가 건마다 짧게 하고,
 * 포털기관 조회와 메일 발송 동안에는 DB 트랜잭션·커넥션을 붙잡지 않는다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingBatchService {

	private final TrackingBatchPersistenceService persistenceService;
	private final FoundItemCollectionService foundItemCollectionService;
	private final TrackingCandidateMatcher candidateMatcher;
	private final EmailSender emailSender;
	private final Clock clock;

	public TrackingBatchResult runDailyBatch() {
		LocalDateTime now = LocalDateTime.now(clock);
		LocalDate today = now.toLocalDate();

		int expired = persistenceService.expireOverdue(now);
		List<TrackingTarget> targets = persistenceService.findTargets(today, now);
		if (targets.isEmpty()) {
			return summarize(new TrackingBatchResult(expired, 0, 0, 0));
		}

		LocalDate fetchStart = targets.stream()
				.map(target -> TrackingSearchWindow.effectiveStart(target.condition().searchStartDate(), today))
				.min(LocalDate::compareTo)
				.orElse(today);
		List<FoundItemListEntry> entries;
		try {
			entries = foundItemCollectionService.lookupPortalByFoundDate(fetchStart, today);
		}
		catch (FoundItemLookupException e) {
			// 최근_자동검색_완료일을 갱신하지 않으므로 다음 실행에서 다시 검색한다.
			log.warn("추적 재검색용 포털기관 조회 실패: {}~{}", fetchStart, today, e);
			return summarize(new TrackingBatchResult(expired, 0, 0, targets.size()));
		}

		int searched = 0;
		int notified = 0;
		int failed = 0;
		for (TrackingTarget target : targets) {
			try {
				List<FoundItemListEntry> matched = candidateMatcher.match(entries, target.condition());
				Optional<PendingEmail> pending = persistenceService.applyDailyResult(
						target.lostItemId(), target.condition(), matched, now);
				searched++;
				if (pending.isPresent() && send(pending.get())) {
					notified++;
				}
			}
			catch (RuntimeException e) {
				failed++;
				log.error("분실물 {} 추적 재검색 반영 실패", target.lostItemId(), e);
			}
		}
		return summarize(new TrackingBatchResult(expired, searched, notified, failed));
	}

	private boolean send(PendingEmail pending) {
		try {
			emailSender.send(pending.recipientEmail(), pending.subject(), pending.body());
			persistenceService.markSent(pending.notificationId(), LocalDateTime.now(clock));
			return true;
		}
		catch (EmailSendException e) {
			log.warn("이메일_알림 {} 발송 실패: {}", pending.notificationId(), e.getErrorCode());
			persistenceService.markFailed(pending.notificationId(), e.getErrorCode());
			return false;
		}
	}

	private TrackingBatchResult summarize(TrackingBatchResult result) {
		log.info("추적 배치 완료: 만료 {}건, 재검색 {}건, 알림 발송 {}건, 실패 {}건",
				result.expired(), result.searched(), result.notified(), result.failed());
		return result;
	}

	/** 실행 요약. failed는 조회 실패로 처리하지 못한 대상 수 또는 반영 중 예외가 난 건수다. */
	public record TrackingBatchResult(int expired, int searched, int notified, int failed) {
	}
}
