package com.whereisit.backend.search.error;

import org.springframework.http.HttpStatus;

import com.whereisit.backend.global.error.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SearchErrorCode implements ErrorCode {

	AI_CONDITION_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "AI search condition extraction is unavailable."),
	SEARCH_TIMEOUT(HttpStatus.GATEWAY_TIMEOUT, "AI search condition extraction timed out."),
	RATE_LIMITED(HttpStatus.TOO_MANY_REQUESTS, "AI service rate limit was exceeded."),
	LOST_API_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "Both found-item sources are currently unavailable."),
	TRACKING_BASELINE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Could not establish a baseline candidate set for tracking.");

	private final HttpStatus status;
	private final String message;

	@Override
	public String getCode() {
		return name();
	}
}
