package com.whereisit.backend.global.error;

import org.springframework.http.HttpStatus;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 도메인에 속하지 않는 공통 오류 코드. 도메인 코드는 각 도메인 패키지의 enum에 둔다.
 */
@Getter
@RequiredArgsConstructor
public enum CommonErrorCode implements ErrorCode {

	VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "Request validation failed."),
	RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "Requested resource was not found."),
	METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "HTTP method is not allowed for this endpoint."),
	UNSUPPORTED_MEDIA_TYPE(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Content type is not supported."),
	INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error.");

	private final HttpStatus status;
	private final String message;

	@Override
	public String getCode() {
		return name();
	}
}
