package com.whereisit.backend.global.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** 추적 배치(TrackingBatchJob)의 @Scheduled 실행을 켠다. */
@Configuration
@EnableScheduling
public class SchedulingConfig {
}
