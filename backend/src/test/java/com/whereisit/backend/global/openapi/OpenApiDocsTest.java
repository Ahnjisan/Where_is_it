package com.whereisit.backend.global.openapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.fasterxml.jackson.databind.JsonNode;
import com.whereisit.backend.support.ApiTestSupport;

/**
 * Issue #87. /v3/api-docs에 공개 대상 API만 싣는다. API-08·09·11·12·13은 문서에서만 숨기고 URL은 그대로 동작하며,
 * 추적 활성화는 API-17로 표기한다. API-06·15의 노출 상태와 API-16 미구현 상태는 바뀌지 않아야 한다.
 */
@DisplayName("Issue #87 OpenAPI 문서 노출 범위")
class OpenApiDocsTest extends ApiTestSupport {

	private static final List<String> HIDDEN_PATHS = List.of(
			"/api/lost-items/update/{lostItemId}",
			"/api/lost-items/delete/{lostItemId}",
			"/api/lost-items/{lostItemId}/searches",
			"/api/lost-items/{lostItemId}/candidates",
			"/api/lost-items/{lostItemId}/candidates/{candidateId}");

	@Autowired
	@Qualifier("requestMappingHandlerMapping")
	private RequestMappingHandlerMapping handlerMapping;

	@Test
	@DisplayName("API-14 표기는 없고 추적 활성화는 API-17로 표기한다")
	void trackingActivationIsApi17() throws Exception {
		String body = apiDocsBody();
		JsonNode paths = objectMapper.readTree(body).get("paths");

		assertThat(body).doesNotContain("API-14");
		assertThat(paths.path("/api/lost-items/{lostItemId}/tracking").path("post").path("summary").asText())
				.startsWith("API-17");
	}

	@Test
	@DisplayName("API-08·09·11·12·13은 문서에 싣지 않는다")
	void hiddenEndpointsAreNotDocumented() throws Exception {
		String body = apiDocsBody();
		JsonNode paths = objectMapper.readTree(body).get("paths");

		assertThat(HIDDEN_PATHS).allSatisfy(path -> assertThat(paths.has(path)).as(path).isFalse());
		assertThat(summaries(paths)).noneMatch(summary -> summary.startsWith("API-08") || summary.startsWith("API-09")
				|| summary.startsWith("API-11") || summary.startsWith("API-12") || summary.startsWith("API-13"));
	}

	@Test
	@DisplayName("API-06·15는 그대로 싣고 API-16은 지금처럼 없다")
	void protectedEndpointsKeepExposure() throws Exception {
		String body = apiDocsBody();
		JsonNode paths = objectMapper.readTree(body).get("paths");

		assertThat(paths.path("/api/lost-items").path("get").path("summary").asText())
				.isEqualTo("API-06 내 분실물 목록");
		assertThat(paths.path("/api/lost-items/{lostItemId}/notifications").path("get").path("summary").asText())
				.isEqualTo("API-15 신규 후보 이메일 알림 이력");
		assertThat(body).doesNotContain("API-16");
	}

	@Test
	@DisplayName("문서에서 숨긴 API도 URL 매핑은 그대로 남아 있다")
	void hiddenEndpointsAreStillMapped() {
		Set<String> mappings = handlerMapping.getHandlerMethods().keySet().stream()
				.flatMap(info -> info.getMethodsCondition().getMethods().stream()
						.flatMap(method -> info.getPatternValues().stream().map(pattern -> method + " " + pattern)))
				.collect(Collectors.toSet());

		assertThat(mappings).contains(
				"POST /api/lost-items/update/{lostItemId}",
				"POST /api/lost-items/delete/{lostItemId}",
				"POST /api/lost-items/{lostItemId}/searches",
				"GET /api/lost-items/{lostItemId}/candidates",
				"GET /api/lost-items/{lostItemId}/candidates/{candidateId}");
	}

	private String apiDocsBody() throws Exception {
		return mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
	}

	private static List<String> summaries(JsonNode paths) {
		List<String> summaries = new ArrayList<>();
		paths.forEach(pathItem -> pathItem.forEach(operation -> summaries.add(operation.path("summary").asText())));
		return summaries;
	}
}
