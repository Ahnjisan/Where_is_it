package com.whereisit.backend.auth.jwt;

import java.time.LocalDateTime;

/**
 * 발급한 토큰 원문과 만료 시각(KST). 원문은 응답으로만 내보내고 로그에 남기지 않는다.
 */
public record IssuedToken(String value, LocalDateTime expiresAt) {

	/** 실수로 로그에 찍혀도 원문이 드러나지 않게 한다. */
	@Override
	public String toString() {
		return "IssuedToken[value=***, expiresAt=" + expiresAt + "]";
	}
}
