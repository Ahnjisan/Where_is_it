package com.whereisit.backend.e2e;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;

import com.whereisit.backend.candidate.ranking.CandidateRankingRequest;
import com.whereisit.backend.candidate.ranking.CandidateRankingResult;
import com.whereisit.backend.candidate.ranking.RankedCandidate;
import com.whereisit.backend.candidate.ranking.initial.InitialSearchCandidateRanker;
import com.whereisit.backend.founditem.client.FoundItemDetailClient;
import com.whereisit.backend.founditem.client.FoundItemListEntry;
import com.whereisit.backend.founditem.client.FoundItemLookupClient;
import com.whereisit.backend.founditem.client.PortalFoundItemNameStorageClient;
import com.whereisit.backend.founditem.client.PortalFoundItemSearchQuery;
import com.whereisit.backend.founditem.entity.FoundItemSourceType;
import com.whereisit.backend.notification.service.EmailSender;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractionResult;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractor;

/** E2E가 외부 네트워크와 SMTP에 도달하지 못하도록 모든 외부 경계를 결정적으로 대체한다. */
@Profile("e2e")
@Configuration(proxyBeanMethods = false)
public class E2eStubConfiguration {

	public static final String DESCRIPTION = "I lost a black wallet near Seoul Station.";
	public static final String ASSISTANT_MESSAGE =
			"I found two deterministic test candidates near Seoul Station.";
	public static final String RANK_ONE_REASON =
			"The item, color, and Seoul Station location match.";
	public static final String RANK_TWO_REASON =
			"The color and item type match the description.";

	private static final String LOCAL_DATA_IMAGE =
			"data:image/svg+xml,%3Csvg%20xmlns%3D%22http%3A%2F%2Fwww.w3.org%2F2000%2Fsvg%22%20"
					+ "width%3D%222%22%20height%3D%222%22%3E%3Crect%20width%3D%222%22%20height%3D%222%22%20"
					+ "fill%3D%22black%22%2F%3E%3C%2Fsvg%3E";

	@Bean
	@Primary
	AiSearchConditionExtractor e2eAiSearchConditionExtractor() {
		return request -> {
			if (!DESCRIPTION.equals(request.userNaturalLanguage()) || !"en".equals(request.languageCode())) {
				throw new IllegalArgumentException("Unexpected E2E search input");
			}
			return new AiSearchConditionExtractionResult(
					null, null, "Seoul Station", "wallet", "Seoul Station", ASSISTANT_MESSAGE);
		};
	}

	@Bean
	@Primary
	PortalFoundItemNameStorageClient e2ePortalFoundItemNameStorageClient() {
		PortalFoundItemNameStorageClient client = mock(PortalFoundItemNameStorageClient.class);
		when(client.search(any(PortalFoundItemSearchQuery.class))).thenAnswer(invocation -> {
			PortalFoundItemSearchQuery query = invocation.getArgument(0);
			if (!"wallet".equals(query.productNameKeyword())
					|| !"Seoul Station".equals(query.storagePlaceKeyword())) {
				throw new IllegalArgumentException("Unexpected E2E public-data query");
			}
			deterministicDelay();
			return List.of(rankTwoInputCandidate(), rankOneInputCandidate());
		});
		return client;
	}

	@Bean
	@Primary
	InitialSearchCandidateRanker e2eInitialSearchCandidateRanker() {
		InitialSearchCandidateRanker ranker = mock(InitialSearchCandidateRanker.class);
		when(ranker.rank(any(CandidateRankingRequest.class))).thenAnswer(invocation -> {
			CandidateRankingRequest request = invocation.getArgument(0);
			List<RankedCandidate> ranked = request.candidates().stream()
					.map(candidate -> switch (candidate.productName()) {
						case "Dummy Black Wallet A" ->
							new RankedCandidate(candidate.candidateKey(), 1, RANK_ONE_REASON, true);
						case "Dummy Black Wallet B" ->
							new RankedCandidate(candidate.candidateKey(), 2, RANK_TWO_REASON, true);
						default -> throw new IllegalArgumentException("Unexpected E2E candidate");
					})
					.toList();
			return CandidateRankingResult.success(ranked);
		});
		return ranker;
	}

	@Bean("policeFoundItemLookupClient")
	FoundItemLookupClient e2ePoliceFoundItemLookupClient() {
		return lookupClient(FoundItemSourceType.POLICE);
	}

	@Bean("portalFoundItemLookupClient")
	FoundItemLookupClient e2ePortalFoundItemLookupClient() {
		return lookupClient(FoundItemSourceType.PORTAL);
	}

	@Bean("policeFoundItemDetailClient")
	FoundItemDetailClient e2ePoliceFoundItemDetailClient() {
		return detailClient(FoundItemSourceType.POLICE);
	}

	@Bean("portalFoundItemDetailClient")
	FoundItemDetailClient e2ePortalFoundItemDetailClient() {
		return detailClient(FoundItemSourceType.PORTAL);
	}

	@Bean
	EmailSender e2eEmailSender() {
		return (recipientEmail, subject, body) -> {
			// E2E에서는 SMTP를 호출하지 않는다.
		};
	}

	private FoundItemLookupClient lookupClient(FoundItemSourceType sourceType) {
		FoundItemLookupClient client = mock(FoundItemLookupClient.class);
		when(client.sourceType()).thenReturn(sourceType);
		return client;
	}

	private FoundItemDetailClient detailClient(FoundItemSourceType sourceType) {
		FoundItemDetailClient client = mock(FoundItemDetailClient.class);
		when(client.sourceType()).thenReturn(sourceType);
		when(client.fetchDetail(any(), any())).thenReturn(Optional.empty());
		return client;
	}

	private FoundItemListEntry rankTwoInputCandidate() {
		return new FoundItemListEntry(
				FoundItemSourceType.PORTAL,
				"DUMMY-ATC-001",
				"0002",
				"Dummy Black Wallet B",
				"Dummy wallet fixture B",
				"Wallet",
				"Black",
				LocalDate.of(2026, 9, 22),
				"Dummy Seoul Station Storage",
				null);
	}

	private FoundItemListEntry rankOneInputCandidate() {
		return new FoundItemListEntry(
				FoundItemSourceType.PORTAL,
				"DUMMY-ATC-002",
				"0001",
				"Dummy Black Wallet A",
				"Dummy wallet fixture A",
				"Wallet",
				"Black",
				LocalDate.of(2026, 9, 21),
				"Dummy Seoul Station Information Desk",
				LOCAL_DATA_IMAGE);
	}

	private void deterministicDelay() {
		try {
			Thread.sleep(300);
		}
		catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException("E2E stub delay interrupted", e);
		}
	}
}
