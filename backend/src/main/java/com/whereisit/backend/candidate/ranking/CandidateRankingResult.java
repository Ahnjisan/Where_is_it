package com.whereisit.backend.candidate.ranking;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

public record CandidateRankingResult(
		List<RankedCandidate> candidates,
		RankingStatus rankingStatus,
		List<String> warnings,
		boolean openAiResultUsed) {

	public CandidateRankingResult {
		candidates = List.copyOf(candidates);
		warnings = List.copyOf(new LinkedHashSet<>(warnings));
	}

	public static CandidateRankingResult success(List<RankedCandidate> candidates) {
		return new CandidateRankingResult(candidates, RankingStatus.SUCCESS, List.of(), true);
	}

	public static CandidateRankingResult unavailable(List<RankedCandidate> candidates) {
		return new CandidateRankingResult(candidates, RankingStatus.UNAVAILABLE,
				List.of("AI_RANKING_UNAVAILABLE"), false);
	}

	public static CandidateRankingResult notRun() {
		return new CandidateRankingResult(List.of(), RankingStatus.NOT_RUN, List.of(), false);
	}

	public CandidateRankingResult withWarning(String warning) {
		List<String> merged = new ArrayList<>(warnings);
		merged.add(warning);
		return new CandidateRankingResult(candidates, rankingStatus, merged, openAiResultUsed);
	}

	public enum RankingStatus {
		SUCCESS,
		UNAVAILABLE,
		NOT_RUN
	}
}
