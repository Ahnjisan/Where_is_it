package com.whereisit.backend.tracking;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.whereisit.backend.candidate.entity.LostItemCandidate;
import com.whereisit.backend.candidate.repository.LostItemCandidateRepository;
import com.whereisit.backend.founditem.client.FoundItemListEntry;
import com.whereisit.backend.founditem.entity.FoundItem;
import com.whereisit.backend.founditem.service.FoundItemCollectionService;
import com.whereisit.backend.lostitem.entity.LostItem;

import lombok.RequiredArgsConstructor;

/**
 * 추적 매칭 결과를 분실물_후보에 반영한다. 추적 활성화(기준선)와 일일 재검색이 같은 규칙을 쓴다.
 * 이 분실물에 처음 나타난 습득물만 "새 후보"다. 이번 결과에 없는 기존 후보는 기록을 남기고 현재_결과_여부만 내린다.
 */
@Component
@RequiredArgsConstructor
public class TrackingCandidateRecorder {

	private final FoundItemCollectionService foundItemCollectionService;
	private final LostItemCandidateRepository candidateRepository;

	/** 호출자의 짧은 저장 트랜잭션에 참여한다. 새로 만든 후보만 돌려준다. */
	@Transactional(propagation = Propagation.MANDATORY)
	public List<LostItemCandidate> record(LostItem lostItem, List<FoundItemListEntry> matched,
			LocalDateTime now, boolean asBaseline) {
		List<FoundItem> foundItems = foundItemCollectionService.persist(matched);
		Map<Long, LostItemCandidate> existing = candidateRepository.findByLostItemId(lostItem.getId()).stream()
				.collect(Collectors.toMap(candidate -> candidate.getFoundItem().getId(), Function.identity()));

		List<LostItemCandidate> created = new ArrayList<>();
		Set<Long> seenFoundItemIds = new HashSet<>();
		for (FoundItem foundItem : foundItems) {
			LostItemCandidate candidate = existing.get(foundItem.getId());
			if (candidate == null) {
				candidate = candidateRepository.save(LostItemCandidate.create(lostItem, foundItem, now));
				created.add(candidate);
			}
			candidate.markSeenByTracking(now);
			if (asBaseline) {
				candidate.markAsBaseline();
			}
			seenFoundItemIds.add(foundItem.getId());
		}

		for (LostItemCandidate previous : existing.values()) {
			if (previous.isCurrent() && !seenFoundItemIds.contains(previous.getFoundItem().getId())) {
				previous.markNotInCurrentResult();
			}
		}
		return created;
	}
}
