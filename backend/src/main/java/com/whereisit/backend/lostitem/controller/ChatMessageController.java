package com.whereisit.backend.lostitem.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.whereisit.backend.global.response.ApiResponse;
import com.whereisit.backend.lostitem.dto.MessagePageResponse;
import com.whereisit.backend.lostitem.service.ChatMessageService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * API-10 분실물 대화 조회.
 * 명세 01_API목록의 URL(/api/lost-items/messages)에는 lostItemId가 없지만 04_요청필드는 path lostItemId를
 * 필수로 요구해 명세 내부 불일치가 있다. 다른 하위 리소스와 일관되게 /{lostItemId}/messages로 구현했다(PR 설명 참고).
 */
@Tag(name = "대화", description = "분실물별 USER·ASSISTANT 대화 기록")
@RestController
@RequestMapping("/api/lost-items/{lostItemId}/messages")
@RequiredArgsConstructor
@Validated
public class ChatMessageController {

	private final ChatMessageService chatMessageService;

	@Operation(summary = "API-10 분실물 대화 조회", description = """
			오래된 메시지부터 limit건을 준다. 다음 페이지는 응답의 nextAfterMessageId를 afterMessageId로 보내 이어서 받는다.""")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "VALIDATION_ERROR(limit 범위 밖)"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_REQUIRED(토큰 없음), INVALID_TOKEN, TOKEN_EXPIRED"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "RESOURCE_NOT_FOUND(없거나, 삭제됐거나, 다른 회원의 분실물)")
	})
	@GetMapping
	public ApiResponse<MessagePageResponse> list(@AuthenticationPrincipal Long memberId,
			@Parameter(description = "분실물 ID") @PathVariable Long lostItemId,
			@Parameter(description = "이 메시지 ID 다음부터 조회. 보내지 않으면 처음부터") @RequestParam(required = false) Long afterMessageId,
			@Parameter(description = "가져올 개수(1~100)") @RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
		return ApiResponse.ok(chatMessageService.list(memberId, lostItemId, afterMessageId, limit));
	}
}
