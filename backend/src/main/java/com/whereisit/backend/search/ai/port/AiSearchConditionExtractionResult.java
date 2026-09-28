package com.whereisit.backend.search.ai.port;

import java.time.LocalDate;

public record AiSearchConditionExtractionResult(
		LocalDate lostDateFrom,
		LocalDate lostDateTo,
		String lostPlaceText,
		String assistantMessage) {
}
