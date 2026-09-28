package com.whereisit.backend.founditem.client;

import java.util.List;

/** Client dedicated to portal operation 2 (name/custody-place list lookup). */
public class PortalFoundItemNameStorageClient {

	private final FoundItemLookupClient delegate;

	public PortalFoundItemNameStorageClient(FoundItemLookupClient delegate) {
		this.delegate = delegate;
	}

	public List<FoundItemListEntry> search(PortalFoundItemSearchQuery query) {
		return delegate.searchByNameAndStorage(query);
	}
}
