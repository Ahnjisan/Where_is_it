package com.whereisit.backend;

import java.util.TimeZone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.whereisit.backend.global.config.TimeConfig;

@SpringBootApplication
public class BackendApplication {

	public static void main(String[] args) {
		// 로그 시각도 KST로 찍히도록, 로깅이 초기화되는 run() 이전에 기본 시간대를 고정한다.
		TimeZone.setDefault(TimeZone.getTimeZone(TimeConfig.KST));
		SpringApplication.run(BackendApplication.class, args);
	}

}
