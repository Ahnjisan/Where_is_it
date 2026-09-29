package com.whereisit.backend.lostitem.dto;

import java.time.Clock;
import java.util.List;
import java.util.function.ToIntFunction;

import org.springframework.data.domain.Page;

import com.whereisit.backend.lostitem.entity.LostItem;

/** API-16 전용 페이지(Issue #99). 페이지 필드는 공유 {@link LostItemPageResponse}와 같다. */
public record TrackedLostItemPageResponse(
		List<TrackedLostItemResponse> items, int page, int size, long totalElements, int totalPages) {

	public static TrackedLostItemPageResponse from(Page<LostItem> page, Clock clock,
			ToIntFunction<Long> currentCandidateCountOf) {
		List<TrackedLostItemResponse> items = page.getContent().stream()
				.map(item -> TrackedLostItemResponse.from(item, clock, currentCandidateCountOf.applyAsInt(item.getId())))
				.toList();
		return new TrackedLostItemPageResponse(
				items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
}
