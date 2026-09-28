# 개발 환경 및 실행 가이드

> 어디갔지 Where is it의 확정된 개발 환경, 실행 명령 및 로컬 Database 운영 원칙입니다.

## 1. 개발 환경

Frontend와 Backend를 하나의 저장소에서 관리하며 두 프로젝트 모두 기본 구성이 생성되어 있습니다.

| 구분 | 기술 및 버전 | 상태 |
| --- | --- | --- |
| Frontend | Next.js 15.5.25, React 19.1.x, JavaScript | 확정 |
| Frontend Runtime | Node.js 24.20.0, npm 11.6.2 | 확정 |
| Backend | Java 17, Spring Boot 3.5.16, Gradle Wrapper 8.14.5 | 확정 |
| Backend 기본 패키지 | `com.whereisit` | 확정 |
| Database | MySQL 8.4 (LTS), Docker Compose로 로컬 실행 | 확정 |
| 외부 데이터 API | 경찰청 습득물정보 조회 API | 확정 |
| 형상 관리 | GitHub Issues와 Pull Requests를 사용하는 GitHub Flow | 확정 |
| 시간 기준 | KST(Asia/Seoul). API 응답의 date-time에 `+09:00` 오프셋 포함 | 확정 |

## 2. 프로젝트 구조

```text
Where_is_it/
├── frontend/             # Next.js Frontend
├── backend/              # Spring Boot Backend
├── docs/                 # 요구사항 및 개발 문서
├── .github/              # Issue 및 PR 템플릿
├── .gitignore
├── README.md
└── CONTRIBUTING.md
```

## 3. Backend 빌드 및 실행

Database 연결 없이 가능한 기본 검증은 `clean assemble`입니다. 이 명령은 소스 컴파일과 패키징을 수행하지만 테스트나 애플리케이션 실행은 수행하지 않습니다.

`bootRun`과 `test`는 MySQL에 연결하므로 먼저 5장의 절차로 MySQL을 실행합니다. `bootRun`은 종료할 때까지 계속 실행되므로 `test`는 `bootRun`을 종료한 뒤 실행하거나 다른 터미널에서 실행합니다.

### Windows PowerShell

빌드:

```powershell
cd backend
.\gradlew.bat clean assemble
```

애플리케이션 실행:

```powershell
cd backend
.\gradlew.bat bootRun
```

테스트:

```powershell
cd backend
.\gradlew.bat test
```

### macOS/Linux

빌드:

```bash
cd backend
./gradlew clean assemble
```

애플리케이션 실행:

```bash
cd backend
./gradlew bootRun
```

테스트:

```bash
cd backend
./gradlew test
```

### SQLite 옵션 (`testSqlite`)

Docker나 MySQL 없이 Backend 테스트만 빠르게 실행하고 싶을 때 사용합니다. 인메모리 SQLite로 실행하므로 5장의 MySQL 실행 절차가 필요 없습니다. 실제 DBMS 결정(MySQL)에는 영향을 주지 않는 로컬 개발 편의용 옵션입니다.

```bash
cd backend
./gradlew testSqlite
```

`test`(MySQL)와의 차이는 다음과 같습니다.

| 구분 | `test` | `testSqlite` |
| --- | --- | --- |
| Database | MySQL 8.4 (Docker Compose) | SQLite 인메모리 |
| 사전 준비 | 5장의 Docker Compose 실행 | 없음 |
| 커넥션 풀 | 기본 설정 | 1개로 고정 |
| UNIQUE·FK 제약 | 엔티티 정의대로 생성 | 생성되지 않음(Hibernate SQLite 방언의 한계) |
| 용도 | 실제 배포 대상과 동일한 DB로 검증 | Docker 없이 빠른 로컬 확인 |

`testSqlite`는 커넥션이 1개로 고정되어 있어 `@Transactional(propagation = REQUIRES_NEW)`처럼 같은 스레드에서 커넥션 2개를 동시에 요구하는 코드는 실패합니다. 이런 코드가 포함된 테스트는 `test`(MySQL)로만 확인하고, 최종 검증 기준은 항상 `test`입니다.

`testSqlite`는 UNIQUE와 FK 제약도 만들지 않아서 중복·참조 위반이 그대로 저장됩니다. 제약 위반을 확인하는 테스트는 `@DisabledIfSystemProperty(named = "spring.profiles.active", matches = "sqlite")`를 붙여 `testSqlite`에서 제외합니다(예: `SignupRaceTest`).

### API 문서 (Swagger UI)

`bootRun`으로 실행한 뒤 브라우저에서 확인합니다. 문서는 springdoc-openapi가 Controller에서 자동으로 만듭니다.

| 항목 | 주소 |
| --- | --- |
| Swagger UI | http://localhost:8080/swagger-ui.html |
| OpenAPI JSON | http://localhost:8080/v3/api-docs |

- 문서는 로그인 없이 볼 수 있습니다. 보호 API를 호출하려면 로그인(API-02) 응답의 `accessToken`을 오른쪽 위 **Authorize**에 넣습니다. `Bearer ` 접두어 없이 토큰만 넣습니다.
- 넣은 토큰은 새로고침해도 이 브라우저에 남습니다. 공용 PC에서는 **Logout**으로 지웁니다.
- API를 추가하거나 바꾸면 Controller의 `@Operation`·`@ApiResponses` 설명도 함께 고칩니다. 오류 응답의 본문 형식(`ErrorResponse`)은 `OpenApiConfig`가 한 번에 붙이므로 상태 코드와 오류 코드만 적습니다.

## 4. Frontend 설치, 빌드 및 실행

Frontend는 Node.js 24.20.0과 npm 11.6.2를 기준으로 합니다. `.nvmrc`와 `package.json`에 버전 기준이 기록되어 있습니다.

Windows, macOS 및 Linux에서 다음 명령을 사용합니다.

```bash
cd frontend
npm ci
npm run build
npm run dev
```

개발 서버의 기본 주소는 http://localhost:3000 입니다. `npm ci`는 `package-lock.json`과 정확히 일치하도록 의존성을 설치합니다.

Lint와 테스트는 다음 명령으로 실행합니다. `lint`는 ESLint(`eslint-config-next`)를, `test`는 Vitest와 Testing Library를 사용합니다.

```bash
cd frontend
npm run lint
npm run test
```

## 5. MySQL 로컬 실행

### 5.1 운영 원칙

- Database는 MySQL 8.4(LTS)를 사용합니다.
- 팀원 PC에 MySQL을 직접 설치하지 않고, `backend/compose.yaml`로 모두 같은 MySQL 환경을 실행합니다.
- Compose에는 MySQL만 둡니다. 애플리케이션은 컨테이너로 만들지 않고 `bootRun` 또는 IDE로 실행합니다.
- 현재 Redis는 사용하지 않습니다.
- 시각은 모두 KST(Asia/Seoul)로 저장합니다. MySQL은 `compose.yaml`의 `TZ`로, 서버는 애플리케이션이 시작할 때 기본 시간대를 Asia/Seoul로 고정합니다. 실행하는 PC나 CI 러너의 시간대가 무엇이든 저장·응답 시각은 KST입니다.

### 5.2 처음 한 번 준비

1. Docker Desktop(Windows·macOS) 또는 Docker Engine과 Compose(Linux)를 설치하고 실행합니다.
2. `backend/.env.example`을 `backend/.env`로 복사하고 `DB_USERNAME`·`DB_PASSWORD`·`JWT_SECRET` 값을 채웁니다. 작성 규칙은 6장을 따릅니다.

Windows PowerShell:

```powershell
cd backend
Copy-Item .env.example .env
```

macOS/Linux:

```bash
cd backend
cp .env.example .env
```

### 5.3 실행, 중지, 초기화

아래 명령은 `backend/`에서 실행하며 Windows, macOS, Linux에서 같습니다.

| 작업 | 명령 | 설명 |
| --- | --- | --- |
| 실행 | `docker compose up -d --wait` | MySQL이 접속을 받을 수 있는 상태(healthy)가 되면 끝납니다. 처음에는 이미지 다운로드와 초기화로 더 오래 걸립니다. |
| 상태 확인 | `docker compose ps` | `STATUS`에 `(healthy)`가 보이면 정상입니다. |
| 로그 확인 | `docker compose logs mysql` | 기동에 실패했을 때 원인을 확인합니다. |
| 중지 | `docker compose down` | 컨테이너만 지웁니다. 데이터는 볼륨에 남습니다. |
| 초기화 | `docker compose down -v` | 볼륨까지 지웁니다. 로컬 DB 데이터가 모두 사라집니다. |

| 항목 | 값 |
| --- | --- |
| 접속 주소 | `127.0.0.1:3306` (이 PC에서만 접속 가능) |
| Database | `where_is_it` |
| 계정 | `.env`의 `DB_USERNAME`·`DB_PASSWORD`. 볼륨이 처음 만들어질 때 생성됩니다. |
| root 계정 | 사용하지 않습니다. 비밀번호는 무작위로 만들어지며 첫 실행 로그에만 출력되고, 컨테이너를 다시 만들면 확인할 수 없습니다. |
| 문자셋 | `utf8mb4` (MySQL 8.4 기본값, 한글·이모지 저장 가능) |
| 시간대 | `Asia/Seoul` |
| 데이터 볼륨 | `where-is-it_mysql-data` |

DBeaver, MySQL Workbench 같은 DB 도구로 접속할 때도 위 주소와 `.env`의 계정을 사용합니다. 컨테이너 안에서 직접 조회할 때는 다음 명령을 사용합니다. `YOUR_DB_USERNAME`에는 `.env`의 `DB_USERNAME` 값을 넣고, 비밀번호는 프롬프트에 입력합니다.

```bash
docker compose exec mysql mysql -u YOUR_DB_USERNAME -p where_is_it
```

### 5.4 문제 해결

| 증상 | 원인과 해결 |
| --- | --- |
| `required variable DB_USERNAME is missing a value` (또는 `DB_PASSWORD`) | `backend/.env`가 없거나 값이 비어 있습니다. 5.2 절차로 `.env`를 만들고 값을 채웁니다. |
| `ports are not available`, `port is already allocated`, `address already in use` | PC에 설치된 MySQL·MariaDB 서비스나 다른 Compose 스택이 3306을 쓰고 있습니다. 해당 서비스나 컨테이너를 중지한 뒤 다시 실행합니다. |
| `bootRun`·`test`에서 `Access denied for user` | 계정 정보는 볼륨이 처음 만들어질 때만 적용됩니다. `.env`의 계정·비밀번호를 바꿨거나 예전에 만든 볼륨이 남아 있다면 `docker compose down -v`로 초기화한 뒤 다시 실행합니다. 이 경우에도 컨테이너는 healthy로 보이므로 상태만으로는 알 수 없습니다. |
| `bootRun`·`test`에서 `Unable to determine Dialect without JDBC metadata` | 이 메시지 아래의 원인(`Caused by`)을 확인합니다. `Communications link failure`이면 MySQL이 꺼져 있으므로 `docker compose up -d --wait`를 먼저 실행합니다. `Public Key Retrieval is not allowed`이면 `DB_URL`에서 `useSSL=false`를 지우고 `.env.example`의 값을 그대로 사용합니다. |
| 한글·이모지가 깨져서 저장됨 | 애플리케이션(JDBC)과 컨테이너 안의 `mysql` 명령은 utf8mb4로 접속합니다. 다른 클라이언트로 직접 넣을 때는 접속 문자셋이 utf8mb4인지 확인하고, `mysql` 클라이언트라면 `--default-character-set=utf8mb4`를 붙입니다. |

## 6. 환경변수 관리

실제 환경변수 파일과 비밀값은 Git에 올리지 않습니다. API Key, Database 계정·비밀번호, Access·Refresh Token 및 이메일 인증정보를 코드나 문서에 기록하지 않습니다.

- Backend 설정값은 운영체제 환경변수 또는 승인된 로컬 실행 환경으로 주입합니다.
- `backend/.env.example`은 변수명과 형식만 설명하며 실제 값을 포함하지 않습니다.
- Frontend에는 브라우저에 공개 가능한 설정만 둡니다.
- Frontend에서 실제 환경변수가 필요해질 때만 값이 비어 있는 `frontend/.env.example`을 추가합니다.
- `.env.example`은 추적할 수 있지만 실제 `.env`와 `.env.*` 파일은 Git에서 제외합니다.
- `backend/.env`는 Spring과 Docker Compose가 함께 읽습니다. 두 도구가 값을 다르게 읽지 않도록 따옴표·`export`·줄 끝 주석을 쓰지 않고, 값에 `$`, `\`, `#`, 공백을 넣지 않습니다. 예를 들어 Compose는 `ab$cd`를 `ab`로 읽지만 Spring은 그대로 읽어서 두 쪽의 비밀번호가 달라집니다.
- `DB_USERNAME`에는 `root`를 쓸 수 없습니다. MySQL 컨테이너가 이 이름으로 일반 계정을 새로 만들기 때문입니다.
- `JWT_SECRET`은 로그인 토큰(JWT)의 서명 키입니다. 32바이트 이상의 무작위 값을 Base64로 넣습니다. 만드는 명령은 `backend/.env.example`에 있으며, 각자 로컬에서 만들고 팀원과 공유하지 않습니다. 기본값이 없어서 비어 있으면 `bootRun`이 `JWT_SECRET is not set`으로 실패합니다. 키를 바꾸면 이미 발급된 토큰은 모두 무효가 됩니다.
- 테스트(`test`·`testSqlite`)는 `backend/src/test/resources/config/application.yml`의 테스트 전용 키를 쓰므로 `JWT_SECRET`이 없어도 실행됩니다. 이 키는 공개된 값이므로 실행 환경에 쓰지 않습니다.
- `docker compose config`는 `.env`의 비밀번호를 그대로 출력합니다. 설정을 검증할 때는 `docker compose config --quiet`를 사용합니다.
- MySQL 첫 실행 로그에는 무작위 root 비밀번호가 출력됩니다. 로그를 PR이나 Discord에 붙일 때는 이 줄을 빼고 붙입니다.

## 7. 검증 기준

기본 환경 변경 후 다음을 확인합니다.

```text
[ ] Backend clean assemble 성공
[ ] docker compose config --quiet 통과
[ ] docker compose up -d --wait 후 MySQL이 healthy
[ ] Backend bootRun이 MySQL에 연결되어 시작됨
[ ] Backend test 통과
[ ] Frontend npm ci 성공
[ ] Frontend npm run build 성공
[ ] Frontend 개발 서버가 http://localhost:3000 에서 시작됨
[ ] Backend Main·Test package가 com.whereisit.backend로 일치
[ ] Frontend 프로젝트명이 package.json과 package-lock.json에서 일치
[ ] Next.js 15.5.25와 React 19.1.x 유지
[ ] 실제 환경변수 파일이 Git에서 제외됨
[ ] .env.example이 추적 가능함
[ ] 실제 비밀번호·API Key·Token이 추적 파일에 없음
```

## 8. OpenAI 자연어 검색조건 설정

API-05 통합 검색은 Backend에서 OpenAI Responses API를 호출해 분실 날짜·장소, 물품명 검색어와 보관 장소 검색어를 구조화하고 사용자 언어의 ASSISTANT 메시지를 생성한 뒤, 포털기관 목록 2번 API 조회와 후보 랭킹까지 한 흐름에서 처리합니다. API Key는 Frontend로 전달하지 않습니다.

| 환경변수 | 필수 여부 | 설명 |
| --- | --- | --- |
| `OPENAI_API_KEY` | AI 호출 시 필수 | OpenAI 인증 Key. 코드·로그·응답에 기록하지 않습니다. |
| `OPENAI_MODEL` | AI 호출 시 필수 | 기본 운영 모델은 `gpt-4o-mini`입니다. `.env.example`은 이 값을 예시로 제공하지만 Java 코드와 `application.yml`에는 기본값이 없습니다. |
| `OPENAI_TIMEOUT` | 선택 | 연결·응답 제한시간. 기본값은 `10s`입니다. |

Key 또는 model이 없어도 애플리케이션은 시작하지만 AI 기능 호출은 `AI_CONDITION_UNAVAILABLE`로 실패합니다. OpenAI와 포털기관 HTTP 호출은 DB 트랜잭션 밖에서 실행하며, USER 메시지는 외부 호출 전에 commit하고 AI 조건·ASSISTANT 메시지와 FoundItem 원본 캐시만 각각 짧은 트랜잭션으로 반영합니다. OpenAI 호출은 자동 재시도하지 않습니다.

Timeout과 OpenAI HTTP 408·504는 `SEARCH_TIMEOUT`, HTTP 429는 `RATE_LIMITED`, 그 밖의 HTTP 오류·refusal·빈 응답·잘못된 JSON 또는 Schema 위반은 `AI_CONDITION_UNAVAILABLE`로 응답합니다.

테스트는 Fake Port와 Spring HTTP Mock만 사용합니다. 테스트에서 실제 OpenAI Key·모델을 사용하거나 OpenAI·경찰청·포털기관 외부 네트워크를 호출하지 않습니다.

### 후보 랭킹과 추천 이유

이 절의 20건 사전 축약·AI score·서버 rank·fallback 정책(Issue #80)은 API-05(`POST /api/lost-items`) 초기 검색에만 적용하며, API-05 전용 `CandidatePreselector`·`InitialSearchCandidateRanker`·`InitialSearchRuleScorer`(`candidate.ranking.initial` 패키지)로 격리합니다. 신규 Frontend에서 사용하지 않는 Legacy API-11은 기존 `CandidateRanker`(`OpenAiCandidateRanker`, 최대 100건·AI rank 검증)와 `SimpleTextSimilarityRanker`, 기존 오류 전파·Candidate 저장 정책을 그대로 사용합니다.

API-05에서는 포털기관 목록 2번 API(`getPtLosfundInfoAccTpNmCstdyPlace`)가 반환한 ItemList(1회 최대 100건)를 자연키 `(sourceType, atcId, fdSn)`로 중복 제거한 뒤, `CandidatePreselector`가 규칙 점수 내림차순 → 습득일 최신순 → 출처 → `atcId` → `fdSn` 순서로 정렬해 상위 최대 20건만 OpenAI 평가 대상으로 고릅니다. 자연키가 마지막 기준이므로 외부 응답·DB·컬렉션 순서와 무관하게 같은 입력은 같은 20건과 같은 순서를 만듭니다. 20건을 넘으면 `RESULT_LIMIT_REACHED` warning을 한 번만 반환하고, 응답 `candidates`와 FoundItem 원본 캐시도 이 20건으로 제한됩니다. 이 API에는 공식 파라미터 `PRDT_NM`(물품명), `DEP_PLACE`(보관 장소), `pageNo`, `numOfRows`만 전달하며 날짜는 지원하지 않으므로 임의 query parameter로 보내지 않습니다. OpenAI 입력에는 분실 설명·언어·기간·장소와 후보의 상품명·제목·분류·색상·습득일·보관장소만 포함하며, DB ID와 외부 관리번호는 요청 범위의 `c1` 형식 key로 대체합니다.

규칙 점수는 비교용 projection에서만 NFKC 정규화, 소문자화, 공백·구두점·기호의 단일 공백화를 적용하며 원본 문자열과 DB 값은 바꾸지 않습니다. 설명 token이 후보 token과 같거나, 공백을 제거한 후보 필드에 포함되거나(`검은색 지갑` ↔ `검은색지갑`, `서울` ↔ `서울역`), 2글자 이상 후보 token을 포함하면(`서울역` ↔ `서울`) 일치로 계산합니다.

API-05 내부 후보 평가도 Responses API, `store=false`, strict JSON Schema를 사용하며 모델은 `OPENAI_MODEL`(현재 `gpt-4o-mini`)만 사용합니다. Schema는 요청 후보 수 N에 맞춰 동적으로 만들며 root `candidates` object의 properties와 required가 정확히 `c1..cN`이고, 각 후보는 `score`(0~100 integer), `isSimilar`(boolean), `reason`만 허용합니다(`additionalProperties=false`). AI는 rank나 순서를 만들지 않으며, 서버가 AI `score` 내림차순 → 규칙 점수 내림차순 → 습득일 최신순 → 사전 축약 순서(출처·`atcId`·`fdSn` 포함)로 정렬해 `rank=1..N`을 부여합니다. AI 응답 object의 key 순서는 사용하지 않습니다. 추천 이유는 1~500 Unicode code point이고 한국어는 한글, 영어는 Latin 문자를 하나 이상 포함해야 하며 HTML·제어문자를 거부합니다. 후보 key 누락·추가·대소문자 변경, 중복 JSON key, 문자열·실수·범위 밖 score, 추가 필드를 서버에서 다시 검증하며 최종 HTTP 요청 body가 128 KiB를 초과하면 호출하지 않습니다. `max_output_tokens`는 설정하지 않습니다. Prompt와 OpenAI 응답 원문은 로그에 남기지 않습니다.

후보가 없으면 OpenAI를 호출하지 않고 `rankingStatus=NOT_RUN`입니다. API-05 후보 평가는 후보 조회 뒤의 보조 단계이므로 timeout·HTTP 408·504·429·연결 실패·그 밖의 HTTP 오류·Key/model 미설정·refusal·incomplete·output limit·JSON·Schema·후보·score·reason 검증 실패를 모두 API-05 전체 실패로 전파하지 않습니다. HTTP 201을 유지하고 같은 20건 후보 집합의 `InitialSearchRuleScorer` 규칙 기반 결과로 fallback하며 `rankingStatus=UNAVAILABLE`, `AI_RANKING_UNAVAILABLE` warning을 반환합니다. API-05 검색조건 구조화의 기존 오류 매핑(timeout·408·504 → `SEARCH_TIMEOUT` 504, 429 → `RATE_LIMITED` 429, 실패 시 포털기관 미호출)은 그대로 유지하며 두 단계의 실패 정책을 혼합하지 않습니다. Legacy API-11 후보 랭킹의 timeout·429 오류 매핑도 변경하지 않습니다. 자동 모델 fallback이나 자동 재시도는 사용하지 않습니다.

실패 단계는 공개 응답에 노출하지 않는 내부 category(`CONFIG_MISSING`, `REQUEST_INVALID`, `REQUEST_TOO_LARGE`, `HTTP_ERROR`, `CONNECTION_ERROR`, `TIMEOUT`, `RATE_LIMITED`, `REFUSAL`, `INCOMPLETE`, `OUTPUT_LIMIT`(incomplete reason `max_output_tokens`), `ENVELOPE_INVALID`, `JSON_INVALID`, `SCHEMA_INVALID`, `CANDIDATE_SET_INVALID`, `SCORE_INVALID`, `REASON_INVALID`)로 `InitialSearchCandidateRanker` WARN 로그에 남깁니다(timeout·408·504는 `TIMEOUT`, 429는 `RATE_LIMITED`). 로그 필드는 category, HTTP status, 후보 수, 요청 byte 수, 알려진 응답 status·incomplete reason(그 밖의 값은 `other`)과 로그 패턴의 requestId뿐이며 API Key·Authorization·사용자 설명·Prompt·후보 필드값·응답 본문·외부 예외 메시지는 기록하지 않습니다. 후보 표시 필드의 개인정보 마스킹은 별도 보안 Issue에서 다룹니다.

포털기관 조회, 20건 사전 축약, OpenAI 평가와 서버 순위 산출은 DB 트랜잭션 밖에서 실행합니다. 응답 확정 직전 짧은 트랜잭션에서 분실물 row를 `PESSIMISTIC_WRITE`로 잠그고 snapshot을 다시 확인하며, 변경됐다면 `ITEM_BUSY`(409)를 반환합니다. 같은 잠금 범위에서는 Frontend 카드의 안정적인 `foundItemId`를 위한 FoundItem 외부 원본 캐시만 upsert합니다. API-05는 LostItemCandidate를 생성하거나 current/baseline을 변경하지 않습니다.

API-05 응답은 `lookupStatus`, `rankingStatus`, `persisted`, `warnings`, `candidates`를 최상위에 두는 평면 계약을 유지합니다. `candidates`는 현재 Frontend 호환용 이름일 뿐 Candidate DB 저장을 뜻하지 않으며 `candidateId=null`, `isCurrent=false`, `isBaseline=false`, `persisted=false`입니다. `foundItem.openId`는 포털기관 `atcId`와 같고 `fdSn`은 문자열 원형을 보존합니다. 결과는 API-05 응답과 Frontend 메모리 Context에서만 유지되어 새로고침 시 만료되며, 재검색하면 새 `lostItemId`가 생성될 수 있습니다. 결과 재조회 API와 Snapshot·중복 알림 정책은 후속 작업입니다.

API-11~15는 신규 Frontend 공개 흐름에서 사용하지 않습니다. API-07, `GET /api/lost-items/{lostItemId}/results`는 구현하지 않았습니다. API-17 7일 추적 활성화(`POST /api/lost-items/{lostItemId}/tracking`)는 구현되어 있으며 9절의 방식으로 동작합니다.

API-16 `GET /api/members/me/lost-items`는 로그인 회원의 추적 등록 건(`TRACKING`·`EXPIRED`)만 생성 시각 DESC, 분실물 ID DESC로 조회합니다. `SEARCHING`, 논리 삭제 건, 다른 회원의 건은 제외하고 `status`·`sort` 요청값은 받지 않습니다. `page`(0부터)·`size`(기본 20, 최대 50, 초과 시 50)만 쓰며 응답은 `LostItemPage`입니다. 만료 시각이 지난 `TRACKING`은 응답에서만 `EXPIRED`로 표시하고 DB 상태는 바꾸지 않으며, 외부 API를 호출하지 않습니다.

## 9. 추적 재검색과 이메일 알림

추적 활성화(API-17)와 매일 09:00(KST)에 실행되는 추적 배치는 같은 방식으로 후보를 찾습니다.

1. 포털기관 목록 1번 API(`getPtLosfundInfoAccToClAreaPd`)를 분류·색상·지역 코드 없이 `START_YMD`~`END_YMD`(습득일)만으로 호출하고, `numOfRows=5000`으로 모든 페이지를 받습니다.
2. `lost_items`에 저장된 물품명 검색어가 물품명(`fdPrdtNm`) 또는 분류명(`prdtClNm`)에 포함되고, 보관장소 검색어가 있으면 보관장소(`depPlace`)에 포함되며, 습득일이 `search_start_date` 이후인 습득물을 고릅니다. 대소문자와 공백 차이는 무시합니다.
3. 조회 시작일은 `search_start_date`이며 최대 30일 전으로 제한합니다. 습득 후 늦게 등록되는 습득물이 있어 매번 시작일부터 오늘까지 전체를 다시 조회합니다.

| 구분 | 동작 |
| --- | --- |
| 추적 활성화 | 물품명 검색어가 없으면 `INVALID_SEARCH_CONDITION`. `search_start_date`가 없으면 `lost_date_from`, 그것도 없으면 검색 건 생성일로 채웁니다. 매칭 결과를 기준 후보(`is_baseline=1`)로 저장하고 당일을 `last_auto_search_date`로 기록합니다. 포털기관 조회 실패는 `LOST_API_UNAVAILABLE`입니다. |
| 일일 배치 | 만료 시각이 지난 TRACKING 건은 검색 없이 EXPIRED로 바꿉니다. 오늘 재검색하지 않은 TRACKING 건 전체에 대해 포털기관 조회는 한 번만 하고, 건마다 짧은 트랜잭션으로 후보를 반영합니다. 조회가 실패하면 `last_auto_search_date`를 갱신하지 않습니다. |
| 새 후보 | 이 분실물에 처음 나타난 습득물입니다. 새 후보가 있으면 `(lost_item_id, notification_date)`당 이메일_알림 1건을 만들고 분실물의 사용 언어로 발송합니다. |
| 발송 결과 | 성공은 `SENT`, 실패는 `FAILED`와 오류 코드(예외 클래스 이름)로 기록합니다. `SENT`는 메일 제공자 접수를 뜻하며 수신·열람을 보장하지 않습니다. |
| 알림 이력 | API-15 `GET /api/lost-items/{lostItemId}/notifications`로 조회합니다. |

포털기관 조회와 메일 발송은 DB 트랜잭션 밖에서 실행합니다.

### 메일 발송 설정

| 환경변수 | 필수 여부 | 설명 |
| --- | --- | --- |
| `MAIL_HOST` | 발송 시 필수 | SMTP 서버 주소. 기본값은 `localhost`입니다. |
| `MAIL_PORT` | 선택 | 기본값은 `587`(STARTTLS)입니다. |
| `MAIL_USERNAME` | 발송 시 필수 | SMTP 계정. |
| `MAIL_PASSWORD` | 발송 시 필수 | SMTP 비밀번호. Gmail은 앱 비밀번호를 사용합니다. 코드·로그·문서에 기록하지 않습니다. |

값이 없어도 애플리케이션은 시작하며 발송만 `FAILED`로 기록됩니다. SMTP 연결·읽기·쓰기 제한시간은 각 10초입니다. 테스트는 실제 SMTP를 호출하지 않고 `EmailSender`를 Mock으로 대체합니다.

## 10. 핵심 검색 Browser E2E

Issue #89의 E2E는 실제 Chromium과 Next.js, Spring Security·Controller·Service·Repository를 연결하되 Backend는 프로세스 전용 인메모리 SQLite를 사용합니다. OpenAI 조건 추출, 포털기관 조회, 후보 평가는 `src/test`의 결정적 Stub으로 교체하고 SMTP와 Scheduling도 실행하지 않습니다. 따라서 실제 OpenAI·공공데이터 Key, DB 계정, 메일 계정 및 `.env`가 필요하지 않으며 외부 API를 호출하지 않습니다.

로컬에서는 세 터미널에서 Backend, Frontend, Playwright 순서로 실행합니다.

```powershell
cd backend
.\gradlew.bat e2eServer
```

```powershell
cd frontend
npm run dev -- --hostname 127.0.0.1
```

```powershell
cd frontend
npm run test:e2e -- e2e/core-search.e2e.js --project=chromium
```

CI는 같은 테스트 전용 Backend를 8080, 빌드된 Frontend를 3000에서 시작하고 readiness 확인 후 Chromium 한 worker로 E2E를 실행합니다. trace와 video는 저장하지 않으며 실패 screenshot만 `frontend/test-results`에서 Artifact로 보존합니다. E2E Backend 종료 시 인메모리 SQLite의 회원·검색·후보 원본 캐시도 함께 폐기됩니다.
