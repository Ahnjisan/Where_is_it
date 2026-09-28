package com.whereisit.backend.search.ai.openai;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whereisit.backend.global.error.BusinessException;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractionRequest;
import com.whereisit.backend.search.error.SearchErrorCode;

class OpenAiSearchConditionExtractorTest {

	private MockRestServiceServer server;
	private OpenAiSearchConditionExtractor extractor;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.com/v1");
		server = MockRestServiceServer.bindTo(builder).build();
		extractor = new OpenAiSearchConditionExtractor(
				new OpenAiProperties("test-only-key", "test-only-model", Duration.ofSeconds(10)),
				builder.build(), new ObjectMapper());
	}

	@Test
	void sendsStrictMinimalRequestAndParsesMessageAfterReasoning() {
		server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("Authorization", "Bearer test-only-key"))
				.andExpect(header("Content-Type", Matchers.startsWith("application/json")))
				.andExpect(content().string(Matchers.containsString("\"model\":\"test-only-model\"")))
				.andExpect(content().string(Matchers.containsString("\"store\":false")))
				.andExpect(content().string(Matchers.containsString("\"type\":\"json_schema\"")))
				.andExpect(content().string(Matchers.containsString("\"strict\":true")))
				.andExpect(content().string(Matchers.containsString("\"additionalProperties\":false")))
				.andExpect(content().string(Matchers.containsString(
						"\"required\":[\"lostDateFrom\",\"lostDateTo\",\"lostPlaceText\",\"assistantMessage\"]")))
				.andExpect(content().string(Matchers.not(Matchers.containsString("categoryLargeCode"))))
				.andExpect(content().string(Matchers.not(Matchers.containsString("categoryMiddleCode"))))
				.andExpect(content().string(Matchers.not(Matchers.containsString("colorCode"))))
				.andExpect(content().string(Matchers.not(Matchers.containsString("regionCode"))))
				.andExpect(content().string(Matchers.not(Matchers.containsString("subRegionCode"))))
				.andExpect(content().string(Matchers.not(Matchers.containsString("searchStartDate"))))
				.andExpect(content().string(Matchers.not(Matchers.containsString("memberId"))))
				.andExpect(content().string(Matchers.not(Matchers.containsString("lostItemId"))))
				.andExpect(content().string(Matchers.not(Matchers.containsString("foundItemId"))))
				.andExpect(content().string(Matchers.not(Matchers.containsString("candidateId"))))
				.andExpect(content().string(Matchers.not(Matchers.containsString("trackingStartedAt"))))
				.andExpect(content().string(Matchers.not(Matchers.containsString("trackingExpiresAt"))))
				.andExpect(content().string(Matchers.not(Matchers.containsString("notificationEmail"))))
				.andRespond(withSuccess(responseWithReasoning(output(
						"2026-09-20", "2026-09-21", "서울역", "조건을 반영했습니다.")), MediaType.APPLICATION_JSON));

		var result = extractor.extract(request());

		assertThat(result.lostDateFrom()).isEqualTo(LocalDate.of(2026, 9, 20));
		assertThat(result.lostDateTo()).isEqualTo(LocalDate.of(2026, 9, 21));
		assertThat(result.lostPlaceText()).isEqualTo("서울역");
		assertThat(result.assistantMessage()).isEqualTo("조건을 반영했습니다.");
		server.verify();
	}

	@Test
	void missingConfigurationFailsBeforeHttpCall() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.com/v1");
		OpenAiSearchConditionExtractor missingKey = new OpenAiSearchConditionExtractor(
				new OpenAiProperties("", "test-only-model", Duration.ofSeconds(10)),
				builder.build(), new ObjectMapper());
		assertError(missingKey, SearchErrorCode.AI_CONDITION_UNAVAILABLE);

		OpenAiSearchConditionExtractor missingModel = new OpenAiSearchConditionExtractor(
				new OpenAiProperties("test-only-key", "", Duration.ofSeconds(10)),
				builder.build(), new ObjectMapper());
		assertError(missingModel, SearchErrorCode.AI_CONDITION_UNAVAILABLE);
	}

	@ParameterizedTest
	@MethodSource("invalidResponses")
	void rejectsInvalidEnvelopeAndStructuredOutput(String response) {
		assertUnavailableResponse(response);
	}

	private static Stream<Arguments> invalidResponses() {
		return Stream.of(
				Arguments.of(""),
				Arguments.of("not-json"),
				Arguments.of("{}"),
				Arguments.of("{\"status\":\"incomplete\",\"output\":[]}"),
				Arguments.of("{\"status\":\"completed\",\"output\":{}}"),
				Arguments.of("{\"status\":\"completed\",\"output\":[1]}"),
				Arguments.of("{\"status\":\"completed\",\"output\":[{\"content\":[]}]}"),
				Arguments.of("{\"status\":\"completed\",\"output\":[{\"type\":\"message\",\"content\":[]}]}"),
				Arguments.of(refusalResponse()),
				Arguments.of(response(output(null, null, null, "ok"), output(null, null, null, "again"))),
				Arguments.of(response("not-json")),
				Arguments.of(response("{\"lostDateFrom\":null,\"lostDateTo\":null,\"lostPlaceText\":null}")),
				Arguments.of(response("{\"lostDateFrom\":null,\"lostDateTo\":null,\"lostPlaceText\":null,"
						+ "\"assistantMessage\":\"ok\",\"regionCode\":\"SEOUL\"}")),
				Arguments.of(response("{\"lostDateFrom\":1,\"lostDateTo\":null,\"lostPlaceText\":null,"
						+ "\"assistantMessage\":\"ok\"}")),
				Arguments.of(response("{\"lostDateFrom\":null,\"lostDateTo\":null,\"lostPlaceText\":1,"
						+ "\"assistantMessage\":\"ok\"}")),
				Arguments.of(response("{\"lostDateFrom\":null,\"lostDateTo\":null,\"lostPlaceText\":null,"
						+ "\"assistantMessage\":1}")),
				Arguments.of(response(output("not-a-date", null, null, "ok"))),
				Arguments.of(response(output("2026-09-21", "2026-09-20", null, "ok"))),
				Arguments.of(response(output(null, null, "x".repeat(256), "ok"))),
				Arguments.of(response(output(null, null, null, " "))),
				Arguments.of(response(output(null, null, null, "x".repeat(2001)))));
	}

	@ParameterizedTest
	@MethodSource("httpErrors")
	void mapsHttpErrorsWithoutRetry(HttpStatus status, SearchErrorCode expected) {
		server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
				.andRespond(withStatus(status));

		assertError(extractor, expected);
		server.verify();
	}

	private static Stream<Arguments> httpErrors() {
		return Stream.of(
				Arguments.of(HttpStatus.REQUEST_TIMEOUT, SearchErrorCode.SEARCH_TIMEOUT),
				Arguments.of(HttpStatus.GATEWAY_TIMEOUT, SearchErrorCode.SEARCH_TIMEOUT),
				Arguments.of(HttpStatus.TOO_MANY_REQUESTS, SearchErrorCode.RATE_LIMITED),
				Arguments.of(HttpStatus.BAD_REQUEST, SearchErrorCode.AI_CONDITION_UNAVAILABLE),
				Arguments.of(HttpStatus.UNAUTHORIZED, SearchErrorCode.AI_CONDITION_UNAVAILABLE),
				Arguments.of(HttpStatus.INTERNAL_SERVER_ERROR, SearchErrorCode.AI_CONDITION_UNAVAILABLE));
	}

	@Test
	void mapsConnectAndReadTimeoutWithoutRetry() {
		server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
				.andRespond(request -> {
					throw new ResourceAccessException("sanitized-test-timeout", new SocketTimeoutException());
				});
		assertError(extractor, SearchErrorCode.SEARCH_TIMEOUT);
		server.verify();

		setUp();
		server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
				.andRespond(request -> {
					throw new ResourceAccessException("sanitized-test-timeout", new HttpTimeoutException("timeout"));
				});
		assertError(extractor, SearchErrorCode.SEARCH_TIMEOUT);
		server.verify();
	}

	private void assertUnavailableResponse(String body) {
		server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
				.andRespond(withSuccess(body, MediaType.APPLICATION_JSON));
		assertError(extractor, SearchErrorCode.AI_CONDITION_UNAVAILABLE);
		server.verify();
	}

	private void assertError(OpenAiSearchConditionExtractor target, SearchErrorCode expected) {
		assertThatThrownBy(() -> target.extract(request()))
				.isInstanceOfSatisfying(BusinessException.class,
						exception -> assertThat(exception.getErrorCode()).isEqualTo(expected));
	}

	private AiSearchConditionExtractionRequest request() {
		return new AiSearchConditionExtractionRequest(
				"서울역에서 잃어버렸어요", "파란 지갑", "ko", LocalDate.of(2026, 9, 27), null, null, null);
	}

	private static String responseWithReasoning(String output) {
		return "{\"status\":\"completed\",\"output\":[{\"type\":\"reasoning\",\"content\":[]},"
				+ message(output) + "]}";
	}

	private static String response(String... outputs) {
		return "{\"status\":\"completed\",\"output\":["
				+ Stream.of(outputs).map(OpenAiSearchConditionExtractorTest::message)
						.reduce((left, right) -> left + "," + right).orElse("")
				+ "]}";
	}

	private static String message(String output) {
		String escaped = output.replace("\\", "\\\\").replace("\"", "\\\"");
		return "{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\""
				+ escaped + "\"}]}";
	}

	private static String refusalResponse() {
		return "{\"status\":\"completed\",\"output\":[{\"type\":\"message\",\"content\":["
				+ "{\"type\":\"refusal\",\"refusal\":\"no\"}]}]}";
	}

	private static String output(String from, String to, String place, String message) {
		return "{\"lostDateFrom\":" + json(from) + ",\"lostDateTo\":" + json(to)
				+ ",\"lostPlaceText\":" + json(place) + ",\"assistantMessage\":" + json(message) + "}";
	}

	private static String json(String value) {
		return value == null ? "null" : "\"" + value + "\"";
	}
}
