package com.whereisit.backend.candidate.ranking.initial;

import java.util.Set;

/**
 * 민감정보 없는 후보 랭킹 실패 진단. 메시지는 고정 category 이름뿐이며 외부 예외(cause)와 stack trace를 보관하지 않아
 * 응답 원문·요청 본문·API Key가 로그나 예외 메시지로 새지 않는다.
 */
final class RankingFailure extends RuntimeException {

	private static final long serialVersionUID = 1L;
	private static final Set<String> KNOWN_RESPONSE_STATUSES = Set.of(
			"completed", "incomplete", "failed", "cancelled", "queued", "in_progress");
	private static final Set<String> KNOWN_INCOMPLETE_REASONS = Set.of("max_output_tokens", "content_filter");

	private final RankingFailureCategory category;
	private final Integer httpStatus;
	private final Integer requestBytes;
	private final String responseStatus;
	private final String incompleteReason;

	private RankingFailure(RankingFailureCategory category, Integer httpStatus, Integer requestBytes,
			String responseStatus, String incompleteReason) {
		super(category.name(), null, false, false);
		this.category = category;
		this.httpStatus = httpStatus;
		this.requestBytes = requestBytes;
		this.responseStatus = responseStatus;
		this.incompleteReason = incompleteReason;
	}

	static RankingFailure of(RankingFailureCategory category) {
		return new RankingFailure(category, null, null, null, null);
	}

	static RankingFailure http(RankingFailureCategory category, int httpStatus) {
		return new RankingFailure(category, httpStatus, null, null, null);
	}

	/** 응답 status·incomplete reason은 알려진 고정값만 남기고 그 밖의 값은 "other"로 바꾼다. */
	static RankingFailure response(RankingFailureCategory category, String responseStatus, String incompleteReason) {
		return new RankingFailure(category, null, null, allowlisted(responseStatus, KNOWN_RESPONSE_STATUSES),
				allowlisted(incompleteReason, KNOWN_INCOMPLETE_REASONS));
	}

	RankingFailure withRequestBytes(int bytes) {
		return new RankingFailure(category, httpStatus, bytes, responseStatus, incompleteReason);
	}

	RankingFailureCategory category() {
		return category;
	}

	Integer httpStatus() {
		return httpStatus;
	}

	Integer requestBytes() {
		return requestBytes;
	}

	String responseStatus() {
		return responseStatus;
	}

	String incompleteReason() {
		return incompleteReason;
	}

	private static String allowlisted(String value, Set<String> known) {
		if (value == null) {
			return null;
		}
		return known.contains(value) ? value : "other";
	}
}
