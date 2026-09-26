package com.whereisit.backend.auth.jwt;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * application.yml의 app.jwt.* 설정.
 *
 * @param secret          HS256 서명 키(Base64, 디코딩 후 32바이트 이상). 기본값을 두지 않아서 없으면 부팅이 실패한다.
 * @param accessTokenTtl  AT 유효시간(명세: 30분)
 * @param refreshTokenTtl RT 유효시간(명세: 14일)
 */
@ConfigurationProperties("app.jwt")
public record JwtProperties(String secret, Duration accessTokenTtl, Duration refreshTokenTtl) {
}
