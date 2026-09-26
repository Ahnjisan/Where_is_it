package com.whereisit.backend.auth.error;

import org.springframework.http.HttpStatus;

import com.whereisit.backend.global.error.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AuthErrorCode implements ErrorCode {

	/** 없는 이메일과 틀린 비밀번호를 구분하지 않는다. */
	INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "Email or password is incorrect."),
	AUTH_REQUIRED(HttpStatus.UNAUTHORIZED, "Authentication is required."),
	INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "Access token is invalid."),
	TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Access token has expired."),
	/** RT가 없음·만료·무효(회전으로 이미 쓴 RT 포함)·형식 오류. 원인은 구분하지 않는다. */
	INVALID_REFRESH_TOKEN(HttpStatus.UNAUTHORIZED, "Refresh token is invalid.");

	private final HttpStatus status;
	private final String message;

	@Override
	public String getCode() {
		return name();
	}
}
