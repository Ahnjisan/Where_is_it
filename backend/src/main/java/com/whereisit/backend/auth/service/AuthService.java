package com.whereisit.backend.auth.service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.whereisit.backend.auth.dto.LoginRequest;
import com.whereisit.backend.auth.dto.LoginResponse;
import com.whereisit.backend.auth.dto.SignupRequest;
import com.whereisit.backend.auth.entity.RefreshToken;
import com.whereisit.backend.auth.error.AuthErrorCode;
import com.whereisit.backend.auth.jwt.IssuedToken;
import com.whereisit.backend.auth.jwt.JwtTokenProvider;
import com.whereisit.backend.auth.jwt.TokenHasher;
import com.whereisit.backend.auth.repository.RefreshTokenRepository;
import com.whereisit.backend.global.error.BusinessException;
import com.whereisit.backend.global.error.CommonErrorCode;
import com.whereisit.backend.member.dto.MemberResponse;
import com.whereisit.backend.member.entity.LanguageCode;
import com.whereisit.backend.member.entity.Member;
import com.whereisit.backend.member.error.MemberErrorCode;
import com.whereisit.backend.member.repository.MemberRepository;

@Service
public class AuthService {

	private final MemberRepository memberRepository;
	private final RefreshTokenRepository refreshTokenRepository;
	private final PasswordEncoder passwordEncoder;
	private final JwtTokenProvider jwtTokenProvider;
	private final Clock clock;

	/**
	 * 없는 이메일로 로그인해도 비밀번호 비교를 한 번 하게 만드는 해시.
	 * 비교를 건너뛰면 응답 시간 차이로 가입 여부가 드러난다.
	 */
	private final String dummyPasswordHash;

	public AuthService(MemberRepository memberRepository, RefreshTokenRepository refreshTokenRepository,
			PasswordEncoder passwordEncoder, JwtTokenProvider jwtTokenProvider, Clock clock) {
		this.memberRepository = memberRepository;
		this.refreshTokenRepository = refreshTokenRepository;
		this.passwordEncoder = passwordEncoder;
		this.jwtTokenProvider = jwtTokenProvider;
		this.clock = clock;
		this.dummyPasswordHash = passwordEncoder.encode("timing-equalizer-password");
	}

	/** API-01. 가입 응답에는 토큰을 넣지 않는다. */
	@Transactional
	public MemberResponse signup(SignupRequest request) {
		LanguageCode languageCode = LanguageCode.fromCode(request.languageCode())
				.orElseThrow(() -> new BusinessException(CommonErrorCode.UNSUPPORTED_LANGUAGE));
		if (memberRepository.existsByEmail(request.email())) {
			throw new BusinessException(MemberErrorCode.EMAIL_ALREADY_EXISTS);
		}

		Member member = Member.create(request.email(), passwordEncoder.encode(request.password()), languageCode);
		try {
			// 같은 이메일로 동시에 가입하면 위 검사를 둘 다 통과한다. 그때는 UNIQUE 제약이 막고 409로 바꾼다.
			memberRepository.saveAndFlush(member);
		} catch (DataIntegrityViolationException e) {
			throw new BusinessException(MemberErrorCode.EMAIL_ALREADY_EXISTS);
		}
		return MemberResponse.from(member);
	}

	/**
	 * API-02. 로그인할 때마다 RT를 새로 추가한다(기기별 세션). 다른 기기의 RT는 그대로 두고,
	 * 이 회원의 만료된 RT만 함께 지운다.
	 */
	@Transactional
	public LoginResponse login(LoginRequest request) {
		Member member = memberRepository.findByEmail(request.email()).orElse(null);
		if (member == null) {
			passwordEncoder.matches(request.password(), dummyPasswordHash);
			throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
		}
		if (!passwordEncoder.matches(request.password(), member.getPasswordHash())) {
			throw new BusinessException(AuthErrorCode.INVALID_CREDENTIALS);
		}

		deleteExpiredRefreshTokens(member.getId());

		IssuedToken accessToken = jwtTokenProvider.issueAccessToken(member.getId());
		IssuedToken refreshToken = jwtTokenProvider.issueRefreshToken(member.getId());
		refreshTokenRepository.save(
				RefreshToken.issue(member, TokenHasher.sha256Hex(refreshToken.value()), refreshToken.expiresAt()));

		return LoginResponse.of(accessToken.value(), jwtTokenProvider.accessTokenTtlSeconds(),
				refreshToken.value(), MemberResponse.from(member));
	}

	/**
	 * 이 회원의 만료된 RT를 지운다. 만료 행의 ID를 먼저 읽고 PK로 지워서, 지우는 행에만 잠금이 걸리게 한다.
	 * 범위 조건 DELETE는 갭 락 때문에 같은 회원의 동시 로그인에서 데드락이 났다(Issue #48).
	 */
	private void deleteExpiredRefreshTokens(Long memberId) {
		List<Long> expiredIds = refreshTokenRepository.findExpiredIdsByMemberId(memberId, LocalDateTime.now(clock));
		if (!expiredIds.isEmpty()) {
			refreshTokenRepository.deleteAllByIdInBatch(expiredIds);
		}
	}
}
