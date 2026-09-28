package com.whereisit.backend.candidate.ranking;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

/** OpenAI 실패 fallback과 후보 사전 축약에 사용하는 규칙 기반 랭커. */
@Component
public class SimpleTextSimilarityRanker implements CandidateRanker {

	private static final Pattern TOKEN_DELIMITER = Pattern.compile("[^\\p{IsAlphabetic}\\p{Digit}]+");

	@Override
	public CandidateRankingResult rank(CandidateRankingRequest request) {
		Set<String> descriptionTokens = tokenize(request.description());
		List<Scored> scored = new ArrayList<>();
		for (int i = 0; i < request.candidates().size(); i++) {
			scored.add(score(descriptionTokens, request.candidates().get(i), i));
		}
		scored.sort(Comparator.comparingInt(Scored::score).reversed()
				.thenComparingInt(Scored::originalIndex));

		List<RankedCandidate> ranked = new ArrayList<>(scored.size());
		for (int i = 0; i < scored.size(); i++) {
			ranked.add(toRankedCandidate(scored.get(i), i + 1, request.languageCode()));
		}
		return CandidateRankingResult.unavailable(ranked);
	}

	/** 사전 축약 정렬의 첫 번째 기준인 규칙 점수. */
	public int similarityScore(String description, CandidateRankingInput item) {
		return score(tokenize(description), item, 0).score();
	}

	private Scored score(Set<String> descriptionTokens, CandidateRankingInput item, int originalIndex) {
		Set<String> itemTokens = tokenize(String.join(" ",
				nullToEmpty(item.productName()), nullToEmpty(item.subject()),
				nullToEmpty(item.categoryName()), nullToEmpty(item.colorName()),
				nullToEmpty(item.storagePlace())));
		Set<String> matched = new LinkedHashSet<>(descriptionTokens);
		matched.retainAll(itemTokens);
		return new Scored(item, matched, originalIndex);
	}

	private RankedCandidate toRankedCandidate(Scored scored, int rank, String languageCode) {
		boolean similar = !scored.matched().isEmpty();
		String reason;
		if ("en".equals(languageCode)) {
			reason = similar
					? "Matching description terms: " + String.join(", ", scored.matched())
					: "No matching description terms were found.";
		}
		else {
			reason = similar
					? "설명과 겹치는 단어: " + String.join(", ", scored.matched())
					: "설명과 겹치는 단어를 찾지 못했습니다.";
		}
		return new RankedCandidate(scored.item().candidateKey(), rank,
				CandidateRankingInput.truncate(reason, 500), similar);
	}

	private Set<String> tokenize(String text) {
		if (text == null || text.isBlank()) {
			return Set.of();
		}
		Set<String> tokens = new LinkedHashSet<>();
		for (String token : TOKEN_DELIMITER.split(text.toLowerCase())) {
			if (!token.isBlank()) {
				tokens.add(token);
			}
		}
		return tokens;
	}

	private String nullToEmpty(String value) {
		return value == null ? "" : value;
	}

	private record Scored(CandidateRankingInput item, Set<String> matched, int originalIndex) {
		int score() {
			return matched.size();
		}
	}
}
