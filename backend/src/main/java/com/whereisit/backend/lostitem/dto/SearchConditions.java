package com.whereisit.backend.lostitem.dto;

import java.time.LocalDate;

import com.whereisit.backend.lostitem.entity.LostItem;

/**
 * API 명세 v2 05_응답필드 SearchConditions. 실제 코드표 검증(Issue #39)은 아직 연결하지 않았다.
 */
public record SearchConditions(
		String categoryLargeCode,
		String categoryMiddleCode,
		String colorCode,
		String regionCode,
		LocalDate lostDateFrom,
		LocalDate lostDateTo,
		String lostPlaceText,
		LocalDate searchStartDate) {

	public static SearchConditions from(LostItem lostItem) {
		return new SearchConditions(
				lostItem.getCategoryLargeCode(),
				lostItem.getCategoryMiddleCode(),
				lostItem.getColorCode(),
				lostItem.getRegionCode(),
				lostItem.getLostDateFrom(),
				lostItem.getLostDateTo(),
				lostItem.getLostPlaceText(),
				lostItem.getSearchStartDate());
	}
}
