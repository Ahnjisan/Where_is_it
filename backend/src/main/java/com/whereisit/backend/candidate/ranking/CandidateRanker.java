package com.whereisit.backend.candidate.ranking;

import java.util.List;

import com.whereisit.backend.founditem.entity.FoundItem;

/**
 * 분실물 설명과 습득물 후보를 비교해 우선순위·추천 이유를 만든다.
 * AI API 제공자가 decision-log에서 아직 미결정이라, 실제 AI 대신 규칙 기반 구현(SimpleTextSimilarityRanker)을
 * 쓴다. 나중에 AI로 교체할 때 이 인터페이스의 구현체만 바꾸면 된다.
 */
public interface CandidateRanker {

	/**
	 * requirements.md "AI 활용 원칙": 전달받은 foundItems 안의 습득물만 후보로 삼고, 없는 습득물을 만들어내지 않는다.
	 */
	List<RankedCandidate> rank(String description, List<FoundItem> foundItems);
}
