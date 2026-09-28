package com.whereisit.backend.tracking;

import java.time.LocalDate;

/**
 * 추적 재검색의 습득일 조회 범위. 늦게 등록되는 습득물(습득 후 수개월 뒤 등록된 사례 확인)을 놓치지 않도록
 * 매번 습득물_조회_시작일부터 오늘까지 전체를 다시 받되, 조회량이 커지지 않게 시작일을 최대 30일 전으로 제한한다.
 */
public final class TrackingSearchWindow {

	public static final int MAX_LOOKBACK_DAYS = 30;

	private TrackingSearchWindow() {
	}

	/** 실제 조회에 쓸 시작일. 너무 이르면 오늘-30일로, 미래면 오늘로 맞춘다. */
	public static LocalDate effectiveStart(LocalDate searchStartDate, LocalDate today) {
		LocalDate earliest = today.minusDays(MAX_LOOKBACK_DAYS);
		if (searchStartDate == null || searchStartDate.isBefore(earliest)) {
			return earliest;
		}
		return searchStartDate.isAfter(today) ? today : searchStartDate;
	}
}
