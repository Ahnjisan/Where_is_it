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
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractionRequest;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractionResult;
import com.whereisit.backend.search.ai.port.AiSearchConditionExtractor;
import com.whereisit.backend.search.error.SearchErrorCode;

@Component
public class OpenAiSearchConditionExtractor implements AiSearchConditionExtractor {

	private static final int MAX_PLACE_LENGTH = 255;
	private static final int MAX_SEARCH_KEYWORD_LENGTH = 200;
	private static final int MAX_ASSISTANT_LENGTH = 2000;
	private static final Set<String> OUTPUT_FIELDS = Set.of(
			"lostDateFrom", "lostDateTo", "lostPlaceText", "productNameKeyword",
			"storagePlaceKeyword", "assistantMessage");
	private static final String INSTRUCTIONS = """
			Extract the lost date range, the place where the user lost the item, and short search keywords.
			productNameKeyword is the item-name query for the portal found-item list API.
			storagePlaceKeyword is the custody/storage-place query for that API; it is not the lost place.
			Preserve an existing date or place when the user does not change it. Use null only when it is unknown or cleared.
			Write assistantMessage in the requested language and keep it concise.
			Never create or return common codes, identifiers, status, tracking data, or email data.
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
			return parseResponse(rawResponse);
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

		Map<String, Object> schema = new LinkedHashMap<>();
		schema.put("type", "object");
		schema.put("properties", propertiesSchema);
		schema.put("required", List.of("lostDateFrom", "lostDateTo", "lostPlaceText",
				"productNameKeyword", "storagePlaceKeyword", "assistantMessage"));
		schema.put("additionalProperties", false);

		Map<String, Object> format = new LinkedHashMap<>();
		format.put("type", "json_schema");
		format.put("name", "lost_item_search_conditions");
		format.put("strict", true);
		format.put("schema", schema);

		Map<String, Object> body = new LinkedHashMap<>();
		body.put("model", properties.model());
		body.put("store", false);
		body.put("instructions", INSTRUCTIONS);
		body.put("input", inputText(request));
		body.put("text", Map.of("format", format));
		return body;
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

	private AiSearchConditionExtractionResult parseResponse(String rawResponse) {
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
			return parseStructuredOutput(outputTexts.get(0));
		}
		catch (JsonProcessingException | DateTimeParseException e) {
			throw unavailable();
		}
	}

	private AiSearchConditionExtractionResult parseStructuredOutput(String outputText)
			throws JsonProcessingException {
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
		if (!actualFields.equals(OUTPUT_FIELDS)) {
			throw unavailable();
		}

		LocalDate from = nullableDate(output.get("lostDateFrom"));
		LocalDate to = nullableDate(output.get("lostDateTo"));
		String place = nullableString(output.get("lostPlaceText"), MAX_PLACE_LENGTH);
		String productNameKeyword = nullableNonBlankString(
				output.get("productNameKeyword"), MAX_SEARCH_KEYWORD_LENGTH);
		String storagePlaceKeyword = nullableNonBlankString(
				output.get("storagePlaceKeyword"), MAX_SEARCH_KEYWORD_LENGTH);
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
				from, to, place, productNameKeyword, storagePlaceKeyword, assistantMessage);
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
