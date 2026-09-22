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

### 5.2 처음 한 번 준비

1. Docker Desktop(Windows·macOS) 또는 Docker Engine과 Compose(Linux)를 설치하고 실행합니다.
2. `backend/.env.example`을 `backend/.env`로 복사하고 `DB_USERNAME`·`DB_PASSWORD` 값을 채웁니다. 작성 규칙은 6장을 따릅니다.

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
