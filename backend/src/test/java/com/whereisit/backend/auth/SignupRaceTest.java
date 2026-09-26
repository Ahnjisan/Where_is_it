package com.whereisit.backend.auth;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.whereisit.backend.member.repository.MemberRepository;
import com.whereisit.backend.support.ApiTestSupport;

/**
 * 같은 이메일로 거의 동시에 가입하면 두 요청 모두 중복 검사(existsByEmail)를 통과할 수 있다.
 * 그때는 DB의 UNIQUE 제약(uk_members_email)이 막고, 서버는 그 위반을 409로 바꿔야 한다.
 * 중복 검사가 "없음"을 돌려주게 만들어 이 상황을 재현한다.
 *
 * testSqlite에서는 실행하지 않는다. Hibernate의 SQLite 방언은 UNIQUE 제약을 DDL에 만들지 않아서
 * 중복 저장이 그대로 성공하기 때문이다. DB 제약에 기대는 동작은 test(MySQL)로만 확인한다.
 */
@DisabledIfSystemProperty(named = "spring.profiles.active", matches = "sqlite",
		disabledReason = "SQLite 방언은 UNIQUE 제약을 만들지 않는다")
@DisplayName("API-01 동시 가입 경합")
class SignupRaceTest extends ApiTestSupport {

	private static final String EMAIL = "race@example.test";

	@MockitoSpyBean
	private MemberRepository memberRepository;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@Test
	@DisplayName("중복 검사를 통과한 뒤 UNIQUE 제약에 걸리면 500이 아니라 409 EMAIL_ALREADY_EXISTS")
	void uniqueViolationBecomesConflict() throws Exception {
		signup(EMAIL, PASSWORD, "en").andExpect(status().isCreated());
		// 먼저 가입한 요청이 아직 커밋되지 않아 중복 검사에서 보이지 않은 것처럼 만든다.
		doReturn(false).when(memberRepository).existsByEmail(EMAIL);

		signup(" Race@Example.test ", PASSWORD, "en")
				.andExpect(status().isConflict())
				.andExpect(jsonPath("$.error.code").value("EMAIL_ALREADY_EXISTS"));

		Integer count = jdbcTemplate.queryForObject("select count(*) from members where email = ?", Integer.class, EMAIL);
		assertThat(count).isEqualTo(1);
	}
}
