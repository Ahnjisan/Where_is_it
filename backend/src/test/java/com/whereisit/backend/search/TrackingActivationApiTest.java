package com.whereisit.backend.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
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

@DisplayName("API-17 7일 추적 활성화")
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

	@Autowired
	private JdbcTemplate jdbcTemplate;

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

	@Test
	@DisplayName("Issue #85 저장된 물품명 검색어·분실 기간 시작일로 조회해 조건에 맞는 습득물만 기준 후보가 된다")
	void baselineUsesStoredKeywordAndLostDateFrom() throws Exception {
		when(aiSearchConditionExtractor.extract(any())).thenReturn(new AiSearchConditionExtractionResult(
				LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 21), "서울역", "지갑", null, "검색 조건을 확인했습니다."));
		mockSources(List.of(
				portalEntry("A", "검정 반지갑", "지갑 > 남성용 지갑", LocalDate.of(2026, 9, 21)),
				portalEntry("U", "우산", "우산", LocalDate.of(2026, 9, 21)),
				portalEntry("OLD", "지갑", "지갑 > 기타 지갑", LocalDate.of(2026, 9, 19))));
		String accessToken = signupAndLogin("user@example.test").get("accessToken").asText();
		String lostItemId = createLostItem(accessToken, "서울역에서 검은 지갑을 잃어버렸어요");

		authorizedPostJson("/api/lost-items/" + lostItemId + "/tracking", accessToken, Map.of())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("TRACKING"))
				.andExpect(jsonPath("$.data.conditions.searchStartDate").value("2026-09-20"))
				.andExpect(jsonPath("$.data.lastAutoSearchDate").value("2026-09-22"))
				.andExpect(jsonPath("$.data.currentCandidateCount").value(1));

		verify(portalClient).searchAllByFoundDate(LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 22));
		assertThat(jdbcTemplate.queryForList("select f.atc_id, c.is_baseline from lost_item_candidates c "
				+ "join found_items f on f.found_item_id = c.found_item_id"))
				.singleElement()
				.satisfies(row -> {
					assertThat(row.get("atc_id")).isEqualTo("A");
					assertThat(flag(row.get("is_baseline"))).isEqualTo(1);
				});
	}

	@Test
	@DisplayName("Issue #85 분실 기간 시작일도 없으면 검색 건 생성일부터 조회한다")
	void searchStartDateFallsBackToCreatedDate() throws Exception {
		String accessToken = signupAndLogin("user@example.test").get("accessToken").asText();
		String lostItemId = createLostItem(accessToken, "지갑을 잃어버렸어요");

		authorizedPostJson("/api/lost-items/" + lostItemId + "/tracking", accessToken, Map.of())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.conditions.searchStartDate").value("2026-09-22"));

		verify(portalClient).searchAllByFoundDate(LocalDate.of(2026, 9, 22), LocalDate.of(2026, 9, 22));
	}

	@Test
	@DisplayName("Issue #85 조회 시작일이 30일보다 이르면 조회는 30일 전부터 하되 저장된 시작일은 유지한다")
	void lookupIsCappedAtThirtyDays() throws Exception {
		when(aiSearchConditionExtractor.extract(any())).thenReturn(new AiSearchConditionExtractionResult(
				LocalDate.of(2026, 7, 1), null, null, "지갑", null, "검색 조건을 확인했습니다."));
		String accessToken = signupAndLogin("user@example.test").get("accessToken").asText();
		String lostItemId = createLostItem(accessToken, "7월에 지갑을 잃어버렸어요");

		authorizedPostJson("/api/lost-items/" + lostItemId + "/tracking", accessToken, Map.of())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.conditions.searchStartDate").value("2026-07-01"));

		verify(portalClient).searchAllByFoundDate(LocalDate.of(2026, 8, 23), LocalDate.of(2026, 9, 22));
	}

	@Test
	@DisplayName("Issue #85 조회하는 동안 검색어가 바뀌면 오래된 기준선을 저장하지 않고 409 ITEM_BUSY")
	void conditionChangeDuringLookupIsRejected() throws Exception {
		String accessToken = signupAndLogin("user@example.test").get("accessToken").asText();
		String lostItemId = createLostItem(accessToken, "지갑을 잃어버렸어요");
		when(portalClient.searchAllByFoundDate(any(), any())).thenAnswer(invocation -> {
			jdbcTemplate.update("update lost_items set product_name_keyword = ? where lost_item_id = ?",
					"카드지갑", Long.valueOf(lostItemId));
			return List.of(portalEntry("A", "지갑", "지갑", LocalDate.of(2026, 9, 22)));
		});

		authorizedPostJson("/api/lost-items/" + lostItemId + "/tracking", accessToken, Map.of())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("ITEM_BUSY"));

		authorizedGet("/api/lost-items/" + lostItemId, accessToken)
				.andExpect(jsonPath("$.data.status").value("SEARCHING"));
		assertThat(jdbcTemplate.queryForObject("select count(*) from lost_item_candidates", Long.class)).isZero();
	}

	private String createLostItem(String accessToken, String description) throws Exception {
		JsonNode created = objectMapper.readTree(
				authorizedPostJson("/api/lost-items", accessToken, Map.of("description", description))
						.andReturn().getResponse().getContentAsString()).get("data");
		return created.get("lostItem").get("lostItemId").asText();
	}

	private FoundItemListEntry portalEntry(String atcId, String productName, String categoryName, LocalDate foundDate) {
		return new FoundItemListEntry(FoundItemSourceType.PORTAL, atcId, "1", productName, productName, categoryName,
				"블랙(검정)", foundDate, "서울역 유실물센터", null);
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

	/** MySQL은 boolean 컬럼을 bit로 만들어 Boolean을, SQLite는 정수를 돌려준다. */
	private static int flag(Object value) {
		return value instanceof Boolean bool ? (bool ? 1 : 0) : ((Number) value).intValue();
	}
}
