package com.whereisit.backend.global.error;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDateTime;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.whereisit.backend.global.config.RequestBodyJsonConfig;
import com.whereisit.backend.global.config.TimeConfig;
import com.whereisit.backend.global.response.ApiResponse;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 실제 컨트롤러가 아직 없어서, 테스트 전용 컨트롤러로 공통 응답·오류 형식을 확인한다.
 */
@WebMvcTest
@Import({ GlobalExceptionHandlerTest.TestController.class, TimeConfig.class, RequestBodyJsonConfig.class })
@DisplayName("공통 응답과 오류 처리")
class GlobalExceptionHandlerTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	@DisplayName("성공 응답은 success·data 형식이고, 시각에 +09:00이 붙는다")
	void successResponse() throws Exception {
		mockMvc.perform(get("/test/ok"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data.name").value("hello"))
				.andExpect(jsonPath("$.data.createdAt").value("2026-09-23T15:00:00.000000+09:00"));
	}

	@Test
	@DisplayName("돌려줄 데이터가 없으면 data는 null이다")
	void successResponseWithoutData() throws Exception {
		mockMvc.perform(get("/test/no-data"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.success").value(true))
				.andExpect(jsonPath("$.data").value(nullValue()));
	}

	@Test
	@DisplayName("BusinessException은 담긴 오류 코드와 상태로 응답한다")
	void businessException() throws Exception {
		mockMvc.perform(get("/test/business"))
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.success").value(false))
				.andExpect(jsonPath("$.error.code").value("SAMPLE_CONFLICT"))
				.andExpect(jsonPath("$.error.fields").isArray())
				.andExpect(jsonPath("$.error.requestId").isNotEmpty());
	}

	@Test
	@DisplayName("본문 검증 실패는 400 VALIDATION_ERROR와 필드 목록을 준다")
	void bodyValidationError() throws Exception {
		mockMvc.perform(post("/test/validate")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"\",\"password\":\"short\"}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.error.fields[*].field", hasItems("email", "password")))
				.andExpect(jsonPath("$.error.fields[*].reason", hasItems("NotBlank", "Size")));
	}

	@Test
	@DisplayName("JSON 형식이 깨진 본문은 400 VALIDATION_ERROR다")
	void malformedJson() throws Exception {
		mockMvc.perform(post("/test/validate")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"));
	}

	@Test
	@DisplayName("알 수 없는 JSON 필드는 400 VALIDATION_ERROR와 필드 경로를 준다")
	void unknownJsonField() throws Exception {
		mockMvc.perform(post("/test/validate")
				.contentType(MediaType.APPLICATION_JSON)
				.content("{\"email\":\"a@example.com\",\"password\":\"password1\",\"typo\":1}"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.error.fields[0].field").value("typo"))
				.andExpect(jsonPath("$.error.fields[0].reason").value("UnknownField"));
	}

	@Test
	@DisplayName("필수 쿼리 파라미터가 없으면 400 VALIDATION_ERROR다")
	void missingParameter() throws Exception {
		mockMvc.perform(get("/test/param"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.error.fields[0].field").value("size"))
				.andExpect(jsonPath("$.error.fields[0].reason").value("Required"));
	}

	@Test
	@DisplayName("쿼리 파라미터 타입이 맞지 않으면 400 VALIDATION_ERROR다")
	void typeMismatch() throws Exception {
		mockMvc.perform(get("/test/param").param("size", "abc"))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.error.code").value("VALIDATION_ERROR"))
				.andExpect(jsonPath("$.error.fields[0].reason").value("TypeMismatch"));
	}

	@Test
	@DisplayName("허용하지 않는 메서드는 405 METHOD_NOT_ALLOWED다")
	void methodNotAllowed() throws Exception {
		mockMvc.perform(post("/test/ok"))
				.andExpect(status().isMethodNotAllowed())
				.andExpect(jsonPath("$.error.code").value("METHOD_NOT_ALLOWED"));
	}

	@Test
	@DisplayName("지원하지 않는 Content-Type은 415 UNSUPPORTED_MEDIA_TYPE이다")
	void unsupportedMediaType() throws Exception {
		mockMvc.perform(post("/test/validate")
				.contentType(MediaType.TEXT_PLAIN)
				.content("hello"))
				.andExpect(status().isUnsupportedMediaType())
				.andExpect(jsonPath("$.error.code").value("UNSUPPORTED_MEDIA_TYPE"));
	}

	@Test
	@DisplayName("없는 경로는 404 RESOURCE_NOT_FOUND다")
	void noResource() throws Exception {
		mockMvc.perform(get("/api/does-not-exist"))
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
	}

	@Test
	@DisplayName("예상하지 못한 예외는 500 INTERNAL_ERROR이고, 예외 원문을 응답에 담지 않는다")
	void unexpectedError() throws Exception {
		MvcResult result = mockMvc.perform(get("/test/boom"))
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"))
				.andExpect(jsonPath("$.error.message").value("Unexpected server error."))
				.andExpect(jsonPath("$.error.requestId").isNotEmpty())
				.andReturn();

		assertThat(result.getResponse().getContentAsString()).doesNotContain("응답에 나가면 안 되는 원인");
	}

	@RestController
	static class TestController {

		@GetMapping("/test/ok")
		ApiResponse<SampleResponse> ok() {
			return ApiResponse.ok(new SampleResponse("hello", LocalDateTime.of(2026, 9, 23, 15, 0, 0)));
		}

		@GetMapping("/test/no-data")
		ApiResponse<Void> noData() {
			return ApiResponse.ok();
		}

		@GetMapping("/test/business")
		ApiResponse<Void> business() {
			throw new BusinessException(TestErrorCode.SAMPLE_CONFLICT);
		}

		@GetMapping("/test/boom")
		ApiResponse<Void> boom() {
			throw new IllegalStateException("응답에 나가면 안 되는 원인");
		}

		@GetMapping("/test/param")
		ApiResponse<Void> param(@RequestParam int size) {
			return ApiResponse.ok();
		}

		@PostMapping("/test/validate")
		ApiResponse<Void> validate(@RequestBody @Valid SampleRequest request) {
			return ApiResponse.ok();
		}
	}

	record SampleResponse(String name, LocalDateTime createdAt) {
	}

	record SampleRequest(@NotBlank String email, @Size(min = 8, max = 20) String password) {
	}

	/** 도메인 enum이 ErrorCode를 구현하는 방식도 함께 확인한다. Lombok은 테스트 클래스패스에 없어 직접 쓴다. */
	enum TestErrorCode implements ErrorCode {

		SAMPLE_CONFLICT(HttpStatus.CONFLICT, "Sample conflict.");

		private final HttpStatus status;
		private final String message;

		TestErrorCode(HttpStatus status, String message) {
			this.status = status;
			this.message = message;
		}

		@Override
		public String getCode() {
			return name();
		}

		@Override
		public HttpStatus getStatus() {
			return status;
		}

		@Override
		public String getMessage() {
			return message;
		}
	}
}
