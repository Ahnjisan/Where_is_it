package com.whereisit.backend.lostitem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.IntStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.whereisit.backend.candidate.ranking.CandidateRankingRequest;
import com.whereisit.backend.candidate.ranking.CandidateRankingResult;
import com.whereisit.backend.candidate.ranking.RankedCandidate;
import com.whereisit.backend.candidate.ranking.initial.InitialSearchCandidateRanker;
import com.whereisit.backend.candidate.ranking.initial.InitialSearchRuleScorer;
import com.whereisit.backend.candidate.ranking.openai.OpenAiCandidateRanker;
import com.whereisit.backend.candidate.repository.LostItemCandidateRepository;
import com.whereisit.backend.founditem.client.FoundItemListEntry;
import com.whereisit.backend.founditem.client.FoundItemLookupClient;
import com.whereisit.backend.founditem.client.FoundItemLookupException;
import com.whereisit.backend.founditem.client.PortalFoundItemNameStorageClient;
import com.whereisit.backend.founditem.client.PortalFoundItemSearchQuery;
import com.whereisit.backend.founditem.entity.FoundItemSourceType;
import com.whereisit.backend.founditem.repository.FoundItemRepository;
import com.whereisit.backend.global.error.BusinessException;
import com.whereisit.backend.lostitem.entity.ChatRole;
import com.whereisit.backend.lostitem.repository.ChatMessageRepository;
import com.whereisit.backend.lostitem.repository.LostItemRepository;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractionResult;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractor;
import com.whereisit.backend.search.error.SearchErrorCode;
import com.whereisit.backend.support.ApiTestSupport;

@DisplayName("API-05 initial integrated search")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(scripts = "/sql/cleanup-search-test-data.sql",
		config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
		executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/cleanup-search-test-data.sql",
		config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
		executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class InitialSearchApiTest extends ApiTestSupport {

	@MockitoBean(name = "policeFoundItemLookupClient")
	private FoundItemLookupClient policeClient;

	@MockitoBean(name = "portalFoundItemLookupClient")
	private FoundItemLookupClient legacyPortalClient;

	@MockitoBean
	private PortalFoundItemNameStorageClient portalNameStorageClient;

	@MockitoBean
	private AiSearchConditionExtractor aiSearchConditionExtractor;

	@MockitoBean
	private InitialSearchCandidateRanker candidateRanker;

	/** Legacy API-11·14 랭커. API-05는 이 Bean을 호출하지 않아야 한다. */
	@MockitoBean
	private OpenAiCandidateRanker legacyCandidateRanker;

	@Autowired private LostItemRepository lostItemRepository;
	@Autowired private ChatMessageRepository chatMessageRepository;
	@Autowired private LostItemCandidateRepository candidateRepository;
	@Autowired private FoundItemRepository foundItemRepository;
	@Autowired private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void setUp() {
		when(policeClient.sourceType()).thenReturn(FoundItemSourceType.POLICE);
		when(legacyPortalClient.sourceType()).thenReturn(FoundItemSourceType.PORTAL);
		when(aiSearchConditionExtractor.extract(any())).thenReturn(aiResult());
		when(portalNameStorageClient.search(any())).thenReturn(List.of());
		when(candidateRanker.rank(any())).thenAnswer(invocation -> successfulRanking(invocation.getArgument(0)));
	}

	@Test
	@DisplayName("Frontend-compatible transient candidates expose openId and keep external calls outside transactions")
	void returnsFrontendCompatibleTransientRankedResults() throws Exception {
		AtomicBoolean userCommittedBeforeAi = new AtomicBoolean();
		AtomicBoolean aiTransactionActive = new AtomicBoolean(true);
		AtomicBoolean portalTransactionActive = new AtomicBoolean(true);
		AtomicBoolean rankingTransactionActive = new AtomicBoolean(true);
		when(aiSearchConditionExtractor.extract(any())).thenAnswer(invocation -> {
			aiTransactionActive.set(TransactionSynchronizationManager.isActualTransactionActive());
			userCommittedBeforeAi.set(chatMessageRepository.findAll().stream()
					.filter(message -> message.getRole() == ChatRole.USER).count() == 1);
			return aiResult();
		});
		when(portalNameStorageClient.search(any())).thenAnswer(invocation -> {
			portalTransactionActive.set(TransactionSynchronizationManager.isActualTransactionActive());
			return List.of(found("A-1", "007", "검은색 카드지갑"), found("A-2", "2", "검은색 지갑"));
		});
		doAnswer(invocation -> {
			rankingTransactionActive.set(TransactionSynchronizationManager.isActualTransactionActive());
			return successfulRanking(invocation.getArgument(0));
		}).when(candidateRanker).rank(any());
		String accessToken = signupAndLogin("api05@example.test").get("accessToken").asText();

		var result = authorizedPostJson("/api/lost-items", accessToken,
				Map.of("description", "서울역에서 검은색 지갑을 잃어버렸어요", "languageCode", "ko"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.lookupStatus").value("COMPLETE"))
				.andExpect(jsonPath("$.data.rankingStatus").value("SUCCESS"))
				.andExpect(jsonPath("$.data.persisted").value(false))
				.andExpect(jsonPath("$.data.warnings.length()").value(0))
				.andExpect(jsonPath("$.data.candidates.length()").value(2))
				.andExpect(jsonPath("$.data.candidates[0].candidateId").doesNotExist())
				.andExpect(jsonPath("$.data.candidates[0].foundItem.foundItemId").isString())
				.andExpect(jsonPath("$.data.candidates[0].foundItem.openId").value("A-1"))
				.andExpect(jsonPath("$.data.candidates[0].foundItem.atcId").value("A-1"))
				.andExpect(jsonPath("$.data.candidates[0].foundItem.fdSn").value("007"))
				.andExpect(jsonPath("$.data.candidates[0].rank").value(1))
				.andExpect(jsonPath("$.data.candidates[0].reason").value("AI 추천 이유 1"))
				.andExpect(jsonPath("$.data.candidates[0].isCurrent").value(false))
				.andExpect(jsonPath("$.data.candidates[0].isBaseline").value(false))
				.andExpect(jsonPath("$.data.candidates[0].isSimilar").value(true))
				.andExpect(jsonPath("$.data.candidates[0].score").doesNotExist())
				.andExpect(jsonPath("$.data.candidates[1].rank").value(2))
				.andExpect(jsonPath("$.data.clarificationQuestions").isArray())
				.andReturn();

		JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
		assertThat(data.fieldNames()).toIterable().containsExactlyInAnyOrder("lostItem", "assistantMessage",
				"lookupStatus", "rankingStatus", "persisted", "warnings", "candidates", "clarificationQuestions");
		assertThat(data.get("candidates").get(0).has("score")).isFalse();
		assertThat(data.get("candidates").get(0).get("foundItem").has("foundItemId")).isTrue();
		String lostItemId = data.get("lostItem").get("lostItemId").asText();
		assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION)).isEqualTo("/api/lost-items/" + lostItemId);
		assertThat(userCommittedBeforeAi).isTrue();
		assertThat(aiTransactionActive).isFalse();
		assertThat(portalTransactionActive).isFalse();
		assertThat(rankingTransactionActive).isFalse();
		assertThat(chatMessageRepository.findAll()).extracting(message -> message.getRole())
				.containsExactly(ChatRole.USER, ChatRole.ASSISTANT);
		assertThat(candidateRepository.count()).isZero();
		assertThat(foundItemRepository.count()).isEqualTo(2);
		verify(policeClient, never()).search(any());
		verify(legacyPortalClient, never()).search(any());
		verify(legacyCandidateRanker, never()).rank(any());

		ArgumentCaptor<PortalFoundItemSearchQuery> query = ArgumentCaptor.forClass(PortalFoundItemSearchQuery.class);
		verify(portalNameStorageClient).search(query.capture());
		assertThat(query.getValue().productNameKeyword()).isEqualTo("지갑");
		assertThat(query.getValue().storagePlaceKeyword()).isEqualTo("서울역 유실물센터");
	}

	@Test
	void zeroResultsDoNotRunRanking() throws Exception {
		clearInvocations(candidateRanker);
		String token = signupAndLogin("zero@example.test").get("accessToken").asText();
		authorizedPostJson("/api/lost-items", token, Map.of("description", "지갑"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.rankingStatus").value("NOT_RUN"))
				.andExpect(jsonPath("$.data.persisted").value(false))
				.andExpect(jsonPath("$.data.candidates.length()").value(0));
		verify(candidateRanker, never()).rank(any());
		assertThat(candidateRepository.count()).isZero();
		assertThat(foundItemRepository.count()).isZero();
	}

	@Test
	void rankingFailureReturnsRuleBasedTransientFallback() throws Exception {
		when(portalNameStorageClient.search(any())).thenReturn(List.of(found("F-1", "1", "검은 지갑")));
		doAnswer(invocation -> new InitialSearchRuleScorer().fallback(invocation.getArgument(0)))
				.when(candidateRanker).rank(any());
		String token = signupAndLogin("fallback@example.test").get("accessToken").asText();
		authorizedPostJson("/api/lost-items", token, Map.of("description", "검은 지갑", "languageCode", "ko"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.lookupStatus").value("COMPLETE"))
				.andExpect(jsonPath("$.data.rankingStatus").value("UNAVAILABLE"))
				.andExpect(jsonPath("$.data.persisted").value(false))
				.andExpect(jsonPath("$.data.warnings[0]").value("AI_RANKING_UNAVAILABLE"))
				.andExpect(jsonPath("$.data.candidates[0].candidateId").doesNotExist())
				.andExpect(jsonPath("$.data.candidates[0].isCurrent").value(false));
		assertThat(candidateRepository.count()).isZero();
		assertThat(foundItemRepository.count()).isOne();
	}

	@Test
	void oneAndTwentyResultsAreRankedWithoutLimitWarning() throws Exception {
		when(portalNameStorageClient.search(any())).thenReturn(List.of(found("ONE", "1", "지갑")));
		String oneToken = signupAndLogin("one@example.test").get("accessToken").asText();
		authorizedPostJson("/api/lost-items", oneToken, Map.of("description", "지갑"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.candidates.length()").value(1))
				.andExpect(jsonPath("$.data.candidates[0].rank").value(1));

		when(portalNameStorageClient.search(any())).thenReturn(IntStream.rangeClosed(1, 20)
				.mapToObj(i -> found("T-" + i, Integer.toString(i), "물품 " + i)).toList());
		String twentyToken = signupAndLogin("twenty@example.test").get("accessToken").asText();
		authorizedPostJson("/api/lost-items", twentyToken, Map.of("description", "물품"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.candidates.length()").value(20))
				.andExpect(jsonPath("$.data.candidates[19].rank").value(20))
				.andExpect(jsonPath("$.data.warnings.length()").value(0));
		assertThat(candidateRepository.count()).isZero();
	}

	@Test
	void moreThanTwentyResultsArePreselectedToDeterministicTopTwenty() throws Exception {
		for (int count : new int[] {21, 72, 100, 101}) {
			clearInvocations(candidateRanker);
			List<FoundItemListEntry> entries = new ArrayList<>(IntStream.range(0, count)
					.mapToObj(i -> found("L-%03d".formatted(i), "%03d".formatted(i), "item-" + i)).toList());
			entries.add(entries.get(count - 1));
			when(portalNameStorageClient.search(any())).thenReturn(entries);
			ArgumentCaptor<CandidateRankingRequest> captor = ArgumentCaptor.forClass(CandidateRankingRequest.class);
			String token = signupAndLogin("limit-" + count + "@example.test").get("accessToken").asText();

			authorizedPostJson("/api/lost-items", token, Map.of("description", "일치하지 않는 설명"))
					.andExpect(status().isCreated())
					.andExpect(jsonPath("$.data.rankingStatus").value("SUCCESS"))
					.andExpect(jsonPath("$.data.persisted").value(false))
					.andExpect(jsonPath("$.data.candidates.length()").value(20))
					.andExpect(jsonPath("$.data.candidates[0].foundItem.atcId").value("L-000"))
					.andExpect(jsonPath("$.data.candidates[0].foundItem.fdSn").value("000"))
					.andExpect(jsonPath("$.data.candidates[19].foundItem.atcId").value("L-019"))
					.andExpect(jsonPath("$.data.candidates[19].rank").value(20))
					.andExpect(jsonPath("$.data.warnings.length()").value(1))
					.andExpect(jsonPath("$.data.warnings[0]").value("RESULT_LIMIT_REACHED"));

			verify(candidateRanker).rank(captor.capture());
			verify(legacyCandidateRanker, never()).rank(any());
			assertThat(captor.getValue().candidates()).extracting(candidate -> candidate.candidateKey())
					.containsExactlyElementsOf(IntStream.rangeClosed(1, 20).mapToObj(i -> "c" + i).toList());
			assertThat(captor.getValue().candidates()).extracting(candidate -> candidate.productName())
					.containsExactlyElementsOf(IntStream.range(0, 20).mapToObj(i -> "item-" + i).toList());
			assertThat(candidateRepository.count()).isZero();
			assertThat(foundItemRepository.count()).isEqualTo(20);
		}
	}

	@Test
	void aiRankingAndFallbackUseSameTwentyCandidateSet() throws Exception {
		List<FoundItemListEntry> entries = IntStream.range(0, 72)
				.mapToObj(i -> found("S-%03d".formatted(i), "%03d".formatted(i),
						i % 9 == 0 ? "검은색지갑" : "물품 " + i)).toList();
		when(portalNameStorageClient.search(any())).thenReturn(entries);
		ArgumentCaptor<CandidateRankingRequest> captor = ArgumentCaptor.forClass(CandidateRankingRequest.class);

		String successToken = signupAndLogin("same-success@example.test").get("accessToken").asText();
		authorizedPostJson("/api/lost-items", successToken, Map.of("description", "검은색 지갑", "languageCode", "ko"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.rankingStatus").value("SUCCESS"))
				.andExpect(jsonPath("$.data.candidates.length()").value(20));

		doAnswer(invocation -> new InitialSearchRuleScorer().fallback(invocation.getArgument(0)))
				.when(candidateRanker).rank(any());
		String fallbackToken = signupAndLogin("same-fallback@example.test").get("accessToken").asText();
		var fallback = authorizedPostJson("/api/lost-items", fallbackToken,
				Map.of("description", "검은색 지갑", "languageCode", "ko"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.lookupStatus").value("COMPLETE"))
				.andExpect(jsonPath("$.data.rankingStatus").value("UNAVAILABLE"))
				.andExpect(jsonPath("$.data.persisted").value(false))
				.andExpect(jsonPath("$.data.warnings[0]").value("AI_RANKING_UNAVAILABLE"))
				.andExpect(jsonPath("$.data.warnings[1]").value("RESULT_LIMIT_REACHED"))
				.andExpect(jsonPath("$.data.warnings.length()").value(2))
				.andExpect(jsonPath("$.data.candidates.length()").value(20))
				.andExpect(jsonPath("$.data.candidates[0].candidateId").doesNotExist())
				.andExpect(jsonPath("$.data.candidates[0].foundItem.productName").value("검은색지갑"))
				.andExpect(jsonPath("$.data.candidates[0].isCurrent").value(false))
				.andExpect(jsonPath("$.data.candidates[0].isBaseline").value(false))
				.andReturn();

		verify(candidateRanker, org.mockito.Mockito.times(2)).rank(captor.capture());
		assertThat(captor.getAllValues().get(1)).isEqualTo(captor.getAllValues().get(0));
		JsonNode candidates = objectMapper.readTree(fallback.getResponse().getContentAsString())
				.get("data").get("candidates");
		List<Integer> ranks = new ArrayList<>();
		candidates.forEach(candidate -> ranks.add(candidate.get("rank").intValue()));
		assertThat(ranks).containsExactlyElementsOf(IntStream.rangeClosed(1, 20).boxed().toList());
		assertThat(candidateRepository.count()).isZero();
	}

	@Test
	void portalFailureReturnsExistingUnavailableErrorAfterCreation() throws Exception {
		when(portalNameStorageClient.search(any()))
				.thenThrow(new FoundItemLookupException(FoundItemSourceType.PORTAL, "99", "TIMEOUT"));
		String token = signupAndLogin("portal-failure@example.test").get("accessToken").asText();
		authorizedPostJson("/api/lost-items", token, Map.of("description", "지갑"))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.error.code").value("LOST_API_UNAVAILABLE"));
		assertThat(lostItemRepository.count()).isOne();
		assertThat(chatMessageRepository.findAll()).extracting(message -> message.getRole())
				.containsExactly(ChatRole.USER, ChatRole.ASSISTANT);
		verify(candidateRanker, never()).rank(any());
	}

	@Test
	void searchConditionFailureKeepsCreationAndSkipsPortal() throws Exception {
		when(aiSearchConditionExtractor.extract(any())).thenThrow(new BusinessException(SearchErrorCode.SEARCH_TIMEOUT));
		String token = signupAndLogin("ai-failure@example.test").get("accessToken").asText();
		authorizedPostJson("/api/lost-items", token, Map.of("description", "지갑"))
				.andExpect(status().isGatewayTimeout())
				.andExpect(jsonPath("$.error.code").value("SEARCH_TIMEOUT"));
		assertThat(lostItemRepository.count()).isOne();
		assertThat(chatMessageRepository.findAll()).extracting(message -> message.getRole())
				.containsExactly(ChatRole.USER);
		verify(portalNameStorageClient, never()).search(any());
	}

	@Test
	void snapshotChangeDuringRankingReturnsItemBusyWithoutCachingResults() throws Exception {
		when(portalNameStorageClient.search(any())).thenReturn(List.of(found("BUSY", "1", "지갑")));
		doAnswer(invocation -> {
			Long id = jdbcTemplate.queryForObject("select max(lost_item_id) from lost_items", Long.class);
			jdbcTemplate.update("update lost_items set description = ? where lost_item_id = ?", "동시에 변경된 설명", id);
			return successfulRanking(invocation.getArgument(0));
		}).when(candidateRanker).rank(any());
		String token = signupAndLogin("busy@example.test").get("accessToken").asText();
		authorizedPostJson("/api/lost-items", token, Map.of("description", "지갑"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("ITEM_BUSY"));
		assertThat(candidateRepository.count()).isZero();
		assertThat(foundItemRepository.count()).isZero();
	}

	private AiSearchConditionExtractionResult aiResult() {
		return new AiSearchConditionExtractionResult(null, null, "서울역", "지갑", "서울역 유실물센터",
				"검색 조건을 확인했습니다.");
	}

	private CandidateRankingResult successfulRanking(CandidateRankingRequest request) {
		List<RankedCandidate> ranked = new ArrayList<>();
		for (int i = 0; i < request.candidates().size(); i++) {
			ranked.add(new RankedCandidate(request.candidates().get(i).candidateKey(), i + 1,
					"AI 추천 이유 " + (i + 1), true));
		}
		return CandidateRankingResult.success(ranked);
	}

	private FoundItemListEntry found(String atcId, String fdSn, String name) {
		return new FoundItemListEntry(FoundItemSourceType.PORTAL, atcId, fdSn, name, name, "지갑", "검정",
				null, "서울역 유실물센터", null);
	}
}
