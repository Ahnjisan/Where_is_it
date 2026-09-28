package com.whereisit.backend.notification.repository;

import java.time.LocalDate;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.whereisit.backend.notification.entity.EmailNotification;

public interface EmailNotificationRepository extends JpaRepository<EmailNotification, Long> {

	Optional<EmailNotification> findByLostItemIdAndNotificationDate(Long lostItemId, LocalDate notificationDate);

	/** API-15. 최근 생성순. */
	Page<EmailNotification> findByLostItemIdOrderByCreatedAtDescIdDesc(Long lostItemId, Pageable pageable);
}
