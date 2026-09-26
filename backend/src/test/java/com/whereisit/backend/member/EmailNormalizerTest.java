package com.whereisit.backend.member;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Locale;
import java.util.stream.Stream;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

@DisplayName("이메일 정규화")
class EmailNormalizerTest {

	static Stream<Arguments> spacesAndCases() {
		return Stream.of(
				Arguments.of("ASCII 공백·탭·줄바꿈", " \t User@Example.TEST \n"),
				Arguments.of("NBSP(U+00A0)", " User@Example.TEST "),
				Arguments.of("전각 공백(U+3000)", "　User@Example.TEST　"),
				Arguments.of("좁은 NBSP(U+202F)·숫자 공백(U+2007)", " User@Example.TEST "),
				Arguments.of("여러 종류가 섞인 공백", "  　User@Example.TEST\t  "));
	}

	@ParameterizedTest(name = "{0}")
	@MethodSource("spacesAndCases")
	@DisplayName("앞뒤의 ASCII·유니코드 공백을 지우고 소문자로 바꾼다")
	void removesEdgeSpacesAndLowercases(String description, String input) {
		assertThat(EmailNormalizer.normalize(input)).isEqualTo("user@example.test");
	}

	@Test
	@DisplayName("가운데 공백은 지우지 않는다(형식 검증이 거부하도록 둔다)")
	void keepsInnerSpaces() {
		assertThat(EmailNormalizer.normalize(" a b@example.test ")).isEqualTo("a b@example.test");
	}

	@Test
	@DisplayName("기본 로캘이 터키어여도 I를 i로 바꾼다")
	void lowercasesIndependentOfDefaultLocale() {
		Locale original = Locale.getDefault();
		try {
			Locale.setDefault(Locale.forLanguageTag("tr"));
			assertThat(EmailNormalizer.normalize("TITLE@EXAMPLE.TEST")).isEqualTo("title@example.test");
		} finally {
			Locale.setDefault(original);
		}
	}

	@Test
	@DisplayName("null은 그대로 둔다")
	void keepsNull() {
		assertThat(EmailNormalizer.normalize(null)).isNull();
	}
}
