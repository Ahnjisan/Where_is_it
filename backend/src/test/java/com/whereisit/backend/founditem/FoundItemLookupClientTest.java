package com.whereisit.backend.founditem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.whereisit.backend.founditem.client.FoundItemListEntry;
import com.whereisit.backend.founditem.client.FoundItemLookupClient;
import com.whereisit.backend.founditem.client.FoundItemLookupException;
import com.whereisit.backend.founditem.client.FoundItemSearchQuery;
import com.whereisit.backend.founditem.client.PortalFoundItemSearchQuery;
import com.whereisit.backend.founditem.entity.FoundItemSourceType;

/** Issue #35. 실제 서버 없이 08_외부API에서 확인한 응답 형태로 XML 매핑을 검증한다. */
class FoundItemLookupClientTest {

	private static final String BASE_URL = "https://apis.data.go.kr/1320000/LosfundInfoInqireService/getLosfundInfoAccToClAreaPd";
	private static final String PORTAL_NAME_STORAGE_URL = "https://apis.data.go.kr/1320000/LosPtfundInfoInqireService/getPtLosfundInfoAccTpNmCstdyPlace";

	@Test
	void portalOperationTwoUsesOnlyOfficialNameAndStorageParameters() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(request -> {
			String uri = request.getURI().toString();
			assertThat(uri).startsWith(PORTAL_NAME_STORAGE_URL)
					.contains("serviceKey=test-key", "pageNo=1", "numOfRows=100",
							"PRDT_NM=%EC%A7%80%EA%B0%91", "DEP_PLACE=%EC%84%9C%EC%9A%B8%EC%97%AD")
					.doesNotContain("START_YMD", "END_YMD", "FD_COL_CD", "N_FD_LCT_CD");
		}).andRespond(withSuccess("""
				<response>
				  <header><resultCode>00</resultCode><resultMsg>NORMAL SERVICE.</resultMsg></header>
				  <body><items><item><atcId>A-1</atcId><fdSn>007</fdSn><fdPrdtNm>지갑</fdPrdtNm></item></items></body>
				</response>
				""", MediaType.APPLICATION_XML));

		FoundItemLookupClient client = new FoundItemLookupClient(
				builder, PORTAL_NAME_STORAGE_URL, "test-key", "", FoundItemSourceType.PORTAL);
		List<FoundItemListEntry> entries = client.searchByNameAndStorage(
				new PortalFoundItemSearchQuery("지갑", "서울역"));

		assertThat(entries).singleElement().satisfies(entry -> {
			assertThat(entry.atcId()).isEqualTo("A-1");
			assertThat(entry.fdSn()).isEqualTo("007");
		});
		server.verify();
	}

	@Test
	void parsesListResponseIntoEntries() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(startsWith(BASE_URL)))
				.andExpect(queryParam("serviceKey", "test-key"))
				.andExpect(queryParam("FD_COL_CD", "CLR001"))
				.andRespond(withSuccess("""
						<response>
						  <header><resultCode>00</resultCode><resultMsg>NORMAL SERVICE.</resultMsg></header>
						  <body>
						    <items>
						      <item>
						        <atcId>12345</atcId>
						        <fdSn>1</fdSn>
						        <fdPrdtNm>지갑</fdPrdtNm>
						        <fdSbjt>블루투스 지갑 습득</fdSbjt>
						        <prdtClNm>가방</prdtClNm>
						        <clrNm>파랑색</clrNm>
						        <fdYmd>2026-09-20</fdYmd>
						        <depPlace>서울지방경찰청</depPlace>
						        <fdFilePathImg>https://example.test/a.jpg</fdFilePathImg>
						      </item>
						    </items>
						  </body>
						</response>
						""", MediaType.APPLICATION_XML));

		FoundItemLookupClient client = new FoundItemLookupClient(
				builder, BASE_URL, "test-key", "FD_COL_CD", FoundItemSourceType.POLICE);

		List<FoundItemListEntry> entries = client.search(
				new FoundItemSearchQuery(null, null, "CLR001", null, null, null));

		assertThat(entries).hasSize(1);
		FoundItemListEntry entry = entries.get(0);
		assertThat(entry.sourceType()).isEqualTo(FoundItemSourceType.POLICE);
		assertThat(entry.atcId()).isEqualTo("12345");
		assertThat(entry.fdSn()).isEqualTo("1");
		assertThat(entry.productName()).isEqualTo("지갑");
		assertThat(entry.colorName()).isEqualTo("파랑색");
		assertThat(entry.foundDate()).isEqualTo(LocalDate.of(2026, 9, 20));
		assertThat(entry.storagePlace()).isEqualTo("서울지방경찰청");
		server.verify();
	}

	@Test
	void emptyItemsElementMeansZeroResults() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(startsWith(BASE_URL)))
				.andRespond(withSuccess("""
						<response>
						  <header><resultCode>00</resultCode><resultMsg>NORMAL SERVICE.</resultMsg></header>
						  <body><items/></body>
						</response>
						""", MediaType.APPLICATION_XML));

		FoundItemLookupClient client = new FoundItemLookupClient(
				builder, BASE_URL, "test-key", "FD_COL_CD", FoundItemSourceType.POLICE);

		assertThat(client.search(new FoundItemSearchQuery(null, null, null, null, null, null))).isEmpty();
	}

	@Test
	void nonNormalResultCodeThrowsInsteadOfReturningEmptyList() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(startsWith(BASE_URL)))
				.andRespond(withSuccess("""
						<response>
						  <header><resultCode>30</resultCode><resultMsg>SERVICE KEY IS NOT REGISTERED ERROR.</resultMsg></header>
						</response>
						""", MediaType.APPLICATION_XML));

		FoundItemLookupClient client = new FoundItemLookupClient(
				builder, BASE_URL, "bad-key", "FD_COL_CD", FoundItemSourceType.POLICE);

		assertThatThrownBy(() -> client.search(new FoundItemSearchQuery(null, null, null, null, null, null)))
				.isInstanceOf(FoundItemLookupException.class)
				.hasMessageContaining("30");
	}
}
