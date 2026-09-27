package com.whereisit.backend.auth.controller;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.whereisit.backend.auth.dto.LoginRequest;
import com.whereisit.backend.auth.dto.LoginResponse;
import com.whereisit.backend.auth.dto.RefreshTokenRequest;
import com.whereisit.backend.auth.dto.SignupRequest;
import com.whereisit.backend.auth.service.AuthService;
import com.whereisit.backend.global.response.ApiResponse;
import com.whereisit.backend.member.dto.MemberResponse;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	/** API-01 회원가입. 201과 Member. */
	@PostMapping("/signup")
	public ResponseEntity<ApiResponse<MemberResponse>> signup(@Valid @RequestBody SignupRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(authService.signup(request)));
	}

	/** API-02 로그인. 토큰이 캐시에 남지 않게 no-store를 붙인다. */
	@PostMapping("/login")
	public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.body(ApiResponse.ok(authService.login(request)));
	}

	/** API-18 AT·RT 재발급. AT 없이 본문의 RT로 인증하고, 로그인과 같은 응답에 no-store를 붙인다. */
	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<LoginResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.body(ApiResponse.ok(authService.refresh(request)));
	}

	/** API-19 로그아웃. RT가 없거나 이미 무효여도 200(data: null). */
	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
		authService.logout(request);
		return ResponseEntity.ok(ApiResponse.ok());
	}
}
