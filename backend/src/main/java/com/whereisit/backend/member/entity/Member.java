package com.whereisit.backend.member.entity;

import com.whereisit.backend.global.entity.BaseTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 회원. 테이블 명세 v2 05_회원(members).
 * UNSIGNED는 Hibernate가 만들지 못하므로 ID가 음수가 되지 않는 것은 자동 증가에 맡긴다.
 */
@Getter
@Entity
@Table(name = "members", uniqueConstraints = @UniqueConstraint(name = "uk_members_email", columnNames = "email"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "member_id")
	private Long id;

	/** 정규화(trim + 소문자)된 값만 저장한다. */
	@Column(name = "email", nullable = false, length = 254)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Convert(converter = LanguageCodeConverter.class)
	@Column(name = "language_code", nullable = false, length = 35)
	private LanguageCode languageCode;

	private Member(String email, String passwordHash, LanguageCode languageCode) {
		this.email = email;
		this.passwordHash = passwordHash;
		this.languageCode = languageCode;
	}

	public static Member create(String email, String passwordHash, LanguageCode languageCode) {
		return new Member(email, passwordHash, languageCode);
	}
}
