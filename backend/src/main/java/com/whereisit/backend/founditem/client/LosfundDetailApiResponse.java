package com.whereisit.backend.founditem.client;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlElementWrapper;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlProperty;
import com.fasterxml.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * 경찰청·포털기관 습득물 상세 API의 공통 XML 응답 형태(response/header/body/items/item, 목록과 같은 봉투).
 * 상세 전용 필드(fdPlace·tel·uniq)만 매핑한다(08_외부API·decision-log 2026-09-26).
 */
@JacksonXmlRootElement(localName = "response")
@JsonIgnoreProperties(ignoreUnknown = true)
public class LosfundDetailApiResponse {

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

	@JsonIgnoreProperties(ignoreUnknown = true)
	public static class Item {
		@JacksonXmlProperty(localName = "atcId")
		public String atcId;

		@JacksonXmlProperty(localName = "fdSn")
		public String fdSn;

		@JacksonXmlProperty(localName = "fdPlace")
		public String fdPlace;

		@JacksonXmlProperty(localName = "tel")
		public String tel;

		@JacksonXmlProperty(localName = "uniq")
		public String uniq;
	}
}
