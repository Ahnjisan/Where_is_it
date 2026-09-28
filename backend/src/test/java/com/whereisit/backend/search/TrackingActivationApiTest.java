package com.whereisit.backend.search;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.whereisit.backend.candidate.ranking.CandidateRanker;
import com.whereisit.backend.candidate.ranking.CandidateRankingRequest;
import com.whereisit.backend.candidate.ranking.CandidateRankingResult;
import com.whereisit.backend.candidate.ranking.RankedCandidate;
import com.whereisit.backend.founditem.client.FoundItemListEntry;
import com.whereisit.backend.founditem.client.FoundItemLookupClient;
import com.whereisit.backend.founditem.client.FoundItemLookupException;
import com.whereisit.backend.founditem.client.PortalFoundItemNameStorageClient;
import com.whereisit.backend.founditem.entity.FoundItemSourceType;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractionResult;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractor;
import com.whereisit.backend.support.ApiTestSupport;

@DisplayName("API-14 7일 추적 활성화")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(scripts = "/sql/cleanup-search-test-data.sql",
		config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
		executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/cleanup-search-test-data.sql",
		config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
		executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TrackingActivationApiTest extends ApiTestSupport {

	@MockitoBean(name = "policeFoundItemLookupClient")
	private FoundItemLookupClient policeClient;

	@MockitoBean(name = "portalFoundItemLookupClient")
	private FoundItemLookupClient portalClient;

	@MockitoBean
	private PortalFoundItemNameStorageClient portalNameStorageClient;

	@MockitoBean
	private AiSearchConditionExtractor aiSearchConditionExtractor;

	@MockitoBean
	private CandidateRanker candidateRanker;

	@BeforeEach
	void mockAi() {
		when(portalNameStorageClient.search(any())).thenReturn(List.of());
		when(policeClient.sourceType()).thenReturn(FoundItemSourceType.POLICE);
		when(portalClient.sourceType()).thenReturn(FoundItemSourceType.PORTAL);
		when(policeClient.search(any())).thenReturn(List.of());
		when(portalClient.search(any())).thenReturn(List.of());
		when(aiSearchConditionExtractor.extract(any())).thenReturn(
				new AiSearchConditionExtractionResult(null, null, null, "지갑", null, "검색 조건을 확인했습니다."));
		when(portalClient.searchAllByFoundDate(any(), any())).thenReturn(List.of());
		when(candidateRanker.rank(any())).thenAnswer(invocation -> successfulRanking(invocation.getArgument(0)));
	}

	@Test
	@DisplayName("TC-16 완전조회 가능하면 TRACKING이 되고 기준 후보가 저장되며 만료는 시작+168시간이다")
	void activatesTrackingWithBaselineCandidates() throws Exception {
		mockSources(List.of(new FoundItemListEntry(
				FoundItemSourceType.PORTAL, "1", "1", "지갑", null, null, null, LocalDate.of(2026, 9, 10), null, null)));
		String accessToken = signupAndLogin("user@example.test").get("accessToken").asText();
		String lostItemId = createLostItemWithSearchStartDate(accessToken, "지갑을 잃어버렸어요");

		authorizedPostJson("/api/lost-items/" + lostItemId + "/tracking", accessToken, Map.of())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("TRACKING"))
				.andExpect(jsonPath("$.data.notificationEmail").value("user@example.test"))
				.andExpect(jsonPath("$.data.startedAt").value("2026-09-22T15:00:00.000000+09:00"))
				.andExpect(jsonPath("$.data.expiresAt").value("2026-09-29T15:00:00.000000+09:00"));

		authorizedGet("/api/lost-items/" + lostItemId + "/candidates?scope=ALL", accessToken)
				.andExpect(jsonPath("$.data.items[0].isBaseline").value(true));
	}

	@Test
	@DisplayName("TC-18 TRACKING 상태에서 다시 호출하면 시작·만료시각을 바꾸지 않고 그대로 200을 준다")
	void repeatedActivationIsANoop() throws Exception {
		mockSources(List.of());
		String accessToken = signupAndLogin("user@example.test").get("accessToken").asText();
		String lostItemId = createLostItemWithSearchStartDate(accessToken, "지갑을 잃어버렸어요");

		JsonNode first = objectMapper.readTree(
				authorizedPostJson("/api/lost-items/" + lostItemId + "/tracking", accessToken, Map.of())
						.andReturn().getResponse().getContentAsString()).get("data");

		JsonNode second = objectMapper.readTree(
				authorizedPostJson("/api/lost-items/" + lostItemId + "/tracking", accessToken, Map.of())
						.andReturn().getResponse().getContentAsString()).get("data");

		org.assertj.core.api.Assertions.assertThat(second.get("startedAt")).isEqualTo(first.get("startedAt"));
		org.assertj.core.api.Assertions.assertThat(second.get("expiresAt")).isEqualTo(first.get("expiresAt"));
	}

	@Test
	@DisplayName("API-05에서 물품명 검색어가 구조화되지 않았으면 400 INVALID_SEARCH_CONDITION")
	void requiresProductNameKeyword() throws Exception {
		when(aiSearchConditionExtractor.extract(any())).thenReturn(
				new AiSearchConditionExtractionResult(null, null, null, "검색 조건을 확인했습니다."));
		String accessToken = signupAndLogin("user@example.test").get("accessToken").asText();
		JsonNode created = objectMapper.readTree(
				authorizedPostJson("/api/lost-items", accessToken, Map.of("description", "지갑을 잃어버렸어요"))
						.andReturn().getResponse().getContentAsString()).get("data");

		authorizedPostJson("/api/lost-items/" + created.get("lostItem").get("lostItemId").asText() + "/tracking", accessToken, Map.of())
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("INVALID_SEARCH_CONDITION"));
	}

	@Test
	@DisplayName("포털기관 조회가 실패해 기준 후보를 확정할 수 없으면 활성화하지 않고 오류를 응답한다")
	void unavailableBaselineIsRejected() throws Exception {
		mockSources(List.of());
		String accessToken = signupAndLogin("user@example.test").get("accessToken").asText();
		String lostItemId = createLostItemWithSearchStartDate(accessToken, "지갑을 잃어버렸어요");

		when(portalClient.searchAllByFoundDate(any(), any()))
				.thenThrow(new FoundItemLookupException(FoundItemSourceType.PORTAL, "99", "TIMEOUT"));

		authorizedPostJson("/api/lost-items/" + lostItemId + "/tracking", accessToken, Map.of())
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.error.code").value("LOST_API_UNAVAILABLE"));

		authorizedGet("/api/lost-items/" + lostItemId, accessToken)
				.andExpect(jsonPath("$.data.status").value("SEARCHING"));
	}

	private void mockSources(List<FoundItemListEntry> portalResults) {
		when(portalClient.sourceType()).thenReturn(FoundItemSourceType.PORTAL);
		when(portalClient.searchAllByFoundDate(any(), any())).thenReturn(portalResults);
	}

	private String createLostItemWithSearchStartDate(String accessToken, String description) throws Exception {
		JsonNode created = objectMapper.readTree(
				authorizedPostJson("/api/lost-items", accessToken, Map.of("description", description))
						.andReturn().getResponse().getContentAsString()).get("data");
		String lostItemId = created.get("lostItem").get("lostItemId").asText();

		authorizedPostJson("/api/lost-items/update/" + lostItemId, accessToken,
				Map.of("conditions", Map.of("searchStartDate", "2026-09-01")))
				.andExpect(status().isOk());
		return lostItemId;
	}

	private CandidateRankingResult successfulRanking(CandidateRankingRequest request) {
		List<RankedCandidate> ranked = new java.util.ArrayList<>();
		for (int i = 0; i < request.candidates().size(); i++) {
			ranked.add(new RankedCandidate(request.candidates().get(i).candidateKey(), i + 1,
					"legacy ranking " + (i + 1), true));
		}
		return CandidateRankingResult.success(ranked);
	}
}
