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
 *
 * 필터·보안 단계의 오류는 원래 요청이 끝난 뒤 /error로 다시 전달(ERROR dispatch)된다.
 * 그때도 같은 requestId를 쓰도록 요청 속성에 남겨 두고, ERROR dispatch에서도 이 필터를 실행한다.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

	public static final String REQUEST_ID_KEY = "requestId";

	private static final String REQUEST_ID_ATTRIBUTE = RequestIdFilter.class.getName() + ".REQUEST_ID";

	@Override
	protected boolean shouldNotFilterErrorDispatch() {
		return false;
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		String requestId = request.getAttribute(REQUEST_ID_ATTRIBUTE) instanceof String existing
				? existing
				: UUID.randomUUID().toString();
		request.setAttribute(REQUEST_ID_ATTRIBUTE, requestId);
		MDC.put(REQUEST_ID_KEY, requestId);
		try {
			chain.doFilter(request, response);
		} finally {
			MDC.remove(REQUEST_ID_KEY);
		}
	}
}
