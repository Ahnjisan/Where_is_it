package com.whereisit.backend.tracking;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.whereisit.backend.founditem.client.FoundItemListEntry;
import com.whereisit.backend.founditem.entity.FoundItemSourceType;

@DisplayName("Issue #85 추적 후보 매칭")
class TrackingCandidateMatcherTest {

	private static final LocalDate START = LocalDate.of(2026, 9, 20);

	private final TrackingCandidateMatcher matcher = new TrackingCandidateMatcher();

	@Test
	@DisplayName("물품명 검색어는 물품명 또는 분류명에 포함되면 맞는다")
	void matchesProductNameOrCategoryName() {
		List<FoundItemListEntry> entries = List.of(
				entry("A", "검정 반지갑", "지갑 > 남성용 지갑", "서울역 유실물센터", START),
				entry("B", "가죽 케이스", "지갑 > 기타 지갑", "서울역 유실물센터", START),
				entry("C", "우산", "우산", "서울역 유실물센터", START));

		assertThat(matcher.match(entries, condition("지갑", null)))
				.extracting(FoundItemListEntry::atcId)
				.containsExactly("A", "B");
	}

	@Test
	@DisplayName("대소문자와 공백 차이는 무시한다")
	void ignoresCaseAndWhitespace() {
		List<FoundItemListEntry> entries = List.of(
				entry("A", "AirPods Pro", "전자기기 > 이어폰", "강남 경찰서", START),
				entry("B", "에어 팟", "전자기기 > 이어폰", "강남 경찰서", START));

		assertThat(matcher.match(entries, condition("airpods", null))).extracting(FoundItemListEntry::atcId)
				.containsExactly("A");
		assertThat(matcher.match(entries, condition("에어팟", "강남경찰서"))).extracting(FoundItemListEntry::atcId)
				.containsExactly("B");
	}

	@Test
	@DisplayName("보관장소 검색어가 있으면 보관장소에도 포함돼야 한다")
	void storagePlaceKeywordNarrowsResults() {
		List<FoundItemListEntry> entries = List.of(
				entry("A", "지갑", "지갑", "서울역 유실물센터", START),
				entry("B", "지갑", "지갑", "부산역 유실물센터", START),
				entry("C", "지갑", "지갑", null, START));

		assertThat(matcher.match(entries, condition("지갑", "서울역"))).extracting(FoundItemListEntry::atcId)
				.containsExactly("A");
	}

	@Test
	@DisplayName("습득일이 조회 시작일보다 이르거나 없으면 제외하고, 시작일 당일은 포함한다")
	void excludesItemsFoundBeforeSearchStartDate() {
		List<FoundItemListEntry> entries = List.of(
				entry("BEFORE", "지갑", "지갑", "보관소", START.minusDays(1)),
				entry("SAME_DAY", "지갑", "지갑", "보관소", START),
				entry("AFTER", "지갑", "지갑", "보관소", START.plusDays(1)),
				entry("NO_DATE", "지갑", "지갑", "보관소", null));

		assertThat(matcher.match(entries, condition("지갑", null))).extracting(FoundItemListEntry::atcId)
				.containsExactly("SAME_DAY", "AFTER");
	}

	@Test
	@DisplayName("물품명 검색어가 없거나 공백뿐이면 아무것도 매칭하지 않는다")
	void blankProductKeywordMatchesNothing() {
		List<FoundItemListEntry> entries = List.of(entry("A", "지갑", "지갑", "보관소", START));

		assertThat(matcher.match(entries, condition(null, null))).isEmpty();
		assertThat(matcher.match(entries, condition("  ", null))).isEmpty();
	}

	@Test
	@DisplayName("같은 (출처, 관리번호, 습득순번)은 한 번만 돌려준다")
	void removesNaturalKeyDuplicates() {
		List<FoundItemListEntry> entries = List.of(
				entry("A", "지갑", "지갑", "보관소", START),
				entry("A", "지갑(중복)", "지갑", "보관소", START));

		assertThat(matcher.match(entries, condition("지갑", null))).singleElement()
				.extracting(FoundItemListEntry::productName).isEqualTo("지갑");
	}

	private TrackingCondition condition(String productNameKeyword, String storagePlaceKeyword) {
		return new TrackingCondition(productNameKeyword, storagePlaceKeyword, START);
	}

	private FoundItemListEntry entry(String atcId, String productName, String categoryName, String storagePlace,
			LocalDate foundDate) {
		return new FoundItemListEntry(FoundItemSourceType.PORTAL, atcId, "1", productName, productName,
				categoryName, "블랙(검정)", foundDate, storagePlace, null);
	}
}
