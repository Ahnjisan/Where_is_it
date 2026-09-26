package com.whereisit.backend.member.dto;

import java.time.LocalDateTime;

import com.whereisit.backend.member.entity.Member;

/**
 * API 명세 v2 05_응답필드 Member. 비밀번호 해시는 담지 않는다.
 * memberId는 JSON 숫자의 정밀도 손실을 피하려고 숫자 문자열로 준다(02_공통규칙 R11).
 */
public record MemberResponse(String memberId, String email, String languageCode, LocalDateTime createdAt) {

	public static MemberResponse from(Member member) {
		return new MemberResponse(
				String.valueOf(member.getId()),
				member.getEmail(),
				member.getLanguageCode().getCode(),
				member.getCreatedAt());
	}
}
