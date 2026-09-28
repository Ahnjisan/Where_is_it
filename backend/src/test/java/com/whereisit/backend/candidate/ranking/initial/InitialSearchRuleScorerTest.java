package com.whereisit.backend.candidate.ranking.initial;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.whereisit.backend.candidate.ranking.CandidateRankingInput;
import com.whereisit.backend.candidate.ranking.CandidateRankingRequest;
import com.whereisit.backend.candidate.ranking.CandidateRankingResult;
import com.whereisit.backend.candidate.ranking.RankedCandidate;

class InitialSearchRuleScorerTest {

	private final InitialSearchRuleScorer scorer = new InitialSearchRuleScorer();

	@Test
	void normalizesWithNfkcLowercaseAndSeparatorsOnlyForComparison() {
		assertThat(CandidateTextNormalizer.normalize("  ＷＡＬＬＥＴ\t-  검은색!!지갑 ")).isEqualTo("wallet 검은색 지갑");
		assertThat(CandidateTextNormalizer.compact("검은색 · 지갑")).isEqualTo("검은색지갑");
		assertThat(CandidateTextNormalizer.compact("카드-지갑")).isEqualTo(CandidateTextNormalizer.compact("카드지갑"));
		assertThat(CandidateTextNormalizer.tokens("지갑, 지갑  카드")).containsExactly("지갑", "카드");
		assertThat(CandidateTextNormalizer.normalize(null)).isEmpty();
		assertThat(CandidateTextNormalizer.normalize(" \t ")).isEmpty();
		assertThat(CandidateTextNormalizer.tokens(null)).isEmpty();

		String original = "검은색 지갑";
		CandidateTextNormalizer.compact(original);
		assertThat(original).isEqualTo("검은색 지갑");
	}

	@Test
	void spacingAndPartialPlaceNamesAreComparable() {
		assertThat(scorer.score("검은색 지갑", item("c1", "검은색지갑", null))).isEqualTo(2);
		assertThat(scorer.score("검은색지갑", item("c1", "검은색 지갑", null))).isEqualTo(1);
		assertThat(scorer.score("카드 지갑", item("c1", "카드지갑", null))).isEqualTo(2);
		assertThat(scorer.score("카드지갑", item("c1", "카드 지갑", null))).isEqualTo(1);
		assertThat(scorer.score("서울", place("서울역"))).isEqualTo(1);
		assertThat(scorer.score("서울역", place("서울"))).isEqualTo(1);
		assertThat(scorer.score("검은색, 지갑!", item("c1", "검은색-지갑", null))).isEqualTo(2);
		assertThat(scorer.score("ＢＬＵＥ Wallet", item("c1", "blue wallet", null))).isEqualTo(2);
		assertThat(scorer.score("지갑", item("c1", "우산", null))).isZero();
	}

	@Test
	void singleCharacterTokensDoNotMatchPartially() {
		assertThat(scorer.score("갑", item("c1", "지갑", null))).isZero();
		assertThat(scorer.score("갑", item("c1", "갑", null))).isEqualTo(1);
	}

	@Test
	void nullAndBlankValuesAreSafe() {
		assertThat(scorer.score(null, item("c1", "지갑", null))).isZero();
		assertThat(scorer.score("  ", item("c1", "지갑", null))).isZero();
		assertThat(scorer.score("지갑", new CandidateRankingInput("c1", null, null, null, null, null, null))).isZero();
		CandidateRankingResult result = scorer.fallback(new CandidateRankingRequest(null, "ko", null, null, null,
				List.of(new CandidateRankingInput("c1", null, null, null, null, null, null))));
		assertThat(result.candidates()).singleElement().satisfies(candidate -> {
			assertThat(candidate.similar()).isFalse();
			assertThat(candidate.reason()).isNotBlank();
		});
	}

	@Test
	void fallbackRanksMoreOverlappingItemsHigherWithConsecutiveRanks() {
		CandidateRankingResult result = scorer.fallback(request("ko", List.of(
				item("c1", "우산", "우산 습득"), item("c2", "파란색 지갑", "지갑 습득"), item("c3", "파란색지갑", null))));
		List<RankedCandidate> ranked = result.candidates();

		assertThat(ranked).extracting(RankedCandidate::candidateKey).containsExactly("c2", "c3", "c1");
		assertThat(ranked).extracting(RankedCandidate::rank).containsExactly(1, 2, 3);
		assertThat(ranked.get(0).similar()).isTrue();
		assertThat(ranked.get(0).reason()).contains("파란색");
		assertThat(ranked.get(2).similar()).isFalse();
		assertThat(result.rankingStatus()).isEqualTo(CandidateRankingResult.RankingStatus.UNAVAILABLE);
		assertThat(result.openAiResultUsed()).isFalse();
		assertThat(result.warnings()).containsExactly("AI_RANKING_UNAVAILABLE");
	}

	@Test
	void fallbackKeepsRequestOrderForTiesAndWritesEnglishReason() {
		CandidateRankingResult result = scorer.fallback(request("en", List.of(
				item("c1", "umbrella", null), item("c2", "umbrella", null), item("c3", "wallet", "found wallet"))));

		assertThat(result.candidates()).extracting(RankedCandidate::candidateKey).containsExactly("c3", "c1", "c2");
		assertThat(result.candidates().get(0).reason()).contains("Matching description terms");
	}

	@Test
	void descriptionIsTruncatedLikeTheRankingRequest() {
		String longDescription = "지갑 " + "가".repeat(3000);
		CandidateRankingRequest request = request("ko", List.of(item("c1", "지갑", null)));

		assertThat(InitialSearchRuleScorer.truncate(longDescription, 2000))
				.isEqualTo(new CandidateRankingRequest(longDescription, "ko", null, null, null, List.of()).description());
		assertThat(scorer.score(longDescription, request.candidates().get(0))).isEqualTo(1);
	}

	private CandidateRankingRequest request(String language, List<CandidateRankingInput> candidates) {
		return new CandidateRankingRequest(
				language.equals("en") ? "blue wallet" : "파란색 지갑을 잃어버렸어요",
				language, null, null, null, candidates);
	}

	private CandidateRankingInput item(String key, String productName, String subject) {
		return new CandidateRankingInput(key, productName, subject, null, null, null, null);
	}

	private CandidateRankingInput place(String storagePlace) {
		return new CandidateRankingInput("c1", null, null, null, null, null, storagePlace);
	}
}
