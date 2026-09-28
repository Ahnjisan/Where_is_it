package com.whereisit.backend.notification.service;

import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/** Spring Mail(JavaMailSender)로 SMTP 발송한다. 서버 주소와 계정은 application.yml의 spring.mail.*(MAIL_* 환경변수)로 설정한다. */
@Component
@RequiredArgsConstructor
public class JavaMailEmailSender implements EmailSender {

	private final JavaMailSender mailSender;

	@Override
	public void send(String recipientEmail, String subject, String body) throws EmailSendException {
		try {
			SimpleMailMessage message = new SimpleMailMessage();
			message.setTo(recipientEmail);
			message.setSubject(subject);
			message.setText(body);
			mailSender.send(message);
		}
		catch (MailException e) {
			throw new EmailSendException(e.getClass().getSimpleName(), e);
		}
	}
}
