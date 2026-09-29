package com.whereisit.backend.lostitem.dto;

import java.time.LocalDate;

import com.whereisit.backend.lostitem.entity.LostItem;

/**
 * API-16 전용 검색조건(Issue #99). 공유 {@link SearchConditions}의 8개 필드에 사용자가 등록한 분실물의
 * 표시값 2개만 더한다. 표시값은 경찰청 검색용 공식 코드(categoryLargeCode·categoryMiddleCode·colorCode)가 아니다.
 *
 * @param itemTypeName 물품 종류 표시값. API-05에서 AI가 구조화한 물품명 검색어(productNameKeyword)를 그대로 쓴다.
 * @param colorName 색상 표시값. API-05에서 AI가 사용자 설명에서만 뽑은 색상명이며, 없거나 모호하면 null이다.
 */
public record TrackedLostItemConditions(
		String categoryLargeCode,
		String categoryMiddleCode,
		String colorCode,
		String regionCode,
		LocalDate lostDateFrom,
		LocalDate lostDateTo,
		String lostPlaceText,
		LocalDate searchStartDate,
		String itemTypeName,
		String colorName) {

	public static TrackedLostItemConditions from(SearchConditions conditions, LostItem lostItem) {
		return new TrackedLostItemConditions(
				conditions.categoryLargeCode(),
				conditions.categoryMiddleCode(),
				conditions.colorCode(),
				conditions.regionCode(),
				conditions.lostDateFrom(),
				conditions.lostDateTo(),
				conditions.lostPlaceText(),
				conditions.searchStartDate(),
				lostItem.getProductNameKeyword(),
				lostItem.getColorName());
	}
}
