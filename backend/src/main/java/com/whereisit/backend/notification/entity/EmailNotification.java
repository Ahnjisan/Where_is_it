package com.whereisit.backend.notification.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.whereisit.backend.global.entity.BaseTimeEntity;
import com.whereisit.backend.lostitem.entity.LostItem;
import com.whereisit.backend.member.entity.LanguageCode;
import com.whereisit.backend.member.entity.LanguageCodeConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 이메일_알림. 테이블 명세 v2 09_이메일알림(email_notifications).
 * 한 분실물의 하루치 신규 후보를 메일 한 건으로 묶는다((lost_item_id, notification_date) UNIQUE).
 */
@Getter
@Entity
@Table(name = "email_notifications",
		uniqueConstraints = @UniqueConstraint(name = "uk_email_notifications_daily", columnNames = { "lost_item_id", "notification_date" }))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailNotification extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "notification_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "lost_item_id", nullable = false, updatable = false)
	private LostItem lostItem;

	@Column(name = "notification_date", nullable = false, updatable = false)
	private LocalDate notificationDate;

	@Column(name = "recipient_email", nullable = false, length = 254, updatable = false)
	private String recipientEmail;

	@Convert(converter = LanguageCodeConverter.class)
	@Column(name = "language_code", nullable = false, length = 35, updatable = false)
	private LanguageCode languageCode;

	@Column(name = "subject", nullable = false, length = 255, updatable = false)
	private String subject;

	@Column(name = "body", nullable = false, columnDefinition = "TEXT", updatable = false)
	private String body;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 16)
	private EmailNotificationStatus status;

	@Column(name = "attempt_count", nullable = false)
	private int attemptCount;

	@Column(name = "error_code", length = 64)
	private String errorCode;

	@Column(name = "sent_at")
	private LocalDateTime sentAt;

	private EmailNotification(LostItem lostItem, LocalDate notificationDate, String recipientEmail,
			LanguageCode languageCode, String subject, String body) {
		this.lostItem = lostItem;
		this.notificationDate = notificationDate;
		this.recipientEmail = recipientEmail;
		this.languageCode = languageCode;
		this.subject = subject;
		this.body = body;
		this.status = EmailNotificationStatus.PENDING;
		this.attemptCount = 0;
	}

	public static EmailNotification create(LostItem lostItem, LocalDate notificationDate, String recipientEmail,
			LanguageCode languageCode, String subject, String body) {
		return new EmailNotification(lostItem, notificationDate, recipientEmail, languageCode, subject, body);
	}

	public void markSending() {
		this.status = EmailNotificationStatus.SENDING;
		this.attemptCount++;
	}

	public void markSent(LocalDateTime sentAt) {
		this.status = EmailNotificationStatus.SENT;
		this.sentAt = sentAt;
		this.errorCode = null;
	}

	public void markFailed(String errorCode) {
		this.status = EmailNotificationStatus.FAILED;
		this.errorCode = errorCode;
	}
}
