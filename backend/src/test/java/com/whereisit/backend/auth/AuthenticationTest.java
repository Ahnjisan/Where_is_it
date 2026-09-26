package com.whereisit.backend.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

import javax.crypto.SecretKey;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.whereisit.backend.auth.jwt.JwtProperties;
import com.whereisit.backend.support.ApiTestSupport;
import com.whereisit.backend.support.TestClockConfig;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.persistence.EntityManager;

/**
 * 보호 API(API-03 /api/members/me)로 AT 검증과 인증 오류 응답을 확인한다.
 */
@DisplayName("AT 인증과 API-03 내 회원정보 조회")
class AuthenticationTest extends ApiTestSupport {

	private static final String ME = "/api/members/me";
	private static final String EMAIL = "user@example.test";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private JwtProperties jwtProperties;

	private JsonNode loginData;

	@BeforeEach
	void login() throws Exception {
		loginData = signupAndLogin(EMAIL);
	}

	@Test
	@DisplayName("TC-32 유효한 AT로 조회하면 200과 Member(비밀번호 해시 없음)")
	void getMe() throws Exception {
		getMeWith("Bearer " + accessToken())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.memberId").value(loginData.get("member").get("memberId").asText()))
				.andExpect(jsonPath("$.data.email").value(EMAIL))
				.andExpect(jsonPath("$.data.languageCode").value("en"))
				.andExpect(jsonPath("$.data.createdAt").value("2026-09-22T15:00:00.000000+09:00"))
				.andExpect(jsonPath("$.data.passwordHash").doesNotExist());
	}

	@Test
	@DisplayName("TC-32 AT는 유효하지만 회원이 DB에서 삭제됐으면 401 INVALID_TOKEN")
	void memberDeleted() throws Exception {
		long memberId = loginData.get("member").get("memberId").asLong();
		jdbcTemplate.update("delete from refresh_tokens where member_id = ?", memberId);
		jdbcTemplate.update("delete from members where member_id = ?", memberId);
		// 같은 테스트 트랜잭션 안에서 이미 읽어 둔 회원을 다시 DB에서 찾게 한다.
		entityManager.clear();

		getMeWith("Bearer " + accessToken())
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
	}

	@Test
	@DisplayName("TC-04 토큰이 없으면 401 AUTH_REQUIRED와 WWW-Authenticate: Bearer, requestId")
	void noToken() throws Exception {
		mockMvc.perform(get(ME))
				.andExpect(status().isUnauthorized())
				.andExpect(header().string(HttpHeaders.WWW_AUTHENTICATE, "Bearer"))
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value("AUTH_REQUIRED"))
				.andExpect(jsonPath("$.error.fields").isEmpty())
				.andExpect(jsonPath("$.error.requestId").isNotEmpty());
	}

	@Test
	@DisplayName("TC-04 만료된 AT(30분 경과)는 401 TOKEN_EXPIRED")
	void expiredToken() throws Exception {
		clock.advance(Duration.ofMinutes(30).plusSeconds(1));

		getMeWith("Bearer " + accessToken())
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("TOKEN_EXPIRED"));
	}

	@Test
	@DisplayName("TC-04 30분이 되기 직전까지는 유효하다")
	void tokenValidUntilExpiry() throws Exception {
		clock.advance(Duration.ofMinutes(30).minusSeconds(1));

		getMeWith("Bearer " + accessToken()).andExpect(status().isOk());
	}

	@Test
	@DisplayName("TC-04 다른 키로 서명한 AT는 401 INVALID_TOKEN")
	void wrongSignature() throws Exception {
		SecretKey otherKey = Keys.hmacShaKeyFor("another-test-only-key-32-bytes!!".getBytes(StandardCharsets.UTF_8));
		String forged = Jwts.builder()
				.issuer("where-is-it")
				.subject(loginData.get("member").get("memberId").asText())
				.claim("token_type", "access")
				.issuedAt(Date.from(TestClockConfig.START))
				.expiration(Date.from(TestClockConfig.START.plus(Duration.ofMinutes(30))))
				.signWith(otherKey, Jwts.SIG.HS256)
				.compact();

		getMeWith("Bearer " + forged)
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
	}

	@Test
	@DisplayName("서명은 맞지만 token_type이 문자열이 아닌 토큰은 500이 아니라 401 INVALID_TOKEN")
	void nonStringTokenType() throws Exception {
		SecretKey serverKey = Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtProperties.secret()));
		String token = Jwts.builder()
				.issuer("where-is-it")
				.subject(loginData.get("member").get("memberId").asText())
				.claim("token_type", 1)
				.issuedAt(Date.from(TestClockConfig.START))
				.expiration(Date.from(TestClockConfig.START.plus(Duration.ofMinutes(30))))
				.signWith(serverKey, Jwts.SIG.HS256)
				.compact();

		getMeWith("Bearer " + token)
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
	}

	@Test
	@DisplayName("TC-04 형식이 잘못된 AT는 401 INVALID_TOKEN")
	void malformedToken() throws Exception {
		getMeWith("Bearer not-a-jwt")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
	}

	@Test
	@DisplayName("TC-04 RT를 AT 자리에 넣으면 401 INVALID_TOKEN")
	void refreshTokenAsAccessToken() throws Exception {
		getMeWith("Bearer " + loginData.get("refreshToken").asText())
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
	}

	@Test
	@DisplayName("Bearer가 아닌 인증 방식이면 401 INVALID_TOKEN")
	void notBearerScheme() throws Exception {
		getMeWith("Basic dXNlcjpwYXNz")
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
	}

	@Test
	@DisplayName("토큰 없이 없는 경로로 요청하면 404보다 인증 확인이 먼저라 401 AUTH_REQUIRED")
	void unknownPathWithoutToken() throws Exception {
		mockMvc.perform(get("/api/unknown"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("AUTH_REQUIRED"));
	}

	@Test
	@DisplayName("유효한 AT로 없는 경로를 요청하면 404 RESOURCE_NOT_FOUND")
	void unknownPathWithToken() throws Exception {
		mockMvc.perform(get("/api/unknown").header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken()))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
	}

	private String accessToken() {
		return loginData.get("accessToken").asText();
	}

	private ResultActions getMeWith(String authorization) throws Exception {
		return mockMvc.perform(get(ME).header(HttpHeaders.AUTHORIZATION, authorization));
	}
}
