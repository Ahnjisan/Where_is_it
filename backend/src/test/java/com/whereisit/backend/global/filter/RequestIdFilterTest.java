package com.whereisit.backend.global.filter;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.DispatcherType;

@DisplayName("요청 ID 필터")
class RequestIdFilterTest {

	private final RequestIdFilter filter = new RequestIdFilter();

	@Test
	@DisplayName("요청 처리 중에만 MDC에 requestId를 두고, 끝나면 지운다")
	void putsRequestIdOnlyDuringRequest() throws Exception {
		AtomicReference<String> seen = new AtomicReference<>();

		filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
				(request, response) -> seen.set(MDC.get(RequestIdFilter.REQUEST_ID_KEY)));

		assertThat(seen.get()).isNotBlank();
		assertThat(MDC.get(RequestIdFilter.REQUEST_ID_KEY)).isNull();
	}

	@Test
	@DisplayName("/error로 다시 전달(ERROR dispatch)돼도 원래 요청과 같은 requestId를 쓴다")
	void reusesRequestIdOnErrorDispatch() throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		AtomicReference<String> original = new AtomicReference<>();
		AtomicReference<String> onError = new AtomicReference<>();

		filter.doFilter(request, new MockHttpServletResponse(),
				(req, res) -> original.set(MDC.get(RequestIdFilter.REQUEST_ID_KEY)));
		request.setDispatcherType(DispatcherType.ERROR);
		filter.doFilter(request, new MockHttpServletResponse(),
				(req, res) -> onError.set(MDC.get(RequestIdFilter.REQUEST_ID_KEY)));

		assertThat(onError.get()).isNotBlank().isEqualTo(original.get());
	}

	@Test
	@DisplayName("다른 요청에는 다른 requestId를 준다")
	void differentRequestsGetDifferentIds() throws Exception {
		AtomicReference<String> first = new AtomicReference<>();
		AtomicReference<String> second = new AtomicReference<>();

		filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
				(req, res) -> first.set(MDC.get(RequestIdFilter.REQUEST_ID_KEY)));
		filter.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(),
				(req, res) -> second.set(MDC.get(RequestIdFilter.REQUEST_ID_KEY)));

		assertThat(first.get()).isNotEqualTo(second.get());
	}
}
