package com.whereisit.backend.lostitem;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.fasterxml.jackson.databind.JsonNode;
import com.whereisit.backend.lostitem.entity.ChatMessage;
import com.whereisit.backend.lostitem.entity.ChatRole;
import com.whereisit.backend.lostitem.repository.ChatMessageRepository;
import com.whereisit.backend.lostitem.repository.LostItemRepository;
import com.whereisit.backend.support.ApiTestSupport;

@DisplayName("API-10 분실물 대화 조회")
class ChatMessageApiTest extends ApiTestSupport {

	@Autowired
	private LostItemRepository lostItemRepository;

	@Autowired
	private ChatMessageRepository chatMessageRepository;

	@Test
	@DisplayName("등록 직후에는 첫 USER 메시지 하나가 조회된다")
	void firstMessageIsStoredOnCreate() throws Exception {
		String accessToken = signupAndLogin("user@example.test").get("accessToken").asText();
		String lostItemId = createLostItem(accessToken, "lost blue wallet");

		authorizedGet("/api/lost-items/" + lostItemId + "/messages", accessToken)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items.length()").value(1))
				.andExpect(jsonPath("$.data.items[0].role").value("USER"))
				.andExpect(jsonPath("$.data.items[0].content").value("lost blue wallet"))
				.andExpect(jsonPath("$.data.hasMore").value(false));
	}

	@Test
	@DisplayName("다른 회원의 분실물 대화를 조회하면 404")
	void messagesAreScopedToOwner() throws Exception {
		String ownerToken = signupAndLogin("owner@example.test").get("accessToken").asText();
		String otherToken = signupAndLogin("other@example.test").get("accessToken").asText();
		String lostItemId = createLostItem(ownerToken, "lost wallet");

		authorizedGet("/api/lost-items/" + lostItemId + "/messages", otherToken)
				.andExpect(status().isNotFound())
				.andExpect(jsonPath("$.error.code").value("RESOURCE_NOT_FOUND"));
	}

	@Test
	@DisplayName("limit보다 메시지가 많으면 hasMore=true이고 nextAfterMessageId로 이어서 조회된다")
	void pagesThroughMessagesWithCursor() throws Exception {
		String accessToken = signupAndLogin("user@example.test").get("accessToken").asText();
		String lostItemId = createLostItem(accessToken, "first message");
		chatMessageRepository.save(ChatMessage.of(lostItemRepository.findById(Long.valueOf(lostItemId)).orElseThrow(),
				ChatRole.ASSISTANT, "second message"));

		JsonNode firstPage = objectMapper.readTree(
				authorizedGet("/api/lost-items/" + lostItemId + "/messages?limit=1", accessToken)
						.andExpect(status().isOk())
						.andExpect(jsonPath("$.data.hasMore").value(true))
						.andExpect(jsonPath("$.data.items[0].content").value("first message"))
						.andReturn().getResponse().getContentAsString()).get("data");
		String nextAfterMessageId = firstPage.get("nextAfterMessageId").asText();

		authorizedGet("/api/lost-items/" + lostItemId + "/messages?limit=1&afterMessageId=" + nextAfterMessageId, accessToken)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.items[0].content").value("second message"))
				.andExpect(jsonPath("$.data.hasMore").value(false));
	}

	private String createLostItem(String accessToken, String description) throws Exception {
		JsonNode created = objectMapper.readTree(
				authorizedPostJson("/api/lost-items", accessToken, Map.of("description", description))
						.andReturn().getResponse().getContentAsString()).get("data");
		return created.get("lostItemId").asText();
	}
}
