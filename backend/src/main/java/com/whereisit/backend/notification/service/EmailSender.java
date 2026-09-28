package com.whereisit.backend.notification.service;

/**
 * 실제 발송 방식을 감춘다. 지금은 SMTP(JavaMailSender) 구현체 하나만 쓰지만,
 * 이메일 발송 서비스가 확정되면 이 인터페이스의 구현체만 바꾼다.
 */
public interface EmailSender {

	/** 발송 실패 시 {@link EmailSendException}을 던진다. 성공은 발송 요청이 접수됐다는 뜻이지 수신·열람을 보장하지 않는다. */
	void send(String recipientEmail, String subject, String body) throws EmailSendException;
}
