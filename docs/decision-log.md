# 결정 기록

확정된 사항과 아직 결정하지 않은 사항을 구분합니다. 변경 이력은 실제 결정·승인이 확인될 때 기록합니다.

## 확정된 결정

| 항목 | 결정 |
| --- | --- |
| 프로젝트명 | 어디갔지 Where is it |
| 주제 구분 | 자유주제 |
| 대상 사용자 | 한국에서 물건을 잃어버린 외국인 |
| 데이터 출처 | 경찰청 습득물정보 조회 API |
| 저장소 구조 | Frontend와 Backend Monorepo |
| Branch 전략 | `main`과 Issue별 작업 Branch를 사용하는 GitHub Flow |
| `develop` Branch | 사용하지 않음 |
| Backend Java | Java 17 |
| Backend Framework | Spring Boot 3.5.16 |
| Backend Build Tool | Gradle Wrapper 8.14.5 |
| Java 기본 패키지 | `com.whereisit` |
| Frontend Framework | Next.js 15.5.25 |
| Frontend UI Runtime | React 19.1.x |
| Frontend 언어 | JavaScript |
| Node.js | 24.20.0 |
| Package Manager | npm 11.6.2 |
| Database | MySQL 8.4 (LTS) |
| 로컬 Database 실행 | 팀원 PC에 직접 설치하지 않고 `backend/compose.yaml`(Docker Compose)로 실행. Compose에는 MySQL만 두고 애플리케이션은 로컬에서 실행 |
| Redis | 현재 도입하지 않음 |
| AI 결과 범위 | 경찰청 API가 실제 반환한 후보로 제한 |
| 추적 기간 | 등록 후 7일 |
| 재검색 주기 | 하루 1회 |
| 알림 방식 | 신규 유사 후보 발견 시 이메일 알림 |
| 지원 언어 | 한국어(`ko`)·영어(`en`). API의 `languageCode` 값과 회원 사용 언어로 사용 |
| 시간 기준 | 모든 날짜·시각을 KST(Asia/Seoul)로 저장·처리하고, API 응답의 date-time에는 `+09:00` 오프셋을 포함 |
| Database 논리명 | 업무 용어와 ERD 논리명은 한글을 사용하며, 한글은 문서 설명과 Database COMMENT에도 사용 가능 |
| MySQL 물리 식별자 | 테이블·컬럼·제약조건·인덱스 이름은 소문자 영문 `snake_case` 사용 |
| 애플리케이션 식별자 | Java 클래스는 영문 `PascalCase`, Java 필드와 API JSON 필드는 영문 `camelCase`, Enum 값은 영문 `UPPER_SNAKE_CASE` 사용 |
| 제약조건·인덱스 접두어 | PRIMARY KEY `pk_`, FOREIGN KEY `fk_`, UNIQUE `uk_`, INDEX `idx_`, CHECK `chk_` 사용 |
| 식별자 안전성 | 이해하기 어려운 축약어와 MySQL 예약어 충돌을 피하고, 플랫폼별 대소문자 차이를 방지하기 위해 영문 물리명은 소문자로 통일 |
| Refresh Token 키 정책 | `refresh_tokens.member_id`는 FK이며 단독 UNIQUE가 아님. `token_hash`는 UNIQUE이고 `(member_id, expires_at)`은 비고유 INDEX이며 회원별 복수 기기 세션 허용 |
| 인증 방식 | 이메일·비밀번호 회원가입과 로그인. Spring Security와 JWT(HS256)를 사용하고 AT는 `Authorization: Bearer`로 전달. AT 30분, RT 14일. RT도 JWT이며 로그인마다 기기별로 발급하고 DB에는 SHA-256 해시만 저장. 비밀번호는 BCrypt 해시로 저장. 서명 키는 환경변수 `JWT_SECRET` |

## 미결정 항목

| 항목 | 상태 |
| --- | --- |
| AI API 제공자와 모델 | 미결정 |
| 이메일 발송 서비스 | 미결정 |
| 외부 API 잔여 계약 | 일반·포털 상세 API 계약과 필드 매핑은 확인됨. 공통코드 실제 계약·코드표와 일반 정상 빈 결과·별도 오류 봉투 세부 구조는 미확정 |
| Database 전체 구조와 세부 구현 | 미결정 |
| AI 입출력 JSON Schema | 미결정 |
| 화면 상세 설계 | 미결정 |
| 배포 환경 | 미결정 |

## 변경 이력

| 일자 | 구분 | 결정 또는 변경 내용 | 근거 | 승인자 |
| --- | --- | --- | --- | --- |
| 2026-09-19 | 개발 환경 기준선 | Java 17, Spring Boot 3.5.16, Gradle Wrapper 8.14.5와 Java 기본 패키지 `com.whereisit` 사용 | Issue #6 | 안지산 |
| 2026-09-19 | Frontend 기준선 | Next.js 15.5.25, React 19.1.x, JavaScript, Node.js 24.20.0, npm 11.6.2 사용 | Issue #6 | 안지산 |
| 2026-09-19 | Database 운영 | MySQL을 사용하고 추후 Docker Compose로 제공하며, 현재 Redis는 도입하지 않음 | Issue #6 | 안지산 |
| 2026-09-21 | 로컬 Database 실행 | MySQL 8.4를 `backend/compose.yaml`로 실행하고, Compose에는 MySQL만 둠(애플리케이션 컨테이너·Redis 제외) | Issue #8 | 반정욱(PR #9 머지) |
| 2026-09-22 | 시간 기준 | 모든 날짜·시각을 KST(Asia/Seoul)로 저장·처리하고 API 응답에 `+09:00` 오프셋을 포함. 서버(JVM)와 DB의 시간대를 Asia/Seoul로 고정 | Issue #27 | 안지산(PR #42 승인) |
| 2026-09-22 | 지원 언어 | 지원 언어를 한국어(`ko`)·영어(`en`)로 확정. API 명세 v2 API-01 `languageCode`의 허용값 | API 명세 v2 API-01, Issue #46 | PR 승인 후 기재 |
| 2026-09-24 | Database 식별자와 Refresh Token 키 정책 | 한글 논리명과 영문 물리명을 구분하고 MySQL 물리명 규칙 및 제약조건·인덱스 접두어를 확정. `refresh_tokens.member_id`의 단독 UNIQUE를 배제하고 회원별 복수 기기 세션을 허용 | Issue #31, #32 | 안지산 |
| 2026-09-26 | 인증 방식 | Spring Security + JWT(HS256) 인증 확정. AT 30분·RT 14일, RT는 JWT로 기기별 발급하고 SHA-256 해시만 저장, 서명 키는 `JWT_SECRET` | Issue #28 | 반정욱(PR #45 머지) |
| 2026-09-26 | 습득물 외부 API 실호출 검증 | 일반·포털 습득물 목록·상세 API 실제 호출 성공. 목록 `depPlace` → `storage_place`, 상세 `fdPlace` → `found_place`, `tel` → `storage_phone`, `uniq` → `description` 매핑을 확정. 실제 응답은 `application/xml`, 정상 XML Namespace 없음, 목록·상세 모두 `body/items/item`. NULL 정책은 기존대로 유지. 경찰민원24 개편은 2026-09-26 실제 호출 기준 일반·포털 목록·상세 연동 영향이 확인되지 않음. 공통코드 API는 승인대기로 별도 검증이 필요하며, 일반 정상 빈 결과와 별도 오류 봉투 세부 구조는 미확정 | Issue #41 | 안지산 |
