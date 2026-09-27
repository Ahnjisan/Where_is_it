package com.whereisit.backend.search.ai.port;

public interface AiSearchConditionExtractor {

	AiSearchConditionExtractionResult extract(AiSearchConditionExtractionRequest request);
}
