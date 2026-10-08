---

description: "002 계정 관리 (마이페이지) 작업 목록"
---

# Tasks: 계정 관리 (마이페이지)

**Input**: `/specs/002-account-management/`의 설계 문서

**Prerequisites**: [plan.md](plan.md), [spec.md](spec.md), [research.md](research.md), [data-model.md](data-model.md), [contracts/account-api.md](contracts/account-api.md), [quickstart.md](quickstart.md)

**결정 반영 (2026-10-08)**: research의 `D-1 ~ D-6`이 모두 정해졌다. 이 목록은 그 결정을 따른다.

| ID | 결정 | 이 목록에서 |
|---|---|---|
| D-1 | 회원 줄은 남기고(`deleted_at`) 남는 댓글은 그대로. 화면에 "탈퇴한 사용자" | T031, T035 |
| D-2 | 탈퇴할 때 이메일·비밀번호·닉네임·소개를 알아볼 수 없는 값으로 바꾼다 (FR-030) | T031 |
| D-3 | 잠금 문구 "비밀번호를 5회 잘못 입력해 잠겼습니다. {N}분 뒤에 다시 시도해 주세요" | T024 |
| D-4 | 현재 비밀번호가 맞으면 실패 횟수를 0으로 | T024 |
| D-5 | 내가 한 신고는 남긴다 (신고 표는 `005`가 만든다) | T035 |
| D-6 | 비밀번호 변경 알림 메일은 이번에 넣지 않는다 (추후 확장 후보) | T037 |

**Tests**: `001`의 코드에 테스트가 있으므로(`backend/src/test/java/**`) 이 기능도 **사용자 이야기마다 서버 테스트 작업**을 넣었다. 테스트는 [quickstart.md](quickstart.md)의 시나리오 번호를 그대로 이름에 붙인다. 화면에는 아직 테스트 도구가 없어서(`frontend/package.json`에 없음) 화면은 각 단계 끝의 `Checkpoint`에서 손으로 확인한다.

**Organization**: 사용자 이야기(US)별로 묶었습니다. 각 이야기는 따로 만들고 따로 확인할 수 있습니다. PR도 이야기 하나에 하나씩 올립니다 (CLAUDE.md `커밋과 올리기`).

## Format: `[ID] [P?] [Story] 설명`

- **[P]**: 동시에 해도 되는 작업 (다른 파일이고, 끝나지 않은 작업에 기대지 않음)
- **[Story]**: 어느 사용자 이야기의 작업인지 (US1 ~ US4)

## 경로 약속

> `001`과 같다. 코드는 이 저장소의 `backend/`, `frontend/`에 있다. 테스트 경로 `BE-TEST/`를 더했다.

| 줄임 | 실제 경로 |
|---|---|
| `BE/` | `backend/src/main/java/com/myblog/` (서버, Spring Boot) |
| `BE-RES/` | `backend/src/main/resources/` |
| `BE-TEST/` | `backend/src/test/java/com/myblog/` (서버 테스트) |
| `FE/` | `frontend/src/` (화면, React + Vite) |

## 쉬운 설명: 이미 있는 것과 새로 만드는 것

`001`이 이 기능의 바탕을 거의 다 만들어 두었다. **다시 만들지 않고 그대로 쓴다.**

| 이미 있는 것 (`001`) | 이 기능에서 |
|---|---|
| `users` 표의 `failed_login_count`, `locked_until` (`V2__auth_tables.sql`) | 비밀번호 변경·탈퇴에서 틀린 횟수도 **같은 칸에 합산** (FR-018) |
| `LoginAttemptService` + `LoginAttemptListener` (Spring Security 인증 이벤트로 실패를 센다) | 현재 비밀번호 확인도 **같은 `AuthenticationManager`로** 하면 횟수 세기·잠금·0으로 되돌리기(D-4)가 저절로 된다 (T024) |
| `MemberPrincipal`의 이름(`getUsername`) = `users_id` | Spring Session의 `FindByIndexNameSessionRepository`가 이 이름으로 "이 회원의 세션 목록"을 찾는다 (T020) |
| `MemberRegisteredEvent` → `blog`의 `BlogProvisioner`(`@EventListener`) | 탈퇴도 같은 방식: `user`가 `MemberWithdrawnEvent`를 내고, `blog`가 듣고 **자기 표를 스스로** 지운다 (T030, T032) |
| `@ValidNickname`, `@ValidPassword`, `@PasswordConfirmed`, `UserInputRules`, `AuthProperties` | 닉네임·새 비밀번호 검사에 그대로 쓴다 |
| `ErrorCode`, `ApiException`, `GlobalExceptionHandler`, `FE/api/client.ts`의 `ApiError` | 새 오류 이름만 더한다 |
| `RequireLogin`, `LoginModal`, `useAuth`, `index.css`의 원고지 디자인 토큰 | 마이페이지 화면에 그대로 쓴다 |

**모듈 방향** (Spring Modulith, `ModularityTest`): `user ← blog ← post …`. `user`는 `common`만 부를 수 있다. 그래서 `user`는 `blog`를 **직접 부르지 않는다.** 탈퇴 정리는 이벤트로(T030, T032), 마이페이지의 "내 블로그 번호"는 `user`가 정한 질문 틀(인터페이스)을 `blog`가 채우는 방식으로(T010) 한다.

---

## Phase 1: Setup (공통 준비)

**Purpose**: 이 기능을 위한 새 준비는 **없다.** 프로젝트, 빌드 파일, DB, Redis, 설정 나누기, CI는 `001`의 T001 ~ T005, T008에서 끝났다. 새 Flyway 파일(`V3__…`)도 만들지 않는다 (data-model: "이 기능은 새 표를 만들지 않는다", 필요한 칸은 `V2`에 이미 있다).

- [x] T001 시작 전 확인: `001`의 T026 ~ T035(로그인, 잠금, 로그인 창)가 `main`에 merge되어 있고 CI가 통과하는지 본다. US마다 브랜치를 만든다 (`feat/002-us1-profile`, `feat/002-us2-password`, `feat/002-us3-withdrawal`, 가안)

---

## Phase 2: Foundational (모든 이야기의 바탕)

**Purpose**: US1 ~ US3이 함께 기대는 부분

**⚠️ CRITICAL**: 이 단계가 끝나기 전에는 사용자 이야기 작업을 시작하지 않는다

- [x] T002 [P] 설정값 묶음 `BE/user/config/AccountProperties.java`(`@ConfigurationProperties(prefix = "account")`, `@ConfigurationPropertiesScan`으로 자동 등록)를 만들고 `BE-RES/application.yml`에 `account.intro.min-length: 0`, `account.intro.max-length: 100`을 더한다 (plan `설정값 목록`, 헌법 VI, FR-008)
- [x] T003 [P] `BE/common/error/ErrorCode.java`에 이 기능의 오류 이름을 더한다 (contracts 2 ~ 4): `CURRENT_PASSWORD_MISMATCH`(400, "현재 비밀번호가 올바르지 않습니다"), `PASSWORD_SAME_AS_CURRENT`(400, "현재 비밀번호와 다른 값을 입력해 주세요"), `WITHDRAWAL_CONFLICT`(409, "※ 잠시 뒤 다시 시도해 주세요"), 칸별 문구 `INTRO_TOO_LONG`("※ 소개는 100자 이하로 입력해 주세요"), `CURRENT_PASSWORD_REQUIRED`("※ 현재 비밀번호를 입력해 주세요"), `PASSWORD_REQUIRED`("※ 비밀번호를 입력해 주세요"), `WITHDRAWAL_NOT_AGREED`("※ 탈퇴 안내를 확인해 주세요"). `CURRENT_PASSWORD_MISMATCH`는 **401이 아니다** (401은 로그인 창을 띄우는 약속, research B-2)
- [x] T004 [P] `BE/user/repository/UserRepository.java`에 쿼리를 더한다: `findActiveById(id)`(탈퇴하지 않은 회원만), `existsActiveByNicknameExcept(nickname, id)`(소문자 비교, 나는 빼고). 값은 파라미터로만 넘긴다 (헌법 IV, FR-007)
- [x] T005 **세션 속 회원을 DB에서 다시 확인**하는 곳 `BE/user/service/CurrentMemberService.java`: `@AuthenticationPrincipal MemberPrincipal`의 `users_id`로 `findActiveById`를 부르고, 없으면(탈퇴함) `401 UNAUTHENTICATED`를 던진다 (research B-1, FR-002, FR-029). `BE/user/controller/AuthController.java`의 `GET /api/auth/me`도 이것을 써서 **DB의 닉네임**을 돌려주게 고친다 — 지금은 세션에 저장된 `MemberPrincipal.nickname`을 돌려주므로 닉네임을 바꿔도 머리글이 옛 이름으로 남는다 (contracts 2의 5단계) — T004 다음
- [x] T006 [P] 화면 주소 도구를 바꾼다: `FE/App.tsx`의 `<BrowserRouter>`를 `createBrowserRouter` + `<RouterProvider>`로 바꾸고, `AuthProvider`·`LoginModalProvider`·`SiteHeader`는 맨 위 레이아웃 경로(`<Outlet />`)에 둔다. "저장하지 않은 내용" 확인(FR-011)에 쓰는 React Router의 `useBlocker`는 이 방식에서만 동작한다. 기존 `/`, `/login`, `/signup` 동작은 그대로인지 확인한다
- [x] T007 [P] `FE/auth/useAuth.ts`, `FE/auth/AuthContext.tsx`에 `refresh()`(다시 `GET /api/auth/me`, 닉네임을 바꾼 뒤 머리글용)와 `signedOut()`(서버에 로그아웃을 보내지 않고 화면만 로그아웃 상태로, 탈퇴 뒤용)을 더한다
- [x] T008 [P] 화면 요청 함수 `FE/account/accountApi.ts`(`getAccount`, `updateProfile`, `changePassword`, `withdraw`, 응답 타입은 contracts 1 ~ 4)를 만들고, `FE/auth/rules.ts`에 `INTRO_MAX = 100`과 소개 문구, 현재 비밀번호·탈퇴 문구를 더한다 (서버 `account.*`·contracts 문구와 같게)
- [x] T009 화면 주소 `/mypage`(2026-10-08 US1에서. `/mypage/password`는 US2, `/mypage/withdrawal`은 US3에서 더한다), `/mypage/password`, `/mypage/withdrawal`(가안, research B-1: 주소에 회원 번호 없음)을 `FE/App.tsx`에 `<RequireLogin>`으로 감싸 더한다. `FE/components/SiteHeader.tsx`에 `마이페이지` 링크를 넣고, `MEMBER_ONLY_PREFIXES`의 `'/me'`를 `'/mypage'`로 맞춘다 (로그아웃하면 첫 화면으로, `001` FR-031) — T006 다음

**Checkpoint**: 서버 빌드·테스트(CI)와 `ModularityTest`가 통과한다. 로그인한 상태에서 `/mypage`를 열면 빈 화면이, 로그아웃 상태면 로그인 창이 뜬다. `GET /api/auth/me`가 DB의 닉네임을 돌려준다

---

## Phase 3: User Story 1 - 마이페이지에서 내 정보 보고 고치기 (Priority: P1) 🎯 MVP

**Goal**: 로그인한 회원이 내 정보(이메일, 닉네임, 소개, 가입일)를 보고 닉네임·소개를 고친다. 다른 회원의 정보는 볼 방법이 없다

**Independent Test**: quickstart S-1, S-2, S-3

### Tests for User Story 1

- [x] T010 [P] [US1] `BE-TEST/user/controller/AccountProfileTest.java` (`LoginFlowTest`처럼 `@SpringBootTest` + MockMvc + `springSecurity()`): S-1의 2·4·5(응답에 비밀번호·해시 없음, 로그인 안 하면 네 주소 모두 `401`, 요청에 남의 `userId`를 넣어도 나만 바뀜), S-2의 2 ~ 9·11(이메일 무시, 내 닉네임 그대로·대소문자만 바꿈 허용, 남의 닉네임 `409`+`fieldErrors.nickname`, 1자·11자·특수문자, 소개 100자 통과·101자 거절·이모지는 한 글자로 셈, 빈 소개 허용, 저장 뒤 `GET /api/auth/me`의 닉네임이 바뀜), CSRF 토큰 없으면 거절(S-11의 1)
- [x] T011 [P] [US1] `BE-TEST/user/repository/UserRepositoryTest.java`에 더한다: 두 회원이 같은 새 닉네임(대소문자만 다름)으로 바꾸면 DB의 `uq_users_nickname_active`가 두 번째를 막는다 (S-2의 10, SC-002)

### Implementation for User Story 1

- [x] T012 [P] [US1] "내 블로그 번호" 묻기: 질문 틀 `BE/user/MemberBlogLookup.java`(`Optional<Long> blogIdOf(Long memberId)`, `user` 맨 위 패키지라 다른 모듈에 보인다)를 만들고, `blog` 모듈이 `BE/blog/service/MemberBlogLookupAdapter.java`로 채운다(`BlogRepository.findByOwnerId`). `user`는 인터페이스만 알고 `blog`를 직접 부르지 않으므로 모듈 방향(`user ← blog`)이 지켜진다 (FR-003, contracts 1)
- [x] T013 [P] [US1] 소개 검사 `BE/user/validation/ValidIntro.java` + `IntroValidator.java`: `account.intro.max-length` 이하, **글자 단위(코드 포인트)로 센다**(research B-4: Java 길이는 이모지를 2로 셀 수 있음), 비어 있거나 없어도 된다. 오류 이름 `INTRO_TOO_LONG`, 문구의 숫자는 `AccountProperties`에서 채운다(`FieldErrorMessages`를 구현하는 `BE/user/validation/AccountFieldErrorMessages.java`) (FR-008)
- [x] T014 [US1] `BE/user/domain/User.java`에 `changeProfile(nickname, intro)`를 더한다: 닉네임은 앞뒤 공백을 지우고, 소개는 입력 그대로 두되 빈 문자열이면 `NULL`. 이메일을 바꾸는 메서드는 만들지 않는다 (FR-005, FR-006, research B-4)
- [x] T015 [US1] `BE/user/service/ProfileService.java`: `view(memberId)` → 이메일·닉네임·소개(없으면 `""`)·가입일(`created_at`, JPA Auditing이 채운 값)·블로그 번호(T012). `update(memberId, nickname, intro)` → `existsActiveByNicknameExcept`로 중복 확인 → `changeProfile` → `flush`. DB 중복 불가 위반(`uq_users_nickname_active`)은 `NICKNAME_ALREADY_USED`로 바꾸고 `fieldErrors`의 `nickname` 칸에 넣는다(`SignupService.duplicate`와 같은 모양). 같은 값이어도 오류 없이 `200` (contracts 2, FR-007, FR-009, FR-010) — T004, T012 ~ T014 다음
- [x] T016 [US1] `BE/user/controller/AccountController.java`(`/api/account`)와 요청 본문 `BE/user/controller/dto/AccountRequests.java`(`UpdateProfile(@ValidNickname nickname, @ValidIntro intro)`, **이메일 칸 없음**)를 만든다. `GET /api/account`, `PATCH /api/account/profile`. 회원은 `@AuthenticationPrincipal` → `CurrentMemberService`로만 정하고 **주소·본문에서 회원 번호를 받지 않는다**. 성공 문구 "저장했습니다" (contracts 1·2, FR-001 ~ FR-012, FR-029). `SecurityConfig`는 고치지 않는다: `/api/account/**`는 이미 `anyRequest().authenticated()`라 로그인하지 않으면 `401`
- [x] T017 [US1] 마이페이지 `FE/pages/MyPage.tsx` + `FE/pages/mypage.css`: 내 정보(이메일은 **글자로만** 보여 고칠 칸이 없음, 가입일은 날짜로), 내 정보 수정, 비밀번호 변경·회원 탈퇴로 가는 링크, `내 블로그` 바로가기(블로그 주소 모양은 `003`이 정할 때까지 `/blog/{blog.id}` 가안). 프로필 사진 칸은 없다. 디자인은 `index.css`의 원고지 토큰과 `auth-layout.css`의 칸 모양을 따른다 (FR-001, FR-003 ~ FR-005, FR-012)
- [x] T018 [US1] 내 정보 수정 `FE/account/ProfileForm.tsx`: 처음 값과 비교해 **바뀐 것이 없으면(원래 값으로 되돌린 경우 포함) 저장 버튼 잠금**, 저장 중 버튼 잠금, 칸별 오류는 `ApiError.messageFor`로 칸 아래, 성공하면 "저장했습니다" + 처음 값 갱신 + `refresh()`로 머리글 닉네임 갱신 (FR-009, FR-010, contracts 2의 5단계)
- [x] T019 [US1] 저장하지 않은 내용 확인 `FE/account/useUnsavedChangesPrompt.ts`: 바뀐 것이 있을 때 `useBlocker`로 화면 안 이동을 막고 "저장하지 않은 내용이 있습니다. 나갈까요?"를 묻는다. 탭 닫기·새로고침은 `beforeunload`로 브라우저가 묻게 한다(문구는 브라우저 것, research R-5). `ProfileForm`에서 쓴다 (FR-011, SC-009) — T006, T018 다음

**Checkpoint**: T010, T011이 통과하고 quickstart S-1 ~ S-3을 화면으로 확인한다. 여기까지가 MVP다 (PR 하나)

---

## Phase 4: User Story 2 - 비밀번호 변경 (Priority: P1)

**Goal**: 현재 비밀번호를 확인하고 새 비밀번호로 바꾼다. 지금 기기는 로그인이 유지되고 다른 기기는 모두 끊긴다. 틀린 횟수는 로그인 실패와 합산한다

**Independent Test**: quickstart S-4, S-5, S-6

> **먼저 확인 (research R-2)**: T020을 가장 먼저 한다. 회원 기준으로 세션을 찾지 못하면 "다른 기기 끊기"(확정)를 지킬 수 없다.

### Tests for User Story 2

- [ ] T020 [US2] **R-2 확인 + 세션 지우기 도구** `BE/user/security/MemberSessions.java`: Spring Session의 `FindByIndexNameSessionRepository<? extends Session>`를 받아 `findByPrincipalName(String.valueOf(users_id))`로 그 회원의 세션 목록을 얻고, `expireOthers(memberId, keepSessionId)`(지금 세션만 남김), `expireAll(memberId)`를 만든다. 이름표는 `MemberPrincipal.getUsername()`(= `users_id`)이고 세션 표의 `PRINCIPAL_NAME` 칸(`V1`)에 저장된다. 같은 파일에 대한 테스트 `BE-TEST/user/security/MemberSessionsTest.java`: 회원 A 세션 3개, B 세션 1개를 저장소로 직접 만들고 `expireOthers` 뒤 A는 하나, B는 그대로인지 본다. 세션 저장소는 **자기 트랜잭션으로 저장**하므로 이 테스트는 `@Transactional`을 쓰지 않고 끝에서 지운다. 결과를 research R-2에 적는다 (FR-020, SC-005, S-5의 5·6)
- [ ] T021 [P] [US2] `BE-TEST/user/controller/AccountPasswordTest.java`: S-4의 1 ~ 8(성공 뒤 옛 비밀번호 로그인 실패·새 비밀번호 성공, 틀리면 `400 CURRENT_PASSWORD_MISMATCH`(401 아님), 같은 값, 규칙 위반 5종, 확인 불일치, 빈 칸, 실패한 요청 뒤 해시 그대로), S-6의 1·2·5 ~ 8(로그인 3번 + 변경 2번 = 5번째 응답이 `423 ACCOUNT_LOCKED`와 D-3 문구, 잠긴 동안 맞는 비밀번호도 거절, 시간이 지나면 성공하고 횟수 0, 세 곳 합산, D-4: 맞으면 0으로, 형식 오류는 횟수에 안 넣음). **이 테스트는 클래스 전체에 `@Transactional`을 걸지 않는다** — 걸면 "틀린 횟수 +1이 오류 응답과 함께 되돌려지는" 실수(T024, T026, T033)를 잡지 못한다
- [ ] T022 [P] [US2] `BE-TEST/user/controller/AccountSessionFlowTest.java`: MockMvc에 Spring Session의 `SessionRepositoryFilter`를 더해(실제 JDBC 세션을 쓰게) 같은 회원으로 세 번 로그인 → 하나에서 비밀번호 변경 → 그 세션은 `GET /api/auth/me` 성공, 나머지 둘은 `401`, 남은 세션의 만든 시각이 그대로 (S-5의 2 ~ 4·7)

### Implementation for User Story 2

- [ ] T023 [P] [US2] `@PasswordConfirmed`를 다른 칸 이름에도 쓰게 넓힌다: `BE/user/validation/PasswordConfirmed.java`에 `confirmField`(기본 `"passwordConfirm"`) 속성을 더하고 `PasswordConfirmedValidator.java`가 그 이름으로 오류를 붙인다. 비밀번호 변경 요청은 `PasswordConfirmation`의 `password()`·`passwordConfirm()`을 `newPassword`·`newPasswordConfirm`으로 돌려준다. 가입(`AuthRequests.Signup`)은 그대로 동작해야 한다 (FR-017)
- [ ] T024 [US2] **현재 비밀번호 확인은 한 곳에서** `BE/user/service/CurrentPasswordChecker.java` (research B-2): 회원의 현재 이메일로 `001`의 `AuthenticationManager.authenticate(...)`를 **그대로** 부른다. 그러면 잠금 확인(`isAccountNonLocked` → `LockedException`), BCrypt 비교, 틀리면 `+1`·5번째 잠금(`LoginAttemptListener` → `LoginAttemptService.recordFailure`), 맞으면 0(`recordSuccess`, **D-4**)이 이미 있는 코드로 된다. 결과를 바꿔 답한다: `LockedException`이거나 방금 잠겼으면 `423 ACCOUNT_LOCKED` + `retryAfterSeconds` + **D-3 문구** "비밀번호를 %d회 잘못 입력해 잠겼습니다. %d분 뒤에 다시 시도해 주세요"(`%d`는 `auth.login.max-failed-attempts`와 남은 분 올림), 그 밖의 `BadCredentialsException`은 `CURRENT_PASSWORD_MISMATCH`(`fieldErrors.currentPassword`). 남은 시간 계산은 `LoginService.locked()`에서 `LoginAttemptService`로 옮겨 로그인과 함께 쓴다. 남은 횟수는 어디에도 넣지 않는다. **이 확인은 변경·탈퇴 트랜잭션보다 먼저, 그 밖에서 부른다**(안에서 부르면 뒤의 오류로 `+1`까지 되돌려진다) (FR-014, FR-018, FR-022, SC-006)
- [ ] T025 [US2] `BE/user/domain/User.java`에 `changePasswordHash(hash)`를 더한다. 원문은 받지 않는다 (CF-01-8)
- [ ] T026 [US2] `BE/user/service/PasswordChangeService.java`: ① `CurrentPasswordChecker`(트랜잭션 밖) ② 새 비밀번호가 현재와 같으면 `PASSWORD_SAME_AS_CURRENT`(`fieldErrors.newPassword`) ③ `@Transactional` 안에서 BCrypt 해시로 바꿔 저장 ④ **마지막 단계로** `MemberSessions.expireOthers(memberId, 지금 세션 ID)`. 세션 저장소가 바깥 트랜잭션에 묶이는지 확인하고(research R-1), 삭제가 실패하면 오류로 답해 다시 하게 한다. 결과를 research R-1에 적는다 (FR-016, FR-019, FR-020, SC-004, SC-005) — T020, T024, T025 다음
- [ ] T027 [US2] `AccountController`에 `POST /api/account/password`, `AccountRequests.ChangePassword`(`@NotBlank(message = "CURRENT_PASSWORD_REQUIRED") currentPassword`, `@ValidPassword newPassword`, `newPasswordConfirm`, `@PasswordConfirmed(confirmField = "newPasswordConfirm")`)를 더한다. 지금 세션 ID는 `HttpSession.getId()`(Spring Session이 감싼 세션). 성공 문구 "비밀번호를 변경했습니다". 비밀번호는 응답·로그에 넣지 않는다 (contracts 3, FR-013, FR-015, FR-017, FR-019, FR-029, NF-01)
- [ ] T028 [US2] 비밀번호 변경 화면 `FE/account/PasswordChangePage.tsx`: 세 칸(모두 채워야 버튼이 눌림), 새 비밀번호 규칙 충족을 칸 아래에 바로 표시(`rules.ts`의 `checkPassword`, 가입 화면과 같음), 칸별 오류, 잠금은 서버 문구를 그대로 보여 줌, 성공하면 "비밀번호를 변경했습니다"와 세 칸 비우기. `CURRENT_PASSWORD_MISMATCH`는 `400`이라 로그인 창이 뜨지 않는다 (FR-013 ~ FR-019)

**Checkpoint**: T020 ~ T022가 통과하고 quickstart S-4 ~ S-6을 브라우저 두세 개로 확인한다. US1도 그대로 동작한다 (PR 하나)

---

## Phase 5: User Story 3 - 회원 탈퇴 (Priority: P2)

**Goal**: 안내를 읽고 체크하고 비밀번호를 넣고 두 번 확인한 뒤 탈퇴한다. 내 블로그·분류(·나중에 글·댓글·좋아요)는 지우고, 회원 줄은 남기되 개인정보를 알아볼 수 없게 바꾸며, 모든 기기에서 로그아웃된다

**Independent Test**: quickstart S-7, S-8(지금 있는 표만), S-9, S-9a

> **이번에 지우는 범위**: 지금 DB에는 `users`, `blog`, `category`만 있다. 글·댓글·좋아요·태그·이미지·신고·일별 통계 표는 `003`, `005`, `006`이 만든다. **그 표를 이 기능에서 만들지 않는다.** 대신 `user`는 "탈퇴했다" 이벤트를, `blog`는 "블로그를 닫는다" 이벤트를 내고, 나중에 `post`·`comment`·`stats` 모듈이 그것을 듣고 **자기 표를 스스로** 지운다 (T035). 모든 듣는 쪽은 `@EventListener`로 **같은 트랜잭션 안에서 바로** 돌아서, 하나라도 실패하면 탈퇴 전체가 취소된다. 탈퇴 뒤에 따로 도는 `@TransactionalEventListener`·`@ApplicationModuleListener`는 쓰지 않는다(묶음이 깨진다).

### Tests for User Story 3

- [ ] T029 [P] [US3] `BE-TEST/user/service/WithdrawalFlowTest.java` (클래스 전체 `@Transactional` 쓰지 않음, 끝에서 지움): S-7의 5·6(틀린 비밀번호·`agreed: false`면 아무것도 안 지움, 틀린 횟수 +1), S-7의 8(다른 세션도 `401`, `SessionRepositoryFilter` 사용), S-7의 9·S-9a(옛 이메일로 로그인 실패, 회원 줄이 `deleted-{번호}@deleted.invalid`·`!deleted`·`탈퇴한사용자{번호}`·소개 NULL·`deleted_at` 있음), S-8의 1·6(블로그·분류 없음, 회원 줄은 남음), S-8의 7(테스트용 `@EventListener`가 예외를 던지면 블로그·회원 줄이 그대로), S-9의 1·2(같은 이메일·닉네임으로 `MemberRegistration.register` → 새 `users_id`, 새 블로그와 `미분류`만). `ModularityTest`도 통과해야 한다

### Implementation for User Story 3

- [ ] T030 [P] [US3] 이벤트 `BE/user/MemberWithdrawnEvent.java`(`record(Long memberId)`, `user` 맨 위 패키지라 다른 모듈이 들을 수 있다). 주석에 "듣는 쪽은 `@EventListener`로 같은 트랜잭션 안에서 자기 데이터를 지운다"를 적는다 (FR-024)
- [ ] T031 [US3] `BE/user/domain/User.java`에 `withdraw(Instant now)`를 더한다 (**D-2**, FR-030): `deletedAt = now`, `email = "deleted-" + id + "@deleted.invalid"`, `passwordHash = "!deleted"`(BCrypt 모양이 아니라 어떤 비밀번호와도 맞지 않음), `nickname = "탈퇴한사용자" + id`, `intro = null`. 값의 틀은 상수로 두고 data-model 3의 12번과 같게 한다. 회원 줄은 지우지 않는다(**D-1**, 남는 댓글이 가리킴). `UserRepository`에 `findActiveByIdForUpdate(id)`(`@Lock(PESSIMISTIC_WRITE)`)를 더해 탈퇴하는 동안 같은 회원 줄을 잠근다 (research R-3)
- [ ] T032 [P] [US3] `blog` 모듈의 탈퇴 정리: 이벤트 `BE/blog/BlogClosingEvent.java`(`record(Long blogId, Long ownerId)`, `blog` 맨 위 패키지 — `003`의 글, `006`의 통계가 들을 입구)와 `BE/blog/service/BlogWithdrawalCleaner.java`(`@EventListener`로 `MemberWithdrawnEvent`를 받아 ① 내 블로그를 찾고 ② `BlogClosingEvent`를 내서 아래 모듈이 먼저 지우게 하고 ③ 분류를 모두 지우고(`미분류` 포함) ④ 블로그를 지운다). `BE/blog/repository/CategoryRepository.java`에 `deleteByBlogId`(`@Modifying` 쿼리)를 더한다. 팀 ERD에 자동 연쇄 삭제가 없으므로 **자식부터** 지운다 (data-model 3의 9·11번, FR-024, SC-007)
- [ ] T033 [US3] `BE/user/service/WithdrawalService.java`: ① `CurrentPasswordChecker`(트랜잭션 밖, 틀리면 아무것도 지우지 않음, SC-003) ② `@Transactional` 안에서 `findActiveByIdForUpdate` → `MemberWithdrawnEvent` 발행 → `user.withdraw(now)` → `flush`. DB가 거절하면(`DataIntegrityViolationException`, 탈퇴하는 동안 다른 기기에서 글을 쓴 경우 등) `409 WITHDRAWAL_CONFLICT`로 답하고 전부 취소된다 ③ 트랜잭션이 끝난 뒤 `MemberSessions.expireAll(memberId)` (FR-022, FR-024, FR-027, FR-030) — T024, T030 ~ T032 다음
- [ ] T034 [US3] `AccountController`에 `POST /api/account/withdrawal`, `AccountRequests.Withdraw`(`@NotBlank(message = "PASSWORD_REQUIRED") password`, `@AssertTrue(message = "WITHDRAWAL_NOT_AGREED") boolean agreed` — `Boolean`이면 비어 있을 때 통과하므로 `boolean`)를 더한다. 성공하면 Spring Security의 `SecurityContextLogoutHandler`(지금 세션 끝내기·로그인 정보 지우기)와 `CookieClearingLogoutHandler(SessionConfig.COOKIE_NAME)`(쿠키 만료)로 로그아웃시키고 "탈퇴가 완료되었습니다"를 돌려준다 (contracts 4, FR-025, FR-027, FR-029)
- [ ] T035 [US3] 다음 기능이 이어 붙일 곳을 적어 둔다: `MemberWithdrawnEvent`·`BlogClosingEvent`의 주석과 data-model 3 표에 "`003` post 모듈: `BlogClosingEvent`로 글·태그 연결·이미지 기록·글 신고 삭제 / `005` comment 모듈: `BlogClosingEvent`로 내 글의 댓글·좋아요·댓글 신고, `MemberWithdrawnEvent`로 내가 누른 좋아요 삭제, 내가 한 신고는 남김(**D-5**), 탈퇴한 작성자는 닉네임 대신 '탈퇴한 사용자'(**D-1**) / `006` stats: `BlogClosingEvent`로 일별 통계 삭제"를 남긴다. 이미지 파일은 트랜잭션 뒤에 지운다(research R-7) (FR-024)
- [ ] T036 [US3] 탈퇴 화면 `FE/account/WithdrawalPage.tsx`: 되돌릴 수 없다는 것, 삭제되는 것(내 블로그, 글, 분류, 내가 쓴 댓글 중 내 블로그 안의 것, 좋아요), 남는 것(다른 사람 글에 단 댓글, "탈퇴한 사용자"로 표시) 안내, 체크해야 `탈퇴하기`가 눌림, 비밀번호 칸, 누르면 "정말 탈퇴하시겠습니까? 이 작업은 되돌릴 수 없습니다" 확인 창(`<dialog>`), **취소하면 요청을 보내지 않는다**, 확인하면 요청 → 성공 시 `signedOut()` 후 첫 화면으로(`state: { withdrawn: true }`), `FE/pages/HomePage.tsx`가 "탈퇴가 완료되었습니다"를 보여 준다. 틀린 비밀번호·잠금은 칸 아래·문구로 (FR-022, FR-023, FR-025 ~ FR-027)

**Checkpoint**: T029가 통과하고 quickstart S-7, S-8(블로그·분류까지), S-9, S-9a를 화면으로 확인한다. US1, US2도 그대로 동작한다 (PR 하나)

---

## Phase 6: User Story 4 - 비밀번호 변경 알림 메일 (Priority: P3, 선택)

**Goal**: **이번에는 만들지 않는다** (research **D-6** B, 2026-10-08). 코드 작업은 없다

- [ ] T037 [US4] D-6 결정을 문서에 맞춘다: `spec.md`의 US4와 FR-021의 "넣을지 아직 정하지 않았다"를 "이번에는 넣지 않음(추후 확장 후보)"으로, `contracts/account-api.md` 3의 7단계를 같은 뜻으로 고치고, 추후 확장 후보 목록에 "비밀번호 변경 알림 메일 (CF-15-16)"을 올린다. 다시 넣게 되면 research D-6의 "A를 고르면" 가안(저장 뒤 `@Async`로 발송, 실패해도 변경 유지)을 따른다

**Checkpoint**: quickstart S-10은 건너뛴다

---

## Phase 7: Polish & Cross-Cutting Concerns

**Purpose**: 여러 이야기에 걸친 마무리

- [ ] T038 [P] 결정과 구현에 맞게 문서를 같이 고친다 (헌법 `작업 흐름`: 두 곳이 같이 바뀐다): `contracts/account-api.md` 3·4의 `ACCOUNT_LOCKED` 문구를 D-3 문구로, 3의 3단계 "D-4에서 정함"을 "맞으면 0", 1의 "내 블로그 번호"를 T012 방식으로 / `plan.md`의 "아직 하나도 정하지 않았습니다", `정해야 할 것 요약`의 D-2 ~ D-6 줄, `Technical Context`의 버전 `미정`, `Structure Decision`의 "`user` 모듈이 그것들을 한 묶음으로 부른다"(→ 이벤트로) / `data-model.md` 1의 `failed_login_count`·`locked_until` "없음 ⚠"(→ `V2`에 있음) / `docs/2-요구사항/상세/02-계정관리.md`의 `구현 방식` / `docs/3-설계/기술스택-아키텍처.md` 2.2 표의 Spring Session JDBC·Spring 이벤트 줄에 "002 다른 기기 끊기(`FindByIndexNameSessionRepository`), 탈퇴 정리(`MemberWithdrawnEvent`)"
- [ ] T039 [P] quickstart S-11(보안 점검: CSRF 없이 세 변경 요청, 소개의 `<script>`가 글자로 보임, `' OR 1=1 --`, 이상한 JSON, 응답·로그에 비밀번호 없음)과 S-3(저장하지 않은 내용 확인)을 화면에서 실행한다
- [ ] T040 quickstart.md 전체 시나리오를 실행하고 끝의 `구현 뒤에 채울 것`(실행 명령, 화면 순서, 탈퇴용 데이터 만드는 방법, D-1 ~ D-6 반영 문구)을 채운다. `tasks.md` 체크박스와 `CLAUDE.md`의 `6. 지금 상태`를 고친다
- [ ] T041 NF-04(비밀번호 변경은 HTTPS로)는 `001`의 T041과 같이 배포 환경(`001` research D-5)이 정해지면 `prod`의 `Secure` 쿠키와 함께 확인한다

---

## 요구사항 → 작업 연결표

| FR | 작업 |
|---|---|
| FR-001 | T016, T017 |
| FR-002 | T005, T009, T010, T016 |
| FR-003 | T012, T017 |
| FR-004 | T015, T016, T017 |
| FR-005 | T010, T014, T016, T017 |
| FR-006 | T014, T015 |
| FR-007 | T004, T010, T011, T015 |
| FR-008 | T002, T010, T013 |
| FR-009 | T015, T018 |
| FR-010 | T015, T018 |
| FR-011 | T006, T019 |
| FR-012 | T016, T017 |
| FR-013 | T021, T027, T028 |
| FR-014 | T003, T021, T024 |
| FR-015 | T021, T027, T028 |
| FR-016 | T003, T021, T026 |
| FR-017 | T023, T027 |
| FR-018 | T021, T024 |
| FR-019 | T026, T027, T028 |
| FR-020 | T020, T022, T026 |
| FR-021 | T037 (만들지 않음, D-6) |
| FR-022 | T024, T029, T033 |
| FR-023 | T036 |
| FR-024 | T029, T030, T032, T033, T035 |
| FR-025 | T029, T034, T036 |
| FR-026 | T036 |
| FR-027 | T020, T029, T033, T034, T036 |
| FR-028 | T029, T031 |
| FR-029 | T005, T010, T016, T021, T027, T029, T034 |
| FR-030 | T029, T031, T033 |

| SC | 확인하는 작업 |
|---|---|
| SC-001 | T010, T039 |
| SC-002 | T010, T011 |
| SC-003 | T021, T029 |
| SC-004 | T021 |
| SC-005 | T020, T022 |
| SC-006 | T021 |
| SC-007 | T029 (블로그·분류. 나머지는 `003`, `005`, `006`) |
| SC-008 | T029 |
| SC-009 | T018, T019, T039 |
| SC-010 | T029 |

---

## Dependencies & Execution Order

### Phase Dependencies

- **Setup (Phase 1)**: 확인만 한다
- **Foundational (Phase 2)**: Setup 다음. **모든 사용자 이야기를 막는다**
- **US1 (P1)**: Foundational 다음
- **US2 (P1)**: Foundational 다음. US1과 서로 기대지 않는다 (`AccountController`를 같이 고치므로 같은 사람이 하거나 US1을 먼저 merge한다). **T020(R-2 확인)이 가장 먼저**
- **US3 (P2)**: US2의 T020(`MemberSessions`)과 T024(`CurrentPasswordChecker`) 다음
- **US4 (P3)**: 만들지 않는다. T037은 언제든
- **Polish**: 원하는 이야기가 끝난 뒤

### Within Each User Story

- 테스트 → 도메인(`User`) → 서비스 → 주소(컨트롤러) → 화면 순서
- 테스트는 먼저 써 두고 실패하는 것을 본 뒤 구현한다 (CI로 확인. 이 작업 공간에서는 Maven 빌드가 막혀 있다)
- 한 이야기를 끝내고 Checkpoint를 통과한 뒤 PR을 merge하고 다음 이야기로 간다

### Parallel Opportunities

- T002, T003, T004는 서로 다른 파일이라 동시에 해도 된다. 화면의 T006, T007, T008도 동시에
- US1의 T010, T011, T012, T013은 동시에
- US2의 T021, T022, T023은 동시에 (T020은 혼자 먼저)
- US3의 T029, T030, T032는 동시에

---

## Parallel Example: User Story 2

```text
먼저 혼자:
Task: "T020 MemberSessions + R-2 확인 (FindByIndexNameSessionRepository)"
그다음 동시에:
Task: "T021 AccountPasswordTest (S-4, S-6)"
Task: "T022 AccountSessionFlowTest (S-5)"
Task: "T023 @PasswordConfirmed confirmField"
그다음: T024 → T025 → T026 → T027 → T028
```

---

## Implementation Strategy

### MVP First (User Story 1만)

1. Phase 1, 2를 끝낸다
2. Phase 3(US1)을 끝낸다
3. **멈추고 확인**: quickstart S-1 ~ S-3
4. 보여 줄 수 있으면 보여 준다

### Incremental Delivery

1. Foundational → 바탕 준비 (US1 PR에 같이 넣는다)
2. US1(내 정보) → 확인 → MVP
3. US2(비밀번호 변경, 다른 기기 끊기) → 확인
4. US3(탈퇴, 지금 있는 표까지) → 확인
5. `003`, `005`, `006`을 만들 때 T035에 적은 대로 이벤트를 듣는 쪽을 더하고 quickstart S-8 전체를 다시 확인한다

---

## Notes

- 작업 하나 또는 묶음 하나를 끝낼 때마다 커밋하고, 커밋 메시지에 작업 ID를 적는다 (예: `002 T020`). PR은 이야기 하나에 하나
- `가안`인 주소(`/api/account/…`, `/mypage…`), 오류 이름, 설정 이름, 이벤트 이름을 바꾸면 T038처럼 문서도 같이 고친다
- 남은 결정: 없음 (D-1 ~ D-6 결정됨). 확인할 위험: research R-1(세션 삭제와 비밀번호 저장의 묶음, T026), R-2(회원 기준 세션 찾기, T020), R-3(탈퇴 중 글쓰기, T031·T033)
