package com.whereisit.backend.member.controller;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.whereisit.backend.global.response.ApiResponse;
import com.whereisit.backend.member.dto.MemberResponse;
import com.whereisit.backend.member.dto.UpdateMemberRequest;
import com.whereisit.backend.member.service.MemberService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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

	/** API-20 내 회원정보 수정. 공통규칙상 PATCH 대신 POST를 쓰고, 지금은 사용 언어만 바꾼다. */
	@Operation(summary = "API-20 내 회원정보 수정", description = """
			AT의 회원 ID로 사용 언어(languageCode)를 바꾼다. 같은 언어로 다시 요청해도 200이다.
			이미 등록한 분실물의 언어는 바뀌지 않고, 이후 새로 등록하는 분실물부터 기본값이 된다.""")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "수정 성공"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
					description = "VALIDATION_ERROR(languageCode 누락·빈 값, 정의되지 않은 필드), UNSUPPORTED_LANGUAGE(ko·en 외 언어)"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_REQUIRED(토큰 없음), INVALID_TOKEN, TOKEN_EXPIRED")
	})
	@PostMapping("/me")
	public ApiResponse<MemberResponse> updateMe(
			@AuthenticationPrincipal Long memberId, @Valid @RequestBody UpdateMemberRequest request) {
		return ApiResponse.ok(memberService.updateMe(memberId, request));
	}
}
