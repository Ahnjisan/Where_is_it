package com.whereisit.backend.search.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.whereisit.backend.global.response.ApiResponse;
import com.whereisit.backend.lostitem.dto.LostItemResponse;
import com.whereisit.backend.search.dto.ActivateTrackingRequest;
import com.whereisit.backend.search.service.TrackingActivationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** API-14 7일 추적 활성화. */
@Tag(name = "추적", description = "7일 추적 활성화")
@RestController
@RequestMapping("/api/lost-items/{lostItemId}/tracking")
@RequiredArgsConstructor
public class TrackingController {

	private final TrackingActivationService trackingActivationService;

	@Operation(summary = "API-14 7일 추적 활성화", description = """
			등록 직전에 다시 검색해 기준 후보를 정한 뒤 TRACKING으로 바꾼다. 만료 시각은 시작 시각 + 7일이다.
			검색 시작일(conditions.searchStartDate)이 있어야 한다. 본문은 생략할 수 있고,
			notificationEmail을 보내지 않으면 회원 이메일로 알린다. 이미 TRACKING이면 기존 정보 그대로 200이다.""")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "활성화 성공 또는 이미 TRACKING"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "VALIDATION_ERROR, INVALID_SEARCH_CONDITION(검색 시작일 없음)"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_REQUIRED(토큰 없음), INVALID_TOKEN, TOKEN_EXPIRED"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "RESOURCE_NOT_FOUND(없거나, 삭제됐거나, 다른 회원의 분실물)"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "TRACKING_EXPIRED"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "429", description = "RATE_LIMITED(AI 사용량 초과)"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "AI_CONDITION_UNAVAILABLE, LOST_API_UNAVAILABLE"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "503", description = "TRACKING_BASELINE_UNAVAILABLE(한쪽 출처만 성공해 기준 후보를 정하지 못함)"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "504", description = "SEARCH_TIMEOUT")
	})
	@PostMapping
	public ApiResponse<LostItemResponse> activate(@AuthenticationPrincipal Long memberId,
			@Parameter(description = "분실물 ID") @PathVariable Long lostItemId, @Valid @RequestBody(required = false) ActivateTrackingRequest request) {
		String notificationEmail = request == null ? null : request.notificationEmail();
		return ApiResponse.ok(trackingActivationService.activate(memberId, lostItemId, notificationEmail));
	}
}
