# AI Agent Prompt Guide

> Frontend와 Backend 작업의 공통 절차와 표준 프롬프트입니다. 아래 기술명과 명령은 작업별 선택지 또는 예시이며 현재 프로젝트의 확정 기술이 아닙니다.

## 1. 문서 목적

AI 에이전트는 구현과 검토를 보조하며 작업 범위와 결과의 최종 책임은 담당 팀원에게 있습니다. 요구사항의 임의 해석과 범위 초과를 막기 위해 작업 목적, Issue, Branch, 기술 스택, 요구사항, 허용·제외 범위, 완료 조건, 검증 방법과 보고 형식을 전달합니다.

## 2. AI 에이전트 작업 절차

```text
1. GitHub Issue 작성
2. 담당자가 작업 Branch 생성
3. AI 사전점검 프롬프트 실행
4. 담당자가 사전점검 결과 검토
5. 정확한 수정 범위 확정
6. AI 구현 및 검증 프롬프트 실행
7. 담당자가 변경 파일과 실행 결과 확인
8. 필요하면 AI 독립 리뷰 실행
9. 담당자가 Commit 및 Push
10. Pull Request 생성 및 팀원 리뷰
```

사전점검에서는 코드를 수정하지 않습니다.

## 3. AI 에이전트 공통 규칙

### 3.1 Git 작업 제한

AI 에이전트는 다음 작업을 수행하지 않습니다.

```text
git checkout
git switch
git branch
git commit
git push
git pull
git merge
git rebase
git reset
git clean
Pull Request 생성
Pull Request 병합
Branch 삭제
```

`git status`, `git branch --show-current`, `git rev-parse HEAD`, `git diff`, `git diff --check`, `git log` 같은 읽기 전용 확인은 허용할 수 있습니다. Branch 생성·Commit·Push·PR 생성과 병합은 담당 팀원이 수행합니다.

### 3.2 작업 범위 제한

- 관련 Issue와 승인된 파일 범위만 작업합니다.
- 요구하지 않은 기능, 폴더 재구성, 재구현, 다른 팀원 영역의 변경을 임의로 수행하지 않습니다.
- 추가 수정이 필요하면 이유와 대상 파일을 먼저 보고하고, 승인 범위를 벗어나야 하면 구현을 중단합니다.

### 3.3 의존성 변경 제한

다음 경로는 해당 기술로 프로젝트가 생성된 경우의 예시이며, 현재 파일이 존재한다는 뜻이 아닙니다. 사전 승인 없이 수정하지 않습니다.

```text
frontend/package.json
frontend/package-lock.json
backend/build.gradle
backend/settings.gradle
backend/package.json
backend/package-lock.json
```

새 라이브러리가 필요하면 이름, 목적, 기존 기술로 대체할 수 없는 이유, 추가 파일과 영향을 먼저 보고합니다. 승인 없이 패키지를 설치하거나 의존성 파일을 변경하지 않습니다.

### 3.4 민감정보 보호

실제 API Key, 비밀번호, Token, 개인정보를 프롬프트에 넣지 않습니다. AI도 코드·로그·테스트·문서에 AI·공공데이터 API Key, DB 비밀번호, Access·Refresh Token, JWT Secret, 개인정보를 기록하지 않습니다. 필요한 코드는 환경변수를 참조합니다.

### 3.5 기존 규칙 우선

작업 전 존재하는 README, CONTRIBUTING, Code Convention, 개발 환경 가이드, 관련 API 명세·Issue와 AI 지침을 확인합니다. 문서와 사용자 지시가 충돌하면 임의로 결정하지 않고 보고합니다.

### 3.6 결과 검증

수정 후 가능한 범위에서 빌드, Lint, Formatter, 단위 테스트, 담당 기능, 기존 기능 영향, 민감정보, 변경 범위와 문서 정합성을 확인합니다. 실행하지 못한 검증을 통과했다고 표현하지 않습니다.

## 4. 공통 프롬프트 구성 요소

모든 구현 프롬프트에 역할, 프로젝트 정보, Issue, 목적, 현재 상태, 요구사항, 허용 범위, 제외 범위, 완료 조건, 검증 방법, 보고 형식을 넣고 빈 항목은 작업에 맞게 채웁니다.

# Backend Prompt

## 5. Backend 사전점검 프롬프트

```text
당신은 이 프로젝트의 Backend 사전점검 담당자입니다.
이번 단계에서는 코드를 수정하지 말고 저장소 상태와 기존 구조를 읽기 전용으로 분석하세요.

## 1. 프로젝트 정보
- 프로젝트명: [프로젝트명]
- 관련 Issue: #[Issue 번호] [Issue 제목]
- Backend 기술: [확정된 기술; Spring Boot 또는 Express는 선택지 예시]
- Database: [확정된 DB; MySQL 또는 PostgreSQL은 선택지 예시]
- 작업 Branch: [현재 작업 Branch]
- 작업 목적: [구현하려는 기능]

## 2. 반드시 확인할 문서
- README.md, CONTRIBUTING.md, Code Convention
- 개발 환경 및 실행 가이드, 관련 API 명세, DB 설계 문서, GitHub Issue
- 저장소의 AI 에이전트 지침

## 3. 사전점검 대상
1. Branch와 HEAD, Working Tree
2. Backend 폴더·패키지와 Controller, Service, Repository, DTO, Entity
3. API 요청·응답, Validation, 공통 예외 처리
4. Database·Migration, 외부 API·AI API Client
5. 환경변수·설정, 테스트 구조와 실행 명령
6. 충돌할 기존 기능, 생성·수정 파일, 위험과 반례

## 4. Backend 확인 기준
- Controller의 비즈니스 로직과 요청·응답 DTO 분리, Entity 직접 반환 여부
- 입력 Validation, 오류·빈 데이터, 외부 API·AI API 실패·Timeout 처리
- Backend 환경변수로 API Key 관리, DB 변경의 기존 데이터 호환성
- Frontend와 합의한 API 계약 영향, 새 의존성 필요 여부

## 5. 금지 사항
- 코드 수정·파일 생성·삭제·패키지 설치·의존성 변경 금지
- Branch 변경·Commit·Push·Pull·Merge·Rebase 금지
- Issue 범위 밖 개선을 제안 없이 포함하지 않기

## 6. 보고 형식
### 1. 현재 상태
- Branch, HEAD, Working Tree, Backend 기술, 테스트 실행 명령어
### 2. 기존 구조 분석
- 관련 파일, 현재 처리 흐름, 재사용 코드, 관련 문서
### 3. 구현 제안
- 처리 흐름, Validation, 오류 처리, Database·외부 API·Frontend 계약 영향
### 4. 예상 변경 범위
- 생성·수정·삭제 파일, 의존성 변경 또는 사유
### 5. 예상 위험 및 반례
-
### 6. 검증 계획
-
### 7. 확인이 필요한 결정
-
분석이 끝나면 구현하지 말고 담당자의 승인을 기다리세요.
```

## 6. Backend 구현 및 검증 프롬프트

```text
당신은 이 프로젝트의 Backend 구현 담당자입니다.
사전점검 결과와 아래 승인된 범위만 기준으로 구현하세요.

## 1. 프로젝트 정보
- 프로젝트명: [프로젝트명]
- 관련 Issue: #[Issue 번호] [Issue 제목]
- Backend 기술: [확정된 기술]
- Database: [확정된 DB]
- 작업 Branch: [현재 작업 Branch]

## 2. 작업 목적
[구체적인 목적]

## 3. 구현 요구사항
1. [요구사항 1]
2. [요구사항 2]
3. [요구사항 3]

## 4. 승인된 변경 범위
### 생성 허용 파일
-
### 수정 허용 파일
-
### 삭제 허용 파일
- 없음
### 의존성 변경
- 없음 또는 승인된 의존성과 사유
목록 밖 파일이 필요하면 즉시 중단하고 이유를 보고하세요.

## 5. 구현 기준
- 기존 구조·Code Convention 준수, Controller·Service·Repository 책임 분리
- 요청·응답 DTO 사용, Entity 직접 반환 금지, 입력 Validation
- 정상·잘못된 요청·빈 결과·외부 API 실패 구분, 공통 예외 처리 재사용
- AI·외부 API Key는 환경변수로만 참조하고 로그·오류 응답에 출력 금지
- 요구하지 않은 리팩터링 금지, 기존 API 계약 변경 전 보고

## 6. 완료 조건
- [완료 조건 1]
- [완료 조건 2]
- [완료 조건 3]
- 기존 기능 영향 없음, 민감정보 없음, 관련 테스트 통과

## 7. 검증 요구사항
- 실제 프로젝트에서 확인된 Backend Build, Formatter 또는 Lint
- 관련 단위·통합 테스트, 정상·Validation 실패·빈 결과·외부 API 실패
- 기존 기능 회귀, git diff --check
- 실행하지 못한 검증은 이유를 보고

## 8. Git 작업 제한
- Branch 생성·변경, Commit, Push, Pull, Merge, Rebase, Reset, PR 생성·병합 금지

## 9. 최종 보고 형식
### 1. 최종 상태
- Branch, HEAD, Working Tree, Staged 파일
### 2. 변경 파일
- 생성, 수정, 삭제
### 3. 구현 내용
-
### 4. API 및 Database 영향
- API, Database, 환경변수, 의존성
### 5. 검증 결과
- Build, Lint 또는 Formatter, 단위·통합 테스트, 수동 확인, git diff --check
### 6. 미해결 사항
-
### 7. 담당자가 직접 확인할 사항
-
구현과 검증까지만 수행하고 Commit, Push, PR 생성은 하지 마세요.
```

# Frontend Prompt

## 7. Frontend 사전점검 프롬프트

```text
당신은 이 프로젝트의 Frontend 사전점검 담당자입니다.
이번 단계에서는 코드를 수정하지 말고 기존 구조를 읽기 전용으로 분석하세요.

## 1. 프로젝트 정보
- 프로젝트명: [프로젝트명]
- 관련 Issue: #[Issue 번호] [Issue 제목]
- Frontend 기술: [확정된 기술; React, Vite, JavaScript, TypeScript는 선택지 예시]
- 스타일링 방식: [확정된 방식]
- 작업 Branch: [현재 작업 Branch]
- 작업 목적: [화면 또는 기능]

## 2. 반드시 확인할 문서
- README.md, CONTRIBUTING.md, Code Convention, 개발 환경 및 실행 가이드
- 화면 설계, API 명세, GitHub Issue, 저장소의 AI 지침

## 3. 사전점검 대상
1. Branch·HEAD·Working Tree와 실제 기술·버전·언어
2. 폴더 구조, Router, Page·Component, Layout·Style, API 모듈
3. 상태 관리, 환경변수, Loading·Error·Empty 처리
4. 반응형·접근성, 테스트·Lint 명령
5. 생성·수정 파일과 위험·반례

## 4. Frontend 확인 기준
- 기존 Component 재사용, Page와 공통 Component 책임 분리, API 코드 중복 여부
- Backend 계약 일치, Loading·Error·Empty, 입력 Validation
- 작은 화면과 접근성, Frontend의 AI·외부 API 직접 호출 여부
- VITE_ 환경변수의 민감정보 노출과 새 패키지 필요 여부

## 5. 금지 사항
- 코드 수정·파일 생성·삭제·패키지 설치·의존성 파일 변경 금지
- Branch 변경·Commit·Push·Pull·Merge·Rebase 금지
- 요구 없는 UI 재설계나 Issue 밖 리팩터링 금지

## 6. 보고 형식
### 1. 현재 상태
- Branch, HEAD, Working Tree, 실제 기술·버전·언어, Lint·Build 명령
### 2. 기존 구조 분석
- Page, Component, API 모듈, Router, 상태 관리, Style, 재사용 코드
### 3. 구현 제안
- 화면 구성, Component, 상태 관리, API, Loading·Error·Empty, 입력 검증, 접근성·반응형
### 4. 예상 변경 범위
- 생성·수정·삭제 파일과 의존성 변경 사유
### 5. 예상 위험 및 반례
-
### 6. 검증 계획
-
### 7. 확인이 필요한 결정
-
분석이 끝나면 구현하지 말고 담당자의 승인을 기다리세요.
```

## 8. Frontend 구현 및 검증 프롬프트

```text
당신은 이 프로젝트의 Frontend 구현 담당자입니다.
사전점검 결과와 아래 승인된 범위만 기준으로 구현하세요.

## 1. 프로젝트 정보
- 프로젝트명: [프로젝트명]
- 관련 Issue: #[Issue 번호] [Issue 제목]
- Frontend 기술: [확정된 기술]
- 스타일링 방식: [확정된 방식]
- 작업 Branch: [현재 작업 Branch]

## 2. 작업 목적
[구체적인 목적]

## 3. 구현 요구사항
1. [요구사항 1]
2. [요구사항 2]
3. [요구사항 3]

## 4. 승인된 변경 범위
### 생성 허용 파일
-
### 수정 허용 파일
-
### 삭제 허용 파일
- 없음
### 의존성 변경
- 없음 또는 승인된 의존성과 사유
목록 밖 파일이 필요하면 즉시 중단하고 이유를 보고하세요.

## 5. 구현 기준
- 기존 구조·Code Convention 준수, Page와 공통 Component 책임 분리
- 기존 공통 Component·Style 재사용, API 요청은 API 모듈에 작성
- AI·외부 API를 브라우저에서 직접 호출하지 않고 비밀값을 Frontend에 기록하지 않기
- 정상·Loading·Error·Empty 상태와 사용자 입력 검증
- 이미지 대체 텍스트, 버튼의 의미 있는 텍스트 또는 접근성 속성
- 요구 없는 재설계·패키지 설치·대규모 포맷 변경 금지, API 계약 불일치 보고

## 6. 완료 조건
- [완료 조건 1]
- [완료 조건 2]
- [완료 조건 3]
- Loading·Error·Empty 동작, 기존 화면 영향 없음, 민감정보 없음
- 실제 프로젝트의 Build·Lint 통과

## 7. 검증 요구사항
- 실제 프로젝트에서 확인된 Build·Lint·Formatter·관련 테스트
- 정상·빈 데이터·Loading·API 오류 화면과 입력 Validation
- 작은 화면, Console 오류, 기존 화면 회귀, git diff --check
- npm 명령은 npm 기반 프로젝트로 확인된 경우에만 실행
- 실행하지 못한 검증은 이유를 보고

## 8. Git 작업 제한
- Branch 생성·변경, Commit, Push, Pull, Merge, Rebase, Reset, PR 생성·병합 금지

## 9. 최종 보고 형식
### 1. 최종 상태
- Branch, HEAD, Working Tree, Staged 파일
### 2. 변경 파일
- 생성, 수정, 삭제
### 3. 구현 내용
-
### 4. 화면 및 API 영향
- 화면, API, 환경변수, 의존성
### 5. 검증 결과
- Build, Lint, Formatter, 테스트, 수동 확인, Console 오류, git diff --check
### 6. 미해결 사항
-
### 7. 담당자가 직접 확인할 사항
-
구현과 검증까지만 수행하고 Commit, Push, PR 생성은 하지 마세요.
```

# Review Prompt

## 9. 독립 코드 리뷰 프롬프트

```text
당신은 이 프로젝트의 독립 코드 리뷰 담당자입니다.
코드를 수정하지 말고 현재 변경 사항을 읽기 전용으로 검토하세요.

## 1. 리뷰 대상
- 프로젝트명: [프로젝트명]
- 관련 Issue: #[Issue 번호] [Issue 제목]
- 영역: [Frontend / Backend]
- 작업 Branch: [현재 작업 Branch]
- 비교 기준 Branch: main
- 구현 목적: [구현 목적]

## 2. 반드시 확인할 내용
- Issue 요구사항, 승인된 범위, Git diff와 변경 파일 전체
- 관련 기존 코드, Code Convention, API 명세
- 실행·테스트 결과, 환경변수와 민감정보 처리

## 3. 리뷰 기준
### Critical
- 민감정보 노출, 데이터 손실, 실행 불가, 주요 보안 취약점, 다른 작업의 심각한 손상
### Major
- 요구사항 미충족, 주요 기능 오류, API 계약 불일치, 오류 처리 누락
- 잘못된 저장, 기존 기능 회귀, 병합하기 어려운 테스트 부족
### Minor
- 이름·가독성, 중복 코드, 작은 문서 불일치, 선택적 구조 개선

## 4. 추가 확인 항목
- Frontend: Loading·Error·Empty, 입력 검증, API 응답, 접근성, 반응형, Console, 민감정보
- Backend: Validation, Controller·Service·Repository 책임, DTO·Entity 경계
- Backend: 예외 처리, 외부 API 실패·Timeout, Database, 환경변수·로그 민감정보

## 5. 금지 사항
- 코드 수정·파일 생성·삭제·패키지 설치 금지
- Commit·Push·Pull·Merge·Rebase 금지
- 근거 없는 추측이나 개인 취향만으로 변경 요구 금지

## 6. 리뷰 결과 형식
### 1. 최종 판정
- APPROVE / COMMENT / REQUEST CHANGES / BLOCKED 중 하나
### 2. 발견 사항
#### Critical
-
#### Major
-
#### Minor
-
각 항목에 파일·위치, 실제 문제, 발생 조건, 영향, 수정 방향을 포함하세요.
### 3. 요구사항 충족 여부
-
### 4. 변경 범위 준수 여부
-
### 5. 테스트 및 검증 평가
-
### 6. 문서 및 API 정합성
-
### 7. 병합 전 필수 조치
-
리뷰만 수행하고 코드를 수정하지 마세요.
```

## 10. 피해야 할 프롬프트

`이 프로젝트 완성해 줘`, `로그인 만들어 줘`, `전체적으로 예쁘게 수정해 줘`, `오류를 전부 알아서 고쳐 줘`, `리팩터링도 같이 해 줘`처럼 범위가 모호한 요청은 피합니다.

```text
Issue #12의 검색 결과 화면만 구현하세요.
수정 허용 파일: [실제로 존재하고 승인된 파일]
요구사항: 결과 목록, Loading·Error·Empty 구분, 기존 API 모듈 재사용
검증: 실제 프로젝트에서 확인된 Build·Lint와 상태별 화면 확인
패키지 설치와 다른 파일 수정, Commit·Push·PR 생성 금지
```

## 11. 프롬프트 작성 전 확인사항

```text
[ ] 관련 GitHub Issue가 생성되었는가?
[ ] 담당자와 작업 범위가 정해졌는가?
[ ] 담당자가 작업 Branch를 생성했는가?
[ ] 목적, 요구사항, 허용 파일, 제외 범위가 명확한가?
[ ] 새 의존성 허용 여부와 완료 조건이 있는가?
[ ] 실제 실행 가능한 검증 방법이 있는가?
[ ] Git 작업 금지와 민감정보 보호가 포함되었는가?
[ ] 사전점검과 구현 프롬프트가 분리되었는가?
```
