package com.whereisit.backend.tracking;

import java.time.LocalDate;

/**
 * 추적 재검색 한 건의 매칭 조건. 분실물_검색_추적에 저장된 검색어와 습득물_조회_시작일로 만든다.
 *
 * @param productNameKeyword 필수. 습득물 물품명 또는 분류명에 포함돼야 한다.
 * @param storagePlaceKeyword 선택. 있으면 보관장소에 포함돼야 한다.
 * @param searchStartDate 필수. 습득일이 이 날짜 이후여야 한다.
 */
public record TrackingCondition(
		String productNameKeyword,
		String storagePlaceKeyword,
		LocalDate searchStartDate) {
}
