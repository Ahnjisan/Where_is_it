package com.whereisit.backend.global.config;

import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.whereisit.backend.global.error.ErrorResponse;

import io.swagger.v3.core.converter.ModelConverters;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

/**
 * Swagger UI(/swagger-ui.html)와 OpenAPI 문서(/v3/api-docs) 설정(Issue #62).
 * 보호 API가 대부분이라 Bearer 인증을 문서 전체의 기본값으로 두고, 공개 API(/api/auth/**)만 Controller에서 해제한다.
 */
@Configuration
public class OpenApiConfig {

	public static final String BEARER_AUTH = "bearerAuth";

	private static final String ERROR_RESPONSE_SCHEMA = "ErrorResponse";

	@Bean
	public OpenAPI openAPI() {
		return new OpenAPI()
				.info(new Info()
						.title("Where is it API")
						.version("v1")
						.description("""
								어디갔지 Where is it Backend API.
								로그인(API-02) 응답의 accessToken을 오른쪽 위 Authorize에 넣으면 보호 API를 호출할 수 있다.
								성공 응답은 {"success": true, "data": ...}, 오류 응답은 {"success": false, "error": {...}} 형식이다.
								date-time 값에는 KST 오프셋(+09:00)이 붙는다."""))
				.components(new Components()
						.addSecuritySchemes(BEARER_AUTH, new SecurityScheme()
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")
								.description("로그인 응답의 accessToken. 'Bearer ' 접두어 없이 토큰만 넣는다.")))
				.addSecurityItem(new SecurityRequirement().addList(BEARER_AUTH));
	}

	/**
	 * Controller에는 오류 응답의 상태 코드와 설명만 적고, 본문 형식(ErrorResponse)은 여기서 한 번에 붙인다.
	 * springdoc은 본문을 적지 않은 응답에 메서드의 반환 타입(성공 응답)을 채우므로, 4xx·5xx는 모두 ErrorResponse로 바꾼다.
	 */
	@Bean
	public OpenApiCustomizer errorResponseCustomizer() {
		return openApi -> {
			ModelConverters.getInstance().readAll(ErrorResponse.class)
					.forEach((name, schema) -> openApi.getComponents().addSchemas(name, schema));
			Content errorContent = new Content().addMediaType(org.springframework.http.MediaType.APPLICATION_JSON_VALUE,
					new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + ERROR_RESPONSE_SCHEMA)));

			openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation -> {
				if (operation.getResponses() == null) {
					return;
				}
				operation.getResponses().forEach((code, response) -> {
					if (code.startsWith("4") || code.startsWith("5")) {
						response.setContent(errorContent);
					}
				});
			}));
		};
	}
}
