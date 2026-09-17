# GitHub 협업 규칙

> LG CNS AM INSPIRE 6기 Mini Project 1, 어디갔지 Where is it의 Issue, Branch, Commit, Pull Request 운영 규칙입니다.

## 1. 협업 규칙을 정하는 이유

GitHub는 코드와 함께 작업 과정과 의사결정을 기록합니다. 여러 팀원의 코드 충돌과 작업 누락을 줄이고 변경 시점과 이유를 확인하기 위해 같은 규칙을 사용합니다.

```text
Issue 등록
→ 최신 main에서 작업 Branch 생성
→ 기능 구현 및 검증
→ Commit 및 Push
→ Pull Request 생성
→ 팀원 리뷰
→ main 병합
→ 작업 Branch 삭제
```

## 2. Branch 운영 전략

### 2.1 기본 구조

별도 `develop` Branch 없이 `main`과 Issue별 작업 Branch만 사용하는 GitHub Flow를 적용합니다.

```text
main
├── feature/12-search-page
├── feature/15-summary-api
├── fix/21-response-error
├── docs/7-api-spec
├── refactor/18-api-client
├── test/25-summary-service
└── chore/3-project-setup
```

| Branch | 용도 |
| --- | --- |
| `main` | 실행 가능하고 통합이 완료된 기준 Branch |
| `feature/*` | 새로운 기능 구현 |
| `fix/*` | 오류 수정 |
| `docs/*` | 문서 작업 |
| `refactor/*` | 기능 변경 없는 코드 구조 개선 |
| `test/*` | 테스트 코드 추가 및 수정 |
| `chore/*` | 설정, 패키지, 환경 구성 |

### 2.2 핵심 원칙

- `main`에는 직접 Commit하거나 Push하지 않습니다.
- 모든 작업은 GitHub Issue 등록 후 시작합니다.
- 작업 Branch는 최신 `main`에서 생성합니다.
- 하나의 Branch에서는 하나의 Issue만 처리합니다.
- 모든 변경은 PR을 통해 `main`에 병합합니다.
- 병합된 작업 Branch는 삭제합니다.
- 다른 팀원이 사용 중인 Branch에는 임의로 Push하지 않습니다.

## 3. Issue 작성 규칙

### 3.1 Issue를 먼저 등록하는 이유

Issue는 작업 목적, 필요성, 완료 조건, 담당자, 의존 작업을 명확히 합니다. 코드를 작성하기 전에 무엇을 왜 구현하며 어디까지 완료할지 확인할 수 있어야 합니다.

### 3.2 Issue 제목

`[작업유형] 작업 내용` 형식을 사용합니다.

```text
[Feature] 검색 결과 화면 구현
[Fix] 빈 검색어 요청 오류 수정
[Docs] API 명세 작성
[Chore] 프론트엔드 초기 환경 구성
```

### 3.3 Issue 본문 양식

```markdown
## 작업 목적

-

## 작업 내용

- [ ]

## 완료 조건

- [ ]

## 참고 사항

-

## 담당자

-
```

### 3.4 Issue 작업 범위

하나의 Issue가 지나치게 크지 않도록 기능을 나눕니다. 검색 조건 입력 화면, 검색 API, 결과 목록 화면을 각각 관리할 수 있습니다. Frontend와 Backend 전체 구현을 한 Issue로 묶지 않습니다.

## 4. Branch 이름 작성 규칙

### 4.1 기본 형식

`작업유형/Issue번호-작업내용` 형식입니다.

```text
feature/12-search-page
feature/15-summary-api
fix/21-response-error
docs/7-api-spec
refactor/18-api-client
test/25-summary-service
chore/3-project-setup
```

### 4.2 Branch 접두어

| 접두어 | 용도 | 예시 |
| --- | --- | --- |
| `feature` | 새 기능 | `feature/12-search-page` |
| `fix` | 오류 수정 | `fix/21-response-error` |
| `docs` | 문서 | `docs/7-api-spec` |
| `refactor` | 기능 변경 없는 개선 | `refactor/18-api-client` |
| `test` | 테스트 | `test/25-summary-service` |
| `chore` | 설정과 기타 작업 | `chore/3-project-setup` |

### 4.3 Branch 이름 주의사항

- 영문 소문자와 숫자를 사용하고 단어 사이는 하이픈(`-`)으로 구분합니다.
- 이름만 보고 작업 내용을 파악할 수 있게 작성합니다.
- `new`, `temp`, `test123`, `unknown`처럼 불분명한 이름을 사용하지 않습니다.
- 띄어쓰기와 한글을 사용하지 않습니다.
- Commit의 `feat`와 달리 기능 Branch에는 `feature/`를 사용합니다.

### 4.4 작업 Branch 생성

담당 팀원이 최신 `main`을 반영한 뒤 Issue 번호를 포함해 생성합니다.

```bash
git switch main
git pull origin main
git switch -c feature/12-search-page
```

## 5. Commit 작성 규칙

### 5.1 기본 원칙

- 하나의 Commit에 하나의 작업 목적만 담고 실행되지 않는 상태의 코드는 가급적 Commit하지 않습니다.
- 변경 파일을 확인한 뒤 필요한 파일만 Stage합니다.
- API Key, 비밀번호, Token 등 민감정보와 담당 범위 밖 파일을 포함하지 않습니다.
- 리뷰 가능한 크기로 나눕니다.

### 5.2 Commit 메시지 형식

Conventional Commits의 `<type>(<scope>): <description>`을 사용합니다. `scope`는 생략할 수 있습니다.

```text
feat(frontend): 검색 결과 카드 구현
fix(api): 빈 검색어 요청 검증 추가
docs(readme): 프로젝트 안내 수정
test(backend): 요약 서비스 단위 테스트 추가
chore(frontend): API 요청 라이브러리 설치
```

### 5.3 Commit type

| type | 의미 | 사용 예시 |
| --- | --- | --- |
| `feat` | 새 기능 | 화면, API, 비즈니스 기능 |
| `fix` | 오류 수정 | 응답, 화면 오류 |
| `docs` | 문서 | README, API 명세 |
| `style` | 동작 변경 없는 형식 수정 | 포맷팅, CSS 스타일 |
| `refactor` | 기능 변경 없는 구조 개선 | 중복 코드 제거 |
| `test` | 테스트 | 단위·통합 테스트 |
| `chore` | 설정과 기타 작업 | 의존성, 환경설정 |
| `ci` | 자동화 설정 | GitHub Actions |

### 5.4 권장 scope

변경 영역에 따라 `frontend`, `backend`, `api`, `db`, `ai`, `docs`, `config`를 사용할 수 있습니다. 예: `feat(backend): 검색 결과 조회 API 구현`.

### 5.5 Commit 제목 작성 규칙

`type`은 영문 소문자로 쓰고 콜론 뒤에는 공백 한 칸을 둡니다. 제목 끝에 마침표를 붙이지 않습니다. `수정`, `작업`, `업데이트`만 쓰지 말고 변경 내용을 구체적으로 적습니다.

### 5.6 Breaking Change

API 요청·응답처럼 다른 팀원의 작업에 영향을 주는 변경은 먼저 공유합니다. 호환성이 깨지면 `feat(api)!: 검색 응답 필드 구조 변경`처럼 `!`를 표시합니다. Commit 본문이나 PR에 변경 전·후 구조, 이유, 영향받는 기능과 다른 팀원이 수정할 부분을 기록합니다.

### 5.7 WIP Commit

개인 작업 Branch의 임시 백업에 한해 `wip(frontend): 검색 결과 화면 구현 중`을 제한적으로 사용합니다. WIP 상태의 PR은 Draft로 만듭니다. `main` 병합 시 `Squash and merge`로 WIP Commit을 남기지 않습니다. 공동 Branch의 Commit 이력을 임의로 Rebase하거나 강제 Push하지 않습니다.

### 5.8 Commit 예시

```text
feat(backend): 외부 데이터 조회 API 구현

- 외부 API 요청 서비스를 추가
- 응답 데이터를 내부 DTO로 변환
- 정상 응답과 빈 응답을 확인
```

```text
fix(frontend): API 오류 메시지 미출력 문제 수정

- API 요청 실패 상태를 화면에 표시
- 재시도 버튼을 추가
- 네트워크 오류 동작 확인
```

### 5.9 Commit 전 확인사항

```text
[ ] Issue 범위의 변경만 포함했는가?
[ ] 변경 파일과 내용을 직접 확인했는가?
[ ] 실행 또는 테스트가 정상 완료되었는가?
[ ] 불필요한 디버깅 코드가 없는가?
[ ] API Key, 비밀번호, Token과 실제 .env 파일이 없는가?
[ ] 사용하지 않는 파일과 코드가 없는가?
[ ] Commit 메시지가 변경을 명확히 설명하는가?
```

## 6. 작업 파일 Stage 및 Push

변경 파일을 먼저 확인하고 필요한 파일을 명시하여 Stage합니다. 아래 경로와 명령은 해당 파일이 실제로 존재할 때 담당 팀원이 사용하는 예시입니다.

```bash
git status
git diff
git add frontend/src/components/SearchForm.jsx
git add frontend/src/components/SearchResult.jsx
git commit -m "feat(frontend): 검색 결과 화면 구현"
git push -u origin feature/12-search-page
```

원격 Branch가 이미 등록된 경우 `git push origin feature/12-search-page`를 사용합니다.

## 7. Pull Request 규칙

### 7.1 기본 원칙

- 대상 Branch는 `main`이고 하나의 PR에서 하나의 Issue만 처리합니다.
- 팀원이 리뷰할 수 있는 크기로 유지하고 작성자가 먼저 변경 내용을 확인합니다.
- 미완료 작업은 Draft PR을 사용합니다.
- 관련 Issue는 본문에 `Closes #Issue번호`로 연결합니다.
- 최소 한 명의 리뷰 승인을 받은 후 병합합니다. 작성자는 자신의 PR을 승인할 수 없습니다.

### 7.2 PR 흐름과 제목

```text
feature/12-search-page → Pull Request → main
```

제목도 Conventional Commits 형식이며 설명 부분은 팀원이 이해하기 쉽도록 한글로 씁니다. 예: `feat(frontend): 검색 결과 화면 구현`.

### 7.3 PR 본문 양식

```markdown
## 작업 목적

-

## 주요 변경 사항

-

## 확인 및 테스트 결과

- [ ] 로컬 실행 확인
- [ ] 담당 기능 정상 동작 확인
- [ ] 기존 기능 영향 여부 확인
- [ ] 민감정보 미포함 확인

## 화면 또는 실행 결과

<!-- 화면이면 스크린샷, API면 요청·응답 결과를 기록합니다. -->

-

## 리뷰 요청 사항

-

## 관련 Issue

Closes #12
```

### 7.4 PR 작성 예시

검색 결과 화면 PR이라면 작업 목적, 카드·결과 없음·로딩 상태 변경, 성공·빈 결과·실패 확인 결과, 화면 캡처, 리뷰 요청 사항과 `Closes #12`를 기록합니다. 실행하지 못한 검증은 통과한 것으로 표시하지 않고 이유를 적습니다.

## 8. PR 리뷰 규칙

### 8.1 리뷰 기준

```text
[ ] Issue와 PR의 기능이 구현되고 정상 실행되는가?
[ ] 담당 범위 밖 변경이나 불필요한 중복 코드가 없는가?
[ ] 파일과 코드 이름이 역할을 표현하는가?
[ ] 합의한 API 계약과 일치하는가?
[ ] 다른 기능 영향, 오류와 빈 데이터가 처리되는가?
[ ] API Key, 비밀번호, 개인정보가 없는가?
[ ] 환경변수·실행 방법 변경이 문서에 반영되었는가?
[ ] 화면 변경의 스크린샷이 첨부되었는가?
```

### 8.2 리뷰 의견 구분

| 구분 | 의미 | 병합 가능 여부 |
| --- | --- | --- |
| `BLOCKER` | 기능 오류, 보안 문제, 병합 불가 | 반드시 수정 |
| `MAJOR` | 주요 로직 또는 설계 문제 | 수정 후 병합 |
| `MINOR` | 가독성, 이름, 작은 개선 | 협의 후 가능 |
| `QUESTION` | 확인할 질문 | 답변 또는 협의 필요 |
| `SUGGESTION` | 선택적 개선 | 반영하지 않아도 가능 |

### 8.3 리뷰 작성 원칙

문제, 이유와 가능한 해결 방향을 함께 적습니다. 예: `[MAJOR] 외부 API 실패가 처리되지 않아 500 응답이 발생할 수 있습니다. 기존 예외 처리 방식에 맞춰 안전한 오류 응답을 반환해 주세요.` 단순히 “이상합니다”라고 쓰지 않습니다.

### 8.4 리뷰 반영

작성자는 코멘트에 답하고 필요한 내용을 수정한 뒤 기존 PR을 업데이트합니다. 새 PR을 만들지 않습니다. 다음은 해당 파일이 실제로 존재하는 작업에서 담당자가 수행하는 예시입니다.

```bash
git add backend/src/main/java/example/service/SearchService.java
git commit -m "fix(backend): 외부 API 오류 처리 추가"
git push origin feature/15-search-api
```

## 9. PR 병합 기준

```text
[ ] Issue 완료 조건 충족
[ ] 구현 기능 정상 실행
[ ] 리뷰어 최소 한 명 승인
[ ] BLOCKER 또는 MAJOR 리뷰 없음
[ ] Branch 충돌 없음
[ ] 민감정보 없음
[ ] 필요한 문서 업데이트
[ ] PR 본문에 확인 결과 기록
```

### 권장 병합 방식

기본 방식은 GitHub의 `Squash and merge`입니다. 여러 Commit을 하나로 정리하고 Squash Commit 제목은 PR 제목과 같은 형식으로 씁니다. 병합 후 담당자가 원격 작업 Branch를 삭제하고 로컬 `main`을 갱신한 뒤 로컬 작업 Branch를 삭제합니다.

```bash
git switch main
git pull origin main
git branch -d feature/12-search-page
```

## 10. Merge Conflict 처리

### 10.1 충돌 발생 시 원칙

- 확인하지 않고 코드를 삭제하거나 `ours`·`theirs`를 무조건 선택하지 않습니다.
- 같은 파일을 수정한 팀원과 확인합니다. 공통 설정은 팀장 또는 영역 담당자와 확인합니다.
- 해결 후 담당 기능과 관련 기능을 다시 실행하고 PR에 결과를 남깁니다.

### 10.2 기본 처리 흐름

현재 작업을 Commit한 후 담당자가 최신 `main`을 가져와 작업 Branch에 병합합니다.

```bash
git switch main
git pull origin main
git switch feature/12-search-page
git merge main
git status
```

충돌 내용을 직접 정리한 뒤 관련 파일을 Stage하고 Commit·Push합니다.

```bash
git add frontend/src/components/SearchResult.jsx
git commit -m "chore(merge): main 병합 충돌 해결"
git push origin feature/12-search-page
```

## 11. 민감정보 관리

생성형 AI·공공데이터 API Key, 데이터베이스 계정과 비밀번호, Access·Refresh Token, 개인 이메일·전화번호 등 개인정보, 실제 `.env`, IDE·운영체제 개인 설정을 GitHub에 Commit하지 않습니다.

프로젝트가 생성된 뒤 필요한 `.env.example`에는 변수명만 기록합니다. 다음은 실제 값이 없는 형식 예시입니다.

```text
AI_API_KEY=
EXTERNAL_API_KEY=
DB_URL=
DB_USERNAME=
DB_PASSWORD=
```

실제 비밀값은 팀이 합의한 별도 채널에서 공유합니다. 비밀값을 실수로 Commit했다면 파일 삭제만으로 끝내지 말고 즉시 팀장에게 알리며 노출된 Key나 Token을 폐기하고 새로 발급합니다.

## 12. 최종 GitHub 운영 규칙 요약

```text
1. 모든 작업은 GitHub Issue 등록 후 시작한다.
2. main에는 직접 Commit하거나 Push하지 않는다.
3. 최신 main에서 Issue별 작업 Branch를 생성한다.
4. 하나의 Branch와 PR에서는 하나의 Issue만 처리한다.
5. Branch 이름에는 작업 유형, Issue 번호, 작업 내용을 포함한다.
6. Commit 메시지와 PR 제목은 Conventional Commits 형식을 사용한다.
7. 변경 파일을 확인한 후 필요한 파일만 Stage한다.
8. 미완료 작업은 Draft PR을 사용한다.
9. PR 대상 Branch는 main이다.
10. PR 본문에 변경 사항과 확인 결과를 기록한다.
11. 최소 한 명의 리뷰 승인을 받은 후 병합한다.
12. BLOCKER 또는 MAJOR 문제가 남아 있으면 병합하지 않는다.
13. 기본 병합 방식은 Squash and merge이다.
14. 병합된 작업 Branch는 원격과 로컬에서 삭제한다.
15. API Key, 비밀번호, Token, 개인정보는 Commit하지 않는다.
16. 충돌은 관련 파일을 수정한 팀원과 확인한다.
17. 병합 후 main을 갱신하고 다음 작업을 시작한다.
```
