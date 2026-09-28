package com.whereisit.backend.tracking;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * 매일 한국시간 09:00에 추적 재검색을 1회 실행한다. 서버가 꺼져 있던 시각의 실행은 재기동 후 보충하지 않는다.
 * 같은 날 여러 번 실행돼도 최근_자동검색_완료일과 이메일_알림 (분실물, 기준일) UNIQUE로 한 번만 처리된다.
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
