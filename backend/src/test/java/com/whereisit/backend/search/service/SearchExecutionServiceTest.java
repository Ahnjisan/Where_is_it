package com.whereisit.backend.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import com.whereisit.backend.candidate.repository.LostItemCandidateRepository;
import com.whereisit.backend.candidate.ranking.CandidateRankingRequest;
import com.whereisit.backend.candidate.ranking.CandidateRankingResult;
import com.whereisit.backend.candidate.ranking.RankedCandidate;
import com.whereisit.backend.candidate.ranking.SimpleTextSimilarityRanker;
import com.whereisit.backend.candidate.ranking.openai.OpenAiCandidateRanker;
import com.whereisit.backend.founditem.client.FoundItemListEntry;
import com.whereisit.backend.founditem.client.FoundItemLookupClient;
import com.whereisit.backend.founditem.entity.FoundItemSourceType;
import com.whereisit.backend.global.error.BusinessException;
import com.whereisit.backend.lostitem.dto.CreateLostItemRequest;
import com.whereisit.backend.lostitem.dto.SearchConditionsPatch;
import com.whereisit.backend.lostitem.dto.UpdateLostItemRequest;
import com.whereisit.backend.lostitem.entity.ChatMessage;
import com.whereisit.backend.lostitem.entity.ChatRole;
import com.whereisit.backend.lostitem.entity.LostItem;
import com.whereisit.backend.lostitem.repository.ChatMessageRepository;
import com.whereisit.backend.lostitem.repository.LostItemRepository;
import com.whereisit.backend.lostitem.service.LostItemService;
import com.whereisit.backend.member.entity.LanguageCode;
import com.whereisit.backend.member.entity.Member;
import com.whereisit.backend.member.repository.MemberRepository;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractionMode;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractionRequest;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractionResult;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractor;
import com.whereisit.backend.search.dto.RunSearchRequest;
import com.whereisit.backend.search.dto.SearchMode;
import com.whereisit.backend.search.error.SearchErrorCode;
import com.whereisit.backend.support.TestClockConfig;

import jakarta.persistence.LockModeType;

@SpringBootTest
@Import(TestClockConfig.class)
@Sql(scripts = "/sql/cleanup-search-test-data.sql",
		config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
		executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD)
@Sql(scripts = "/sql/cleanup-search-test-data.sql",
		config = @SqlConfig(transactionMode = SqlConfig.TransactionMode.ISOLATED),
		executionPhase = Sql.ExecutionPhase.AFTER_TEST_METHOD)
class SearchExecutionServiceTest {

	@Autowired SearchExecutionService searchExecutionService;
	@Autowired LostItemService lostItemService;
	@Autowired MemberRepository memberRepository;
	@Autowired LostItemRepository lostItemRepository;
	@Autowired ChatMessageRepository chatMessageRepository;
	@Autowired LostItemCandidateRepository candidateRepository;
	@Autowired TransactionTemplate transactionTemplate;

	@MockitoBean AiSearchConditionExtractor extractor;
	@MockitoBean OpenAiCandidateRanker candidateRanker;
	@MockitoBean(name = "policeFoundItemLookupClient") FoundItemLookupClient policeClient;
	@MockitoBean(name = "portalFoundItemLookupClient") FoundItemLookupClient portalClient;

	@BeforeEach
	void setUp() {
		when(policeClient.sourceType()).thenReturn(FoundItemSourceType.POLICE);
		when(portalClient.sourceType()).thenReturn(FoundItemSourceType.PORTAL);
		when(policeClient.search(any())).thenReturn(List.of());
		when(portalClient.search(any())).thenReturn(List.of());
		when(candidateRanker.rank(any())).thenAnswer(invocation -> successfulRanking(invocation.getArgument(0)));
	}

	@Test
	void initialUsesDescriptionOnceWithoutAddingUserAndPreservesProtectedConditions() {
		Ids ids = createItem("initial@example.test", "파란 지갑을 서울역에서 잃어버렸어요");
		lostItemService.update(ids.memberId(), ids.lostItemId(), new UpdateLostItemRequest(null, null,
				new SearchConditionsPatch("LARGE", "MIDDLE", "BLUE", "SEOUL", null, null, null,
						LocalDate.of(2026, 9, 1)), null));
		AtomicBoolean aiTransactionActive = new AtomicBoolean(true);
		when(extractor.extract(any())).thenAnswer(invocation -> {
			aiTransactionActive.set(TransactionSynchronizationManager.isActualTransactionActive());
			return new AiSearchConditionExtractionResult(
					LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 21), "서울역", "조건을 반영했습니다.");
		});

		searchExecutionService.execute(ids.memberId(), ids.lostItemId(),
				new RunSearchRequest(SearchMode.INITIAL, null, null));

		ArgumentCaptor<AiSearchConditionExtractionRequest> captor =
				ArgumentCaptor.forClass(AiSearchConditionExtractionRequest.class);
		verify(extractor).extract(captor.capture());
		assertThat(captor.getValue().userNaturalLanguage()).isEqualTo("파란 지갑을 서울역에서 잃어버렸어요");
		assertThat(captor.getValue().currentDescription()).isEqualTo("파란 지갑을 서울역에서 잃어버렸어요");
		assertThat(aiTransactionActive.get()).isFalse();

		List<ChatMessage> messages = messages(ids.lostItemId());
		assertThat(messages).extracting(ChatMessage::getRole)
				.containsExactly(ChatRole.USER, ChatRole.ASSISTANT);
		LostItem saved = transactionTemplate.execute(status -> lostItemRepository.findById(ids.lostItemId()).orElseThrow());
		assertThat(saved.getCategoryLargeCode()).isEqualTo("LARGE");
		assertThat(saved.getCategoryMiddleCode()).isEqualTo("MIDDLE");
		assertThat(saved.getColorCode()).isEqualTo("BLUE");
		assertThat(saved.getRegionCode()).isEqualTo("SEOUL");
		assertThat(saved.getSearchStartDate()).isEqualTo(LocalDate.of(2026, 9, 1));
		assertThat(saved.getLostDateFrom()).isEqualTo(LocalDate.of(2026, 9, 20));
		assertThat(saved.getLostPlaceText()).isEqualTo("서울역");
		assertThat(saved.getMember().getId()).isEqualTo(ids.memberId());
		assertThat(saved.getStatus().name()).isEqualTo("SEARCHING");
		assertThat(saved.getStartedAt()).isNull();
		assertThat(saved.getExpiresAt()).isNull();
		assertThat(saved.getNotificationEmail()).isNull();
	}

	@Test
	void textCommitsUserBeforeAiAndCallsAllExternalPortsWithoutTransaction() {
		Ids ids = createItem("text@example.test", "지갑을 잃어버렸어요");
		AtomicBoolean userVisibleBeforeAi = new AtomicBoolean();
		AtomicBoolean aiTransactionActive = new AtomicBoolean(true);
		AtomicBoolean policeTransactionActive = new AtomicBoolean(true);
		AtomicBoolean portalTransactionActive = new AtomicBoolean(true);
		AtomicBoolean rankingTransactionActive = new AtomicBoolean(true);
		when(extractor.extract(any())).thenAnswer(invocation -> {
			aiTransactionActive.set(TransactionSynchronizationManager.isActualTransactionActive());
			userVisibleBeforeAi.set(chatMessageRepository.count() == 2);
			return new AiSearchConditionExtractionResult(null, null, "강남역", "장소를 반영했습니다.");
		});
		when(policeClient.search(any())).thenAnswer(invocation -> {
			policeTransactionActive.set(TransactionSynchronizationManager.isActualTransactionActive());
			return List.of(new FoundItemListEntry(
					FoundItemSourceType.POLICE, "tx", "1", "지갑", null, null, null, null, null, null));
		});
		when(portalClient.search(any())).thenAnswer(invocation -> {
			portalTransactionActive.set(TransactionSynchronizationManager.isActualTransactionActive());
			return List.of();
		});
		doAnswer(invocation -> {
			rankingTransactionActive.set(TransactionSynchronizationManager.isActualTransactionActive());
			return successfulRanking(invocation.getArgument(0));
		}).when(candidateRanker).rank(any());

		searchExecutionService.execute(ids.memberId(), ids.lostItemId(),
				new RunSearchRequest(SearchMode.TEXT, "강남역 근처였어요", null));

		assertThat(userVisibleBeforeAi.get()).isTrue();
		assertThat(aiTransactionActive.get()).isFalse();
		assertThat(policeTransactionActive.get()).isFalse();
		assertThat(portalTransactionActive.get()).isFalse();
		assertThat(rankingTransactionActive.get()).isFalse();
		assertThat(messages(ids.lostItemId())).extracting(ChatMessage::getRole)
				.containsExactly(ChatRole.USER, ChatRole.USER, ChatRole.ASSISTANT);
	}

	@Test
	void aiFailureKeepsCommittedUserAndExistingCandidateAndSkipsFoundItemApis() {
		Ids ids = createItem("failure@example.test", "지갑을 잃어버렸어요");
		FoundItemListEntry found = new FoundItemListEntry(
				FoundItemSourceType.POLICE, "1", "1", "지갑", null, null, null, null, null, null);
		when(policeClient.search(any())).thenReturn(List.of(found));
		searchExecutionService.execute(ids.memberId(), ids.lostItemId(), new RunSearchRequest(
				SearchMode.FILTER, null, new SearchConditionsPatch(null, null, null, null,
						LocalDate.of(2026, 9, 10), LocalDate.of(2026, 9, 20), "기존 장소", null)));
		assertThat(candidateRepository.count()).isEqualTo(1);
		clearInvocations(policeClient, portalClient);
		when(extractor.extract(any())).thenThrow(new BusinessException(SearchErrorCode.SEARCH_TIMEOUT));

		assertThatThrownBy(() -> searchExecutionService.execute(ids.memberId(), ids.lostItemId(),
				new RunSearchRequest(SearchMode.TEXT, "추가 설명", null)))
				.isInstanceOf(BusinessException.class);

		assertThat(messages(ids.lostItemId())).extracting(ChatMessage::getRole)
				.containsExactly(ChatRole.USER, ChatRole.ASSISTANT, ChatRole.USER);
		assertThat(candidateRepository.count()).isEqualTo(1);
		LostItem saved = transactionTemplate.execute(status -> lostItemRepository.findById(ids.lostItemId()).orElseThrow());
		assertThat(saved.getLostPlaceText()).isEqualTo("기존 장소");
		verify(policeClient, never()).search(any());
		verify(portalClient, never()).search(any());
	}

	@Test
	void filterSkipsAiAndStoresOnlyFixedAssistant() {
		Ids ids = createItem("filter@example.test", "지갑을 잃어버렸어요");

		var response = searchExecutionService.execute(ids.memberId(), ids.lostItemId(), new RunSearchRequest(
				SearchMode.FILTER, null, new SearchConditionsPatch(null, null, "BLUE", null,
						null, null, null, null)));

		verify(extractor, never()).extract(any());
		assertThat(response.assistantMessage().role()).isEqualTo("ASSISTANT");
		assertThat(messages(ids.lostItemId())).extracting(ChatMessage::getRole)
				.containsExactly(ChatRole.USER, ChatRole.ASSISTANT);
	}

	@Test
	void legacyInitialAndTextRequestSixFieldsAndNeverStoreColorName() {
		Ids ids = createItem("legacy-color@example.test", "검은 지갑을 잃어버렸어요");
		// 모드와 관계없이 색상명을 돌려주는 Stub이어도 API-11은 저장하지 않아야 한다.
		when(extractor.extract(any())).thenReturn(new AiSearchConditionExtractionResult(
				null, null, "서울역", "지갑", null, "검정", "조건을 반영했습니다."));

		searchExecutionService.execute(ids.memberId(), ids.lostItemId(),
				new RunSearchRequest(SearchMode.INITIAL, null, null));
		searchExecutionService.execute(ids.memberId(), ids.lostItemId(),
				new RunSearchRequest(SearchMode.TEXT, "검은색이었어요", null));
		searchExecutionService.execute(ids.memberId(), ids.lostItemId(), new RunSearchRequest(
				SearchMode.FILTER, null, new SearchConditionsPatch(null, null, "BLUE", null, null, null, null, null)));

		ArgumentCaptor<AiSearchConditionExtractionRequest> captor =
				ArgumentCaptor.forClass(AiSearchConditionExtractionRequest.class);
		verify(extractor, org.mockito.Mockito.times(2)).extract(captor.capture());
		assertThat(captor.getAllValues()).extracting(AiSearchConditionExtractionRequest::mode)
				.containsOnly(AiSearchConditionExtractionMode.SEARCH_CONDITIONS);
		LostItem saved = transactionTemplate.execute(status -> lostItemRepository.findById(ids.lostItemId()).orElseThrow());
		assertThat(saved.getColorName()).isNull();
		assertThat(saved.getProductNameKeyword()).isEqualTo("지갑");
		assertThat(saved.getColorCode()).isEqualTo("BLUE");
	}

	private Ids createItem(String email, String description) {
		Member member = memberRepository.save(Member.create(email, "hash", LanguageCode.KO));
		String lostItemId = lostItemService.create(member.getId(), new CreateLostItemRequest(description, "ko"))
				.lostItemId();
		return new Ids(member.getId(), Long.valueOf(lostItemId));
	}

	private List<ChatMessage> messages(Long lostItemId) {
		return chatMessageRepository.findByLostItemIdOrderByIdAsc(lostItemId, Limit.of(100));
	}

	@Test
	void deduplicatesNaturalKeysAndPreselectsStableTopOneHundred() {
		Ids ids = createItem("limit@example.test", "찾는 설명과 겹치지 않음");
		List<FoundItemListEntry> entries = new ArrayList<>();
		for (int i = 0; i <= 100; i++) {
			entries.add(new FoundItemListEntry(FoundItemSourceType.POLICE, "%03d".formatted(i), "1",
					"item-" + i, null, null, null, LocalDate.of(2026, 1, 1).plusDays(i), null, null));
		}
		entries.add(entries.get(100));
		when(policeClient.search(any())).thenReturn(entries);
		ArgumentCaptor<CandidateRankingRequest> rankingCaptor = ArgumentCaptor.forClass(CandidateRankingRequest.class);

		var response = searchExecutionService.execute(ids.memberId(), ids.lostItemId(), new RunSearchRequest(
				SearchMode.FILTER, null, new SearchConditionsPatch(null, null, null, null, null, null, null, null)));

		verify(candidateRanker).rank(rankingCaptor.capture());
		assertThat(rankingCaptor.getValue().candidates()).hasSize(100);
		assertThat(rankingCaptor.getValue().candidates().get(0).productName()).isEqualTo("item-100");
		assertThat(rankingCaptor.getValue().candidates()).extracting(candidate -> candidate.candidateKey())
				.containsExactlyElementsOf(java.util.stream.IntStream.rangeClosed(1, 100)
						.mapToObj(i -> "c" + i).toList());
		assertThat(response.warnings()).contains("RESULT_LIMIT_REACHED");
		assertThat(response.candidates()).hasSize(100);
	}

	@Test
	void changedSnapshotFailsWithItemBusyBeforePersistence() {
		Ids ids = createItem("busy@example.test", "원래 설명");
		when(policeClient.search(any())).thenReturn(List.of(new FoundItemListEntry(
				FoundItemSourceType.POLICE, "busy", "1", "지갑", null, null, null, null, null, null)));
		doAnswer(invocation -> {
			lostItemService.update(ids.memberId(), ids.lostItemId(),
					new UpdateLostItemRequest("동시에 변경된 설명", null, null, null));
			return successfulRanking(invocation.getArgument(0));
		}).when(candidateRanker).rank(any());

		assertThatThrownBy(() -> searchExecutionService.execute(ids.memberId(), ids.lostItemId(),
				new RunSearchRequest(SearchMode.FILTER, null,
						new SearchConditionsPatch(null, null, null, null, null, null, null, null))))
				.isInstanceOfSatisfying(BusinessException.class,
						exception -> assertThat(exception.getErrorCode()).isEqualTo(SearchErrorCode.ITEM_BUSY));
		assertThat(candidateRepository.count()).isZero();
	}

	@Test
	void changedSnapshotDuringConditionExtractionFailsBeforeApplyingAiResult() {
		Ids ids = createItem("ai-busy@example.test", "원래 설명");
		when(extractor.extract(any())).thenAnswer(invocation -> {
			lostItemService.update(ids.memberId(), ids.lostItemId(),
					new UpdateLostItemRequest("동시에 변경된 설명", null, null, null));
			return new AiSearchConditionExtractionResult(
					LocalDate.of(2026, 9, 20), LocalDate.of(2026, 9, 21), "서울역", "조건을 반영했습니다.");
		});

		assertThatThrownBy(() -> searchExecutionService.execute(ids.memberId(), ids.lostItemId(),
				new RunSearchRequest(SearchMode.INITIAL, null, null)))
				.isInstanceOfSatisfying(BusinessException.class,
						exception -> assertThat(exception.getErrorCode()).isEqualTo(SearchErrorCode.ITEM_BUSY));
		verify(policeClient, never()).search(any());
		verify(portalClient, never()).search(any());
	}

	@Test
	void completeRankingFallbackPreservesExistingCurrentCandidates() {
		Ids ids = createItem("fallback-cache@example.test", "지갑");
		when(policeClient.search(any())).thenReturn(List.of(new FoundItemListEntry(
				FoundItemSourceType.POLICE, "old", "1", "지갑", null, null, null, null, null, null)));
		searchExecutionService.execute(ids.memberId(), ids.lostItemId(), new RunSearchRequest(
				SearchMode.FILTER, null, new SearchConditionsPatch(null, null, null, null, null, null, null, null)));
		Long oldFoundItemId = candidateRepository.findByLostItemIdAndCurrentTrue(ids.lostItemId())
				.get(0).getFoundItem().getId();

		when(policeClient.search(any())).thenReturn(List.of(new FoundItemListEntry(
				FoundItemSourceType.POLICE, "new", "1", "새 지갑", null, null, null, null, null, null)));
		doAnswer(invocation -> new SimpleTextSimilarityRanker().rank(invocation.getArgument(0)))
				.when(candidateRanker).rank(any());

		var response = searchExecutionService.execute(ids.memberId(), ids.lostItemId(), new RunSearchRequest(
				SearchMode.FILTER, null, new SearchConditionsPatch(null, null, null, null, null, null, null, null)));

		assertThat(response.lookupStatus()).isEqualTo("COMPLETE");
		assertThat(response.rankingStatus()).isEqualTo("UNAVAILABLE");
		assertThat(response.persisted()).isFalse();
		assertThat(response.warnings()).contains("AI_RANKING_UNAVAILABLE");
		assertThat(response.candidates()).singleElement().satisfies(candidate -> {
			assertThat(candidate.candidateId()).isNull();
			assertThat(candidate.isCurrent()).isFalse();
		});
		assertThat(candidateRepository.findByLostItemIdAndCurrentTrue(ids.lostItemId()))
				.singleElement().satisfies(candidate -> assertThat(candidate.getFoundItem().getId()).isEqualTo(oldFoundItemId));
		assertThat(candidateRepository.count()).isEqualTo(1);
	}

	@Test
	void completeZeroCandidatesClearsExistingCurrentCandidatesWithoutRanking() {
		Ids ids = createItem("zero-cache@example.test", "지갑");
		when(policeClient.search(any())).thenReturn(List.of(new FoundItemListEntry(
				FoundItemSourceType.POLICE, "old", "1", "지갑", null, null, null, null, null, null)));
		searchExecutionService.execute(ids.memberId(), ids.lostItemId(), new RunSearchRequest(
				SearchMode.FILTER, null, new SearchConditionsPatch(null, null, null, null, null, null, null, null)));
		clearInvocations(candidateRanker);
		when(policeClient.search(any())).thenReturn(List.of());

		var response = searchExecutionService.execute(ids.memberId(), ids.lostItemId(), new RunSearchRequest(
				SearchMode.FILTER, null, new SearchConditionsPatch(null, null, null, null, null, null, null, null)));

		verify(candidateRanker, never()).rank(any());
		assertThat(response.rankingStatus()).isEqualTo("NOT_RUN");
		assertThat(response.persisted()).isTrue();
		assertThat(response.candidates()).isEmpty();
		assertThat(candidateRepository.findByLostItemIdAndCurrentTrue(ids.lostItemId())).isEmpty();
	}

	@Test
	void lostItemFinalizationRepositoryMethodUsesPessimisticWriteLock() throws Exception {
		Method method = LostItemRepository.class.getMethod(
				"findOwnedActiveByIdForUpdate", Long.class, Long.class);

		assertThat(method.getAnnotation(Lock.class)).isNotNull();
		assertThat(method.getAnnotation(Lock.class).value()).isEqualTo(LockModeType.PESSIMISTIC_WRITE);
	}

	private CandidateRankingResult successfulRanking(CandidateRankingRequest request) {
		List<RankedCandidate> ranked = new java.util.ArrayList<>();
		for (int i = 0; i < request.candidates().size(); i++) {
			ranked.add(new RankedCandidate(request.candidates().get(i).candidateKey(), i + 1,
					"AI 추천 이유", true));
		}
		return CandidateRankingResult.success(ranked);
	}

	private record Ids(Long memberId, Long lostItemId) {
	}
}
