package com.whereisit.backend.lostitem.repository;

import java.util.List;

import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;

import com.whereisit.backend.lostitem.entity.ChatMessage;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {

	List<ChatMessage> findByLostItemIdAndIdGreaterThanOrderByIdAsc(Long lostItemId, Long afterMessageId, Limit limit);

	List<ChatMessage> findByLostItemIdOrderByIdAsc(Long lostItemId, Limit limit);
}
