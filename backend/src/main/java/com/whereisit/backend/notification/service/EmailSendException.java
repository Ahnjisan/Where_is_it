package com.whereisit.backend.notification.service;

public class EmailSendException extends Exception {

	/** 민감정보를 뺀, 저장해도 안전한 짧은 원인 코드(예: 예외 클래스 이름). */
	private final String errorCode;

	public EmailSendException(String errorCode, Throwable cause) {
		super(errorCode, cause);
		this.errorCode = errorCode;
	}

	public String getErrorCode() {
		return errorCode;
	}
}
