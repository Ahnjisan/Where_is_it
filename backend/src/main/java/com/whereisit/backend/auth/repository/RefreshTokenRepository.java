package com.whereisit.backend.auth.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.whereisit.backend.auth.entity.RefreshToken;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

	/** 해당 회원의 만료된 RT만 지운다. 다른 회원의 RT는 건드리지 않는다(TC-31). */
	@Modifying(flushAutomatically = true)
	@Query("delete from RefreshToken t where t.member.id = :memberId and t.expiresAt <= :now")
	int deleteExpiredByMemberId(@Param("memberId") Long memberId, @Param("now") LocalDateTime now);

	long countByMemberId(Long memberId);
}
