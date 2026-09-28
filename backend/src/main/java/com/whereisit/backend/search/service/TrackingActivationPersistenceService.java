package com.whereisit.backend.search.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.whereisit.backend.candidate.entity.LostItemCandidate;
import com.whereisit.backend.candidate.repository.LostItemCandidateRepository;
import com.whereisit.backend.global.error.BusinessException;
import com.whereisit.backend.lostitem.dto.LostItemResponse;
import com.whereisit.backend.lostitem.entity.LostItem;
import com.whereisit.backend.lostitem.entity.LostItemStatus;
import com.whereisit.backend.lostitem.error.LostItemErrorCode;
import com.whereisit.backend.lostitem.service.LostItemService;

import java.time.Clock;
import java.time.LocalDateTime;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TrackingActivationPersistenceService {

	private final LostItemService lostItemService;
	private final LostItemCandidateRepository candidateRepository;
	private final Clock clock;

	@Transactional(readOnly = true)
	public ActivationPreparation prepare(Long memberId, Long lostItemId, String notificationEmailOverride) {
		LostItem lostItem = lostItemService.findOwned(memberId, lostItemId);
		if (lostItem.getStatus() == LostItemStatus.TRACKING) {
			return new ActivationPreparation(true, null, LostItemResponse.from(lostItem, clock));
		}
		lostItemService.ensureNotExpired(lostItem);
		if (lostItem.getSearchStartDate() == null) {
			throw new BusinessException(LostItemErrorCode.INVALID_SEARCH_CONDITION);
		}
		String notificationEmail = notificationEmailOverride != null
				? notificationEmailOverride
				: lostItem.getMember().getEmail();
		return new ActivationPreparation(false, notificationEmail, null);
	}

	@Transactional
	public LostItemResponse activateAfterBaseline(Long memberId, Long lostItemId, String notificationEmail) {
		LostItem lostItem = lostItemService.findOwned(memberId, lostItemId);
		if (lostItem.getStatus() == LostItemStatus.TRACKING) {
			return LostItemResponse.from(lostItem, clock);
		}
		lostItemService.ensureNotExpired(lostItem);
		if (lostItem.getSearchStartDate() == null) {
			throw new BusinessException(LostItemErrorCode.INVALID_SEARCH_CONDITION);
		}
		for (LostItemCandidate candidate : candidateRepository.findByLostItemIdAndCurrentTrue(lostItemId)) {
			candidate.markAsBaseline();
		}
		lostItem.activateTracking(LocalDateTime.now(clock), notificationEmail);
		return LostItemResponse.from(lostItem, clock);
	}

	public record ActivationPreparation(
			boolean alreadyTracking, String notificationEmail, LostItemResponse existingResponse) {
	}
}
