package com.whereisit.backend.search.dto;

import java.util.List;

import com.whereisit.backend.candidate.dto.CandidateResponse;
import com.whereisit.backend.lostitem.dto.ChatMessageResponse;
import com.whereisit.backend.lostitem.dto.LostItemResponse;

/**
 * API 명세 v2 05_응답필드 SearchData를 단순화한 응답.
 * lookup을 LookupSummary 전체(소스별 SourceStatus 배열 등) 대신 lookupStatus·warnings로 줄였다(PR 설명 참고).
 */
public record SearchExecutionResponse(
		LostItemResponse lostItem,
		ChatMessageResponse assistantMessage,
		String lookupStatus,
		List<String> warnings,
		List<CandidateResponse> candidates) {
}
