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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** 공개 API라서 문서 기본값인 Bearer 인증을 해제한다(@SecurityRequirements). */
@Tag(name = "인증", description = "회원가입·로그인·토큰 재발급·로그아웃")
@SecurityRequirements
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

	private final AuthService authService;

	/** API-01 회원가입. 201과 Member. */
	@Operation(summary = "API-01 회원가입", description = """
			이메일·비밀번호로 가입한다. 이메일은 앞뒤 공백을 지우고 소문자로 바꿔 저장한다.
			비밀번호는 영문·숫자·ASCII 특수문자 8~20자이며 공백은 쓸 수 없다. languageCode는 ko, en만 허용한다.""")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "가입 성공. 가입한 회원 정보"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400",
					description = "VALIDATION_ERROR(형식 오류), UNSUPPORTED_LANGUAGE(ko·en 외 언어)"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "EMAIL_ALREADY_EXISTS")
	})
	@PostMapping("/signup")
	public ResponseEntity<ApiResponse<MemberResponse>> signup(@Valid @RequestBody SignupRequest request) {
		return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(authService.signup(request)));
	}

	/** API-02 로그인. 토큰이 캐시에 남지 않게 no-store를 붙인다. */
	@Operation(summary = "API-02 로그인", description = """
			AT(30분)와 RT(14일)를 발급한다. 로그인할 때마다 기기별 RT가 새로 발급된다.
			이메일이나 비밀번호가 틀리면 형식과 관계없이 INVALID_CREDENTIALS다.""")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공. AT·RT와 회원 정보"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "VALIDATION_ERROR(빈 값)"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "INVALID_CREDENTIALS")
	})
	@PostMapping("/login")
	public ResponseEntity<ApiResponse<LoginResponse>> login(@Valid @RequestBody LoginRequest request) {
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.body(ApiResponse.ok(authService.login(request)));
	}

	/** API-18 AT·RT 재발급. AT 없이 본문의 RT로 인증하고, 로그인과 같은 응답에 no-store를 붙인다. */
	@Operation(summary = "API-18 토큰 재발급", description = """
			AT 없이 본문의 RT로 인증한다. 성공하면 요청에 쓴 RT는 즉시 무효가 되고 새 AT·RT를 발급한다(RT 회전).
			같은 RT로 동시에 요청하면 하나만 성공한다.""")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "재발급 성공. 새 AT·RT와 회원 정보"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "VALIDATION_ERROR(빈 값)"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401",
					description = "INVALID_REFRESH_TOKEN(없거나 만료·무효인 RT)")
	})
	@PostMapping("/refresh")
	public ResponseEntity<ApiResponse<LoginResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
		return ResponseEntity.ok()
				.cacheControl(CacheControl.noStore())
				.body(ApiResponse.ok(authService.refresh(request)));
	}

	/** API-19 로그아웃. RT가 없거나 이미 무효여도 200(data: null). */
	@Operation(summary = "API-19 로그아웃", description = """
			본문의 RT 하나(이 기기)만 무효화한다. RT가 없거나 이미 무효여도 200(data: null)이다.
			AT는 만료될 때까지 유효하므로 클라이언트가 AT·RT를 지운다.""")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 처리됨(data: null)"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "VALIDATION_ERROR(refreshToken 누락)")
	})
	@PostMapping("/logout")
	public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
		authService.logout(request);
		return ResponseEntity.ok(ApiResponse.ok());
	}
}
