package com.whereisit.backend.global.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("시간 기준 설정")
class TimeConfigTest {

	private final TimeConfig timeConfig = new TimeConfig();
	private final TimeZone originalTimeZone = TimeZone.getDefault();

	@AfterEach
	void restoreTimeZone() {
		TimeZone.setDefault(originalTimeZone);
	}

	@Test
	@DisplayName("기본 시간대가 UTC인 환경(CI 러너)에서도 KST로 고정한다")
	void applyDefaultTimeZone() {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

		timeConfig.applyDefaultTimeZone();

		assertThat(TimeZone.getDefault().toZoneId()).isEqualTo(TimeConfig.KST);
	}

	@Test
	@DisplayName("시계는 기본 시간대와 상관없이 KST를 쓴다")
	void clockUsesKst() {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

		assertThat(timeConfig.clock().getZone()).isEqualTo(TimeConfig.KST);
	}
}
