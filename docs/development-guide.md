# 개발 환경 및 실행 가이드

> 어디갔지 Where is it의 현재 개발 환경 상태와 향후 프로젝트 생성 후 확인할 실행 절차입니다. 상태가 '확정'인 항목과 Backend 실행 절차 외의 기술 후보와 명령 예시는 선택 결과나 실행 검증 결과가 아닙니다.

## 1. 개발 환경

Frontend와 Backend를 하나의 저장소에서 관리합니다. GitHub 저장소는 https://github.com/Ahnjisan/Where_is_it 입니다. `frontend/`와 `backend/`에는 각각 프로젝트가 생성되어 있으며, 기술별 확정 여부는 아래 표의 상태를 기준으로 합니다.

| 구분 | 기술 후보 또는 현황 | 상태 |
| --- | --- | --- |
| Frontend | React·Vite 및 JavaScript 또는 TypeScript | 후보·언어 미결정 |
| Backend | Java 17, Spring Boot 3.5.16, Gradle 8.14.5 | 확정 |
| Database | MySQL 8.4 | 확정 |
| Redis | Redis 8.10 | 로컬 구성 완료·용도 미결정 |
| 로컬 인프라 | Docker Compose(MySQL·Redis, 선택적으로 Backend 앱) | 확정 |
| AI API | OpenAI API 또는 기타 생성형 AI API | 제공자·모델 미결정 |
| 외부 데이터 API | 경찰청 습득물정보 조회 API | 확정 |
| 이메일 발송 서비스 | 서비스 미선정 | 미결정 |
| 협업 도구 | GitHub, Notion, Google Sheets, Discord, Miro | 확정 |
| 형상 관리 | GitHub Issues와 Pull Requests를 사용하는 GitHub Flow | 확정 |

Backend(Java·Spring Boot·Gradle)와 Database의 버전은 위 표와 같이 확정되었습니다. Frontend의 실제 사용 기술과 버전은 Frontend 프로젝트 기준으로 기록합니다.

## 2. 사용 기술

### Frontend

React·Vite는 검토 중인 기술입니다. JavaScript와 TypeScript 중 선택하지 않았습니다. HTML5와 CSS3를 사용할 수 있으나 구체적인 프로젝트 구성은 생성 후 확인합니다.

### Backend

Java 17, Spring Boot 3.5.16, Gradle 8.14.5(Gradle Wrapper 포함, Groovy DSL)를 사용하며 Java 패키지는 `com.example.backend`입니다. 의존성과 용도는 `backend/build.gradle`의 주석에 기록되어 있습니다(Web, Validation, Data JPA, Data Redis, Mail, MySQL Driver, Lombok).

start.spring.io는 Spring Boot 4.0 이상만 생성하므로 프로젝트는 4.0.8로 생성한 뒤 3.5.16으로 낮췄습니다. 의존성을 추가할 때는 3.x 스타터 이름을 사용합니다. 예를 들어 4.x의 `spring-boot-starter-webmvc`는 3.x에서 `spring-boot-starter-web`이며, 4.x의 `*-test` 스타터 대신 `spring-boot-starter-test`를 사용합니다. Spring Boot 3.5는 Gradle 7.6.4 이상 또는 8.4 이상의 8.x만 공식 지원하므로 Gradle Wrapper를 9.x로 올리지 않습니다.

### Database

MySQL 8.4를 사용하며 DB 이름은 `where_is_it`입니다. 로컬에서는 `backend/compose.yaml`로 컨테이너를 실행합니다. JPA는 개발용으로 `ddl-auto: update`를 사용하고 `open-in-view: false`로 설정되어 있으므로, 지연 로딩이 필요한 Entity는 Service 계층에서 DTO로 변환해 반환합니다. 테이블 구조는 Database 설계 후 기록합니다.

### Redis

Redis 8.10을 로컬 컨테이너로 실행하고 `spring-boot-starter-data-redis`로 연결할 수 있게 설정했습니다. Redis는 처음 사용할 때 연결되므로 Redis를 사용하는 기능이 없으면 Redis 없이도 애플리케이션이 실행됩니다. 사용 용도는 아직 정하지 않았습니다.

### AI API

OpenAI API 또는 기타 생성형 AI API가 후보입니다. 제공자와 모델은 정하지 않았습니다. AI API는 Frontend에서 직접 호출하지 않고 Backend를 통해 호출합니다.

```text
Frontend → Backend → AI API
```

이 경계는 브라우저에 AI API Key를 노출하지 않기 위한 것입니다.

### 외부 데이터 API

경찰청 습득물정보 조회 API를 사용합니다. 실제 요청·응답 필드와 호출 방법은 API 설계 후 기록합니다.

### 협업 도구

GitHub, GitHub Issues와 Pull Requests, Notion, Google Sheets, Discord, Miro를 사용합니다. 작업 절차는 [협업 규칙](../CONTRIBUTING.md)을 따릅니다.

## 3. 프로젝트 기본 구조

다음은 현재 저장소의 주요 구조입니다.

```text
Where_is_it/
├── frontend/             # Frontend 프로젝트
├── backend/              # Spring Boot 프로젝트
│   ├── src/              # 애플리케이션 코드, 설정(application.yml), 테스트
│   ├── build.gradle      # 의존성과 빌드 설정
│   ├── compose.yaml      # 로컬 MySQL·Redis(·Backend 앱) 컨테이너 구성
│   ├── Dockerfile        # Backend 앱 이미지 빌드
│   └── .env.example      # Backend 환경변수 예시
├── docs/                 # 요구사항 및 협업 관련 문서
├── .github/              # Issue 및 PR 템플릿
├── .gitignore
├── README.md
└── CONTRIBUTING.md
```

세부 디렉터리 구조는 선택한 기술과 프로젝트 생성 결과에 따라 결정합니다. GitHub Actions는 두 프로젝트가 생성되고 로컬 빌드·테스트 명령을 확인한 뒤 별도 Issue에서 구성합니다. 서비스의 하루 1회 재검색은 GitHub Actions 예약 Workflow로 구현하지 않습니다.

## 4. 프로젝트 실행 방법

Backend 실행 절차(4.3)는 실제 프로젝트에서 확인한 명령입니다. Frontend 명령(4.2)은 기술에 맞게 검토할 예시이며, 사용 전 `package.json`의 Script와 설정을 확인해야 합니다. 존재하지 않는 Script를 가정하지 않습니다.

### 4.1 저장소 Clone

저장소 URL과 디렉터리명은 확정되어 있습니다. Clone은 담당 팀원이 협업 절차에 맞춰 수행합니다.

```bash
git clone https://github.com/Ahnjisan/Where_is_it.git
cd Where_is_it
```

### 4.2 Frontend 실행 예시

npm 기반 Frontend 프로젝트가 생성된 경우에 한해 `frontend/`의 `package.json`과 실제 Script를 확인합니다. 그 후 `npm install`, `npm run dev`, `npm run build`의 사용 여부를 결정합니다. 필요한 환경변수 예시 파일이 실제로 생성된 뒤에는 Git Bash·macOS·Linux의 `cp .env.example .env.local` 또는 PowerShell의 `Copy-Item .env.example .env.local`을 사용할 수 있습니다. 현재 `frontend/.env.example`은 없습니다.

Vite를 선택한 경우에도 개발 서버 주소와 포트는 실제 설정으로 확인해야 하며 특정 포트를 기본값으로 확정하지 않습니다.

### 4.3 Backend 실행

JDK 17과 Docker(Compose v2 포함)가 필요합니다. 아래 명령은 모두 `backend/`에서 실행하며 WSL2(Ubuntu) 환경에서 확인했습니다. Windows용 명령은 확인하지 못했습니다.

1. 환경변수 파일을 만들고 `DB_USERNAME`, `DB_PASSWORD`를 채웁니다. 작성 규칙은 5.2를 따릅니다. PowerShell에서는 `Copy-Item .env.example .env`를 사용합니다.

```bash
cp .env.example .env
```

2. MySQL과 Redis를 실행합니다.

```bash
docker compose up -d
```

3. 애플리케이션을 실행하거나 테스트합니다. 테스트도 MySQL에 접속하므로 2단계가 먼저 필요합니다. Windows에서는 `./gradlew.bat`을 사용합니다.

```bash
./gradlew bootRun
./gradlew test
```

애플리케이션은 `http://localhost:8080`에서 실행됩니다. Backend 앱까지 컨테이너로 실행하려면 `docker compose --profile app up -d --build`를, 모두 중지하려면 `docker compose --profile app down`을 사용합니다.

- MySQL 컨테이너는 처음 실행할 때만 `.env`의 계정을 만듭니다. `.env`의 계정이나 비밀번호를 바꾼 뒤에는 `docker compose down -v`로 데이터를 지우고 다시 실행해야 하며, 이때 DB 데이터도 삭제됩니다.
- 포트는 `127.0.0.1`의 3306(MySQL), 6379(Redis), 8080(Backend 앱)을 사용합니다. 이미 사용 중인 포트가 있으면 충돌합니다.

## 5. 환경변수 관리 규칙

실제 환경변수 파일을 GitHub에 올리지 않습니다. AI·공공데이터 API Key, 데이터베이스 주소·계정·비밀번호, Access·Refresh Token, JWT Secret, 이메일 인증정보를 코드와 문서에 값으로 기록하지 않습니다. 인증 기능과 이메일 발송 서비스의 구체 설정은 미결정입니다.

### 5.1 Frontend 환경변수

Frontend에는 공개 가능한 설정만 둡니다. Vite를 선택하면 `VITE_` 접두어의 값은 빌드 결과를 통해 브라우저에 노출될 수 있습니다. `VITE_API_BASE_URL`은 공개 가능한 설정의 이름 **예시**이며 실제 사용 여부와 값은 프로젝트 생성 후 정합니다.

AI_API_KEY, PUBLIC_DATA_API_KEY, DB_PASSWORD, JWT_SECRET, ACCESS_TOKEN, REFRESH_TOKEN과 이메일 인증정보는 Frontend에 넣지 않습니다.

### 5.2 Backend 환경변수

민감정보는 Backend의 실행 환경에 주입합니다. 현재 코드가 참조하는 환경변수는 다음과 같으며, 목록은 `backend/.env.example`과 같게 유지합니다.

| 변수 | 용도 | 필수 여부 |
| --- | --- | --- |
| `DB_URL` | MySQL 접속 URL, 형식: `jdbc:mysql://localhost:3306/where_is_it` | 필수 |
| `DB_USERNAME` | MySQL 계정, `root` 사용 불가 | 필수 |
| `DB_PASSWORD` | MySQL 비밀번호 | 필수 |
| `REDIS_HOST` | Redis 호스트, 기본값 `localhost` | 선택 |
| `REDIS_PORT` | Redis 포트, 기본값 `6379` | 선택 |

`AI_API_KEY`, `PUBLIC_DATA_API_KEY` 등 앞으로 필요한 변수는 해당 기능을 구현할 때 이름을 정합니다. 인증 기능을 채택할 때만 인증 관련 변수를, 이메일 발송 서비스를 정한 뒤에만 필요한 발송 설정 변수를 추가합니다.

`application.yml`의 `spring.config.import` 설정으로 `backend/.env` 파일을 읽으며 별도 dotenv 라이브러리는 사용하지 않습니다. `backend/`에서 실행하면 `.env`를, 저장소 루트에서 실행하면(VS Code 기본값) `backend/.env`를 찾고, 파일이 없으면 건너뜁니다. 같은 이름의 운영체제 환경변수가 있으면 운영체제 환경변수가 우선합니다. Docker Compose도 같은 `backend/.env`를 읽으므로 `.env`는 `KEY=값` 형식으로 한 줄에 하나씩 쓰고, 따옴표·`export`·줄 끝 주석을 붙이지 않으며 값에 `$`를 넣지 않습니다.

### 5.3 `.env.example` 작성 규칙

`backend/.env.example`은 생성되어 있습니다. 다른 `.env.example`도 필요한 경우 다음 규칙에 따라 작성합니다.

- 실제 API Key와 비밀번호를 넣지 않습니다.
- 실제 코드가 참조하는 변수명과 동일하게 씁니다.
- 필요한 경우 값의 형식만 예시로 적습니다.
- 사용하지 않는 환경변수는 미리 추가하지 않습니다.
- 새 환경변수가 생기면 예시 파일도 갱신하고 PR 본문에 변경 사항을 기록합니다.

## 6. API Key 보관 규칙

| 구분 | 관리 방법 |
| --- | --- |
| 실제 API Key·DB 비밀번호·Token | Backend의 승인된 로컬 또는 실행 환경에만 보관 |
| Frontend 환경변수 | 공개 가능한 설정만 사용 |
| `.env.example` | 실제 값 없이 변수명과 필요한 형식만 기록 |
| GitHub·저장소 문서·README | 실제 비밀값 기록 금지 |
| Notion·Google Sheets·발표자료·AI Agent Prompt | 실제 비밀값 기록 금지 |
| 코드·로그·테스트 결과 | 실제 비밀값 기록 금지 |
| 팀원 공유 | 팀장이 지정한 안전한 방식 사용 |

실제 환경변수 파일은 Git에서 제외합니다. 현재 루트 `.gitignore`는 `.env`, `.env.*`를 제외하고 `.env.example`은 허용합니다. 프로젝트를 생성할 때 해당 규칙이 실제 환경변수 파일에 적용되는지 확인합니다. Backend는 `backend/.env`가 제외되고 `backend/.env.example`은 Commit되는 것을 확인했으며, `backend/.dockerignore`로 `.env`가 Docker 이미지에 포함되지 않게 했습니다. 원문에서 예시로 든 `*.key`, `*.pem`, `*.p12`, `*.pfx` 등 비밀키·인증서 파일도 Commit하지 않습니다. 해당 파일을 실제로 사용하는 기술이 정해지면 필요한 제외 규칙을 별도 작업에서 검토합니다.

## 7. 민감정보 노출 시 대응

민감정보를 Commit했다면 파일을 삭제하거나 새 Commit으로 덮는 것만으로 해결되지 않습니다. Git 기록에 값이 남아 있을 수 있습니다.

1. 추가 Commit과 Push를 중단합니다.
2. 즉시 팀장과 팀원에게 알립니다.
3. 노출된 API Key, Token 또는 비밀번호를 폐기합니다.
4. 새로운 Key 또는 비밀번호를 발급합니다.
5. Git 기록 정리 방법을 팀장과 함께 확인합니다.
6. `.gitignore`와 `.env.example` 설정을 다시 점검합니다.
7. 노출 원인과 조치 결과를 기록합니다.

노출 사실을 숨기거나 혼자 해결하지 않습니다.

## 8. 개발 환경 확인사항

프로젝트 생성 및 개발 시작 시 확인할 항목입니다. 아직 수행할 수 없는 항목을 완료된 것으로 표시하지 않습니다.

```text
[ ] 저장소 URL을 확인했는가?
[ ] 기술 스택과 각 기술의 버전을 확정했는가?
[ ] Frontend와 Backend 프로젝트가 생성되었는가?
[ ] 선택한 경우 Node.js와 npm 버전이 팀 기준과 일치하는가?
[ ] 선택한 경우 Java와 Gradle 버전이 팀 기준과 일치하는가?
[ ] Database 종류와 버전이 확정되었는가?
[ ] Frontend와 Backend가 각각 실행되는가?
[ ] 실제 코드가 요구하는 환경변수를 확인했는가?
[ ] 실제 환경변수 파일이 Git에서 제외되는가?
[ ] .env.example에 실제 값이 없는가?
[ ] API Key가 Frontend에 포함되지 않았는가?
[ ] GitHub와 협업 문서에 실제 Key가 없는가?
[ ] 로컬 빌드와 테스트 명령을 확인했는가?
[ ] GitHub Actions를 구성한다면 로컬에서 확인한 명령과 일치하는가?
```
