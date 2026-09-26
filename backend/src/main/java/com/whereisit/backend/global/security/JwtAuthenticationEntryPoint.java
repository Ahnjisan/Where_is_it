package com.whereisit.backend.global.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whereisit.backend.auth.error.AuthErrorCode;
import com.whereisit.backend.global.error.ErrorCode;
import com.whereisit.backend.global.error.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.RequiredArgsConstructor;

/**
 * 인증이 필요한 경로에 인증 없이 온 요청을 공통 오류 형식(02_공통규칙 R8)의 401로 응답한다.
 * 토큰이 없으면 AUTH_REQUIRED, 필터가 남긴 오류가 있으면 그 코드(TOKEN_EXPIRED·INVALID_TOKEN)다.
 */
@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

	private final ObjectMapper objectMapper;

	@Override
	public void commence(HttpServletRequest request, HttpServletResponse response,
			AuthenticationException authException) throws IOException {
		ErrorCode errorCode = request.getAttribute(JwtAuthenticationFilter.AUTH_ERROR_ATTRIBUTE) instanceof ErrorCode code
				? code
				: AuthErrorCode.AUTH_REQUIRED;

		// 401에는 인증 방식을 알려 주는 헤더를 붙인다(RFC 6750 §3, 06_오류코드 AUTH_REQUIRED).
		response.setHeader(HttpHeaders.WWW_AUTHENTICATE, "Bearer");
		response.setStatus(errorCode.getStatus().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		objectMapper.writeValue(response.getWriter(), ErrorResponse.of(errorCode, List.of()));
	}
}
