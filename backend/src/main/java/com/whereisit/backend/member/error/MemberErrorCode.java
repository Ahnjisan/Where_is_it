package com.whereisit.backend.member.error;

import org.springframework.http.HttpStatus;

import com.whereisit.backend.global.error.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum MemberErrorCode implements ErrorCode {

	EMAIL_ALREADY_EXISTS(HttpStatus.CONFLICT, "Email is already registered.");

	private final HttpStatus status;
	private final String message;

	@Override
	public String getCode() {
		return name();
	}
}
