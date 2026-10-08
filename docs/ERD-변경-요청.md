# ERD: 팀 공통 요청과 내 확장

> 작성: JaeUng · 2026-10-07 (2026-10-08 다시 정리) · 상태: **팀 확인 전**
> 기준: 팀원들과 만든 ERD **최신판(2026-10-07에 받은 판)**
> 근거: `specs/001` ~ `specs/006`의 명세와 기술 계획의 결정(`research.md`의 `D-항목`), 팀 공통 범위는 `docs/요구사항-공통.md`

## 정리 원칙 (2026-10-08)

팀 공통 ERD에는 **네 명이 함께 쓰는 기본 기능**(`요구사항-공통.md`의 CF-01 ~ CF-22)에 꼭 필요한 것만 요청합니다. 제 개인 상세 규칙(로그인 잠금, 재가입, 중복 저장 방지, 블로그 관리·통계 등)에 필요한 칸과 인덱스는 **제 블로그를 구현할 때 제 저장소에서 더합니다.** 공통 표에 칸과 인덱스를 **더하기만** 하므로 공통 ERD와 부딪히지 않습니다.

## 0. 이미 반영된 것

이전 판에서 문제로 보았던 것 중 최신판에서 이미 고쳐진 것입니다.

| 표 | 바뀐 것 | 덕분에 지켜지는 요구사항 |
|---|---|---|
| `comment` | `UNIQUE(users_id, post_id)` 삭제 | 한 사람이 한 글에 댓글을 여러 개 달 수 있다 (CF-18) |
| `category` | `sort_order` 추가 | 분류 순서 바꾸기 (CF-08) |
| `post_report` | `UNIQUE(users_id, post_id)` 추가 | 같은 글을 두 번 신고할 수 없다 (CF-21) |
| `blog_daily_stat` | 기본값 0, `UNIQUE(blog_id, stat_date)` 위치 정리 | 하루에 한 줄 |

## 1. 팀에 요청할 것 (공통, 3건)

| # | 표 | 지금 | 제안 | 왜 공통인가 | 관련 |
|---|---|---|---|---|---|
| T-1 | `post_image` | `post_id` 필수 | **검토 요청**: `post_id` NULL 허용 + 올린 사람(`users_id`), 올린 시각(`created_at`) 추가 | 글쓰기 중에 이미지를 넣으려면(CF-22) 아직 없는 글에는 이미지 기록을 만들 수 없다. 누가 구현하든 같은 문제를 만난다. 글을 저장한 뒤에만 올리게 할 거라면 지금 그대로 둬도 된다 | CF-22, `005` D-3 |
| T-2 | `post` | `content` 기본값 `''` | 기본값 없애기 | 본문은 필수(CF-06)라 빈 값으로 저장되면 안 된다 | CF-06 |
| T-3 | `post`, `category`, `post_report`, `comment_report` | 값 목록이 주석에만 있음 | `CHECK` 추가: `visibility IN ('public','private')`, `reason IN ('SPAM','ABUSE','ADULT','OTHER')` | 오타 값(`Public` 등)이 들어가면 비공개 글이 새어 나간다. DB가 값을 막아 준다 | CF-13, CF-21 |

```sql
-- T-1 (검토)
ALTER TABLE post_image ALTER COLUMN post_id DROP NOT NULL;
ALTER TABLE post_image ADD COLUMN users_id BIGINT NOT NULL REFERENCES users (users_id);
ALTER TABLE post_image ADD COLUMN created_at TIMESTAMPTZ NOT NULL;

-- T-2
ALTER TABLE post ALTER COLUMN content DROP DEFAULT;

-- T-3
ALTER TABLE post           ADD CONSTRAINT ck_post_visibility     CHECK (visibility IN ('public', 'private'));
ALTER TABLE category       ADD CONSTRAINT ck_category_visibility CHECK (visibility IN ('public', 'private'));
ALTER TABLE post_report    ADD CONSTRAINT ck_post_report_reason    CHECK (reason IN ('SPAM', 'ABUSE', 'ADULT', 'OTHER'));
ALTER TABLE comment_report ADD CONSTRAINT ck_comment_report_reason CHECK (reason IN ('SPAM', 'ABUSE', 'ADULT', 'OTHER'));
```

**참고 (요청 아님)**: 지금 DDL은 MySQL 문법(`` ` ``, `COMMENT`)과 PostgreSQL 타입(`TIMESTAMPTZ`)이 섞여 있습니다. DB가 PostgreSQL(가안)이면 실제 DDL을 만들 때 PostgreSQL 문법으로 정리하면 됩니다.

## 2. 내 확장 (내 블로그를 구현할 때 내 저장소에서 추가)

팀 ERD는 바꾸지 않고, 제 저장소의 DB 변경 파일(마이그레이션)로 더합니다.

| # | 표 | 더하는 것 | 왜 | 관련 |
|---|---|---|---|---|
| E-1 | `users` | `failed_login_count`(INT, 기본 0), `locked_until`(TIMESTAMPTZ, NULL) | 로그인 5회 실패 10분 잠금. **Redis로 옮길지는 구현 때 다시 정한다** | `001` FR-027, D-3 |
| E-2 | `users` | 기존 `UNIQUE` 대신 "탈퇴하지 않은 회원 + 소문자 비교" 인덱스 | 탈퇴 후 재가입(CF-15-21), `Kim`과 `kim`은 같은 닉네임 | `001` D-4 |
| E-3 | `category` | 이름 소문자 비교 인덱스, `color_index`(SMALLINT) | "Java"와 "java"는 같은 분류, 분류 색 | `003` D-5, `006` D-9 |
| E-4 | `post` | `request_key`(VARCHAR(36), NULL, 중복 불가) | 저장을 여러 번 눌러도 한 번만 저장 | `003` D-6 |
| E-5 | **새 표** `post_daily_stat` | `post_id`, `stat_date`, `views` | 최근 7일 인기 글 | `006` D-6 |
| E-6 | `blog` | `comments_read_at`(TIMESTAMPTZ, NULL) | 새 댓글 N개 | `006` D-8 |

```sql
-- E-1
ALTER TABLE users ADD COLUMN failed_login_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN locked_until TIMESTAMPTZ NULL;

-- E-2 (email, nickname의 기존 UNIQUE는 지운다)
CREATE UNIQUE INDEX uq_users_email_active    ON users (lower(email))    WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_users_nickname_active ON users (lower(nickname)) WHERE deleted_at IS NULL;

-- E-3 (기존 UNIQUE(blog_id, name)은 지운다)
CREATE UNIQUE INDEX uq_category_blog_name ON category (blog_id, lower(name));
ALTER TABLE category ADD COLUMN color_index SMALLINT NOT NULL DEFAULT 0;

-- E-4
ALTER TABLE post ADD COLUMN request_key VARCHAR(36) NULL UNIQUE;

-- E-5
CREATE TABLE post_daily_stat (
  post_daily_stat_id BIGSERIAL PRIMARY KEY,
  post_id    BIGINT  NOT NULL REFERENCES post (post_id),
  stat_date  DATE    NOT NULL,
  views      INTEGER NOT NULL DEFAULT 0,
  UNIQUE (post_id, stat_date)
);

-- E-6
ALTER TABLE blog ADD COLUMN comments_read_at TIMESTAMPTZ NULL;
```

> E-2와 E-3은 기존 `UNIQUE`를 **더 좁은 규칙으로 바꾸는** 것이라, 공통 ERD의 `UNIQUE`를 그대로 두면 탈퇴 후 재가입이 막힙니다. 재가입은 제 규칙이므로 제 저장소에서만 바꿉니다.

## 3. 바꾸지 않는 것 (공유만)

| 표 | 내용 | 이유 |
|---|---|---|
| `users` | 탈퇴해도 줄을 지우지 않고 `deleted_at`만 넣는다. 그 사람의 댓글은 "탈퇴한 사용자"로 보인다 | 지금 ERD 그대로 (`002` D-1) |
| `blog` | `users_id` `UNIQUE`(회원 1명 = 블로그 1개) 그대로 | 이번 범위는 1인 1블로그 (`003` D-4) |
| `post` | `topic_id` 그대로. 주제는 글마다 고른다 | 2026-10-07 결정 (`003` D-3) |
| `category` | `visibility` 그대로. 비공개 분류의 글은 주인만 본다 | 2026-10-07 팀 답변 (`003` D-5) |
| `post` | `blog_id` 없이 **분류를 거쳐** 블로그의 글을 찾는다 | 지금은 충분하다 (`004` D-6) |
| `comment` | `parent_id`, `is_secret`은 두되 이번에는 쓰지 않는다 | `005` D-7 |
| `comment_report` | 표는 두되 이번에는 쓰지 않는다 | `005` D-8 |
| 외래 키 | `ON DELETE CASCADE` 없이, 글을 지울 때 서버가 한 묶음으로 자식부터 지운다 | `003` D-7, `005` D-11 |

## 4. 팀에 묻고 싶은 것

1. **T-1**: 글을 쓰는 중에 이미지를 올리게 할 건가요, 글을 저장한 뒤에만 올리게 할 건가요?

**답을 받은 것 (2026-10-07)**
- `category.visibility`는 분류를 비공개로 하기 위한 칸이다 → 그대로 쓴다.
- 주제는 글을 쓸 때 고른다 → `post.topic_id`를 그대로 쓴다.

## 5. 이 정리가 끝나면

- 팀이 T-1 ~ T-3에 답하면 `001`의 작업 T006(팀 ERD 반영 확인)이 끝난다. T007(DB 표 만들기)에서는 **팀 ERD + 내 확장(E-1 ~ E-6)** 으로 표를 만든다.
- Crowfoot: `팀 공통 ERD` 문서(팀 ERD + T-1 ~ T-3)와 `myblog-제안 (2026-10-07)` 문서(= 내 확장판, 팀 ERD + T + E 전부)
