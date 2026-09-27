package com.whereisit.backend.auth.dto;

import jakarta.validation.constraints.NotEmpty;

/**
 * API-18 refresh·API-19 로그아웃 요청. 둘 다 AT 대신 이 RT로 인증한다.
 * 빈 값만 400 VALIDATION_ERROR로 막는다(04_요청필드: 최소길이 1).
 */
public record RefreshTokenRequest(@NotEmpty String refreshToken) {

	/** RT 원문이 로그에 찍히지 않게 한다. */
	@Override
	public String toString() {
		return "RefreshTokenRequest[refreshToken=***]";
	}
}
