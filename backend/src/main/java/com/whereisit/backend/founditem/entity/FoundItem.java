package com.whereisit.backend.founditem.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 습득물. 테이블 명세 v2 08_습득물(found_items).
 * (source_type, atc_id, fd_sn)이 외부 출처의 자연키이며, 같은 조합을 다시 수집하면 새 행을 만들지 않고
 * 표시용 필드와 최근_수집_시각만 갱신한다.
 */
@Getter
@Entity
@Table(name = "found_items",
		uniqueConstraints = @UniqueConstraint(name = "uk_found_items_source", columnNames = { "source_type", "atc_id", "fd_sn" }))
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class FoundItem {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "found_item_id")
	private Long id;

	@Enumerated(EnumType.STRING)
	@Column(name = "source_type", nullable = false, length = 16, updatable = false)
	private FoundItemSourceType sourceType;

	@Column(name = "atc_id", nullable = false, length = 32, updatable = false)
	private String atcId;

	@Column(name = "fd_sn", nullable = false, length = 10, updatable = false)
	private String fdSn;

	@Column(name = "product_name", length = 200)
	private String productName;

	@Column(name = "subject", length = 300)
	private String subject;

	@Column(name = "category_name", length = 100)
	private String categoryName;

	@Column(name = "color_name", length = 100)
	private String colorName;

	@Column(name = "found_date")
	private LocalDate foundDate;

	@Column(name = "storage_place", length = 255)
	private String storagePlace;

	@Column(name = "image_url", length = 2048)
	private String imageUrl;

	@Column(name = "found_place", length = 255)
	private String foundPlace;

	@Column(name = "storage_phone", length = 40)
	private String storagePhone;

	@Column(name = "description", columnDefinition = "TEXT")
	private String description;

	@Column(name = "detail_fetched_at")
	private LocalDateTime detailFetchedAt;

	@Column(name = "first_fetched_at", nullable = false, updatable = false)
	private LocalDateTime firstFetchedAt;

	@Column(name = "last_fetched_at", nullable = false)
	private LocalDateTime lastFetchedAt;

	private FoundItem(FoundItemSourceType sourceType, String atcId, String fdSn, LocalDateTime now) {
		this.sourceType = sourceType;
		this.atcId = atcId;
		this.fdSn = fdSn;
		this.firstFetchedAt = now;
		this.lastFetchedAt = now;
	}

	public static FoundItem create(FoundItemSourceType sourceType, String atcId, String fdSn, LocalDateTime now) {
		return new FoundItem(sourceType, atcId, fdSn, now);
	}

	/** 같은 (출처, 외부 식별자) 습득물을 다시 조회했을 때 표시용 필드를 최신 값으로 덮어쓴다. */
	public void refreshListFields(String productName, String subject, String categoryName, String colorName,
			LocalDate foundDate, String storagePlace, String imageUrl, LocalDateTime now) {
		this.productName = productName;
		this.subject = subject;
		this.categoryName = categoryName;
		this.colorName = colorName;
		this.foundDate = foundDate;
		this.storagePlace = storagePlace;
		this.imageUrl = imageUrl;
		this.lastFetchedAt = now;
	}

	/** 상세 API(fdPlace·tel·uniq) 응답을 반영한다. 상세는 최초 1회만 조회하고 이후에는 재호출하지 않는다. */
	public void applyDetail(String foundPlace, String storagePhone, String description, LocalDateTime now) {
		this.foundPlace = foundPlace;
		this.storagePhone = storagePhone;
		this.description = description;
		this.detailFetchedAt = now;
	}
}
