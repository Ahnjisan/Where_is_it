package com.whereisit.backend.auth.dto;

import com.whereisit.backend.member.EmailNormalizer;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/**
 * API-02 로그인 요청.
 * 이메일 형식과 비밀번호 형식·길이는 검사하지 않는다. 틀리면 형식과 관계없이 INVALID_CREDENTIALS다(TC-03).
 * 빈 값만 400 VALIDATION_ERROR로 막는다.
 */
public record LoginRequest(
		@NotBlank @Size(max = 254) String email,
		@NotEmpty String password) {

	public LoginRequest {
		email = EmailNormalizer.normalize(email);
	}

	/** 비밀번호가 로그에 찍히지 않게 한다. */
	@Override
	public String toString() {
		return "LoginRequest[email=" + email + ", password=***]";
	}
}
