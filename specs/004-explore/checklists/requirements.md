# Specification Quality Checklist: 글 탐색 (글 목록과 검색)

**Purpose**: Validate specification completeness and quality before proceeding to planning
**Created**: 2026-10-07
**Feature**: [spec.md](../spec.md)

## Content Quality

- [x] No implementation details (languages, frameworks, APIs)
- [x] Focused on user value and business needs
- [x] Written for non-technical stakeholders
- [x] All mandatory sections completed

## Requirement Completeness

- [x] No [NEEDS CLARIFICATION] markers remain
- [x] Requirements are testable and unambiguous
- [x] Success criteria are measurable
- [x] Success criteria are technology-agnostic (no implementation details)
- [x] All acceptance scenarios are defined
- [x] Edge cases are identified
- [x] Scope is clearly bounded
- [x] Dependencies and assumptions identified

## Feature Readiness

- [x] All functional requirements have clear acceptance criteria
- [x] User scenarios cover primary flows
- [x] Feature meets measurable outcomes defined in Success Criteria
- [x] No implementation details leak into specification

## Notes

- 기술 용어 검사(Redis, SMTP, Spring, BCrypt, JWT, PostgreSQL, React, 세션, 쿠키, HTTPS, 해시, API, 데이터베이스, DB, SQL, 쿼리, 인덱스, 서버, JSON, 형태소, 스크립트 등)에서 본문 누출 없음. 원본의 `구현 방식`과 `쉬운 설명`은 옮기지 않았다.
- `[NEEDS CLARIFICATION]` 표시는 0건(`grep -c` 결과 0).
- 원본 `docs/2-요구사항/상세/04-탐색.md`의 `구현 방식` 앞부분에서 뽑은 ID 25개(CF-10, CF-10-1~9, CF-11, CF-11-1~9, CF-16-1, CF-17, CF-17-1~2, NF-07)가 모두 spec에 있음. 요구사항 표의 세부 ID 20개(CF-10-1~9, CF-11-1~9, CF-17-1~2)는 FR 20개의 끝에 `(CF-xx-x)` 형태로 하나씩 연결됨. CF-10, CF-11, CF-17은 묶음 제목에, CF-16-1은 FR-020 본문에, NF-07은 FR-018 끝에 연결됨. 연결하지 못한 ID는 없음.
- spec에만 있는 ID는 NF-08(기본값은 바꿀 수 있어야 한다)과 NF-09(글 목록은 2초 안)다. 둘 다 `07-공통규칙.md`에 있고 `04-탐색.md`에는 없다. NF-09는 SC-009에, NF-08은 Assumptions에 반영함.
- 안내 문구 3개("글이 없습니다", "검색어를 2자 이상 입력해 주세요", "검색 결과가 없습니다")와 `기본값` 표의 값(10개, 100자, 2~50자)은 원본 그대로 씀. 새로 만든 제안값은 없다.
- 원본에 없어 **읽는 방식을 정한 것**(Assumptions에 적음): 글 목록은 한 블로그의 글 목록, "N개의 글"의 N은 지금 보고 있는 목록의 전체 글 수, 검색은 여러 블로그의 공개 글 대상, 여러 단어는 제목과 본문 중 어느 쪽에 있어도 됨. 팀 확인이 필요하다.
- 원본에 없어 **정하지 않고 남긴 것**(Assumptions의 "아직 정하지 않은 것"): 50자를 넘는 검색어, 잘못된 페이지 번호(1보다 작은 값·숫자가 아닌 값, 검색 결과의 범위 밖 번호), 검색 결과 본문 앞부분의 길이, NF-09의 측정 조건. 원본에 "정하지 못한 것"으로 적힌 항목은 없다.
- 용어는 "분류(카테고리)"만 쓰고, "주제"는 구분을 위해 한 번만 언급함.
- 다음 단계: `/speckit-clarify`(선택) 또는 `/speckit-plan`
