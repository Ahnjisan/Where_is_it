package com.whereisit.backend.auth.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
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

	long countByMemberId(Long memberId);
}
