package com.whereisit.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;

import com.fasterxml.jackson.databind.JsonNode;
import com.whereisit.backend.auth.entity.RefreshToken;
import com.whereisit.backend.auth.jwt.TokenHasher;
import com.whereisit.backend.auth.repository.RefreshTokenRepository;
import com.whereisit.backend.support.ApiTestSupport;

@DisplayName("API-02 로그인")
class LoginApiTest extends ApiTestSupport {

	private static final String EMAIL = "user@example.test";

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@BeforeEach
	void createMember() throws Exception {
		signup(EMAIL, PASSWORD, "ko").andExpect(status().isCreated());
	}

	@Test
	@DisplayName("TC-03 정상 로그인은 200과 LoginData, Cache-Control: no-store")
	void login() throws Exception {
		login(EMAIL, PASSWORD)
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
				.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.data.expiresIn").value(1800))
				.andExpect(jsonPath("$.data.member.email").value(EMAIL))
				.andExpect(jsonPath("$.data.member.languageCode").value("ko"))
				.andExpect(jsonPath("$.data.member.passwordHash").doesNotExist());
	}

	@Test
	@DisplayName("TC-03 이메일의 대소문자·앞뒤 공백이 달라도 로그인된다")
	void loginWithDifferentCaseEmail() throws Exception {
		login("  USER@Example.test ", PASSWORD)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.member.email").value(EMAIL));
	}

	@Test
	@DisplayName("이메일 앞뒤에 NBSP·전각 공백이 붙어 있어도 로그인된다")
	void loginWithUnicodeSpaces() throws Exception {
		login("　User@Example.test ", PASSWORD)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.member.email").value(EMAIL));
	}

	@ParameterizedTest(name = "[{index}] {2}")
	@CsvSource({
			"user@example.test, WrongPass1!, 틀린 비밀번호",
			"nobody@example.test, Password1!, 없는 이메일",
			"user@example.test, Pass1!x, 가입 규칙보다 짧은 비밀번호(7자)",
			"user@example.test, 비밀번호입니다, 한글 비밀번호",
			"not-an-email, Password1!, 이메일 형식이 아님"
	})
	@DisplayName("TC-03 틀린 비밀번호·없는 이메일·규칙 밖 비밀번호는 모두 같은 401 INVALID_CREDENTIALS와 WWW-Authenticate")
	void invalidCredentials(String email, String password, String description) throws Exception {
		login(email, password)
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
				.andExpect(jsonPath("$.error.code").value("INVALID_CREDENTIALS"))
				.andExpect(jsonPath("$.error.message").value("Email or password is incorrect."));
	}

	@Test
	@DisplayName("TC-03 비밀번호가 빈 값이면 400 VALIDATION_ERROR")
	void emptyPassword() throws Exception {
		login(EMAIL, "")
				.andExpect(status().isBadRequest())
				.andExpect(header().doesNotExist(HttpHeaders.WWW_AUTHENTICATE))
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.error.fields[0].field").value("password"));
	}

	@Test
	@DisplayName("TC-30 같은 계정으로 두 기기에서 로그인하면 RT 2개가 해시로 저장된다")
	void loginFromTwoDevices() throws Exception {
		// 같은 시각(가짜 시계가 멈춰 있음)에 두 번 발급해도 토큰이 달라야 한다.
		JsonNode deviceA = loginData(EMAIL);
		JsonNode deviceB = loginData(EMAIL);
		String refreshTokenA = deviceA.get("refreshToken").asText();
		String refreshTokenB = deviceB.get("refreshToken").asText();
		assertThat(refreshTokenA).isNotEqualTo(refreshTokenB);

		Long memberId = deviceA.get("member").get("memberId").asLong();
		List<String> storedHashes = refreshTokenRepository.findAll().stream()
				.filter(token -> token.getMember().getId().equals(memberId))
				.map(RefreshToken::getTokenHash)
				.toList();
		assertThat(storedHashes).containsExactlyInAnyOrder(
				TokenHasher.sha256Hex(refreshTokenA), TokenHasher.sha256Hex(refreshTokenB));
		assertThat(storedHashes).doesNotContain(refreshTokenA, refreshTokenB);
	}

	@Test
	@DisplayName("RT 만료 시각은 발급 시각 + 14일이다")
	void refreshTokenExpiresIn14Days() throws Exception {
		Long memberId = loginData(EMAIL).get("member").get("memberId").asLong();

		// 로컬 DB에 다른 데이터가 있을 수 있으므로 이 회원의 RT만 본다.
		assertThat(refreshTokenRepository.findAll())
				.filteredOn(token -> token.getMember().getId().equals(memberId))
				.singleElement()
				// 가짜 시계 시작 시각 2026-09-22 15:00 KST + 14일
				.satisfies(token -> assertThat(token.getExpiresAt()).hasToString("2026-10-06T15:00"));
	}

	@Test
	@DisplayName("TC-31 로그인할 때 이 회원의 만료된 RT만 지우고, 다른 회원의 RT는 그대로 둔다")
	void loginDeletesOnlyOwnExpiredTokens() throws Exception {
		Long memberId = loginData(EMAIL).get("member").get("memberId").asLong();
		Long otherMemberId = signupAndLogin("other@example.test").get("member").get("memberId").asLong();

		clock.advance(Duration.ofDays(14).plusSeconds(1));
		String newRefreshToken = loginData(EMAIL).get("refreshToken").asText();

		assertThat(refreshTokenRepository.countByMemberId(memberId)).isEqualTo(1);
		assertThat(refreshTokenRepository.findAll())
				.filteredOn(token -> token.getMember().getId().equals(memberId))
				.extracting(RefreshToken::getTokenHash)
				.containsExactly(TokenHasher.sha256Hex(newRefreshToken));
		assertThat(refreshTokenRepository.countByMemberId(otherMemberId)).isEqualTo(1);
	}
}
