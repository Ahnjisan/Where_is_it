package com.whereisit.backend.global.error;

import java.util.List;

import org.slf4j.MDC;

import com.whereisit.backend.global.filter.RequestIdFilter;

/**
 * 공통 오류 응답. API 명세 02_공통규칙 R8:
 * {"success": false, "error": {"code", "message", "fields", "requestId"}}
 */
public record ErrorResponse(boolean success, Body error) {

	/** 401 응답의 WWW-Authenticate 값. 이 API의 인증 방식은 Bearer 토큰 하나다. */
	public static final String BEARER_CHALLENGE = "Bearer";

	public record Body(String code, String message, List<FieldError> fields, String requestId) {
	}

	/** reason은 환경마다 달라지지 않도록 제약 이름(NotBlank, Size 등) 같은 고정 값을 쓴다. */
	public record FieldError(String field, String reason) {
	}

	/**
	 * requestId는 현재 요청의 것을 {@link RequestIdFilter}가 MDC에 넣어 둔 값에서 읽는다.
	 * 예외 처리기·인증 EntryPoint·/error 컨트롤러가 모두 이 메서드로 응답을 만든다.
	 */
	public static ErrorResponse of(ErrorCode errorCode, List<FieldError> fields) {
		return new ErrorResponse(false, new Body(
				errorCode.getCode(),
				errorCode.getMessage(),
				fields == null ? List.of() : fields,
				MDC.get(RequestIdFilter.REQUEST_ID_KEY)));
	}
}
