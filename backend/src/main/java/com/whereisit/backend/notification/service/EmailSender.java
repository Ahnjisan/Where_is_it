package com.whereisit.backend.notification.service;

/**
 * 실제 발송 방식을 감춘다. 지금은 JavaMailSender(SMTP) 구현체 하나만 쓰지만,
 * 나중에 다른 이메일 제공자로 바꿀 때 이 인터페이스의 구현체만 바꾸면 되게 한다.
 */
public interface EmailSender {

	/** 발송 실패 시 {@link EmailSendException}을 던진다. 성공은 발송 요청이 접수됐다는 뜻이지 수신·열람을 보장하지 않는다. */
	void send(String recipientEmail, String subject, String body) throws EmailSendException;
}
