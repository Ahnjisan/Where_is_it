package com.whereisit.backend.global.error;

import lombok.Getter;

/**
 * 오류 코드를 담아 던지는 예외. 도메인마다 예외 클래스를 새로 만들지 않고
 * new BusinessException(MemberErrorCode.EMAIL_ALREADY_EXISTS) 처럼 쓴다.
 */
@Getter
public class BusinessException extends RuntimeException {

	private final transient ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode) {
		super(errorCode.getMessage());
		this.errorCode = errorCode;
	}
}
