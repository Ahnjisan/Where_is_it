package com.whereisit.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 테스트 트랜잭션 없이 실제 서버로 가입 → 로그인 → 내 정보 조회를 확인한다.
 * 다른 API 테스트는 요청이 테스트 트랜잭션 안에서 처리되므로, 실제 커밋과 트랜잭션 밖 응답 직렬화
 * (open-in-view: false)는 여기서만 확인된다. 실제로 커밋되므로 끝나면 이 테스트가 만든 행만 지운다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DisplayName("실제 서버에서 가입·로그인·내 정보 조회")
class AuthFlowHttpTest {

	/** 로컬 DB의 다른 데이터와 겹치지 않는 이메일 */
	private final String email = "flow-" + UUID.randomUUID() + "@example.test";

	private final HttpClient httpClient = HttpClient.newHttpClient();

	@LocalServerPort
	private int port;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@AfterEach
	void deleteCreatedRows() {
		jdbcTemplate.update(
				"delete from refresh_tokens where member_id in (select member_id from members where email = ?)", email);
		jdbcTemplate.update("delete from members where email = ?", email);
	}

	@Test
	@DisplayName("가입·로그인이 DB에 커밋되고, 발급받은 AT로 내 정보를 조회한다")
	void signupLoginAndGetMe() throws Exception {
		HttpResponse<String> signup = post("/api/auth/signup",
				Map.of("email", email, "password", "Password1!", "languageCode", "ko"));
		assertThat(signup.statusCode()).isEqualTo(201);
		String memberId = data(signup).get("memberId").asText();

		HttpResponse<String> login = post("/api/auth/login", Map.of("email", email, "password", "Password1!"));
		assertThat(login.statusCode()).isEqualTo(200);
		assertThat(login.headers().firstValue("Cache-Control")).hasValueSatisfying(
				value -> assertThat(value).contains("no-store"));
		String accessToken = data(login).get("accessToken").asText();

		assertThat(jdbcTemplate.queryForObject("select count(*) from members where email = ?", Integer.class, email))
				.isEqualTo(1);
		assertThat(jdbcTemplate.queryForObject(
				"select count(*) from refresh_tokens where member_id = ?", Integer.class, Long.valueOf(memberId)))
				.isEqualTo(1);

		HttpResponse<String> me = httpClient.send(
				HttpRequest.newBuilder(uri("/api/members/me")).header("Authorization", "Bearer " + accessToken).GET().build(),
				HttpResponse.BodyHandlers.ofString());
		assertThat(me.statusCode()).isEqualTo(200);
		JsonNode member = data(me);
		assertThat(member.get("memberId").asText()).isEqualTo(memberId);
		assertThat(member.get("email").asText()).isEqualTo(email);
		assertThat(member.get("languageCode").asText()).isEqualTo("ko");
		assertThat(member.get("createdAt").asText()).endsWith("+09:00");
	}

	private HttpResponse<String> post(String path, Map<String, String> body) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(uri(path))
				.header("Content-Type", "application/json")
				.POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
				.build();
		return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private URI uri(String path) {
		return URI.create("http://localhost:" + port + path);
	}

	private JsonNode data(HttpResponse<String> response) throws IOException {
		JsonNode body = objectMapper.readTree(response.body());
		assertThat(body.get("success").asBoolean()).isTrue();
		return body.get("data");
	}
}
