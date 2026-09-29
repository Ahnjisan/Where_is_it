package com.whereisit.backend.search.ai.port;

/**
 * 검색조건 추출 한 번에 AI에게 요청할 출력 범위. OpenAI 호출 수는 모드와 관계없이 1회다.
 */
public enum AiSearchConditionExtractionMode {

	/** API-11 INITIAL·TEXT. 날짜·장소·물품명/보관장소 검색어·ASSISTANT 메시지 6개 필드만 요청한다. */
	SEARCH_CONDITIONS,

	/** API-05 최초 검색 전용(Issue #99). 기존 6개 필드에 API-16 표시용 색상명(colorName)만 더 요청한다. */
	INITIAL_SEARCH_WITH_COLOR_NAME
}
