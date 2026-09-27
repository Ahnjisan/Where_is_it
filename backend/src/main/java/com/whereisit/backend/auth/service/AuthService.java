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
import com.whereisit.backend.auth.dto.RefreshTokenRequest;
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
		return issueTokens(member);
	}

	/**
	 * API-18. 요청의 RT를 지우고 새 AT·RT를 발급한다(회전). 새 RT의 만료는 지금부터 다시 14일이다.
	 * 한 트랜잭션이므로, 새 토큰을 저장하다 실패하면 삭제도 롤백되어 기존 RT가 그대로 유효하다.
	 */
	@Transactional
	public LoginResponse refresh(RefreshTokenRequest request) {
		// 서명·만료·토큰 종류는 JWT로 확인한다. DB의 expires_at은 JWT의 exp와 같은 값이다.
		Long memberId = jwtTokenProvider.getMemberIdFromRefreshToken(request.refreshToken());
		Long refreshTokenId = refreshTokenRepository.findIdByTokenHash(TokenHasher.sha256Hex(request.refreshToken()))
				.orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN));

		// 같은 RT로 동시에 요청하면 둘 다 위 조회를 통과한다. PK 행 잠금 때문에 한쪽만 1행을 지우고,
		// 나머지는 앞선 트랜잭션이 끝난 뒤 0행을 지우게 되어 거부된다.
		// 해시 조건으로 바로 DELETE하지 않는 이유: 없는 값이면 유니크 인덱스에 갭 락이 걸린다(Issue #48).
		if (refreshTokenRepository.deleteByIdReturningCount(refreshTokenId) == 0) {
			throw new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN);
		}

		// 해시가 토큰 원문 전체(sub 포함)에서 나오므로, 찾은 행의 회원은 토큰의 sub와 같다.
		Member member = memberRepository.findById(memberId)
				.orElseThrow(() -> new BusinessException(AuthErrorCode.INVALID_REFRESH_TOKEN));
		deleteExpiredRefreshTokens(memberId);
		return issueTokens(member);
	}

	/**
	 * API-19. 요청한 RT 하나만 지운다. 같은 회원의 다른 기기 RT는 그대로 둔다.
	 * RT가 없거나, 만료됐거나, 이미 지워졌어도 성공으로 처리한다(멱등). 그래서 JWT 검증도 하지 않는다.
	 * 이미 발급된 AT는 만료될 때까지 유효하다.
	 */
	@Transactional
	public void logout(RefreshTokenRequest request) {
		refreshTokenRepository.findIdByTokenHash(TokenHasher.sha256Hex(request.refreshToken()))
				.ifPresent(refreshTokenRepository::deleteByIdReturningCount);
	}

	/** 새 AT·RT를 발급하고 RT의 해시를 저장한다. 로그인과 refresh가 같은 응답(LoginData)을 쓴다. */
	private LoginResponse issueTokens(Member member) {
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
