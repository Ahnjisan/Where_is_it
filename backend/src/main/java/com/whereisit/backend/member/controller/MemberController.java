package com.whereisit.backend.member.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.whereisit.backend.global.response.ApiResponse;
import com.whereisit.backend.member.dto.MemberResponse;
import com.whereisit.backend.member.service.MemberService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

	private final MemberService memberService;

	/** API-03 내 회원정보 조회. 회원 ID는 요청이 아니라 AT에서 정한다. */
	@GetMapping("/me")
	public ApiResponse<MemberResponse> getMe(@AuthenticationPrincipal Long memberId) {
		return ApiResponse.ok(memberService.getMe(memberId));
	}
}
