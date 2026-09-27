package com.whereisit.backend.candidate.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.whereisit.backend.candidate.entity.LostItemCandidate;

public interface LostItemCandidateRepository extends JpaRepository<LostItemCandidate, Long> {

	Optional<LostItemCandidate> findByLostItemIdAndFoundItemId(Long lostItemId, Long foundItemId);

	Optional<LostItemCandidate> findByIdAndLostItemId(Long id, Long lostItemId);

	List<LostItemCandidate> findByLostItemIdAndCurrentTrue(Long lostItemId);

	List<LostItemCandidate> findByNotificationId(Long notificationId);

	/** scope=CURRENT. API-12 규칙 3: 순위 ASC(null 마지막), 후보ID ASC. */
	@Query("select c from LostItemCandidate c where c.lostItem.id = :lostItemId and c.current = true "
			+ "order by case when c.rankNo is null then 1 else 0 end, c.rankNo asc, c.id asc")
	Page<LostItemCandidate> findCurrentByLostItemId(@Param("lostItemId") Long lostItemId, Pageable pageable);

	/** scope=ALL. API-12 규칙 4: 최근확인 DESC, ID DESC. */
	Page<LostItemCandidate> findByLostItemIdOrderByLastSeenAtDescIdDesc(Long lostItemId, Pageable pageable);

	/** B05 신규 후보. 유사기준 통과·추적전 확인후보 아님·아직 어떤 메일에도 안 묶인 현재 후보만 고른다. */
	@Query("select c from LostItemCandidate c where c.lostItem.id = :lostItemId and c.current = true "
			+ "and c.isSimilar = true and c.baseline = false and c.notification is null")
	List<LostItemCandidate> findNewSimilarCandidates(@Param("lostItemId") Long lostItemId);
}
