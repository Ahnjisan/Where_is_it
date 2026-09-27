package com.whereisit.backend.founditem.client;

import java.time.LocalDate;

import com.whereisit.backend.founditem.entity.FoundItemSourceType;

/**
 * 습득물 목록 API 응답 한 건. 외부 필드명({@code fdPrdtNm} 등)은 08_외부API 매핑을 따라 내부 이름으로 바꿨다.
 */
public record FoundItemListEntry(
		FoundItemSourceType sourceType,
		String atcId,
		String fdSn,
		String productName,
		String subject,
		String categoryName,
		String colorName,
		LocalDate foundDate,
		String storagePlace,
		String imageUrl) {
}
