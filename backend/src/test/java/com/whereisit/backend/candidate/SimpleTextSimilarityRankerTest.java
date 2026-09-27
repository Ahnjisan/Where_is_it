package com.whereisit.backend.candidate;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.whereisit.backend.candidate.ranking.RankedCandidate;
import com.whereisit.backend.candidate.ranking.SimpleTextSimilarityRanker;
import com.whereisit.backend.founditem.entity.FoundItem;
import com.whereisit.backend.founditem.entity.FoundItemSourceType;

class SimpleTextSimilarityRankerTest {

	private final SimpleTextSimilarityRanker ranker = new SimpleTextSimilarityRanker();

	@Test
	void ranksMoreOverlappingItemsHigherAndMarksThemSimilar() {
		FoundItem walletMatch = item("1", "파란색 지갑", "지갑 습득");
		FoundItem umbrellaNoMatch = item("2", "우산", "우산 습득");

		List<RankedCandidate> ranked = ranker.rank("파란색 지갑을 잃어버렸어요", List.of(umbrellaNoMatch, walletMatch));

		assertThat(ranked).hasSize(2);
		assertThat(ranked.get(0).foundItem()).isEqualTo(walletMatch);
		assertThat(ranked.get(0).rank()).isEqualTo(1);
		assertThat(ranked.get(0).similar()).isTrue();
		assertThat(ranked.get(0).reason()).contains("파란색");

		assertThat(ranked.get(1).foundItem()).isEqualTo(umbrellaNoMatch);
		assertThat(ranked.get(1).rank()).isEqualTo(2);
		assertThat(ranked.get(1).similar()).isFalse();
	}

	@Test
	void neverInventsItemsBeyondWhatWasPassedIn() {
		FoundItem only = item("1", "지갑", "지갑");

		List<RankedCandidate> ranked = ranker.rank("지갑", List.of(only));

		assertThat(ranked).hasSize(1);
		assertThat(ranked.get(0).foundItem()).isSameAs(only);
	}

	private FoundItem item(String atcId, String productName, String subject) {
		FoundItem item = FoundItem.create(FoundItemSourceType.POLICE, atcId, "1", LocalDateTime.now());
		item.refreshListFields(productName, subject, null, null, null, null, null, LocalDateTime.now());
		return item;
	}
}
