package com.whereisit.backend.notification.entity;

/** 발송_상태. 테이블 명세 v2 09_이메일알림. */
public enum EmailNotificationStatus {
	PENDING,
	SENDING,
	SENT,
	FAILED,
	UNKNOWN,
	CANCELLED
}
