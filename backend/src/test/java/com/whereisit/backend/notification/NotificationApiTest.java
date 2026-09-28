package com.whereisit.backend.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDate;
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
import com.whereisit.backend.founditem.client.FoundItemListEntry;
import com.whereisit.backend.founditem.client.FoundItemLookupClient;
import com.whereisit.backend.founditem.client.PortalFoundItemNameStorageClient;
import com.whereisit.backend.founditem.entity.FoundItemSourceType;
import com.whereisit.backend.notification.service.EmailSender;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractionResult;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractor;
import com.whereisit.backend.support.ApiTestSupport;
import com.whereisit.backend.tracking.TrackingBatchService;

@DisplayName("API-15 신규 후보 이메일 알림 이력")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(scripts = "/sql/cleanup-search-test-data.sql",
		config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
		executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/cleanup-search-test-data.sql",
		config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
		executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class NotificationApiTest extends ApiTestSupport {

	@MockitoBean(name = "policeFoundItemLookupClient")
	private FoundItemLookupClient policeClient;

	@MockitoBean(name = "portalFoundItemLookupClient")
	private FoundItemLookupClient portalClient;

	@MockitoBean
	private PortalFoundItemNameStorageClient portalNameStorageClient;

	@MockitoBean
	private AiSearchConditionExtractor aiSearchConditionExtractor;

	@MockitoBean
	private EmailSender emailSender;

	@Autowired
	private TrackingBatchService batchService;

	@BeforeEach
	void setUp() {
		when(policeClient.sourceType()).thenReturn(FoundItemSourceType.POLICE);
		when(portalClient.sourceType()).thenReturn(FoundItemSourceType.PORTAL);
		when(portalNameStorageClient.search(any())).thenReturn(List.of());
		when(aiSearchConditionExtractor.extract(any())).thenReturn(new AiSearchConditionExtractionResult(
				LocalDate.of(2026, 9, 20), null, null, "지갑", null, "검색 조건을 확인했습니다."));
		doReturn(List.of()).when(portalClient).searchAllByFoundDate(any(), any());
	}

	@Test
	@DisplayName("배치가 보낸 알림을 본인 분실물 기준으로 조회하고, 메일 본문은 주지 않는다")
	void ownerSeesNotificationHistory() throws Exception {
		String lostItemId = startTracking("owner@example.test");
		String accessToken = loginData("owner@example.test").get("accessToken").asText();
		authorizedGet("/api/lost-items/" + lostItemId + "/notifications", accessToken)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(0));

		clock.advance(Duration.ofDays(1));
		doReturn(List.of(wallet("B"))).when(portalClient).searchAllByFoundDate(any(), any());
		batchService.runDailyBatch();

		String freshToken = loginData("owner@example.test").get("accessToken").asText();
		String candidateId = objectMapper.readTree(authorizedGet(
				"/api/lost-items/" + lostItemId + "/candidates?scope=ALL", freshToken)
				.andReturn().getResponse().getContentAsString())
				.get("data").get("items").get(0).get("candidateId").asText();
		authorizedGet("/api/lost-items/" + lostItemId + "/notifications", freshToken)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.items[0].lostItemId").value(lostItemId))
				.andExpect(jsonPath("$.data.items[0].notificationDate").value("2026-09-23"))
				.andExpect(jsonPath("$.data.items[0].recipientEmail").value("owner@example.test"))
				.andExpect(jsonPath("$.data.items[0].languageCode").value("ko"))
				.andExpect(jsonPath("$.data.items[0].status").value("SENT"))
				.andExpect(jsonPath("$.data.items[0].candidateCount").value(1))
				.andExpect(jsonPath("$.data.items[0].candidateIds[0]").value(candidateId))
				.andExpect(jsonPath("$.data.items[0].body").doesNotExist());
		authorizedGet("/api/lost-items/" + lostItemId + "/candidates?scope=ALL", freshToken)
				.andExpect(jsonPath("$.data.items[0].notificationId").isString());
	}

	@Test
	@DisplayName("다른 회원의 분실물 알림 이력을 조회하면 404")
	void otherMemberCannotSeeNotifications() throws Exception {
		String lostItemId = startTracking("owner@example.test");
		String otherToken = signupAndLogin("other@example.test").get("accessToken").asText();

		authorizedGet("/api/lost-items/" + lostItemId + "/notifications", otherToken)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
	}

	private String startTracking(String email) throws Exception {
		signup(email, PASSWORD, "ko").andExpect(status().isCreated());
		String accessToken = loginData(email).get("accessToken").asText();
		JsonNode created = objectMapper.readTree(authorizedPostJson("/api/lost-items", accessToken,
				Map.of("description", "검은 지갑을 잃어버렸어요", "languageCode", "ko"))
				.andReturn().getResponse().getContentAsString()).get("data");
		String lostItemId = created.get("lostItem").get("lostItemId").asText();
		authorizedPostJson("/api/lost-items/" + lostItemId + "/tracking", accessToken, Map.of())
				.andExpect(status().isOk());
		return lostItemId;
	}

	private FoundItemListEntry wallet(String atcId) {
		return new FoundItemListEntry(FoundItemSourceType.PORTAL, atcId, "1", "카드지갑", "카드지갑", "지갑 > 기타 지갑",
				"블랙(검정)", LocalDate.of(2026, 9, 22), "서울역 유실물센터", null);
	}
}
