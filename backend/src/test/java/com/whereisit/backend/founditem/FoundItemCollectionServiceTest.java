package com.whereisit.backend.founditem;

import static org.assertj.core.api.Assertions.assertThat;
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

import com.whereisit.backend.founditem.client.FoundItemListEntry;
import com.whereisit.backend.founditem.client.FoundItemLookupClient;
import com.whereisit.backend.founditem.client.FoundItemSearchQuery;
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
	private FoundItemCollectionService collectionService;

	@BeforeEach
	void setUp() {
		policeClient = Mockito.mock(FoundItemLookupClient.class);
		when(policeClient.sourceType()).thenReturn(FoundItemSourceType.POLICE);
		collectionService = new FoundItemCollectionService(List.of(policeClient), foundItemRepository, clock);
	}

	@Test
	void reCollectingSameNaturalKeyUpdatesInsteadOfDuplicating() {
		FoundItemSearchQuery query = new FoundItemSearchQuery(null, null, null, null, null, null);
		when(policeClient.search(query)).thenReturn(List.of(
				new FoundItemListEntry(FoundItemSourceType.POLICE, "12345", "1", "지갑", "지갑 습득", "가방", "파랑",
						LocalDate.of(2026, 9, 20), "서울청", "https://example.test/a.jpg")));

		List<FoundItem> firstRun = collectionService.collect(FoundItemSourceType.POLICE, query);
		assertThat(firstRun).hasSize(1);
		assertThat(foundItemRepository.count()).isEqualTo(1);

		when(policeClient.search(query)).thenReturn(List.of(
				new FoundItemListEntry(FoundItemSourceType.POLICE, "12345", "1", "지갑(수정)", "지갑 습득", "가방", "파랑",
						LocalDate.of(2026, 9, 20), "서울청", "https://example.test/a.jpg")));

		List<FoundItem> secondRun = collectionService.collect(FoundItemSourceType.POLICE, query);

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
		collectionService = new FoundItemCollectionService(List.of(policeClient, portalClient), foundItemRepository, clock);

		List<FoundItem> result = collectionService.collectAllSources(query);

		assertThat(result).hasSize(2);
		assertThat(foundItemRepository.count()).isEqualTo(2);
	}
}
