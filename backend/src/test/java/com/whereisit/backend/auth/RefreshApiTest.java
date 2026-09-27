package com.whereisit.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.whereisit.backend.auth.entity.RefreshToken;
import com.whereisit.backend.auth.jwt.TokenHasher;
import com.whereisit.backend.auth.repository.RefreshTokenRepository;
import com.whereisit.backend.support.ApiTestSupport;

@DisplayName("API-18 AT·RT 재발급")
class RefreshApiTest extends ApiTestSupport {

	private static final String EMAIL = "refresh@example.test";

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Test
	@DisplayName("TC-33 AT 없이 유효한 RT로 refresh하면 200, 새 AT·RT·member와 Cache-Control: no-store")
	void refresh() throws Exception {
		JsonNode login = signupAndLogin(EMAIL);
		String oldRefreshToken = login.get("refreshToken").asText();

		JsonNode data = dataOf(refresh(oldRefreshToken)
				.andExpect(status().isOk())
				.andExpect(header().string(HttpHeaders.CACHE_CONTROL, containsString("no-store")))
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.accessToken").isNotEmpty())
				.andExpect(jsonPath("$.data.tokenType").value("Bearer"))
				.andExpect(jsonPath("$.data.expiresIn").value(1800))
				.andExpect(jsonPath("$.data.member.email").value(EMAIL))
				.andExpect(jsonPath("$.data.member.passwordHash").doesNotExist()));

		String newRefreshToken = data.get("refreshToken").asText();
		assertThat(newRefreshToken).isNotEqualTo(oldRefreshToken);
		assertThat(hashesOf(memberIdOf(login))).containsExactly(TokenHasher.sha256Hex(newRefreshToken));
	}

	@Test
	@DisplayName("TC-33 새 RT의 만료는 refresh한 시점부터 다시 14일이다")
	void newRefreshTokenExpiresIn14DaysFromRefresh() throws Exception {
		JsonNode login = signupAndLogin(EMAIL);
		clock.advance(Duration.ofDays(3));

		refresh(login.get("refreshToken").asText()).andExpect(status().isOk());

		assertThat(tokensOf(memberIdOf(login)))
				.singleElement()
				// 가짜 시계 시작 2026-09-22 15:00 KST + 3일(refresh 시점) + 14일
				.satisfies(token -> assertThat(token.getExpiresAt()).hasToString("2026-10-09T15:00"));
	}

	@Test
	@DisplayName("TC-33 요청에 쓴 RT는 즉시 무효가 되어, 같은 RT로 다시 refresh하면 401 INVALID_REFRESH_TOKEN")
	void usedRefreshTokenIsRejected() throws Exception {
		String refreshToken = signupAndLogin(EMAIL).get("refreshToken").asText();
		refresh(refreshToken).andExpect(status().isOk());

		expectInvalidRefreshToken(refresh(refreshToken));
	}

	@Test
	@DisplayName("만료되었거나 잘못된 AT가 Authorization에 같이 와도 RT로 refresh된다")
	void ignoresAccessTokenHeader() throws Exception {
		String refreshToken = signupAndLogin(EMAIL).get("refreshToken").asText();

		mockMvc.perform(post("/api/auth/refresh")
						.header(HttpHeaders.AUTHORIZATION, "Bearer not-a-valid-access-token")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
	}

	@Test
	@DisplayName("TC-34 만료된 RT는 401 INVALID_REFRESH_TOKEN(만료 시각까지는 유효하고, 1초 지나면 만료)")
	void expiredRefreshToken() throws Exception {
		JsonNode login = signupAndLogin(EMAIL);
		String refreshToken = login.get("refreshToken").asText();
		String anotherRefreshToken = loginData(EMAIL).get("refreshToken").asText();

		// 만료 시각과 정확히 같은 순간은 아직 유효하다(JWT 검증·만료 RT 정리와 같은 기준).
		clock.advance(Duration.ofDays(14));
		refresh(anotherRefreshToken).andExpect(status().isOk());

		clock.advance(Duration.ofSeconds(1));
		expectInvalidRefreshToken(refresh(refreshToken));
	}

	@Test
	@DisplayName("TC-34 서명은 맞지만 DB에 없는 RT는 401 INVALID_REFRESH_TOKEN")
	void refreshTokenNotInDatabase() throws Exception {
		JsonNode login = signupAndLogin(EMAIL);
		refreshTokenRepository.deleteAll(tokensOf(memberIdOf(login)));

		expectInvalidRefreshToken(refresh(login.get("refreshToken").asText()));
	}

	@ParameterizedTest(name = "[{index}] {0}")
	@ValueSource(strings = {"not-a-jwt", "a.b.c", " "})
	@DisplayName("TC-34 JWT 형식이 아닌 RT는 401 INVALID_REFRESH_TOKEN")
	void malformedRefreshToken(String refreshToken) throws Exception {
		expectInvalidRefreshToken(refresh(refreshToken));
	}

	@Test
	@DisplayName("RT 자리에 AT를 넣으면 401 INVALID_REFRESH_TOKEN")
	void accessTokenInsteadOfRefreshToken() throws Exception {
		String accessToken = signupAndLogin(EMAIL).get("accessToken").asText();

		expectInvalidRefreshToken(refresh(accessToken));
	}

	@Test
	@DisplayName("TC-34 refreshToken이 없으면 400 VALIDATION_ERROR")
	void missingRefreshToken() throws Exception {
		postJson("/api/auth/refresh", Map.of())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.error.fields[0].field").value("refreshToken"));
	}

	@Test
	@DisplayName("refreshToken이 빈 문자열이면 400 VALIDATION_ERROR")
	void emptyRefreshToken() throws Exception {
		refresh("")
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.error.fields[0].field").value("refreshToken"));
	}

	@Test
	@DisplayName("TC-30 두 기기에서 로그인하면 각자의 RT로 refresh할 수 있고, 한 기기의 refresh는 다른 기기 RT를 건드리지 않는다")
	void refreshFromTwoDevices() throws Exception {
		JsonNode deviceA = signupAndLogin(EMAIL);
		String refreshTokenB = loginData(EMAIL).get("refreshToken").asText();

		String newRefreshTokenA = dataOf(refresh(deviceA.get("refreshToken").asText()).andExpect(status().isOk()))
				.get("refreshToken").asText();
		String newRefreshTokenB = dataOf(refresh(refreshTokenB).andExpect(status().isOk()))
				.get("refreshToken").asText();

		assertThat(hashesOf(memberIdOf(deviceA))).containsExactlyInAnyOrder(
				TokenHasher.sha256Hex(newRefreshTokenA), TokenHasher.sha256Hex(newRefreshTokenB));
	}

	@Test
	@DisplayName("TC-31 refresh할 때 이 회원의 만료된 RT만 지우고, 다른 회원의 RT는 그대로 둔다")
	void refreshDeletesOnlyOwnExpiredTokens() throws Exception {
		Long memberId = memberIdOf(signupAndLogin(EMAIL));
		Long otherMemberId = memberIdOf(signupAndLogin("other@example.test"));

		// 7일 뒤 로그인한 기기의 RT만 14일 1초 시점에 아직 유효하다.
		clock.advance(Duration.ofDays(7));
		String validRefreshToken = loginData(EMAIL).get("refreshToken").asText();
		clock.advance(Duration.ofDays(7).plusSeconds(1));

		String newRefreshToken = dataOf(refresh(validRefreshToken).andExpect(status().isOk()))
				.get("refreshToken").asText();

		assertThat(hashesOf(memberId)).containsExactly(TokenHasher.sha256Hex(newRefreshToken));
		assertThat(refreshTokenRepository.countByMemberId(otherMemberId)).isEqualTo(1);
	}

	private void expectInvalidRefreshToken(ResultActions result) throws Exception {
		result.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value("INVALID_REFRESH_TOKEN"))
				.andExpect(jsonPath("$.error.message").value("Refresh token is invalid."));
	}

	private JsonNode dataOf(ResultActions result) throws Exception {
		return objectMapper.readTree(result.andReturn().getResponse().getContentAsString()).get("data");
	}

	private static Long memberIdOf(JsonNode loginData) {
		return loginData.get("member").get("memberId").asLong();
	}

	/** 로컬 DB에 다른 데이터가 있을 수 있으므로 이 회원의 RT만 본다. */
	private List<RefreshToken> tokensOf(Long memberId) {
		return refreshTokenRepository.findAll().stream()
				.filter(token -> token.getMember().getId().equals(memberId))
				.toList();
	}

	private List<String> hashesOf(Long memberId) {
		return tokensOf(memberId).stream().map(RefreshToken::getTokenHash).toList();
	}
}
