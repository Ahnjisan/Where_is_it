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
| Database | MySQL | 확정, 실행 환경 구성 예정 |
| 외부 데이터 API | 경찰청 습득물정보 조회 API | 확정 |
| 형상 관리 | GitHub Issues와 Pull Requests를 사용하는 GitHub Flow | 확정 |

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

### Windows PowerShell

```powershell
cd backend
.\gradlew.bat clean assemble
```

MySQL 연결 환경이 준비된 이후에는 다음 명령으로 애플리케이션을 실행합니다.

```powershell
cd backend
.\gradlew.bat bootRun
```

### macOS/Linux

```bash
cd backend
./gradlew clean assemble
```

MySQL 연결 환경이 준비된 이후에는 다음 명령으로 애플리케이션을 실행합니다.

```bash
cd backend
./gradlew bootRun
```

현재 `bootRun`과 Database 연동 테스트는 MySQL Docker Compose 환경이 아직 구성되지 않았으므로 기본 환경 검증의 성공 조건으로 사용하지 않습니다.

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

## 5. MySQL 운영 원칙

- Database는 MySQL을 사용합니다.
- 팀원 PC에 MySQL을 직접 설치하지 않습니다.
- 추후 Docker Compose로 팀에 동일한 로컬 MySQL 환경을 제공합니다.
- Docker Compose 파일 작성과 실제 연결 검증은 별도 Issue에서 진행합니다.
- 현재 Redis는 사용하지 않습니다.
- 이번 기본 환경 작업에서는 Dockerfile, Compose 파일, Entity, Repository 또는 datasource 구조를 변경하지 않습니다.

## 6. 환경변수 관리

실제 환경변수 파일과 비밀값은 Git에 올리지 않습니다. API Key, Database 계정·비밀번호, Access·Refresh Token 및 이메일 인증정보를 코드나 문서에 기록하지 않습니다.

- Backend 설정값은 운영체제 환경변수 또는 승인된 로컬 실행 환경으로 주입합니다.
- `backend/.env.example`은 변수명과 형식만 설명하며 실제 값을 포함하지 않습니다.
- Frontend에는 브라우저에 공개 가능한 설정만 둡니다.
- Frontend에서 실제 환경변수가 필요해질 때만 값이 비어 있는 `frontend/.env.example`을 추가합니다.
- `.env.example`은 추적할 수 있지만 실제 `.env`와 `.env.*` 파일은 Git에서 제외합니다.

## 7. 검증 기준

기본 환경 변경 후 다음을 확인합니다.

```text
[ ] Backend clean assemble 성공
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

MySQL Docker Compose 구현과 Backend `bootRun`·Database 연결 검증은 별도 Issue에서 수행합니다.
