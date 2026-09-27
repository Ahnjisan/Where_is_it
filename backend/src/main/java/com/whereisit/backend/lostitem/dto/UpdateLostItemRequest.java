package com.whereisit.backend.lostitem.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

/**
 * API 명세 v2 04_요청필드 API-08. 전송한 필드만 바꾼다.
 * description은 값을 보냈다면 공백만으로는 안 되는데(공백-only 거부), null 자체는 "안 바꿈"이라 허용해야 해서
 * 그 검사는 어노테이션 대신 LostItemService에서 한다.
 */
public record UpdateLostItemRequest(
		@Size(max = 2000) String description,
		@Size(min = 2, max = 35) String languageCode,
		@Valid SearchConditionsPatch conditions,
		@Email @Size(max = 254) String notificationEmail) {
}
