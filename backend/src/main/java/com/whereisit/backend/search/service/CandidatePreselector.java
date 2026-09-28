package com.whereisit.backend.search.service;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.whereisit.backend.candidate.ranking.CandidateRankingInput;
import com.whereisit.backend.candidate.ranking.initial.InitialSearchRuleScorer;
import com.whereisit.backend.founditem.client.FoundItemListEntry;
import com.whereisit.backend.founditem.entity.FoundItemSourceType;

import lombok.RequiredArgsConstructor;

/**
 * API-05 전용 후보 사전 축약. 자연키 {@code (sourceType, atcId, fdSn)}로 중복을 제거하고
 * 규칙 점수 내림차순 → 습득일 최신순 → 출처 → atcId → fdSn의 전순서로 정렬해 상위 limit건을 고른다.
 * 자연키가 마지막 기준이므로 외부 API·DB·컬렉션의 입력 순서와 무관하게 같은 입력은 같은 결과를 만든다.
 * Legacy API-11·14의 후보 선택(최대 100건)에는 사용하지 않는다.
 */
@Component
@RequiredArgsConstructor
public class CandidatePreselector {

	/** API-05 OpenAI 후보 평가와 fallback이 함께 쓰는 최대 후보 수. */
	public static final int AI_EVALUATION_LIMIT = 20;

	private static final Comparator<Scored> ORDER = Comparator
			.comparingInt(Scored::score).reversed()
			.thenComparing(item -> item.entry().foundDate(), Comparator.nullsLast(Comparator.reverseOrder()))
			.thenComparing(item -> item.entry().sourceType(), Comparator.nullsLast(Comparator.<FoundItemSourceType>naturalOrder()))
			.thenComparing(item -> item.entry().atcId(), Comparator.nullsFirst(Comparator.<String>naturalOrder()))
			.thenComparing(item -> item.entry().fdSn(), Comparator.nullsFirst(Comparator.<String>naturalOrder()));

	private final InitialSearchRuleScorer ruleScorer;

	public Preselection select(String description, List<FoundItemListEntry> entries, int limit) {
		Map<NaturalKey, FoundItemListEntry> unique = new LinkedHashMap<>();
		for (FoundItemListEntry entry : entries) {
			unique.putIfAbsent(new NaturalKey(entry.sourceType(), entry.atcId(), entry.fdSn()), entry);
		}
		List<FoundItemListEntry> sorted = unique.values().stream()
				.map(entry -> new Scored(entry, ruleScorer.score(description, toRankingInput("c0", entry))))
				.sorted(ORDER)
				.map(Scored::entry)
				.toList();
		boolean limited = sorted.size() > limit;
		return new Preselection(limited ? sorted.subList(0, limit) : sorted, limited);
	}

	static CandidateRankingInput toRankingInput(String key, FoundItemListEntry entry) {
		return new CandidateRankingInput(key, entry.productName(), entry.subject(), entry.categoryName(),
				entry.colorName(), entry.foundDate(), entry.storagePlace());
	}

	public record Preselection(List<FoundItemListEntry> entries, boolean limited) {

		public Preselection {
			entries = List.copyOf(entries);
		}
	}

	private record NaturalKey(FoundItemSourceType sourceType, String atcId, String fdSn) {
	}

	private record Scored(FoundItemListEntry entry, int score) {
	}
}
