package com.whereisit.backend.candidate.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.whereisit.backend.founditem.entity.FoundItem;

/** API 명세 v2 05_응답필드 FoundItem. 상세 API(found_place 등)는 Issue #35에서 아직 채우지 않아 항상 null이다. */
public record FoundItemResponse(
		String foundItemId,
		String sourceType,
		String atcId,
		String fdSn,
		String productName,
		String subject,
		String categoryName,
		String colorName,
		LocalDate foundDate,
		String storagePlace,
		String imageUrl,
		String foundPlace,
		String storagePhone,
		String description,
		LocalDateTime detailFetchedAt,
		LocalDateTime firstFetchedAt,
		LocalDateTime lastFetchedAt) {

	public static FoundItemResponse from(FoundItem foundItem) {
		return new FoundItemResponse(
				String.valueOf(foundItem.getId()),
				foundItem.getSourceType().name(),
				foundItem.getAtcId(),
				foundItem.getFdSn(),
				foundItem.getProductName(),
				foundItem.getSubject(),
				foundItem.getCategoryName(),
				foundItem.getColorName(),
				foundItem.getFoundDate(),
				foundItem.getStoragePlace(),
				foundItem.getImageUrl(),
				foundItem.getFoundPlace(),
				foundItem.getStoragePhone(),
				foundItem.getDescription(),
				foundItem.getDetailFetchedAt(),
				foundItem.getFirstFetchedAt(),
				foundItem.getLastFetchedAt());
	}
}
