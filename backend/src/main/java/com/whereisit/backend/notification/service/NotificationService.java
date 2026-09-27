package com.whereisit.backend.notification.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.whereisit.backend.candidate.repository.LostItemCandidateRepository;
import com.whereisit.backend.lostitem.service.LostItemService;
import com.whereisit.backend.notification.dto.NotificationPageResponse;
import com.whereisit.backend.notification.dto.NotificationResponse;
import com.whereisit.backend.notification.entity.EmailNotification;
import com.whereisit.backend.notification.repository.EmailNotificationRepository;

import lombok.RequiredArgsConstructor;

/** API-15 내 신규 후보 이메일 이력. */
@Service
@RequiredArgsConstructor
public class NotificationService {

	private final LostItemService lostItemService;
	private final EmailNotificationRepository notificationRepository;
	private final LostItemCandidateRepository candidateRepository;

	@Transactional(readOnly = true)
	public NotificationPageResponse list(Long memberId, Long lostItemId, Pageable pageable) {
		lostItemService.findOwned(memberId, lostItemId);

		Page<EmailNotification> page = notificationRepository.findByLostItemIdOrderByCreatedAtDescIdDesc(lostItemId, pageable);
		var items = page.getContent().stream()
				.map(notification -> NotificationResponse.from(notification, candidateRepository.findByNotificationId(notification.getId())))
				.toList();
		return new NotificationPageResponse(items, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
	}
}
