package com.whereisit.backend.founditem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import com.whereisit.backend.founditem.client.FoundItemDetail;
import com.whereisit.backend.founditem.client.FoundItemDetailClient;
import com.whereisit.backend.founditem.client.FoundItemListEntry;
import com.whereisit.backend.founditem.client.FoundItemLookupClient;
import com.whereisit.backend.founditem.client.FoundItemSearchQuery;
import com.whereisit.backend.founditem.client.PortalFoundItemNameStorageClient;
import com.whereisit.backend.founditem.entity.FoundItem;
import com.whereisit.backend.founditem.entity.FoundItemSourceType;
import com.whereisit.backend.founditem.repository.FoundItemRepository;
import com.whereisit.backend.founditem.service.FoundItemCollectionService;
import com.whereisit.backend.support.TestClockConfig;

/** Issue #35. 같은 (출처, 외부 관리번호, 외부 습득순번)을 다시 수집해도 행이 늘지 않는지 확인한다. */
@SpringBootTest
@Transactional
@Import(TestClockConfig.class)
class FoundItemCollectionServiceTest {

	@Autowired
	private FoundItemRepository foundItemRepository;

	@Autowired
	private Clock clock;

	private FoundItemLookupClient policeClient;
	private FoundItemDetailClient policeDetailClient;
	private FoundItemDetailClient portalDetailClient;
	private PortalFoundItemNameStorageClient portalNameStorageClient;
	private FoundItemCollectionService collectionService;

	@BeforeEach
	void setUp() {
		policeClient = Mockito.mock(FoundItemLookupClient.class);
		when(policeClient.sourceType()).thenReturn(FoundItemSourceType.POLICE);
		policeDetailClient = Mockito.mock(FoundItemDetailClient.class);
		when(policeDetailClient.sourceType()).thenReturn(FoundItemSourceType.POLICE);
		portalDetailClient = Mockito.mock(FoundItemDetailClient.class);
		when(portalDetailClient.sourceType()).thenReturn(FoundItemSourceType.PORTAL);
		portalNameStorageClient = Mockito.mock(PortalFoundItemNameStorageClient.class);
		collectionService = new FoundItemCollectionService(
				List.of(policeClient), portalNameStorageClient, policeDetailClient, portalDetailClient,
				foundItemRepository, clock);
	}

	@Test
	void reCollectingSameNaturalKeyUpdatesInsteadOfDuplicating() {
		FoundItemSearchQuery query = new FoundItemSearchQuery(null, null, null, null, null, null);
		when(policeClient.search(query)).thenReturn(List.of(
				new FoundItemListEntry(FoundItemSourceType.POLICE, "12345", "1", "지갑", "지갑 습득", "가방", "파랑",
						LocalDate.of(2026, 9, 20), "서울청", "https://example.test/a.jpg")));

		List<FoundItem> firstRun = collectionService.persist(
				collectionService.lookup(FoundItemSourceType.POLICE, query));
		assertThat(firstRun).hasSize(1);
		assertThat(foundItemRepository.count()).isEqualTo(1);

		when(policeClient.search(query)).thenReturn(List.of(
				new FoundItemListEntry(FoundItemSourceType.POLICE, "12345", "1", "지갑(수정)", "지갑 습득", "가방", "파랑",
						LocalDate.of(2026, 9, 20), "서울청", "https://example.test/a.jpg")));

		List<FoundItem> secondRun = collectionService.persist(
				collectionService.lookup(FoundItemSourceType.POLICE, query));

		assertThat(foundItemRepository.count()).isEqualTo(1);
		assertThat(secondRun.get(0).getId()).isEqualTo(firstRun.get(0).getId());
		assertThat(secondRun.get(0).getProductName()).isEqualTo("지갑(수정)");
		assertThat(secondRun.get(0).getLastFetchedAt()).isNotNull();
	}

	@Test
	void differentSourceTypeWithSameExternalIdIsASeparateRow() {
		FoundItemSearchQuery query = new FoundItemSearchQuery(null, null, null, null, null, null);
		FoundItemLookupClient portalClient = Mockito.mock(FoundItemLookupClient.class);
		when(portalClient.sourceType()).thenReturn(FoundItemSourceType.PORTAL);
		when(policeClient.search(query)).thenReturn(List.of(
				new FoundItemListEntry(FoundItemSourceType.POLICE, "999", "1", "우산", null, null, null, null, null, null)));
		when(portalClient.search(query)).thenReturn(List.of(
				new FoundItemListEntry(FoundItemSourceType.PORTAL, "999", "1", "우산(포털)", null, null, null, null, null, null)));
		collectionService = new FoundItemCollectionService(
				List.of(policeClient, portalClient), portalNameStorageClient, policeDetailClient, portalDetailClient,
				foundItemRepository, clock);

		List<FoundItemListEntry> entries = new java.util.ArrayList<>(
				collectionService.lookup(FoundItemSourceType.POLICE, query));
		entries.addAll(collectionService.lookup(FoundItemSourceType.PORTAL, query));
		List<FoundItem> result = collectionService.persist(entries);

		assertThat(result).hasSize(2);
		assertThat(foundItemRepository.count()).isEqualTo(2);
	}

	@Test
	void applyDetailFillsDetailFieldsAndStampsDetailFetchedAt() {
		FoundItem foundItem = foundItemRepository.save(
				FoundItem.create(FoundItemSourceType.POLICE, "12345", "1", clock.instant().atZone(clock.getZone()).toLocalDateTime()));
		when(policeDetailClient.fetchDetail("12345", "1"))
				.thenReturn(Optional.of(new FoundItemDetail("서울역 유실물센터", "02-1234-5678", "검정 장지갑")));

		Optional<FoundItemDetail> detail = collectionService.fetchDetail(FoundItemSourceType.POLICE, "12345", "1");
		assertThat(detail).isPresent();

		FoundItem updated = collectionService.applyDetail(foundItem.getId(), detail.get());
		assertThat(updated.getFoundPlace()).isEqualTo("서울역 유실물센터");
		assertThat(updated.getStoragePhone()).isEqualTo("02-1234-5678");
		assertThat(updated.getDescription()).isEqualTo("검정 장지갑");
		assertThat(updated.getDetailFetchedAt()).isNotNull();
	}

	@Test
	void portalDetailLookupUsesOnlyPortalDetailClient() {
		when(portalDetailClient.fetchDetail("portal-atc", "portal-fd-sn"))
				.thenReturn(Optional.empty());

		collectionService.fetchDetail(FoundItemSourceType.PORTAL, "portal-atc", "portal-fd-sn");

		verify(portalDetailClient).fetchDetail("portal-atc", "portal-fd-sn");
		verify(policeDetailClient, never()).fetchDetail("portal-atc", "portal-fd-sn");
	}
}
