package com.whereisit.backend.member.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * API-20 회원 정보 수정 요청. 지금은 사용 언어만 바꿀 수 있다.
 *
 * @param languageCode 회원가입(API-01)과 같이 문자열로 받고 허용값(ko, en)은 서비스에서 확인한다.
 *                     enum으로 받으면 ja·KO가 VALIDATION_ERROR가 되지만, 명세는 UNSUPPORTED_LANGUAGE다(TC-29).
 */
public record UpdateMemberRequest(@NotBlank String languageCode) {
}
