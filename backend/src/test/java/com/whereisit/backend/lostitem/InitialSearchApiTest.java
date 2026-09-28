package com.whereisit.backend.lostitem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import com.fasterxml.jackson.databind.JsonNode;
import com.whereisit.backend.candidate.repository.LostItemCandidateRepository;
import com.whereisit.backend.founditem.client.FoundItemListEntry;
import com.whereisit.backend.founditem.client.FoundItemLookupClient;
import com.whereisit.backend.founditem.client.FoundItemLookupException;
import com.whereisit.backend.founditem.entity.FoundItemSourceType;
import com.whereisit.backend.global.error.BusinessException;
import com.whereisit.backend.lostitem.entity.ChatRole;
import com.whereisit.backend.lostitem.repository.ChatMessageRepository;
import com.whereisit.backend.lostitem.repository.LostItemRepository;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractionResult;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractor;
import com.whereisit.backend.search.error.SearchErrorCode;
import com.whereisit.backend.support.ApiTestSupport;

@DisplayName("API-05 최초 검색 통합")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Sql(scripts = "/sql/cleanup-search-test-data.sql",
		config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
		executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/cleanup-search-test-data.sql",
		config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
		executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class InitialSearchApiTest extends ApiTestSupport {

	@MockitoBean(name = "policeFoundItemLookupClient")
	private FoundItemLookupClient policeClient;

	@MockitoBean(name = "portalFoundItemLookupClient")
	private FoundItemLookupClient portalClient;

	@MockitoBean
	private AiSearchConditionExtractor aiSearchConditionExtractor;

	@Autowired
	private LostItemRepository lostItemRepository;

	@Autowired
	private ChatMessageRepository chatMessageRepository;

	@Autowired
	private LostItemCandidateRepository candidateRepository;

	@BeforeEach
	void setUp() {
		when(policeClient.sourceType()).thenReturn(FoundItemSourceType.POLICE);
		when(portalClient.sourceType()).thenReturn(FoundItemSourceType.PORTAL);
		when(aiSearchConditionExtractor.extract(any())).thenReturn(
				new AiSearchConditionExtractionResult(null, null, null, "검색 조건을 확인했습니다."));
		when(policeClient.search(any())).thenReturn(List.of());
		when(portalClient.search(any())).thenReturn(List.of());
	}

	@Test
	@DisplayName("COMPLETE는 생성 커밋 뒤 트랜잭션 없이 조회하고 201, Location, 정렬 후보를 반환한다")
	void completeSearchCreatesAndReturnsRankedCandidates() throws Exception {
		AtomicBoolean userVisibleBeforeAi = new AtomicBoolean();
		AtomicBoolean aiTransactionActive = new AtomicBoolean(true);
		AtomicBoolean policeTransactionActive = new AtomicBoolean(true);
		AtomicBoolean portalTransactionActive = new AtomicBoolean(true);
		when(aiSearchConditionExtractor.extract(any())).thenAnswer(invocation -> {
			aiTransactionActive.set(TransactionSynchronizationManager.isActualTransactionActive());
			userVisibleBeforeAi.set(chatMessageRepository.findAll().stream()
					.filter(message -> message.getRole() == ChatRole.USER).count() == 1);
			return new AiSearchConditionExtractionResult(null, null, null, "검색 조건을 확인했습니다.");
		});
		when(policeClient.search(any())).thenAnswer(invocation -> {
			policeTransactionActive.set(TransactionSynchronizationManager.isActualTransactionActive());
			return List.of(found(FoundItemSourceType.POLICE, "1", "파란 지갑"));
		});
		when(portalClient.search(any())).thenAnswer(invocation -> {
			portalTransactionActive.set(TransactionSynchronizationManager.isActualTransactionActive());
			return List.of(found(FoundItemSourceType.PORTAL, "2", "검은 우산"));
		});
		String accessToken = signupAndLogin("complete@example.test").get("accessToken").asText();

		var result = authorizedPostJson("/api/lost-items", accessToken,
				Map.of("description", "파란 지갑을 잃어버렸어요", "languageCode", "ko"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.lookupStatus").value("COMPLETE"))
				.andExpect(jsonPath("$.data.candidates.length()").value(2))
				.andExpect(jsonPath("$.data.candidates[0].rank").value(1))
				.andExpect(jsonPath("$.data.candidates[1].rank").value(2))
				.andReturn();

		JsonNode data = objectMapper.readTree(result.getResponse().getContentAsString()).get("data");
		String lostItemId = data.get("lostItem").get("lostItemId").asText();
		assertThat(result.getResponse().getHeader(HttpHeaders.LOCATION))
				.isEqualTo("/api/lost-items/" + lostItemId);
		assertThat(userVisibleBeforeAi.get()).isTrue();
		assertThat(aiTransactionActive.get()).isFalse();
		assertThat(policeTransactionActive.get()).isFalse();
		assertThat(portalTransactionActive.get()).isFalse();
		assertThat(chatMessageRepository.findAll()).extracting(message -> message.getRole())
				.containsExactly(ChatRole.USER, ChatRole.ASSISTANT);
		assertThat(candidateRepository.count()).isEqualTo(2);
	}

	@Test
	@DisplayName("PARTIAL은 201과 warning, 저장되지 않은 임시 후보를 반환한다")
	void partialSearchReturnsTransientCandidates() throws Exception {
		when(policeClient.search(any())).thenReturn(List.of(found(FoundItemSourceType.POLICE, "1", "지갑")));
		when(portalClient.search(any())).thenThrow(
				new FoundItemLookupException(FoundItemSourceType.PORTAL, "99", "TIMEOUT"));
		String accessToken = signupAndLogin("partial@example.test").get("accessToken").asText();

		authorizedPostJson("/api/lost-items", accessToken, Map.of("description", "지갑을 잃어버렸어요"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.lookupStatus").value("PARTIAL"))
				.andExpect(jsonPath("$.data.warnings[0]").value("PARTIAL_SOURCE_RESULT"))
				.andExpect(jsonPath("$.data.candidates[0].candidateId").doesNotExist());

		assertThat(candidateRepository.count()).isZero();
	}

	@Test
	@DisplayName("두 출처 실패 시 502지만 생성된 검색 건과 최초 USER 메시지는 유지한다")
	void bothSourcesFailAfterCreation() throws Exception {
		when(policeClient.search(any())).thenThrow(
				new FoundItemLookupException(FoundItemSourceType.POLICE, "99", "TIMEOUT"));
		when(portalClient.search(any())).thenThrow(
				new FoundItemLookupException(FoundItemSourceType.PORTAL, "99", "TIMEOUT"));
		String accessToken = signupAndLogin("unavailable@example.test").get("accessToken").asText();

		authorizedPostJson("/api/lost-items", accessToken, Map.of("description", "지갑을 잃어버렸어요"))
				.andExpect(status().isBadGateway())
				.andExpect(jsonPath("$.error.code").value("LOST_API_UNAVAILABLE"));

		assertThat(lostItemRepository.count()).isEqualTo(1);
		assertThat(chatMessageRepository.findAll().stream()
				.filter(message -> message.getRole() == ChatRole.USER)).hasSize(1);
	}

	@Test
	@DisplayName("AI 실패 시 생성 데이터는 유지하고 공공데이터 Client는 호출하지 않는다")
	void aiFailureKeepsCreationAndSkipsSources() throws Exception {
		when(aiSearchConditionExtractor.extract(any()))
				.thenThrow(new BusinessException(SearchErrorCode.SEARCH_TIMEOUT));
		String accessToken = signupAndLogin("ai-failure@example.test").get("accessToken").asText();

		authorizedPostJson("/api/lost-items", accessToken, Map.of("description", "지갑을 잃어버렸어요"))
				.andExpect(status().isGatewayTimeout())
				.andExpect(jsonPath("$.error.code").value("SEARCH_TIMEOUT"));

		assertThat(lostItemRepository.count()).isEqualTo(1);
		assertThat(chatMessageRepository.findAll()).extracting(message -> message.getRole())
				.containsExactly(ChatRole.USER);
		verify(policeClient, never()).search(any());
		verify(portalClient, never()).search(any());
	}

	@Test
	@DisplayName("두 출처가 0건이면 COMPLETE 201과 빈 후보를 반환한다")
	void zeroResultsAreSuccessful() throws Exception {
		String accessToken = signupAndLogin("empty@example.test").get("accessToken").asText();

		authorizedPostJson("/api/lost-items", accessToken, Map.of("description", "지갑을 잃어버렸어요"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.data.lookupStatus").value("COMPLETE"))
				.andExpect(jsonPath("$.data.candidates.length()").value(0));
	}

	private FoundItemListEntry found(FoundItemSourceType sourceType, String atcId, String name) {
		return new FoundItemListEntry(sourceType, atcId, "1", name, name, "기타", null,
				null, "보관소", null);
	}
}
