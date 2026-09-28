package com.whereisit.backend.tracking;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.whereisit.backend.founditem.client.FoundItemListEntry;
import com.whereisit.backend.founditem.entity.FoundItemSourceType;

/**
 * 습득일 범위로 받은 포털기관 목록에서 추적 조건에 맞는 습득물을 고른다.
 * 물품명 검색어는 포털기관 목록 2번 API의 PRDT_NM처럼 물품명·분류명 부분 일치로 비교하고,
 * 대소문자와 공백 차이는 무시한다.
 */
@Component
public class TrackingCandidateMatcher {

	public List<FoundItemListEntry> match(List<FoundItemListEntry> entries, TrackingCondition condition) {
		String productKeyword = normalize(condition.productNameKeyword());
		if (productKeyword == null) {
			return List.of();
		}
		String storageKeyword = normalize(condition.storagePlaceKeyword());

		Map<NaturalKey, FoundItemListEntry> matched = new LinkedHashMap<>();
		for (FoundItemListEntry entry : entries) {
			if (foundOnOrAfter(entry.foundDate(), condition.searchStartDate())
					&& (contains(entry.productName(), productKeyword) || contains(entry.categoryName(), productKeyword))
					&& (storageKeyword == null || contains(entry.storagePlace(), storageKeyword))) {
				matched.putIfAbsent(new NaturalKey(entry), entry);
			}
		}
		return List.copyOf(matched.values());
	}

	private boolean foundOnOrAfter(LocalDate foundDate, LocalDate searchStartDate) {
		return foundDate != null && (searchStartDate == null || !foundDate.isBefore(searchStartDate));
	}

	private boolean contains(String text, String normalizedKeyword) {
		String normalizedText = normalize(text);
		return normalizedText != null && normalizedText.contains(normalizedKeyword);
	}

	private String normalize(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
		return normalized.isEmpty() ? null : normalized;
	}

	private record NaturalKey(FoundItemSourceType sourceType, String atcId, String fdSn) {
		NaturalKey(FoundItemListEntry entry) {
			this(entry.sourceType(), entry.atcId(), entry.fdSn());
		}
	}
}
