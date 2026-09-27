package com.whereisit.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.reset;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.whereisit.backend.auth.dto.LoginRequest;
import com.whereisit.backend.auth.dto.LoginResponse;
import com.whereisit.backend.auth.dto.RefreshTokenRequest;
import com.whereisit.backend.auth.dto.SignupRequest;
import com.whereisit.backend.auth.entity.RefreshToken;
import com.whereisit.backend.auth.error.AuthErrorCode;
import com.whereisit.backend.auth.jwt.TokenHasher;
import com.whereisit.backend.auth.repository.RefreshTokenRepository;
import com.whereisit.backend.auth.service.AuthService;
import com.whereisit.backend.global.error.BusinessException;

/**
 * TC-35. refresh의 원자성을 실제 커밋·롤백으로 확인한다.
 * 테스트 트랜잭션 안에서는 서비스가 실패해도 삭제가 롤백되지 않고, 동시 요청의 행 잠금도 재현되지 않는다.
 * 그래서 테스트 트랜잭션 없이 실행하고, 끝나면 이 테스트가 만든 행만 지운다.
 */
@SpringBootTest
@AutoConfigureMockMvc
@DisplayName("API-18 refresh의 롤백과 동시 요청")
class RefreshTransactionTest {

	private static final String PASSWORD = "Password1!";
	private static final int CONCURRENT_REFRESHES = 8;
	/** 동시성 문제는 타이밍에 따라 나므로 여러 번 반복한다. */
	private static final int ROUNDS = 5;

	private final String emailPrefix = "refresh-tx-" + UUID.randomUUID() + "-";

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private AuthService authService;

	@MockitoSpyBean
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
	@DisplayName("TC-35 새 RT를 저장하다 DB 오류가 나면 500이고, 기존 RT는 그대로 유효해서 다시 요청하면 200")
	void failedRefreshKeepsOldToken() throws Exception {
		String refreshToken = signupAndLogin(emailPrefix + "rollback@example.test").refreshToken();
		doThrow(new DataAccessResourceFailureException("injected failure"))
				.when(refreshTokenRepository).save(any(RefreshToken.class));

		refresh(refreshToken)
				.andExpect(status().isInternalServerError())
				.andExpect(jsonPath("$.error.code").value("INTERNAL_ERROR"));
		// 요청한 RT의 삭제도 함께 롤백되어 남아 있어야 한다.
		assertThat(countByHash(refreshToken)).isEqualTo(1);

		reset(refreshTokenRepository);
		refresh(refreshToken)
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.data.refreshToken").isNotEmpty());
		assertThat(countByHash(refreshToken)).isZero();
	}

	@Test
	@DisabledIfSystemProperty(named = "spring.profiles.active", matches = "sqlite",
			disabledReason = "동시 트랜잭션과 행 잠금은 MySQL에서만 확인할 수 있다")
	@DisplayName("TC-35 같은 RT로 동시에 refresh하면 하나만 성공하고 나머지는 모두 INVALID_REFRESH_TOKEN")
	void concurrentRefreshWithSameToken() throws Exception {
		for (int round = 0; round < ROUNDS; round++) {
			LoginResponse login = signupAndLogin(emailPrefix + round + "@example.test");
			Long memberId = Long.valueOf(login.member().memberId());

			List<Object> results = refreshConcurrently(login.refreshToken());

			List<LoginResponse> successes = results.stream()
					.filter(LoginResponse.class::isInstance).map(LoginResponse.class::cast).toList();
			assertThat(successes).hasSize(1);
			// 실패는 모두 INVALID_REFRESH_TOKEN이어야 한다. 데드락 같은 다른 예외가 섞이면 안 된다.
			assertThat(results).filteredOn(result -> !(result instanceof LoginResponse))
					.hasSize(CONCURRENT_REFRESHES - 1)
					.allSatisfy(failure -> assertThat(failure)
							.isInstanceOfSatisfying(BusinessException.class,
									e -> assertThat(e.getErrorCode()).isEqualTo(AuthErrorCode.INVALID_REFRESH_TOKEN)));
			// 쓴 RT는 지워지고, 성공한 요청이 발급한 RT 하나만 남는다.
			assertThat(jdbcTemplate.queryForList(
					"select token_hash from refresh_tokens where member_id = ?", String.class, memberId))
					.containsExactly(TokenHasher.sha256Hex(successes.get(0).refreshToken()));
		}
	}

	/** 같은 RT로 동시에 refresh하고, 요청마다 성공 응답(LoginResponse) 또는 실패 예외를 돌려준다. */
	private List<Object> refreshConcurrently(String refreshToken) throws Exception {
		CyclicBarrier startTogether = new CyclicBarrier(CONCURRENT_REFRESHES);
		Callable<LoginResponse> refresh = () -> {
			startTogether.await(10, TimeUnit.SECONDS);
			return authService.refresh(new RefreshTokenRequest(refreshToken));
		};

		ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_REFRESHES);
		try {
			List<Future<LoginResponse>> futures = new ArrayList<>();
			for (int i = 0; i < CONCURRENT_REFRESHES; i++) {
				futures.add(executor.submit(refresh));
			}
			List<Object> results = new ArrayList<>();
			for (Future<LoginResponse> future : futures) {
				try {
					results.add(future.get(30, TimeUnit.SECONDS));
				} catch (ExecutionException e) {
					results.add(e.getCause());
				}
			}
			return results;
		} finally {
			executor.shutdownNow();
		}
	}

	private LoginResponse signupAndLogin(String email) {
		authService.signup(new SignupRequest(email, PASSWORD, "en"));
		return authService.login(new LoginRequest(email, PASSWORD));
	}

	private ResultActions refresh(String refreshToken) throws Exception {
		return mockMvc.perform(post("/api/auth/refresh")
				.contentType(MediaType.APPLICATION_JSON)
				.content(objectMapper.writeValueAsString(Map.of("refreshToken", refreshToken))));
	}

	private Integer countByHash(String refreshToken) {
		return jdbcTemplate.queryForObject("select count(*) from refresh_tokens where token_hash = ?",
				Integer.class, TokenHasher.sha256Hex(refreshToken));
	}
}
