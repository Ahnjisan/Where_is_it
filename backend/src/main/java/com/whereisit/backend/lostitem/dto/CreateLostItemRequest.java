package com.whereisit.backend.lostitem.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * API 명세 v2 04_요청필드 API-05. languageCode 미전송 시 회원 언어를 쓴다.
 */
public record CreateLostItemRequest(
		@NotBlank @Size(max = 2000) String description,
		@Size(min = 2, max = 35) String languageCode) {
}
