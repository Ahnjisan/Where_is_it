package com.whereisit.backend.global.json;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.whereisit.backend.global.config.TimeConfig;

/**
 * 응답의 시각을 KST 오프셋과 함께 내려준다(예: 2026-09-23T15:00:00.000000+09:00).
 * 저장값은 시간대 정보가 없는 LocalDateTime이지만, 명세(02_공통규칙 R12)는 오프셋을 요구한다.
 */
public class KstLocalDateTimeSerializer extends JsonSerializer<LocalDateTime> {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX");

	@Override
	public void serialize(LocalDateTime value, JsonGenerator gen, SerializerProvider serializers) throws IOException {
		gen.writeString(value.atZone(TimeConfig.KST).format(FORMATTER));
	}
}
