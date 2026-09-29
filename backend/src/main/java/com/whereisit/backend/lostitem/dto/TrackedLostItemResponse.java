package com.whereisit.backend.lostitem.dto;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.whereisit.backend.candidate.dto.CandidateResponse;
import com.whereisit.backend.lostitem.entity.LostItem;

/**
 * API-16 전용 LostItem(Issue #99). 최상위 필드는 공유 {@link LostItemResponse}와 같고, conditions만
 * 표시값 2개가 더해진 {@link TrackedLostItemConditions}이다. 다른 API의 응답 계약은 바꾸지 않는다.
 * 값은 {@link LostItemResponse}가 계산한 것을 그대로 옮기므로 상태 표시(만료 시각이 지난 TRACKING → EXPIRED)도 같다.
 */
public record TrackedLostItemResponse(
		String lostItemId,
		String description,
		String languageCode,
		TrackedLostItemConditions conditions,
		String status,
		String notificationEmail,
		LocalDateTime startedAt,
		LocalDateTime expiresAt,
		LocalDate lastAutoSearchDate,
		int currentCandidateCount,
		LocalDateTime createdAt,
		LocalDateTime updatedAt,
		CandidateResponse currentCandidate) {

	/** API-16은 후보를 조회하지 않으므로 currentCandidate는 기존처럼 null이다. */
	public static TrackedLostItemResponse from(LostItem lostItem, Clock clock, int currentCandidateCount) {
		LostItemResponse base = LostItemResponse.from(lostItem, clock, currentCandidateCount);
		return new TrackedLostItemResponse(
				base.lostItemId(),
				base.description(),
				base.languageCode(),
				TrackedLostItemConditions.from(base.conditions(), lostItem),
				base.status(),
				base.notificationEmail(),
				base.startedAt(),
				base.expiresAt(),
				base.lastAutoSearchDate(),
				base.currentCandidateCount(),
				base.createdAt(),
				base.updatedAt(),
				base.currentCandidate());
	}
}
