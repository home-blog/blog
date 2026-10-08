# Specification Quality Checklist: 블로그·분류·글

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

- 기술 용어 검사(Redis, SMTP, Spring, BCrypt, JWT, PostgreSQL, React, 세션, 쿠키, HTTPS, 해시, API, 데이터베이스, DB, 트랜잭션, 서버, 마크다운, HTML 등)에서 본문 누출 없음. 원본의 `구현 방식` 섹션과 `쉬운 설명`은 옮기지 않았다.
- `NEEDS CLARIFICATION` 표시는 0개. 원본의 "정하지 못한 것"(블로그를 여러 개 만들 수 있게 할지)은 Assumptions의 "아직 정하지 않은 것"에 원본 그대로 옮겼다.
- 원본 `docs/2-요구사항/상세/03-코어-블로그-글.md`의 `구현 방식` 앞부분에서 뽑은 ID 53개가 모두 spec에 있다(누락 0개). 세부 ID 44개(CF-16-1 포함)는 모두 FR 끝에 연결되어 있다. 기능 묶음·참조 ID 9개 중 CF-06, CF-12, CF-13은 FR에 연결했고, 나머지 CF-03, CF-04, CF-05, CF-07, CF-08, CF-09는 Input 설명과 Assumptions에 적었다. 반대로 spec에는 원본에 없는 CF-10, CF-11, CF-17이 `04-탐색`을 가리키는 Assumptions에만 나온다.
- 원본에서 세부 규칙이 없는 `CF-06`(본문 필수), `CF-12`(작성자 권한)는 각각 FR-011, FR-019에, `CF-16-1`(로그인 유도)은 FR-008에 함께 연결했다. 로그인 유도의 규칙 자체는 `001-member-signup-login`에서 정한다.
- 원본의 `안내 문구` 표 8개와 `기본값` 표의 값(블로그 이름 1~30자, 소개 0~200자, 제목 1~100자, 본문 1~10,000자, 분류 이름 1~20자)을 그대로 따랐다. 새로 만든 숫자는 SC-011의 3분 하나뿐이다.
- SC-011(글쓰기를 시작해 저장한 글의 상세 화면을 볼 때까지 3분 안)은 기존 문서에 없는 **제안값**이다. 팀 확인이 필요하다.
- 원본에 없는 해석: 확인 창(나가기, 글 삭제, 공개로 바꾸기)에서 취소하면 아무것도 바뀌지 않는다는 시나리오와, 길이 규칙을 어겼을 때 "저장되지 않는다"까지만 정하고 문구는 정하지 않은 것. 둘 다 Assumptions에 적었다.
- FR-044(화면을 거치지 않은 요청에도 같은 권한 규칙)는 원본 표에 없는 문장이다. `CF-04-3`, `CF-05-12`, `CF-08-1`의 취지를 `001-member-signup-login`의 FR-034와 같은 방식으로 묶은 것이다.
- FR-045와 SC-012(글 제목·본문에 넣은 스크립트가 실행되지 않는다)는 `docs/2-요구사항/상세/07-공통규칙.md`의 NF-07을 이 명세의 글에 적용한 것이다. 같은 규칙을 `005-community-extras`는 댓글에, `004-explore`는 검색어에 적용했다. (헌법 원칙 IV)
- 다음 단계: `/speckit-clarify`(선택) 또는 `/speckit-plan`
