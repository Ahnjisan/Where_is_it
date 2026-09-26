package com.whereisit.backend.global.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.Filter;

/**
 * 컨트롤러 밖에서 난 오류가 컨테이너의 /error 전달(ERROR dispatch)을 거쳐 공통 형식으로 나가는지 실제 서버로 확인한다.
 * MockMvc는 /error 전달을 하지 않아서 이 경로를 재현하지 못한다.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ExtendWith(OutputCaptureExtension.class)
@DisplayName("컨트롤러 밖 오류의 공통 형식 응답")
class ErrorDispatchHttpTest {

	private static final String FILTER_ERROR_PATH = "/test/filter-error";
	private static final String INTERNAL_DETAIL = "internal-detail-must-not-leak";

	private final HttpClient httpClient = HttpClient.newHttpClient();

	@LocalServerPort
	private int port;

	@Autowired
	private ObjectMapper objectMapper;

	@ParameterizedTest(name = "{0}")
	@ValueSource(strings = { "/api/members/me;x=1", "/api//members/me", "/api/members/%2e%2e/me" })
	@DisplayName("Security 방화벽이 거부한 URL은 400 VALIDATION_ERROR 공통 형식이고, 로그에 같은 requestId가 남는다")
	void firewallRejection(String path, CapturedOutput output) throws Exception {
		HttpResponse<String> response = get(path);

		assertThat(response.statusCode()).isEqualTo(400);
		assertThat(response.headers().firstValue("Content-Type")).hasValueSatisfying(
				type -> assertThat(type).startsWith("application/json"));
		JsonNode error = errorOf(response);
		assertThat(error.get("code").asText()).isEqualTo("VALIDATION_ERROR");
		assertThat(error.get("fields")).isEmpty();
		String requestId = error.get("requestId").asText();
		assertThat(requestId).isNotBlank();
		assertThat(output.getOut()).contains("[" + requestId + "]").contains("request rejected outside controllers");
	}

	@Test
	@DisplayName("필터에서 난 예외는 500 INTERNAL_ERROR 공통 형식이고, 예외 원문은 응답이 아니라 로그에만 남는다")
	void exceptionInFilter(CapturedOutput output) throws Exception {
		HttpResponse<String> response = get(FILTER_ERROR_PATH);

		assertThat(response.statusCode()).isEqualTo(500);
		JsonNode error = errorOf(response);
		assertThat(error.get("code").asText()).isEqualTo("INTERNAL_ERROR");
		assertThat(error.get("message").asText()).isEqualTo("Unexpected server error.");
		assertThat(response.body()).doesNotContain(INTERNAL_DETAIL);
		String requestId = error.get("requestId").asText();
		assertThat(output.getOut()).contains("[" + requestId + "]").contains(INTERNAL_DETAIL);
	}

	private HttpResponse<String> get(String path) throws IOException, InterruptedException {
		HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
		return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
	}

	private JsonNode errorOf(HttpResponse<String> response) throws IOException {
		JsonNode body = objectMapper.readTree(response.body());
		assertThat(body.get("success").asBoolean()).isFalse();
		return body.get("error");
	}

	/** 요청 처리 중 예외를 던지는 필터. 인증 필터보다 앞에 둬서 인증과 관계없이 예외가 나게 한다. */
	@TestConfiguration(proxyBeanMethods = false)
	static class ThrowingFilterConfig {

		@Bean
		FilterRegistrationBean<Filter> throwingFilter() {
			FilterRegistrationBean<Filter> registration = new FilterRegistrationBean<>((request, response, chain) -> {
				throw new IllegalStateException(INTERNAL_DETAIL);
			});
			registration.addUrlPatterns(FILTER_ERROR_PATH);
			registration.setOrder(Ordered.HIGHEST_PRECEDENCE + 10);
			return registration;
		}
	}
}
