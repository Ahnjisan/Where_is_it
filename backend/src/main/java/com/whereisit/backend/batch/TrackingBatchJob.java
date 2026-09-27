package com.whereisit.backend.batch;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * B02 제안: 매일 한국시간 09:00에 1회 실행한다. 서버가 꺼져 있던 시간대는 수행하지 않으며,
 * 재기동으로 이전 실행을 대신하지 않는다(B10).
 */
@Component
@RequiredArgsConstructor
public class TrackingBatchJob {

	private final TrackingBatchService trackingBatchService;

	@Scheduled(cron = "0 0 9 * * *", zone = "Asia/Seoul")
	public void runDaily() {
		trackingBatchService.runDailyBatch();
	}
}
