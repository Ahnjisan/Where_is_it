package com.whereisit.backend.lostitem.dto;

import java.time.Clock;
import java.util.List;
import java.util.function.ToIntFunction;

import org.springframework.data.domain.Page;

import com.whereisit.backend.lostitem.entity.LostItem;

/** API 명세 v2 05_응답필드 LostItemPage. */
public record LostItemPageResponse(
		List<LostItemResponse> items, int page, int size, long totalElements, int totalPages) {

	public static LostItemPageResponse from(Page<LostItem> page, Clock clock, ToIntFunction<Long> currentCandidateCountOf) {
		List<LostItemResponse> items = page.getContent().stream()
				.map(item -> LostItemResponse.from(item, clock, currentCandidateCountOf.applyAsInt(item.getId())))
				.toList();
		return new LostItemPageResponse(items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
}
