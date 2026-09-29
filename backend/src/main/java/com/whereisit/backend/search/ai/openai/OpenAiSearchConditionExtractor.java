package com.whereisit.backend.search.ai.openai;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.whereisit.backend.global.error.BusinessException;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractionMode;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractionRequest;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractionResult;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractor;
import com.whereisit.backend.search.error.SearchErrorCode;

@Component
public class OpenAiSearchConditionExtractor implements AiSearchConditionExtractor {

	private static final int MAX_PLACE_LENGTH = 255;
	private static final int MAX_SEARCH_KEYWORD_LENGTH = 200;
	private static final int MAX_ASSISTANT_LENGTH = 2000;
	private static final int MAX_COLOR_NAME_LENGTH = 100;
	private static final Set<String> OUTPUT_FIELDS = Set.of(
			"lostDateFrom", "lostDateTo", "lostPlaceText", "productNameKeyword",
			"storagePlaceKeyword", "assistantMessage");
	/** API-05 전용 출력 필드. 기존 6개 필드에 colorName 하나만 더한다. */
	private static final Set<String> INITIAL_SEARCH_OUTPUT_FIELDS = Set.of(
			"lostDateFrom", "lostDateTo", "lostPlaceText", "productNameKeyword",
			"storagePlaceKeyword", "assistantMessage", "colorName");
	private static final String INSTRUCTIONS = """
			Extract the lost date range, the place where the user lost the item, and short search keywords.
			productNameKeyword is the item-name query for the portal found-item list API.
			storagePlaceKeyword is the custody/storage-place query for that API; it is not the lost place.
			Preserve an existing date or place when the user does not change it. Use null only when it is unknown or cleared.
			Write assistantMessage in the requested language and keep it concise.
			Never create or return common codes, identifiers, status, tracking data, or email data.
			""";
	/**
	 * API-05 전용 추가 지시(Issue #99). 기존 지시 뒤에 덧붙이기만 하며, 다른 필드의 추출 규칙은 바꾸지 않는다.
	 */
	private static final String COLOR_NAME_INSTRUCTIONS = """
			colorName is a separate display-only field for the color of the item the user lost.
			Extracting colorName must not change how productNameKeyword or any other field is extracted.
			Fill colorName only when the user explicitly states the lost item's color; otherwise use null.
			Write colorName as a short Korean color name whatever the input language, for example 검은, 검정색, black -> 검정.
			If the item clearly has several colors, join the Korean color names with "/", for example 검정/흰색.
			Use null when the color is vague or uncertain, for example 어두운 색 or dark.
			Never return a color code, and never take a color from found-item candidates.
			""";

	private final OpenAiProperties properties;
	private final RestClient restClient;
	private final ObjectMapper objectMapper;

	public OpenAiSearchConditionExtractor(OpenAiProperties properties, RestClient openAiRestClient,
			ObjectMapper objectMapper) {
		this.properties = properties;
		this.restClient = openAiRestClient;
		this.objectMapper = objectMapper.copy()
				.configure(JsonParser.Feature.STRICT_DUPLICATE_DETECTION, true);
	}

	@Override
	public AiSearchConditionExtractionResult extract(AiSearchConditionExtractionRequest request) {
		properties.requireConfigured();
		try {
			String rawResponse = restClient.post()
					.uri("/responses")
					.contentType(MediaType.APPLICATION_JSON)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.apiKey())
					.body(buildRequest(request))
					.retrieve()
					.body(String.class);
			return parseResponse(rawResponse, request.mode());
		}
		catch (ResourceAccessException e) {
			if (hasTimeoutCause(e)) {
				throw new BusinessException(SearchErrorCode.SEARCH_TIMEOUT);
			}
			throw unavailable();
		}
		catch (RestClientResponseException e) {
			int status = e.getStatusCode().value();
			if (status == HttpStatus.REQUEST_TIMEOUT.value() || status == HttpStatus.GATEWAY_TIMEOUT.value()) {
				throw new BusinessException(SearchErrorCode.SEARCH_TIMEOUT);
			}
			if (status == HttpStatus.TOO_MANY_REQUESTS.value()) {
				throw new BusinessException(SearchErrorCode.RATE_LIMITED);
			}
			throw unavailable();
		}
		catch (RestClientException e) {
			throw unavailable();
		}
	}

	private Map<String, Object> buildRequest(AiSearchConditionExtractionRequest request) {
		Map<String, Object> propertiesSchema = new LinkedHashMap<>();
		propertiesSchema.put("lostDateFrom", nullableDateSchema());
		propertiesSchema.put("lostDateTo", nullableDateSchema());
		propertiesSchema.put("lostPlaceText",
				Map.of("type", List.of("string", "null"), "maxLength", MAX_PLACE_LENGTH));
		propertiesSchema.put("productNameKeyword",
				Map.of("type", List.of("string", "null"), "minLength", 1, "maxLength", MAX_SEARCH_KEYWORD_LENGTH));
		propertiesSchema.put("storagePlaceKeyword",
				Map.of("type", List.of("string", "null"), "minLength", 1, "maxLength", MAX_SEARCH_KEYWORD_LENGTH));
		propertiesSchema.put("assistantMessage",
				Map.of("type", "string", "minLength", 1, "maxLength", MAX_ASSISTANT_LENGTH));
		if (includesColorName(request.mode())) {
			propertiesSchema.put("colorName",
					Map.of("type", List.of("string", "null"), "minLength", 1, "maxLength", MAX_COLOR_NAME_LENGTH));
		}

		Map<String, Object> schema = new LinkedHashMap<>();
		schema.put("type", "object");
		schema.put("properties", propertiesSchema);
		// strict Structured Outputs는 모든 properties가 required여야 한다. 같은 Map에서 만들어 둘이 어긋나지 않게 한다.
		schema.put("required", List.copyOf(propertiesSchema.keySet()));
		schema.put("additionalProperties", false);

		Map<String, Object> format = new LinkedHashMap<>();
		format.put("type", "json_schema");
		format.put("name", "lost_item_search_conditions");
		format.put("strict", true);
		format.put("schema", schema);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("model", properties.model());
		body.put("store", false);
		body.put("instructions",
				includesColorName(request.mode()) ? INSTRUCTIONS + COLOR_NAME_INSTRUCTIONS : INSTRUCTIONS);
		body.put("input", inputText(request));
		body.put("text", Map.of("format", format));
		return body;
	}

	private static boolean includesColorName(AiSearchConditionExtractionMode mode) {
		return mode == AiSearchConditionExtractionMode.INITIAL_SEARCH_WITH_COLOR_NAME;
	}

	private Map<String, Object> nullableDateSchema() {
		return Map.of("type", List.of("string", "null"), "format", "date");
	}

	private String inputText(AiSearchConditionExtractionRequest request) {
		return "userNaturalLanguage=" + valueOf(request.userNaturalLanguage())
				+ "\ncurrentDescription=" + valueOf(request.currentDescription())
				+ "\nlanguageCode=" + valueOf(request.languageCode())
				+ "\nreferenceDate=" + valueOf(request.referenceDate())
				+ "\nexistingLostDateFrom=" + valueOf(request.existingLostDateFrom())
				+ "\nexistingLostDateTo=" + valueOf(request.existingLostDateTo())
				+ "\nexistingLostPlaceText=" + valueOf(request.existingLostPlaceText());
	}

	private String valueOf(Object value) {
		return value == null ? "null" : value.toString();
	}

	private AiSearchConditionExtractionResult parseResponse(String rawResponse, AiSearchConditionExtractionMode mode) {
		if (rawResponse == null || rawResponse.isBlank()) {
			throw unavailable();
		}
		try {
			JsonNode root = objectMapper.readTree(rawResponse);
			if (root == null || !root.isObject() || !root.path("status").isTextual()
					|| !"completed".equals(root.path("status").textValue()) || !root.path("output").isArray()) {
				throw unavailable();
			}

			List<String> outputTexts = new ArrayList<>();
			boolean refused = false;
			for (JsonNode outputItem : root.path("output")) {
				if (!outputItem.isObject() || !outputItem.path("type").isTextual()) {
					throw unavailable();
				}
				if (!"message".equals(outputItem.path("type").textValue())) {
					continue;
				}
				JsonNode content = outputItem.get("content");
				if (content == null || !content.isArray()) {
					throw unavailable();
				}
				for (JsonNode part : content) {
					if (!part.isObject() || !part.path("type").isTextual()) {
						throw unavailable();
					}
					String type = part.path("type").textValue();
					if ("refusal".equals(type)) {
						refused = true;
					}
					if ("output_text".equals(type)) {
						JsonNode text = part.get("text");
						if (text == null || !text.isTextual()) {
							throw unavailable();
						}
						outputTexts.add(text.textValue());
					}
				}
			}
			if (refused || outputTexts.size() != 1) {
				throw unavailable();
			}
			return parseStructuredOutput(outputTexts.get(0), mode);
		}
		catch (JsonProcessingException | DateTimeParseException e) {
			throw unavailable();
		}
	}

	private AiSearchConditionExtractionResult parseStructuredOutput(String outputText,
			AiSearchConditionExtractionMode mode) throws JsonProcessingException {
		if (outputText == null || outputText.isBlank()) {
			throw unavailable();
		}
		JsonNode output = objectMapper.readTree(outputText);
		if (output == null || !output.isObject()) {
			throw unavailable();
		}
		Set<String> actualFields = new LinkedHashSet<>();
		Iterator<String> fieldNames = output.fieldNames();
		fieldNames.forEachRemaining(actualFields::add);
		if (!actualFields.equals(includesColorName(mode) ? INITIAL_SEARCH_OUTPUT_FIELDS : OUTPUT_FIELDS)) {
			throw unavailable();
		}

		LocalDate from = nullableDate(output.get("lostDateFrom"));
		LocalDate to = nullableDate(output.get("lostDateTo"));
		String place = nullableString(output.get("lostPlaceText"), MAX_PLACE_LENGTH);
		String productNameKeyword = nullableNonBlankString(
				output.get("productNameKeyword"), MAX_SEARCH_KEYWORD_LENGTH);
		String storagePlaceKeyword = nullableNonBlankString(
				output.get("storagePlaceKeyword"), MAX_SEARCH_KEYWORD_LENGTH);
		String colorName = includesColorName(mode) ? displayColorName(output.get("colorName")) : null;
		JsonNode assistantNode = output.get("assistantMessage");
		if (assistantNode == null || !assistantNode.isTextual()) {
			throw unavailable();
		}
		String assistantMessage = assistantNode.textValue();
		if (assistantMessage.isBlank()
				|| assistantMessage.codePointCount(0, assistantMessage.length()) > MAX_ASSISTANT_LENGTH) {
			throw unavailable();
		}
		if (from != null && to != null && from.isAfter(to)) {
			throw unavailable();
		}
		return new AiSearchConditionExtractionResult(
				from, to, place, productNameKeyword, storagePlaceKeyword, colorName, assistantMessage);
	}

	private LocalDate nullableDate(JsonNode node) {
		if (node == null) {
			throw unavailable();
		}
		if (node.isNull()) {
			return null;
		}
		if (!node.isTextual()) {
			throw unavailable();
		}
		return LocalDate.parse(node.textValue());
	}

	private String nullableString(JsonNode node, int maxLength) {
		if (node == null) {
			throw unavailable();
		}
		if (node.isNull()) {
			return null;
		}
		if (!node.isTextual() || node.textValue().codePointCount(0, node.textValue().length()) > maxLength) {
			throw unavailable();
		}
		return node.textValue();
	}

	/**
	 * API-05 표시용 색상명은 검색조건이 아닌 선택값이다. 필드 누락·JSON 타입 오류는 다른 필드처럼 실패시키지만,
	 * 문자열 내용을 쓸 수 없으면(앞뒤 공백 제거 후 빈 값·100자 초과) 검색 전체를 실패시키지 않고 null로 둔다.
	 */
	private String displayColorName(JsonNode node) {
		String value = nullableString(node, Integer.MAX_VALUE);
		if (value == null) {
			return null;
		}
		String stripped = value.strip();
		if (stripped.isEmpty() || stripped.codePointCount(0, stripped.length()) > MAX_COLOR_NAME_LENGTH) {
			return null;
		}
		return stripped;
	}

	private String nullableNonBlankString(JsonNode node, int maxLength) {
		String value = nullableString(node, maxLength);
		if (value != null && value.isBlank()) {
			throw unavailable();
		}
		return value;
	}

	private boolean hasTimeoutCause(Throwable error) {
		for (Throwable current = error; current != null; current = current.getCause()) {
			if (current instanceof HttpTimeoutException || current instanceof SocketTimeoutException) {
				return true;
			}
		}
		return false;
	}

	private BusinessException unavailable() {
		return new BusinessException(SearchErrorCode.AI_CONDITION_UNAVAILABLE);
	}
}
