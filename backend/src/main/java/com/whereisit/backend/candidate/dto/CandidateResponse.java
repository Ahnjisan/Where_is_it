package com.whereisit.backend.candidate.dto;

import java.time.LocalDateTime;

import com.whereisit.backend.candidate.entity.LostItemCandidate;
import com.whereisit.backend.candidate.ranking.RankedCandidate;
import com.whereisit.backend.founditem.entity.FoundItem;

/**
 * API 명세 v2 05_응답필드 Candidate. notificationId는 이메일_알림 테이블이 생기는
 * Issue #37 전까지 항상 null이다.
 */
public record CandidateResponse(
		String candidateId,
		FoundItemResponse foundItem,
		Integer rank,
		Boolean isSimilar,
		String reason,
		boolean isCurrent,
		boolean isBaseline,
		String notificationId,
		LocalDateTime firstSeenAt,
		LocalDateTime lastSeenAt) {

	public static CandidateResponse from(LostItemCandidate candidate) {
		return from(candidate, candidate.getFoundItem());
	}

	/**
	 * API-07 상세조회용. detail 조회 직후에는 candidate.getFoundItem()이 이전 트랜잭션에서
	 * 읽은 오래된(상세 반영 전) 인스턴스일 수 있어, 방금 갱신한 FoundItem을 따로 받아 조립한다.
	 */
	public static CandidateResponse from(LostItemCandidate candidate, FoundItem foundItem) {
		return new CandidateResponse(
				String.valueOf(candidate.getId()),
				FoundItemResponse.from(foundItem),
				candidate.getRankNo(),
				candidate.getIsSimilar(),
				candidate.getRecommendationReason(),
				candidate.isCurrent(),
				candidate.isBaseline(),
				null,
				candidate.getFirstSeenAt(),
				candidate.getLastSeenAt());
	}

	/** API-05처럼 Candidate 관계를 저장하지 않은 Frontend 호환용 일시 결과. candidateId는 null이다. */
	public static CandidateResponse transientOf(FoundItem foundItem, RankedCandidate ranked) {
		return new CandidateResponse(
				null,
				FoundItemResponse.from(foundItem),
				ranked.rank(),
				ranked.similar(),
				ranked.reason(),
				false,
				false,
				null,
				null,
				null);
	}
}
