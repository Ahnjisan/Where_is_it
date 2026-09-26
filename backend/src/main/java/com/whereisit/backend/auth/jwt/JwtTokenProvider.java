package com.whereisit.backend.auth.jwt;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

import javax.crypto.SecretKey;

import org.springframework.stereotype.Component;

import com.whereisit.backend.auth.error.AuthErrorCode;
import com.whereisit.backend.global.config.TimeConfig;
import com.whereisit.backend.global.error.BusinessException;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

/**
 * AT·RT를 HS256 JWT로 발급하고 검증한다.
 * 클레임: iss, sub(memberId만), jti(난수), token_type, iat, exp. 시각은 주입받은 Clock 기준이다.
 */
@Component
public class JwtTokenProvider {

	private static final String ISSUER = "where-is-it";

	private final SecretKey key;
	private final Clock clock;
	private final JwtProperties properties;
	private final JwtParser parser;

	public JwtTokenProvider(JwtProperties properties, Clock clock) {
		this.properties = properties;
		this.clock = clock;
		this.key = createKey(properties.secret());
		this.parser = Jwts.parser()
				.verifyWith(key)
				.requireIssuer(ISSUER)
				.clock(() -> Date.from(clock.instant()))
				.build();
	}

	public IssuedToken issueAccessToken(Long memberId) {
		return issue(memberId, TokenType.ACCESS, properties.accessTokenTtl().toSeconds());
	}

	/**
	 * 로그인할 때마다 새 RT를 발급한다. 같은 초에 두 번 발급해도 jti가 달라서 원문(과 해시)이 겹치지 않는다.
	 */
	public IssuedToken issueRefreshToken(Long memberId) {
		return issue(memberId, TokenType.REFRESH, properties.refreshTokenTtl().toSeconds());
	}

	public long accessTokenTtlSeconds() {
		return properties.accessTokenTtl().toSeconds();
	}

	/**
	 * AT를 검증하고 memberId를 돌려준다.
	 * 만료는 TOKEN_EXPIRED, 서명·형식·발급자·토큰 종류(RT를 넣은 경우 포함) 오류는 INVALID_TOKEN이다.
	 */
	public Long getMemberIdFromAccessToken(String token) {
		Claims claims = parse(token);
		if (!TokenType.ACCESS.getClaimValue().equals(claims.get(TokenType.CLAIM_NAME, String.class))) {
			throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
		}
		return parseMemberId(claims.getSubject());
	}

	private IssuedToken issue(Long memberId, TokenType type, long ttlSeconds) {
		// JWT의 iat·exp는 초 단위라서, DB에 저장할 만료 시각도 같은 값이 되게 초 단위로 자른다.
		Instant issuedAt = clock.instant().truncatedTo(ChronoUnit.SECONDS);
		Instant expiresAt = issuedAt.plusSeconds(ttlSeconds);
		String token = Jwts.builder()
				.issuer(ISSUER)
				.subject(String.valueOf(memberId))
				.id(UUID.randomUUID().toString())
				.claim(TokenType.CLAIM_NAME, type.getClaimValue())
				.issuedAt(Date.from(issuedAt))
				.expiration(Date.from(expiresAt))
				.signWith(key, Jwts.SIG.HS256)
				.compact();
		return new IssuedToken(token, LocalDateTime.ofInstant(expiresAt, TimeConfig.KST));
	}

	private Claims parse(String token) {
		try {
			return parser.parseSignedClaims(token).getPayload();
		} catch (ExpiredJwtException e) {
			throw new BusinessException(AuthErrorCode.TOKEN_EXPIRED);
		} catch (JwtException | IllegalArgumentException e) {
			// 원인(서명·형식 등)은 응답에 구분하지 않는다. 토큰 원문도 로그에 남기지 않는다.
			throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
		}
	}

	private Long parseMemberId(String subject) {
		try {
			long memberId = Long.parseLong(subject);
			if (memberId < 1) {
				throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
			}
			return memberId;
		} catch (NumberFormatException e) {
			throw new BusinessException(AuthErrorCode.INVALID_TOKEN);
		}
	}

	private static SecretKey createKey(String secret) {
		// 환경변수가 없으면 설정 바인딩이 "${JWT_SECRET}"을 문자 그대로 넘긴다.
		if (secret == null || secret.isBlank() || secret.startsWith("${")) {
			throw new IllegalStateException(
					"JWT_SECRET is not set. Add it to backend/.env (see backend/.env.example).");
		}
		try {
			return Keys.hmacShaKeyFor(Decoders.BASE64.decode(secret));
		} catch (RuntimeException e) {
			// 키 값은 메시지에 넣지 않는다. 키가 잘못되면 부팅을 멈춘다.
			throw new IllegalStateException(
					"app.jwt.secret(JWT_SECRET) must be Base64 of at least 32 random bytes.", e);
		}
	}
}
