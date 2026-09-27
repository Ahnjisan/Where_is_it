package com.whereisit.backend.lostitem.controller;

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
import com.whereisit.backend.lostitem.service.LostItemService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * API-05·06·07·08·09. URL은 API 명세 v2 02_공통규칙에 따라 조회는 GET, 쓰기는 POST만 쓴다(PUT·PATCH·DELETE 없음).
 */
@RestController
@RequestMapping("/api/lost-items")
@RequiredArgsConstructor
public class LostItemController {

	private final LostItemService lostItemService;

	@PostMapping
	public ResponseEntity<ApiResponse<LostItemResponse>> create(
			@AuthenticationPrincipal Long memberId, @Valid @RequestBody CreateLostItemRequest request) {
		LostItemResponse response = lostItemService.create(memberId, request);
		return ResponseEntity.status(HttpStatus.CREATED)
				.header("Location", "/api/lost-items/" + response.lostItemId())
				.body(ApiResponse.ok(response));
	}

	@GetMapping
	public ApiResponse<LostItemPageResponse> list(@AuthenticationPrincipal Long memberId,
			@RequestParam(required = false) LostItemStatus status, Pageable pageable) {
		return ApiResponse.ok(lostItemService.list(memberId, status, pageable));
	}

	@GetMapping("/{lostItemId}")
	public ApiResponse<LostItemResponse> getDetail(@AuthenticationPrincipal Long memberId, @PathVariable Long lostItemId) {
		return ApiResponse.ok(lostItemService.getDetail(memberId, lostItemId));
	}

	@PostMapping("/update/{lostItemId}")
	public ApiResponse<LostItemResponse> update(@AuthenticationPrincipal Long memberId, @PathVariable Long lostItemId,
			@Valid @RequestBody UpdateLostItemRequest request) {
		return ApiResponse.ok(lostItemService.update(memberId, lostItemId, request));
	}

	@PostMapping("/delete/{lostItemId}")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal Long memberId, @PathVariable Long lostItemId) {
		lostItemService.delete(memberId, lostItemId);
		return ResponseEntity.noContent().build();
	}
}
