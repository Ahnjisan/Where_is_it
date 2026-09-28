package com.whereisit.backend.founditem.client;

/** Portal operation 2 query: item name and custody place are optional search terms. */
public record PortalFoundItemSearchQuery(
		String productNameKeyword,
		String storagePlaceKeyword) {

	public PortalFoundItemSearchQuery {
		productNameKeyword = normalize(productNameKeyword);
		storagePlaceKeyword = normalize(storagePlaceKeyword);
	}

	private static String normalize(String value) {
		if (value == null || value.isBlank()) {
			return null;
		}
		return value;
	}
}
