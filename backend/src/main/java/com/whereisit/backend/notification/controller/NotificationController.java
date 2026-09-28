package com.whereisit.backend.notification.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.whereisit.backend.global.response.ApiResponse;
import com.whereisit.backend.notification.dto.NotificationPageResponse;
import com.whereisit.backend.notification.service.NotificationService;

import lombok.RequiredArgsConstructor;

/** API-15. */
@RestController
@RequestMapping("/api/lost-items/{lostItemId}/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService notificationService;

	@GetMapping
	public ApiResponse<NotificationPageResponse> list(@AuthenticationPrincipal Long memberId,
			@PathVariable Long lostItemId, Pageable pageable) {
		return ApiResponse.ok(notificationService.list(memberId, lostItemId, pageable));
	}
}
