package com.whereisit.backend.lostitem.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import com.whereisit.backend.lostitem.entity.LostItem;
import com.whereisit.backend.lostitem.entity.LostItemStatus;

public interface LostItemRepository extends JpaRepository<LostItem, Long> {

	Optional<LostItem> findByIdAndMemberIdAndDeletedAtIsNull(Long id, Long memberId);

	/** 삭제 여부와 무관하게 찾는다. API-09는 반복 삭제도 204(멱등)이어야 해서 이미 지워졌어도 찾아야 한다. */
	Optional<LostItem> findByIdAndMemberId(Long id, Long memberId);

	Page<LostItem> findByMemberIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(Long memberId, Pageable pageable);

	Page<LostItem> findByMemberIdAndDeletedAtIsNullAndStatusOrderByCreatedAtDescIdDesc(
			Long memberId, LostItemStatus status, Pageable pageable);

	/** B01 일일 배치 대상. 만료·삭제 재확인은 배치가 각 건을 처리하는 시점에 다시 한다. */
	List<LostItem> findByStatusAndDeletedAtIsNull(LostItemStatus status);
}
