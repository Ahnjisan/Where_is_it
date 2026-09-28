package com.whereisit.backend.notification.entity;

/** 발송_상태. 테이블 명세 v2 09_이메일알림. SENT는 메일 제공자 접수 성공이며 수신·열람을 뜻하지 않는다. */
public enum EmailNotificationStatus {
	PENDING,
	SENDING,
	SENT,
	FAILED,
	UNKNOWN,
	CANCELLED
}
