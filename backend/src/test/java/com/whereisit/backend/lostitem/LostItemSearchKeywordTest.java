package com.whereisit.backend.lostitem;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.whereisit.backend.lostitem.entity.LostItem;
import com.whereisit.backend.member.entity.LanguageCode;
import com.whereisit.backend.member.entity.Member;

@DisplayName("Issue #85 분실물 추적 검색어·조회 시작일")
class LostItemSearchKeywordTest {

	private LostItem newLostItem() {
		return LostItem.create(Member.create("user@example.test", "hash", LanguageCode.KO), "검은 지갑", LanguageCode.KO);
	}

	@Test
	@DisplayName("AI가 검색어를 돌려주면 저장하고, 이후 null이면 기존 검색어를 유지한다")
	void keepsKeywordsWhenAiReturnsNull() {
		LostItem lostItem = newLostItem();

		lostItem.updateSearchKeywords("지갑", "서울역 유실물센터");
		lostItem.updateSearchKeywords(null, null);

		assertThat(lostItem.getProductNameKeyword()).isEqualTo("지갑");
		assertThat(lostItem.getStoragePlaceKeyword()).isEqualTo("서울역 유실물센터");

		lostItem.updateSearchKeywords("카드지갑", null);
		assertThat(lostItem.getProductNameKeyword()).isEqualTo("카드지갑");
		assertThat(lostItem.getStoragePlaceKeyword()).isEqualTo("서울역 유실물센터");
	}

	@Test
	@DisplayName("습득물_조회_시작일은 비어 있을 때만 채운다")
	void fillsSearchStartDateOnlyWhenAbsent() {
		LostItem lostItem = newLostItem();

		lostItem.fillSearchStartDateIfAbsent(LocalDate.of(2026, 9, 20));
		lostItem.fillSearchStartDateIfAbsent(LocalDate.of(2026, 9, 1));

		assertThat(lostItem.getSearchStartDate()).isEqualTo(LocalDate.of(2026, 9, 20));
	}
}
