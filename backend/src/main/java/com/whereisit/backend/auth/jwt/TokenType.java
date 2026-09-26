package com.whereisit.backend.auth.jwt;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * AT와 RT가 같은 키로 서명되므로, 토큰 종류 클레임으로 둘을 구분한다.
 * AT 자리에 RT를 넣으면 이 값이 달라서 거부된다(TC-04).
 */
@Getter
@RequiredArgsConstructor
public enum TokenType {

	ACCESS("access"),
	REFRESH("refresh");

	public static final String CLAIM_NAME = "token_type";

	private final String claimValue;
}
