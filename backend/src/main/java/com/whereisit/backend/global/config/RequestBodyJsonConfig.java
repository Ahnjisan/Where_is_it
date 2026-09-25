package com.whereisit.backend.global.config;

import java.util.List;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

@Configuration
public class RequestBodyJsonConfig implements WebMvcConfigurer {

	private final ObjectMapper objectMapper;

	public RequestBodyJsonConfig(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	@Override
	public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
		ObjectMapper requestBodyObjectMapper = objectMapper.copy()
				.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true);

		MappingJackson2HttpMessageConverter requestBodyConverter = new MappingJackson2HttpMessageConverter(
				requestBodyObjectMapper) {
			@Override
			public boolean canWrite(Class<?> clazz, MediaType mediaType) {
				return false;
			}
		};
		requestBodyConverter.setSupportedMediaTypes(List.of(MediaType.APPLICATION_JSON, MediaType.valueOf("application/*+json")));
		converters.add(0, requestBodyConverter);
	}
}
