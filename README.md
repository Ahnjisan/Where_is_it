# 어디갔지 Where is it

LG CNS AM INSPIRE 6기 Mini Project 1

> 한국에서 물건을 잃어버린 외국인이 자신의 언어로 분실물을 설명하고, 경찰청의 실제 습득물 정보에서 관련 후보를 찾고 추적하는 서비스입니다.

<img width="1536" height="1024" alt="service-flow" src="https://github.com/user-attachments/assets/8a29459d-7da0-4ef2-9ec9-1ef7f1a39416" />

## 해결하려는 문제와 핵심 사용자

한국에서 물건을 잃어버린 외국인은 한국어 검색조건을 만들고 경찰청 습득물 정보를 확인하는 데 어려움을 겪을 수 있습니다. Where is it은 사용자의 언어로 받은 설명을 검색조건으로 구조화하고, 실제 조회된 후보를 사용자의 언어로 안내합니다.

## 핵심 사용자 흐름

1. 사용자가 지원 언어로 분실물의 특징, 분실 시기와 장소를 입력합니다.
2. AI가 물품 분류, 색상, 기간, 지역 등의 검색조건을 구조화하고 경찰청 검색에 사용할 한국어 기준으로 변환합니다.
3. 경찰청 습득물정보 조회 API에서 실제 후보를 조회합니다.
4. AI가 조회 결과와 설명을 비교하여 후보의 우선순위와 추천 이유를 사용자 언어로 제공합니다.
5. 사용자는 후보의 사진, 습득 장소, 보관기관과 상세정보를 확인합니다.
6. 원하는 후보가 없으면 검색조건을 추적 대상으로 등록합니다.
7. 등록 후 7일 동안 하루 1회 같은 조건으로 재검색하고, 새로운 유사 후보가 발견되면 건수를 포함한 이메일 알림을 보냅니다. 7일이 지나면 추적을 자동 종료합니다.

<img width="1672" height="941" alt="어디갔니 ui 와이어프레임" src="https://github.com/user-attachments/assets/03a97c11-b791-4efd-a55b-42efa43a522c" />


## 핵심 기능

- 다국어 자연어 입력과 AI 기반 검색조건 구조화
- 경찰청 습득물정보 조회 API 기반 후보 검색과 AI 우선순위·추천 이유 제공
- 후보 목록 및 사진·습득 장소·보관기관을 포함한 상세정보 확인
- 추적 등록, 목록·상세 조회, 분실물 설명·검색조건·이메일 정보 수정, 등록 삭제
- 7일간 하루 1회 재검색, 신규 유사 후보 이메일 알림, 7일 경과 시 자동 종료

AI는 경찰청 API가 실제로 반환한 후보만 평가하며, 존재하지 않는 습득물 후보를 생성하지 않습니다.

## 제외 범위

사진만으로 동일 물품 자동 판별, 실제 소유자 판정, 경찰 신고·반환 신청 대행, 경찰청 데이터 등록·수정, 7일 초과 장기 추적, 실시간 상시 감시, 모든 언어 지원은 범위에 포함하지 않습니다.

## 팀원과 일정

- 팀원: 안지산, 반정욱, 최규성, 한예림, 윤창일
- 프로젝트 진행 기간: 2026.09.21 ~ 2026.09.30
- 실제 프로젝트 진행일: 2026.09.21 ~ 2026.09.22, 2026.09.28 ~ 2026.09.30

## 저장소와 협업

Frontend와 Backend를 한 저장소에서 관리하며 두 프로젝트 모두 기본 구성이 생성되어 있습니다.

- Backend: Java 17, Spring Boot 3.5.16, Gradle Wrapper 8.14.5
- Frontend: Next.js 15.5.25, React 19.1.x, JavaScript, npm
- Database: MySQL 8.4. 팀원 PC에 직접 설치하지 않고 `backend/compose.yaml`(Docker Compose)로 실행합니다.
- Redis: 현재 사용하지 않습니다.

### 기본 검증 및 실행

Backend 빌드:

```bash
cd backend
./gradlew clean assemble
```

Backend 실행(Docker 필요). 처음 한 번은 `backend/.env.example`을 `backend/.env`로 복사하고 `DB_USERNAME`·`DB_PASSWORD` 값을 채웁니다.

```bash
cd backend
docker compose up -d --wait
./gradlew bootRun
```

Windows PowerShell에서는 `./gradlew` 대신 `.\gradlew.bat`을 사용합니다. MySQL 중지·초기화와 문제 해결은 [개발 환경 및 실행 가이드](docs/development-guide.md)의 5장을 확인하세요.

Frontend 설치, 빌드 및 실행:

```bash
cd frontend
npm ci
npm run build
npm run dev
```

Frontend 개발 서버의 기본 주소는 http://localhost:3000 입니다. 자세한 환경 기준과 운영체제별 명령은 [개발 환경 및 실행 가이드](docs/development-guide.md)를 확인하세요.

Issue 등록 → 최신 `main`에서 Issue별 작업 Branch 생성 → 구현·검증 → Commit·Push → PR → 팀원 Review → `main` 병합 → 작업 Branch 삭제 순서로 진행합니다. `main`에 직접 Commit하거나 Push하지 않으며 `develop` Branch는 사용하지 않습니다. 자세한 규칙은 [CONTRIBUTING.md](CONTRIBUTING.md)를 확인하세요.

상세 문서는 [문서 안내](docs/README.md)에서 확인할 수 있습니다.

## 안전 안내

검색 결과는 설명과 관련성이 높은 후보이며, 동일 물품 여부와 반환 절차는 보관기관을 통해 최종 확인해야 합니다.
