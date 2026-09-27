package com.whereisit.backend.lostitem.dto;

import java.util.List;

/** API 명세 v2 05_응답필드 MessagePage. */
public record MessagePageResponse(List<ChatMessageResponse> items, String nextAfterMessageId, boolean hasMore) {
}
