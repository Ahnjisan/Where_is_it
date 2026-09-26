# 명세 문서 안내

## 문서 역할

현재 Backend와 Database 구현 기준은 다음 확정본입니다.

- [API 명세서 v2](whereisit-api-spec-v2.xlsx)
- [테이블 명세서 v2](whereisit-table-spec-v2.xlsx)

두 canonical 확정본의 내부 버전은 v2.1이며 기준일은 2026-09-26입니다.

원본 및 historical source는 다음 경로에 보관합니다.

- [API v0.1 DRAFT 원본 Snapshot](source/whereisit-api-spec-v0.1-draft.xlsx)
- [한글 SQL historical source](source/whereisit_schema_ko_v0_3.sql)

## 원본과 확정본 구분

API 원본의 내부 버전은 `API v0.1 DRAFT`입니다. API 명세서 v2는 이 원본을 복제한 뒤 현재 결정과 PR #42의 공통 오류 처리 내용을 반영한 확정본입니다. 원본 Snapshot은 수정하지 않았습니다.

한글 SQL은 historical source이며 현재 구현 기준이 아닙니다. 현재 DB 구현 기준은 영문 물리 식별자를 사용하는 테이블 명세서 v2입니다.

## 누락 원본

다음 파일은 원본이 확인되지 않아 저장소에 추가하지 않았습니다.

- `whereisit_schema_simple_v0_3.sql`
- `whereisit_table_spec_ko_v0_3.md`

두 파일은 XLSX 또는 다른 문서에서 역생성하지 않았습니다.

## SHA-256

| 문서 | SHA-256 |
| --- | --- |
| API v0.1 DRAFT 원본 | `6c1ce371e4f3f27945a10248976c0b80ff4a47b6378c8c536e42c23e963d7c00` |
| API v2 확정본 | `1ae387ca8ca334d173f15edbbc1b6f2229700c8679a35d83832ec087a8252940` |
| 테이블 명세서 v2 | `0caf73f18cd11feefcbd93de77da6ed0f6434ffc87828a71ed41062438b442d8` |
| 한글 SQL historical source | `29f917b4c3084a697204619c8eb1ed4b9cbfa3992172fc3df49290b8debf62f9` |

## 참조 규칙

- Backend와 Frontend 구현에서는 `source`가 아닌 v2 확정본을 참조합니다.
- Issue와 PR에서는 저장소 상대경로로 문서를 참조합니다.
- 명세를 변경할 때 버전, 기준일, SHA-256을 함께 갱신합니다.
