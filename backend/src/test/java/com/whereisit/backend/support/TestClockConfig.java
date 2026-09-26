package com.whereisit.backend.support;

import java.time.Instant;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.whereisit.backend.global.config.TimeConfig;

/**
 * 운영 Clock 대신 {@link MutableClock}을 주입한다. JWT 발급·검증, JPA 생성 시각이 모두 이 시계를 쓴다.
 */
@TestConfiguration(proxyBeanMethods = false)
public class TestClockConfig {

	/** 2026-09-22 15:00:00 KST */
	public static final Instant START = Instant.parse("2026-09-22T06:00:00Z");

	@Bean
	@Primary
	public MutableClock testClock() {
		return new MutableClock(START, TimeConfig.KST);
	}
}
