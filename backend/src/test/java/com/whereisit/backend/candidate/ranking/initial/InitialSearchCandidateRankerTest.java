package com.whereisit.backend.candidate.ranking.initial;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.ExpectedCount.times;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.net.ConnectException;
import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whereisit.backend.candidate.ranking.CandidateRankingInput;
import com.whereisit.backend.candidate.ranking.CandidateRankingRequest;
import com.whereisit.backend.candidate.ranking.CandidateRankingResult;
import com.whereisit.backend.candidate.ranking.RankedCandidate;
import com.whereisit.backend.search.ai.openai.OpenAiProperties;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

class InitialSearchCandidateRankerTest {

	private static final String API_URL = "https://api.openai.com/v1/responses";
	private final ObjectMapper objectMapper = new ObjectMapper();
	private MockRestServiceServer server;
	private InitialSearchCandidateRanker ranker;
	private ListAppender<ILoggingEvent> logs;

	@BeforeEach
	void setUp() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.com/v1");
		server = MockRestServiceServer.bindTo(builder).build();
		ranker = new InitialSearchCandidateRanker(
				new OpenAiProperties("test-only-key", "test-only-model", Duration.ofSeconds(10)),
				builder.build(), new ObjectMapper(), new InitialSearchRuleScorer());
		logs = new ListAppender<>();
		logs.start();
		((Logger) LoggerFactory.getLogger(InitialSearchCandidateRanker.class)).addAppender(logs);
	}

	@AfterEach
	void tearDown() {
		((Logger) LoggerFactory.getLogger(InitialSearchCandidateRanker.class)).detachAppender(logs);
	}

	@Test
	void sendsStrictMinimalRequestWithDynamicScoreSchema() throws Exception {
		AtomicReference<String> body = new AtomicReference<>();
		server.expect(once(), requestTo(API_URL))
				.andExpect(method(HttpMethod.POST))
				.andExpect(header("Authorization", "Bearer test-only-key"))
				.andExpect(request -> body.set(((MockClientHttpRequest) request).getBodyAsString()))
				.andRespond(withSuccess(response(true, output(
						entry("c1", 40, true, "파란 지갑이라는 설명과 일부 일치합니다."),
						entry("c2", 90, false, "설명과 일치하지 않는 후보입니다."))), MediaType.APPLICATION_JSON));

		CandidateRankingResult result = ranker.rank(request("ko", 2));

		assertThat(result.rankingStatus()).isEqualTo(CandidateRankingResult.RankingStatus.SUCCESS);
		assertThat(result.openAiResultUsed()).isTrue();
		server.verify();

		JsonNode root = objectMapper.readTree(body.get());
		assertThat(root.get("model").asText()).isEqualTo("test-only-model");
		assertThat(root.get("store").isBoolean()).isTrue();
		assertThat(root.get("store").booleanValue()).isFalse();
		assertThat(root.has("max_output_tokens")).isFalse();
		JsonNode format = root.get("text").get("format");
		assertThat(format.get("type").asText()).isEqualTo("json_schema");
		assertThat(format.get("strict").booleanValue()).isTrue();
		assertSchema(format.get("schema"), 2);

		String input = root.get("input").asText();
		assertThat(input).contains("\"candidateKey\":\"c1\"", "\"candidateKey\":\"c2\"");
		assertThat(body.get()).doesNotContain("atcId", "fdSn", "sourceType", "imageUrl", "memberId", "lostItemId",
				"foundItemId", "\"rank\"", "rankings", "minItems");
	}

	@Test
	void schemaListsExactlyTwentyRequestScopedKeys() throws Exception {
		JsonNode root = objectMapper.readTree(ranker.serializeRequestBody(request("ko", 20)));

		assertSchema(root.get("text").get("format").get("schema"), 20);
	}

	@Test
	void serverAssignsConsecutiveRanksByAiScoreRegardlessOfResponseOrder() {
		server.expect(once(), requestTo(API_URL))
				.andRespond(withSuccess(response(true, output(
						entry("c1", 40, true, "파란 지갑이라는 설명과 일부 일치합니다."),
						entry("c2", 90, false, "설명과 일치하지 않는 후보입니다."))), MediaType.APPLICATION_JSON));

		List<RankedCandidate> ranked = ranker.rank(request("ko", 2)).candidates();

		assertThat(ranked).extracting(RankedCandidate::candidateKey).containsExactly("c2", "c1");
		assertThat(ranked).extracting(RankedCandidate::rank).containsExactly(1, 2);
		assertThat(ranked.get(0).similar()).isFalse();
		assertThat(ranked.get(0).reason()).isEqualTo("설명과 일치하지 않는 후보입니다.");
		assertThat(ranked.get(1).similar()).isTrue();
		server.verify();
	}

	@Test
	void responseObjectOrderDoesNotAffectFinalRanking() {
		server.expect(once(), requestTo(API_URL))
				.andRespond(withSuccess(response(true, output(
						entry("c1", 70, true, "한글 이유 1"), entry("c2", 80, true, "한글 이유 2"),
						entry("c3", 70, false, "한글 이유 3"))), MediaType.APPLICATION_JSON));
		server.expect(once(), requestTo(API_URL))
				.andRespond(withSuccess(response(false, output(
						entry("c3", 70, false, "한글 이유 3"), entry("c2", 80, true, "한글 이유 2"),
						entry("c1", 70, true, "한글 이유 1"))), MediaType.APPLICATION_JSON));

		List<RankedCandidate> first = ranker.rank(request("ko", 3)).candidates();
		List<RankedCandidate> second = ranker.rank(request("ko", 3)).candidates();

		assertThat(first).isEqualTo(second);
		assertThat(first).extracting(RankedCandidate::candidateKey).containsExactly("c2", "c1", "c3");
		server.verify();
	}

	@Test
	void reasoningAfterMessageIsAlsoAccepted() {
		server.expect(once(), requestTo(API_URL))
				.andRespond(withSuccess(response(false, output(entry("c1", 88, true, "A matching wallet."))),
						MediaType.APPLICATION_JSON));
		assertThat(ranker.rank(request("en", 1)).rankingStatus())
				.isEqualTo(CandidateRankingResult.RankingStatus.SUCCESS);
		server.verify();
	}

	@Test
	void equalAiScoresFallBackToRulePreScore() {
		List<CandidateRankingInput> candidates = List.of(
				input("c1", "우산", LocalDate.of(2026, 9, 20)),
				input("c2", "파란 지갑", LocalDate.of(2026, 9, 1)));
		expectSuccess(entry("c1", 50, true, "한글 이유"), entry("c2", 50, true, "한글 이유"));

		assertThat(ranker.rank(request("파란 지갑", candidates)).candidates())
				.extracting(RankedCandidate::candidateKey).containsExactly("c2", "c1");
	}

	@Test
	void equalAiAndPreScoresFallBackToNewestFoundDateThenRequestOrder() {
		List<CandidateRankingInput> candidates = List.of(
				input("c1", "우산", null),
				input("c2", "우산", LocalDate.of(2026, 9, 10)),
				input("c3", "우산", LocalDate.of(2026, 9, 20)),
				input("c4", "우산", LocalDate.of(2026, 9, 20)));
		expectSuccess(entry("c1", 60, true, "한글"), entry("c2", 60, true, "한글"),
				entry("c3", 60, true, "한글"), entry("c4", 60, true, "한글"));

		List<RankedCandidate> ranked = ranker.rank(request("파란 지갑", candidates)).candidates();

		assertThat(ranked).extracting(RankedCandidate::candidateKey).containsExactly("c3", "c4", "c2", "c1");
		assertThat(ranked).extracting(RankedCandidate::rank).containsExactly(1, 2, 3, 4);
	}

	@Test
	void acceptsExactlyTwentyCandidatesWithConsecutiveServerRanks() {
		String[] entries = IntStream.rangeClosed(1, 20)
				.mapToObj(i -> entry("c" + i, i * 5, i % 2 == 0, "한글 이유 " + i)).toArray(String[]::new);
		expectSuccess(entries);

		CandidateRankingResult result = ranker.rank(request("ko", 20));

		assertThat(result.rankingStatus()).isEqualTo(CandidateRankingResult.RankingStatus.SUCCESS);
		assertThat(result.candidates()).extracting(RankedCandidate::rank)
				.containsExactlyElementsOf(IntStream.rangeClosed(1, 20).boxed().toList());
		assertThat(result.candidates()).extracting(RankedCandidate::candidateKey)
				.containsExactlyElementsOf(IntStream.iterate(20, i -> i - 1).limit(20).mapToObj(i -> "c" + i).toList());
	}

	@Test
	void acceptsReasonAtMaximumLength() {
		expectSuccess(entry("c1", 10, true, "한".repeat(InitialSearchCandidateRanker.MAX_REASON_CODE_POINTS)));

		assertThat(ranker.rank(request("ko", 1)).rankingStatus())
				.isEqualTo(CandidateRankingResult.RankingStatus.SUCCESS);
	}

	@ParameterizedTest
	@MethodSource("invalidResponses")
	void invalidResponsesAreClassified(String response, RankingFailureCategory category) {
		server.expect(once(), requestTo(API_URL)).andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

		assertFailure(() -> ranker.evaluate(request("ko", 2)), category);
		server.verify();
	}

	@ParameterizedTest
	@MethodSource("invalidResponses")
	void invalidResponsesDiscardAllAiOutputAndFallbackWithoutRetry(String response, RankingFailureCategory category) {
		server.expect(once(), requestTo(API_URL)).andRespond(withSuccess(response, MediaType.APPLICATION_JSON));

		assertFallback(ranker.rank(request("ko", 2)), 2);
		assertThat(logs.list).singleElement().extracting(ILoggingEvent::getFormattedMessage)
				.asString().contains("category=" + category.name(), "candidateCount=2", "requestBytes=");
		server.verify();
	}

	private static Stream<Arguments> invalidResponses() {
		String valid2 = entry("c2", 10, false, "한글 이유");
		return Stream.of(
				Arguments.of("", RankingFailureCategory.ENVELOPE_INVALID),
				Arguments.of("not-json", RankingFailureCategory.ENVELOPE_INVALID),
				Arguments.of("{}", RankingFailureCategory.ENVELOPE_INVALID),
				Arguments.of("[]", RankingFailureCategory.ENVELOPE_INVALID),
				Arguments.of("{\"status\":\"failed\",\"output\":[]}", RankingFailureCategory.ENVELOPE_INVALID),
				Arguments.of("{\"status\":\"incomplete\",\"output\":[]}", RankingFailureCategory.INCOMPLETE),
				Arguments.of("{\"status\":\"incomplete\",\"incomplete_details\":{\"reason\":\"content_filter\"},"
						+ "\"output\":[]}", RankingFailureCategory.INCOMPLETE),
				Arguments.of("{\"status\":\"incomplete\",\"incomplete_details\":{\"reason\":\"max_output_tokens\"},"
						+ "\"output\":[]}", RankingFailureCategory.OUTPUT_LIMIT),
				Arguments.of("{\"status\":\"completed\",\"output\":[{\"type\":\"reasoning\",\"content\":[]}]}",
						RankingFailureCategory.ENVELOPE_INVALID),
				Arguments.of("{\"status\":\"completed\",\"output\":{}}", RankingFailureCategory.ENVELOPE_INVALID),
				Arguments.of("{\"status\":\"completed\",\"output\":[{\"type\":\"function_call\"}]}",
						RankingFailureCategory.ENVELOPE_INVALID),
				Arguments.of("{\"status\":\"completed\",\"output\":[{\"type\":\"message\",\"content\":["
						+ "{\"type\":\"image\"}]}]}", RankingFailureCategory.ENVELOPE_INVALID),
				Arguments.of(refusalResponse(), RankingFailureCategory.REFUSAL),
				Arguments.of(responseWithTwoMessages(), RankingFailureCategory.ENVELOPE_INVALID),
				Arguments.of(responseWithTwoTextParts(), RankingFailureCategory.ENVELOPE_INVALID),
				Arguments.of(response(true, ""), RankingFailureCategory.JSON_INVALID),
				Arguments.of(response(true, "not-json"), RankingFailureCategory.JSON_INVALID),
				Arguments.of(response(true, output(entry("c1", 1, true, "한글"), valid2) + " {}"),
						RankingFailureCategory.JSON_INVALID),
				Arguments.of(response(true, output(entry("c1", 1, true, "한글"), entry("c1", 2, true, "한글"), valid2)),
						RankingFailureCategory.JSON_INVALID),
				Arguments.of(response(true, output(rawEntry("c1",
						"{\"score\":1,\"score\":2,\"isSimilar\":true,\"reason\":\"한글\"}"), valid2)),
						RankingFailureCategory.JSON_INVALID),
				Arguments.of(response(true, "[]"), RankingFailureCategory.SCHEMA_INVALID),
				Arguments.of(response(true, "{\"candidates\":[]}"), RankingFailureCategory.SCHEMA_INVALID),
				Arguments.of(response(true, "{\"rankings\":[]}"), RankingFailureCategory.SCHEMA_INVALID),
				Arguments.of(response(true, "{\"candidates\":{" + entry("c1", 1, true, "한글") + "," + valid2
						+ "},\"extra\":1}"), RankingFailureCategory.SCHEMA_INVALID),
				Arguments.of(response(true, output(valid2)), RankingFailureCategory.CANDIDATE_SET_INVALID),
				Arguments.of(response(true, output(entry("c1", 1, true, "한글"), valid2, entry("c3", 1, true, "한글"))),
						RankingFailureCategory.CANDIDATE_SET_INVALID),
				Arguments.of(response(true, output(entry("C1", 1, true, "한글"), valid2)),
						RankingFailureCategory.CANDIDATE_SET_INVALID),
				Arguments.of(response(true, output(rawEntry("c1",
						"{\"score\":1,\"isSimilar\":true,\"reason\":\"한글\",\"rank\":1}"), valid2)),
						RankingFailureCategory.SCHEMA_INVALID),
				Arguments.of(response(true, output(rawEntry("c1", "5"), valid2)), RankingFailureCategory.SCHEMA_INVALID),
				Arguments.of(response(true, output(rawEntry("c1", "{\"isSimilar\":true,\"reason\":\"한글\"}"), valid2)),
						RankingFailureCategory.SCHEMA_INVALID),
				Arguments.of(response(true, output(rawEntry("c1",
						"{\"score\":\"90\",\"isSimilar\":true,\"reason\":\"한글\"}"), valid2)),
						RankingFailureCategory.SCORE_INVALID),
				Arguments.of(response(true, output(rawEntry("c1",
						"{\"score\":90.5,\"isSimilar\":true,\"reason\":\"한글\"}"), valid2)),
						RankingFailureCategory.SCORE_INVALID),
				Arguments.of(response(true, output(rawEntry("c1",
						"{\"score\":90.0,\"isSimilar\":true,\"reason\":\"한글\"}"), valid2)),
						RankingFailureCategory.SCORE_INVALID),
				Arguments.of(response(true, output(entry("c1", -1, true, "한글"), valid2)),
						RankingFailureCategory.SCORE_INVALID),
				Arguments.of(response(true, output(entry("c1", 101, true, "한글"), valid2)),
						RankingFailureCategory.SCORE_INVALID),
				Arguments.of(response(true, output(rawEntry("c1",
						"{\"score\":99999999999,\"isSimilar\":true,\"reason\":\"한글\"}"), valid2)),
						RankingFailureCategory.SCORE_INVALID),
				Arguments.of(response(true, output(rawEntry("c1",
						"{\"score\":1,\"isSimilar\":\"true\",\"reason\":\"한글\"}"), valid2)),
						RankingFailureCategory.SCHEMA_INVALID),
				Arguments.of(response(true, output(rawEntry("c1",
						"{\"score\":1,\"isSimilar\":true,\"reason\":5}"), valid2)),
						RankingFailureCategory.SCHEMA_INVALID),
				Arguments.of(response(true, output(entry("c1", 1, true, " "), valid2)),
						RankingFailureCategory.REASON_INVALID),
				Arguments.of(response(true, output(entry("c1", 1, true, "한".repeat(501)), valid2)),
						RankingFailureCategory.REASON_INVALID),
				Arguments.of(response(true, output(entry("c1", 1, true, "English only"), valid2)),
						RankingFailureCategory.REASON_INVALID),
				Arguments.of(response(true, output(entry("c1", 1, true, "<b>한글</b>"), valid2)),
						RankingFailureCategory.REASON_INVALID),
				Arguments.of(response(true, output(rawEntry("c1",
						"{\"score\":1,\"isSimilar\":true,\"reason\":\"한글\\u0001이유\"}"), valid2)),
						RankingFailureCategory.REASON_INVALID));
	}

	@Test
	void rejectsReasonThatClearlyViolatesEnglishLanguage() {
		server.expect(once(), requestTo(API_URL))
				.andRespond(withSuccess(response(true, output(entry("c1", 1, true, "한글 이유"))),
						MediaType.APPLICATION_JSON));

		assertFailure(() -> ranker.evaluate(request("en", 1)), RankingFailureCategory.REASON_INVALID);
		server.verify();
	}

	@ParameterizedTest
	@MethodSource("fallbackHttpErrors")
	void genericHttpErrorFallsBackWithoutRetry(HttpStatus status) {
		server.expect(once(), requestTo(API_URL)).andRespond(withStatus(status).body("SENSITIVE-ERROR-BODY"));

		CandidateRankingResult result = ranker.rank(request("ko", 1));

		assertFallback(result, 1);
		assertThat(result.candidates()).extracting(RankedCandidate::candidateKey).containsExactly("c1");
		assertThat(logs.list).singleElement().extracting(ILoggingEvent::getFormattedMessage).asString()
				.contains("category=HTTP_ERROR", "httpStatus=" + status.value())
				.doesNotContain("SENSITIVE-ERROR-BODY");
		server.verify();
	}

	private static Stream<Arguments> fallbackHttpErrors() {
		return Stream.of(HttpStatus.BAD_REQUEST, HttpStatus.UNAUTHORIZED, HttpStatus.FORBIDDEN,
				HttpStatus.INTERNAL_SERVER_ERROR).map(Arguments::of);
	}

	@ParameterizedTest
	@MethodSource("timeoutHttpErrors")
	void timeoutHttpErrorFallsBackWithoutRetry(HttpStatus status) {
		server.expect(once(), requestTo(API_URL)).andRespond(withStatus(status));

		assertFallback(ranker.rank(request("ko", 1)), 1);
		assertThat(logs.list).singleElement().extracting(ILoggingEvent::getFormattedMessage).asString()
				.contains("category=TIMEOUT", "httpStatus=" + status.value());
		server.verify();
	}

	private static Stream<Arguments> timeoutHttpErrors() {
		return Stream.of(HttpStatus.REQUEST_TIMEOUT, HttpStatus.GATEWAY_TIMEOUT).map(Arguments::of);
	}

	@Test
	void rateLimitFallsBackWithoutRetry() {
		server.expect(once(), requestTo(API_URL)).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

		assertFallback(ranker.rank(request("ko", 1)), 1);
		assertThat(logs.list).singleElement().extracting(ILoggingEvent::getFormattedMessage).asString()
				.contains("category=RATE_LIMITED", "httpStatus=429");
		server.verify();
	}

	@Test
	void httpStatusCategoriesAreClassified() {
		assertThat(InitialSearchCandidateRanker.httpFailureCategory(400)).isEqualTo(RankingFailureCategory.HTTP_ERROR);
		assertThat(InitialSearchCandidateRanker.httpFailureCategory(401)).isEqualTo(RankingFailureCategory.HTTP_ERROR);
		assertThat(InitialSearchCandidateRanker.httpFailureCategory(403)).isEqualTo(RankingFailureCategory.HTTP_ERROR);
		assertThat(InitialSearchCandidateRanker.httpFailureCategory(500)).isEqualTo(RankingFailureCategory.HTTP_ERROR);
		assertThat(InitialSearchCandidateRanker.httpFailureCategory(408)).isEqualTo(RankingFailureCategory.TIMEOUT);
		assertThat(InitialSearchCandidateRanker.httpFailureCategory(504)).isEqualTo(RankingFailureCategory.TIMEOUT);
		assertThat(InitialSearchCandidateRanker.httpFailureCategory(429)).isEqualTo(RankingFailureCategory.RATE_LIMITED);
	}

	@Test
	void socketTimeoutFallsBackWithoutRetry() {
		server.expect(once(), requestTo(API_URL))
				.andRespond(request -> {
					throw new ResourceAccessException("sanitized-test-timeout", new SocketTimeoutException());
				});

		assertFallback(ranker.rank(request("ko", 1)), 1);
		assertThat(logs.list).singleElement().extracting(ILoggingEvent::getFormattedMessage).asString()
				.contains("category=TIMEOUT");
		server.verify();
	}

	@Test
	void httpClientTimeoutIsClassifiedAsTimeout() {
		server.expect(once(), requestTo(API_URL))
				.andRespond(request -> {
					throw new ResourceAccessException("sanitized-test-timeout", new HttpTimeoutException("timeout"));
				});

		assertFailure(() -> ranker.evaluate(request("ko", 1)), RankingFailureCategory.TIMEOUT);
		server.verify();
	}

	@Test
	void connectionFailureFallsBackAsConnectionError() {
		server.expect(once(), requestTo(API_URL))
				.andRespond(request -> {
					throw new ResourceAccessException("SENSITIVE-CONNECTION-MESSAGE", new ConnectException("refused"));
				});

		assertFallback(ranker.rank(request("ko", 1)), 1);
		assertThat(logs.list).singleElement().extracting(ILoggingEvent::getFormattedMessage).asString()
				.contains("category=CONNECTION_ERROR")
				.doesNotContain("SENSITIVE-CONNECTION-MESSAGE", "refused");
		server.verify();
	}

	@Test
	void missingConfigurationDoesNotCallHttp() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.com/v1");
		MockRestServiceServer missingServer = MockRestServiceServer.bindTo(builder).build();
		InitialSearchCandidateRanker missing = new InitialSearchCandidateRanker(
				new OpenAiProperties("", "", Duration.ofSeconds(10)), builder.build(),
				new ObjectMapper(), new InitialSearchRuleScorer());

		assertFailure(() -> missing.evaluate(request("ko", 1)), RankingFailureCategory.CONFIG_MISSING);
		assertFallback(missing.rank(request("ko", 1)), 1);
		missingServer.verify();
	}

	@Test
	void unsupportedLanguageOrKeysAreRejectedBeforeHttp() {
		assertFailure(() -> ranker.evaluate(request("ja", 1)), RankingFailureCategory.REQUEST_INVALID);
		assertFailure(() -> ranker.evaluate(request("파란 지갑",
				List.of(input("x1", "지갑", null)))), RankingFailureCategory.REQUEST_INVALID);
		assertFailure(() -> ranker.evaluate(request("파란 지갑",
				List.of(input("c2", "지갑", null)))), RankingFailureCategory.REQUEST_INVALID);
		server.verify();
	}

	@Test
	void moreThanTwentyCandidatesAreNeverSentToOpenAi() {
		assertFailure(() -> ranker.evaluate(request("ko", 21)), RankingFailureCategory.REQUEST_TOO_LARGE);
		assertFallback(ranker.rank(request("ko", 21)), 21);
		assertFallback(ranker.rank(request("ko", 101)), 101);
		server.verify();
	}

	@Test
	void oversizedTwentyCandidateBodyDoesNotCallHttp() {
		String heavy = "\u0001";
		List<CandidateRankingInput> huge = IntStream.rangeClosed(1, 20)
				.mapToObj(i -> new CandidateRankingInput("c" + i, heavy.repeat(200), heavy.repeat(300),
						heavy.repeat(100), heavy.repeat(100), LocalDate.of(2026, 9, 28), heavy.repeat(255)))
				.toList();
		CandidateRankingRequest request = new CandidateRankingRequest(
				heavy.repeat(2000), "ko", null, null, heavy.repeat(255), huge);

		assertThatThrownBy(() -> ranker.evaluate(request))
				.isInstanceOfSatisfying(RankingFailure.class, failure -> {
					assertThat(failure.category()).isEqualTo(RankingFailureCategory.REQUEST_TOO_LARGE);
					assertThat(failure.requestBytes()).isGreaterThan(InitialSearchCandidateRanker.MAX_REQUEST_BYTES);
				});
		assertFallback(ranker.rank(request), 20);
		server.verify();
	}

	@Test
	void finalSerializedBodyUsesInclusiveOneHundredTwentyEightKibLimit() throws Exception {
		byte[] normalBody = ranker.serializeRequestBody(request("ko", 20));

		assertThat(normalBody.length).isLessThanOrEqualTo(InitialSearchCandidateRanker.MAX_REQUEST_BYTES);
		assertThat(InitialSearchCandidateRanker.isWithinRequestLimit(
				new byte[InitialSearchCandidateRanker.MAX_REQUEST_BYTES])).isTrue();
		assertThat(InitialSearchCandidateRanker.isWithinRequestLimit(
				new byte[InitialSearchCandidateRanker.MAX_REQUEST_BYTES + 1])).isFalse();
	}

	@Test
	void zeroCandidatesDoesNotCallHttpAndOneCandidateMustHaveRankOne() {
		CandidateRankingResult zero = ranker.rank(request("ko", 0));
		assertThat(zero.rankingStatus()).isEqualTo(CandidateRankingResult.RankingStatus.NOT_RUN);
		assertThat(zero.candidates()).isEmpty();

		expectSuccess(entry("c1", 0, false, "한글 이유"));
		CandidateRankingResult one = ranker.rank(request("ko", 1));
		assertThat(one.candidates()).singleElement().extracting(RankedCandidate::rank).isEqualTo(1);
	}

	@Test
	void sameRequestAndResponseAlwaysProduceSameRanking() {
		server.expect(times(3), requestTo(API_URL))
				.andRespond(withSuccess(response(true, output(IntStream.rangeClosed(1, 20)
						.mapToObj(i -> entry("c" + i, i % 3 * 10, true, "한글 이유 " + i)).toArray(String[]::new))),
						MediaType.APPLICATION_JSON));

		List<RankedCandidate> first = ranker.rank(request("ko", 20)).candidates();
		assertThat(ranker.rank(request("ko", 20)).candidates()).isEqualTo(first);
		assertThat(ranker.rank(request("ko", 20)).candidates()).isEqualTo(first);
		server.verify();
	}

	@Test
	void diagnosticsNeverContainSecretsPromptCandidateValuesOrResponseText() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.com/v1");
		MockRestServiceServer secretServer = MockRestServiceServer.bindTo(builder).build();
		InitialSearchCandidateRanker secretRanker = new InitialSearchCandidateRanker(
				new OpenAiProperties("sk-SECRET-KEY-VALUE", "test-only-model", Duration.ofSeconds(10)),
				builder.build(), new ObjectMapper(), new InitialSearchRuleScorer());
		secretServer.expect(once(), requestTo(API_URL)).andRespond(withSuccess(
				"{\"status\":\"completed\",\"output\":[{\"type\":\"message\",\"content\":["
						+ "{\"type\":\"refusal\",\"refusal\":\"SENSITIVE-REFUSAL-TEXT\"}]}]}",
				MediaType.APPLICATION_JSON));
		secretServer.expect(once(), requestTo(API_URL)).andRespond(withStatus(HttpStatus.UNAUTHORIZED)
				.body("{\"error\":\"SENSITIVE-ERROR sk-SECRET-KEY-VALUE\"}"));
		CandidateRankingRequest request = new CandidateRankingRequest(
				"홍길동 010-1234-5678 검은 지갑", "ko", null, null, "SECRET-PLACE",
				List.of(new CandidateRankingInput("c1", "SECRET-PRODUCT", "SECRET-SUBJECT", null, null, null,
						"SECRET-STORAGE 02-123-4567")));

		assertThatThrownBy(() -> secretRanker.evaluate(request))
				.isInstanceOfSatisfying(RankingFailure.class, failure -> {
					assertThat(failure.getMessage()).isEqualTo("REFUSAL");
					assertThat(failure.getCause()).isNull();
					assertThat(failure.getStackTrace()).isEmpty();
				});
		secretRanker.rank(request);

		assertThat(logs.list).isNotEmpty();
		for (ILoggingEvent event : logs.list) {
			assertThat(event.getFormattedMessage()).doesNotContain("sk-SECRET-KEY-VALUE", "Bearer", "홍길동",
					"010-1234-5678", "02-123-4567", "SECRET-PRODUCT", "SECRET-SUBJECT", "SECRET-STORAGE",
					"SECRET-PLACE", "SENSITIVE", "검은 지갑");
			assertThat(event.getThrowableProxy()).isNull();
		}
		secretServer.verify();
	}

	@Test
	void unknownResponseStatusAndIncompleteReasonAreNotLoggedVerbatim() {
		server.expect(once(), requestTo(API_URL)).andRespond(withSuccess(
				"{\"status\":\"incomplete\",\"incomplete_details\":{\"reason\":\"SENSITIVE-REASON\"}}",
				MediaType.APPLICATION_JSON));

		assertThatThrownBy(() -> ranker.evaluate(request("ko", 1)))
				.isInstanceOfSatisfying(RankingFailure.class, failure -> {
					assertThat(failure.category()).isEqualTo(RankingFailureCategory.INCOMPLETE);
					assertThat(failure.responseStatus()).isEqualTo("incomplete");
					assertThat(failure.incompleteReason()).isEqualTo("other");
				});
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

	private void assertSchema(JsonNode schema, int size) {
		List<String> keys = IntStream.rangeClosed(1, size).mapToObj(i -> "c" + i).toList();
		assertThat(schema.get("type").asText()).isEqualTo("object");
		assertThat(schema.get("additionalProperties").booleanValue()).isFalse();
		assertThat(texts(schema.get("required"))).containsExactly("candidates");
		assertThat(fieldNames(schema.get("properties"))).containsExactly("candidates");

		JsonNode candidates = schema.get("properties").get("candidates");
		assertThat(candidates.get("type").asText()).isEqualTo("object");
		assertThat(candidates.get("additionalProperties").booleanValue()).isFalse();
		assertThat(fieldNames(candidates.get("properties"))).containsExactlyElementsOf(keys);
		assertThat(texts(candidates.get("required"))).containsExactlyElementsOf(keys);

		for (String key : keys) {
			JsonNode candidate = candidates.get("properties").get(key);
			assertThat(candidate.get("type").asText()).isEqualTo("object");
			assertThat(candidate.get("additionalProperties").booleanValue()).isFalse();
			assertThat(texts(candidate.get("required"))).containsExactly("score", "isSimilar", "reason");
			assertThat(fieldNames(candidate.get("properties"))).containsExactly("score", "isSimilar", "reason");
			JsonNode score = candidate.get("properties").get("score");
			assertThat(score.get("type").asText()).isEqualTo("integer");
			assertThat(score.get("minimum").intValue()).isZero();
			assertThat(score.get("maximum").intValue()).isEqualTo(100);
			assertThat(candidate.get("properties").get("isSimilar").get("type").asText()).isEqualTo("boolean");
			JsonNode reason = candidate.get("properties").get("reason");
			assertThat(reason.get("type").asText()).isEqualTo("string");
			assertThat(reason.get("minLength").intValue()).isEqualTo(1);
			assertThat(reason.get("maxLength").intValue()).isEqualTo(500);
		}
	}

	private List<String> texts(JsonNode array) {
		List<String> values = new ArrayList<>();
		array.forEach(node -> values.add(node.asText()));
		return values;
	}

	private List<String> fieldNames(JsonNode node) {
		List<String> names = new ArrayList<>();
		Iterator<String> iterator = node.fieldNames();
		iterator.forEachRemaining(names::add);
		return names;
	}

	private void expectSuccess(String... entries) {
		server.expect(once(), requestTo(API_URL))
				.andRespond(withSuccess(response(true, output(entries)), MediaType.APPLICATION_JSON));
	}

	private void assertFailure(Runnable action, RankingFailureCategory category) {
		assertThatThrownBy(action::run)
				.isInstanceOfSatisfying(RankingFailure.class, failure -> {
					assertThat(failure.category()).isEqualTo(category);
					assertThat(failure.getMessage()).isEqualTo(category.name());
					assertThat(failure.getCause()).isNull();
				});
	}

	private void assertFallback(CandidateRankingResult result, int size) {
		assertThat(result.rankingStatus()).isEqualTo(CandidateRankingResult.RankingStatus.UNAVAILABLE);
		assertThat(result.openAiResultUsed()).isFalse();
		assertThat(result.warnings()).containsExactly("AI_RANKING_UNAVAILABLE");
		assertThat(result.candidates()).hasSize(size);
		assertThat(result.candidates()).extracting(RankedCandidate::rank)
				.containsExactlyElementsOf(IntStream.rangeClosed(1, size).boxed().toList());
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

	private CandidateRankingRequest request(String description, List<CandidateRankingInput> candidates) {
		return new CandidateRankingRequest(description, "ko", null, null, null, candidates);
	}

	private CandidateRankingInput input(String key, String productName, LocalDate foundDate) {
		return new CandidateRankingInput(key, productName, null, null, null, foundDate, null);
	}

	private static String response(boolean reasoningFirst, String output) {
		String message = message(output);
		String reasoning = "{\"type\":\"reasoning\",\"content\":[]}";
		return "{\"status\":\"completed\",\"output\":["
				+ (reasoningFirst ? reasoning + "," + message : message + "," + reasoning) + "]}";
	}

	private static String responseWithTwoMessages() {
		String output = output(entry("c1", 1, true, "한글"), entry("c2", 2, false, "한글"));
		return "{\"status\":\"completed\",\"output\":[" + message(output) + "," + message(output) + "]}";
	}

	private static String responseWithTwoTextParts() {
		String text = escape(output(entry("c1", 1, true, "한글"), entry("c2", 2, false, "한글")));
		return "{\"status\":\"completed\",\"output\":[{\"type\":\"message\",\"content\":["
				+ "{\"type\":\"output_text\",\"text\":\"" + text + "\"},"
				+ "{\"type\":\"output_text\",\"text\":\"" + text + "\"}]}]}";
	}

	private static String refusalResponse() {
		return "{\"status\":\"completed\",\"output\":[{\"type\":\"message\",\"content\":["
				+ "{\"type\":\"refusal\",\"refusal\":\"no\"}]}]}";
	}

	private static String message(String output) {
		return "{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\"" + escape(output) + "\"}]}";
	}

	private static String escape(String value) {
		return value.replace("\\", "\\\\").replace("\"", "\\\"");
	}

	private static String output(String... entries) {
		return "{\"candidates\":{" + String.join(",", entries) + "}}";
	}

	private static String entry(String key, int score, boolean similar, String reason) {
		return rawEntry(key, "{\"score\":" + score + ",\"isSimilar\":" + similar
				+ ",\"reason\":\"" + escape(reason) + "\"}");
	}

	private static String rawEntry(String key, String rawJson) {
		return "\"" + key + "\":" + rawJson;
	}
}
