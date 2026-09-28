package com.whereisit.backend.candidate.ranking.openai;

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
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
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
import com.whereisit.backend.candidate.ranking.CandidateRankingInput;
import com.whereisit.backend.candidate.ranking.CandidateRankingRequest;
import com.whereisit.backend.candidate.ranking.CandidateRankingResult;
import com.whereisit.backend.candidate.ranking.SimpleTextSimilarityRanker;
import com.whereisit.backend.global.error.BusinessException;
import com.whereisit.backend.search.ai.openai.OpenAiProperties;
import com.whereisit.backend.search.error.SearchErrorCode;

class OpenAiCandidateRankerTest {

	private MockRestServiceServer server;
	private OpenAiCandidateRanker ranker;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.com/v1");
		server = MockRestServiceServer.bindTo(builder).build();
		ranker = new OpenAiCandidateRanker(
				new OpenAiProperties("test-only-key", "test-only-model", Duration.ofSeconds(10)),
				builder.build(), new ObjectMapper(), new SimpleTextSimilarityRanker());
	}

	@Test
	void sendsStrictMinimalRequestAndParsesReasoningInAnyOrder() {
		server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("Authorization", "Bearer test-only-key"))
				.andExpect(content().string(Matchers.containsString("\"model\":\"test-only-model\"")))
				.andExpect(content().string(Matchers.containsString("\"store\":false")))
				.andExpect(content().string(Matchers.containsString("\"type\":\"json_schema\"")))
				.andExpect(content().string(Matchers.containsString("\"strict\":true")))
				.andExpect(content().string(Matchers.containsString("\"additionalProperties\":false")))
				.andExpect(content().string(Matchers.containsString("\"minItems\":2")))
				.andExpect(content().string(Matchers.containsString("\"maxLength\":500")))
				.andExpect(content().string(Matchers.containsString("\\\"candidateKey\\\":\\\"c1\\\"")))
				.andExpect(content().string(Matchers.not(Matchers.containsString("atcId"))))
				.andExpect(content().string(Matchers.not(Matchers.containsString("fdSn"))))
				.andExpect(content().string(Matchers.not(Matchers.containsString("sourceType"))))
				.andExpect(content().string(Matchers.not(Matchers.containsString("imageUrl"))))
				.andExpect(content().string(Matchers.not(Matchers.containsString("memberId"))))
				.andExpect(content().string(Matchers.not(Matchers.containsString("lostItemId"))))
				.andRespond(withSuccess(response(true, output(
						row("c2", 1, false, "설명과 일치하지 않는 후보입니다."),
						row("c1", 2, true, "파란 지갑이라는 설명과 일치합니다."))), MediaType.APPLICATION_JSON));

		CandidateRankingResult result = ranker.rank(request("ko", 2));

		assertThat(result.rankingStatus()).isEqualTo(CandidateRankingResult.RankingStatus.SUCCESS);
		assertThat(result.openAiResultUsed()).isTrue();
		assertThat(result.candidates()).extracting(candidate -> candidate.candidateKey())
				.containsExactly("c2", "c1");
		server.verify();
	}

	@Test
	void reasoningAfterMessageIsAlsoAccepted() {
		server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
				.andRespond(withSuccess(response(false, output(row("c1", 1, true, "A matching wallet."))),
						MediaType.APPLICATION_JSON));
		assertThat(ranker.rank(request("en", 1)).rankingStatus())
				.isEqualTo(CandidateRankingResult.RankingStatus.SUCCESS);
		server.verify();
	}

	@ParameterizedTest
	@MethodSource("invalidResponses")
	void invalidResponsesDiscardAllAiOutputAndFallback(String response) {
		server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
				.andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

		CandidateRankingResult result = ranker.rank(request("ko", 2));

		assertFallback(result, 2);
		server.verify();
	}

	private static Stream<Arguments> invalidResponses() {
		return Stream.of(
				Arguments.of(""),
				Arguments.of("not-json"),
				Arguments.of("{}"),
				Arguments.of("{\"status\":\"incomplete\",\"output\":[]}"),
				Arguments.of("{\"status\":\"completed\",\"output\":[{\"type\":\"reasoning\",\"content\":[]}]}"),
				Arguments.of("{\"status\":\"completed\",\"output\":{}}"),
				Arguments.of(refusalResponse()),
				Arguments.of(responseWithTexts()),
				Arguments.of(response(true, "not-json")),
				Arguments.of(response(true, "{\"rankings\":[],\"extra\":1}")),
				Arguments.of(response(true, "{\"rankings\":["
						+ "{\"candidateKey\":\"c1\",\"rank\":1,\"isSimilar\":true},"
						+ row("c2", 2, false, "한글 이유") + "]}")),
				Arguments.of(response(true, "{\"rankings\":["
						+ "{\"candidateKey\":\"c1\",\"rank\":1,\"isSimilar\":true,\"reason\":\"한글\",\"extra\":1},"
						+ row("c2", 2, false, "한글 이유") + "]}")),
				Arguments.of(response(true, output(
						"{\"candidateKey\":\"c1\",\"rank\":\"1\",\"isSimilar\":true,\"reason\":\"한글\"}",
						row("c2", 2, false, "한글 이유")))),
				Arguments.of(response(true, output(row("c1", 1, true, "한글")))),
				Arguments.of(response(true, output(row("c1", 1, true, "한글"), row("c2", 2, false, "한글"),
						row("c3", 3, false, "한글")))),
				Arguments.of(response(true, output(row("C1", 1, true, "한글"), row("c2", 2, false, "한글")))),
				Arguments.of(response(true, output(row("c1", 1, true, "한글"), row("c1", 2, false, "한글")))),
				Arguments.of(response(true, output(row("c1", 1, true, "한글"), row("c2", 1, false, "한글")))),
				Arguments.of(response(true, output(row("c1", 1, true, "한글"), row("c2", 3, false, "한글")))),
				Arguments.of(response(true, output(row("c1", 1, true, " "), row("c2", 2, false, "한글")))),
				Arguments.of(response(true, output(row("c1", 1, true, "x".repeat(501)), row("c2", 2, false, "한글")))),
				Arguments.of(response(true, output(row("c1", 1, true, "English only"), row("c2", 2, false, "한글")))),
				Arguments.of(response(true, output(row("c1", 1, true, "<b>한글</b>"), row("c2", 2, false, "한글")))),
				Arguments.of(response(true, output(
						rowWithRawJsonReason("c1", 1, true, "한글" + "\\" + "u0001이유"),
						row("c2", 2, false, "한글")))),
				Arguments.of(response(true, "{\"rankings\":["
						+ "{\"candidateKey\":\"c1\",\"candidateKey\":\"c1\",\"rank\":1,\"isSimilar\":true,\"reason\":\"한글\"},"
						+ row("c2", 2, false, "한글") + "]}")));
	}

	@ParameterizedTest
	@MethodSource("fallbackHttpErrors")
	void genericHttpErrorFallsBackWithoutRetry(HttpStatus status) {
		server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
				.andRespond(withStatus(status));

		assertFallback(ranker.rank(request("ko", 1)), 1);
		server.verify();
	}

	private static Stream<Arguments> fallbackHttpErrors() {
		return Stream.of(HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED, HttpStatus.INTERNAL_SERVER_ERROR)
				.map(Arguments::of);
	}

	@ParameterizedTest
	@MethodSource("timeoutHttpErrors")
	void timeoutHttpErrorMapsToExistingTimeoutErrorWithoutRetry(HttpStatus status) {
		server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
				.andRespond(withStatus(status));

		assertError(SearchErrorCode.SEARCH_TIMEOUT);
		server.verify();
	}

	private static Stream<Arguments> timeoutHttpErrors() {
		return Stream.of(HttpStatus.REQUEST_TIMEOUT, HttpStatus.GATEWAY_TIMEOUT).map(Arguments::of);
	}

	@Test
	void rateLimitMapsToExistingRateLimitErrorWithoutRetry() {
		server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
				.andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

		assertError(SearchErrorCode.RATE_LIMITED);
		server.verify();
	}

	@Test
	void socketTimeoutMapsToExistingTimeoutErrorWithoutRetry() {
		server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
				.andRespond(request -> {
					throw new ResourceAccessException("sanitized-test-timeout", new SocketTimeoutException());
				});

		assertError(SearchErrorCode.SEARCH_TIMEOUT);
		server.verify();
	}

	@Test
	void missingConfigurationAndOversizedBodyDoNotCallHttp() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.com/v1");
		OpenAiCandidateRanker missing = new OpenAiCandidateRanker(
				new OpenAiProperties("", "", Duration.ofSeconds(10)), builder.build(),
				new ObjectMapper(), new SimpleTextSimilarityRanker());
		assertFallback(missing.rank(request("ko", 1)), 1);

		List<CandidateRankingInput> huge = IntStream.rangeClosed(1, 100)
				.mapToObj(i -> new CandidateRankingInput("c" + i, "😀".repeat(200), "😀".repeat(300),
						"😀".repeat(100), "😀".repeat(100), LocalDate.of(2026, 9, 28), "😀".repeat(255)))
				.toList();
		assertFallback(ranker.rank(new CandidateRankingRequest(
				"😀".repeat(2000), "ko", null, null, "😀".repeat(255), huge)), 100);
		server.verify();
	}

	@Test
	void finalSerializedBodyUsesInclusiveOneHundredTwentyEightKibLimit() throws Exception {
		byte[] normalBody = ranker.serializeRequestBody(request("ko", 1));

		assertThat(normalBody.length).isLessThanOrEqualTo(OpenAiCandidateRanker.MAX_REQUEST_BYTES);
		assertThat(OpenAiCandidateRanker.isWithinRequestLimit(
				new byte[OpenAiCandidateRanker.MAX_REQUEST_BYTES])).isTrue();
		assertThat(OpenAiCandidateRanker.isWithinRequestLimit(
				new byte[OpenAiCandidateRanker.MAX_REQUEST_BYTES + 1])).isFalse();
	}

	@Test
	void zeroCandidatesDoesNotCallHttpAndOneCandidateMustHaveRankOne() {
		CandidateRankingResult zero = ranker.rank(request("ko", 0));
		assertThat(zero.rankingStatus()).isEqualTo(CandidateRankingResult.RankingStatus.NOT_RUN);

		server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
				.andRespond(withSuccess(response(true, output(row("c1", 1, true, "한글 이유"))),
						MediaType.APPLICATION_JSON));
		CandidateRankingResult one = ranker.rank(request("ko", 1));
		assertThat(one.candidates()).singleElement().extracting(candidate -> candidate.rank()).isEqualTo(1);
		server.verify();
	}

	@Test
	void acceptsExactlyOneHundredCandidates() {
		String[] rows = IntStream.rangeClosed(1, 100)
				.mapToObj(i -> row("c" + i, i, true, "한글 이유 " + i)).toArray(String[]::new);
		server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
				.andRespond(withSuccess(response(true, output(rows)), MediaType.APPLICATION_JSON));

		CandidateRankingResult result = ranker.rank(request("ko", 100));
		assertThat(result.candidates()).hasSize(100);
		assertThat(result.rankingStatus()).isEqualTo(CandidateRankingResult.RankingStatus.SUCCESS);
		server.verify();
	}

	@Test
	void rejectsReasonThatClearlyViolatesEnglishLanguage() {
		server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
				.andRespond(withSuccess(response(true, output(row("c1", 1, true, "한글 이유"))),
						MediaType.APPLICATION_JSON));

		assertFallback(ranker.rank(request("en", 1)), 1);
		server.verify();
	}

	@Test
	void truncatesInputOnUnicodeCodePointBoundariesWithEllipsis() {
		CandidateRankingInput input = new CandidateRankingInput(
				"c1", "😀".repeat(201), "😀".repeat(301), "😀".repeat(101),
				"😀".repeat(101), LocalDate.of(2026, 9, 28), "😀".repeat(256));
		CandidateRankingRequest request = new CandidateRankingRequest(
				"😀".repeat(2001), "ko", null, null, "😀".repeat(256), List.of(input));

		assertThat(request.description().codePointCount(0, request.description().length())).isEqualTo(2000);
		assertThat(request.lostPlaceText().codePointCount(0, request.lostPlaceText().length())).isEqualTo(255);
		assertThat(input.productName().codePointCount(0, input.productName().length())).isEqualTo(200);
		assertThat(input.subject().codePointCount(0, input.subject().length())).isEqualTo(300);
		assertThat(input.categoryName().codePointCount(0, input.categoryName().length())).isEqualTo(100);
		assertThat(input.colorName().codePointCount(0, input.colorName().length())).isEqualTo(100);
		assertThat(input.storagePlace().codePointCount(0, input.storagePlace().length())).isEqualTo(255);
		assertThat(request.description()).endsWith("…");
		assertThat(input.productName()).endsWith("…");
	}

	private void assertFallback(CandidateRankingResult result, int size) {
		assertThat(result.rankingStatus()).isEqualTo(CandidateRankingResult.RankingStatus.UNAVAILABLE);
		assertThat(result.openAiResultUsed()).isFalse();
		assertThat(result.warnings()).contains("AI_RANKING_UNAVAILABLE");
		assertThat(result.candidates()).hasSize(size);
	}

	private void assertError(SearchErrorCode errorCode) {
		assertThatThrownBy(() -> ranker.rank(request("ko", 1)))
				.isInstanceOfSatisfying(BusinessException.class,
						exception -> assertThat(exception.getErrorCode()).isEqualTo(errorCode));
	}

	private CandidateRankingRequest request(String language, int count) {
		List<CandidateRankingInput> candidates = new ArrayList<>();
		for (int i = 1; i <= count; i++) {
			candidates.add(new CandidateRankingInput("c" + i, i == 1 ? "파란 지갑" : "우산",
					"습득물", "가방", "파랑", LocalDate.of(2026, 9, 20), "서울역"));
		}
		return new CandidateRankingRequest(language.equals("en") ? "blue wallet" : "파란 지갑", language,
				LocalDate.of(2026, 9, 19), LocalDate.of(2026, 9, 21), "서울역", candidates);
	}

	private static String response(boolean reasoningFirst, String output) {
		String message = message(output);
		String reasoning = "{\"type\":\"reasoning\",\"content\":[]}";
		return "{\"status\":\"completed\",\"output\":["
				+ (reasoningFirst ? reasoning + "," + message : message + "," + reasoning) + "]}";
	}

	private static String responseWithTexts() {
		return "{\"status\":\"completed\",\"output\":[" + message(output(row("c1", 1, true, "한글"),
				row("c2", 2, false, "한글"))) + "," + message(output(row("c1", 1, true, "한글"),
				row("c2", 2, false, "한글"))) + "]}";
	}

	private static String refusalResponse() {
		return "{\"status\":\"completed\",\"output\":[{\"type\":\"message\",\"content\":["
				+ "{\"type\":\"refusal\",\"refusal\":\"no\"}]}]}";
	}

	private static String message(String output) {
		String escaped = output.replace("\\", "\\\\").replace("\"", "\\\"");
		return "{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\""
				+ escaped + "\"}]}";
	}

	private static String output(String... rows) {
		return "{\"rankings\":[" + String.join(",", rows) + "]}";
	}

	private static String row(String key, int rank, boolean similar, String reason) {
		return "{\"candidateKey\":\"" + key + "\",\"rank\":" + rank + ",\"isSimilar\":" + similar
				+ ",\"reason\":\"" + reason.replace("\\", "\\\\").replace("\"", "\\\"") + "\"}";
	}

	private static String rowWithRawJsonReason(String key, int rank, boolean similar, String rawJsonReason) {
		return "{\"candidateKey\":\"" + key + "\",\"rank\":" + rank + ",\"isSimilar\":" + similar
				+ ",\"reason\":\"" + rawJsonReason + "\"}";
	}
}
