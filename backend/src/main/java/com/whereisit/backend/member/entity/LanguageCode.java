package com.whereisit.backend.member.entity;

import java.util.Arrays;
import java.util.Optional;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 서버가 지원하는 사용 언어. API와 DB에는 소문자 코드(ko, en)로 주고받는다.
 * 언어를 늘릴 때는 여기에 상수를 추가한다(DB는 CHECK 없이 VARCHAR(35)).
 */
@Getter
@RequiredArgsConstructor
public enum LanguageCode {

	KO("ko"),
	EN("en");

	private final String code;

	/** 대소문자까지 정확히 같은 코드만 받는다. KO·Ko는 지원하지 않는 언어로 본다. */
	public static Optional<LanguageCode> fromCode(String code) {
		return Arrays.stream(values())
				.filter(language -> language.code.equals(code))
				.findFirst();
	}
}
