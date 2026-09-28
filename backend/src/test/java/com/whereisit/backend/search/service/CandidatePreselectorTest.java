package com.whereisit.backend.search.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.Duration;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.IntStream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whereisit.backend.candidate.ranking.CandidateRankingInput;
import com.whereisit.backend.candidate.ranking.CandidateRankingRequest;
import com.whereisit.backend.candidate.ranking.CandidateRankingResult;
import com.whereisit.backend.candidate.ranking.RankedCandidate;
import com.whereisit.backend.candidate.ranking.initial.InitialSearchCandidateRanker;
import com.whereisit.backend.candidate.ranking.initial.InitialSearchRuleScorer;
import com.whereisit.backend.founditem.client.FoundItemListEntry;
import com.whereisit.backend.founditem.entity.FoundItemSourceType;
import com.whereisit.backend.search.ai.openai.OpenAiProperties;
import com.whereisit.backend.search.service.CandidatePreselector.Preselection;

class CandidatePreselectorTest {

	private static final int LIMIT = CandidatePreselector.AI_EVALUATION_LIMIT;
	private final CandidatePreselector preselector = new CandidatePreselector(new InitialSearchRuleScorer());

	@ParameterizedTest
	@ValueSource(ints = {0, 1, 20, 21, 72, 100, 101, 150})
	void selectsAtMostTwentyAndFlagsOnlyWhenMoreThanTwenty(int count) {
		Preselection result = preselector.select("검은색 지갑", entries(count), LIMIT);

		assertThat(result.entries()).hasSize(Math.min(count, LIMIT));
		assertThat(result.limited()).isEqualTo(count > LIMIT);
		assertThat(result.entries()).extracting(FoundItemListEntry::atcId).doesNotHaveDuplicates();
	}

	@Test
	void aiEvaluationLimitIsTwenty() {
		assertThat(CandidatePreselector.AI_EVALUATION_LIMIT).isEqualTo(20);
	}

	@Test
	void sameInputInAnyOrderAlwaysSelectsSameTopTwentyInSameOrder() {
		List<FoundItemListEntry> entries = entries(72);
		List<FoundItemListEntry> expected = preselector.select("검은색 지갑", entries, LIMIT).entries();

		Random random = new Random(80);
		for (int i = 0; i < 10; i++) {
			List<FoundItemListEntry> shuffled = new ArrayList<>(entries);
			Collections.shuffle(shuffled, random);
			assertThat(preselector.select("검은색 지갑", shuffled, LIMIT).entries()).isEqualTo(expected);
		}
	}

	@Test
	void ordersByScoreThenNewestDateThenSourceAtcIdAndFdSn() {
		LocalDate day = LocalDate.of(2026, 9, 20);
		FoundItemListEntry matching = entry(FoundItemSourceType.PORTAL, "Z", "9", "검은색 지갑", LocalDate.of(2026, 1, 1));
		FoundItemListEntry newest = entry(FoundItemSourceType.PORTAL, "Y", "9", "우산", LocalDate.of(2026, 9, 21));
		FoundItemListEntry police = entry(FoundItemSourceType.POLICE, "B", "1", "우산", day);
		FoundItemListEntry portalA1 = entry(FoundItemSourceType.PORTAL, "A", "1", "우산", day);
		FoundItemListEntry portalA2 = entry(FoundItemSourceType.PORTAL, "A", "2", "우산", day);
		FoundItemListEntry portalB = entry(FoundItemSourceType.PORTAL, "B", "0", "우산", day);
		FoundItemListEntry undated = entry(FoundItemSourceType.POLICE, "A", "0", "우산", null);

		List<FoundItemListEntry> selected = preselector.select("검은색 지갑",
				List.of(undated, portalB, portalA2, police, portalA1, newest, matching), LIMIT).entries();

		assertThat(selected).containsExactly(matching, newest, police, portalA1, portalA2, portalB, undated);
	}

	@Test
	void naturalKeyDuplicatesAreRemovedAndFdSnKeepsLeadingZeros() {
		FoundItemListEntry first = entry(FoundItemSourceType.PORTAL, "A-1", "007", "지갑", null);
		FoundItemListEntry duplicate = entry(FoundItemSourceType.PORTAL, "A-1", "007", "지갑", null);
		FoundItemListEntry otherFdSn = entry(FoundItemSourceType.PORTAL, "A-1", "7", "지갑", null);
		FoundItemListEntry otherSource = entry(FoundItemSourceType.POLICE, "A-1", "007", "지갑", null);

		List<FoundItemListEntry> selected = preselector.select("지갑",
				List.of(first, duplicate, otherFdSn, otherSource), LIMIT).entries();

		assertThat(selected).hasSize(3);
		assertThat(selected).extracting(FoundItemListEntry::fdSn).containsExactly("007", "007", "7");
		assertThat(selected).extracting(FoundItemListEntry::sourceType)
				.containsExactly(FoundItemSourceType.POLICE, FoundItemSourceType.PORTAL, FoundItemSourceType.PORTAL);
	}

	@Test
	void normalizedMatchesArePreferredDuringPreselection() {
		List<FoundItemListEntry> entries = new ArrayList<>(entries(30).stream()
				.map(entry -> entry(entry.sourceType(), entry.atcId(), entry.fdSn(), "우산", entry.foundDate()))
				.toList());
		FoundItemListEntry compactName = entry(FoundItemSourceType.PORTAL, "ZZ-1", "1", "검은색지갑", null);
		FoundItemListEntry partialPlace = new FoundItemListEntry(FoundItemSourceType.PORTAL, "ZZ-2", "1", "물건",
				null, null, null, null, "서울역 유실물센터", null);
		entries.add(compactName);
		entries.add(partialPlace);

		List<FoundItemListEntry> selected = preselector.select("서울 검은색 지갑", entries, LIMIT).entries();

		assertThat(selected.get(0)).isEqualTo(compactName);
		assertThat(selected.get(1)).isEqualTo(partialPlace);
	}

	@Test
	void serverRankUsesPreselectionTieBreakersWhenAiScoresAreEqual() {
		FoundItemListEntry portalB = entry(FoundItemSourceType.PORTAL, "B", "1", "우산", LocalDate.of(2026, 9, 20));
		FoundItemListEntry portalA2 = entry(FoundItemSourceType.PORTAL, "A", "2", "우산", LocalDate.of(2026, 9, 20));
		FoundItemListEntry portalA1 = entry(FoundItemSourceType.PORTAL, "A", "1", "우산", LocalDate.of(2026, 9, 20));
		FoundItemListEntry police = entry(FoundItemSourceType.POLICE, "C", "1", "우산", LocalDate.of(2026, 9, 20));
		FoundItemListEntry matching = entry(FoundItemSourceType.PORTAL, "Z", "1", "지갑", LocalDate.of(2026, 9, 1));
		List<FoundItemListEntry> input = List.of(portalB, portalA2, portalA1, police, matching);

		List<FoundItemListEntry> first = rankWithEqualAiScores(input);
		List<FoundItemListEntry> reversedInput = new ArrayList<>(input);
		Collections.reverse(reversedInput);
		List<FoundItemListEntry> reversed = rankWithEqualAiScores(reversedInput);

		assertThat(first).containsExactly(matching, police, portalA1, portalA2, portalB);
		assertThat(reversed).isEqualTo(first);
	}

	@Test
	void aiFailureFallbackUsesSameTwentyCandidateSet() {
		Preselection preselection = preselector.select("검은색 지갑", entries(72), LIMIT);
		CandidateRankingRequest request = request("검은색 지갑", preselection.entries());
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.com/v1");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(once(), requestTo("https://api.openai.com/v1/responses"))
				.andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

		CandidateRankingResult result = ranker(builder).rank(request);

		assertThat(result.rankingStatus()).isEqualTo(CandidateRankingResult.RankingStatus.UNAVAILABLE);
		assertThat(result.candidates()).extracting(RankedCandidate::candidateKey)
				.containsExactlyInAnyOrderElementsOf(request.candidates().stream()
						.map(CandidateRankingInput::candidateKey).toList());
		assertThat(result.candidates()).hasSize(20);
		server.verify();
	}

	private List<FoundItemListEntry> rankWithEqualAiScores(List<FoundItemListEntry> entries) {
		List<FoundItemListEntry> selected = preselector.select("지갑", entries, LIMIT).entries();
		CandidateRankingRequest request = request("지갑", selected);
		RestClient.Builder builder = RestClient.builder().baseUrl("https://api.openai.com/v1");
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		StringBuilder output = new StringBuilder("{\"candidates\":{");
		for (int i = selected.size(); i >= 1; i--) {
			output.append("\"c").append(i).append("\":{\"score\":70,\"isSimilar\":true,\"reason\":\"한글 이유\"}");
			if (i > 1) {
				output.append(',');
			}
		}
		output.append("}}");
		String escaped = output.toString().replace("\"", "\\\"");
		server.expect(once(), requestTo("https://api.openai.com/v1/responses")).andRespond(withSuccess(
				"{\"status\":\"completed\",\"output\":[{\"type\":\"message\",\"content\":[{\"type\":\"output_text\","
						+ "\"text\":\"" + escaped + "\"}]}]}", MediaType.APPLICATION_JSON));

		CandidateRankingResult result = ranker(builder).rank(request);
		server.verify();

		assertThat(result.rankingStatus()).isEqualTo(CandidateRankingResult.RankingStatus.SUCCESS);
		assertThat(result.candidates()).extracting(RankedCandidate::rank)
				.containsExactlyElementsOf(IntStream.rangeClosed(1, selected.size()).boxed().toList());
		return result.candidates().stream()
				.map(ranked -> selected.get(Integer.parseInt(ranked.candidateKey().substring(1)) - 1))
				.toList();
	}

	private InitialSearchCandidateRanker ranker(RestClient.Builder builder) {
		return new InitialSearchCandidateRanker(new OpenAiProperties("test-only-key", "test-only-model", Duration.ofSeconds(10)),
				builder.build(), new ObjectMapper(), new InitialSearchRuleScorer());
	}

	private CandidateRankingRequest request(String description, List<FoundItemListEntry> selected) {
		List<CandidateRankingInput> inputs = new ArrayList<>();
		for (int i = 0; i < selected.size(); i++) {
			inputs.add(CandidatePreselector.toRankingInput("c" + (i + 1), selected.get(i)));
		}
		return new CandidateRankingRequest(description, "ko", null, null, null, inputs);
	}

	private List<FoundItemListEntry> entries(int count) {
		List<FoundItemListEntry> entries = new ArrayList<>();
		for (int i = 0; i < count; i++) {
			entries.add(entry(i % 2 == 0 ? FoundItemSourceType.PORTAL : FoundItemSourceType.POLICE,
					"ATC-%03d".formatted(i), "%02d".formatted(i % 7), i % 5 == 0 ? "검은색 지갑" : "물품 " + i,
					i % 4 == 0 ? null : LocalDate.of(2026, 9, 1).plusDays(i % 9)));
		}
		if (count > 0) {
			entries.add(entries.get(0));
		}
		return entries;
	}

	private FoundItemListEntry entry(FoundItemSourceType source, String atcId, String fdSn, String name,
			LocalDate foundDate) {
		return new FoundItemListEntry(source, atcId, fdSn, name, null, null, null, foundDate, null, null);
	}
}
