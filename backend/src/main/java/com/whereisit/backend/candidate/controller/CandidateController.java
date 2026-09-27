package com.whereisit.backend.candidate.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.whereisit.backend.candidate.dto.CandidatePageResponse;
import com.whereisit.backend.candidate.dto.CandidateResponse;
import com.whereisit.backend.candidate.service.CandidateService;
import com.whereisit.backend.global.response.ApiResponse;

import lombok.RequiredArgsConstructor;

/**
 * API-12·13. notificationId 필터는 이메일_알림 테이블이 생기는 Issue #37에서 추가한다.
 */
@RestController
@RequestMapping("/api/lost-items/{lostItemId}/candidates")
@RequiredArgsConstructor
public class CandidateController {

	private final CandidateService candidateService;

	@GetMapping
	public ApiResponse<CandidatePageResponse> list(@AuthenticationPrincipal Long memberId, @PathVariable Long lostItemId,
			@RequestParam(defaultValue = "CURRENT") String scope, Pageable pageable) {
		return ApiResponse.ok(candidateService.list(memberId, lostItemId, scope, pageable));
	}

	@GetMapping("/{candidateId}")
	public ApiResponse<CandidateResponse> getDetail(@AuthenticationPrincipal Long memberId,
			@PathVariable Long lostItemId, @PathVariable Long candidateId) {
		return ApiResponse.ok(candidateService.getDetail(memberId, lostItemId, candidateId));
	}
}
