package com.whereisit.backend.founditem.client;

import java.util.List;
import java.util.Optional;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.whereisit.backend.founditem.entity.FoundItemSourceType;

import lombok.extern.slf4j.Slf4j;

/**
 * 습득물 상세 API 하나(POLICE 또는 PORTAL)를 호출한다. 목록 API({@link FoundItemLookupClient})와
 * 같은 방식으로 소스별 인스턴스를 두 번 생성해 쓴다.
 *
 * 요청 파라미터명({@code ATC_ID}·{@code FD_SN})은 목록 API의 대문자 스네이크케이스 관례를 따라
 * 임시로 정한 값이다. 실제 승인받은 data.go.kr 활용신청 문서의 상세 operation 경로·파라미터명을
 * 재확인하기 전까지는 검증되지 않은 값이다.
 *
 * 상세는 "실패·미실행 시 null 허용" 정책이라(08_외부API), 목록 API({@link FoundItemLookupClient})와
 * 달리 실패 사유를 구분해 예외를 던지지 않고 모든 실패를 {@code Optional.empty()}로 수렴한다.
 */
@Slf4j
public class FoundItemDetailClient {

	private static final String OK_RESULT_CODE = "00";

	private final RestClient restClient;
	private final String serviceKey;
	private final FoundItemSourceType sourceType;

	public FoundItemDetailClient(RestClient.Builder builder, String baseUrl, String serviceKey,
			FoundItemSourceType sourceType) {
		this.restClient = builder.clone().baseUrl(baseUrl).build();
		this.serviceKey = serviceKey;
		this.sourceType = sourceType;
	}

	public FoundItemSourceType sourceType() {
		return sourceType;
	}

	public Optional<FoundItemDetail> fetchDetail(String atcId, String fdSn) {
		LosfundDetailApiResponse response;
		try {
			response = restClient.get()
					.uri(uriBuilder -> uriBuilder
							.queryParam("serviceKey", serviceKey)
							.queryParam("ATC_ID", atcId)
							.queryParam("FD_SN", fdSn)
							.build())
					.retrieve()
					.body(LosfundDetailApiResponse.class);
		} catch (RestClientException e) {
			log.warn("{} 상세 조회 실패, null로 처리합니다", sourceType, e);
			return Optional.empty();
		}

		if (response == null || response.header == null || !OK_RESULT_CODE.equals(response.header.resultCode)) {
			String resultCode = response == null || response.header == null ? null : response.header.resultCode;
			String resultMsg = response == null || response.header == null ? null : response.header.resultMsg;
			log.warn("{} 상세 조회 실패, null로 처리합니다: resultCode={}, resultMsg={}", sourceType, resultCode, resultMsg);
			return Optional.empty();
		}
		List<LosfundDetailApiResponse.Item> items = response.body == null ? null : response.body.items;
		if (items == null || items.isEmpty()) {
			return Optional.empty();
		}
		LosfundDetailApiResponse.Item item = items.get(0);
		return Optional.of(new FoundItemDetail(item.fdPlace, item.tel, item.uniq));
	}
}
