package com.whereisit.backend.lostitem.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.whereisit.backend.lostitem.dto.CreateLostItemRequest;
import com.whereisit.backend.lostitem.dto.LostItemResponse;
import com.whereisit.backend.search.dto.SearchExecutionResponse;
import com.whereisit.backend.search.service.SearchExecutionService;

import lombok.RequiredArgsConstructor;

/**
 * Creates the initial lost-item search before running external AI and found-item lookups.
 * The orchestration itself must not hold a transaction so LostItemService#create commits first.
 */
@Service
@RequiredArgsConstructor
public class InitialSearchOrchestrator {

	private final LostItemService lostItemService;
	private final SearchExecutionService searchExecutionService;

	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public SearchExecutionResponse createAndSearch(Long memberId, CreateLostItemRequest request) {
		LostItemResponse created = lostItemService.create(memberId, request);
		Long lostItemId = Long.valueOf(created.lostItemId());
		return searchExecutionService.executeInitial(memberId, lostItemId);
	}
}
