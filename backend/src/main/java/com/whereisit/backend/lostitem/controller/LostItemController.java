package com.whereisit.backend.lostitem.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.whereisit.backend.global.response.ApiResponse;
import com.whereisit.backend.lostitem.dto.CreateLostItemRequest;
import com.whereisit.backend.lostitem.dto.LostItemPageResponse;
import com.whereisit.backend.lostitem.dto.LostItemResponse;
import com.whereisit.backend.lostitem.dto.UpdateLostItemRequest;
import com.whereisit.backend.lostitem.entity.LostItemStatus;
import com.whereisit.backend.lostitem.service.InitialSearchOrchestrator;
import com.whereisit.backend.lostitem.service.LostItemService;
import com.whereisit.backend.search.dto.SearchExecutionResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * API-05·06·07·08·09. URL은 API 명세 v2 02_공통규칙에 따라 조회는 GET, 쓰기는 POST만 쓴다(PUT·PATCH·DELETE 없음).
 */
@Tag(name = "분실물", description = "분실물 검색·추적 등록 CRUD")
@RestController
@RequestMapping("/api/lost-items")
@RequiredArgsConstructor
public class LostItemController {

	private final LostItemService lostItemService;
	private final InitialSearchOrchestrator initialSearchOrchestrator;

	@Operation(summary = "API-05 최초 분실물 검색", description = """
			분실물 검색 건과 첫 USER 메시지를 저장한 뒤 AI 조건 추출, 공공데이터 조회, 후보 랭킹을 실행한다.
			languageCode를 보내지 않으면 회원의 사용 언어를 쓴다.""")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "등록 성공. Location 헤더에 상세 조회 경로"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "VALIDATION_ERROR, UNSUPPORTED_LANGUAGE"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_REQUIRED(토큰 없음), INVALID_TOKEN, TOKEN_EXPIRED")
	})
	@PostMapping
	public ResponseEntity<ApiResponse<SearchExecutionResponse>> create(
			@AuthenticationPrincipal Long memberId, @Valid @RequestBody CreateLostItemRequest request) {
		SearchExecutionResponse response = initialSearchOrchestrator.createAndSearch(memberId, request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.header("Location", "/api/lost-items/" + response.lostItem().lostItemId())
				.body(ApiResponse.ok(response));
	}

	@Operation(summary = "API-06 내 분실물 목록", description = """
			삭제하지 않은 내 분실물을 최근 등록순으로 조회한다. page는 0부터, size는 기본 20·최대 50이다.
			status 필터는 저장된 상태로 거르고, 응답의 status는 만료 시각이 지난 TRACKING을 EXPIRED로 보여준다.""")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_REQUIRED(토큰 없음), INVALID_TOKEN, TOKEN_EXPIRED")
	})
	@GetMapping
	public ApiResponse<LostItemPageResponse> list(@AuthenticationPrincipal Long memberId,
			@Parameter(description = "상태 필터. 보내지 않으면 전체") @RequestParam(required = false) LostItemStatus status,
			@ParameterObject Pageable pageable) {
		return ApiResponse.ok(lostItemService.list(memberId, status, pageable));
	}

	@Operation(summary = "API-07 분실물 상세")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_REQUIRED(토큰 없음), INVALID_TOKEN, TOKEN_EXPIRED"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "RESOURCE_NOT_FOUND(없거나, 삭제됐거나, 다른 회원의 분실물)")
	})
	@GetMapping("/{lostItemId}")
	public ApiResponse<LostItemResponse> getDetail(@AuthenticationPrincipal Long memberId,
			@Parameter(description = "분실물 ID") @PathVariable Long lostItemId) {
		return ApiResponse.ok(lostItemService.getDetail(memberId, lostItemId));
	}

	@Operation(summary = "API-08 분실물 수정", description = """
			보낸 필드만 바꾼다. 단, conditions를 보내면 그 안의 8개 필드를 통째로 바꾸므로 빠진 필드는 null이 된다.
			notificationEmail은 TRACKING 상태에서만 바꿀 수 있다. EXPIRED 건은 수정할 수 없다.""")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
					description = "VALIDATION_ERROR, UNSUPPORTED_LANGUAGE, INVALID_SEARCH_CONDITION(분실 기간 시작일이 종료일보다 늦음)"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_REQUIRED(토큰 없음), INVALID_TOKEN, TOKEN_EXPIRED"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "RESOURCE_NOT_FOUND(없거나, 삭제됐거나, 다른 회원의 분실물)"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "TRACKING_EXPIRED, TRACKING_NOT_STARTED(추적 전 알림 이메일 수정)")
	})
	@PostMapping("/update/{lostItemId}")
	public ApiResponse<LostItemResponse> update(@AuthenticationPrincipal Long memberId,
			@Parameter(description = "분실물 ID") @PathVariable Long lostItemId,
			@Valid @RequestBody UpdateLostItemRequest request) {
		return ApiResponse.ok(lostItemService.update(memberId, lostItemId, request));
	}

	@Operation(summary = "API-09 분실물 삭제", description = "논리 삭제한다. 이미 삭제한 건에 다시 호출해도 204다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "204", description = "삭제됨(본문 없음)"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_REQUIRED(토큰 없음), INVALID_TOKEN, TOKEN_EXPIRED"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "RESOURCE_NOT_FOUND(없거나 다른 회원의 분실물)")
	})
	@PostMapping("/delete/{lostItemId}")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal Long memberId,
			@Parameter(description = "분실물 ID") @PathVariable Long lostItemId) {
		lostItemService.delete(memberId, lostItemId);
		return ResponseEntity.noContent().build();
	}
}
