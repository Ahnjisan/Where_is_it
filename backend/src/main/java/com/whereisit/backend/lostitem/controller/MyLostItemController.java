package com.whereisit.backend.lostitem.controller;

import org.springframework.data.domain.Pageable;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.whereisit.backend.global.response.ApiResponse;
import com.whereisit.backend.lostitem.dto.TrackedLostItemPageResponse;
import com.whereisit.backend.lostitem.service.LostItemService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;

/**
 * API-16 내 추적 분실물 목록. 회원 ID는 요청이 아니라 AT에서 정한다.
 * page·size는 전역 Pageable 설정(기본 20·최대 50, page는 0부터)을 그대로 따르고, 정렬은 고정이라 sort는 받지 않는다.
 */
@Tag(name = "내 추적", description = "추적을 등록한 내 분실물 목록")
@RestController
@RequestMapping("/api/members/me/lost-items")
@RequiredArgsConstructor
public class MyLostItemController {

	private final LostItemService lostItemService;

	@Operation(summary = "API-16 내 추적 분실물 목록", description = """
			추적을 등록한 내 분실물(TRACKING·EXPIRED)을 최근 등록순(생성 시각 DESC, 분실물 ID DESC)으로 조회한다.
			SEARCHING·삭제한 건·다른 회원의 건은 포함하지 않는다. page는 0부터, size는 기본 20·최대 50이며
			50을 넘기면 50으로 조회한다. 만료 시각이 지난 TRACKING은 status를 EXPIRED로 보여준다. 결과가 없으면 빈 items다.
			conditions에는 이 API에만 있는 등록 물품 표시값 itemTypeName(물품명 검색어)과 colorName(표시용 색상명)이
			더 있다. 둘 다 없으면 null이며 공식 코드(categoryLargeCode·colorCode 등)가 아니다. currentCandidate는 항상 null이다.""")
	@ApiResponses({
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "조회 성공(없으면 빈 목록)"),
			@io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "AUTH_REQUIRED(토큰 없음), INVALID_TOKEN, TOKEN_EXPIRED")
	})
	@Parameter(name = "page", in = ParameterIn.QUERY, description = "페이지 번호(0부터)",
			schema = @Schema(type = "integer", defaultValue = "0", minimum = "0"))
	@Parameter(name = "size", in = ParameterIn.QUERY, description = "페이지 크기. 50을 넘기면 50으로 조회한다",
			schema = @Schema(type = "integer", defaultValue = "20", minimum = "1", maximum = "50"))
	@GetMapping
	public ApiResponse<TrackedLostItemPageResponse> listTracked(@AuthenticationPrincipal Long memberId,
			@Parameter(hidden = true) Pageable pageable) {
		return ApiResponse.ok(lostItemService.listTracked(memberId, pageable));
	}
}
