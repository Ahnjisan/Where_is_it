package com.whereisit.backend.tracking;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Issue #85 추적 조회 시작일")
class TrackingSearchWindowTest {

	private static final LocalDate TODAY = LocalDate.of(2026, 9, 22);

	@Test
	@DisplayName("30일 이내의 시작일은 그대로 쓴다")
	void keepsStartWithinLookback() {
		assertThat(TrackingSearchWindow.effectiveStart(LocalDate.of(2026, 9, 1), TODAY)).isEqualTo(LocalDate.of(2026, 9, 1));
		assertThat(TrackingSearchWindow.effectiveStart(TODAY.minusDays(30), TODAY)).isEqualTo(TODAY.minusDays(30));
	}

	@Test
	@DisplayName("30일보다 이르거나 비어 있으면 오늘-30일로, 미래면 오늘로 맞춘다")
	void clampsOutOfRangeStart() {
		assertThat(TrackingSearchWindow.effectiveStart(TODAY.minusDays(31), TODAY)).isEqualTo(TODAY.minusDays(30));
		assertThat(TrackingSearchWindow.effectiveStart(null, TODAY)).isEqualTo(TODAY.minusDays(30));
		assertThat(TrackingSearchWindow.effectiveStart(TODAY.plusDays(3), TODAY)).isEqualTo(TODAY);
	}
}
