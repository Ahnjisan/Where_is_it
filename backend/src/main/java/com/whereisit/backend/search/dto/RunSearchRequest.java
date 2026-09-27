package com.whereisit.backend.search.dto;

import com.whereisit.backend.lostitem.dto.SearchConditionsPatch;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * API-11 요청. mode에 따라 message(TEXT)·conditions(FILTER)가 필요하며, 서비스에서 확인한다.
 * 명세는 mode별로 body 모양이 다른 판별 union이지만, 하나의 평면 구조로 단순화했다.
 */
public record RunSearchRequest(
		@NotNull SearchMode mode,
		@Size(min = 1, max = 2000) String message,
		@Valid SearchConditionsPatch conditions) {
}
