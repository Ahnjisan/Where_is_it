package com.whereisit.backend.auth.dto;

import com.whereisit.backend.member.dto.MemberResponse;

/**
 * API 명세 v2 05_응답필드 LoginData. 로그인과 refresh(#29)가 같은 형식을 쓴다.
 */
public record LoginResponse(
		String accessToken,
		String tokenType,
		long expiresIn,
		String refreshToken,
		MemberResponse member) {

	private static final String BEARER = "Bearer";

	public static LoginResponse of(String accessToken, long expiresIn, String refreshToken, MemberResponse member) {
		return new LoginResponse(accessToken, BEARER, expiresIn, refreshToken, member);
	}

	/** 토큰 원문이 로그에 찍히지 않게 한다. */
	@Override
	public String toString() {
		return "LoginResponse[accessToken=***, tokenType=" + tokenType + ", expiresIn=" + expiresIn
				+ ", refreshToken=***, member=" + member + "]";
	}
}
