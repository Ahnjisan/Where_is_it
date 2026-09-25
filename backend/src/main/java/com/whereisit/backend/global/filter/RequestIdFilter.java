package com.whereisit.backend.global.filter;

import java.io.IOException;
import java.util.UUID;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * 요청마다 requestId를 만들어 로그와 오류 응답에 함께 남긴다.
 * 인증 필터에서 나는 오류에도 붙도록 가장 먼저 실행한다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

	public static final String REQUEST_ID_KEY = "requestId";

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		MDC.put(REQUEST_ID_KEY, UUID.randomUUID().toString());
		try {
			chain.doFilter(request, response);
		} finally {
			MDC.remove(REQUEST_ID_KEY);
		}
	}
}
