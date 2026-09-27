package com.whereisit.backend.lostitem.service;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.whereisit.backend.lostitem.dto.ChatMessageResponse;
import com.whereisit.backend.lostitem.dto.MessagePageResponse;
import com.whereisit.backend.lostitem.entity.ChatMessage;
import com.whereisit.backend.lostitem.repository.ChatMessageRepository;

import lombok.RequiredArgsConstructor;

/** API-10 분실물 대화 조회. */
@Service
@RequiredArgsConstructor
public class ChatMessageService {

	private final LostItemService lostItemService;
	private final ChatMessageRepository chatMessageRepository;

	@Transactional(readOnly = true)
	public MessagePageResponse list(Long memberId, Long lostItemId, Long afterMessageId, int limit) {
		lostItemService.findOwned(memberId, lostItemId);

		// hasMore를 알기 위해 요청한 것보다 한 건 더 가져온다.
		Limit fetchLimit = Limit.of(limit + 1);
		List<ChatMessage> fetched = afterMessageId == null
				? chatMessageRepository.findByLostItemIdOrderByIdAsc(lostItemId, fetchLimit)
				: chatMessageRepository.findByLostItemIdAndIdGreaterThanOrderByIdAsc(lostItemId, afterMessageId, fetchLimit);

		boolean hasMore = fetched.size() > limit;
		List<ChatMessage> page = hasMore ? fetched.subList(0, limit) : fetched;
		String nextAfterMessageId = hasMore ? String.valueOf(page.get(page.size() - 1).getId()) : null;

		return new MessagePageResponse(page.stream().map(ChatMessageResponse::from).toList(), nextAfterMessageId, hasMore);
	}
}
