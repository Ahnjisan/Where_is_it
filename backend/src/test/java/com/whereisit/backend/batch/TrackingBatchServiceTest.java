package com.whereisit.backend.batch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import com.fasterxml.jackson.databind.JsonNode;
import com.whereisit.backend.candidate.repository.LostItemCandidateRepository;
import com.whereisit.backend.founditem.client.FoundItemListEntry;
import com.whereisit.backend.founditem.client.FoundItemLookupClient;
import com.whereisit.backend.founditem.entity.FoundItemSourceType;
import com.whereisit.backend.lostitem.entity.LostItem;
import com.whereisit.backend.lostitem.entity.LostItemStatus;
import com.whereisit.backend.lostitem.repository.LostItemRepository;
import com.whereisit.backend.notification.entity.EmailNotificationStatus;
import com.whereisit.backend.notification.repository.EmailNotificationRepository;
import com.whereisit.backend.notification.service.EmailSendException;
import com.whereisit.backend.notification.service.EmailSender;
import com.whereisit.backend.support.ApiTestSupport;
import com.whereisit.backend.support.TestClockConfig;

@DisplayName("Issue #37 7일 자동 재검색·추적 만료·이메일 알림")
class TrackingBatchServiceTest extends ApiTestSupport {

	@MockitoBean(name = "policeFoundItemLookupClient")
	private FoundItemLookupClient policeClient;

	@MockitoBean(name = "portalFoundItemLookupClient")
	private FoundItemLookupClient portalClient;

	@MockitoBean
	private EmailSender emailSender;

	@Autowired
	private TrackingBatchService trackingBatchService;

	@Autowired
	private LostItemRepository lostItemRepository;

	@Autowired
	private EmailNotificationRepository notificationRepository;

	@Autowired
	private LostItemCandidateRepository candidateRepository;

	private String lostItemId;

	@BeforeEach
	void activateTracking() throws Exception {
		when(policeClient.sourceType()).thenReturn(FoundItemSourceType.POLICE);
		when(portalClient.sourceType()).thenReturn(FoundItemSourceType.PORTAL);
		when(policeClient.search(any())).thenReturn(List.of());
		when(portalClient.search(any())).thenReturn(List.of());

		String accessToken = signupAndLogin("user@example.test").get("accessToken").asText();
		JsonNode created = objectMapper.readTree(
				authorizedPostJson("/api/lost-items", accessToken, Map.of("description", "파란색 지갑을 잃어버렸어요"))
						.andReturn().getResponse().getContentAsString()).get("data");
		lostItemId = created.get("lostItemId").asText();

		authorizedPostJson("/api/lost-items/update/" + lostItemId, accessToken,
				Map.of("conditions", Map.of("searchStartDate", "2026-09-01")))
				.andExpect(status().isOk());
		authorizedPostJson("/api/lost-items/" + lostItemId + "/tracking", accessToken, Map.of())
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("새 유사 후보가 생기면 이메일 알림을 하나 만들어 보내고 최근_자동검색_완료일을 갱신한다")
	void sendsEmailForNewSimilarCandidate() throws Exception {
		when(policeClient.search(any())).thenReturn(List.of(new FoundItemListEntry(
				FoundItemSourceType.POLICE, "new-1", "1", "파란색 지갑", "지갑 습득", null, null, null, null, null)));
		doNothing().when(emailSender).send(anyString(), anyString(), anyString());
		clock.setInstant(TestClockConfig.START.plus(Duration.ofDays(1)));

		trackingBatchService.runDailyBatch();

		LostItem lostItem = lostItemRepository.findById(Long.valueOf(lostItemId)).orElseThrow();
		assertThat(lostItem.getLastAutoSearchDate()).isEqualTo(clock.instant().atZone(clock.getZone()).toLocalDate());
		assertThat(notificationRepository.findByLostItemIdOrderByCreatedAtDescIdDesc(Long.valueOf(lostItemId),
				org.springframework.data.domain.Pageable.unpaged()).getContent()).hasSize(1);
		var notification = notificationRepository.findByLostItemIdOrderByCreatedAtDescIdDesc(
				Long.valueOf(lostItemId), org.springframework.data.domain.Pageable.unpaged()).getContent().get(0);
		assertThat(notification.getStatus()).isEqualTo(EmailNotificationStatus.SENT);
		assertThat(candidateRepository.findByNotificationId(notification.getId())).hasSize(1);
	}

	@Test
	@DisplayName("메일 발송이 실패하면 이메일_알림 상태를 FAILED로 남긴다")
	void marksFailedWhenSendThrows() throws Exception {
		when(policeClient.search(any())).thenReturn(List.of(new FoundItemListEntry(
				FoundItemSourceType.POLICE, "new-1", "1", "파란색 지갑", "지갑 습득", null, null, null, null, null)));
		doThrow(new EmailSendException("MailSendException", new RuntimeException("boom")))
				.when(emailSender).send(anyString(), anyString(), anyString());
		clock.setInstant(TestClockConfig.START.plus(Duration.ofDays(1)));

		trackingBatchService.runDailyBatch();

		var notification = notificationRepository.findByLostItemIdOrderByCreatedAtDescIdDesc(
				Long.valueOf(lostItemId), org.springframework.data.domain.Pageable.unpaged()).getContent().get(0);
		assertThat(notification.getStatus()).isEqualTo(EmailNotificationStatus.FAILED);
		assertThat(notification.getErrorCode()).isEqualTo("MailSendException");
	}

	@Test
	@DisplayName("같은 날 두 번 실행해도 이메일은 한 건만 만들어진다")
	void doesNotDoubleSendOnSameDay() throws Exception {
		when(policeClient.search(any())).thenReturn(List.of(new FoundItemListEntry(
				FoundItemSourceType.POLICE, "new-1", "1", "파란색 지갑", "지갑 습득", null, null, null, null, null)));
		doNothing().when(emailSender).send(anyString(), anyString(), anyString());
		clock.setInstant(TestClockConfig.START.plus(Duration.ofDays(1)));

		trackingBatchService.runDailyBatch();
		trackingBatchService.runDailyBatch();

		assertThat(notificationRepository.findByLostItemIdOrderByCreatedAtDescIdDesc(Long.valueOf(lostItemId),
				org.springframework.data.domain.Pageable.unpaged()).getContent()).hasSize(1);
	}

	@Test
	@DisplayName("추적 만료시각이 지나면 검색 없이 EXPIRED로만 바꾼다")
	void expiresWithoutSearchingAfterDeadline() {
		clock.setInstant(TestClockConfig.START.plus(Duration.ofDays(8)));

		trackingBatchService.runDailyBatch();

		LostItem lostItem = lostItemRepository.findById(Long.valueOf(lostItemId)).orElseThrow();
		assertThat(lostItem.getStatus()).isEqualTo(LostItemStatus.EXPIRED);
		assertThat(notificationRepository.findByLostItemIdOrderByCreatedAtDescIdDesc(Long.valueOf(lostItemId),
				org.springframework.data.domain.Pageable.unpaged()).getContent()).isEmpty();
	}
}
