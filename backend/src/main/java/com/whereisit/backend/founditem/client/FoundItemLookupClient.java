package com.whereisit.backend.founditem.client;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.whereisit.backend.founditem.entity.FoundItemSourceType;

import lombok.extern.slf4j.Slf4j;

/**
 * 습득물 목록 API 하나(POLICE 또는 PORTAL)를 호출한다. 두 출처는 base URL과 색상 파라미터 이름만 다르고
 * 나머지 요청·응답 형태가 같아서(08_외부API), 클래스 하나를 값만 다르게 두 번 생성해 쓴다(FoundItemClientConfig).
 */
@Slf4j
public class FoundItemLookupClient {

	private static final DateTimeFormatter YMD = DateTimeFormatter.ofPattern("yyyyMMdd");
	private static final DateTimeFormatter YMD_HYPHEN = DateTimeFormatter.ofPattern("yyyy-MM-dd");
	private static final String OK_RESULT_CODE = "00";
	private static final int PAGE_NO = 1;
	private static final int NUM_OF_ROWS = 100;
	/** 날짜 범위 전량 조회의 페이지 크기. 2026-09-28 실호출에서 5000건 1회 호출이 약 4.5초였다. */
	static final int BULK_NUM_OF_ROWS = 5000;
	/** 전량 조회가 끝나지 않는 경우를 막는 상한(5000건 × 20페이지 = 10만 건). */
	static final int MAX_BULK_PAGES = 20;

	private final RestClient restClient;
	private final String serviceKey;
	private final String colorParamName;
	private final FoundItemSourceType sourceType;

	/**
	 * builder를 받는 이유: 테스트가 MockRestServiceServer.bindTo(builder)로 실제 HTTP 없이 응답을 흉내 낼 수 있게 하기 위해서다.
	 * 두 클라이언트(POLICE·PORTAL)가 같은 builder 인스턴스를 공유해 baseUrl을 덮어쓰지 않도록 clone()한다.
	 */
	public FoundItemLookupClient(RestClient.Builder builder, String baseUrl, String serviceKey,
			String colorParamName, FoundItemSourceType sourceType) {
		this.restClient = builder.clone().baseUrl(baseUrl).build();
		this.serviceKey = serviceKey;
		this.colorParamName = colorParamName;
		this.sourceType = sourceType;
	}

	public FoundItemSourceType sourceType() {
		return sourceType;
	}

	public List<FoundItemListEntry> search(FoundItemSearchQuery query) {
		LosfundApiResponse response;
		try {
			response = restClient.get()
					.uri(uriBuilder -> {
						uriBuilder.queryParam("serviceKey", serviceKey)
								.queryParam("pageNo", PAGE_NO)
								.queryParam("numOfRows", NUM_OF_ROWS);
						if (query.categoryLargeCode() != null) {
							uriBuilder.queryParam("PRDT_CL_CD_01", query.categoryLargeCode());
						}
						if (query.categoryMiddleCode() != null) {
							uriBuilder.queryParam("PRDT_CL_CD_02", query.categoryMiddleCode());
						}
						if (query.colorCode() != null) {
							uriBuilder.queryParam(colorParamName, query.colorCode());
						}
						if (query.regionCode() != null) {
							uriBuilder.queryParam("N_FD_LCT_CD", query.regionCode());
						}
						if (query.startDate() != null) {
							uriBuilder.queryParam("START_YMD", query.startDate().format(YMD));
						}
						if (query.endDate() != null) {
							uriBuilder.queryParam("END_YMD", query.endDate().format(YMD));
						}
						return uriBuilder.build();
					})
					.retrieve()
					.body(LosfundApiResponse.class);
		} catch (RestClientException e) {
			throw new FoundItemLookupException(sourceType, e);
		}

		if (response == null || response.header == null || !OK_RESULT_CODE.equals(response.header.resultCode)) {
			String resultCode = response == null || response.header == null ? null : response.header.resultCode;
			String resultMsg = response == null || response.header == null ? null : response.header.resultMsg;
			throw new FoundItemLookupException(sourceType, resultCode, resultMsg);
		}
		if (response.body == null || response.body.items == null) {
			return List.of();
		}
		return response.body.items.stream().map(this::toEntry).toList();
	}

	/** Official portal operation 2. It accepts only optional PRDT_NM and DEP_PLACE filters. */
	public List<FoundItemListEntry> searchByNameAndStorage(PortalFoundItemSearchQuery query) {
		LosfundApiResponse response;
		try {
			response = restClient.get()
					.uri(uriBuilder -> {
						uriBuilder.queryParam("serviceKey", serviceKey)
								.queryParam("pageNo", PAGE_NO)
								.queryParam("numOfRows", NUM_OF_ROWS);
						if (query.productNameKeyword() != null) {
							uriBuilder.queryParam("PRDT_NM", query.productNameKeyword());
						}
						if (query.storagePlaceKeyword() != null) {
							uriBuilder.queryParam("DEP_PLACE", query.storagePlaceKeyword());
						}
						return uriBuilder.build();
					})
					.retrieve()
					.body(LosfundApiResponse.class);
		}
		catch (RestClientException e) {
			throw new FoundItemLookupException(sourceType, e);
		}
		return entries(response);
	}

	/**
	 * 분류·색상·지역 코드 없이 습득일 범위만으로 모든 페이지를 받는다(추적 재검색용).
	 * 코드 없이 날짜만 주면 습득일 기준으로 정확히 걸러진다(2026-09-28 실호출 확인). 물품명·보관장소 매칭은 호출자가 한다.
	 */
	public List<FoundItemListEntry> searchAllByFoundDate(LocalDate startDate, LocalDate endDate) {
		List<FoundItemListEntry> all = new ArrayList<>();
		for (int page = 1; page <= MAX_BULK_PAGES; page++) {
			LosfundApiResponse response = fetchFoundDatePage(startDate, endDate, page);
			List<FoundItemListEntry> entries = entries(response);
			all.addAll(entries);
			Integer totalCount = response.body == null ? null : response.body.totalCount;
			boolean lastPage = entries.size() < BULK_NUM_OF_ROWS
					|| (totalCount != null && all.size() >= totalCount);
			if (lastPage) {
				return all;
			}
		}
		log.warn("{} 습득일 범위 조회가 {}페이지를 넘어 이후 결과를 생략합니다: {}~{}",
				sourceType, MAX_BULK_PAGES, startDate, endDate);
		return all;
	}

	private LosfundApiResponse fetchFoundDatePage(LocalDate startDate, LocalDate endDate, int page) {
		try {
			return restClient.get()
					.uri(uriBuilder -> uriBuilder
							.queryParam("serviceKey", serviceKey)
							.queryParam("pageNo", page)
							.queryParam("numOfRows", BULK_NUM_OF_ROWS)
							.queryParam("START_YMD", startDate.format(YMD))
							.queryParam("END_YMD", endDate.format(YMD))
							.build())
					.retrieve()
					.body(LosfundApiResponse.class);
		}
		catch (RestClientException e) {
			throw new FoundItemLookupException(sourceType, e);
		}
	}

	private List<FoundItemListEntry> entries(LosfundApiResponse response) {
		if (response == null || response.header == null || !OK_RESULT_CODE.equals(response.header.resultCode)) {
			String resultCode = response == null || response.header == null ? null : response.header.resultCode;
			String resultMsg = response == null || response.header == null ? null : response.header.resultMsg;
			throw new FoundItemLookupException(sourceType, resultCode, resultMsg);
		}
		if (response.body == null || response.body.items == null) {
			return List.of();
		}
		return response.body.items.stream().map(this::toEntry).toList();
	}

	private FoundItemListEntry toEntry(LosfundApiResponse.Item item) {
		return new FoundItemListEntry(
				sourceType,
				item.atcId,
				item.fdSn,
				item.fdPrdtNm,
				item.fdSbjt,
				item.prdtClNm,
				item.clrNm,
				parseFdYmd(item.fdYmd),
				item.depPlace,
				item.fdFilePathImg);
	}

	/** 실제 응답은 yyyy-MM-dd지만, 공식 문서 예시(yyyyMMdd)도 방어적으로 처리한다(2026-09-26 검증 결과). */
	private LocalDate parseFdYmd(String fdYmd) {
		if (fdYmd == null || fdYmd.isBlank()) {
			return null;
		}
		try {
			return LocalDate.parse(fdYmd, YMD_HYPHEN);
		} catch (DateTimeParseException hyphenFailed) {
			try {
				return LocalDate.parse(fdYmd, YMD);
			} catch (DateTimeParseException plainFailed) {
				log.warn("{} fdYmd를 날짜로 해석하지 못했습니다: {}", sourceType, fdYmd);
				return null;
			}
		}
	}
}
