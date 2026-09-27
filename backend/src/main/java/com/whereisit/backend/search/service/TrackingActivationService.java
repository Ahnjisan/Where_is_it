package com.whereisit.backend.search.service;

import java.time.Clock;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.whereisit.backend.candidate.entity.LostItemCandidate;
import com.whereisit.backend.candidate.repository.LostItemCandidateRepository;
import com.whereisit.backend.global.error.BusinessException;
import com.whereisit.backend.global.error.CommonErrorCode;
import com.whereisit.backend.lostitem.dto.LostItemResponse;
import com.whereisit.backend.lostitem.entity.LostItem;
import com.whereisit.backend.lostitem.entity.LostItemStatus;
import com.whereisit.backend.lostitem.error.LostItemErrorCode;
import com.whereisit.backend.lostitem.service.LostItemService;
import com.whereisit.backend.member.entity.Member;
import com.whereisit.backend.member.repository.MemberRepository;
import com.whereisit.backend.search.dto.RunSearchRequest;
import com.whereisit.backend.search.dto.SearchExecutionResponse;
import com.whereisit.backend.search.dto.SearchMode;
import com.whereisit.backend.search.error.SearchErrorCode;

import lombok.RequiredArgsConstructor;

/**
 * API-14 7일 추적 활성화. 등록 전 재검색으로 기준 후보(baseline)를 확정한 뒤에만 TRACKING으로 바꾼다.
 */
@Service
@RequiredArgsConstructor
public class TrackingActivationService {

	private final LostItemService lostItemService;
	private final SearchExecutionService searchExecutionService;
	private final LostItemCandidateRepository candidateRepository;
	private final MemberRepository memberRepository;
	private final Clock clock;

	@Transactional
	public LostItemResponse activate(Long memberId, Long lostItemId, String notificationEmailOverride) {
		LostItem lostItem = lostItemService.findOwned(memberId, lostItemId);

		// TRACKING 반복 호출은 기존 정보 그대로 200(API-14 규칙 4).
		if (lostItem.getStatus() == LostItemStatus.TRACKING) {
			return LostItemResponse.from(lostItem, clock);
		}
		lostItemService.ensureNotExpired(lostItem);
		if (lostItem.getSearchStartDate() == null) {
			throw new BusinessException(LostItemErrorCode.INVALID_SEARCH_CONDITION);
		}

		String notificationEmail = notificationEmailOverride != null ? notificationEmailOverride : memberEmail(memberId);

		SearchExecutionResponse baseline = searchExecutionService.execute(
				memberId, lostItemId, new RunSearchRequest(SearchMode.INITIAL, null, null));
		if (!"COMPLETE".equals(baseline.lookupStatus())) {
			throw new BusinessException(SearchErrorCode.TRACKING_BASELINE_UNAVAILABLE);
		}

		for (LostItemCandidate candidate : candidateRepository.findByLostItemIdAndCurrentTrue(lostItemId)) {
			candidate.markAsBaseline();
		}

		lostItem.activateTracking(LocalDateTime.now(clock), notificationEmail);
		return LostItemResponse.from(lostItem, clock);
	}

	private String memberEmail(Long memberId) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
		return member.getEmail();
	}
}
