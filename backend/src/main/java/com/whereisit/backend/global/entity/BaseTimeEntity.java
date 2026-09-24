package com.whereisit.backend.global.entity;

import java.time.LocalDateTime;

import org.springframework.data.annotation.LastModifiedDate;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;

/**
 * 생성·수정 시각이 모두 있는 테이블이 상속한다.
 * 테이블 명세 v2 기준으로 회원, 분실물_검색_추적, 이메일_알림이 해당한다.
 */
@Getter
@MappedSuperclass
public abstract class BaseTimeEntity extends BaseCreatedTimeEntity {

	@LastModifiedDate
	@Column(name = "updated_at", nullable = false)
	private LocalDateTime updatedAt;
}
