package com.whereisit.backend.lostitem.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.whereisit.backend.lostitem.entity.LostItem;
import com.whereisit.backend.lostitem.entity.LostItemStatus;

import jakarta.persistence.LockModeType;

public interface LostItemRepository extends JpaRepository<LostItem, Long> {

	Optional<LostItem> findByIdAndMemberIdAndDeletedAtIsNull(Long id, Long memberId);

	/** API-05 snapshot 비교와 FoundItem 원본 캐시 반영을 같은 짧은 transaction에서 직렬화한다. */
	@Lock(LockModeType.PESSIMISTIC_WRITE)
	@Query("select l from LostItem l where l.id = :lostItemId and l.member.id = :memberId and l.deletedAt is null")
	Optional<LostItem> findOwnedActiveByIdForUpdate(
			@Param("lostItemId") Long lostItemId, @Param("memberId") Long memberId);

	/** 삭제 여부와 무관하게 찾는다. API-09는 반복 삭제도 204(멱등)이어야 해서 이미 지워졌어도 찾아야 한다. */
	Optional<LostItem> findByIdAndMemberId(Long id, Long memberId);

	Page<LostItem> findByMemberIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(Long memberId, Pageable pageable);

	Page<LostItem> findByMemberIdAndDeletedAtIsNullAndStatusOrderByCreatedAtDescIdDesc(
			Long memberId, LostItemStatus status, Pageable pageable);
}
