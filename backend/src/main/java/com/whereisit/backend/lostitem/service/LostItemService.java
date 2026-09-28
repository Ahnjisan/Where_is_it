package com.whereisit.backend.lostitem.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.whereisit.backend.candidate.dto.CandidateResponse;
import com.whereisit.backend.candidate.entity.LostItemCandidate;
import com.whereisit.backend.candidate.repository.LostItemCandidateRepository;
import com.whereisit.backend.founditem.client.FoundItemDetail;
import com.whereisit.backend.founditem.entity.FoundItem;
import com.whereisit.backend.founditem.service.FoundItemCollectionService;
import com.whereisit.backend.global.error.BusinessException;
import com.whereisit.backend.global.error.CommonErrorCode;
import com.whereisit.backend.lostitem.dto.CreateLostItemRequest;
import com.whereisit.backend.lostitem.dto.LostItemPageResponse;
import com.whereisit.backend.lostitem.dto.LostItemResponse;
import com.whereisit.backend.lostitem.dto.SearchConditionsPatch;
import com.whereisit.backend.lostitem.dto.UpdateLostItemRequest;
import com.whereisit.backend.lostitem.entity.ChatMessage;
import com.whereisit.backend.lostitem.entity.ChatRole;
import com.whereisit.backend.lostitem.entity.LostItem;
import com.whereisit.backend.lostitem.entity.LostItemStatus;
import com.whereisit.backend.lostitem.error.LostItemErrorCode;
import com.whereisit.backend.lostitem.repository.ChatMessageRepository;
import com.whereisit.backend.lostitem.repository.LostItemRepository;
import com.whereisit.backend.member.entity.LanguageCode;
import com.whereisit.backend.member.entity.Member;
import com.whereisit.backend.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

/**
 * 분실물 검색·추적 CRUD(API-05~09). findOwned·ensureNotExpired·applyConditions는
 * 검색 실행·추적 활성화(Issue #36의 SearchExecutionService)에서도 그대로 재사용한다.
 */
@Service
@RequiredArgsConstructor
public class LostItemService {

	/** API-16이 보여주는 상태. SEARCHING(추적 등록 전)은 제외한다. */
	private static final List<LostItemStatus> TRACKED_STATUSES = List.of(LostItemStatus.TRACKING, LostItemStatus.EXPIRED);

	/** API 명세 02_공통규칙: size 최대 50. application.yml의 max-page-size와 같다. */
	private static final int MAX_PAGE_SIZE = 50;

	private final LostItemRepository lostItemRepository;
	private final ChatMessageRepository chatMessageRepository;
	private final MemberRepository memberRepository;
	private final LostItemCandidateRepository candidateRepository;
	private final FoundItemCollectionService foundItemCollectionService;
	private final Clock clock;

	/** API-05. SEARCHING 건과 첫 USER 메시지만 저장하고, 실제 검색은 실행하지 않는다. */
	@Transactional
	public LostItemResponse create(Long memberId, CreateLostItemRequest request) {
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
		LanguageCode languageCode = request.languageCode() == null
				? member.getLanguageCode()
				: languageCodeOrThrow(request.languageCode());

		LostItem lostItem = LostItem.create(member, request.description(), languageCode);
		lostItemRepository.save(lostItem);
		chatMessageRepository.save(ChatMessage.of(lostItem, ChatRole.USER, request.description()));

		return LostItemResponse.from(lostItem, clock);
	}

	/** API-06. status는 저장된 값 그대로 필터한다(응답 필드는 만료시각을 반영해 다시 계산). */
	@Transactional(readOnly = true)
	public LostItemPageResponse list(Long memberId, LostItemStatus status, Pageable pageable) {
		Page<LostItem> page = status == null
				? lostItemRepository.findByMemberIdAndDeletedAtIsNullOrderByCreatedAtDescIdDesc(memberId, pageable)
				: lostItemRepository.findByMemberIdAndDeletedAtIsNullAndStatusOrderByCreatedAtDescIdDesc(memberId, status, pageable);
		return LostItemPageResponse.from(page, clock, id -> (int) candidateRepository.countByLostItemIdAndCurrentTrue(id));
	}

	/**
	 * API-16. 추적을 등록한 건(TRACKING·EXPIRED)만 조회한다. 정렬은 저장소 메서드로 고정하므로 요청의 sort는 쓰지 않고
	 * page·size만 옮긴다. 만료 시각이 지난 TRACKING은 응답에서만 EXPIRED로 보이고 DB 상태는 바꾸지 않는다.
	 */
	@Transactional(readOnly = true)
	public LostItemPageResponse listTracked(Long memberId, Pageable pageable) {
		Pageable pageOnly = PageRequest.of(pageable.getPageNumber(), Math.min(pageable.getPageSize(), MAX_PAGE_SIZE));
		Page<LostItem> page = lostItemRepository.findByMemberIdAndDeletedAtIsNullAndStatusInOrderByCreatedAtDescIdDesc(
				memberId, TRACKED_STATUSES, pageOnly);
		return LostItemPageResponse.from(page, clock, this::currentCandidateCount);
	}

	/**
	 * API-07. 현재 결과 1위 후보("대표 후보", scope=CURRENT와 같은 정렬 기준)의 상세(found_place 등)가
	 * 아직 없으면 상세 API를 호출해 채운다. 외부 호출 동안 DB 트랜잭션을 물고 있지 않는다
	 * (SearchExecutionService·TrackingActivationService와 같은 방식).
	 */
	@Transactional(propagation = Propagation.NOT_SUPPORTED)
	public LostItemResponse getDetail(Long memberId, Long lostItemId) {
		LostItem lostItem = findOwned(memberId, lostItemId);

		List<LostItemCandidate> top = candidateRepository.findCurrentOrderedByLostItemId(lostItemId, PageRequest.of(0, 1));
		if (top.isEmpty()) {
			return LostItemResponse.from(lostItem, clock, currentCandidateCount(lostItemId), null);
		}

		LostItemCandidate candidate = top.get(0);
		FoundItem foundItem = candidate.getFoundItem();
		CandidateResponse currentCandidate = foundItem.getDetailFetchedAt() == null
				? fetchAndApplyDetail(candidate, foundItem)
				: CandidateResponse.from(candidate);

		return LostItemResponse.from(lostItem, clock, currentCandidateCount(lostItemId), currentCandidate);
	}

	/** 상세 호출·저장 모두 실패·미실행 시 null 허용 정책이라, 실패해도 요청은 계속 200으로 성공한다. */
	private CandidateResponse fetchAndApplyDetail(LostItemCandidate candidate, FoundItem foundItem) {
		Optional<FoundItemDetail> detail = foundItemCollectionService.fetchDetail(
				foundItem.getSourceType(), foundItem.getAtcId(), foundItem.getFdSn());
		return detail
				.map(d -> CandidateResponse.from(candidate, foundItemCollectionService.applyDetail(foundItem.getId(), d)))
				.orElseGet(() -> CandidateResponse.from(candidate));
	}

	/** API-08. 전송된 필드만 바꾸고 EXPIRED 건은 거부한다. */
	@Transactional
	public LostItemResponse update(Long memberId, Long lostItemId, UpdateLostItemRequest request) {
		LostItem lostItem = findOwned(memberId, lostItemId);
		ensureNotExpired(lostItem);

		if (request.description() != null) {
			if (request.description().isBlank()) {
				throw new BusinessException(CommonErrorCode.VALIDATION_ERROR);
			}
			lostItem.updateDescription(request.description());
		}
		if (request.languageCode() != null) {
			lostItem.updateLanguageCode(languageCodeOrThrow(request.languageCode()));
		}
		if (request.conditions() != null) {
			applyConditions(lostItem, request.conditions());
		}
		if (request.notificationEmail() != null) {
			if (lostItem.getStatus() != LostItemStatus.TRACKING) {
				throw new BusinessException(LostItemErrorCode.TRACKING_NOT_STARTED);
			}
			lostItem.updateNotificationEmail(request.notificationEmail());
		}

		return LostItemResponse.from(lostItem, clock, currentCandidateCount(lostItem.getId()));
	}

	/** API-09. 논리 삭제이며, 이미 삭제된 건에 다시 호출해도 204(멱등)다. */
	@Transactional
	public void delete(Long memberId, Long lostItemId) {
		LostItem lostItem = lostItemRepository.findByIdAndMemberId(lostItemId, memberId)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
		if (!lostItem.isDeleted()) {
			lostItem.delete(LocalDateTime.now(clock));
		}
	}

	public int currentCandidateCount(Long lostItemId) {
		return (int) candidateRepository.countByLostItemIdAndCurrentTrue(lostItemId);
	}

	public LostItem findOwned(Long memberId, Long lostItemId) {
		return lostItemRepository.findByIdAndMemberIdAndDeletedAtIsNull(lostItemId, memberId)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.RESOURCE_NOT_FOUND));
	}

	public boolean isEffectivelyExpired(LostItem lostItem) {
		return lostItem.getStatus() == LostItemStatus.EXPIRED
				|| (lostItem.getStatus() == LostItemStatus.TRACKING
						&& lostItem.getExpiresAt() != null
						&& !lostItem.getExpiresAt().isAfter(LocalDateTime.now(clock)));
	}

	public void ensureNotExpired(LostItem lostItem) {
		if (isEffectivelyExpired(lostItem)) {
			throw new BusinessException(LostItemErrorCode.TRACKING_EXPIRED);
		}
	}

	/**
	 * conditions를 보냈으면 그 8개 필드를 통째로 반영한다. 필드 하나하나의 "null 전송(해제)"과
	 * "필드 자체를 안 보냄(유지)"을 구분하는 것은 이번 범위에서 단순화했다(PR 설명 참고).
	 */
	public void applyConditions(LostItem lostItem, SearchConditionsPatch patch) {
		if (patch.lostDateFrom() != null && patch.lostDateTo() != null && patch.lostDateFrom().isAfter(patch.lostDateTo())) {
			throw new BusinessException(LostItemErrorCode.INVALID_SEARCH_CONDITION);
		}
		lostItem.updateConditions(
				patch.categoryLargeCode(), patch.categoryMiddleCode(), patch.colorCode(), patch.regionCode(),
				patch.lostDateFrom(), patch.lostDateTo(), patch.lostPlaceText(), patch.searchStartDate());
	}

	private LanguageCode languageCodeOrThrow(String code) {
		return LanguageCode.fromCode(code)
				.orElseThrow(() -> new BusinessException(CommonErrorCode.UNSUPPORTED_LANGUAGE));
	}
}
