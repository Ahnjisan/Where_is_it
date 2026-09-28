package com.whereisit.backend.e2e;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.boot.autoconfigure.mail.MailSenderAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import com.whereisit.backend.BackendApplication;
import com.whereisit.backend.candidate.ranking.initial.InitialSearchCandidateRanker;
import com.whereisit.backend.candidate.ranking.openai.OpenAiCandidateRanker;
import com.whereisit.backend.founditem.client.FoundItemClientConfig;
import com.whereisit.backend.global.config.SchedulingConfig;
import com.whereisit.backend.notification.service.JavaMailEmailSender;
import com.whereisit.backend.search.ai.openai.OpenAiConfig;
import com.whereisit.backend.search.ai.openai.OpenAiSearchConditionExtractor;
import com.whereisit.backend.tracking.TrackingBatchJob;

/** Browser E2E 전용 진입점. 운영 artifact와 운영 component scan에는 포함되지 않는다. */
@Profile("e2e")
@Configuration(proxyBeanMethods = false)
@EnableAutoConfiguration(exclude = MailSenderAutoConfiguration.class)
@EntityScan(basePackages = "com.whereisit.backend")
@EnableJpaRepositories(basePackages = "com.whereisit.backend")
@ComponentScan(
		basePackages = "com.whereisit.backend",
		excludeFilters = @ComponentScan.Filter(
				type = FilterType.ASSIGNABLE_TYPE,
				classes = {
						BackendApplication.class,
						SchedulingConfig.class,
						TrackingBatchJob.class,
						FoundItemClientConfig.class,
						OpenAiConfig.class,
						OpenAiSearchConditionExtractor.class,
						OpenAiCandidateRanker.class,
						InitialSearchCandidateRanker.class,
						JavaMailEmailSender.class
				}))
public class E2eBackendApplication {

	public static void main(String[] args) {
		TimeZone.setDefault(TimeZone.getTimeZone("Asia/Seoul"));
		SpringApplication.run(E2eBackendApplication.class, args);
	}
}
