package com.whereisit.backend.member.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * enum 상수 이름(KO)이 아니라 API와 같은 소문자 코드(ko)로 저장한다.
 */
@Converter
public class LanguageCodeConverter implements AttributeConverter<LanguageCode, String> {

	@Override
	public String convertToDatabaseColumn(LanguageCode language) {
		return language == null ? null : language.getCode();
	}

	@Override
	public LanguageCode convertToEntityAttribute(String code) {
		if (code == null) {
			return null;
		}
		return LanguageCode.fromCode(code)
				.orElseThrow(() -> new IllegalStateException("Unsupported language code in DB: " + code));
	}
}
