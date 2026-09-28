package com.whereisit.backend.candidate;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.whereisit.backend.candidate.ranking.CandidateRankingInput;
import com.whereisit.backend.candidate.ranking.CandidateRankingRequest;
import com.whereisit.backend.candidate.ranking.CandidateRankingResult;
import com.whereisit.backend.candidate.ranking.RankedCandidate;
import com.whereisit.backend.candidate.ranking.SimpleTextSimilarityRanker;

class SimpleTextSimilarityRankerTest {

	private final SimpleTextSimilarityRanker ranker = new SimpleTextSimilarityRanker();

	@Test
	void ranksMoreOverlappingItemsHigherAndMarksThemSimilar() {
		CandidateRankingResult result = ranker.rank(request("ko", List.of(
				item("c1", "우산", "우산 습득"), item("c2", "파란색 지갑", "지갑 습득"))));
		List<RankedCandidate> ranked = result.candidates();

		assertThat(ranked).hasSize(2);
		assertThat(ranked.get(0).candidateKey()).isEqualTo("c2");
		assertThat(ranked.get(0).rank()).isEqualTo(1);
		assertThat(ranked.get(0).similar()).isTrue();
		assertThat(ranked.get(0).reason()).contains("파란색");
		assertThat(ranked.get(1).candidateKey()).isEqualTo("c1");
		assertThat(ranked.get(1).similar()).isFalse();
		assertThat(result.rankingStatus()).isEqualTo(CandidateRankingResult.RankingStatus.UNAVAILABLE);
	}

	@Test
	void producesEnglishFallbackReasonAndNeverInventsCandidates() {
		CandidateRankingResult result = ranker.rank(request("en", List.of(item("c1", "wallet", "found wallet"))));

		assertThat(result.candidates()).singleElement()
				.satisfies(candidate -> {
					assertThat(candidate.candidateKey()).isEqualTo("c1");
					assertThat(candidate.reason()).contains("Matching description terms");
				});
		assertThat(result.warnings()).containsExactly("AI_RANKING_UNAVAILABLE");
	}

	@Test
	void truncatesWithoutSplittingUnicodeCodePoints() {
		String longValue = "😀".repeat(201);
		CandidateRankingInput input = new CandidateRankingInput("c1", longValue, null, null, null, null, null);

		assertThat(input.productName().codePointCount(0, input.productName().length())).isEqualTo(200);
		assertThat(input.productName()).endsWith("…");
	}

	private CandidateRankingRequest request(String language, List<CandidateRankingInput> candidates) {
		return new CandidateRankingRequest(
				language.equals("en") ? "blue wallet" : "파란색 지갑을 잃어버렸어요",
				language, null, null, null, candidates);
	}

	private CandidateRankingInput item(String key, String productName, String subject) {
		return new CandidateRankingInput(key, productName, subject, null, null, null, null);
	}
}
