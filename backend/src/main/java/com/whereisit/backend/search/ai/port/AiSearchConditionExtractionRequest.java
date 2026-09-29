package com.whereisit.backend.search.ai.port;

import java.time.LocalDate;
import java.util.Objects;

public record AiSearchConditionExtractionRequest(
		String userNaturalLanguage,
		String currentDescription,
		String languageCode,
		LocalDate referenceDate,
		LocalDate existingLostDateFrom,
		LocalDate existingLostDateTo,
		String existingLostPlaceText,
		AiSearchConditionExtractionMode mode) {

	public AiSearchConditionExtractionRequest {
		Objects.requireNonNull(mode, "mode");
	}

	/** 모드를 지정하지 않으면 API-11과 같은 기존 6개 필드만 요청한다. */
	public AiSearchConditionExtractionRequest(
			String userNaturalLanguage, String currentDescription, String languageCode, LocalDate referenceDate,
			LocalDate existingLostDateFrom, LocalDate existingLostDateTo, String existingLostPlaceText) {
		this(userNaturalLanguage, currentDescription, languageCode, referenceDate, existingLostDateFrom,
				existingLostDateTo, existingLostPlaceText, AiSearchConditionExtractionMode.SEARCH_CONDITIONS);
	}
}
