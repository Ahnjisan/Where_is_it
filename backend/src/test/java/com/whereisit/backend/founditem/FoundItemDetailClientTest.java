package com.whereisit.backend.founditem;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.queryParam;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import com.whereisit.backend.founditem.client.FoundItemDetail;
import com.whereisit.backend.founditem.client.FoundItemDetailClient;
import com.whereisit.backend.founditem.entity.FoundItemSourceType;

/**
 * Issue #35 후속(API-07). 목록과 같은 XML 봉투 형태로 상세 필드(fdPlace·tel·uniq)를 매핑하는지
 * 검증한다. 실제 operation 경로·파라미터명은 아직 미확정이라 파라미터명(ATC_ID·FD_SN)은
 * 목록 API 명명 관례를 따른 임시값이다(실제 값 확인 전까지 검증 필요).
 */
class FoundItemDetailClientTest {

	private static final String BASE_URL = "https://apis.data.go.kr/1320000/LosfundInfoInqireService/getLosfundInfoAccToAtcId";

	@Test
	void parsesDetailResponseIntoFoundItemDetail() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(startsWith(BASE_URL)))
				.andExpect(queryParam("serviceKey", "test-key"))
				.andExpect(queryParam("ATC_ID", "12345"))
				.andExpect(queryParam("FD_SN", "1"))
				.andRespond(withSuccess("""
						<response>
						  <header><resultCode>00</resultCode><resultMsg>NORMAL SERVICE.</resultMsg></header>
						  <body>
						    <items>
						      <item>
						        <atcId>12345</atcId>
						        <fdSn>1</fdSn>
						        <fdPlace>서울역 유실물센터</fdPlace>
						        <tel>02-1234-5678</tel>
						        <uniq>검정 장지갑, 안쪽에 명함 있음</uniq>
						      </item>
						    </items>
						  </body>
						</response>
						""", MediaType.APPLICATION_XML));

		FoundItemDetailClient client = new FoundItemDetailClient(builder, BASE_URL, "test-key", FoundItemSourceType.POLICE);

		Optional<FoundItemDetail> detail = client.fetchDetail("12345", "1");

		assertThat(detail).isPresent();
		assertThat(detail.get().foundPlace()).isEqualTo("서울역 유실물센터");
		assertThat(detail.get().storagePhone()).isEqualTo("02-1234-5678");
		assertThat(detail.get().description()).isEqualTo("검정 장지갑, 안쪽에 명함 있음");
		server.verify();
	}

	@Test
	void emptyItemsElementMeansNoDetail() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(startsWith(BASE_URL)))
				.andRespond(withSuccess("""
						<response>
						  <header><resultCode>00</resultCode><resultMsg>NORMAL SERVICE.</resultMsg></header>
						  <body><items/></body>
						</response>
						""", MediaType.APPLICATION_XML));

		FoundItemDetailClient client = new FoundItemDetailClient(builder, BASE_URL, "test-key", FoundItemSourceType.POLICE);

		assertThat(client.fetchDetail("12345", "1")).isEmpty();
	}

	@Test
	void nonNormalResultCodeMeansNoDetailInsteadOfThrowing() {
		RestClient.Builder builder = RestClient.builder();
		MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
		server.expect(requestTo(startsWith(BASE_URL)))
				.andRespond(withSuccess("""
						<response>
						  <header><resultCode>30</resultCode><resultMsg>SERVICE KEY IS NOT REGISTERED ERROR.</resultMsg></header>
						</response>
						""", MediaType.APPLICATION_XML));

		FoundItemDetailClient client = new FoundItemDetailClient(builder, BASE_URL, "bad-key", FoundItemSourceType.POLICE);

		assertThat(client.fetchDetail("12345", "1")).isEmpty();
	}
}
