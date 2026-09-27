package com.whereisit.backend.lostitem.error;

import org.springframework.http.HttpStatus;

import com.whereisit.backend.global.error.ErrorCode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum LostItemErrorCode implements ErrorCode {

	INVALID_SEARCH_CONDITION(HttpStatus.BAD_REQUEST, "Search condition is invalid."),
	TRACKING_EXPIRED(HttpStatus.CONFLICT, "This lost item search has already expired."),
	TRACKING_NOT_STARTED(HttpStatus.CONFLICT, "Tracking has not been activated for this lost item.");

	private final HttpStatus status;
	private final String message;

	@Override
	public String getCode() {
		return name();
	}
}
