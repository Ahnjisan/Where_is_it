package com.whereisit.backend.member;

import java.util.Locale;

/**
 * 이메일 정규화 규칙(API 명세 v2 API-01·02): 앞뒤 공백을 지우고 소문자로 바꾼다.
 * 가입과 로그인이 같은 규칙을 써야 대소문자·공백만 다른 이메일을 같은 회원으로 본다.
 */
public final class EmailNormalizer {

	private EmailNormalizer() {
	}

	/** null은 그대로 둔다. 필수 여부는 요청 검증(@NotBlank)이 판단한다. */
	public static String normalize(String email) {
		// 터키어 로캘 등에서 I가 다른 문자로 바뀌지 않도록 ROOT를 쓴다.
		return email == null ? null : email.trim().toLowerCase(Locale.ROOT);
	}
}
