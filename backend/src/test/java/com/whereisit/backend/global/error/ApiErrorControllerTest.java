package com.whereisit.backend.global.error;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.slf4j.MDC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;

import com.whereisit.backend.global.filter.RequestIdFilter;

import jakarta.servlet.RequestDispatcher;

/**
 * /error로 넘어온 상태 코드를 공통 오류 코드로 바꾸는 규칙. 실제 /error 전달은 ErrorDispatchHttpTest에서 확인한다.
 */
@DisplayName("/error 컨트롤러의 상태 코드 매핑")
class ApiErrorControllerTest {

	private final ApiErrorController controller = new ApiErrorController();

	@BeforeEach
	void putRequestId() {
		MDC.put(RequestIdFilter.REQUEST_ID_KEY, "test-request-id");
	}

	@AfterEach
	void clearRequestId() {
		MDC.remove(RequestIdFilter.REQUEST_ID_KEY);
	}

	@ParameterizedTest(name = "{0} → {1} {2}")
	@CsvSource({
			"400, 400, VALIDATION_ERROR",
			"401, 401, AUTH_REQUIRED",
			"404, 404, RESOURCE_NOT_FOUND",
			"405, 405, METHOD_NOT_ALLOWED",
			"415, 415, UNSUPPORTED_MEDIA_TYPE",
			"500, 500, INTERNAL_ERROR",
			"503, 500, INTERNAL_ERROR",
			"403, 500, INTERNAL_ERROR"
	})
	@DisplayName("원래 상태 코드를 명세의 공통 오류 코드로 바꾸고, 대응 코드가 없으면 INTERNAL_ERROR")
	void mapsStatusToErrorCode(int originalStatus, int expectedStatus, String expectedCode) {
		ResponseEntity<ErrorResponse> response = controller.error(errorRequest(originalStatus));

		assertThat(response.getStatusCode().value()).isEqualTo(expectedStatus);
		assertThat(response.getBody()).isNotNull();
		assertThat(response.getBody().success()).isFalse();
		assertThat(response.getBody().error().code()).isEqualTo(expectedCode);
		assertThat(response.getBody().error().fields()).isEmpty();
		assertThat(response.getBody().error().requestId()).isEqualTo("test-request-id");
	}

	@Test
	@DisplayName("상태 코드 정보가 없으면 500 INTERNAL_ERROR")
	void missingStatus() {
		ResponseEntity<ErrorResponse> response = controller.error(new MockHttpServletRequest());

		assertThat(response.getStatusCode().value()).isEqualTo(500);
		assertThat(response.getBody().error().code()).isEqualTo("INTERNAL_ERROR");
	}

	@Test
	@DisplayName("401에만 WWW-Authenticate: Bearer를 붙인다")
	void wwwAuthenticateOnlyFor401() {
		assertThat(controller.error(errorRequest(401)).getHeaders().getFirst(HttpHeaders.WWW_AUTHENTICATE))
				.isEqualTo("Bearer");
		assertThat(controller.error(errorRequest(400)).getHeaders().containsKey(HttpHeaders.WWW_AUTHENTICATE))
				.isFalse();
	}

	@Test
	@DisplayName("원인 예외의 메시지는 응답에 넣지 않는다")
	void doesNotExposeExceptionMessage() {
		MockHttpServletRequest request = errorRequest(500);
		request.setAttribute(RequestDispatcher.ERROR_EXCEPTION, new IllegalStateException("internal-detail"));

		ResponseEntity<ErrorResponse> response = controller.error(request);

		assertThat(response.getBody().error().message()).isEqualTo("Unexpected server error.");
		assertThat(response.getBody().toString()).doesNotContain("internal-detail");
	}

	private MockHttpServletRequest errorRequest(int status) {
		MockHttpServletRequest request = new MockHttpServletRequest();
		request.setAttribute(RequestDispatcher.ERROR_STATUS_CODE, status);
		return request;
	}
}
