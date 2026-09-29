package com.whereisit.backend.search;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import com.fasterxml.jackson.databind.JsonNode;
import com.whereisit.backend.founditem.client.FoundItemListEntry;
import com.whereisit.backend.founditem.client.FoundItemLookupClient;
import com.whereisit.backend.founditem.client.PortalFoundItemNameStorageClient;
import com.whereisit.backend.founditem.entity.FoundItemSourceType;
import com.whereisit.backend.lostitem.entity.LostItem;
import com.whereisit.backend.lostitem.entity.LostItemStatus;
import com.whereisit.backend.lostitem.repository.LostItemRepository;
import com.whereisit.backend.notification.service.EmailSender;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractionResult;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractor;
import com.whereisit.backend.support.ApiTestSupport;
import com.whereisit.backend.tracking.TrackingBatchService;
import com.whereisit.backend.tracking.TrackingBatchService.TrackingBatchResult;

/**
 * Issue #94 API-21 추적 종료. 추적 등록은 실제 API(API-05 → API-17)로 만들고 외부 호출(포털기관·SMTP)만 Mock으로 둔다.
 * 시계는 2026-09-22 15:00 KST에서 시작하므로 추적 만료 시각은 2026-09-29 15:00이다.
 */
@DisplayName("API-21 추적 종료")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(scripts = "/sql/cleanup-search-test-data.sql",
		config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
		executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/cleanup-search-test-data.sql",
		config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
		executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TrackingStopApiTest extends ApiTestSupport {

	private static final String OWNER = "owner@example.test";
	private static final String STARTED_AT = "2026-09-22T15:00:00.000000+09:00";
	private static final String EXPIRES_AT = "2026-09-29T15:00:00.000000+09:00";

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

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Autowired
	private LostItemRepository lostItemRepository;

	@Autowired
	private TransactionTemplate transactionTemplate;

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
	@DisplayName("TRACKING 건을 종료하면 EXPIRED가 되고 시작·만료 시각은 그대로이며 updatedAt이 종료 시각이다")
	void stopsTrackingAndKeepsPeriod() throws Exception {
		String lostItemId = startTracking();
		clock.advance(Duration.ofHours(2));

		stop(lostItemId)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("EXPIRED"))
				.andExpect(jsonPath("$.data.startedAt").value(STARTED_AT))
				.andExpect(jsonPath("$.data.expiresAt").value(EXPIRES_AT))
				.andExpect(jsonPath("$.data.updatedAt").value("2026-09-22T17:00:00.000000+09:00"));

		assertThat(jdbcTemplate.queryForObject("select status from lost_items", String.class)).isEqualTo("EXPIRED");
	}

	@Test
	@DisplayName("반복 호출해도 200이며 상태와 updatedAt이 바뀌지 않는다")
	void repeatedStopIsIdempotent() throws Exception {
		String lostItemId = startTracking();
		clock.advance(Duration.ofHours(2));
		stop(lostItemId).andExpect(status().isOk());

		clock.advance(Duration.ofMinutes(5));
		stop(lostItemId)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("EXPIRED"))
				.andExpect(jsonPath("$.data.updatedAt").value("2026-09-22T17:00:00.000000+09:00"));
	}

	@Test
	@DisplayName("만료 시각이 지났지만 아직 배치가 바꾸지 않은 TRACKING 건도 200으로 EXPIRED가 되고, 종료일은 expiresAt이 더 이르다")
	void overdueTrackingIsExpiredOnStop() throws Exception {
		String lostItemId = startTracking();
		clock.advance(Duration.ofDays(8));
		assertThat(jdbcTemplate.queryForObject("select status from lost_items", String.class)).isEqualTo("TRACKING");

		stop(lostItemId)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("EXPIRED"))
				.andExpect(jsonPath("$.data.expiresAt").value(EXPIRES_AT))
				.andExpect(jsonPath("$.data.updatedAt").value("2026-09-30T15:00:00.000000+09:00"));
		assertThat(jdbcTemplate.queryForObject("select status from lost_items", String.class)).isEqualTo("EXPIRED");
	}

	@Test
	@DisplayName("종료한 건은 다음 날 배치가 조회하지 않고 새 습득물이 있어도 메일을 보내지 않는다")
	void stoppedTrackingIsExcludedFromBatch() throws Exception {
		String lostItemId = startTracking();
		stop(lostItemId).andExpect(status().isOk());
		clearInvocations(portalClient);
		clock.advance(Duration.ofDays(1));
		doReturn(List.of(wallet("NEW"))).when(portalClient).searchAllByFoundDate(any(), any());

		assertThat(batchService.runDailyBatch()).isEqualTo(new TrackingBatchResult(0, 0, 0, 0));

		verify(portalClient, never()).searchAllByFoundDate(any(), any());
		verify(emailSender, never()).send(anyString(), anyString(), anyString());
		assertThat(jdbcTemplate.queryForObject("select count(*) from email_notifications", Long.class)).isZero();
	}

	@Test
	@DisplayName("배치가 포털기관을 조회하는 동안 종료되면 그 건의 결과를 반영하지 않고 메일도 보내지 않는다")
	void stopDuringBatchLookupWins() throws Exception {
		String lostItemId = startTracking();
		clock.advance(Duration.ofDays(1));
		String accessToken = loginData(OWNER).get("accessToken").asText();
		doAnswer(invocation -> {
			authorizedPostJson("/api/lost-items/" + lostItemId + "/tracking/stop", accessToken, Map.of())
					.andExpect(status().isOk());
			return List.of(wallet("NEW"));
		}).when(portalClient).searchAllByFoundDate(any(), any());

		assertThat(batchService.runDailyBatch()).isEqualTo(new TrackingBatchResult(0, 1, 0, 0));

		verify(emailSender, never()).send(anyString(), anyString(), anyString());
		assertThat(jdbcTemplate.queryForObject("select count(*) from lost_item_candidates", Long.class)).isZero();
		assertThat(jdbcTemplate.queryForObject("select status from lost_items", String.class)).isEqualTo("EXPIRED");
	}

	@Test
	@DisplayName("종료한 건은 API-17로 다시 등록할 수 없고 API-16 목록에는 EXPIRED로 남는다")
	void stoppedTrackingCannotBeReactivated() throws Exception {
		String lostItemId = startTracking();
		stop(lostItemId).andExpect(status().isOk());
		String accessToken = loginData(OWNER).get("accessToken").asText();

		authorizedPostJson("/api/lost-items/" + lostItemId + "/tracking", accessToken, Map.of())
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("TRACKING_EXPIRED"));
		authorizedGet("/api/members/me/lost-items", accessToken)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].lostItemId").value(lostItemId))
				.andExpect(jsonPath("$.data.items[0].status").value("EXPIRED"));
	}

	@Test
	@DisplayName("추적을 등록하지 않은 SEARCHING 건은 409 TRACKING_NOT_STARTED이고 상태가 바뀌지 않는다")
	void searchingItemCannotBeStopped() throws Exception {
		String accessToken = signupAndLogin(OWNER).get("accessToken").asText();
		String lostItemId = createLostItem(accessToken);

		stop(lostItemId)
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("TRACKING_NOT_STARTED"));
		assertThat(jdbcTemplate.queryForObject("select status from lost_items", String.class)).isEqualTo("SEARCHING");
	}

	@Test
	@DisplayName("다른 회원의 건, 없는 건, 삭제한 건은 404 RESOURCE_NOT_FOUND이다")
	void notOwnedMissingOrDeletedIsNotFound() throws Exception {
		String lostItemId = startTracking();
		String otherToken = signupAndLogin("other@example.test").get("accessToken").asText();

		authorizedPostJson("/api/lost-items/" + lostItemId + "/tracking/stop", otherToken, Map.of())
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
		stop("999999")
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
		assertThat(jdbcTemplate.queryForObject("select status from lost_items", String.class)).isEqualTo("TRACKING");

		String accessToken = loginData(OWNER).get("accessToken").asText();
		authorizedPostJson("/api/lost-items/delete/" + lostItemId, accessToken, Map.of())
				.andExpect(status().isNoContent());
		stop(lostItemId)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
	}

	@Test
	@DisabledIfSystemProperty(named = "spring.profiles.active", matches = "sqlite",
			disabledReason = "행 잠금 대기를 재현하려면 커넥션 2개가 동시에 필요하다. 커넥션 풀이 1개인 SQLite에서는 확인할 수 없어 MySQL에서만 확인한다.")
	@DisplayName("배치가 행을 잠그고 갱신하는 중에 종료하면 잠금이 풀린 뒤 최신 값을 읽어 배치의 갱신을 덮어쓰지 않는다")
	void stopWaitsForRowLockAndKeepsConcurrentUpdate() throws Exception {
		String lostItemId = startTracking();
		CountDownLatch locked = new CountDownLatch(1);
		CountDownLatch release = new CountDownLatch(1);
		ExecutorService executor = Executors.newFixedThreadPool(2);
		try {
			Future<?> batchLikeUpdate = executor.submit(() -> transactionTemplate.executeWithoutResult(status -> {
				LostItem lostItem = lostItemRepository.findActiveByIdForUpdate(Long.valueOf(lostItemId)).orElseThrow();
				lostItem.markAutoSearchCompleted(LocalDate.of(2026, 9, 23));
				locked.countDown();
				await(release);
			}));
			assertThat(locked.await(10, TimeUnit.SECONDS)).isTrue();
			Future<Integer> stopRequest = executor.submit(() -> stop(lostItemId).andReturn().getResponse().getStatus());
			Thread.sleep(300);
			release.countDown();

			batchLikeUpdate.get(10, TimeUnit.SECONDS);
			assertThat(stopRequest.get(30, TimeUnit.SECONDS)).isEqualTo(200);
		}
		finally {
			release.countDown();
			executor.shutdownNow();
		}

		LostItem stopped = lostItemRepository.findById(Long.valueOf(lostItemId)).orElseThrow();
		assertThat(stopped.getStatus()).isEqualTo(LostItemStatus.EXPIRED);
		assertThat(stopped.getLastAutoSearchDate()).isEqualTo(LocalDate.of(2026, 9, 23));
	}

	/** 토큰은 30분 뒤 만료되므로 시계를 옮긴 뒤에도 쓸 수 있게 매번 새로 로그인한다. */
	private ResultActions stop(String lostItemId) throws Exception {
		String accessToken = loginData(OWNER).get("accessToken").asText();
		return authorizedPostJson("/api/lost-items/" + lostItemId + "/tracking/stop", accessToken, Map.of());
	}

	private String startTracking() throws Exception {
		String accessToken = signupAndLogin(OWNER).get("accessToken").asText();
		String lostItemId = createLostItem(accessToken);
		authorizedPostJson("/api/lost-items/" + lostItemId + "/tracking", accessToken, Map.of())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.status").value("TRACKING"));
		return lostItemId;
	}

	private String createLostItem(String accessToken) throws Exception {
		JsonNode created = objectMapper.readTree(authorizedPostJson("/api/lost-items", accessToken,
				Map.of("description", "검은 지갑을 잃어버렸어요", "languageCode", "ko"))
				.andReturn().getResponse().getContentAsString()).get("data");
		return created.get("lostItem").get("lostItemId").asText();
	}

	private FoundItemListEntry wallet(String atcId) {
		return new FoundItemListEntry(FoundItemSourceType.PORTAL, atcId, "1", "카드지갑", "카드지갑", "지갑 > 기타 지갑",
				"블랙(검정)", LocalDate.of(2026, 9, 23), "서울역 유실물센터", null);
	}

	private static void await(CountDownLatch latch) {
		try {
			latch.await(10, TimeUnit.SECONDS);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}
}
