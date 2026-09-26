package com.whereisit.backend.global.security;

import java.io.IOException;
import java.util.List;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import com.whereisit.backend.auth.error.AuthErrorCode;
import com.whereisit.backend.auth.jwt.JwtTokenProvider;
import com.whereisit.backend.global.error.BusinessException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authorization: Bearer AT를 검증해 인증 정보(principal = memberId)를 채운다.
 *
 * 이 필터는 예외를 던지지 않는다. 검증에 실패하면 오류 코드를 요청 속성에 남기고 인증 없이 다음으로 넘긴다.
 * 공개 경로는 그대로 처리되고, 보호 경로는 {@link JwtAuthenticationEntryPoint}가 그 코드로 401을 응답한다.
 * 필터에서 던진 예외는 @RestControllerAdvice까지 가지 않기 때문이다.
 *
 * 빈(@Component)으로 등록하지 않는다. 등록하면 서블릿 필터로도 한 번 더 실행된다. SecurityConfig에서 직접 만든다.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

	public static final String AUTH_ERROR_ATTRIBUTE = JwtAuthenticationFilter.class.getName() + ".AUTH_ERROR";

	private static final String BEARER_PREFIX = "Bearer ";

	private final JwtTokenProvider jwtTokenProvider;

	public JwtAuthenticationFilter(JwtTokenProvider jwtTokenProvider) {
		this.jwtTokenProvider = jwtTokenProvider;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String header = request.getHeader(HttpHeaders.AUTHORIZATION);
		if (header != null) {
			authenticate(request, header);
		}
		chain.doFilter(request, response);
	}

	private void authenticate(HttpServletRequest request, String header) {
		// 인증 방식 이름(Bearer)은 대소문자를 구분하지 않는다(RFC 7235).
		if (!header.regionMatches(true, 0, BEARER_PREFIX, 0, BEARER_PREFIX.length())) {
			request.setAttribute(AUTH_ERROR_ATTRIBUTE, AuthErrorCode.INVALID_TOKEN);
			return;
		}
		try {
			Long memberId = jwtTokenProvider.getMemberIdFromAccessToken(header.substring(BEARER_PREFIX.length()).trim());
			SecurityContext context = SecurityContextHolder.createEmptyContext();
			context.setAuthentication(new UsernamePasswordAuthenticationToken(memberId, null, List.of()));
			SecurityContextHolder.setContext(context);
		} catch (BusinessException e) {
			request.setAttribute(AUTH_ERROR_ATTRIBUTE, e.getErrorCode());
		}
	}
}
