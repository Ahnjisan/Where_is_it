package com.whereisit.backend.candidate.entity;

import java.time.LocalDateTime;

import com.whereisit.backend.founditem.entity.FoundItem;
import com.whereisit.backend.lostitem.entity.LostItem;
import com.whereisit.backend.notification.entity.EmailNotification;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 분실물_후보. 테이블 명세 v2 10_분실물후보(lost_item_candidates).
 */
@Getter
@Entity
@Table(name = "lost_item_candidates",
		uniqueConstraints = @UniqueConstraint(name = "uk_lost_item_candidates", columnNames = { "lost_item_id", "found_item_id" }))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LostItemCandidate {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "candidate_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "lost_item_id", nullable = false, updatable = false)
	private LostItem lostItem;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "found_item_id", nullable = false, updatable = false)
	private FoundItem foundItem;

	@Column(name = "rank_no")
	private Integer rankNo;

	@Column(name = "recommendation_reason", columnDefinition = "TEXT")
	private String recommendationReason;

	@Column(name = "is_similar")
	private Boolean isSimilar;

	@Column(name = "is_current", nullable = false)
	private boolean current;

	@Column(name = "is_baseline", nullable = false)
	private boolean baseline;

	@Column(name = "first_seen_at", nullable = false, updatable = false)
	private LocalDateTime firstSeenAt;

	@Column(name = "last_seen_at", nullable = false)
	private LocalDateTime lastSeenAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "notification_id")
	private EmailNotification notification;

	private LostItemCandidate(LostItem lostItem, FoundItem foundItem, LocalDateTime now) {
		this.lostItem = lostItem;
		this.foundItem = foundItem;
		this.current = false;
		this.baseline = false;
		this.firstSeenAt = now;
		this.lastSeenAt = now;
	}

	public static LostItemCandidate create(LostItem lostItem, FoundItem foundItem, LocalDateTime now) {
		return new LostItemCandidate(lostItem, foundItem, now);
	}

	/** 완전히 성공한 검색 결과에 이 후보가 포함됐을 때 부른다(현재_결과_여부=1). */
	public void markSeenInCurrentResult(Integer rankNo, String recommendationReason, boolean isSimilar, LocalDateTime now) {
		this.rankNo = rankNo;
		this.recommendationReason = recommendationReason;
		this.isSimilar = isSimilar;
		this.current = true;
		this.lastSeenAt = now;
	}

	/** 최근 완전 성공 검색에 더는 포함되지 않는 기존 후보에 부른다. 기록은 남기고 현재_결과_여부만 0으로 내린다. */
	public void markNotInCurrentResult() {
		this.current = false;
	}

	public void markAsBaseline() {
		this.baseline = true;
	}

	/** B06 묶음 메일. 이 후보를 오늘 보낸 메일 묶음에 연결한다(Issue #37). */
	public void linkNotification(EmailNotification notification) {
		this.notification = notification;
	}
}
