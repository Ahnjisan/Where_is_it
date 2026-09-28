package com.whereisit.backend.founditem.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

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
