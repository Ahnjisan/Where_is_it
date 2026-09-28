package com.whereisit.backend.lostitem;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.whereisit.backend.candidate.entity.LostItemCandidate;
import com.whereisit.backend.candidate.repository.LostItemCandidateRepository;
import com.whereisit.backend.founditem.client.FoundItemDetail;
import com.whereisit.backend.founditem.client.FoundItemDetailClient;
import com.whereisit.backend.founditem.client.FoundItemLookupClient;
import com.whereisit.backend.founditem.client.PortalFoundItemNameStorageClient;
import com.whereisit.backend.founditem.entity.FoundItem;
import com.whereisit.backend.founditem.entity.FoundItemSourceType;
import com.whereisit.backend.founditem.repository.FoundItemRepository;
import com.whereisit.backend.lostitem.entity.LostItem;
import com.whereisit.backend.lostitem.repository.LostItemRepository;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractionResult;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractor;
import com.whereisit.backend.support.ApiTestSupport;

@DisplayName("API-05~09 분실물 검색·추적 CRUD")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(scripts = "/sql/cleanup-search-test-data.sql",
		config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
		executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/cleanup-search-test-data.sql",
		config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
		executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class LostItemCrudApiTest extends ApiTestSupport {

	@MockitoBean(name = "policeFoundItemLookupClient")
	private FoundItemLookupClient policeClient;

	@MockitoBean(name = "portalFoundItemLookupClient")
	private FoundItemLookupClient portalClient;

	@MockitoBean(name = "policeFoundItemDetailClient")
	private FoundItemDetailClient policeDetailClient;

	@MockitoBean(name = "portalFoundItemDetailClient")
	private FoundItemDetailClient portalDetailClient;

	@MockitoBean
	private PortalFoundItemNameStorageClient portalNameStorageClient;

	@MockitoBean
	private AiSearchConditionExtractor aiSearchConditionExtractor;

	@Autowired
	private LostItemRepository lostItemRepository;

	@Autowired
	private FoundItemRepository foundItemRepository;

	@Autowired
	private LostItemCandidateRepository candidateRepository;

	@BeforeEach
	void mockInitialSearch() {
		when(policeClient.sourceType()).thenReturn(FoundItemSourceType.POLICE);
		when(portalClient.sourceType()).thenReturn(FoundItemSourceType.PORTAL);
		when(policeClient.search(any())).thenReturn(List.of());
		when(portalClient.search(any())).thenReturn(List.of());
		when(portalNameStorageClient.search(any())).thenReturn(List.of());
		when(policeDetailClient.sourceType()).thenReturn(FoundItemSourceType.POLICE);
		when(portalDetailClient.sourceType()).thenReturn(FoundItemSourceType.PORTAL);
		when(aiSearchConditionExtractor.extract(any())).thenReturn(
				new AiSearchConditionExtractionResult(null, null, null, "검색 조건을 확인했습니다."));
	}

	@Test
	@DisplayName("TC-05 최초 분실 설명으로 등록하면 201 SEARCHING이고 언어를 안 주면 회원 언어를 쓴다")
	void createUsesMemberLanguageByDefault() throws Exception {
		JsonNode login = signupAndLogin("user@example.test");
		String accessToken = login.get("accessToken").asText();

		authorizedPostJson("/api/lost-items", accessToken, Map.of("description", "잃어버린 파란색 지갑"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.lostItem.lostItemId").isString())
				.andExpect(jsonPath("$.data.lostItem.description").value("잃어버린 파란색 지갑"))
				.andExpect(jsonPath("$.data.lostItem.languageCode").value("en"))
				.andExpect(jsonPath("$.data.lostItem.status").value("SEARCHING"))
				.andExpect(jsonPath("$.data.lostItem.startedAt").doesNotExist())
				.andExpect(jsonPath("$.data.lostItem.currentCandidateCount").value(0))
				.andExpect(jsonPath("$.data.lookupStatus").value("COMPLETE"))
				.andExpect(jsonPath("$.data.candidates.length()").value(0));
	}

	@Test
	@DisplayName("languageCode를 명시하면 그 값을 쓴다")
	void createWithExplicitLanguage() throws Exception {
		String accessToken = signupAndLogin("user@example.test").get("accessToken").asText();

		authorizedPostJson("/api/lost-items", accessToken, Map.of("description", "lost blue wallet", "languageCode", "ko"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.lostItem.languageCode").value("ko"));
	}

	@Test
	@DisplayName("지원하지 않는 languageCode면 400 UNSUPPORTED_LANGUAGE")
	void createWithUnsupportedLanguage() throws Exception {
		String accessToken = signupAndLogin("user@example.test").get("accessToken").asText();

		authorizedPostJson("/api/lost-items", accessToken, Map.of("description", "lost wallet", "languageCode", "ja"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("UNSUPPORTED_LANGUAGE"));
	}

	@Test
	@DisplayName("설명이 빈 값이면 400 VALIDATION_ERROR")
	void createWithBlankDescription() throws Exception {
		String accessToken = signupAndLogin("user@example.test").get("accessToken").asText();

		authorizedPostJson("/api/lost-items", accessToken, Map.of("description", ""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	@DisplayName("토큰 없이 등록하면 401 AUTH_REQUIRED")
	void createWithoutToken() throws Exception {
		postJson("/api/lost-items", Map.of("description", "lost wallet"))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.error.code").value("AUTH_REQUIRED"));
	}

	@Test
	@DisplayName("TC-13 목록·상세는 본인 것만 보이고 다른 회원 것은 404")
	void listAndDetailAreScopedToOwner() throws Exception {
		String ownerToken = signupAndLogin("owner@example.test").get("accessToken").asText();
		String otherToken = signupAndLogin("other@example.test").get("accessToken").asText();

		JsonNode created = objectMapper.readTree(
				authorizedPostJson("/api/lost-items", ownerToken, Map.of("description", "lost wallet"))
						.andReturn().getResponse().getContentAsString()).get("data");
		String lostItemId = created.get("lostItem").get("lostItemId").asText();

		authorizedGet("/api/lost-items", ownerToken)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].lostItemId").value(lostItemId))
				.andExpect(jsonPath("$.data.page").value(0))
				.andExpect(jsonPath("$.data.totalElements").value(1));

		authorizedGet("/api/lost-items/" + lostItemId, ownerToken)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.lostItemId").value(lostItemId))
				.andExpect(jsonPath("$.data.currentCandidate").doesNotExist());

		authorizedGet("/api/lost-items/" + lostItemId, otherToken)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
	}

	@Test
	@DisabledIfSystemProperty(named = "spring.profiles.active", matches = "sqlite",
			disabledReason = "상세조회는 외부 호출 동안 트랜잭션을 내려놓고 새 트랜잭션을 여는데, "
					+ "커넥션 풀이 1개뿐인 SQLite에서는 같은 스레드가 커넥션 2개를 동시에 요구해 타임아웃난다. MySQL에서만 확인한다.")
	@DisplayName("API-07 상세조회는 현재 결과 1위 후보의 상세를 최초 1회만 조회해 채운다")
	void detailFetchesTopCandidateDetailOnceAndCachesIt() throws Exception {
		String accessToken = signupAndLogin("user@example.test").get("accessToken").asText();
		String lostItemId = createLostItem(accessToken, "lost blue wallet");

		LostItem lostItem = lostItemRepository.findById(Long.valueOf(lostItemId)).orElseThrow();
		FoundItem foundItem = foundItemRepository.save(
				FoundItem.create(FoundItemSourceType.POLICE, "12345", "1", clock.instant().atZone(clock.getZone()).toLocalDateTime()));
		LostItemCandidate candidate = LostItemCandidate.create(lostItem, foundItem, LocalDateTime.now(clock));
		candidate.markSeenInCurrentResult(1, "색상이 일치합니다", true, LocalDateTime.now(clock));
		candidateRepository.save(candidate);

		when(policeDetailClient.fetchDetail("12345", "1"))
				.thenReturn(Optional.of(new FoundItemDetail("서울역 유실물센터", "02-1234-5678", "검정 장지갑")));

		authorizedGet("/api/lost-items/" + lostItemId, accessToken)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.currentCandidate.foundItem.foundPlace").value("서울역 유실물센터"))
				.andExpect(jsonPath("$.data.currentCandidate.foundItem.storagePhone").value("02-1234-5678"))
				.andExpect(jsonPath("$.data.currentCandidate.rank").value(1));

		authorizedGet("/api/lost-items/" + lostItemId, accessToken)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.currentCandidate.foundItem.foundPlace").value("서울역 유실물센터"));

		verify(policeDetailClient, times(1)).fetchDetail("12345", "1");
		verify(portalDetailClient, never()).fetchDetail(any(), any());
	}

	@Test
	@DisplayName("없는 ID를 조회하면 404")
	void detailNotFound() throws Exception {
		String accessToken = signupAndLogin("user@example.test").get("accessToken").asText();

		authorizedGet("/api/lost-items/999999", accessToken)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
	}

	@Test
	@DisplayName("설명·조건을 수정하면 반영되고, 공백만 있는 설명은 거부한다")
	void updateDescriptionAndConditions() throws Exception {
		String accessToken = signupAndLogin("user@example.test").get("accessToken").asText();
		String lostItemId = createLostItem(accessToken, "lost wallet");

		authorizedPostJson("/api/lost-items/update/" + lostItemId, accessToken, Map.of(
				"description", "lost red wallet near station",
				"conditions", Map.of("colorCode", "RD001", "lostPlaceText", "Gangnam station")))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.description").value("lost red wallet near station"))
				.andExpect(jsonPath("$.data.conditions.colorCode").value("RD001"))
				.andExpect(jsonPath("$.data.conditions.lostPlaceText").value("Gangnam station"));

		authorizedPostJson("/api/lost-items/update/" + lostItemId, accessToken, Map.of("description", "   "))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	@DisplayName("분실 기간 시작일이 종료일보다 늦으면 400 INVALID_SEARCH_CONDITION")
	void updateWithInvalidDateRange() throws Exception {
		String accessToken = signupAndLogin("user@example.test").get("accessToken").asText();
		String lostItemId = createLostItem(accessToken, "lost wallet");

		authorizedPostJson("/api/lost-items/update/" + lostItemId, accessToken, Map.of(
				"conditions", Map.of("lostDateFrom", "2026-09-20", "lostDateTo", "2026-09-10")))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("INVALID_SEARCH_CONDITION"));
	}

	@Test
	@DisplayName("SEARCHING 상태에서 알림 이메일을 바꾸려 하면 409 TRACKING_NOT_STARTED")
	void updateNotificationEmailBeforeTracking() throws Exception {
		String accessToken = signupAndLogin("user@example.test").get("accessToken").asText();
		String lostItemId = createLostItem(accessToken, "lost wallet");

		authorizedPostJson("/api/lost-items/update/" + lostItemId, accessToken,
				Map.of("notificationEmail", "user@example.test"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("TRACKING_NOT_STARTED"));
	}

	@Test
	@DisplayName("TC-26 삭제하면 204이고, 반복 삭제도 204이며, 삭제 후 상세조회는 404")
	void deleteIsIdempotentAndHidesDetail() throws Exception {
		String accessToken = signupAndLogin("user@example.test").get("accessToken").asText();
		String lostItemId = createLostItem(accessToken, "lost wallet");

		mockMvc.perform(post("/api/lost-items/delete/" + lostItemId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isNoContent());

		mockMvc.perform(post("/api/lost-items/delete/" + lostItemId)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + accessToken))
				.andExpect(status().isNoContent());

		authorizedGet("/api/lost-items/" + lostItemId, accessToken)
				.andExpect(status().isNotFound());
	}

	private String createLostItem(String accessToken, String description) throws Exception {
		JsonNode created = objectMapper.readTree(
				authorizedPostJson("/api/lost-items", accessToken, Map.of("description", description))
						.andReturn().getResponse().getContentAsString()).get("data");
		return created.get("lostItem").get("lostItemId").asText();
	}
}
