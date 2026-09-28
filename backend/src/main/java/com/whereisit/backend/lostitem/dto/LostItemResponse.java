package com.whereisit.backend.lostitem.dto;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.whereisit.backend.lostitem.entity.LostItem;
import com.whereisit.backend.lostitem.entity.LostItemStatus;

/**
 * API 명세 v2 05_응답필드 LostItem.
 */
public record LostItemResponse(
		String lostItemId,
		String description,
		String languageCode,
		SearchConditions conditions,
		String status,
		String notificationEmail,
		LocalDateTime startedAt,
		LocalDateTime expiresAt,
		LocalDate lastAutoSearchDate,
		int currentCandidateCount,
		LocalDateTime createdAt,
		LocalDateTime updatedAt) {

	/** 후보가 아직 없는(방금 생성된) 분실물용. */
	public static LostItemResponse from(LostItem lostItem, Clock clock) {
		return from(lostItem, clock, 0);
	}

	public static LostItemResponse from(LostItem lostItem, Clock clock, int currentCandidateCount) {
		return new LostItemResponse(
				String.valueOf(lostItem.getId()),
				lostItem.getDescription(),
				lostItem.getLanguageCode().getCode(),
				SearchConditions.from(lostItem),
				effectiveStatus(lostItem, clock).name(),
				lostItem.getNotificationEmail(),
				lostItem.getStartedAt(),
				lostItem.getExpiresAt(),
				lostItem.getLastAutoSearchDate(),
				currentCandidateCount,
				lostItem.getCreatedAt(),
				lostItem.getUpdatedAt());
	}

	/** API-06 규칙 3: 만료시각이 지난 TRACKING도 응답에서는 EXPIRED로 보여준다(DB 값은 배치가 나중에 바꾼다). */
	private static LostItemStatus effectiveStatus(LostItem lostItem, Clock clock) {
		if (lostItem.getStatus() == LostItemStatus.TRACKING
				&& lostItem.getExpiresAt() != null
				&& !lostItem.getExpiresAt().isAfter(LocalDateTime.now(clock))) {
			return LostItemStatus.EXPIRED;
		}
		return lostItem.getStatus();
	}
}
