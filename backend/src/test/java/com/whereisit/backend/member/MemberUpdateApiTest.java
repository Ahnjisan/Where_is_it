package com.whereisit.backend.member;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.JsonNode;
import com.whereisit.backend.lostitem.entity.LostItem;
import com.whereisit.backend.lostitem.repository.LostItemRepository;
import com.whereisit.backend.member.entity.LanguageCode;
import com.whereisit.backend.member.entity.Member;
import com.whereisit.backend.member.repository.MemberRepository;
import com.whereisit.backend.support.ApiTestSupport;

import jakarta.persistence.EntityManager;

/**
 * API-20 내 회원정보 수정. 가입 언어는 en이고, 지금은 사용 언어만 바꿀 수 있다.
 */
@DisplayName("API-20 내 회원정보 수정")
class MemberUpdateApiTest extends ApiTestSupport {

	private static final String ME = "/api/members/me";
	private static final String EMAIL = "user@example.test";

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private EntityManager entityManager;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private LostItemRepository lostItemRepository;

	private JsonNode loginData;

	@BeforeEach
	void login() throws Exception {
		loginData = signupAndLogin(EMAIL);
	}

	@Test
	@DisplayName("사용 언어를 바꾸면 200과 바뀐 Member를 주고, DB와 API-03 조회에도 반영된다")
	void updateLanguage() throws Exception {
		updateMe(Map.of("languageCode", "ko"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.memberId").value(memberId()))
				.andExpect(jsonPath("$.data.email").value(EMAIL))
				.andExpect(jsonPath("$.data.languageCode").value("ko"))
				.andExpect(jsonPath("$.data.createdAt").value("2026-09-22T15:00:00.000000+09:00"))
				.andExpect(jsonPath("$.data.passwordHash").doesNotExist());

		entityManager.flush();
		assertThat(storedLanguageCode()).isEqualTo("ko");

		authorizedGet(ME, accessToken())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.languageCode").value("ko"));
	}

	@Test
	@DisplayName("지금과 같은 언어로 다시 요청해도 200이다")
	void sameLanguage() throws Exception {
		updateMe(Map.of("languageCode", "en"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.languageCode").value("en"));
	}

	@Test
	@DisplayName("언어를 바꿔도 이미 등록한 분실물의 언어는 그대로다")
	void existingLostItemKeepsLanguage() throws Exception {
		Member member = memberRepository.findById(Long.valueOf(memberId())).orElseThrow();
		LostItem lostItem = lostItemRepository.save(LostItem.create(member, "lost blue wallet", LanguageCode.EN));

		updateMe(Map.of("languageCode", "ko")).andExpect(status().isOk());
		entityManager.flush();
		entityManager.clear();

		assertThat(lostItemRepository.findById(lostItem.getId()).orElseThrow().getLanguageCode())
				.isEqualTo(LanguageCode.EN);
	}

	@ParameterizedTest(name = "[{index}] \"{0}\"")
	@ValueSource(strings = { "ja", "KO", "en-US", " en" })
	@DisplayName("지원하지 않는 languageCode(ja, 대문자 KO, en-US, 앞 공백)는 400 UNSUPPORTED_LANGUAGE이고 바뀌지 않는다")
	void unsupportedLanguage(String languageCode) throws Exception {
		updateMe(Map.of("languageCode", languageCode))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("UNSUPPORTED_LANGUAGE"));

		assertThat(storedLanguageCode()).isEqualTo("en");
	}

	@ParameterizedTest(name = "[{index}] \"{0}\"")
	@ValueSource(strings = { "", "   " })
	@DisplayName("languageCode가 빈 값·공백이면 400 VALIDATION_ERROR")
	void blankLanguage(String languageCode) throws Exception {
		updateMe(Map.of("languageCode", languageCode))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("languageCode")));
	}

	@Test
	@DisplayName("languageCode가 없으면 400 VALIDATION_ERROR")
	void missingLanguage() throws Exception {
		updateMe(Map.of())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("languageCode")));
	}

	@Test
	@DisplayName("languageCode가 null이면 400 VALIDATION_ERROR")
	void nullLanguage() throws Exception {
		mockMvc.perform(post(ME)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken())
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"languageCode\":null}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItem("languageCode")));
	}

	@Test
	@DisplayName("언어 외 필드(email)를 보내면 400 VALIDATION_ERROR(UnknownField)이고 아무것도 바뀌지 않는다")
	void otherFieldNotAllowed() throws Exception {
		updateMe(Map.of("languageCode", "ko", "email", "other@example.test"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.error.fields[0].field").value("email"))
				.andExpect(jsonPath("$.error.fields[0].reason").value("UnknownField"));

		assertThat(storedLanguageCode()).isEqualTo("en");
	}

	@Test
	@DisplayName("토큰이 없으면 401 AUTH_REQUIRED")
	void noToken() throws Exception {
		postJson(ME, Map.of("languageCode", "ko"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("AUTH_REQUIRED"));

		assertThat(storedLanguageCode()).isEqualTo("en");
	}

	@Test
	@DisplayName("만료된 AT(30분 경과)는 401 TOKEN_EXPIRED")
	void expiredToken() throws Exception {
		clock.advance(Duration.ofMinutes(30).plusSeconds(1));

		updateMe(Map.of("languageCode", "ko"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("TOKEN_EXPIRED"));
	}

	@Test
	@DisplayName("RT를 AT 자리에 넣으면 401 INVALID_TOKEN")
	void refreshTokenAsAccessToken() throws Exception {
		authorizedPostJson(ME, loginData.get("refreshToken").asText(), Map.of("languageCode", "ko"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
	}

	@Test
	@DisplayName("AT는 유효하지만 회원이 DB에서 삭제됐으면 401 INVALID_TOKEN")
	void memberDeleted() throws Exception {
		long memberId = Long.parseLong(memberId());
		jdbcTemplate.update("delete from refresh_tokens where member_id = ?", memberId);
		jdbcTemplate.update("delete from members where member_id = ?", memberId);
		// 같은 테스트 트랜잭션 안에서 이미 읽어 둔 회원을 다시 DB에서 찾게 한다.
		entityManager.clear();

		updateMe(Map.of("languageCode", "ko"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("INVALID_TOKEN"));
	}

	private ResultActions updateMe(Map<String, ?> body) throws Exception {
		return authorizedPostJson(ME, accessToken(), body);
	}

	private String accessToken() {
		return loginData.get("accessToken").asText();
	}

	private String memberId() {
		return loginData.get("member").get("memberId").asText();
	}

	private String storedLanguageCode() {
		return jdbcTemplate.queryForObject(
				"select language_code from members where member_id = ?", String.class, Long.parseLong(memberId()));
	}
}
