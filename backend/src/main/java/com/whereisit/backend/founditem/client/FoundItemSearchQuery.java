package com.whereisit.backend.founditem.client;

import java.time.LocalDate;

/**
 * 두 습득물 API에 공통으로 넘기는 검색조건. 코드값 검증(Issue #39)은 아직 연결하지 않았다.
 */
public record FoundItemSearchQuery(
		String categoryLargeCode,
		String categoryMiddleCode,
		String colorCode,
		String regionCode,
		LocalDate startDate,
		LocalDate endDate) {
}
