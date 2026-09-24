package com.whereisit.backend.global.response;

/**
 * 공통 성공 응답. API 명세 02_공통규칙 R8: {"success": true, "data": ...}
 */
public record ApiResponse<T>(boolean success, T data) {

	public static <T> ApiResponse<T> ok(T data) {
		return new ApiResponse<>(true, data);
	}

	/** 돌려줄 데이터가 없는 성공 응답(예: 로그아웃)은 data를 null로 둔다. */
	public static ApiResponse<Void> ok() {
		return new ApiResponse<>(true, null);
	}
}
