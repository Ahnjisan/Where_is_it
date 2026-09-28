package com.whereisit.backend.candidate.ranking.initial;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.springframework.stereotype.Component;

import com.whereisit.backend.candidate.ranking.CandidateRankingInput;
import com.whereisit.backend.candidate.ranking.CandidateRankingRequest;
import com.whereisit.backend.candidate.ranking.CandidateRankingResult;
import com.whereisit.backend.candidate.ranking.RankedCandidate;

/**
 * API-05 전용 규칙 점수와 규칙 기반 fallback. Legacy API-11이 쓰는 SimpleTextSimilarityRanker와 분리해
 * 정규화 비교가 다른 흐름의 순위·추천 이유에 영향을 주지 않게 한다.
 */
@Component
public class InitialSearchRuleScorer {

	/** {@link CandidateRankingRequest}의 설명 길이 제한과 같다. 사전 축약과 랭커가 같은 점수를 계산하게 한다. */
	static final int MAX_DESCRIPTION_CODE_POINTS = 2000;
	private static final int MAX_REASON_CODE_POINTS = 500;
	/** 부분 일치(포함 관계)에 사용할 token의 최소 code point 길이. 한 글자 token의 과잉 일치를 막는다. */
	private static final int MIN_PARTIAL_MATCH_LENGTH = 2;

	/** 사전 축약과 서버 최종 순위의 동점 기준으로 쓰는 규칙 점수. */
	public int score(String description, CandidateRankingInput item) {
		return matchedTokens(CandidateTextNormalizer.tokens(truncate(description, MAX_DESCRIPTION_CODE_POINTS)), item)
				.size();
	}

	/** 같은 후보 집합의 규칙 점수 순 결과. 동점은 요청 순서(사전 축약 순서)를 유지한다. */
	public CandidateRankingResult fallback(CandidateRankingRequest request) {
		Set<String> descriptionTokens = CandidateTextNormalizer.tokens(request.description());
		List<Scored> scored = new ArrayList<>();
		for (int i = 0; i < request.candidates().size(); i++) {
			CandidateRankingInput item = request.candidates().get(i);
			scored.add(new Scored(item, matchedTokens(descriptionTokens, item), i));
		}
		scored.sort(Comparator.comparingInt((Scored item) -> item.matched().size()).reversed()
				.thenComparingInt(Scored::originalIndex));

		List<RankedCandidate> ranked = new ArrayList<>(scored.size());
		for (int i = 0; i < scored.size(); i++) {
			Scored item = scored.get(i);
			ranked.add(new RankedCandidate(item.item().candidateKey(), i + 1,
					reason(item.matched(), request.languageCode()), !item.matched().isEmpty()));
		}
		return CandidateRankingResult.unavailable(ranked);
	}

	private Set<String> matchedTokens(Set<String> descriptionTokens, CandidateRankingInput item) {
		Set<String> itemTokens = new LinkedHashSet<>();
		List<String> compactFields = new ArrayList<>();
		Stream.of(item.productName(), item.subject(), item.categoryName(), item.colorName(), item.storagePlace())
				.forEach(field -> {
					itemTokens.addAll(CandidateTextNormalizer.tokens(field));
					String compact = CandidateTextNormalizer.compact(field);
					if (!compact.isEmpty()) {
						compactFields.add(compact);
					}
				});
		Set<String> matched = new LinkedHashSet<>();
		for (String token : descriptionTokens) {
			if (matches(token, itemTokens, compactFields)) {
				matched.add(token);
			}
		}
		return matched;
	}

	/**
	 * 정확히 같은 token, 붙여 쓴 후보 필드에 포함된 token("검은색" ⊂ "검은색지갑", "서울" ⊂ "서울역"),
	 * 또는 후보 token을 포함하는 설명 token("서울역" ⊃ "서울", "지갑을" ⊃ "지갑")을 일치로 본다.
	 */
	private boolean matches(String token, Set<String> itemTokens, List<String> compactFields) {
		if (itemTokens.contains(token)) {
			return true;
		}
		if (CandidateTextNormalizer.length(token) >= MIN_PARTIAL_MATCH_LENGTH
				&& compactFields.stream().anyMatch(field -> field.contains(token))) {
			return true;
		}
		return itemTokens.stream().anyMatch(itemToken ->
				CandidateTextNormalizer.length(itemToken) >= MIN_PARTIAL_MATCH_LENGTH && token.contains(itemToken));
	}

	private String reason(Set<String> matched, String languageCode) {
		String reason;
		if ("en".equals(languageCode)) {
			reason = matched.isEmpty()
					? "No matching description terms were found."
					: "Matching description terms: " + String.join(", ", matched);
		}
		else {
			reason = matched.isEmpty()
					? "설명과 겹치는 단어를 찾지 못했습니다."
					: "설명과 겹치는 단어: " + String.join(", ", matched);
		}
		return truncate(reason, MAX_REASON_CODE_POINTS);
	}

	/** {@code CandidateRankingInput.truncate}와 같은 규칙(초과 시 마지막 code point를 "…"로 대체). */
	static String truncate(String value, int maxCodePoints) {
		if (value == null || value.codePointCount(0, value.length()) <= maxCodePoints) {
			return value;
		}
		int end = value.offsetByCodePoints(0, maxCodePoints - 1);
		return value.substring(0, end) + "…";
	}

	private record Scored(CandidateRankingInput item, Set<String> matched, int originalIndex) {
	}
}
