package com.whereisit.backend.lostitem.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.whereisit.backend.global.entity.BaseTimeEntity;
import com.whereisit.backend.member.entity.LanguageCode;
import com.whereisit.backend.member.entity.LanguageCodeConverter;
import com.whereisit.backend.member.entity.Member;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 분실물 검색·추적. 테이블 명세 v2 06_분실물검색추적(lost_items).
 * 상태별 컬럼 조합(chk_lost_items_tracking_period)과 분실 기간(chk_lost_items_lost_date_range)은
 * DB CHECK 대신 이 클래스와 서비스 레이어가 함께 지킨다.
 */
@Getter
@Entity
@Table(name = "lost_items")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LostItem extends BaseTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "lost_item_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "member_id", nullable = false)
	private Member member;

	@Column(name = "description", nullable = false, columnDefinition = "TEXT")
	private String description;

	@Convert(converter = LanguageCodeConverter.class)
	@Column(name = "language_code", nullable = false, length = 35)
	private LanguageCode languageCode;

	@Column(name = "category_large_code", length = 6)
	private String categoryLargeCode;

	@Column(name = "category_middle_code", length = 6)
	private String categoryMiddleCode;

	@Column(name = "color_code", length = 8)
	private String colorCode;

	@Column(name = "region_code", length = 6)
	private String regionCode;

	@Column(name = "lost_date_from")
	private LocalDate lostDateFrom;

	@Column(name = "lost_date_to")
	private LocalDate lostDateTo;

	@Column(name = "lost_place_text", length = 255)
	private String lostPlaceText;

	@Column(name = "search_start_date")
	private LocalDate searchStartDate;

	/** AI가 구조화한 포털기관 물품명 검색어. 추적 재검색이 같은 검색어로 후보를 매칭한다. */
	@Column(name = "product_name_keyword", length = 200)
	private String productNameKeyword;

	/** AI가 구조화한 포털기관 보관장소 검색어. 분실 장소(lost_place_text)와 다르다. */
	@Column(name = "storage_place_keyword", length = 200)
	private String storagePlaceKeyword;

	@Column(name = "notification_email", length = 254)
	private String notificationEmail;

	@Enumerated(EnumType.STRING)
	@Column(name = "status", nullable = false, length = 16)
	private LostItemStatus status;

	@Column(name = "started_at")
	private LocalDateTime startedAt;

	@Column(name = "expires_at")
	private LocalDateTime expiresAt;

	@Column(name = "last_auto_search_date")
	private LocalDate lastAutoSearchDate;

	@Column(name = "deleted_at")
	private LocalDateTime deletedAt;

	private LostItem(Member member, String description, LanguageCode languageCode) {
		this.member = member;
		this.description = description;
		this.languageCode = languageCode;
		this.status = LostItemStatus.SEARCHING;
	}

	/** API-05. 새 검색 건은 항상 SEARCHING으로 시작하고 추적 관련 컬럼은 비워 둔다. */
	public static LostItem create(Member member, String description, LanguageCode languageCode) {
		return new LostItem(member, description, languageCode);
	}

	public boolean isOwnedBy(Long memberId) {
		return member.getId().equals(memberId);
	}

	public boolean isDeleted() {
		return deletedAt != null;
	}

	/** API-08. 전송된 필드만 바꾸고, EXPIRED 건은 호출 전에 서비스가 막는다. */
	public void updateDescription(String description) {
		this.description = description;
	}

	public void updateLanguageCode(LanguageCode languageCode) {
		this.languageCode = languageCode;
	}

	public void updateConditions(String categoryLargeCode, String categoryMiddleCode, String colorCode,
			String regionCode, LocalDate lostDateFrom, LocalDate lostDateTo, String lostPlaceText,
			LocalDate searchStartDate) {
		this.categoryLargeCode = categoryLargeCode;
		this.categoryMiddleCode = categoryMiddleCode;
		this.colorCode = colorCode;
		this.regionCode = regionCode;
		this.lostDateFrom = lostDateFrom;
		this.lostDateTo = lostDateTo;
		this.lostPlaceText = lostPlaceText;
		this.searchStartDate = searchStartDate;
	}

	/** AI 조건 구조화가 변경할 수 있는 날짜·장소 필드만 갱신한다. */
	public void updateAiSearchConditions(LocalDate lostDateFrom, LocalDate lostDateTo, String lostPlaceText) {
		if (lostDateFrom != null && lostDateTo != null && lostDateFrom.isAfter(lostDateTo)) {
			throw new IllegalArgumentException("Lost date range is reversed.");
		}
		if (lostPlaceText != null && lostPlaceText.length() > 255) {
			throw new IllegalArgumentException("Lost place is too long.");
		}
		this.lostDateFrom = lostDateFrom;
		this.lostDateTo = lostDateTo;
		this.lostPlaceText = lostPlaceText;
	}

	/**
	 * AI가 새로 구조화한 검색어만 반영한다. 후속 대화에서 AI가 검색어를 돌려주지 않으면(null)
	 * 이전 검색어를 유지해, 추적 재검색이 쓰는 조건이 의도치 않게 지워지지 않게 한다.
	 */
	public void updateSearchKeywords(String productNameKeyword, String storagePlaceKeyword) {
		if (productNameKeyword != null) {
			this.productNameKeyword = productNameKeyword;
		}
		if (storagePlaceKeyword != null) {
			this.storagePlaceKeyword = storagePlaceKeyword;
		}
	}

	/** 추적 활성화 시 습득물_조회_시작일이 비어 있으면 정해진 기본값으로 채운다. */
	public void fillSearchStartDateIfAbsent(LocalDate searchStartDate) {
		if (this.searchStartDate == null) {
			this.searchStartDate = searchStartDate;
		}
	}

	public void updateNotificationEmail(String notificationEmail) {
		this.notificationEmail = notificationEmail;
	}

	public void delete(LocalDateTime deletedAt) {
		this.deletedAt = deletedAt;
	}

	/**
	 * API-17 7일 추적 활성화(Issue #36에서 사용). SEARCHING에서만 부르며, 시작 시각+7일을 만료로 고정한다.
	 */
	public void activateTracking(LocalDateTime startedAt, String notificationEmail) {
		this.status = LostItemStatus.TRACKING;
		this.startedAt = startedAt;
		this.expiresAt = startedAt.plusDays(7);
		this.notificationEmail = notificationEmail;
	}

	public void markAutoSearchCompleted(LocalDate date) {
		this.lastAutoSearchDate = date;
	}

	public void expire() {
		this.status = LostItemStatus.EXPIRED;
	}
}
