package com.whereisit.backend.search.ai.port;

import java.time.LocalDate;

public record AiSearchConditionExtractionRequest(
		String userNaturalLanguage,
		String currentDescription,
		String languageCode,
		LocalDate referenceDate,
		LocalDate existingLostDateFrom,
		LocalDate existingLostDateTo,
		String existingLostPlaceText) {
}
