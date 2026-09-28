package com.whereisit.backend.founditem.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.whereisit.backend.founditem.client.FoundItemDetail;
import com.whereisit.backend.founditem.client.FoundItemDetailClient;
import com.whereisit.backend.founditem.client.FoundItemListEntry;
import com.whereisit.backend.founditem.client.FoundItemLookupClient;
import com.whereisit.backend.founditem.client.FoundItemSearchQuery;
import com.whereisit.backend.founditem.entity.FoundItem;
import com.whereisit.backend.founditem.entity.FoundItemSourceType;
import com.whereisit.backend.founditem.repository.FoundItemRepository;

import lombok.RequiredArgsConstructor;

/**
 * Issue #35. 습득물 API 응답을 습득물(found_items) 테이블에 중복 없이 저장한다.
 * 같은 (출처, 외부 관리번호, 외부 습득순번)을 다시 수집하면 새 행 대신 표시용 필드와 최근_수집_시각만 갱신한다.
 */
@Service
@RequiredArgsConstructor
public class FoundItemCollectionService {

	private final List<FoundItemLookupClient> lookupClients;
	private final List<FoundItemDetailClient> detailClients;
	private final FoundItemRepository foundItemRepository;
	private final Clock clock;

	/** 지정한 출처 하나를 트랜잭션 없이 조회한다. */
	@Transactional(propagation = Propagation.NEVER)
	public List<FoundItemListEntry> lookup(FoundItemSourceType sourceType, FoundItemSearchQuery query) {
		FoundItemLookupClient client = lookupClients.stream()
				.filter(candidate -> candidate.sourceType() == sourceType)
				.findFirst()
				.orElseThrow(() -> new NoSuchElementException("No lookup client for " + sourceType));
		return client.search(query);
	}

	/** 이미 조회한 응답을 호출자의 짧은 저장 트랜잭션에 참여해 upsert한다. */
	@Transactional(propagation = Propagation.MANDATORY)
	public List<FoundItem> persist(List<FoundItemListEntry> entries) {
		return entries.stream().map(this::upsert).toList();
	}

	/** API-07 상세조회용. 실패 시 null 허용 여부는 {@link FoundItemDetailClient}가 이미 처리한다. */
	@Transactional(propagation = Propagation.NEVER)
	public Optional<FoundItemDetail> fetchDetail(FoundItemSourceType sourceType, String atcId, String fdSn) {
		FoundItemDetailClient client = detailClients.stream()
				.filter(candidate -> candidate.sourceType() == sourceType)
				.findFirst()
				.orElseThrow(() -> new NoSuchElementException("No detail client for " + sourceType));
		return client.fetchDetail(atcId, fdSn);
	}

	/**
	 * 조회한 상세를 새 트랜잭션에서 반영한다. 호출자(API-07)는 외부 호출 동안 트랜잭션을 물고 있지
	 * 않으므로(NOT_SUPPORTED), id로 새로 읽어 저장한다(호출자가 들고 있던 detached 인스턴스를
	 * 그대로 쓰지 않음).
	 */
	@Transactional(propagation = Propagation.REQUIRES_NEW)
	public FoundItem applyDetail(Long foundItemId, FoundItemDetail detail) {
		FoundItem foundItem = foundItemRepository.findById(foundItemId)
				.orElseThrow(() -> new NoSuchElementException("No found item for id " + foundItemId));
		foundItem.applyDetail(detail.foundPlace(), detail.storagePhone(), detail.description(), LocalDateTime.now(clock));
		return foundItemRepository.save(foundItem);
	}

	private FoundItem upsert(FoundItemListEntry entry) {
		LocalDateTime now = LocalDateTime.now(clock);
		FoundItem foundItem = foundItemRepository
				.findBySourceTypeAndAtcIdAndFdSn(entry.sourceType(), entry.atcId(), entry.fdSn())
				.orElseGet(() -> FoundItem.create(entry.sourceType(), entry.atcId(), entry.fdSn(), now));

		foundItem.refreshListFields(entry.productName(), entry.subject(), entry.categoryName(), entry.colorName(),
				entry.foundDate(), entry.storagePlace(), entry.imageUrl(), now);

		return foundItemRepository.save(foundItem);
	}
}
