package com.whereisit.backend.member.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.whereisit.backend.global.response.ApiResponse;
import com.whereisit.backend.member.dto.MemberResponse;
import com.whereisit.backend.member.service.MemberService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

@Tag(name = "회원", description = "내 회원정보")
@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {

	private final MemberService memberService;

	/** API-03 내 회원정보 조회. 회원 ID는 요청이 아니라 AT에서 정한다. */
	@Operation(summary = "API-03 내 회원정보 조회", description = "AT의 회원 ID로 조회한다. AT는 유효하지만 회원이 없으면 INVALID_TOKEN이다.")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_REQUIRED(토큰 없음), INVALID_TOKEN, TOKEN_EXPIRED")
	})
	@GetMapping("/me")
	public ApiResponse<MemberResponse> getMe(@AuthenticationPrincipal Long memberId) {
		return ApiResponse.ok(memberService.getMe(memberId));
	}
}
