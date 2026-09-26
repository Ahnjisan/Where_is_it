package com.whereisit.backend.member;

import java.util.Locale;
import java.util.regex.Pattern;

/**
 * 이메일 정규화 규칙(API 명세 v2 API-01·02): 앞뒤 공백을 지우고 소문자로 바꾼다.
 * 가입과 로그인이 같은 규칙을 써야 대소문자·공백만 다른 이메일을 같은 회원으로 본다.
 */
public final class EmailNormalizer {

	/**
	 * 앞뒤의 공백 문자. trim()과 strip()은 NBSP(U+00A0) 같은 유니코드 공백을 지우지 못하는데,
	 * 메일이나 웹 페이지에서 주소를 복사하면 이런 공백이 자주 따라온다.
	 * \p{Z}는 유니코드 공백 구분자(NBSP·전각 공백 포함), \s는 탭·줄바꿈 같은 ASCII 공백이다.
	 */
	private static final Pattern EDGE_SPACES = Pattern.compile("^[\\p{Z}\\s]+|[\\p{Z}\\s]+$");

	private EmailNormalizer() {
	}

	/** null은 그대로 둔다. 필수 여부는 요청 검증(@NotBlank)이 판단한다. */
	public static String normalize(String email) {
		if (email == null) {
			return null;
		}
		// 터키어 로캘 등에서 I가 다른 문자로 바뀌지 않도록 ROOT를 쓴다.
		return EDGE_SPACES.matcher(email).replaceAll("").toLowerCase(Locale.ROOT);
	}
}
