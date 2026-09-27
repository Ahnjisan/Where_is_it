package com.whereisit.backend.candidate.ranking;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import com.whereisit.backend.founditem.entity.FoundItem;

/**
 * AI 없이 키워드 겹침만으로 순위를 매기는 임시 구현. 분실물 설명과 습득물의 물품명·게시제목·분류명·색상명을
 * 토큰으로 나눠 겹치는 단어 수를 점수로 쓴다. 겹치는 단어가 하나도 없으면 유사 후보로 보지 않는다(isSimilar=false).
 */
@Component
public class SimpleTextSimilarityRanker implements CandidateRanker {

	private static final Pattern TOKEN_DELIMITER = Pattern.compile("[^\\p{IsAlphabetic}\\p{Digit}]+");

	@Override
	public List<RankedCandidate> rank(String description, List<FoundItem> foundItems) {
		Set<String> descriptionTokens = tokenize(description);

		List<Scored> scored = foundItems.stream()
				.map(item -> score(descriptionTokens, item))
				.sorted(Comparator.comparingInt(Scored::score).reversed()
						.thenComparing(s -> s.item().getId()))
				.toList();

		List<RankedCandidate> ranked = new ArrayList<>(scored.size());
		for (int i = 0; i < scored.size(); i++) {
			ranked.add(toRankedCandidate(scored.get(i), i + 1));
		}
		return ranked;
	}

	private Scored score(Set<String> descriptionTokens, FoundItem item) {
		Set<String> itemTokens = tokenize(String.join(" ",
				nullToEmpty(item.getProductName()), nullToEmpty(item.getSubject()),
				nullToEmpty(item.getCategoryName()), nullToEmpty(item.getColorName())));

		Set<String> matched = new LinkedHashSet<>(descriptionTokens);
		matched.retainAll(itemTokens);
		return new Scored(item, matched);
	}

	private RankedCandidate toRankedCandidate(Scored scored, int rank) {
		boolean similar = !scored.matched().isEmpty();
		String reason = similar
				? "설명과 겹치는 단어: " + String.join(", ", scored.matched())
				: "설명과 겹치는 단어를 찾지 못했습니다.";
		return new RankedCandidate(scored.item(), rank, reason, similar);
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

	private record Scored(FoundItem item, Set<String> matched) {
		int score() {
			return matched.size();
		}
	}
}
