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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** API-11 AI 검색·조건 보정·재검색. */
@Tag(name = "검색", description = "AI 검색조건 구조화와 습득물 후보 검색")
@RestController
@RequestMapping("/api/lost-items/{lostItemId}/searches")
@RequiredArgsConstructor
public class SearchController {

	private final SearchExecutionService searchExecutionService;

	@Operation(summary = "API-11 검색 실행", description = """
			mode에 따라 검색조건을 정하고 경찰청·포털기관 습득물 API를 조회해 후보를 추천한다.
			- INITIAL: 저장된 설명을 AI로 구조화해 검색한다.
			- TEXT: message(추가 설명)를 AI로 반영해 다시 검색한다. message 필수.
			- FILTER: conditions로 조건을 직접 바꿔 검색한다(AI 호출 없음). conditions 필수.
			두 출처가 모두 성공하면 lookupStatus=COMPLETE이고 후보를 저장한다.
			한쪽만 성공하면 PARTIAL이며 후보를 저장하지 않고 이번 응답에만 담는다(candidateId가 null).""")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "검색 성공(COMPLETE 또는 PARTIAL)"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "VALIDATION_ERROR(mode별 필수값 누락 등), INVALID_SEARCH_CONDITION"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_REQUIRED(토큰 없음), INVALID_TOKEN, TOKEN_EXPIRED"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "RESOURCE_NOT_FOUND(없거나, 삭제됐거나, 다른 회원의 분실물)"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "TRACKING_EXPIRED"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "RATE_LIMITED(AI 사용량 초과)"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "AI_CONDITION_UNAVAILABLE, LOST_API_UNAVAILABLE(두 습득물 출처 모두 실패)"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "504", description = "SEARCH_TIMEOUT(AI 응답 시간 초과)")
	})
	@PostMapping
	public ApiResponse<SearchExecutionResponse> run(@AuthenticationPrincipal Long memberId,
			@Parameter(description = "분실물 ID") @PathVariable Long lostItemId, @Valid @RequestBody RunSearchRequest request) {
		return ApiResponse.ok(searchExecutionService.execute(memberId, lostItemId, request));
	}
}
