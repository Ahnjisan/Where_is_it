package com.whereisit.backend.founditem.client;

/**
 * 습득물 상세 API 응답 한 건. 08_외부API 매핑: fdPlace→foundPlace, tel→storagePhone, uniq→description.
 */
public record FoundItemDetail(
		String foundPlace,
		String storagePhone,
		String description) {
}
