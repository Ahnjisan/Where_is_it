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
| Database | MySQL |
| 로컬 Database 실행 | 팀원 PC에 직접 설치하지 않고 추후 Docker Compose로 제공 |
| Redis | 현재 도입하지 않음 |
| AI 결과 범위 | 경찰청 API가 실제 반환한 후보로 제한 |
| 추적 기간 | 등록 후 7일 |
| 재검색 주기 | 하루 1회 |
| 알림 방식 | 신규 유사 후보 발견 시 이메일 알림 |

## 미결정 항목

| 항목 | 상태 |
| --- | --- |
| AI API 제공자와 모델 | 미결정 |
| 이메일 발송 서비스 | 미결정 |
| 인증 기능 | 미결정 |
| 상세 API 계약 | 미결정 |
| Database 구조 | 미결정 |
| AI 입출력 JSON Schema | 미결정 |
| 화면 상세 설계 | 미결정 |
| 배포 환경 | 미결정 |

## 변경 이력

| 일자 | 구분 | 결정 또는 변경 내용 | 근거 | 승인자 |
| --- | --- | --- | --- | --- |
| 2026-09-19 | 개발 환경 기준선 | Java 17, Spring Boot 3.5.16, Gradle Wrapper 8.14.5와 Java 기본 패키지 `com.whereisit` 사용 | Issue #6 | 안지산 |
| 2026-09-19 | Frontend 기준선 | Next.js 15.5.25, React 19.1.x, JavaScript, Node.js 24.20.0, npm 11.6.2 사용 | Issue #6 | 안지산 |
| 2026-09-19 | Database 운영 | MySQL을 사용하고 추후 Docker Compose로 제공하며, 현재 Redis는 도입하지 않음 | Issue #6 | 안지산 |
