package com.whereisit.backend.notification.controller;

import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.whereisit.backend.global.response.ApiResponse;
import com.whereisit.backend.notification.dto.NotificationPageResponse;
import com.whereisit.backend.notification.service.NotificationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/** API-15. */
@Tag(name = "알림", description = "추적 중 새 후보 이메일 알림 이력")
@RestController
@RequestMapping("/api/lost-items/{lostItemId}/notifications")
@RequiredArgsConstructor
public class NotificationController {

	private final NotificationService notificationService;

	@Operation(summary = "API-15 신규 후보 이메일 알림 이력", description = """
			추적 배치가 새 후보를 찾아 보낸 이메일 알림을 최근 생성순으로 준다. 하루에 한 건까지 만들어지며,
			status가 SENT여도 메일 제공자가 접수했다는 뜻이지 수신·열람을 뜻하지 않는다. 메일 본문은 주지 않는다.""")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_REQUIRED(토큰 없음), INVALID_TOKEN, TOKEN_EXPIRED"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "RESOURCE_NOT_FOUND(없거나, 삭제됐거나, 다른 회원의 분실물)")
	})
	@GetMapping
	public ApiResponse<NotificationPageResponse> list(@AuthenticationPrincipal Long memberId,
			@Parameter(description = "분실물 ID") @PathVariable Long lostItemId,
			@ParameterObject Pageable pageable) {
		return ApiResponse.ok(notificationService.list(memberId, lostItemId, pageable));
	}
}
