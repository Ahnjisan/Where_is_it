package com.whereisit.backend.candidate.ranking;

import java.time.LocalDate;

/** OpenAI 후보 랭킹에 전달할 수 있는 필드만 담은 순수 DTO. */
public record CandidateRankingInput(
		String candidateKey,
		String productName,
		String subject,
		String categoryName,
		String colorName,
		LocalDate foundDate,
		String storagePlace) {

	public CandidateRankingInput {
		productName = truncate(productName, 200);
		subject = truncate(subject, 300);
		categoryName = truncate(categoryName, 100);
		colorName = truncate(colorName, 100);
		storagePlace = truncate(storagePlace, 255);
	}

	static String truncate(String value, int maxCodePoints) {
		if (value == null || value.codePointCount(0, value.length()) <= maxCodePoints) {
			return value;
		}
		int end = value.offsetByCodePoints(0, maxCodePoints - 1);
		return value.substring(0, end) + "…";
	}
}
