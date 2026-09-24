package com.whereisit.backend.global.error;

import java.util.List;

/**
 * 공통 오류 응답. API 명세 02_공통규칙 R8:
 * {"success": false, "error": {"code", "message", "fields", "requestId"}}
 */
public record ErrorResponse(boolean success, Body error) {

	public record Body(String code, String message, List<FieldError> fields, String requestId) {
	}

	/** reason은 환경마다 달라지지 않도록 제약 이름(NotBlank, Size 등) 같은 고정 값을 쓴다. */
	public record FieldError(String field, String reason) {
	}

	public static ErrorResponse of(ErrorCode errorCode, List<FieldError> fields, String requestId) {
		return new ErrorResponse(false, new Body(
				errorCode.getCode(),
				errorCode.getMessage(),
				fields == null ? List.of() : fields,
				requestId));
	}
}
