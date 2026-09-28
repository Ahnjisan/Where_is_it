package com.whereisit.backend.founditem.client;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

/**
 * 경찰청 습득물정보 조회 API(POLICE)와 포털기관 습득물정보 조회 API(PORTAL)의 접속 정보.
 * 실제 키는 공공데이터포털에서 발급받아 .env의 PUBLIC_DATA_SERVICE_KEY로 주입한다(Issue #35 참고).
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.public-data")
public class FoundItemApiProperties {

	/** 공공데이터포털 서비스 키. 비어 있으면 클라이언트를 호출할 때 401/오류가 난다(값 자체를 검증하지는 않는다). */
	private String serviceKey = "";

	private String policeFoundItemUrl = "https://apis.data.go.kr/1320000/LosfundInfoInqireService/getLosfundInfoAccToClAreaPd";

	private String portalFoundItemUrl = "https://apis.data.go.kr/1320000/LosPtfundInfoInqireService/getPtLosfundInfoAccToClAreaPd";

	/**
	 * 상세(fdPlace·tel·uniq) 조회 URL(API-07이 사용). 목록과 같은 서비스의 상세 operation이며,
	 * 값을 비워 두면 상세 호출은 항상 실패로 처리되고 NULL 정책이 적용된다.
	 */
	private String policeFoundItemDetailUrl = "https://apis.data.go.kr/1320000/LosfundInfoInqireService/getLosfundDetailInfo";

	private String portalFoundItemDetailUrl = "https://apis.data.go.kr/1320000/LosPtfundInfoInqireService/getPtLosfundDetailInfo";
}
