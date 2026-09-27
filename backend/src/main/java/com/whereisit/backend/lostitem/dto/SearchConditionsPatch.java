package com.whereisit.backend.lostitem.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.Size;

/**
 * API 명세 v2 04_요청필드 API-08 body.conditions. 전송한 필드만 바꾸므로 전부 선택값이다.
 */
public record SearchConditionsPatch(
		@Size(max = 6) String categoryLargeCode,
		@Size(max = 6) String categoryMiddleCode,
		@Size(max = 8) String colorCode,
		@Size(max = 6) String regionCode,
		LocalDate lostDateFrom,
		LocalDate lostDateTo,
		@Size(max = 255) String lostPlaceText,
		LocalDate searchStartDate) {
}
