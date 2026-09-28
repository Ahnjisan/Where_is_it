package com.whereisit.backend.candidate.ranking.initial;

import java.text.Normalizer;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 규칙 기반 후보 비교용 문자열 projection. 원본 문자열과 DB 값은 바꾸지 않고 비교할 때만 사용한다.
 * NFKC 정규화 → 소문자화 → 문자·숫자·결합 부호가 아닌 공백·구두점·기호를 단일 공백으로 치환한다.
 */
public final class CandidateTextNormalizer {

	private static final Pattern SEPARATORS = Pattern.compile("[^\\p{IsAlphabetic}\\p{Digit}\\p{M}]+");

	private CandidateTextNormalizer() {
	}

	/** 공백 하나로 구분된 비교용 문자열. null·blank는 빈 문자열이다. */
	public static String normalize(String value) {
		if (value == null || value.isBlank()) {
			return "";
		}
		String normalized = Normalizer.normalize(value, Normalizer.Form.NFKC).toLowerCase(Locale.ROOT);
		return SEPARATORS.matcher(normalized).replaceAll(" ").trim();
	}

	/** 공백·구두점 차이를 없앤 비교용 문자열. 예: "검은색 지갑"과 "검은색-지갑"은 모두 "검은색지갑"이다. */
	public static String compact(String value) {
		return normalize(value).replace(" ", "");
	}

	/** 입력 순서를 유지한 중복 없는 비교용 token. */
	public static Set<String> tokens(String value) {
		String normalized = normalize(value);
		Set<String> tokens = new LinkedHashSet<>();
		if (normalized.isEmpty()) {
			return tokens;
		}
		for (String token : normalized.split(" ")) {
			if (!token.isEmpty()) {
				tokens.add(token);
			}
		}
		return tokens;
	}

	static int length(String value) {
		return value.codePointCount(0, value.length());
	}
}
