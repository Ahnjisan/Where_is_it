package com.whereisit.backend.tracking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
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
import com.whereisit.backend.founditem.client.FoundItemListEntry;
import com.whereisit.backend.founditem.client.FoundItemLookupClient;
import com.whereisit.backend.founditem.client.FoundItemLookupException;
import com.whereisit.backend.founditem.client.PortalFoundItemNameStorageClient;
import com.whereisit.backend.founditem.entity.FoundItemSourceType;
import com.whereisit.backend.lostitem.entity.LostItemStatus;
import com.whereisit.backend.lostitem.repository.LostItemRepository;
import com.whereisit.backend.notification.service.EmailSendException;
import com.whereisit.backend.notification.service.EmailSender;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractionResult;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractor;
import com.whereisit.backend.support.ApiTestSupport;
import com.whereisit.backend.tracking.TrackingBatchService.TrackingBatchResult;

/**
 * Issue #85 일일 추적 배치. 추적 등록은 실제 API(API-05 → API-17)로 만들고, 외부 호출(포털기관·SMTP)만 Mock으로 둔다.
 * 시계는 2026-09-22 15:00 KST에서 시작한다.
 */
@DisplayName("Issue #85 7일 추적 일일 재검색·만료·이메일 알림")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(scripts = "/sql/cleanup-search-test-data.sql",
		config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
		executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/cleanup-search-test-data.sql",
		config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
		executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TrackingBatchServiceTest extends ApiTestSupport {

	private static final LocalDate LOST_FROM = LocalDate.of(2026, 9, 20);

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

	@BeforeEach
	void setUp() {
		when(policeClient.sourceType()).thenReturn(FoundItemSourceType.POLICE);
		when(portalClient.sourceType()).thenReturn(FoundItemSourceType.PORTAL);
		when(portalNameStorageClient.search(any())).thenReturn(List.of());
		when(aiSearchConditionExtractor.extract(any())).thenReturn(new AiSearchConditionExtractionResult(
				LOST_FROM, LOST_FROM.plusDays(1), "서울역", "지갑", null, "검색 조건을 확인했습니다."));
	}

	@Test
	@DisplayName("새 후보가 생기면 알림 한 건을 만들어 보내고, 그 후보만 알림에 연결하며 최근_자동검색_완료일을 갱신한다")
	void newCandidateIsNotifiedAndLinked() throws Exception {
		startTracking("user@example.test", "ko", List.of(wallet("A", LOST_FROM.plusDays(1))));
		clock.advance(Duration.ofDays(1));
		returnsFromPortal(wallet("A", LOST_FROM.plusDays(1)), wallet("B", LOST_FROM.plusDays(2)));

		TrackingBatchResult result = batchService.runDailyBatch();

		assertThat(result).isEqualTo(new TrackingBatchResult(0, 1, 1, 0));
		verify(emailSender).send(eq("user@example.test"), contains("1건"), contains("카드지갑"));
		assertThat(notifications()).singleElement().satisfies(row -> {
			assertThat(row.get("status")).isEqualTo("SENT");
			assertThat(((Number) row.get("attempt_count")).intValue()).isEqualTo(1);
			assertThat(row.get("error_code")).isNull();
		});
		assertThat(candidates()).containsExactly(
				Map.of("atc_id", "A", "baseline", 1, "current", 1, "notified", 0),
				Map.of("atc_id", "B", "baseline", 0, "current", 1, "notified", 1));
		assertThat(lastAutoSearchDate()).isEqualTo("2026-09-23");
	}

	@Test
	@DisplayName("추적 등록 당일과 이미 처리한 날에는 다시 조회하지 않는다")
	void sameDayRunsDoNothing() throws Exception {
		startTracking("user@example.test", "ko", List.of(wallet("A", LOST_FROM)));
		clearInvocations(portalClient);

		assertThat(batchService.runDailyBatch()).isEqualTo(new TrackingBatchResult(0, 0, 0, 0));

		clock.advance(Duration.ofDays(1));
		returnsFromPortal(wallet("A", LOST_FROM), wallet("B", LOST_FROM.plusDays(2)));
		batchService.runDailyBatch();
		assertThat(batchService.runDailyBatch()).isEqualTo(new TrackingBatchResult(0, 0, 0, 0));

		verify(portalClient, times(1)).searchAllByFoundDate(any(), any());
		verify(emailSender, times(1)).send(anyString(), anyString(), anyString());
		assertThat(notifications()).hasSize(1);
	}

	@Test
	@DisplayName("새 후보가 없으면 메일 없이 최근_자동검색_완료일만 갱신하고, 결과에서 빠진 후보는 현재 결과에서 내린다")
	void noNewCandidateUpdatesDateWithoutEmail() throws Exception {
		startTracking("user@example.test", "ko", List.of(wallet("A", LOST_FROM), wallet("B", LOST_FROM)));
		clock.advance(Duration.ofDays(1));
		returnsFromPortal(wallet("A", LOST_FROM));

		assertThat(batchService.runDailyBatch()).isEqualTo(new TrackingBatchResult(0, 1, 0, 0));

		verify(emailSender, never()).send(anyString(), anyString(), anyString());
		assertThat(notifications()).isEmpty();
		assertThat(lastAutoSearchDate()).isEqualTo("2026-09-23");
		assertThat(candidates()).containsExactly(
				Map.of("atc_id", "A", "baseline", 1, "current", 1, "notified", 0),
				Map.of("atc_id", "B", "baseline", 1, "current", 0, "notified", 0));
	}

	@Test
	@DisplayName("메일 발송이 실패하면 이메일_알림을 FAILED와 오류 코드로 남기고 같은 날 다시 보내지 않는다")
	void mailFailureIsRecordedAsFailed() throws Exception {
		startTracking("user@example.test", "ko", List.of());
		clock.advance(Duration.ofDays(1));
		returnsFromPortal(wallet("B", LOST_FROM.plusDays(2)));
		doThrow(new EmailSendException("MailSendException", null))
				.when(emailSender).send(anyString(), anyString(), anyString());

		assertThat(batchService.runDailyBatch()).isEqualTo(new TrackingBatchResult(0, 1, 0, 0));
		batchService.runDailyBatch();

		assertThat(notifications()).singleElement().satisfies(row -> {
			assertThat(row.get("status")).isEqualTo("FAILED");
			assertThat(row.get("error_code")).isEqualTo("MailSendException");
			assertThat(((Number) row.get("attempt_count")).intValue()).isEqualTo(1);
		});
		verify(emailSender, times(1)).send(anyString(), anyString(), anyString());
	}

	@Test
	@DisplayName("포털기관 조회가 실패하면 최근_자동검색_완료일을 갱신하지 않아 다음 실행에서 다시 조회한다")
	void lookupFailureKeepsLastAutoSearchDate() throws Exception {
		startTracking("user@example.test", "ko", List.of());
		clock.advance(Duration.ofDays(1));
		when(portalClient.searchAllByFoundDate(any(), any()))
				.thenThrow(new FoundItemLookupException(FoundItemSourceType.PORTAL, "99", "TIMEOUT"));

		assertThat(batchService.runDailyBatch()).isEqualTo(new TrackingBatchResult(0, 0, 0, 1));
		assertThat(lastAutoSearchDate()).isEqualTo("2026-09-22");

		returnsFromPortal(wallet("B", LOST_FROM.plusDays(2)));
		assertThat(batchService.runDailyBatch()).isEqualTo(new TrackingBatchResult(0, 1, 1, 0));
		assertThat(lastAutoSearchDate()).isEqualTo("2026-09-23");
	}

	@Test
	@DisplayName("추적_만료_시각이 지나면 검색 없이 EXPIRED로만 바꾼다")
	void expiredTrackingIsExpiredWithoutLookup() throws Exception {
		startTracking("user@example.test", "ko", List.of());
		clearInvocations(portalClient);
		clock.advance(Duration.ofDays(7));

		assertThat(batchService.runDailyBatch()).isEqualTo(new TrackingBatchResult(1, 0, 0, 0));

		verify(portalClient, never()).searchAllByFoundDate(any(), any());
		assertThat(lostItemRepository.findAll()).singleElement()
				.extracting(lostItem -> lostItem.getStatus()).isEqualTo(LostItemStatus.EXPIRED);
	}

	@Test
	@DisplayName("여러 추적 건은 포털기관 조회 한 번을 공유하고, 가장 이른 시작일부터 조회하며, 메일은 각자의 언어로 보낸다")
	void trackedItemsShareOneLookup() throws Exception {
		startTracking("ko@example.test", "ko", List.of());
		when(aiSearchConditionExtractor.extract(any())).thenReturn(new AiSearchConditionExtractionResult(
				LocalDate.of(2026, 9, 10), null, null, "우산", null, "Checked your search conditions."));
		startTracking("en@example.test", "en", List.of());
		clearInvocations(portalClient);
		clock.advance(Duration.ofDays(1));
		returnsFromPortal(wallet("W", LOST_FROM.plusDays(2)),
				entry("U", "장우산", "우산", LocalDate.of(2026, 9, 15)));

		assertThat(batchService.runDailyBatch()).isEqualTo(new TrackingBatchResult(0, 2, 2, 0));

		verify(portalClient, times(1)).searchAllByFoundDate(LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 23));
		verify(emailSender).send(eq("ko@example.test"), contains("[어디갔지]"), contains("카드지갑"));
		verify(emailSender).send(eq("en@example.test"), contains("[Where is it]"), contains("장우산"));
	}

	private void startTracking(String email, String languageCode, List<FoundItemListEntry> baseline) throws Exception {
		returnsFromPortal(baseline.toArray(FoundItemListEntry[]::new));
		signup(email, PASSWORD, languageCode).andExpect(status().isCreated());
		String accessToken = loginData(email).get("accessToken").asText();
		JsonNode created = objectMapper.readTree(authorizedPostJson("/api/lost-items", accessToken,
				Map.of("description", "잃어버린 물건", "languageCode", languageCode))
				.andReturn().getResponse().getContentAsString()).get("data");
		authorizedPostJson("/api/lost-items/" + created.get("lostItem").get("lostItemId").asText() + "/tracking",
				accessToken, Map.of())
				.andExpect(status().isOk());
	}

	private void returnsFromPortal(FoundItemListEntry... entries) {
		doReturn(List.of(entries)).when(portalClient).searchAllByFoundDate(any(), any());
	}

	private FoundItemListEntry wallet(String atcId, LocalDate foundDate) {
		return entry(atcId, "카드지갑", "지갑 > 기타 지갑", foundDate);
	}

	private FoundItemListEntry entry(String atcId, String productName, String categoryName, LocalDate foundDate) {
		return new FoundItemListEntry(FoundItemSourceType.PORTAL, atcId, "1", productName, productName, categoryName,
				"블랙(검정)", foundDate, "서울역 유실물센터", null);
	}

	private List<Map<String, Object>> notifications() {
		return jdbcTemplate.queryForList(
				"select status, attempt_count, error_code from email_notifications order by notification_id");
	}

	private List<Map<String, Object>> candidates() {
		return jdbcTemplate.queryForList("select f.atc_id, c.is_baseline, c.is_current, c.notification_id "
				+ "from lost_item_candidates c join found_items f on f.found_item_id = c.found_item_id order by f.atc_id")
				.stream()
				.map(row -> Map.<String, Object>of(
						"atc_id", row.get("atc_id"),
						"baseline", flag(row.get("is_baseline")),
						"current", flag(row.get("is_current")),
						"notified", row.get("notification_id") == null ? 0 : 1))
				.toList();
	}

	private String lastAutoSearchDate() {
		return lostItemRepository.findAll().get(0).getLastAutoSearchDate().toString();
	}

	/** MySQL은 boolean 컬럼을 bit로 만들어 Boolean을, SQLite는 정수를 돌려준다. */
	private static int flag(Object value) {
		return value instanceof Boolean bool ? (bool ? 1 : 0) : ((Number) value).intValue();
	}
}
