package com.whereisit.backend.global.error;

import java.util.List;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.whereisit.backend.auth.error.AuthErrorCode;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

/**
 * /error로 넘어온 오류도 공통 오류 형식(02_공통규칙 R8)으로 응답한다. Spring Boot 기본 BasicErrorController를 대신한다.
 *
 * 컨트롤러 밖(Security 방화벽의 URL 거부, 필터에서 난 예외, 컨테이너 오류)에서 난 오류는
 * {@link GlobalExceptionHandler}까지 가지 않고 여기로 온다. 컨트롤러 안의 오류는 계속 GlobalExceptionHandler가 처리한다.
 * 여기서는 원래 상태 코드만 보고 공통 코드로 바꾼다. 상태 코드가 잘못된 오류는 발생한 곳에서 고친다.
 */
@Slf4j
@RestController
public class ApiErrorController implements ErrorController {

	@RequestMapping("${server.error.path:${error.path:/error}}")
	public ResponseEntity<ErrorResponse> error(HttpServletRequest request) {
		HttpStatus status = statusOf(request);
		ErrorCode errorCode = errorCodeOf(status);
		logError(request, status, errorCode);

		ResponseEntity.BodyBuilder response = ResponseEntity.status(errorCode.getStatus());
		if (errorCode.getStatus() == HttpStatus.UNAUTHORIZED) {
			response.header(HttpHeaders.WWW_AUTHENTICATE, ErrorResponse.BEARER_CHALLENGE);
		}
		return response.body(ErrorResponse.of(errorCode, List.of()));
	}

	private HttpStatus statusOf(HttpServletRequest request) {
		Object statusCode = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
		HttpStatus status = statusCode instanceof Integer code ? HttpStatus.resolve(code) : null;
		return status == null ? HttpStatus.INTERNAL_SERVER_ERROR : status;
	}

	/**
	 * 명세 06_오류코드에 있는 코드로만 바꾼다. 대응하는 코드가 없는 상태는 INTERNAL_ERROR로 보낸다.
	 * 지금 구조에서 그런 상태(403 등)가 /error까지 올 경로는 없다. 생기면 명세에 코드를 추가하고 여기에 넣는다.
	 */
	private ErrorCode errorCodeOf(HttpStatus status) {
		return switch (status) {
			case BAD_REQUEST -> CommonErrorCode.VALIDATION_ERROR;
			case UNAUTHORIZED -> AuthErrorCode.AUTH_REQUIRED;
			case NOT_FOUND -> CommonErrorCode.RESOURCE_NOT_FOUND;
			case METHOD_NOT_ALLOWED -> CommonErrorCode.METHOD_NOT_ALLOWED;
			case UNSUPPORTED_MEDIA_TYPE -> CommonErrorCode.UNSUPPORTED_MEDIA_TYPE;
			default -> CommonErrorCode.INTERNAL_ERROR;
		};
	}

	/** 원인 예외는 로그에만 남기고 응답에는 넣지 않는다. requestId는 로그 패턴에 함께 찍힌다. */
	private void logError(HttpServletRequest request, HttpStatus status, ErrorCode errorCode) {
		Throwable exception = (Throwable) request.getAttribute(RequestDispatcher.ERROR_EXCEPTION);
		if (status.is5xxServerError()) {
			log.error("error outside controllers: status={}", status.value(), exception);
		} else if (errorCode == CommonErrorCode.INTERNAL_ERROR) {
			log.error("unmapped error status outside controllers: status={}", status.value(), exception);
		} else {
			log.warn("request rejected outside controllers: status={}, cause={}", status.value(),
					exception == null ? "-" : exception.getClass().getSimpleName());
		}
	}
}
