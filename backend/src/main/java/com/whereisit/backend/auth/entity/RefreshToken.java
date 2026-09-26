package com.whereisit.backend.auth.entity;

import java.time.LocalDateTime;

import com.whereisit.backend.global.entity.BaseCreatedTimeEntity;
import com.whereisit.backend.member.entity.Member;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 로그인한 기기별 RT. 테이블 명세 v2 11_리프레시토큰(refresh_tokens).
 * 원문은 저장하지 않고 SHA-256 해시만 둔다. 회원 한 명이 여러 행(기기)을 가질 수 있다.
 * created_at(발급 시각)은 BaseCreatedTimeEntity가 채운다.
 */
@Getter
@Entity
@Table(
		name = "refresh_tokens",
		uniqueConstraints = @UniqueConstraint(name = "uk_refresh_tokens_token_hash", columnNames = "token_hash"),
		indexes = @Index(name = "idx_refresh_tokens_member", columnList = "member_id, expires_at"))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class RefreshToken extends BaseCreatedTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "refresh_token_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY, optional = false)
	@JoinColumn(name = "member_id", nullable = false, foreignKey = @ForeignKey(name = "fk_refresh_tokens_member"))
	private Member member;

	/** 명세상 utf8mb4_bin이지만, 값이 항상 소문자 16진수라서 콜레이션을 따로 지정하지 않는다. */
	@Column(name = "token_hash", nullable = false, length = 64)
	private String tokenHash;

	@Column(name = "expires_at", nullable = false)
	private LocalDateTime expiresAt;

	private RefreshToken(Member member, String tokenHash, LocalDateTime expiresAt) {
		this.member = member;
		this.tokenHash = tokenHash;
		this.expiresAt = expiresAt;
	}

	public static RefreshToken issue(Member member, String tokenHash, LocalDateTime expiresAt) {
		return new RefreshToken(member, tokenHash, expiresAt);
	}
}
