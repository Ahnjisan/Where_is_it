package com.whereisit.backend.auth.jwt;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * RT 원문 대신 DB에 저장할 SHA-256 해시(소문자 16진수 64자)를 만든다. 테이블 명세 v2 11_리프레시토큰.token_hash
 */
public final class TokenHasher {

	private TokenHasher() {
	}

	public static String sha256Hex(String token) {
		try {
			byte[] digest = MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		} catch (NoSuchAlgorithmException e) {
			// 모든 JVM이 SHA-256을 제공해야 하므로 여기에 오지 않는다.
			throw new IllegalStateException(e);
		}
	}
}
