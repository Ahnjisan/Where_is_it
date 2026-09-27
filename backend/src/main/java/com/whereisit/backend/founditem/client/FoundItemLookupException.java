package com.whereisit.backend.founditem.client;

import com.whereisit.backend.founditem.entity.FoundItemSourceType;

/**
 * 습득물 목록 API가 정상(resultCode 00)이 아닌 응답을 줬을 때 던진다.
 * 08_외부API 규칙: 잘못된 키·시간초과를 정상 0건으로 취급하지 않는다. HTTP 오류 코드로의 변환은 호출한 쪽(Issue #36)이 한다.
 */
public class FoundItemLookupException extends RuntimeException {

	public FoundItemLookupException(FoundItemSourceType sourceType, String resultCode, String resultMsg) {
		super("%s lookup failed: resultCode=%s, resultMsg=%s".formatted(sourceType, resultCode, resultMsg));
	}

	public FoundItemLookupException(FoundItemSourceType sourceType, Throwable cause) {
		super("%s lookup failed".formatted(sourceType), cause);
	}
}
