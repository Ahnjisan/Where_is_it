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
import com.whereisit.backend.search.service.TrackingStopService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** API-17 7일 추적 활성화, API-21 추적 종료. */
@Tag(name = "추적", description = "7일 추적 활성화·종료")
@RestController
@RequestMapping("/api/lost-items/{lostItemId}/tracking")
@RequiredArgsConstructor
public class TrackingController {

	private final TrackingActivationService trackingActivationService;
	private final TrackingStopService trackingStopService;

	@Operation(summary = "API-17 7일 추적 활성화", description = """
			포털기관 습득물을 습득일 범위로 조회해 저장된 물품명·보관장소 검색어와 맞는 습득물을 기준 후보로 저장한 뒤
			TRACKING으로 바꾼다. 만료 시각은 시작 시각 + 7일이다. API-05에서 물품명 검색어가 구조화돼 있어야 하며,
			검색 시작일(conditions.searchStartDate)이 없으면 분실 기간 시작일, 그것도 없으면 검색 건 생성일을 쓴다.
			조회 범위는 최대 30일 전까지다. 본문은 생략할 수 있고, notificationEmail을 보내지 않으면 회원 이메일로 알린다.
			이미 TRACKING이면 기존 정보 그대로 200이다.""")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "활성화 성공 또는 이미 TRACKING"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "VALIDATION_ERROR, INVALID_SEARCH_CONDITION(물품명 검색어 없음)"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_REQUIRED(토큰 없음), INVALID_TOKEN, TOKEN_EXPIRED"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "RESOURCE_NOT_FOUND(없거나, 삭제됐거나, 다른 회원의 분실물)"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "TRACKING_EXPIRED, ITEM_BUSY(조회하는 동안 검색 조건이 바뀜)"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "502", description = "LOST_API_UNAVAILABLE(포털기관 조회 실패)")
	})
	@PostMapping
	public ApiResponse<LostItemResponse> activate(@AuthenticationPrincipal Long memberId,
			@Parameter(description = "분실물 ID") @PathVariable Long lostItemId, @Valid @RequestBody(required = false) ActivateTrackingRequest request) {
		String notificationEmail = request == null ? null : request.notificationEmail();
		return ApiResponse.ok(trackingActivationService.activate(memberId, lostItemId, notificationEmail));
	}

	@Operation(summary = "API-21 추적 종료", description = """
			7일 추적 기간이 끝나기 전에 추적을 종료한다. 상태를 EXPIRED로 바꾸고 시작·만료 시각은 그대로 둔다.
			종료한 건은 일일 재검색과 새 후보 이메일 알림 대상에서 빠지며, 다시 추적 등록(API-17)하면 TRACKING_EXPIRED이다.
			이미 EXPIRED이거나 만료 시각이 지난 건도 200이다(반복 호출해도 상태·시각이 바뀌지 않음).
			종료일은 EXPIRED 건의 updatedAt과 expiresAt 중 이른 값으로 표시한다(직접 종료는 updatedAt, 기간 만료는 expiresAt).""")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "종료 성공 또는 이미 EXPIRED"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_REQUIRED(토큰 없음), INVALID_TOKEN, TOKEN_EXPIRED"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "RESOURCE_NOT_FOUND(없거나, 삭제됐거나, 다른 회원의 분실물)"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "TRACKING_NOT_STARTED(추적을 등록하지 않은 SEARCHING 건)")
	})
	@PostMapping("/stop")
	public ApiResponse<LostItemResponse> stop(@AuthenticationPrincipal Long memberId,
			@Parameter(description = "분실물 ID") @PathVariable Long lostItemId) {
		return ApiResponse.ok(trackingStopService.stop(memberId, lostItemId));
	}
}
