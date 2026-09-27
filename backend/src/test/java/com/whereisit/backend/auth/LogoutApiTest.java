package com.whereisit.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.whereisit.backend.auth.entity.RefreshToken;
import com.whereisit.backend.auth.jwt.TokenHasher;
import com.whereisit.backend.auth.repository.RefreshTokenRepository;
import com.whereisit.backend.support.ApiTestSupport;

@DisplayName("API-19 로그아웃")
class LogoutApiTest extends ApiTestSupport {

	private static final String EMAIL = "logout@example.test";

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Test
	@DisplayName("TC-36 AT 없이 유효한 RT로 로그아웃하면 200(data: null)이고, 그 RT로는 refresh할 수 없다")
	void logout() throws Exception {
		String refreshToken = signupAndLogin(EMAIL).get("refreshToken").asText();

		expectLoggedOut(logout(refreshToken));

		refresh(refreshToken)
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"));
	}

	@Test
	@DisplayName("TC-36 요청한 기기의 RT만 지우고, 같은 회원의 다른 기기 RT는 계속 유효하다")
	void logoutOnlyThisDevice() throws Exception {
		JsonNode deviceA = signupAndLogin(EMAIL);
		String refreshTokenB = loginData(EMAIL).get("refreshToken").asText();

		expectLoggedOut(logout(deviceA.get("refreshToken").asText()));

		assertThat(hashesOf(memberIdOf(deviceA))).containsExactly(TokenHasher.sha256Hex(refreshTokenB));
		refresh(refreshTokenB).andExpect(status().isOk());
	}

	@Test
	@DisplayName("TC-36 로그아웃해도 이미 발급된 AT는 만료 전까지 쓸 수 있다(명세상 한계)")
	void accessTokenRemainsValidAfterLogout() throws Exception {
		JsonNode login = signupAndLogin(EMAIL);

		expectLoggedOut(logout(login.get("refreshToken").asText()));

		mockMvc.perform(get("/api/members/me")
						.header(HttpHeaders.AUTHORIZATION, "Bearer " + login.get("accessToken").asText()))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.email").value(EMAIL));
	}

	@Test
	@DisplayName("TC-37 이미 로그아웃한 RT로 다시 로그아웃해도 200(멱등)")
	void logoutTwice() throws Exception {
		String refreshToken = signupAndLogin(EMAIL).get("refreshToken").asText();

		expectLoggedOut(logout(refreshToken));
		expectLoggedOut(logout(refreshToken));
	}

	@Test
	@DisplayName("TC-37 만료된 RT로 로그아웃해도 200이고, 남아 있던 그 RT 행은 지운다")
	void logoutWithExpiredRefreshToken() throws Exception {
		JsonNode login = signupAndLogin(EMAIL);
		clock.advance(Duration.ofDays(14).plusSeconds(1));

		expectLoggedOut(logout(login.get("refreshToken").asText()));

		assertThat(hashesOf(memberIdOf(login))).isEmpty();
	}

	@ParameterizedTest(name = "[{index}] {0}")
	@ValueSource(strings = {"not-a-jwt", "a.b.c", " "})
	@DisplayName("TC-37 DB에 없는 RT(형식 오류 포함)로 로그아웃해도 200이고, 다른 RT는 지우지 않는다")
	void logoutWithUnknownRefreshToken(String refreshToken) throws Exception {
		JsonNode login = signupAndLogin(EMAIL);

		expectLoggedOut(logout(refreshToken));

		assertThat(hashesOf(memberIdOf(login))).containsExactly(
				TokenHasher.sha256Hex(login.get("refreshToken").asText()));
	}

	@Test
	@DisplayName("TC-37 refreshToken이 없으면 400 VALIDATION_ERROR")
	void missingRefreshToken() throws Exception {
		postJson("/api/auth/logout", Map.of())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.error.fields[0].field").value("refreshToken"));
	}

	@Test
	@DisplayName("refreshToken이 빈 문자열이면 400 VALIDATION_ERROR")
	void emptyRefreshToken() throws Exception {
		logout("")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.error.fields[0].field").value("refreshToken"));
	}

	private static void expectLoggedOut(ResultActions result) throws Exception {
		result.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").value(nullValue()));
	}

	private static Long memberIdOf(JsonNode loginData) {
		return loginData.get("member").get("memberId").asLong();
	}

	/** 로컬 DB에 다른 데이터가 있을 수 있으므로 이 회원의 RT 해시만 본다. */
	private List<String> hashesOf(Long memberId) {
		return refreshTokenRepository.findAll().stream()
				.filter(token -> token.getMember().getId().equals(memberId))
				.map(RefreshToken::getTokenHash)
				.toList();
	}
}
