package com.whereisit.backend.search.error;

import org.springframework.http.HttpStatus;

import com.whereisit.backend.global.error.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SearchErrorCode implements ErrorCode {

	LOST_API_UNAVAILABLE(HttpStatus.BAD_GATEWAY, "Both found-item sources are currently unavailable."),
	TRACKING_BASELINE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "Could not establish a baseline candidate set for tracking.");

	private final HttpStatus status;
	private final String message;

	@Override
	public String getCode() {
		return name();
	}
}
