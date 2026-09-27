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

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.RequiredArgsConstructor;

/**
 * API-10 분실물 대화 조회.
 * 명세 01_API목록의 URL(/api/lost-items/messages)에는 lostItemId가 없지만 04_요청필드는 path lostItemId를
 * 필수로 요구해 명세 내부 불일치가 있다. 다른 하위 리소스와 일관되게 /{lostItemId}/messages로 구현했다(PR 설명 참고).
 */
@RestController
@RequestMapping("/api/lost-items/{lostItemId}/messages")
@RequiredArgsConstructor
@Validated
public class ChatMessageController {

	private final ChatMessageService chatMessageService;

	@GetMapping
	public ApiResponse<MessagePageResponse> list(@AuthenticationPrincipal Long memberId, @PathVariable Long lostItemId,
			@RequestParam(required = false) Long afterMessageId,
			@RequestParam(defaultValue = "50") @Min(1) @Max(100) int limit) {
		return ApiResponse.ok(chatMessageService.list(memberId, lostItemId, afterMessageId, limit));
	}
}
