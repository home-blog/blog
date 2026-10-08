---

description: "001 회원 가입과 로그인 작업 목록"
---

# Tasks: 회원 가입과 로그인

**Input**: `/specs/001-member-signup-login/`의 설계 문서

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/auth-api.md](contracts/auth-api.md), [quickstart.md](quickstart.md)

**Tests**: 명세에 자동 테스트 요구가 없어서 **테스트 작성 작업은 넣지 않았습니다.** 대신 각 단계 끝의 `Checkpoint`에서 [quickstart.md](quickstart.md)의 시나리오로 확인합니다. 테스트를 먼저 쓰는 방식(TDD)을 원하면 말씀해 주세요. 그때 테스트 작업을 더합니다.

**Organization**: 사용자 이야기(US)별로 묶었습니다. 각 이야기는 따로 만들고 따로 확인할 수 있습니다.

## Format: `[ID] [P?] [Story] 설명`

- **[P]**: 동시에 해도 되는 작업 (다른 파일이고, 끝나지 않은 작업에 기대지 않음)
- **[Story]**: 어느 사용자 이야기의 작업인지 (US1 ~ US4)

## 경로 약속 (가안)

> 코드는 이 문서 저장소가 아니라 **프로젝트 저장소**에 만듭니다. 아래 경로는 `가안`이고, 프로젝트 저장소의 구조가 정해지면 **한 번에 바꿉니다.**

| 줄임 | 실제 경로 (가안) |
|---|---|
| `BE/` | `backend/src/main/java/com/myblog/` (서버, Spring Boot) |
| `BE-RES/` | `backend/src/main/resources/` |
| `FE/` | `frontend/src/` (화면, React + Vite) |

---

## Phase 1: Setup (공통 준비)

**Purpose**: 프로젝트를 만들고 개발 환경을 띄운다

- [x] T001 서버와 화면 폴더를 만든다: `backend/`(Spring Boot), `frontend/`(React + Vite). **Java와 Spring Boot 버전을 정해** `specs/001-member-signup-login/plan.md`의 `Technical Context` 줄을 고친다 (지금 `미정`) — **2026-10-08 작성됨, 서버 컴파일은 CI 확인 대기** (코드 저장소 `feat/001-project-setup`)
- [x] T002 서버 빌드 파일 `backend/pom.xml`(Maven, 2026-10-08 결정)에 Spring Web, Spring Security, Spring Session JDBC, Spring Data JPA, PostgreSQL 드라이버, Spring Data Redis, Spring Mail, Bean Validation, Flyway(가안)를 넣는다 — **2026-10-08 작성됨, CI 확인 대기 (Session JDBC·Flyway는 Boot 4 스타터 사용)** (코드 저장소 `feat/001-project-setup`)
- [x] T003 [P] `frontend/`를 Vite + React로 만들고, 개발 서버 프록시로 `/api` 요청을 서버로 보낸다 (`frontend/vite.config.js`). 화면과 서버를 같은 주소처럼 쓰기 위해서다 (상세/01 `개발 환경`) — **2026-10-08 완료** (린트·빌드 확인)
- [x] T004 [P] 개발용 PostgreSQL과 Redis를 `docker-compose.yml`로 띄운다. 방법은 `docs/4-가이드/03-로컬환경-도커컴포즈-사용법.md`를 따른다 — **2026-10-08 작성됨 (PostgreSQL 18, Redis 8), 실행 확인 대기** (코드 저장소 `feat/001-project-setup`)
- [x] T005 `BE-RES/application.yml`에 `dev`, `prod` 설정을 나눈다. **DB 비밀번호와 SMTP 계정 정보는 환경 변수로만** 넣고 파일과 저장소에 적지 않는다 (공개 저장소, research D-1) — **2026-10-08 작성됨, CI 확인 대기** (코드 저장소 `feat/001-project-setup`)

---

## Phase 2: Foundational (모든 이야기의 바탕)

**Purpose**: 모든 사용자 이야기가 기대는 공통 부분

**⚠️ CRITICAL**: 이 단계가 끝나기 전에는 사용자 이야기 작업을 시작하지 않는다

- [x] T006 **팀 ERD 확인**: 팀 공통 ERD는 그대로 쓰고, 이 기능에 필요한 칸과 인덱스는 **내 확장**으로 더한다 (`docs/3-설계/ERD-변경-요청.md`의 E-1 실패 횟수·잠금 시각, E-2 탈퇴하지 않은 회원 + 소문자 비교 중복 불가). 팀에 요청한 T-1 ~ T-3의 답도 확인한다. E-1을 Redis로 옮길지는 이때 다시 정하고 research.md D-3에 적는다
- [x] T007 DB 표를 만드는 파일 `BE-RES/db/migration/V2__auth_tables.sql`(Flyway, 가안)을 쓴다. `users`: `users_id` BIGINT 자동 증가 기본키, `email` "VARCHAR(255), NOT NULL", `password` "VARCHAR(255), NOT NULL", `nickname` "VARCHAR(20), NOT NULL", `intro` "VARCHAR(100), NULL", `created_at` "TIMESTAMPTZ, NOT NULL", `deleted_at` "TIMESTAMPTZ, NULL", `failed_login_count` "INT, NOT NULL, 기본 0", `locked_until` "TIMESTAMPTZ, NULL". 중복 불가는 "소문자로 맞춘 값이 같은 탈퇴하지 않은 회원은 둘 이상 없다"를 `lower(email)`, `lower(nickname)`과 `WHERE deleted_at IS NULL`인 부분 인덱스로 건다. 가입에 필요한 `blog`, `category`의 최소 칸도 팀 ERD대로 만든다 (칸과 규칙은 `003`이 정함)
- [x] T008 세션 표를 Spring Session JDBC가 정한 모양으로 만든다 (`BE-RES/db/migration/V1__spring_session.sql`). 자동 생성(`initialize-schema`)은 모든 환경에서 끈다 (2026-10-08, 첫 PR 리뷰 반영)
- [x] T009 [P] 설정값 묶음 `BE/user/config/AuthProperties.java`를 만들어 `application.yml`의 `auth.*` 13개 값을 읽는다. 값은 plan.md `설정값 목록`과 같다: 닉네임 2~10, 비밀번호 8~20, 허용 특수문자 `! @ # $ % ^ & * ( ) _ + - =`, 인증번호 6자리, 유효 10분, 다시 받기 1분, 하루 5번, 틀린 횟수 5, 인증됨 30분, 로그인 실패 5, 잠금 10분, 세션 7일, 최대 30일
- [x] T010 [P] 공통 오류 응답 `BE/common/error/ErrorResponse.java`(`code`, `message`, `fieldErrors`)와 `BE/common/error/GlobalExceptionHandler.java`를 만든다. 응답에 예외 이름, 쿼리, 경로를 넣지 않는다 (FR-035, contracts `공통 약속`)
- [x] T011 보안 설정 `BE/user/config/SecurityConfig.java`: CSRF 토큰을 쓰고(화면이 헤더로 보냄), 세션 쿠키는 `HttpOnly`, `SameSite=Lax`, `prod`에서만 `Secure`. 로그인할 때 세션 ID를 새로 만든다(FR-032). 로그인하지 않은 회원 전용 요청에는 `401` + `{"code":"UNAUTHENTICATED"}`를 돌려준다 (contracts 9)
- [x] T012 `GET /api/auth/csrf`를 `BE/user/controller/CsrfController.java`에 만든다 (contracts 1)
- [x] T013 [P] 회원 `BE/user/domain/User.java`와 `BE/user/repository/UserRepository.java`를 만든다. 이메일은 "앞뒤 공백을 지우고 소문자로 맞춰" 저장·조회하고, 조회는 `deleted_at`이 비어 있는 회원만 대상으로 한다
- [x] T014 [P] 입력 규칙 검사 `BE/user/validation/`을 만든다: 이메일 형식, 닉네임 "2~10자, 한글·영문·숫자만. 공백·특수문자 불가", 비밀번호 정규식 `^(?=.*[A-Za-z])(?=.*\d)(?=.*[!@#$%^&*()_+\-=])[A-Za-z\d!@#$%^&*()_+\-=]{8,20}$`, 비밀번호 확인 일치. 숫자는 `AuthProperties`에서 읽는다 (FR-003, 005 ~ 007, FR-034)
- [x] T015 [P] 화면의 요청 도구 `FE/api/client.ts`: 처음에 CSRF 토큰을 받아 모든 POST 헤더에 싣고, `401 UNAUTHENTICATED`를 받으면 "로그인 필요" 신호를 낸다 (US4에서 씀)

**Checkpoint**: 서버가 뜨고, `GET /api/auth/csrf`가 토큰을 주며, 표가 만들어진다

---

## Phase 3: User Story 1 - 이메일 인증을 마치고 회원 가입 (Priority: P1) 🎯 MVP

**Goal**: 이메일 인증을 마친 사람만 가입하고, 가입하면 블로그와 `미분류` 분류가 함께 생긴다

**Independent Test**: quickstart S-1 ~ S-4. 새 이메일로 인증하고 가입하면 계정이 생기고, 같은 이메일로 다시 가입하면 거절된다

### Implementation for User Story 1

- [x] T016 [P] [US1] Redis 저장 `BE/user/verification/EmailVerificationStore.java`: 키와 만료를 data-model 3번 그대로 쓴다 (2026-10-08 리뷰 반영으로 인증 증표 `emailauth:flow` 추가, 6종). `emailauth:code:{email}` 10분, `emailauth:fail:{email}` 10분, `emailauth:cooldown:{email}` 60초, `emailauth:daily:{email}` 24시간(첫 발송부터), `emailauth:verified:{email}` 30분. 이메일은 소문자로 맞춘 값
- [x] T017 [P] [US1] 인증번호 만들기 `BE/user/verification/VerificationCodeGenerator.java`: `SecureRandom`으로 "영문 대문자+숫자 6자리 (`O`, `0`, `I`, `1` 없음)" (FR-014, NF-05)
- [x] T018 [P] [US1] 메일 보내기 `BE/user/mail/VerificationMailSender.java`(인터페이스)와 SMTP 구현, `dev`에서만 쓰는 로그 출력 구현을 만든다. 메일에는 서비스 이름, 인증번호, 유효 시간, "본인이 요청하지 않았다면 이 메일을 무시해 주세요"를 넣는다 (FR-013, FR-014, research B-9)
- [x] T019 [US1] 인증번호 받기 `BE/user/service/EmailVerificationService.java`의 `send`: ① 형식 검사 ② 가입된 이메일이면 **메일을 보내지 않고** 거절 ③ 닉네임 중복 거절 ④ 1분·하루 제한 ⑤ 번호를 만들어 저장(이전 번호 덮어씀) ⑥ **메일이 나갈 때까지 기다려** 보내고 ⑦ 실패하면 번호를 지우고 1분·하루 횟수에 넣지 않는다 (contracts 2, research D-2) — T013, T016 ~ T018 다음
- [x] T020 [US1] 인증번호 확인 `EmailVerificationService.confirm`: 번호가 없으면 `CODE_EXPIRED`, 맞으면 번호를 바로 지우고 인증됨 표시 30분, 틀리면 틀린 횟수 +1, 5번째면 번호를 지우고 `CODE_ATTEMPTS_EXCEEDED`. 입력은 대문자로 바꿔 비교 (contracts 3, FR-015 ~ 017, 019)
- [x] T021 [US1] 이메일 변경 `EmailVerificationService.cancel`: 번호, 틀린 횟수, 인증됨 표시를 지우고 1분·하루 횟수는 남긴다 (contracts 4, FR-021)
- [x] T022 [US1] 가입하기 `BE/user/service/SignupService.java`: 모든 칸 다시 검사 → 인증됨 표시 확인(없으면 `EMAIL_NOT_VERIFIED`) → 이메일·닉네임 중복 재확인 → 비밀번호 BCrypt 해시 → `users`, `blog`(이름 "{닉네임}의 블로그", 소개 비움), `category`(이름 "미분류", 기본 분류 표시 참)를 **한 묶음(트랜잭션)** 으로 저장 → 인증됨 표시 삭제. DB 중복 불가 위반은 `EMAIL_ALREADY_REGISTERED`로 바꿔 답한다. 가입 뒤 자동 로그인은 하지 않는다 (contracts 5, FR-001 ~ 012, 022, 023)
- [x] T023 [US1] `BE/user/controller/AuthController.java`에 `POST /api/auth/email-verifications`, `/confirm`, `/cancel`, `POST /api/auth/signup`을 만든다. 응답 문구는 상세/01 `안내 문구` 표 그대로 (contracts 2 ~ 5)
- [x] T024 [P] [US1] Redis에 연결할 수 없을 때 `503 SERVICE_UNAVAILABLE` "잠시 뒤 다시 시도해 주세요"로 답하게 `GlobalExceptionHandler`에 더한다 (quickstart S-10)
- [x] T025 [US1] 가입 화면 `FE/pages/SignupPage.tsx`: 인증번호 받기 → 확인 → 이메일 칸 잠금 → `이메일 변경`, 비밀번호 규칙 충족을 칸 아래에 바로 표시(FR-008), 칸별 오류와 맨 위 어긴 칸으로 커서 이동·비밀번호 칸 비우기(FR-009), 누른 뒤 버튼 잠금(FR-012), 가입 완료 문구 뒤 로그인 화면으로 이동

**Checkpoint**: quickstart S-1 ~ S-4가 통과한다. 여기까지가 MVP다

---

## Phase 4: User Story 2 - 로그인과 로그아웃 (Priority: P1)

**Goal**: 가입한 회원이 로그인해 7일(최대 30일) 동안 유지하고, 원할 때 로그아웃한다

**Independent Test**: quickstart S-5, S-6

### Implementation for User Story 2

- [ ] T026 [US2] 로그인 `BE/user/service/LoginService.java`: 이메일을 소문자로 맞추고, 탈퇴하지 않은 회원에서 찾고, 이메일이 없든 비밀번호가 틀리든 **같은 응답** `INVALID_CREDENTIALS`를 준다. 성공하면 세션을 만들고 회원을 찾는 이름표(principal)에 `users_id`를 넣는다 (FR-024, 026, data-model 4)
- [ ] T027 [US2] 세션 유지 `BE/user/config/SessionConfig.java`: 마지막 사용 후 7일, 쿠키도 7일. "로그인한 때부터 30일"은 요청마다 세션을 만든 시각을 확인하는 `BE/user/security/SessionAbsoluteTimeoutFilter.java`로 지킨다 (FR-030, research R-2)
- [ ] T028 [US2] `AuthController`에 `POST /api/auth/login`, `POST /api/auth/logout`(세션 삭제 + 쿠키 만료, 이미 로그아웃이어도 204), `GET /api/auth/me`를 더한다 (contracts 6 ~ 8, FR-031)
- [ ] T029 [US2] **쿠키 연장 확인 (research R-1)**: 요청을 계속할 때 브라우저 쿠키의 만료 시각도 뒤로 밀리는지 quickstart S-6의 4번으로 확인하고, 안 밀리면 요청 때 쿠키를 다시 내려 주는 장치를 `SessionConfig`에 더한다. 결과를 research.md R-1에 적는다
- [ ] T030 [US2] 화면 `FE/auth/AuthContext.jsx`(처음 열 때 `GET /api/auth/me`로 로그인 상태 확인)와 `FE/pages/LoginPage.jsx`(성공하면 이전 화면, 로그인 화면에서 직접 왔으면 첫 화면), 로그아웃 버튼(로그인이 필요한 화면이면 첫 화면으로)을 만든다 (FR-025, 031)

**Checkpoint**: S-5, S-6이 통과하고 US1도 그대로 동작한다

---

## Phase 5: User Story 3 - 로그인 연속 실패 시 계정 보호 (Priority: P2)

**Goal**: 같은 계정으로 연속 5회 실패하면 10분 동안 잠근다

**Independent Test**: quickstart S-7

### Implementation for User Story 3

- [ ] T031 [P] [US3] `UserRepository`에 실패 횟수를 **DB에서 바로 +1** 하는 쿼리와 0으로 되돌리는 쿼리, 잠금 시각을 넣는 쿼리를 더한다. 값은 파라미터로 넘기고 문자열을 이어 붙이지 않는다 (헌법 IV, data-model 1 `로그인 잠금의 상태 변화`)
- [ ] T032 [US3] `LoginService`에 잠금을 더한다: 잠겨 있으면 비밀번호를 비교하지 않고 `423 ACCOUNT_LOCKED` + `retryAfterSeconds`, 잠금 시각이 지났으면 횟수를 0으로 되돌리고 진행, 틀리면 +1 하고 5번째면 `locked_until` = 지금 + 10분과 잠금 문구, 성공하면 0, 없는 이메일은 기록하지 않음, 남은 횟수는 어디에도 넣지 않음 (FR-027 ~ 029) — T031 다음
- [ ] T033 [US3] `FE/pages/LoginPage.jsx`에 잠금 문구 "로그인 시도가 5회 실패해 잠겼습니다. {N}분 뒤에 다시 시도해 주세요"를 넣고, `{N}`은 `retryAfterSeconds`를 올림한 분으로 보여 준다

**Checkpoint**: S-7이 통과하고 US1, US2도 그대로 동작한다

---

## Phase 6: User Story 4 - 로그인이 필요한 기능에서 로그인으로 안내 (Priority: P3)

**Goal**: 로그인하지 않고 회원 전용 기능을 누르면 로그인 창을 띄우고, 로그인하면 하려던 화면으로 돌아간다

**Independent Test**: quickstart S-8

### Implementation for User Story 4

- [ ] T034 [US4] 로그인 창 `FE/auth/LoginModal.jsx`와 회원 전용 화면 감싸개 `FE/auth/RequireLogin.jsx`: `client.js`의 "로그인 필요" 신호를 받으면 하려던 주소를 기억하고 로그인 창을 띄우며, 성공하면 그 주소로 돌아간다 (FR-033)
- [ ] T035 [US4] `SecurityConfig`의 회원 전용 주소 규칙을 다른 기능이 따라 쓸 수 있게 주석과 `contracts/auth-api.md` 9번에 같은 내용을 맞춘다

**Checkpoint**: S-8의 1번(`401`)과 로그인 창 동작이 통과한다. 글쓰기 버튼은 `003`이 만들어진 뒤 다시 확인한다

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: 여러 이야기에 걸친 마무리

- [ ] T036 [P] 로그 설정 `BE-RES/logback-spring.xml`: 요청 본문을 남기지 않아 비밀번호가 로그에 남지 않게 하고, 인증번호 로그 출력은 `dev`에서만 켠다 (FR-010, research B-9)
- [ ] T037 [P] quickstart S-9(보안 점검), S-10(Redis가 꺼졌을 때)을 실행한다
- [ ] T038 quickstart.md 전체 시나리오를 실행하고, 끝의 `구현 뒤에 채울 것`(실행 명령, 화면 순서)을 채운다
- [ ] T039 사람이 직접 가입 시간을 재서 SC-006(5분, 제안값)을 확인하고 결과를 spec.md Assumptions에 적는다
- [ ] T040 [P] 구현하면서 바뀐 가안(주소, 키 이름, 설정 이름)을 plan.md, research.md, contracts, `docs/2-요구사항/상세/01-인증-인가.md`의 `구현 방식`에 같이 반영한다 (헌법 `작업 흐름`: 두 곳이 같이 바뀐다)
- [ ] T041 FR-036(HTTPS)은 배포 환경(research D-5)이 정해지면 `prod` 설정에서 `Secure` 쿠키와 함께 확인한다

---

## 요구사항 → 작업 연결표

| FR | 작업 |
|---|---|
| FR-001, 002 | T022, T023, T025 |
| FR-003 | T013, T014 |
| FR-004 | T019 |
| FR-005 | T014, T019, T022 |
| FR-006, 007 | T014, T022 |
| FR-008, 009 | T010, T025 |
| FR-010 | T022, T036 |
| FR-011, 012 | T022, T025 |
| FR-013, 014 | T017, T018, T019 |
| FR-015 | T020, T025 |
| FR-016 ~ 018 | T016, T019, T020 |
| FR-019 | T020 |
| FR-020 | T019 |
| FR-021 | T021, T025 |
| FR-022, 023 | T022 |
| FR-024 | T026 |
| FR-025 | T030 |
| FR-026 | T026 |
| FR-027 ~ 029 | T031, T032, T033 |
| FR-030 | T027, T029 |
| FR-031 | T028, T030 |
| FR-032 | T011 |
| FR-033 | T015, T034, T035 |
| FR-034 | T014, T022 |
| FR-035 | T010 |
| FR-036 | T011, T041 |

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 바로 시작한다
- **Foundational (Phase 2)**: Setup 다음. **모든 사용자 이야기를 막는다**
- **US1, US2 (P1)**: Foundational 다음. 서로 기대지 않는다. US2를 확인하려면 가입된 계정이 필요하지만, 확인용 계정을 DB에 직접 넣어도 된다
- **US3 (P2)**: US2의 `LoginService` 다음
- **US4 (P3)**: Foundational의 T015 다음. US2의 로그인 화면이 있어야 끝까지 확인할 수 있다
- **Polish**: 원하는 이야기가 끝난 뒤

### Within Each User Story

- 저장(표, Redis) → 서비스 → 주소(컨트롤러) → 화면 순서
- 한 이야기를 끝내고 Checkpoint를 통과한 뒤 다음 이야기로 간다

### Parallel Opportunities

- T003, T004는 동시에 해도 된다
- T009, T010, T013, T014, T015는 서로 다른 파일이라 동시에 해도 된다
- US1의 T016, T017, T018은 동시에 해도 된다
- Foundational이 끝나면 US1과 US2를 다른 사람이 동시에 할 수 있다

---

## Parallel Example: User Story 1

```text
동시에:
Task: "T016 EmailVerificationStore (Redis 키 5종)"
Task: "T017 VerificationCodeGenerator (6자리)"
Task: "T018 VerificationMailSender (SMTP + dev 로그)"
그다음: T019 → T020 → T021 → T022 → T023 → T025
```

---

## Implementation Strategy

### MVP First (User Story 1만)

1. Phase 1, 2를 끝낸다
2. Phase 3(US1)을 끝낸다
3. **멈추고 확인**: quickstart S-1 ~ S-4
4. 보여 줄 수 있으면 보여 준다

### Incremental Delivery

1. Setup + Foundational → 바탕 준비
2. US1(가입) → 확인 → MVP
3. US2(로그인) → 확인
4. US3(잠금) → 확인
5. US4(로그인 유도) → 확인
6. 다음 기능 `002` 계정 관리로 넘어간다 (002는 001의 세션, 회원 표, 잠금 칸을 그대로 쓴다)

---

## Notes

- 작업 하나 또는 묶음 하나를 끝낼 때마다 커밋한다
- `가안`인 주소, 키 이름, 설정 이름을 바꾸면 T040처럼 문서도 같이 고친다
- 남은 결정: research D-1(SMTP 계정, 실제 메일을 보낼 때까지 미뤄도 됨), D-5(배포·HTTPS)
