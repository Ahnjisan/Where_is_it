package com.whereisit.backend.notification.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.whereisit.backend.candidate.entity.LostItemCandidate;
import com.whereisit.backend.notification.entity.EmailNotification;

/**
 * API 명세 v2 05_응답필드 Notification. 메일 본문·오류원문·SMTP자격증명은 노출하지 않는다(02_공통규칙).
 */
public record NotificationResponse(
		String notificationId,
		String lostItemId,
		LocalDate notificationDate,
		String recipientEmail,
		String languageCode,
		String subject,
		String status,
		int candidateCount,
		List<String> candidateIds,
		LocalDateTime sentAt,
		LocalDateTime createdAt) {

	public static NotificationResponse from(EmailNotification notification, List<LostItemCandidate> linkedCandidates) {
		return new NotificationResponse(
				String.valueOf(notification.getId()),
				String.valueOf(notification.getLostItem().getId()),
				notification.getNotificationDate(),
				notification.getRecipientEmail(),
				notification.getLanguageCode().getCode(),
				notification.getSubject(),
				notification.getStatus().name(),
				linkedCandidates.size(),
				linkedCandidates.stream().map(c -> String.valueOf(c.getId())).toList(),
				notification.getSentAt(),
				notification.getCreatedAt());
	}
}
