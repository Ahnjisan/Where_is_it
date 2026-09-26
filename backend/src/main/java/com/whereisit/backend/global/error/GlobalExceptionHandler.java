package com.whereisit.backend.global.error;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.whereisit.backend.global.error.ErrorResponse.FieldError;

import lombok.extern.slf4j.Slf4j;

/**
 * 모든 오류를 API 명세 02_공통규칙 R8 형식으로 바꾼다.
 * 예외 종류별로 처리하되, 맨 끝의 Exception 처리기가 남은 예외를 INTERNAL_ERROR로 받는다.
 * Spring이 던지는 클라이언트 오류(405·415·없는 경로 등)를 아래에 명시해야 그 처리기가 500으로 바꾸지 않는다.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

	@ExceptionHandler(BusinessException.class)
	public ResponseEntity<ErrorResponse> handleBusiness(BusinessException e) {
		log.warn("business error: {}", e.getErrorCode().getCode());
		return toResponse(e.getErrorCode(), List.of());
	}

	/** 요청 본문 @Valid 검증 실패 */
	@ExceptionHandler(MethodArgumentNotValidException.class)
	public ResponseEntity<ErrorResponse> handleBodyValidation(MethodArgumentNotValidException e) {
		List<FieldError> fields = e.getBindingResult().getFieldErrors().stream()
				.map(error -> new FieldError(error.getField(), constraintNameOf(error)))
				.toList();
		return toResponse(CommonErrorCode.VALIDATION_ERROR, fields);
	}

	/** 쿼리·경로 값 검증 실패 */
	@ExceptionHandler(HandlerMethodValidationException.class)
	public ResponseEntity<ErrorResponse> handleParameterValidation(HandlerMethodValidationException e) {
		List<FieldError> fields = new ArrayList<>();
		for (ParameterValidationResult result : e.getParameterValidationResults()) {
			String name = result.getMethodParameter().getParameterName();
			for (MessageSourceResolvable error : result.getResolvableErrors()) {
				fields.add(new FieldError(name, constraintNameOf(error)));
			}
		}
		return toResponse(CommonErrorCode.VALIDATION_ERROR, fields);
	}

	/** 본문이 JSON으로 읽히지 않는 경우(형식 오류, 타입 불일치) */
	@ExceptionHandler(HttpMessageNotReadableException.class)
	public ResponseEntity<ErrorResponse> handleUnreadableBody(HttpMessageNotReadableException e) {
		List<FieldError> fields = e.getCause() instanceof JsonMappingException jsonError
				? List.of(new FieldError(pathOf(jsonError), reasonOf(jsonError)))
				: List.of();
		return toResponse(CommonErrorCode.VALIDATION_ERROR, fields);
	}

	@ExceptionHandler(MissingServletRequestParameterException.class)
	public ResponseEntity<ErrorResponse> handleMissingParameter(MissingServletRequestParameterException e) {
		return toResponse(CommonErrorCode.VALIDATION_ERROR,
				List.of(new FieldError(e.getParameterName(), "Required")));
	}

	@ExceptionHandler(MethodArgumentTypeMismatchException.class)
	public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
		return toResponse(CommonErrorCode.VALIDATION_ERROR,
				List.of(new FieldError(e.getName(), "TypeMismatch")));
	}

	/** 없는 경로로 온 요청 */
	@ExceptionHandler(NoResourceFoundException.class)
	public ResponseEntity<ErrorResponse> handleNoResource(NoResourceFoundException e) {
		return toResponse(CommonErrorCode.RESOURCE_NOT_FOUND, List.of());
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
		return toResponse(CommonErrorCode.METHOD_NOT_ALLOWED, List.of());
	}

	@ExceptionHandler(HttpMediaTypeNotSupportedException.class)
	public ResponseEntity<ErrorResponse> handleMediaTypeNotSupported(HttpMediaTypeNotSupportedException e) {
		return toResponse(CommonErrorCode.UNSUPPORTED_MEDIA_TYPE, List.of());
	}

	/** 위에서 걸러지지 않은 모든 예외. 원인은 로그에만 남기고 응답에는 requestId만 준다. */
	@ExceptionHandler(Exception.class)
	public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
		log.error("unexpected error", e);
		return toResponse(CommonErrorCode.INTERNAL_ERROR, List.of());
	}

	private ResponseEntity<ErrorResponse> toResponse(ErrorCode errorCode, List<FieldError> fields) {
		ErrorResponse body = ErrorResponse.of(errorCode, fields);
		return ResponseEntity.status(errorCode.getStatus()).body(body);
	}

	/** 검증 메시지는 로캘을 타므로, 마지막 코드(NotBlank, Size 등)를 reason으로 쓴다. */
	private String constraintNameOf(MessageSourceResolvable error) {
		String[] codes = error.getCodes();
		return codes == null || codes.length == 0 ? "Invalid" : codes[codes.length - 1];
	}

	private String pathOf(JsonMappingException e) {
		String path = e.getPath().stream()
				.map(reference -> reference.getFieldName() == null
						? "[" + reference.getIndex() + "]"
						: reference.getFieldName())
				.collect(Collectors.joining("."));
		return path.isBlank() ? "body" : path;
	}

	private String reasonOf(JsonMappingException e) {
		return e instanceof UnrecognizedPropertyException ? "UnknownField" : "InvalidFormat";
	}
}
