package com.whereisit.backend.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 실제 보안 필터와 DB(test는 MySQL, testSqlite는 SQLite)까지 거치는 API 테스트의 공통 설정.
 * 요청이 테스트 트랜잭션 안에서 처리되고 끝나면 롤백되므로, 로컬 DB에 테스트 데이터가 남지 않는다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@Import(TestClockConfig.class)
public abstract class ApiTestSupport {

	protected static final String PASSWORD = "Password1!";

	@Autowired
	protected MockMvc mockMvc;

	@Autowired
	protected ObjectMapper objectMapper;

	@Autowired
	protected MutableClock clock;

	@BeforeEach
	void resetClock() {
		clock.setInstant(TestClockConfig.START);
	}

	protected ResultActions postJson(String url, Map<String, ?> body) throws Exception {
		return mockMvc.perform(post(url)
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(body)));
	}

	protected ResultActions signup(String email, String password, String languageCode) throws Exception {
		return postJson("/api/auth/signup", Map.of("email", email, "password", password, "languageCode", languageCode));
	}

	protected ResultActions login(String email, String password) throws Exception {
		return postJson("/api/auth/login", Map.of("email", email, "password", password));
	}

	protected ResultActions refresh(String refreshToken) throws Exception {
		return postJson("/api/auth/refresh", Map.of("refreshToken", refreshToken));
	}

	protected ResultActions logout(String refreshToken) throws Exception {
		return postJson("/api/auth/logout", Map.of("refreshToken", refreshToken));
	}

	/** 가입 후 로그인해서 응답의 data(LoginData)를 돌려준다. */
	protected JsonNode signupAndLogin(String email) throws Exception {
		signup(email, PASSWORD, "en").andExpect(status().isCreated());
		return loginData(email);
	}

	protected JsonNode loginData(String email) throws Exception {
		String body = login(email, PASSWORD)
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(body).get("data");
	}
}
