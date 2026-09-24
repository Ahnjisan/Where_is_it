package com.whereisit.backend.global.error;

import org.springframework.http.HttpStatus;

/**
 * API 명세 06_오류코드의 코드 하나를 나타낸다.
 * 도메인마다 이 인터페이스를 구현한 enum을 두고, 공통 코드만 {@link CommonErrorCode}에 둔다.
 */
public interface ErrorCode {

	/** 응답의 error.code 값. 06_오류코드에 있는 이름이어야 한다. */
	String getCode();

	HttpStatus getStatus();

	/** 서버는 영어 한 가지로만 내려준다. 화면에 보일 문구는 프론트가 code를 보고 고른다. */
	String getMessage();
}
