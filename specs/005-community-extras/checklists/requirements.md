# Specification Quality Checklist: 소통과 부가 기능 (댓글·좋아요·태그·신고·이미지)

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

- 기술 용어 검사(Redis, SMTP, Spring, BCrypt, JWT, PostgreSQL, React, MinIO, 세션, 쿠키, HTTPS, 해시, API, DB, 데이터베이스, 서버, 저장소 등)에서 본문 누출 없음. 원본 CF-23-1의 "데이터베이스에서 직접 확인한다"는 "운영자가 저장된 기록에서 직접 확인한다"로 바꿔 옮겼다.
- `[NEEDS CLARIFICATION]` 표시는 0개다. 원본의 "정하지 못한 것"(이미지 저장 위치, 관리자 기능 시기)은 Assumptions의 "아직 정하지 않은 것"에 옮겼고, 이미지 저장 위치는 "저장 방식은 `plan` 단계에서 정한다"고만 적었다.
- 원본 `docs/상세/05-소통-부가.md`의 요구사항 ID(`## 구현 방식` 앞부분) 36개가 모두 spec에 있다. 세부 ID 29개(CF-16-1, CF-18-1~7, CF-19-1~4, CF-20-1~4, CF-21-1~5, CF-22-1~5, CF-23-1, CF-24-1~2)와 묶음 ID(CF-18~CF-24)를 확인했다. CF-23-1, CF-24-1, CF-24-2는 "이번 범위에서 만들지 않는 것"이라 FR이 아니라 Assumptions에 ID와 함께 적었다.
- `안내 문구` 표의 5개 문구는 원본과 글자 그대로 같다. `기본값` 표의 숫자(댓글 1~500자와 5초, 태그 5개와 1~15자, 신고 기타 0~200자, 이미지 5MB와 10장)도 그대로 썼다. 표에 문구가 없는 상황(글자 수 초과, 5초 제한, 태그 규칙 위반, 이미지 개수 초과)은 문장을 새로 만들지 않고 "이유를 알려 준다"까지만 정했다.
- SC-009(댓글 등록 1분 이내)는 기존 문서에 없는 **제안값**이다. 팀 확인이 필요하다.
- 원본에 순서가 적혀 있지 않아 **해석한 것**: 태그의 1~15자는 앞의 `#`을 지운 뒤의 글자로 센다. 신고의 "처리"는 접수된 신고로 글을 숨기거나 지우는 것으로 본다.
- 원본 05에는 없지만 다른 문서에서 가져온 것: 비회원이 신고를 누르면 로그인 창을 띄운다(`04-탐색.md` CF-17-2), 글을 지우면 좋아요와 태그 연결도 지운다(`03-코어-블로그-글.md` CF-05-15), 화면을 거치지 않은 요청에도 같은 규칙을 적용한다(NF-02의 취지, 001과 같은 방식). FR-028, FR-029와 Assumptions에 출처를 적었다.
- FR-030과 SC-010(댓글에 넣은 스크립트가 실행되지 않는다)은 `docs/상세/07-공통규칙.md`의 NF-07을 댓글에 적용한 것이다. 원본 05에는 적혀 있지 않지만 팀 공통 요구이고 헌법 원칙 IV이므로 검증 단계에서 더했다.
- 다음 단계: `/speckit-clarify`(선택) 또는 `/speckit-plan`
