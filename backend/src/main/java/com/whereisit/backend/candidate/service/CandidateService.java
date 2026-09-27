package com.whereisit.backend.candidate.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.whereisit.backend.candidate.dto.CandidatePageResponse;
import com.whereisit.backend.candidate.dto.CandidateResponse;
import com.whereisit.backend.candidate.entity.LostItemCandidate;
import com.whereisit.backend.candidate.repository.LostItemCandidateRepository;
import com.whereisit.backend.global.error.BusinessException;
import com.whereisit.backend.global.error.CommonErrorCode;
import com.whereisit.backend.lostitem.service.LostItemService;

import lombok.RequiredArgsConstructor;

/** API-12·13. 추천 후보 목록·상세 조회. */
@Service
@RequiredArgsConstructor
public class CandidateService {

	private final LostItemService lostItemService;
	private final LostItemCandidateRepository candidateRepository;

	@Transactional(readOnly = true)
	public CandidatePageResponse list(Long memberId, Long lostItemId, String scope, Pageable pageable) {
		lostItemService.findOwned(memberId, lostItemId);

		Page<LostItemCandidate> page = "ALL".equalsIgnoreCase(scope)
				? candidateRepository.findByLostItemIdOrderByLastSeenAtDescIdDesc(lostItemId, pageable)
				: candidateRepository.findCurrentByLostItemId(lostItemId, pageable);
		return CandidatePageResponse.from(page);
	}

	@Transactional(readOnly = true)
	public CandidateResponse getDetail(Long memberId, Long lostItemId, Long candidateId) {
		lostItemService.findOwned(memberId, lostItemId);
		LostItemCandidate candidate = candidateRepository.findByIdAndLostItemId(candidateId, lostItemId)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
		return CandidateResponse.from(candidate);
	}
}
