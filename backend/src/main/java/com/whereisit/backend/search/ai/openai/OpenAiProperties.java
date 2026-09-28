package com.whereisit.backend.search.ai.openai;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.whereisit.backend.global.error.BusinessException;
import com.whereisit.backend.search.error.SearchErrorCode;

@ConfigurationProperties("app.openai")
public record OpenAiProperties(String apiKey, String model, Duration timeout) {

	private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(10);

	public Duration effectiveTimeout() {
		return timeout == null ? DEFAULT_TIMEOUT : timeout;
	}

	public void requireConfigured() {
		if (apiKey == null || apiKey.isBlank() || model == null || model.isBlank()) {
			throw new BusinessException(SearchErrorCode.AI_CONDITION_UNAVAILABLE);
		}
	}
}
