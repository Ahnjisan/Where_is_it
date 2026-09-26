package com.whereisit.backend.support;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;

/**
 * 테스트에서 시간을 멈추거나 앞으로 보내는 시계. 토큰 만료·RT 정리(TC-04·TC-31)를 기다리지 않고 확인한다.
 */
public class MutableClock extends Clock {

	private Instant instant;
	private final ZoneId zone;

	public MutableClock(Instant instant, ZoneId zone) {
		this.instant = instant;
		this.zone = zone;
	}

	public void setInstant(Instant instant) {
		this.instant = instant;
	}

	public void advance(Duration duration) {
		this.instant = instant.plus(duration);
	}

	@Override
	public ZoneId getZone() {
		return zone;
	}

	@Override
	public Clock withZone(ZoneId zone) {
		return new MutableClock(instant, zone);
	}

	@Override
	public Instant instant() {
		return instant;
	}
}
