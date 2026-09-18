# 개발 환경 및 실행 가이드

> 어디갔지 Where is it의 현재 개발 환경 상태와 향후 프로젝트 생성 후 확인할 실행 절차입니다. 기술 후보와 명령 예시는 선택 결과나 실행 검증 결과가 아닙니다.

## 1. 개발 환경

Frontend와 Backend를 하나의 저장소에서 관리합니다. GitHub 저장소는 https://github.com/Ahnjisan/Where_is_it 입니다. Frontend 프로젝트는 아직 생성되지 않았고, Backend는 Spring Boot 기반 Gradle 프로젝트가 이미 생성되어 있습니다.

| 구분 | 기술 후보 또는 현황 | 상태 |
| --- | --- | --- |
| Frontend | React·Vite 및 JavaScript 또는 TypeScript | 후보·언어 미결정 |
| Backend | Spring Boot 3.5.16 · Gradle 8.14.5 · Java 17 | 구현됨 |
| Database | MySQL (JDBC 드라이버 및 datasource 설정 적용) | 구성됨(실행 환경값 필요) |
| AI API | OpenAI API 또는 기타 생성형 AI API | 제공자·모델 미결정 |
| 외부 데이터 API | 경찰청 습득물정보 조회 API | 확정 |
| 이메일 발송 서비스 | 서비스 미선정 | 미결정 |
| 협업 도구 | GitHub, Notion, Google Sheets, Discord, Miro | 확정 |
| 형상 관리 | GitHub Issues와 Pull Requests를 사용하는 GitHub Flow | 확정 |

Frontend 프로젝트 생성 후 Node.js·npm, React·Vite와 관련 버전을 기록합니다. Backend는 현재 Java 17, Spring Boot 3.5.16, Gradle 8.14.5, MySQL JDBC 설정을 사용합니다.

## 2. 사용 기술

### Frontend

React·Vite는 검토 중인 기술입니다. JavaScript와 TypeScript 중 선택하지 않았습니다. HTML5와 CSS3를 사용할 수 있으나 구체적인 프로젝트 구성은 생성 후 확인합니다.

### Backend

Backend는 Java 17, Spring Boot 3.5.16, Gradle 8.14.5 기반으로 구성되어 있습니다. 실행 시 DB 접속 정보(`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`)를 환경변수로 주입해야 합니다.

### Database

Backend 설정은 MySQL datasource를 사용합니다. 실제 DB 서버 정보는 로컬 또는 실행 환경에서 환경변수로 주입합니다.

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

다음은 현재 저장소에 존재하는 구조입니다. `frontend/`는 아직 프로젝트가 생성되지 않았고, `backend/`는 Spring Boot Gradle 프로젝트가 포함되어 있습니다.

```text
Where_is_it/
├── frontend/             # 현재 빈 디렉터리; 프로젝트 미생성
├── backend/              # Spring Boot Gradle 프로젝트
│   ├── src/
│   ├── gradle/
│   ├── build.gradle
│   ├── gradlew
│   ├── gradlew.bat
│   └── .env.example
├── docs/                 # 요구사항 및 협업 관련 문서
├── .github/              # Issue 및 PR 템플릿
├── .gitignore
├── README.md
└── CONTRIBUTING.md
```

세부 디렉터리 구조는 선택한 기술과 프로젝트 생성 결과에 따라 결정합니다. GitHub Actions는 두 프로젝트가 생성되고 로컬 빌드·테스트 명령을 확인한 뒤 별도 Issue에서 구성합니다. 서비스의 하루 1회 재검색은 GitHub Actions 예약 Workflow로 구현하지 않습니다.

## 4. 프로젝트 실행 방법

현재 Frontend 프로젝트 파일은 없으므로 Frontend 명령의 성공 여부는 확인할 수 없습니다. Backend는 프로젝트 파일이 존재하며, 실행 전 `backend/.env.example`을 기준으로 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`를 준비해야 합니다.

### 4.1 저장소 Clone

저장소 URL과 디렉터리명은 확정되어 있습니다. Clone은 담당 팀원이 협업 절차에 맞춰 수행합니다.

```bash
git clone https://github.com/Ahnjisan/Where_is_it.git
cd Where_is_it
```

### 4.2 Frontend 실행 예시

npm 기반 Frontend 프로젝트가 생성된 경우에 한해 `frontend/`의 `package.json`과 실제 Script를 확인합니다. 그 후 `npm install`, `npm run dev`, `npm run build`의 사용 여부를 결정합니다. 필요한 환경변수 예시 파일이 실제로 생성된 뒤에는 Git Bash·macOS·Linux의 `cp .env.example .env.local` 또는 PowerShell의 `Copy-Item .env.example .env.local`을 사용할 수 있습니다. 현재 `frontend/.env.example`은 없습니다.

Vite를 선택한 경우에도 개발 서버 주소와 포트는 실제 설정으로 확인해야 하며 특정 포트를 기본값으로 확정하지 않습니다.

### 4.3 Backend 실행 예시

Backend는 Spring Boot와 Gradle Wrapper가 이미 구성되어 있습니다. Unix 계열에서는 `./gradlew bootRun` 또는 `./gradlew build`, Windows에서는 `./gradlew.bat bootRun` 등을 사용할 수 있으며 실행 전 datasource 환경변수를 설정해야 합니다.

Express를 선택하고 npm 프로젝트가 생성된 경우, `backend/package.json`의 Script와 의존성을 확인한 뒤 `npm install`이나 `npm run dev`의 사용 여부를 결정합니다. `npm run dev` Script 또는 dotenv 라이브러리가 자동으로 존재한다고 가정하지 않습니다. Backend의 접속 주소와 포트 역시 실제 설정에 따라 달라집니다.

## 5. 환경변수 관리 규칙

실제 환경변수 파일을 GitHub에 올리지 않습니다. AI·공공데이터 API Key, 데이터베이스 주소·계정·비밀번호, Access·Refresh Token, JWT Secret, 이메일 인증정보를 코드와 문서에 값으로 기록하지 않습니다. 인증 기능과 이메일 발송 서비스의 구체 설정은 미결정입니다.

### 5.1 Frontend 환경변수

Frontend에는 공개 가능한 설정만 둡니다. Vite를 선택하면 `VITE_` 접두어의 값은 빌드 결과를 통해 브라우저에 노출될 수 있습니다. `VITE_API_BASE_URL`은 공개 가능한 설정의 이름 **예시**이며 실제 사용 여부와 값은 프로젝트 생성 후 정합니다.

AI_API_KEY, PUBLIC_DATA_API_KEY, DB_PASSWORD, JWT_SECRET, ACCESS_TOKEN, REFRESH_TOKEN과 이메일 인증정보는 Frontend에 넣지 않습니다.

### 5.2 Backend 환경변수

민감정보는 Backend의 실행 환경에 주입합니다. `AI_API_KEY`, `PUBLIC_DATA_API_KEY`, `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`는 이름 예시일 뿐 확정된 코드 계약이 아닙니다. 실제 코드를 작성할 때 필요한 이름만 결정합니다. 인증 기능을 채택할 때만 인증 관련 변수를, 이메일 발송 서비스를 정한 뒤에만 필요한 발송 설정 변수를 추가합니다.

Spring Boot를 선택하더라도 일반적인 `.env` 파일을 별도 설정이나 라이브러리 없이 자동으로 읽지 않습니다. 실제 값은 IDE Run Configuration, 운영체제 환경변수 또는 승인된 실행 환경을 통해 주입할 수 있으며, `application.yml`이나 `application.properties`에서는 `${환경변수명}` 형식으로 참조할 수 있습니다. 별도 dotenv 라이브러리는 의존성 변경 승인을 받은 경우에만 사용합니다. `backend/.env.example`은 향후 필요한 변수 목록을 설명하는 예시 파일로 사용할 수 있습니다.

Express를 선택하는 경우에도 dotenv 같은 라이브러리가 설치되어 있다고 가정하지 않습니다. 실제 환경변수 주입 방식을 프로젝트 구성에 맞춰 확인합니다.

### 5.3 `.env.example` 작성 규칙

현재 `frontend/.env.example`은 생성되지 않았고 `backend/.env.example`은 생성되어 있습니다. Frontend 예시 파일이 필요해지면 다음 규칙에 따라 작성합니다.

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

실제 환경변수 파일은 Git에서 제외합니다. 현재 루트 `.gitignore`는 `.env`, `.env.*`를 제외하고 `.env.example`은 허용합니다. 프로젝트를 생성할 때 해당 규칙이 실제 환경변수 파일에 적용되는지 확인합니다. 원문에서 예시로 든 `*.key`, `*.pem`, `*.p12`, `*.pfx` 등 비밀키·인증서 파일도 Commit하지 않습니다. 해당 파일을 실제로 사용하는 기술이 정해지면 필요한 제외 규칙을 별도 작업에서 검토합니다.

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
