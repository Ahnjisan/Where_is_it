package com.whereisit.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.whereisit.backend.member.repository.MemberRepository;
import com.whereisit.backend.support.ApiTestSupport;

@DisplayName("API-01 회원가입")
class SignupApiTest extends ApiTestSupport {

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private PasswordEncoder passwordEncoder;

	@Test
	@DisplayName("TC-01 유효한 요청이면 201과 Member를 주고, 이메일은 trim+소문자로 저장한다")
	void signup() throws Exception {
		signup("  New.User@Example.TEST ", "Password1!", "ko")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.memberId").isString())
				.andExpect(jsonPath("$.data.email").value("new.user@example.test"))
				.andExpect(jsonPath("$.data.languageCode").value("ko"))
				.andExpect(jsonPath("$.data.createdAt").value("2026-09-22T15:00:00.000000+09:00"))
				.andExpect(jsonPath("$.data.password").doesNotExist())
				.andExpect(jsonPath("$.data.passwordHash").doesNotExist())
				.andExpect(jsonPath("$.data.accessToken").doesNotExist())
				.andExpect(jsonPath("$.data.refreshToken").doesNotExist());

		Map<String, Object> row = jdbcTemplate.queryForMap(
				"select email, password_hash, language_code from members where email = ?", "new.user@example.test");
		assertThat(row.get("language_code")).isEqualTo("ko");
		assertThat((String) row.get("password_hash"))
				.isNotEqualTo("Password1!")
				.satisfies(hash -> assertThat(passwordEncoder.matches("Password1!", hash)).isTrue());
	}

	@Test
	@DisplayName("TC-02 대소문자·앞뒤 공백만 다른 이메일로 다시 가입하면 409 EMAIL_ALREADY_EXISTS")
	void duplicateEmail() throws Exception {
		signup("user@example.test", PASSWORD, "en").andExpect(status().isCreated());

		signup(" User@Example.test ", PASSWORD, "en")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_EXISTS"));
	}

	@ParameterizedTest(name = "[{index}] \"{0}\"")
	@ValueSource(strings = { "Passw1!", "Password1!Password1!x", "Pass word1!", "비밀번호비밀번호1!", "" })
	@DisplayName("TC-29 비밀번호가 7자·21자·공백·한글·빈 값이면 400 VALIDATION_ERROR이고 저장하지 않는다")
	void invalidPassword(String password) throws Exception {
		signup("user@example.test", password, "en")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("password")));

		assertThat(memberRepository.existsByEmail("user@example.test")).isFalse();
	}

	@ParameterizedTest(name = "[{index}] {0}자")
	@ValueSource(strings = { "Passwd1!", "Password1!Password1!" })
	@DisplayName("TC-29 비밀번호 경계값 8자·20자는 201")
	void passwordBoundary(String password) throws Exception {
		signup("user@example.test", password, "en").andExpect(status().isCreated());
	}

	@ParameterizedTest(name = "[{index}] {0}")
	@ValueSource(strings = { "ja", "KO" })
	@DisplayName("TC-29 지원하지 않는 languageCode(ja, 대문자 KO)는 400 UNSUPPORTED_LANGUAGE이고 저장하지 않는다")
	void unsupportedLanguage(String languageCode) throws Exception {
		signup("user@example.test", PASSWORD, languageCode)
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("UNSUPPORTED_LANGUAGE"));

		assertThat(memberRepository.existsByEmail("user@example.test")).isFalse();
	}

	@Test
	@DisplayName("TC-29 languageCode가 빈 값이면 400 VALIDATION_ERROR")
	void blankLanguage() throws Exception {
		signup("user@example.test", PASSWORD, "")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("languageCode")));
	}

	@Test
	@DisplayName("요청에 memberId처럼 정의되지 않은 필드가 있으면 400 VALIDATION_ERROR(UnknownField)")
	void unknownField() throws Exception {
		postJson("/api/auth/signup", Map.of(
				"email", "user@example.test", "password", PASSWORD, "languageCode", "en", "memberId", "1"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.error.fields[0].field").value("memberId"))
				.andExpect(jsonPath("$.error.fields[0].reason").value("UnknownField"));
	}

	@Test
	@DisplayName("공개 경로는 Authorization 헤더가 잘못돼 있어도 처리된다")
	void publicPathIgnoresBrokenToken() throws Exception {
		mockMvc.perform(post("/api/auth/signup")
				.header(HttpHeaders.AUTHORIZATION, "Bearer broken")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(
						Map.of("email", "user@example.test", "password", PASSWORD, "languageCode", "en"))))
				.andExpect(status().isCreated());
	}
}
