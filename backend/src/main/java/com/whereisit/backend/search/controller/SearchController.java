package com.whereisit.backend.search.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.whereisit.backend.global.response.ApiResponse;
import com.whereisit.backend.search.dto.RunSearchRequest;
import com.whereisit.backend.search.dto.SearchExecutionResponse;
import com.whereisit.backend.search.service.SearchExecutionService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** API-11 AI 검색·조건 보정·재검색. */
@RestController
@RequestMapping("/api/lost-items/{lostItemId}/searches")
@RequiredArgsConstructor
public class SearchController {

	private final SearchExecutionService searchExecutionService;

	@PostMapping
	public ApiResponse<SearchExecutionResponse> run(@AuthenticationPrincipal Long memberId,
			@PathVariable Long lostItemId, @Valid @RequestBody RunSearchRequest request) {
		return ApiResponse.ok(searchExecutionService.execute(memberId, lostItemId, request));
	}
}
