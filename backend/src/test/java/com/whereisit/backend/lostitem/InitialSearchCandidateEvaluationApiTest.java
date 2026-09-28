package com.whereisit.backend.lostitem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.net.SocketTimeoutException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.client.MockClientHttpRequest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;
import com.whereisit.backend.candidate.ranking.openai.OpenAiCandidateRanker;
import com.whereisit.backend.candidate.repository.LostItemCandidateRepository;
import com.whereisit.backend.founditem.client.FoundItemListEntry;
import com.whereisit.backend.founditem.client.FoundItemLookupClient;
import com.whereisit.backend.founditem.client.PortalFoundItemNameStorageClient;
import com.whereisit.backend.founditem.entity.FoundItemSourceType;
import com.whereisit.backend.global.error.BusinessException;
import com.whereisit.backend.lostitem.repository.LostItemRepository;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractionResult;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractor;
import com.whereisit.backend.search.error.SearchErrorCode;
import com.whereisit.backend.support.ApiTestSupport;

/** API-05의 실제 후보 평가기(InitialSearchCandidateRanker)를 Mock OpenAI HTTP로 검증한다. 실제 네트워크는 호출하지 않는다. */
@DisplayName("API-05 candidate evaluation with mocked OpenAI HTTP")
@TestPropertySource(properties = {"app.openai.api-key=test-only-key", "app.openai.model=test-only-model"})
@Sql(scripts = "/sql/cleanup-search-test-data.sql",
		config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
		executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/cleanup-search-test-data.sql",
		config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
		executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class InitialSearchCandidateEvaluationApiTest extends ApiTestSupport {

	private static final String RESPONSES_URL = "https://api.openai.com/v1/responses";
	private static MockRestServiceServer openAiServer;

	@TestBean(name = "openAiRestClient", methodName = "mockOpenAiRestClient")
	private RestClient openAiRestClient;

	@MockitoBean(name = "policeFoundItemLookupClient")
	private FoundItemLookupClient policeClient;

	@MockitoBean(name = "portalFoundItemLookupClient")
	private FoundItemLookupClient legacyPortalClient;

	@MockitoBean
	private PortalFoundItemNameStorageClient portalNameStorageClient;

	@MockitoBean
	private AiSearchConditionExtractor aiSearchConditionExtractor;

	/** Legacy API-11·14 랭커. API-05는 이 Bean을 호출하지 않아야 한다. */
	@MockitoBean
	private OpenAiCandidateRanker legacyCandidateRanker;

	@Autowired private LostItemCandidateRepository candidateRepository;
	@Autowired private LostItemRepository lostItemRepository;

	static RestClient mockOpenAiRestClient() {
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.com/v1");
		openAiServer = MockRestServiceServer.bindTo(builder).build();
		return builder.build();
	}

	@BeforeEach
	void setUp() {
		openAiServer.reset();
		when(policeClient.sourceType()).thenReturn(FoundItemSourceType.POLICE);
		when(legacyPortalClient.sourceType()).thenReturn(FoundItemSourceType.PORTAL);
		when(aiSearchConditionExtractor.extract(any())).thenReturn(new AiSearchConditionExtractionResult(
				null, null, "서울역", "지갑", "서울역 유실물센터", "검색 조건을 확인했습니다."));
		when(portalNameStorageClient.search(any())).thenReturn(List.of(
				found("E-1", "001", "우산 A"), found("E-2", "002", "우산 B"), found("E-3", "003", "우산 C")));
	}

	@AfterEach
	void verifyOpenAi() {
		openAiServer.verify();
		verify(legacyCandidateRanker, never()).rank(any());
		assertThat(candidateRepository.count()).isZero();
	}

	@Test
	void aiScoresAreRankedByServerAndReturnedAsTransientCandidates() throws Exception {
		AtomicReference<String> body = new AtomicReference<>();
		openAiServer.expect(once(), requestTo(RESPONSES_URL))
				.andExpect(request -> body.set(((MockClientHttpRequest) request).getBodyAsString()))
				.andRespond(withSuccess(response(entry("c1", 30, false, "물품 종류가 다릅니다."),
						entry("c2", 90, true, "색상과 장소가 일치합니다."), entry("c3", 60, true, "장소가 일치합니다.")),
						MediaType.APPLICATION_JSON));

		createLostItem("success@example.test")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.lookupStatus").value("COMPLETE"))
				.andExpect(jsonPath("$.data.rankingStatus").value("SUCCESS"))
				.andExpect(jsonPath("$.data.persisted").value(false))
				.andExpect(jsonPath("$.data.warnings.length()").value(0))
				.andExpect(jsonPath("$.data.candidates.length()").value(3))
				.andExpect(jsonPath("$.data.candidates[0].foundItem.atcId").value("E-2"))
				.andExpect(jsonPath("$.data.candidates[0].foundItem.openId").value("E-2"))
				.andExpect(jsonPath("$.data.candidates[0].foundItem.fdSn").value("002"))
				.andExpect(jsonPath("$.data.candidates[0].rank").value(1))
				.andExpect(jsonPath("$.data.candidates[0].isSimilar").value(true))
				.andExpect(jsonPath("$.data.candidates[0].reason").value("색상과 장소가 일치합니다."))
				.andExpect(jsonPath("$.data.candidates[0].candidateId").doesNotExist())
				.andExpect(jsonPath("$.data.candidates[0].isCurrent").value(false))
				.andExpect(jsonPath("$.data.candidates[0].isBaseline").value(false))
				.andExpect(jsonPath("$.data.candidates[0].score").doesNotExist())
				.andExpect(jsonPath("$.data.candidates[1].foundItem.atcId").value("E-3"))
				.andExpect(jsonPath("$.data.candidates[1].rank").value(2))
				.andExpect(jsonPath("$.data.candidates[2].foundItem.atcId").value("E-1"))
				.andExpect(jsonPath("$.data.candidates[2].rank").value(3));

		JsonNode schema = objectMapper.readTree(body.get()).get("text").get("format").get("schema")
				.get("properties").get("candidates");
		assertThat(schema.get("required")).extracting(JsonNode::asText).containsExactly("c1", "c2", "c3");
		assertThat(body.get()).doesNotContain("E-1", "E-2", "E-3", "\"rank\"");
	}

	@ParameterizedTest
	@EnumSource(value = HttpStatus.class, names = {"REQUEST_TIMEOUT", "GATEWAY_TIMEOUT", "TOO_MANY_REQUESTS",
			"INTERNAL_SERVER_ERROR", "BAD_REQUEST"})
	void rankingHttpFailureKeepsCreatedResponseWithRuleBasedFallback(HttpStatus status) throws Exception {
		openAiServer.expect(once(), requestTo(RESPONSES_URL)).andRespond(withStatus(status));

		assertFallback(createLostItem("fallback-" + status.value() + "@example.test"));
	}

	@Test
	void rankingTimeoutKeepsCreatedResponseWithRuleBasedFallback() throws Exception {
		openAiServer.expect(once(), requestTo(RESPONSES_URL)).andRespond(request -> {
			throw new ResourceAccessException("sanitized-test-timeout", new SocketTimeoutException());
		});

		assertFallback(createLostItem("timeout@example.test"));
	}

	@Test
	void moreThanTwentyCandidatesSendOnlyDeterministicTopTwentyToOpenAi() throws Exception {
		when(portalNameStorageClient.search(any())).thenReturn(IntStream.range(0, 72)
				.mapToObj(i -> found("M-%03d".formatted(i), "%03d".formatted(i), "우산 " + i)).toList());
		AtomicReference<String> body = new AtomicReference<>();
		openAiServer.expect(once(), requestTo(RESPONSES_URL))
				.andExpect(request -> body.set(((MockClientHttpRequest) request).getBodyAsString()))
				.andRespond(withSuccess(response(IntStream.rangeClosed(1, 20)
						.mapToObj(i -> entry("c" + i, 50, true, "한글 이유 " + i)).toArray(String[]::new)),
						MediaType.APPLICATION_JSON));

		var result = createLostItem("limit@example.test")
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.rankingStatus").value("SUCCESS"))
				.andExpect(jsonPath("$.data.warnings.length()").value(1))
				.andExpect(jsonPath("$.data.warnings[0]").value("RESULT_LIMIT_REACHED"))
				.andExpect(jsonPath("$.data.candidates.length()").value(20))
				.andExpect(jsonPath("$.data.candidates[0].foundItem.atcId").value("M-000"))
				.andExpect(jsonPath("$.data.candidates[19].foundItem.atcId").value("M-019"))
				.andReturn();

		JsonNode required = objectMapper.readTree(body.get()).get("text").get("format").get("schema")
				.get("properties").get("candidates").get("required");
		assertThat(required).extracting(JsonNode::asText)
				.containsExactlyElementsOf(IntStream.rangeClosed(1, 20).mapToObj(i -> "c" + i).toList());
		assertRanks(result.getResponse().getContentAsString(), 20);
	}

	@Test
	void searchConditionTimeoutKeepsExisting504AndSkipsPortalAndRanking() throws Exception {
		when(aiSearchConditionExtractor.extract(any())).thenThrow(new BusinessException(SearchErrorCode.SEARCH_TIMEOUT));

		createLostItem("condition-timeout@example.test")
				.andExpect(status().isGatewayTimeout())
				.andExpect(jsonPath("$.error.code").value("SEARCH_TIMEOUT"));
		verify(portalNameStorageClient, never()).search(any());
		assertThat(lostItemRepository.count()).isOne();
	}

	@Test
	void searchConditionRateLimitKeepsExisting429AndSkipsPortalAndRanking() throws Exception {
		when(aiSearchConditionExtractor.extract(any())).thenThrow(new BusinessException(SearchErrorCode.RATE_LIMITED));

		createLostItem("condition-429@example.test")
				.andExpect(status().isTooManyRequests())
				.andExpect(jsonPath("$.error.code").value("RATE_LIMITED"));
		verify(portalNameStorageClient, never()).search(any());
		assertThat(lostItemRepository.count()).isOne();
	}

	private org.springframework.test.web.servlet.ResultActions createLostItem(String email) throws Exception {
		String token = signupAndLogin(email).get("accessToken").asText();
		return authorizedPostJson("/api/lost-items", token,
				Map.of("description", "서울역에서 검은색 지갑을 잃어버렸어요", "languageCode", "ko"));
	}

	private void assertFallback(org.springframework.test.web.servlet.ResultActions actions) throws Exception {
		var result = actions
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.lookupStatus").value("COMPLETE"))
				.andExpect(jsonPath("$.data.rankingStatus").value("UNAVAILABLE"))
				.andExpect(jsonPath("$.data.persisted").value(false))
				.andExpect(jsonPath("$.data.warnings.length()").value(1))
				.andExpect(jsonPath("$.data.warnings[0]").value("AI_RANKING_UNAVAILABLE"))
				.andExpect(jsonPath("$.data.candidates.length()").value(3))
				.andExpect(jsonPath("$.data.candidates[0].candidateId").doesNotExist())
				.andExpect(jsonPath("$.data.candidates[0].isCurrent").value(false))
				.andExpect(jsonPath("$.data.candidates[0].isBaseline").value(false))
				.andExpect(jsonPath("$.data.candidates[0].foundItem.atcId").value("E-1"))
				.andExpect(jsonPath("$.data.candidates[0].foundItem.fdSn").value("001"))
				.andReturn();
		assertRanks(result.getResponse().getContentAsString(), 3);
	}

	private void assertRanks(String content, int size) throws Exception {
		List<Integer> ranks = new ArrayList<>();
		objectMapper.readTree(content).get("data").get("candidates")
				.forEach(candidate -> ranks.add(candidate.get("rank").intValue()));
		assertThat(ranks).containsExactlyElementsOf(IntStream.rangeClosed(1, size).boxed().toList());
	}

	private static String response(String... entries) {
		String output = "{\"candidates\":{" + String.join(",", entries) + "}}";
		String escaped = output.replace("\\", "\\\\").replace("\"", "\\\"");
		return "{\"status\":\"completed\",\"output\":[{\"type\":\"reasoning\",\"content\":[]},"
				+ "{\"type\":\"message\",\"content\":[{\"type\":\"output_text\",\"text\":\"" + escaped + "\"}]}]}";
	}

	private static String entry(String key, int score, boolean similar, String reason) {
		return "\"" + key + "\":{\"score\":" + score + ",\"isSimilar\":" + similar + ",\"reason\":\"" + reason + "\"}";
	}

	private FoundItemListEntry found(String atcId, String fdSn, String name) {
		return new FoundItemListEntry(FoundItemSourceType.PORTAL, atcId, fdSn, name, name, "지갑", "검정",
				null, "서울역 유실물센터", null);
	}
}
