package com.whereisit.backend.search.ai.port;

import java.time.LocalDate;

public record AiSearchConditionExtractionResult(
		LocalDate lostDateFrom,
		LocalDate lostDateTo,
		String lostPlaceText,
		String productNameKeyword,
		String storagePlaceKeyword,
		String assistantMessage) {

	public AiSearchConditionExtractionResult(
			LocalDate lostDateFrom, LocalDate lostDateTo, String lostPlaceText, String assistantMessage) {
		this(lostDateFrom, lostDateTo, lostPlaceText, null, null, assistantMessage);
	}
}
