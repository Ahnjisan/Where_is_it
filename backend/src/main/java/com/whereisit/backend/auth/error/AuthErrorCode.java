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
	TOKEN_EXPIRED(HttpStatus.UNAUTHORIZED, "Access token has expired.");

	private final HttpStatus status;
	private final String message;

	@Override
	public String getCode() {
		return name();
	}
}
