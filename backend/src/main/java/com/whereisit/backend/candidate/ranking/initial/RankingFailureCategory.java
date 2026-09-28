package com.whereisit.backend.candidate.ranking.initial;

/**
 * API-05 OpenAI 후보 평가 실패 단계. 내부 로그 진단 전용이며 공개 API에는 노출하지 않는다.
 * 모든 category(TIMEOUT·RATE_LIMITED 포함)는 API-05 전체 실패로 전파하지 않고 규칙 기반 fallback으로 처리한다.
 */
enum RankingFailureCategory {
	CONFIG_MISSING,
	REQUEST_INVALID,
	REQUEST_TOO_LARGE,
	HTTP_ERROR,
	CONNECTION_ERROR,
	TIMEOUT,
	RATE_LIMITED,
	REFUSAL,
	INCOMPLETE,
	OUTPUT_LIMIT,
	ENVELOPE_INVALID,
	JSON_INVALID,
	SCHEMA_INVALID,
	CANDIDATE_SET_INVALID,
	SCORE_INVALID,
	REASON_INVALID
}
