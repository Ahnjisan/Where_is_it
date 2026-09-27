package com.whereisit.backend.lostitem.entity;

import com.whereisit.backend.global.entity.BaseCreatedTimeEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 대화_메시지. 테이블 명세 v2 07_대화메시지(chat_messages).
 */
@Getter
@Entity
@Table(name = "chat_messages")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ChatMessage extends BaseCreatedTimeEntity {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	@Column(name = "message_id")
	private Long id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "lost_item_id", nullable = false)
	private LostItem lostItem;

	@Enumerated(EnumType.STRING)
	@Column(name = "role", nullable = false, length = 16)
	private ChatRole role;

	@Column(name = "content", nullable = false, columnDefinition = "TEXT")
	private String content;

	private ChatMessage(LostItem lostItem, ChatRole role, String content) {
		this.lostItem = lostItem;
		this.role = role;
		this.content = content;
	}

	public static ChatMessage of(LostItem lostItem, ChatRole role, String content) {
		return new ChatMessage(lostItem, role, content);
	}
}
