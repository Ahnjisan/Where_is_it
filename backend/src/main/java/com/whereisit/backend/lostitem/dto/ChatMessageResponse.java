package com.whereisit.backend.lostitem.dto;

import java.time.LocalDateTime;

import com.whereisit.backend.lostitem.entity.ChatMessage;

/** API 명세 v2 05_응답필드 ChatMessage. */
public record ChatMessageResponse(String messageId, String role, String content, LocalDateTime createdAt) {

	public static ChatMessageResponse from(ChatMessage message) {
		return new ChatMessageResponse(
				String.valueOf(message.getId()), message.getRole().name(), message.getContent(), message.getCreatedAt());
	}
}
