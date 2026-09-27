package com.whereisit.backend.auth.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.whereisit.backend.auth.entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	/**
	 * 해당 회원의 만료된 RT의 ID. 다른 회원의 RT는 포함하지 않는다(TC-31).
	 *
	 * 범위 조건으로 바로 DELETE하지 않고 먼저 ID만 읽는다. 범위 DELETE는 인덱스에 갭 락을 걸어서,
	 * 같은 회원이 동시에 로그인하면 이어지는 INSERT끼리 데드락이 난다(Issue #48). 일반 SELECT는 잠그지 않는다.
	 * 만료 기준은 JWT 검증(jjwt)과 같다. 만료 시각이 지난 것(expiresAt < now)만 만료이고, 만료 시각 그 자체는 아직 유효하다.
	 */
	@Query("select t.id from RefreshToken t where t.member.id = :memberId and t.expiresAt < :now")
	List<Long> findExpiredIdsByMemberId(@Param("memberId") Long memberId, @Param("now") LocalDateTime now);

	/** 해시에 해당하는 RT의 ID. 잠그지 않는 일반 조회라서 없는 해시여도 인덱스에 잠금을 남기지 않는다. */
	@Query("select t.id from RefreshToken t where t.tokenHash = :tokenHash")
	Optional<Long> findIdByTokenHash(@Param("tokenHash") String tokenHash);

	/**
	 * PK로 한 행을 지우고 실제로 지운 행 수를 돌려준다. 다른 트랜잭션이 먼저 지웠으면 0이다.
	 * refresh는 이 값으로 같은 RT의 동시 사용 중 하나만 통과시킨다(TC-35).
	 *
	 * JPQL 삭제는 영속성 컨텍스트를 거치지 않으므로, 지운 행의 엔티티가 남아 있으면 같은 ID로 새 행을 저장할 때
	 * 식별자 충돌이 난다(SQLite는 지운 최대 ID를 재사용한다). 그래서 삭제 뒤 컨텍스트를 비운다.
	 *
	 * 주의: 이 RT 행만이 아니라 트랜잭션의 영속성 컨텍스트 전체를 비운다. 호출 전 변경은 먼저 저장(flush)되지만,
	 * 호출 전에 읽은 엔티티는 모두 준영속이 되어 이후 지연 로딩에서 LazyInitializationException이 날 수 있다.
	 * 호출 전에는 엔티티를 읽지 말고, 필요한 엔티티는 호출 뒤에 다시 조회한다(refresh가 회원을 삭제 뒤에 조회하는 이유).
	 */
	@Modifying(flushAutomatically = true, clearAutomatically = true)
	@Query("delete from RefreshToken t where t.id = :id")
	int deleteByIdReturningCount(@Param("id") Long id);

	long countByMemberId(Long memberId);
}
