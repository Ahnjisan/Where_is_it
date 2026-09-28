package com.whereisit.backend.candidate.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.whereisit.backend.candidate.dto.CandidatePageResponse;
import com.whereisit.backend.candidate.dto.CandidateResponse;
import com.whereisit.backend.candidate.service.CandidateService;
import com.whereisit.backend.global.response.ApiResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * API-12·13. notificationId 필터는 이메일_알림 테이블이 생기는 Issue #37에서 추가한다.
 */
@Tag(name = "후보", description = "추천 습득물 후보 조회")
@RestController
@RequestMapping("/api/lost-items/{lostItemId}/candidates")
@RequiredArgsConstructor
public class CandidateController {

	private final CandidateService candidateService;

	@Operation(summary = "API-12 추천 후보 목록", description = """
			scope=CURRENT는 마지막 완전 검색(COMPLETE)의 후보를 순위순으로, scope=ALL은 지금까지 나온 모든 후보를 최근 확인순으로 준다.""")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_REQUIRED(토큰 없음), INVALID_TOKEN, TOKEN_EXPIRED"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "RESOURCE_NOT_FOUND(없거나, 삭제됐거나, 다른 회원의 분실물)")
	})
	@GetMapping
	public ApiResponse<CandidatePageResponse> list(@AuthenticationPrincipal Long memberId,
			@Parameter(description = "분실물 ID") @PathVariable Long lostItemId,
			@Parameter(description = "조회 범위", schema = @Schema(allowableValues = { "CURRENT", "ALL" }))
			@RequestParam(defaultValue = "CURRENT") String scope,
			@ParameterObject Pageable pageable) {
		return ApiResponse.ok(candidateService.list(memberId, lostItemId, scope, pageable));
	}

	@Operation(summary = "API-13 추천 후보 상세")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_REQUIRED(토큰 없음), INVALID_TOKEN, TOKEN_EXPIRED"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "RESOURCE_NOT_FOUND(분실물이나 후보가 없거나 다른 회원의 것)")
	})
	@GetMapping("/{candidateId}")
	public ApiResponse<CandidateResponse> getDetail(@AuthenticationPrincipal Long memberId,
			@Parameter(description = "분실물 ID") @PathVariable Long lostItemId,
			@Parameter(description = "후보 ID") @PathVariable Long candidateId) {
		return ApiResponse.ok(candidateService.getDetail(memberId, lostItemId, candidateId));
	}
}
