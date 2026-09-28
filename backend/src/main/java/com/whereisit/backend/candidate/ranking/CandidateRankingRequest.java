package com.whereisit.backend.candidate.ranking;

import java.time.LocalDate;
import java.util.List;

public record CandidateRankingRequest(
		String description,
		String languageCode,
		LocalDate lostDateFrom,
		LocalDate lostDateTo,
		String lostPlaceText,
		List<CandidateRankingInput> candidates) {

	public CandidateRankingRequest {
		description = CandidateRankingInput.truncate(description, 2000);
		lostPlaceText = CandidateRankingInput.truncate(lostPlaceText, 255);
		candidates = candidates == null ? List.of() : List.copyOf(candidates);
	}
}
