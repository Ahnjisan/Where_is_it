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

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** API-14 7일 추적 활성화. */
@RestController
@RequestMapping("/api/lost-items/{lostItemId}/tracking")
@RequiredArgsConstructor
public class TrackingController {

	private final TrackingActivationService trackingActivationService;

	@PostMapping
	public ApiResponse<LostItemResponse> activate(@AuthenticationPrincipal Long memberId,
			@PathVariable Long lostItemId, @Valid @RequestBody(required = false) ActivateTrackingRequest request) {
		String notificationEmail = request == null ? null : request.notificationEmail();
		return ApiResponse.ok(trackingActivationService.activate(memberId, lostItemId, notificationEmail));
	}
}
