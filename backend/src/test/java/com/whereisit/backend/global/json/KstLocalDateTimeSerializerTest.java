package com.whereisit.backend.global.json;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.TimeZone;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;

@DisplayName("응답 시각 직렬화")
class KstLocalDateTimeSerializerTest {

	private final ObjectMapper objectMapper = new ObjectMapper()
			.registerModule(new SimpleModule()
					.addSerializer(LocalDateTime.class, new KstLocalDateTimeSerializer()));

	private final TimeZone originalTimeZone = TimeZone.getDefault();

	@AfterEach
	void restoreTimeZone() {
		TimeZone.setDefault(originalTimeZone);
	}

	@Test
	@DisplayName("마이크로초 6자리와 +09:00 오프셋을 붙인다")
	void writesMicrosecondsAndOffset() throws JsonProcessingException {
		String json = objectMapper.writeValueAsString(LocalDateTime.of(2026, 9, 23, 15, 0, 0, 123_456_000));

		assertThat(json).isEqualTo("\"2026-09-23T15:00:00.123456+09:00\"");
	}

	@Test
	@DisplayName("기본 시간대가 UTC여도 +09:00으로 쓴다")
	void independentFromDefaultTimeZone() throws JsonProcessingException {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));

		String json = objectMapper.writeValueAsString(LocalDateTime.of(2026, 9, 23, 15, 0, 0));

		assertThat(json).isEqualTo("\"2026-09-23T15:00:00.000000+09:00\"");
	}
}
