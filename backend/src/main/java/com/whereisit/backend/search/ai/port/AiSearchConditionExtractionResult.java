package com.whereisit.backend.search.ai.port;

import java.time.LocalDate;

/**
 * @param colorName API-05({@link AiSearchConditionExtractionMode#INITIAL_SEARCH_WITH_COLOR_NAME})에서만 채우는
 *                  사용자 분실물의 표시용 색상명. 공식 색상 코드가 아니며, 그 밖의 모드에서는 항상 null이다.
 */
public record AiSearchConditionExtractionResult(
		LocalDate lostDateFrom,
		LocalDate lostDateTo,
		String lostPlaceText,
		String productNameKeyword,
		String storagePlaceKeyword,
		String colorName,
		String assistantMessage) {

	public AiSearchConditionExtractionResult(
			LocalDate lostDateFrom, LocalDate lostDateTo, String lostPlaceText, String productNameKeyword,
			String storagePlaceKeyword, String assistantMessage) {
		this(lostDateFrom, lostDateTo, lostPlaceText, productNameKeyword, storagePlaceKeyword, null, assistantMessage);
	}

	public AiSearchConditionExtractionResult(
			LocalDate lostDateFrom, LocalDate lostDateTo, String lostPlaceText, String assistantMessage) {
		this(lostDateFrom, lostDateTo, lostPlaceText, null, null, null, assistantMessage);
	}
}
