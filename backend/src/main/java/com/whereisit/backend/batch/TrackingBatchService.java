package com.whereisit.backend.batch;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.whereisit.backend.candidate.entity.LostItemCandidate;
import com.whereisit.backend.candidate.repository.LostItemCandidateRepository;
import com.whereisit.backend.lostitem.entity.LostItem;
import com.whereisit.backend.lostitem.entity.LostItemStatus;
import com.whereisit.backend.lostitem.repository.LostItemRepository;
import com.whereisit.backend.notification.entity.EmailNotification;
import com.whereisit.backend.notification.repository.EmailNotificationRepository;
import com.whereisit.backend.notification.service.EmailSendException;
import com.whereisit.backend.notification.service.EmailSender;
import com.whereisit.backend.search.dto.RunSearchRequest;
import com.whereisit.backend.search.dto.SearchExecutionResponse;
import com.whereisit.backend.search.dto.SearchMode;
import com.whereisit.backend.search.service.SearchExecutionService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Issue #37. 09_내부배치 B01~B09를 단순화해 구현했다. 여러 서버·정확히1회 전달 보장, 분실물 단위 잠금(ITEM_BUSY)은
 * 이번 범위에서 제외했다(PR 설명 참고). 전체를 한 트랜잭션에서 처리해, 한 건 실패는 로그만 남기고 다음 건으로 넘어간다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TrackingBatchService {

	private final LostItemRepository lostItemRepository;
	private final LostItemCandidateRepository candidateRepository;
	private final EmailNotificationRepository notificationRepository;
	private final SearchExecutionService searchExecutionService;
	private final EmailSender emailSender;
	private final Clock clock;

	@Transactional
	public void runDailyBatch() {
		LocalDateTime now = LocalDateTime.now(clock);
		LocalDate today = now.toLocalDate();

		for (LostItem lostItem : lostItemRepository.findByStatusAndDeletedAtIsNull(LostItemStatus.TRACKING)) {
			try {
				processOne(lostItem, now, today);
			} catch (RuntimeException e) {
				log.error("분실물 {} 일일 배치 처리 중 예상하지 못한 오류", lostItem.getId(), e);
			}
		}
	}

	private void processOne(LostItem lostItem, LocalDateTime now, LocalDate today) {
		// B09: 만료된 건은 상태만 바꾸고, 검색·발송은 시작하지 않는다.
		if (lostItem.getExpiresAt() != null && !lostItem.getExpiresAt().isAfter(now)) {
			lostItem.expire();
			return;
		}
		// B02: 오늘 이미 성공한 검색은 다시 실행하지 않는다.
		if (today.equals(lostItem.getLastAutoSearchDate())) {
			return;
		}

		SearchExecutionResponse result;
		try {
			result = searchExecutionService.execute(lostItem.getMember().getId(), lostItem.getId(),
					new RunSearchRequest(SearchMode.INITIAL, null, null));
		} catch (RuntimeException e) {
			// B04: 실패하면 최근_자동검색_완료일을 갱신하지 않고 다음 날 다시 시도한다.
			log.warn("분실물 {} 자동 재검색 실패", lostItem.getId(), e);
			return;
		}
		if (!"COMPLETE".equals(result.lookupStatus())) {
			return;
		}
		lostItem.markAutoSearchCompleted(today);

		List<LostItemCandidate> newCandidates = candidateRepository.findNewSimilarCandidates(lostItem.getId());
		if (newCandidates.isEmpty()) {
			return;
		}
		// UNIQUE(lost_item_id, notification_date)와 별개로 애플리케이션에서도 같은 날 중복 발송을 막는다.
		if (notificationRepository.findByLostItemIdAndNotificationDate(lostItem.getId(), today).isPresent()) {
			return;
		}

		sendNewCandidateEmail(lostItem, today, newCandidates);
	}

	private void sendNewCandidateEmail(LostItem lostItem, LocalDate today, List<LostItemCandidate> newCandidates) {
		String subject = "유사한 습득물 후보 %d건이 새로 확인되었습니다".formatted(newCandidates.size());
		String body = newCandidates.stream()
				.map(c -> "- " + Optional.ofNullable(c.getFoundItem().getProductName()).orElse("(품명 미상)"))
				.collect(Collectors.joining("\n"));

		EmailNotification notification = EmailNotification.create(
				lostItem, today, lostItem.getNotificationEmail(), lostItem.getLanguageCode(), subject, body);
		notificationRepository.save(notification);
		newCandidates.forEach(candidate -> candidate.linkNotification(notification));

		notification.markSending();
		try {
			emailSender.send(notification.getRecipientEmail(), subject, body);
			notification.markSent(LocalDateTime.now(clock));
		} catch (EmailSendException e) {
			notification.markFailed(e.getErrorCode());
		}
	}
}
