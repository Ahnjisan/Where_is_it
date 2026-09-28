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

	long countByLostItemIdAndCurrentTrue(Long lostItemId);

	/** scope=CURRENT. API-12 규칙 3: 순위 ASC(null 마지막), 후보ID ASC. */
	@Query("select c from LostItemCandidate c where c.lostItem.id = :lostItemId and c.current = true "
			+ "order by case when c.rankNo is null then 1 else 0 end, c.rankNo asc, c.id asc")
	Page<LostItemCandidate> findCurrentByLostItemId(@Param("lostItemId") Long lostItemId, Pageable pageable);

	/**
	 * API-07 상세조회용. 순위 1위 후보 하나만 필요하므로 Page가 아닌 List로 받아 count 쿼리를 만들지
	 * 않는다. foundItem을 join fetch해서, open-in-view=false 환경에서 트랜잭션이 끝난 뒤 응답을
	 * 만들 때도(외부 상세 API 호출이 트랜잭션 밖에서 끼어듦) 지연 로딩 예외 없이 읽을 수 있게 한다.
	 */
	@Query("select c from LostItemCandidate c join fetch c.foundItem where c.lostItem.id = :lostItemId and c.current = true "
			+ "order by case when c.rankNo is null then 1 else 0 end, c.rankNo asc, c.id asc")
	List<LostItemCandidate> findCurrentOrderedByLostItemId(@Param("lostItemId") Long lostItemId, Pageable pageable);

	/** scope=ALL. API-12 규칙 4: 최근확인 DESC, ID DESC. */
	Page<LostItemCandidate> findByLostItemIdOrderByLastSeenAtDescIdDesc(Long lostItemId, Pageable pageable);
}
