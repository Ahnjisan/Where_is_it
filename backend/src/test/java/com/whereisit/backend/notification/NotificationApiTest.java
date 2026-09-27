package com.whereisit.backend.notification;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.fasterxml.jackson.databind.JsonNode;
import com.whereisit.backend.batch.TrackingBatchService;
import com.whereisit.backend.founditem.client.FoundItemListEntry;
import com.whereisit.backend.founditem.client.FoundItemLookupClient;
import com.whereisit.backend.founditem.entity.FoundItemSourceType;
import com.whereisit.backend.notification.service.EmailSender;
import com.whereisit.backend.support.ApiTestSupport;
import com.whereisit.backend.support.TestClockConfig;

@DisplayName("API-15 내 신규 후보 이메일 이력")
class NotificationApiTest extends ApiTestSupport {

	@MockitoBean(name = "policeFoundItemLookupClient")
	private FoundItemLookupClient policeClient;

	@MockitoBean(name = "portalFoundItemLookupClient")
	private FoundItemLookupClient portalClient;

	@MockitoBean
	private EmailSender emailSender;

	@Autowired
	private TrackingBatchService trackingBatchService;

	@Test
	@DisplayName("배치가 보낸 알림을 본인 분실물 기준으로 조회할 수 있다")
	void listsNotificationsForOwnedLostItem() throws Exception {
		when(policeClient.sourceType()).thenReturn(FoundItemSourceType.POLICE);
		when(portalClient.sourceType()).thenReturn(FoundItemSourceType.PORTAL);
		when(policeClient.search(any())).thenReturn(List.of());
		when(portalClient.search(any())).thenReturn(List.of());
		doNothing().when(emailSender).send(org.mockito.ArgumentMatchers.anyString(),
				org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString());

		String accessToken = signupAndLogin("user@example.test").get("accessToken").asText();
		JsonNode created = objectMapper.readTree(
				authorizedPostJson("/api/lost-items", accessToken, Map.of("description", "파란색 지갑을 잃어버렸어요"))
						.andReturn().getResponse().getContentAsString()).get("data");
		String lostItemId = created.get("lostItemId").asText();
		authorizedPostJson("/api/lost-items/update/" + lostItemId, accessToken,
				Map.of("conditions", Map.of("searchStartDate", "2026-09-01")))
				.andExpect(status().isOk());
		authorizedPostJson("/api/lost-items/" + lostItemId + "/tracking", accessToken, Map.of())
				.andExpect(status().isOk());

		when(policeClient.search(any())).thenReturn(List.of(new FoundItemListEntry(
				FoundItemSourceType.POLICE, "new-1", "1", "파란색 지갑", "지갑 습득", null, null, null, null, null)));
		clock.setInstant(TestClockConfig.START.plus(Duration.ofDays(1)));
		trackingBatchService.runDailyBatch();
		// AT는 발급시각 기준 30분만 유효하므로, 배치용으로 하루 앞당긴 시계를 다시 되돌리고 조회한다.
		clock.setInstant(TestClockConfig.START);

		authorizedGet("/api/lost-items/" + lostItemId + "/notifications", accessToken)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(1))
				.andExpect(jsonPath("$.data.items[0].status").value("SENT"))
				.andExpect(jsonPath("$.data.items[0].candidateCount").value(1))
				.andExpect(jsonPath("$.data.items[0].recipientEmail").value("user@example.test"));
	}

	@Test
	@DisplayName("다른 회원의 분실물 알림 이력을 조회하면 404")
	void notificationsAreScopedToOwner() throws Exception {
		String ownerToken = signupAndLogin("owner@example.test").get("accessToken").asText();
		String otherToken = signupAndLogin("other@example.test").get("accessToken").asText();
		JsonNode created = objectMapper.readTree(
				authorizedPostJson("/api/lost-items", ownerToken, Map.of("description", "lost wallet"))
						.andReturn().getResponse().getContentAsString()).get("data");

		authorizedGet("/api/lost-items/" + created.get("lostItemId").asText() + "/notifications", otherToken)
				.andExpect(status().isNotFound());
	}
}
