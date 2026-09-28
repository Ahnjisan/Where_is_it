package com.whereisit.backend.notification.dto;

import java.util.List;

/** API 명세 v2 05_응답필드 NotificationPage. */
public record NotificationPageResponse(
		List<NotificationResponse> items, int page, int size, long totalElements, int totalPages) {
}
