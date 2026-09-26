package com.whereisit.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import com.whereisit.backend.auth.dto.LoginRequest;
import com.whereisit.backend.auth.dto.SignupRequest;
import com.whereisit.backend.auth.jwt.TokenHasher;
import com.whereisit.backend.auth.repository.RefreshTokenRepository;
import com.whereisit.backend.auth.service.AuthService;

/**
 * 같은 회원이 동시에 로그인해도 모두 성공해야 한다(Issue #48).
 * 만료 RT를 범위 조건으로 지우던 때는 인덱스 갭 락 때문에 데드락이 나서 일부가 500이 됐다.
 *
 * 실제 커밋과 행 잠금이 있어야 재현되므로 테스트 트랜잭션 없이 실행하고, 끝나면 이 테스트가 만든 행만 지운다.
 * testSqlite에서는 실행하지 않는다. 커넥션이 1개라 동시 실행이 되지 않고, SQLite는 행이 아니라 DB 전체를 잠근다.
 */
@SpringBootTest
@DisabledIfSystemProperty(named = "spring.profiles.active", matches = "sqlite",
		disabledReason = "동시 트랜잭션과 행 잠금은 MySQL에서만 확인할 수 있다")
@DisplayName("API-02 같은 회원의 동시 로그인")
class ConcurrentLoginTest {

	private static final String PASSWORD = "Password1!";
	private static final int CONCURRENT_LOGINS = 8;
	/** 데드락은 타이밍에 따라 나므로 여러 번 반복한다. 수정 전 코드에서는 한 번의 동시 로그인에서도 거의 항상 실패했다. */
	private static final int ROUNDS = 5;

	private final String emailPrefix = "concurrent-" + UUID.randomUUID() + "-";

	@Autowired
	private AuthService authService;

	@Autowired
	private RefreshTokenRepository refreshTokenRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@AfterEach
	void deleteCreatedRows() {
		String pattern = emailPrefix + "%";
		jdbcTemplate.update(
				"delete from refresh_tokens where member_id in (select member_id from members where email like ?)", pattern);
		jdbcTemplate.update("delete from members where email like ?", pattern);
	}

	@Test
	@DisplayName("RT가 없는 새 회원이 동시에 로그인해도 모두 성공하고, 요청 수만큼 RT가 저장된다")
	void concurrentLoginsOfNewMember() throws Exception {
		for (int round = 0; round < ROUNDS; round++) {
			String email = emailPrefix + round + "@example.test";
			Long memberId = Long.valueOf(authService.signup(new SignupRequest(email, PASSWORD, "en")).memberId());

			assertThat(loginConcurrently(email)).isEmpty();
			assertThat(refreshTokenRepository.countByMemberId(memberId)).isEqualTo(CONCURRENT_LOGINS);
		}
	}

	@Test
	@DisplayName("만료된 RT가 있는 회원이 동시에 로그인해도 모두 성공하고, 만료 RT는 모두 지워진다")
	void concurrentLoginsWithExpiredTokens() throws Exception {
		for (int round = 0; round < ROUNDS; round++) {
			String email = emailPrefix + "expired-" + round + "@example.test";
			Long memberId = Long.valueOf(authService.signup(new SignupRequest(email, PASSWORD, "en")).memberId());
			insertExpiredTokens(memberId, 3);

			assertThat(loginConcurrently(email)).isEmpty();
			assertThat(refreshTokenRepository.countByMemberId(memberId)).isEqualTo(CONCURRENT_LOGINS);
			assertThat(jdbcTemplate.queryForObject(
					"select count(*) from refresh_tokens where member_id = ? and expires_at < now(6)",
					Integer.class, memberId)).isZero();
		}
	}

	/** 같은 회원으로 동시에 로그인하고, 실패한 요청의 예외 목록을 돌려준다. 모두 성공하면 빈 목록이다. */
	private List<Throwable> loginConcurrently(String email) throws Exception {
		CyclicBarrier startTogether = new CyclicBarrier(CONCURRENT_LOGINS);
		Callable<Void> login = () -> {
			startTogether.await(10, TimeUnit.SECONDS);
			authService.login(new LoginRequest(email, PASSWORD));
			return null;
		};

		ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_LOGINS);
		try {
			List<Future<Void>> results = new ArrayList<>();
			for (int i = 0; i < CONCURRENT_LOGINS; i++) {
				results.add(executor.submit(login));
			}
			List<Throwable> failures = new ArrayList<>();
			for (Future<Void> result : results) {
				try {
					result.get(30, TimeUnit.SECONDS);
				} catch (ExecutionException e) {
					failures.add(e.getCause());
				}
			}
			return failures;
		} finally {
			executor.shutdownNow();
		}
	}

	private void insertExpiredTokens(Long memberId, int count) {
		for (int i = 0; i < count; i++) {
			jdbcTemplate.update(
					"insert into refresh_tokens (created_at, expires_at, member_id, token_hash)"
							+ " values (now(6) - interval 15 day, now(6) - interval 1 day, ?, ?)",
					memberId, TokenHasher.sha256Hex("expired-" + UUID.randomUUID()));
		}
	}
}
