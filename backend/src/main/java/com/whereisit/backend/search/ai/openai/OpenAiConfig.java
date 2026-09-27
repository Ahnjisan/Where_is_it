package com.whereisit.backend.search.ai.openai;

import java.net.http.HttpClient;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@EnableConfigurationProperties(OpenAiProperties.class)
public class OpenAiConfig {

	@Bean
	RestClient openAiRestClient(OpenAiProperties properties, RestClient.Builder builder) {
		HttpClient httpClient = HttpClient.newBuilder()
				.connectTimeout(properties.effectiveTimeout())
				.build();
		JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(properties.effectiveTimeout());
		return builder.clone()
				.baseUrl("https://api.openai.com/v1")
				.requestFactory(requestFactory)
				.build();
	}
}
