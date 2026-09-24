package com.whereisit.backend.global.config;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.TimeZone;

import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.whereisit.backend.global.json.KstLocalDateTimeSerializer;

import jakarta.annotation.PostConstruct;

/**
 * 시간 기준을 한곳에 모은다. API 명세 11_검토사항 D16:
 * 모든 날짜·시각은 KST로 저장·처리하고, 응답 date-time에는 +09:00 오프셋을 붙인다.
 */
@Configuration
public class TimeConfig {

	public static final ZoneId KST = ZoneId.of("Asia/Seoul");

	/**
	 * 서버가 어느 시간대에서 실행되든(CI 러너는 UTC) 기본 시간대를 KST로 맞춘다.
	 * main()을 거치지 않는 테스트 컨텍스트를 위한 것이고, 앱 실행 경로는 BackendApplication에서 더 먼저 고정한다.
	 */
	@PostConstruct
	void applyDefaultTimeZone() {
		TimeZone.setDefault(TimeZone.getTimeZone(KST));
	}

	/** 시각이 필요한 코드는 이 시계를 주입받는다. 테스트에서는 고정 시계로 바꿔 끼운다. */
	@Bean
	public Clock clock() {
		return Clock.system(KST);
	}

	@Bean
	public Jackson2ObjectMapperBuilderCustomizer kstDateTimeCustomizer() {
		return builder -> builder.serializerByType(LocalDateTime.class, new KstLocalDateTimeSerializer());
	}
}
