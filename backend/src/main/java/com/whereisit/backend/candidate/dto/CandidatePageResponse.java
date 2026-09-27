package com.whereisit.backend.candidate.dto;

import java.util.List;

import org.springframework.data.domain.Page;

import com.whereisit.backend.candidate.entity.LostItemCandidate;

/** API 명세 v2 05_응답필드 CandidatePage. */
public record CandidatePageResponse(
		List<CandidateResponse> items, int page, int size, long totalElements, int totalPages) {

	public static CandidatePageResponse from(Page<LostItemCandidate> page) {
		List<CandidateResponse> items = page.getContent().stream().map(CandidateResponse::from).toList();
		return new CandidatePageResponse(items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
}
