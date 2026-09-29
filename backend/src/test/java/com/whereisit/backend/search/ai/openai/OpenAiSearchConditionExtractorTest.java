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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Stream;

import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whereisit.backend.global.error.BusinessException;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractionMode;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractionRequest;
import com.whereisit.backend.search.error.SearchErrorCode;

class OpenAiSearchConditionExtractorTest {

	private static final List<String> SIX_FIELDS = List.of("lostDateFrom", "lostDateTo", "lostPlaceText",
			"productNameKeyword", "storagePlaceKeyword", "assistantMessage");

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
						"\"required\":[\"lostDateFrom\",\"lostDateTo\",\"lostPlaceText\",\"productNameKeyword\",\"storagePlaceKeyword\",\"assistantMessage\"]")))
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
		assertThat(result.productNameKeyword()).isEqualTo("지갑");
		assertThat(result.storagePlaceKeyword()).isEqualTo("서울역 유실물센터");
		assertThat(result.assistantMessage()).isEqualTo("조건을 반영했습니다.");
		server.verify();
	}

	@Test
	@DisplayName("API-11 모드는 기존 6개 필드 Schema·지시만 보내고 colorName을 요청하지 않는다")
	void searchConditionModeKeepsSixFieldContract() throws Exception {
		AtomicReference<String> body = new AtomicReference<>();
		server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
				.andExpect(request -> body.set(((MockClientHttpRequest) request).getBodyAsString()))
				.andRespond(withSuccess(response(output(null, null, null, "ok")), MediaType.APPLICATION_JSON));

		var result = extractor.extract(request());

		server.verify();
		JsonNode schema = schemaOf(body.get());
		assertThat(fieldNames(schema.get("properties"))).containsExactly(SIX_FIELDS.toArray(String[]::new));
		assertThat(texts(schema.get("required"))).containsExactlyElementsOf(SIX_FIELDS);
		assertThat(body.get()).doesNotContain("colorName");
		assertThat(result.colorName()).isNull();
	}

	@Test
	@DisplayName("API-11 모드에서 colorName이 응답에 섞여 오면 추가 필드로 거부한다")
	void searchConditionModeRejectsColorName() {
		assertUnavailableResponse(response(initialOutput("지갑", "검정")));
	}

	@Test
	@DisplayName("API-05 모드는 같은 1회 호출에서 colorName을 더 요청하고 properties·required·허용 필드가 일치한다")
	void initialSearchModeAddsColorNameWithMatchingSchema() throws Exception {
		AtomicReference<String> body = new AtomicReference<>();
		server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
				.andExpect(request -> body.set(((MockClientHttpRequest) request).getBodyAsString()))
				.andRespond(withSuccess(response(initialOutput("핸드폰", "검정")), MediaType.APPLICATION_JSON));

		var result = extractor.extract(initialRequest("강남역에서 검은 핸드폰을 잃어버렸어요."));

		server.verify();
		JsonNode root = new ObjectMapper().readTree(body.get());
		JsonNode schema = schemaOf(body.get());
		List<String> expected = new ArrayList<>(SIX_FIELDS);
		expected.add("colorName");
		assertThat(fieldNames(schema.get("properties"))).containsExactlyElementsOf(expected);
		assertThat(texts(schema.get("required"))).containsExactlyElementsOf(expected);
		assertThat(schema.get("additionalProperties").booleanValue()).isFalse();
		JsonNode colorSchema = schema.get("properties").get("colorName");
		assertThat(texts(colorSchema.get("type"))).containsExactly("string", "null");
		assertThat(colorSchema.get("minLength").intValue()).isEqualTo(1);
		assertThat(colorSchema.get("maxLength").intValue()).isEqualTo(100);
		// productNameKeyword의 Schema와 기존 지시는 API-11 모드와 같고, 색상 지시는 뒤에 덧붙기만 한다.
		assertThat(schema.get("properties").get("productNameKeyword"))
				.isEqualTo(searchConditionSchema().get("properties").get("productNameKeyword"));
		assertThat(root.get("instructions").textValue()).startsWith(searchConditionInstructions());
		assertThat(root.get("instructions").textValue()).contains("colorName", "검정");
		assertThat(body.get()).doesNotContain("colorCode");

		assertThat(result.productNameKeyword()).isEqualTo("핸드폰");
		assertThat(result.colorName()).isEqualTo("검정");
	}

	@ParameterizedTest
	@MethodSource("initialColorNames")
	@DisplayName("API-05 모드는 AI가 준 색상명(없거나 모호하면 null, 복수는 /)을 그대로 돌려준다")
	void initialSearchModeParsesColorName(String description, String aiColorName) {
		server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
				.andRespond(withSuccess(response(initialOutput("지갑", aiColorName)), MediaType.APPLICATION_JSON));

		var result = extractor.extract(initialRequest(description));

		server.verify();
		assertThat(result.colorName()).isEqualTo(aiColorName);
		assertThat(result.productNameKeyword()).isEqualTo("지갑");
	}

	private static Stream<Arguments> initialColorNames() {
		return Stream.of(
				Arguments.of("강남역에서 검은 지갑을 잃어버렸어요.", "검정"),
				Arguments.of("I lost my black wallet at Gangnam Station.", "검정"),
				Arguments.of("강남역에서 지갑을 잃어버렸어요.", null),
				Arguments.of("어두운 색 지갑을 잃어버렸어요.", null),
				Arguments.of("검정과 흰색 줄무늬 지갑을 잃어버렸어요.", "검정/흰색"),
				Arguments.of("지갑", "가".repeat(100)));
	}

	@ParameterizedTest
	@MethodSource("unusableColorNames")
	@DisplayName("API-05 모드는 쓸 수 없는 colorName 문자열(빈 값·공백·100자 초과)을 실패 없이 null로 둔다")
	void initialSearchModeFailsOpenForUnusableColorName(String aiColorName, String expected) {
		server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
				.andRespond(withSuccess(response(initialOutput("핸드폰", aiColorName)), MediaType.APPLICATION_JSON));

		var result = extractor.extract(initialRequest("강남역에서 검은 핸드폰을 잃어버렸어요."));

		server.verify();
		assertThat(result.colorName()).isEqualTo(expected);
		assertThat(result.productNameKeyword()).isEqualTo("핸드폰");
		assertThat(result.storagePlaceKeyword()).isEqualTo("서울역 유실물센터");
		assertThat(result.assistantMessage()).isEqualTo("조건을 반영했습니다.");
	}

	private static Stream<Arguments> unusableColorNames() {
		return Stream.of(
				Arguments.of("", null),
				Arguments.of("   ", null),
				Arguments.of("가".repeat(101), null),
				Arguments.of(" 검정 ", "검정"));
	}

	@ParameterizedTest
	@MethodSource("invalidInitialOutputs")
	@DisplayName("API-05 모드도 colorName 누락·JSON 타입 오류·추가 필드와 기존 핵심 필드 오류는 기존처럼 실패한다")
	void initialSearchModeRejectsInvalidColorName(String output) {
		server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
				.andRespond(withSuccess(response(output), MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> extractor.extract(initialRequest("검은 지갑")))
				.isInstanceOfSatisfying(BusinessException.class, exception -> assertThat(exception.getErrorCode())
						.isEqualTo(SearchErrorCode.AI_CONDITION_UNAVAILABLE));
		server.verify();
	}

	private static Stream<Arguments> invalidInitialOutputs() {
		return Stream.of(
				Arguments.of(output(null, null, null, "ok")),
				Arguments.of(initialOutput("지갑", "검정").replace("\"colorName\":\"검정\"", "\"colorName\":123")),
				Arguments.of(initialOutput("지갑", "검정").replace("\"colorName\":\"검정\"", "\"colorName\":true")),
				Arguments.of(initialOutput("지갑", "검정").replace("}", ",\"colorCode\":\"BK\"}")),
				Arguments.of("not-json"),
				Arguments.of(initialOutput(" ", "검정")),
				Arguments.of(initialOutput("x".repeat(201), "검정")),
				Arguments.of(initialOutput("지갑", "검정").replace("\"lostDateFrom\":null", "\"lostDateFrom\":\"bad\"")),
				Arguments.of(initialOutput("지갑", "검정").replace(",\"storagePlaceKeyword\":\"서울역 유실물센터\"", "")));
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
				Arguments.of(response(output(null, null, null, " ", "서울역", "ok"))),
				Arguments.of(response(output(null, null, null, "지갑", "x".repeat(201), "ok"))),
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

	private AiSearchConditionExtractionRequest initialRequest(String description) {
		return new AiSearchConditionExtractionRequest(description, description, "ko", LocalDate.of(2026, 9, 27),
				null, null, null, AiSearchConditionExtractionMode.INITIAL_SEARCH_WITH_COLOR_NAME);
	}

	/** API-11 모드 요청 body의 schema. 두 모드의 공통 부분을 비교하는 기준이다. */
	private JsonNode searchConditionSchema() throws Exception {
		return schemaOf(searchConditionBody());
	}

	private String searchConditionInstructions() throws Exception {
		return new ObjectMapper().readTree(searchConditionBody()).get("instructions").textValue();
	}

	private String searchConditionBody() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.com/v1");
		MockRestServiceServer legacyServer = MockRestServiceServer.bindTo(builder).build();
		AtomicReference<String> body = new AtomicReference<>();
		legacyServer.expect(once(), requestTo("https://api.openai.com/v1/responses"))
				.andExpect(request -> body.set(((MockClientHttpRequest) request).getBodyAsString()))
				.andRespond(withSuccess(response(output(null, null, null, "ok")), MediaType.APPLICATION_JSON));
		new OpenAiSearchConditionExtractor(
				new OpenAiProperties("test-only-key", "test-only-model", Duration.ofSeconds(10)),
				builder.build(), new ObjectMapper()).extract(request());
		legacyServer.verify();
		return body.get();
	}

	private static JsonNode schemaOf(String body) throws Exception {
		return new ObjectMapper().readTree(body).get("text").get("format").get("schema");
	}

	private static List<String> fieldNames(JsonNode node) {
		List<String> names = new ArrayList<>();
		node.fieldNames().forEachRemaining(names::add);
		return names;
	}

	private static List<String> texts(JsonNode array) {
		List<String> values = new ArrayList<>();
		array.forEach(value -> values.add(value.textValue()));
		return values;
	}

	private static String initialOutput(String productNameKeyword, String colorName) {
		String sixFields = output(null, null, null, productNameKeyword, "서울역 유실물센터", "조건을 반영했습니다.");
		return sixFields.substring(0, sixFields.length() - 1) + ",\"colorName\":" + json(colorName) + "}";
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
		return output(from, to, place, "지갑", "서울역 유실물센터", message);
	}

	private static String output(String from, String to, String place, String productNameKeyword,
			String storagePlaceKeyword, String message) {
		return "{\"lostDateFrom\":" + json(from) + ",\"lostDateTo\":" + json(to)
				+ ",\"lostPlaceText\":" + json(place)
				+ ",\"productNameKeyword\":" + json(productNameKeyword)
				+ ",\"storagePlaceKeyword\":" + json(storagePlaceKeyword)
				+ ",\"assistantMessage\":" + json(message) + "}";
	}

	private static String json(String value) {
		return value == null ? "null" : "\"" + value + "\"";
	}
}
