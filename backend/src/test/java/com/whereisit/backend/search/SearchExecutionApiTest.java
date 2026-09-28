package com.whereisit.backend.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.whereisit.backend.candidate.repository.LostItemCandidateRepository;
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

@DisplayName("API-11 검색 실행·API-12 후보 조회")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(scripts = "/sql/cleanup-search-test-data.sql",
		config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
		executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/cleanup-search-test-data.sql",
		config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
		executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class SearchExecutionApiTest extends ApiTestSupport {

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

	@Autowired
	private LostItemCandidateRepository candidateRepository;

	@BeforeEach
	void mockAi() {
		when(portalNameStorageClient.search(any())).thenReturn(List.of());
		when(aiSearchConditionExtractor.extract(any())).thenReturn(
				new AiSearchConditionExtractionResult(null, null, null, "검색 조건을 확인했습니다."));
		when(candidateRanker.rank(any())).thenAnswer(invocation -> successfulRanking(invocation.getArgument(0)));
	}

	@Test
	@DisplayName("두 출처가 모두 성공하면 COMPLETE이고 후보가 저장·정렬되어 반환된다")
	void completeSearchPersistsRankedCandidates() throws Exception {
		mockSources(
				List.of(new FoundItemListEntry(FoundItemSourceType.POLICE, "1", "1", "파란색 지갑", "지갑 습득", null, null, null, null, null)),
				List.of(new FoundItemListEntry(FoundItemSourceType.PORTAL, "2", "1", "우산", "우산 습득", null, null, null, null, null)));
		String accessToken = signupAndLogin("user@example.test").get("accessToken").asText();
		String lostItemId = createLostItem(accessToken, "파란색 지갑을 잃어버렸어요");

		authorizedPostJson("/api/lost-items/" + lostItemId + "/searches", accessToken, Map.of("mode", "INITIAL"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.lookupStatus").value("COMPLETE"))
				.andExpect(jsonPath("$.data.candidates.length()").value(2))
				.andExpect(jsonPath("$.data.candidates[0].foundItem.productName").value("파란색 지갑"))
				.andExpect(jsonPath("$.data.candidates[0].isSimilar").value(true))
				.andExpect(jsonPath("$.data.candidates[0].candidateId").isString())
				.andExpect(jsonPath("$.data.assistantMessage.role").value("ASSISTANT"));

		assertThat(candidateRepository.count()).isEqualTo(2);

		authorizedGet("/api/lost-items/" + lostItemId + "/candidates", accessToken)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(2))
				.andExpect(jsonPath("$.data.items[0].isCurrent").value(true));
	}

	@Test
	@DisplayName("한쪽 출처만 실패하면 PARTIAL이고 후보는 저장되지 않는 일시적 결과다")
	void partialFailureReturnsTransientCandidates() throws Exception {
		when(policeClient.sourceType()).thenReturn(FoundItemSourceType.POLICE);
		when(portalClient.sourceType()).thenReturn(FoundItemSourceType.PORTAL);
		when(policeClient.search(any())).thenReturn(
				List.of(new FoundItemListEntry(FoundItemSourceType.POLICE, "1", "1", "지갑", null, null, null, null, null, null)));
		when(portalClient.search(any())).thenThrow(new FoundItemLookupException(FoundItemSourceType.PORTAL, "99", "TIMEOUT"));

		String accessToken = signupAndLogin("user@example.test").get("accessToken").asText();
		String lostItemId = createLostItem(accessToken, "지갑을 잃어버렸어요");

		authorizedPostJson("/api/lost-items/" + lostItemId + "/searches", accessToken, Map.of("mode", "INITIAL"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.lookupStatus").value("PARTIAL"))
				.andExpect(jsonPath("$.data.warnings[0]").value("PARTIAL_SOURCE_RESULT"))
				.andExpect(jsonPath("$.data.candidates[0].candidateId").doesNotExist());

		assertThat(candidateRepository.count()).isZero();
	}

	@Test
	@DisplayName("두 출처가 모두 실패하면 502 LOST_API_UNAVAILABLE이고 0건 성공으로 위장하지 않는다")
	void bothSourcesFailingIsAnError() throws Exception {
		mockSources(List.of(), List.of());
		String accessToken = signupAndLogin("user@example.test").get("accessToken").asText();
		String lostItemId = createLostItem(accessToken, "지갑을 잃어버렸어요");

		when(policeClient.sourceType()).thenReturn(FoundItemSourceType.POLICE);
		when(portalClient.sourceType()).thenReturn(FoundItemSourceType.PORTAL);
		when(policeClient.search(any())).thenThrow(new FoundItemLookupException(FoundItemSourceType.POLICE, "99", "TIMEOUT"));
		when(portalClient.search(any())).thenThrow(new FoundItemLookupException(FoundItemSourceType.PORTAL, "99", "TIMEOUT"));

		authorizedPostJson("/api/lost-items/" + lostItemId + "/searches", accessToken, Map.of("mode", "INITIAL"))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.error.code").value("LOST_API_UNAVAILABLE"));
	}

	@Test
	@DisplayName("FILTER 모드는 conditions를 반영하고, TEXT 모드는 사용자 메시지를 대화에 남긴다")
	void filterAndTextModesUpdateState() throws Exception {
		mockSources(List.of(), List.of());
		String accessToken = signupAndLogin("user@example.test").get("accessToken").asText();
		String lostItemId = createLostItem(accessToken, "지갑을 잃어버렸어요");

		authorizedPostJson("/api/lost-items/" + lostItemId + "/searches", accessToken,
				Map.of("mode", "FILTER", "conditions", Map.of("colorCode", "BL001")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.lostItem.conditions.colorCode").value("BL001"));

		authorizedPostJson("/api/lost-items/" + lostItemId + "/searches", accessToken,
				Map.of("mode", "TEXT", "message", "강남역 근처였어요"))
				.andExpect(status().isOk());

		authorizedGet("/api/lost-items/" + lostItemId + "/messages", accessToken)
				.andExpect(jsonPath("$.data.items[?(@.content=='강남역 근처였어요')]").exists());
	}

	private void mockSources(List<FoundItemListEntry> policeResults, List<FoundItemListEntry> portalResults) {
		when(policeClient.sourceType()).thenReturn(FoundItemSourceType.POLICE);
		when(portalClient.sourceType()).thenReturn(FoundItemSourceType.PORTAL);
		when(policeClient.search(any())).thenReturn(policeResults);
		when(portalClient.search(any())).thenReturn(portalResults);
	}

	private CandidateRankingResult successfulRanking(CandidateRankingRequest request) {
		List<RankedCandidate> ranked = new java.util.ArrayList<>();
		for (int i = 0; i < request.candidates().size(); i++) {
			ranked.add(new RankedCandidate(request.candidates().get(i).candidateKey(), i + 1,
					"legacy ranking " + (i + 1), true));
		}
		return CandidateRankingResult.success(ranked);
	}

	private String createLostItem(String accessToken, String description) throws Exception {
		JsonNode created = objectMapper.readTree(
				authorizedPostJson("/api/lost-items", accessToken, Map.of("description", description))
						.andReturn().getResponse().getContentAsString()).get("data");
		return created.get("lostItem").get("lostItemId").asText();
	}
}
