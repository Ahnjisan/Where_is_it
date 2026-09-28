package com.whereisit.backend.search.dto;

import java.util.List;

import com.whereisit.backend.candidate.dto.CandidateResponse;
import com.whereisit.backend.lostitem.dto.ChatMessageResponse;
import com.whereisit.backend.lostitem.dto.LostItemResponse;

/** API-05 통합 검색 응답. Issue #67의 평면 응답 계약을 유지한다. */
public record SearchExecutionResponse(
		LostItemResponse lostItem,
		ChatMessageResponse assistantMessage,
		String lookupStatus,
		String rankingStatus,
		boolean persisted,
		List<String> warnings,
		List<CandidateResponse> candidates,
		List<String> clarificationQuestions) {

	public SearchExecutionResponse {
		warnings = List.copyOf(warnings);
		candidates = List.copyOf(candidates);
		clarificationQuestions = List.copyOf(clarificationQuestions);
	}
}
