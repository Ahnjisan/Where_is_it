package com.whereisit.backend.lostitem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.databind.JsonNode;
import com.whereisit.backend.candidate.ranking.CandidateRanker;
import com.whereisit.backend.candidate.ranking.initial.InitialSearchCandidateRanker;
import com.whereisit.backend.founditem.client.FoundItemListEntry;
import com.whereisit.backend.founditem.client.FoundItemLookupClient;
import com.whereisit.backend.founditem.client.PortalFoundItemNameStorageClient;
import com.whereisit.backend.founditem.entity.FoundItemSourceType;
import com.whereisit.backend.lostitem.entity.LostItem;
import com.whereisit.backend.lostitem.entity.LostItemStatus;
import com.whereisit.backend.lostitem.repository.LostItemRepository;
import com.whereisit.backend.member.entity.LanguageCode;
import com.whereisit.backend.member.entity.Member;
import com.whereisit.backend.member.repository.MemberRepository;
import com.whereisit.backend.notification.service.EmailSender;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractionResult;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractor;
import com.whereisit.backend.support.ApiTestSupport;

/**
 * Issue #92 API-16 내 추적 분실물 목록. API-05·17은 요청마다 트랜잭션을 따로 열고 닫으므로 테스트 트랜잭션으로
 * 감싸지 않고, 데이터는 전후에 SQL로 지운다. 외부 연동(AI·공공데이터·메일)은 모두 Mock이라 실제로 호출하지 않는다.
 */
@DisplayName("API-16 내 추적 분실물 목록")
@Sql(scripts = "/sql/cleanup-search-test-data.sql",
		config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
		executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/cleanup-search-test-data.sql",
		config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
		executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class TrackedLostItemListApiTest extends ApiTestSupport {

	private static final String URL = "/api/members/me/lost-items";

	/** LostItemPageResponse·LostItemResponse의 JSON 키. DTO를 재사용하므로 API-06과 같아야 한다. */
	private static final Set<String> PAGE_FIELDS = Set.of("items", "page", "size", "totalElements", "totalPages");
	private static final Set<String> ITEM_FIELDS = Set.of("lostItemId", "description", "languageCode", "conditions",
			"status", "notificationEmail", "startedAt", "expiresAt", "lastAutoSearchDate", "currentCandidateCount",
			"createdAt", "updatedAt", "currentCandidate");

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

	@MockitoBean
	private InitialSearchCandidateRanker initialSearchCandidateRanker;

	@MockitoBean
	private EmailSender emailSender;

	@Autowired
	private MemberRepository memberRepository;

	@Autowired
	private LostItemRepository lostItemRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void mockExternal() {
		when(policeClient.sourceType()).thenReturn(FoundItemSourceType.POLICE);
		when(portalClient.sourceType()).thenReturn(FoundItemSourceType.PORTAL);
		when(portalNameStorageClient.search(any())).thenReturn(List.of());
		when(portalClient.searchAllByFoundDate(any(), any())).thenReturn(List.of());
		when(aiSearchConditionExtractor.extract(any())).thenReturn(
				new AiSearchConditionExtractionResult(null, null, null, "지갑", null, "검색 조건을 확인했습니다."));
	}

	@Test
	@DisplayName("토큰 없이 조회하면 401 AUTH_REQUIRED")
	void requiresAuthentication() throws Exception {
		mockMvc.perform(get(URL))
				.andExpect(status().isUnauthorized())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value("AUTH_REQUIRED"));
	}

	@Test
	@DisplayName("본인의 TRACKING·EXPIRED만 반환하고 SEARCHING·삭제한 건·다른 회원의 건은 제외한다")
	void returnsOnlyOwnTrackedItems() throws Exception {
		String token = signupAndLogin("owner@example.test").get("accessToken").asText();
		signupAndLogin("other@example.test");
		Member owner = member("owner@example.test");
		Member other = member("other@example.test");

		Long tracking = save(owner, LostItemStatus.TRACKING, false);
		Long expired = save(owner, LostItemStatus.EXPIRED, false);
		save(owner, LostItemStatus.SEARCHING, false);
		save(owner, LostItemStatus.TRACKING, true);
		save(owner, LostItemStatus.EXPIRED, true);
		save(other, LostItemStatus.TRACKING, false);
		save(other, LostItemStatus.EXPIRED, false);

		JsonNode data = dataOf(tracked(token, "")
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.totalElements").value(2))
				.andExpect(jsonPath("$.data.totalPages").value(1)));

		assertThat(ids(data)).containsExactly(String.valueOf(expired), String.valueOf(tracking));
		assertThat(statuses(data)).containsExactly("EXPIRED", "TRACKING");
	}

	@Test
	@DisplayName("추적한 건이 없으면 404가 아니라 200과 빈 items")
	void emptyResultIsOk() throws Exception {
		String token = signupAndLogin("owner@example.test").get("accessToken").asText();
		save(member("owner@example.test"), LostItemStatus.SEARCHING, false);

		tracked(token, "")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items").isArray())
				.andExpect(jsonPath("$.data.items").isEmpty())
				.andExpect(jsonPath("$.data.page").value(0))
				.andExpect(jsonPath("$.data.size").value(20))
				.andExpect(jsonPath("$.data.totalElements").value(0))
				.andExpect(jsonPath("$.data.totalPages").value(0));
	}

	@Test
	@DisplayName("정렬은 생성 시각 DESC, 같으면 분실물 ID DESC로 고정이고 sort·status 요청값은 무시한다")
	void sortIsFixed() throws Exception {
		String token = signupAndLogin("owner@example.test").get("accessToken").asText();
		Member owner = member("owner@example.test");

		Long first = save(owner, LostItemStatus.TRACKING, false);
		clock.advance(Duration.ofMinutes(1));
		Long secondTie = save(owner, LostItemStatus.EXPIRED, false);
		Long thirdTie = save(owner, LostItemStatus.TRACKING, false);
		clock.advance(Duration.ofMinutes(-2));
		Long oldestButLargestId = save(owner, LostItemStatus.TRACKING, false);
		clock.advance(Duration.ofMinutes(1));

		List<String> expected = List.of(String.valueOf(thirdTie), String.valueOf(secondTie), String.valueOf(first),
				String.valueOf(oldestButLargestId));
		assertThat(ids(dataOf(tracked(token, "")))).containsExactlyElementsOf(expected);
		assertThat(ids(dataOf(tracked(token, "?sort=id,asc")))).containsExactlyElementsOf(expected);
		assertThat(ids(dataOf(tracked(token, "?sort=createdAt,asc&sort=id,asc")))).containsExactlyElementsOf(expected);
		assertThat(ids(dataOf(tracked(token, "?sort=noSuchProperty,desc")))).containsExactlyElementsOf(expected);
		assertThat(ids(dataOf(tracked(token, "?status=TRACKING")))).containsExactlyElementsOf(expected);
	}

	@Test
	@DisplayName("page 기본 0·size 기본 20, size가 50을 넘으면 50, 범위를 넘는 page는 200과 빈 items")
	void pagination() throws Exception {
		String token = signupAndLogin("owner@example.test").get("accessToken").asText();
		Member owner = member("owner@example.test");
		List<String> newestFirst = new ArrayList<>();
		for (int i = 0; i < 52; i++) {
			newestFirst.add(0, String.valueOf(save(owner, LostItemStatus.TRACKING, false)));
			clock.advance(Duration.ofSeconds(1));
		}

		JsonNode defaults = dataOf(tracked(token, "")
				.andExpect(jsonPath("$.data.page").value(0))
				.andExpect(jsonPath("$.data.size").value(20))
				.andExpect(jsonPath("$.data.totalElements").value(52))
				.andExpect(jsonPath("$.data.totalPages").value(3)));
		assertThat(ids(defaults)).containsExactlyElementsOf(newestFirst.subList(0, 20));

		JsonNode secondPage = dataOf(tracked(token, "?page=1&size=20").andExpect(jsonPath("$.data.page").value(1)));
		assertThat(ids(secondPage)).containsExactlyElementsOf(newestFirst.subList(20, 40));

		JsonNode capped = dataOf(tracked(token, "?size=100")
				.andExpect(jsonPath("$.data.size").value(50))
				.andExpect(jsonPath("$.data.totalPages").value(2)));
		assertThat(ids(capped)).containsExactlyElementsOf(newestFirst.subList(0, 50));

		tracked(token, "?page=9")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items").isEmpty())
				.andExpect(jsonPath("$.data.page").value(9))
				.andExpect(jsonPath("$.data.totalElements").value(52));
	}

	@Test
	@DisplayName("음수 page·0 이하 size·숫자가 아닌 값은 전역 Pageable 규칙대로 기본값으로 조회한다(400 아님)")
	void invalidPagingValuesFollowGlobalPageableRule() throws Exception {
		String token = signupAndLogin("owner@example.test").get("accessToken").asText();
		save(member("owner@example.test"), LostItemStatus.TRACKING, false);

		for (String query : List.of("?page=-1", "?size=0", "?size=-5", "?page=abc&size=xyz")) {
			tracked(token, query)
					.andExpect(status().isOk())
					.andExpect(jsonPath("$.data.page").value(0))
					.andExpect(jsonPath("$.data.size").value(20))
					.andExpect(jsonPath("$.data.totalElements").value(1));
		}
	}

	@Test
	@DisplayName("만료 시각이 지난 TRACKING은 EXPIRED로 보여주지만 DB 상태와 행은 바꾸지 않는다")
	void expiredTrackingIsShownAsExpiredWithoutWriting() throws Exception {
		signupAndLogin("owner@example.test");
		Long lostItemId = save(member("owner@example.test"), LostItemStatus.TRACKING, false);

		// AT는 30분이면 만료되므로 시계를 옮긴 뒤 다시 로그인한다.
		clock.advance(Duration.ofDays(7).minusSeconds(1));
		tracked(loginData("owner@example.test").get("accessToken").asText(), "")
				.andExpect(jsonPath("$.data.items[0].status").value("TRACKING"));

		clock.advance(Duration.ofSeconds(1));
		String token = loginData("owner@example.test").get("accessToken").asText();
		List<Map<String, Object>> before = lostItemRows();
		clearExternalInvocations();

		tracked(token, "")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].lostItemId").value(String.valueOf(lostItemId)))
				.andExpect(jsonPath("$.data.items[0].status").value("EXPIRED"))
				.andExpect(jsonPath("$.data.items[0].expiresAt").value("2026-09-29T15:00:00.000000+09:00"));

		assertThat(lostItemRows()).isEqualTo(before);
		assertThat(jdbcTemplate.queryForObject(
				"SELECT status FROM lost_items WHERE lost_item_id = ?", String.class, lostItemId)).isEqualTo("TRACKING");
		verifyNoExternalCalls();
	}

	@Test
	@DisplayName("API-05로 만든 검색 건을 API-17로 추적하면 API-16에 TRACKING으로 나오고, EXPIRED로 바뀌어도 나온다")
	void createTrackThenListFlow() throws Exception {
		String token = signupAndLogin("flow@example.test").get("accessToken").asText();

		JsonNode created = objectMapper.readTree(authorizedPostJson("/api/lost-items", token,
				Map.of("description", "서울역에서 검은 지갑을 잃어버렸어요"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.lostItem.status").value("SEARCHING"))
				.andReturn().getResponse().getContentAsString()).get("data");
		String lostItemId = created.get("lostItem").get("lostItemId").asText();

		tracked(token, "").andExpect(jsonPath("$.data.totalElements").value(0));

		when(portalClient.searchAllByFoundDate(any(), any())).thenReturn(List.of(new FoundItemListEntry(
				FoundItemSourceType.PORTAL, "F1", "1", "검은 지갑", "검은 지갑", "지갑 > 기타 지갑", "블랙(검정)",
				LocalDate.of(2026, 9, 22), "서울역 유실물센터", null)));
		authorizedPostJson("/api/lost-items/" + lostItemId + "/tracking", token, Map.of())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.lostItemId").value(lostItemId))
				.andExpect(jsonPath("$.data.status").value("TRACKING"));

		Integer currentCandidates = jdbcTemplate.queryForObject(
				"SELECT COUNT(*) FROM lost_item_candidates WHERE lost_item_id = ? AND is_current = ?",
				Integer.class, Long.valueOf(lostItemId), true);
		assertThat(currentCandidates).isEqualTo(1);

		List<Map<String, Object>> before = lostItemRows();
		clearExternalInvocations();
		String body = tracked(token, "")
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.items[0].lostItemId").isString())
				.andExpect(jsonPath("$.data.items[0].lostItemId").value(lostItemId))
				.andExpect(jsonPath("$.data.items[0].status").value("TRACKING"))
				.andExpect(jsonPath("$.data.items[0].notificationEmail").value("flow@example.test"))
				.andExpect(jsonPath("$.data.items[0].startedAt").value("2026-09-22T15:00:00.000000+09:00"))
				.andExpect(jsonPath("$.data.items[0].expiresAt").value("2026-09-29T15:00:00.000000+09:00"))
				.andExpect(jsonPath("$.data.items[0].createdAt").value("2026-09-22T15:00:00.000000+09:00"))
				.andExpect(jsonPath("$.data.items[0].lastAutoSearchDate").value("2026-09-22"))
				.andExpect(jsonPath("$.data.items[0].currentCandidateCount").value(currentCandidates))
				.andReturn().getResponse().getContentAsString();
		verifyNoExternalCalls();
		assertThat(lostItemRows()).isEqualTo(before);

		JsonNode data = data(body);
		assertThat(fieldNames(data)).isEqualTo(PAGE_FIELDS);
		assertThat(fieldNames(data.get("items").get(0))).isEqualTo(ITEM_FIELDS);
		assertThat(data.get("items").get(0).has("currentCandidate")).isTrue();
		assertThat(data.get("items").get(0).get("currentCandidate").isNull()).isTrue();

		LostItem lostItem = lostItemRepository.findById(Long.valueOf(lostItemId)).orElseThrow();
		lostItem.expire();
		lostItemRepository.save(lostItem);

		tracked(token, "")
				.andExpect(jsonPath("$.data.totalElements").value(1))
				.andExpect(jsonPath("$.data.items[0].lostItemId").value(lostItemId))
				.andExpect(jsonPath("$.data.items[0].status").value("EXPIRED"));
		verifyNoInteractions(emailSender);
	}

	private ResultActions tracked(String accessToken, String query) throws Exception {
		return authorizedGet(URL + query, accessToken);
	}

	/** Entity 메서드로 상태를 만든 뒤 저장한다(생성 시각은 테스트 시계). 운영 Batch·API-17은 거치지 않는다. */
	private Long save(Member member, LostItemStatus status, boolean deleted) {
		LostItem lostItem = LostItem.create(member, "검은 지갑", LanguageCode.EN);
		LocalDateTime now = LocalDateTime.now(clock);
		if (status != LostItemStatus.SEARCHING) {
			lostItem.activateTracking(now, member.getEmail());
		}
		if (status == LostItemStatus.EXPIRED) {
			lostItem.expire();
		}
		if (deleted) {
			lostItem.delete(now);
		}
		return lostItemRepository.save(lostItem).getId();
	}

	private Member member(String email) {
		return memberRepository.findByEmail(email).orElseThrow();
	}

	private List<Map<String, Object>> lostItemRows() {
		return jdbcTemplate.queryForList("SELECT * FROM lost_items ORDER BY lost_item_id");
	}

	private void clearExternalInvocations() {
		clearInvocations(policeClient, portalClient, portalNameStorageClient, aiSearchConditionExtractor,
				candidateRanker, initialSearchCandidateRanker, emailSender);
	}

	private void verifyNoExternalCalls() {
		verifyNoInteractions(policeClient, portalClient, portalNameStorageClient, aiSearchConditionExtractor,
				candidateRanker, initialSearchCandidateRanker, emailSender);
	}

	private JsonNode dataOf(ResultActions result) throws Exception {
		return data(result.andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
	}

	private JsonNode data(String body) {
		try {
			return objectMapper.readTree(body).get("data");
		}
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	private static List<String> ids(JsonNode data) {
		List<String> ids = new ArrayList<>();
		data.get("items").forEach(item -> ids.add(item.get("lostItemId").asText()));
		return ids;
	}

	private static List<String> statuses(JsonNode data) {
		List<String> statuses = new ArrayList<>();
		data.get("items").forEach(item -> statuses.add(item.get("status").asText()));
		return statuses;
	}

	private static Set<String> fieldNames(JsonNode node) {
		Set<String> names = new java.util.HashSet<>();
		node.fieldNames().forEachRemaining(names::add);
		return names;
	}
}
