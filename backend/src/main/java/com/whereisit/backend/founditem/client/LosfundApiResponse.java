package com.whereisit.backend.founditem.client;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * 경찰청·포털기관 습득물 목록 API의 공통 XML 응답 형태(response/header/body/items/item, 2026-09-26 실호출 확인).
 * 문서화되지 않은 필드는 매핑하지 않는다.
 */
@JacksonXmlRootElement(localName = "response")
@JsonIgnoreProperties(ignoreUnknown = true)
public class LosfundApiResponse {

	@JacksonXmlProperty(localName = "header")
	public Header header;

	@JacksonXmlProperty(localName = "body")
	public Body body;

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Header {
		@JacksonXmlProperty(localName = "resultCode")
		public String resultCode;

		@JacksonXmlProperty(localName = "resultMsg")
		public String resultMsg;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Body {
		@JacksonXmlElementWrapper(localName = "items")
		@JacksonXmlProperty(localName = "item")
		public List<Item> items;
	}

	/** 8_외부API "공통 | 기본 표시" 필드만 옮긴다. 상세 API 전용 필드(fdPlace·tel·uniq)는 여기 없다. */
	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Item {
		@JacksonXmlProperty(localName = "atcId")
		public String atcId;

		@JacksonXmlProperty(localName = "fdSn")
		public String fdSn;

		@JacksonXmlProperty(localName = "fdPrdtNm")
		public String fdPrdtNm;

		@JacksonXmlProperty(localName = "fdSbjt")
		public String fdSbjt;

		@JacksonXmlProperty(localName = "prdtClNm")
		public String prdtClNm;

		@JacksonXmlProperty(localName = "clrNm")
		public String clrNm;

		@JacksonXmlProperty(localName = "fdYmd")
		public String fdYmd;

		@JacksonXmlProperty(localName = "depPlace")
		public String depPlace;

		@JacksonXmlProperty(localName = "fdFilePathImg")
		public String fdFilePathImg;
	}
}
