package com.whereisit.backend.auth.dto;

import com.whereisit.backend.member.EmailNormalizer;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * API-01 회원가입 요청(API 명세 v2 04_요청필드).
 *
 * @param email        trim+소문자로 정규화한 뒤 검증한다. 그래야 " User@Example.com "도 중복 검사까지 간다(TC-02).
 * @param password     영문·숫자·ASCII 특수문자 8~20자, 공백 불가. 조합 규칙은 없다.
 * @param languageCode 문자열로 받고 허용값(ko, en)은 서비스에서 확인한다.
 *                     enum으로 받으면 ja·KO가 VALIDATION_ERROR가 되지만, 명세는 UNSUPPORTED_LANGUAGE다(TC-29).
 */
public record SignupRequest(
		@NotBlank @Email @Size(max = 254) String email,
		@NotNull @Pattern(regexp = "^[!-~]{8,20}$") String password,
		@NotBlank String languageCode) {

	public SignupRequest {
		email = EmailNormalizer.normalize(email);
	}

	/** 비밀번호가 로그에 찍히지 않게 한다. */
	@Override
	public String toString() {
		return "SignupRequest[email=" + email + ", password=***, languageCode=" + languageCode + "]";
	}
}
