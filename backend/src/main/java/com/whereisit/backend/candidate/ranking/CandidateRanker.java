package com.whereisit.backend.candidate.ranking;

/**
 * 분실물 설명과 실제 외부 API 후보를 비교해 우선순위·추천 이유를 만드는 Port.
 * 구현체는 Entity나 Database 식별자를 입력으로 받지 않는다.
 */
public interface CandidateRanker {

	CandidateRankingResult rank(CandidateRankingRequest request);
}
