package com.whereisit.backend.tracking;

import java.util.List;
import java.util.stream.Collectors;

import com.whereisit.backend.candidate.entity.LostItemCandidate;
import com.whereisit.backend.founditem.entity.FoundItem;
import com.whereisit.backend.member.entity.LanguageCode;

/** 새 후보 알림 메일의 제목·본문. 분실물의 사용 언어(ko·en)로 쓴다. 본문에는 습득물 표시 정보만 넣는다. */
public record TrackingEmailContent(String subject, String body) {

	static final int MAX_LISTED_CANDIDATES = 20;

	public static TrackingEmailContent of(LanguageCode languageCode, List<LostItemCandidate> newCandidates) {
		boolean english = languageCode == LanguageCode.EN;
		int count = newCandidates.size();
		String subject = english
				? "[Where is it] %d new found item(s) may match your lost item".formatted(count)
				: "[어디갔지] 분실물과 비슷한 습득물 %d건이 새로 확인되었습니다".formatted(count);

		String lines = newCandidates.stream()
				.limit(MAX_LISTED_CANDIDATES)
				.map(candidate -> line(candidate.getFoundItem(), english))
				.collect(Collectors.joining("\n"));
		String intro = english
				? "New found items matching your tracking conditions were registered."
				: "추적 중인 조건과 맞는 습득물이 새로 등록되었습니다.";
		String more = count > MAX_LISTED_CANDIDATES
				? "\n" + (english ? "...and %d more." : "외 %d건").formatted(count - MAX_LISTED_CANDIDATES)
				: "";
		String outro = english
				? "Check the details in the Where is it service."
				: "자세한 내용은 어디갔지 서비스에서 확인해 주세요.";
		return new TrackingEmailContent(subject, intro + "\n\n" + lines + more + "\n\n" + outro);
	}

	private static String line(FoundItem foundItem, boolean english) {
		String name = valueOr(foundItem.getProductName(), english ? "(unnamed)" : "(품명 없음)");
		String date = foundItem.getFoundDate() == null ? "-" : foundItem.getFoundDate().toString();
		String place = valueOr(foundItem.getStoragePlace(), "-");
		return english
				? "- %s / found %s / kept at %s".formatted(name, date, place)
				: "- %s / 습득일 %s / 보관 %s".formatted(name, date, place);
	}

	private static String valueOr(String value, String fallback) {
		return value == null || value.isBlank() ? fallback : value;
	}
}
