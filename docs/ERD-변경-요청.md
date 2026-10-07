# 팀 ERD 변경 요청

> 작성: JaeUng · 2026-10-07 · 상태: **팀 확인 전**
> 기준: 팀원들과 만든 ERD **최신판(2026-10-07에 받은 판)**
> 근거: `specs/001` ~ `specs/006`의 명세(요구사항)와 기술 계획의 결정(`research.md`의 `D-항목`)

## 한 줄 요약

요구사항을 지키려면 **표 6곳을 바꾸고, 표 1개를 새로 만들어야** 합니다. 그 밖에 확인을 부탁드리는 질문이 1개 있습니다. 이미 고쳐 주신 것 4가지는 요청에서 뺐습니다. (2026-10-07 수정: 주제는 글마다, 분류 공개 여부는 그대로 쓰기로 해서 R-3과 R-4의 ②를 철회했습니다.)

## 0. 이미 반영된 것 (요청에서 뺌)

이전 판에서 문제로 보았던 것 중, 최신판에서 이미 고쳐진 것입니다.

| 표 | 바뀐 것 | 덕분에 지켜지는 요구사항 |
|---|---|---|
| `comment` | `UNIQUE(users_id, post_id)` 삭제 | 한 사람이 한 글에 댓글을 여러 개 달 수 있다 (`005` FR-002) |
| `category` | `sort_order` 추가 | 분류 순서 바꾸기 (`003` FR-039, `006`) |
| `post_report` | `UNIQUE(users_id, post_id)` 추가 | 같은 글을 두 번 신고할 수 없다 (`005`) |
| `blog_daily_stat` | `views`·`visitors` 기본값 0, `UNIQUE(blog_id, stat_date)` 위치 정리 | 하루에 한 줄 (`006`) |

## 1. 요청 목록

| # | 표 | 지금 | 바꿀 모양 | 왜 필요한가 | 관련 |
|---|---|---|---|---|---|
| R-1 | `users` | 로그인 실패를 기록할 칸이 없음 | `failed_login_count`(INT, 기본 0), `locked_until`(TIMESTAMPTZ, NULL) 추가 | 5회 실패하면 10분 잠금 | `001` FR-027, D-3 |
| R-2 | `users` | `email`, `nickname`이 그냥 `UNIQUE` | **탈퇴하지 않은 회원에게만**, **소문자로 비교**하는 중복 불가로 바꿈 | 탈퇴한 사람이 같은 이메일로 다시 가입할 수 있어야 함. `Kim`과 `kim`은 같은 닉네임 | `001` FR-003·005, D-4 / `002` CF-15-21 |
| ~~R-3~~ | `post` | `post.topic_id` 필수 | **철회 (2026-10-07)** — 주제는 글마다 고르기로 해서 ERD 그대로 쓴다 | 글쓰기에 주제 입력을 더했다 | `003` FR-046, D-3 |
| R-4 | `category` | `UNIQUE(blog_id, name)`, 색 칸 없음 | ① 이름 중복을 **소문자로 비교** ② ~~`visibility` 삭제~~ **철회 — 분류 비공개에 쓴다** ③ `color_index`(SMALLINT) 추가 | ① "Java"와 "java"는 같은 분류 ③ 분류마다 정해진 색 | `003` FR-036·047, D-5 / `006` FR-020·042, D-9 |
| R-5 | `post` | 같은 요청을 알아볼 칸이 없음 | `request_key`(VARCHAR(36), NULL, 중복 불가) 추가 | 저장 버튼을 여러 번 눌러도 글이 **한 번만** 저장되게 | `003` FR-018, SC-008, D-6 |
| R-6 | `post_image` | `post_id` 필수, 올린 사람·시각 없음 | `post_id` NULL 허용, `users_id`(FK), `created_at` 추가 | 글을 **저장하기 전에** 이미지를 올려 본문에서 바로 보이게. 저장하지 않고 나간 이미지를 지우기 위해 | `005` FR-024·026, D-3 |
| R-7 | **새 표** `post_daily_stat` | 글별·날짜별 조회수가 없음 | `post_id`, `stat_date`, `views` (글·날짜마다 한 줄) | 대시보드의 "**최근 7일** 인기 글" 계산 | `006` FR-008, D-6 |
| R-8 | `blog` | 댓글함을 마지막으로 본 시각이 없음 | `comments_read_at`(TIMESTAMPTZ, NULL) 추가 | 관리 메뉴의 "새 댓글 N개" | `006` D-8 |

**작은 정리 (선택)**

| # | 표 | 지금 | 제안 | 이유 |
|---|---|---|---|---|
| S-1 | `post` | `content` 기본값 `''` | 기본값 없애기 | 본문은 필수(1~10,000자)라 빈 값으로 저장되면 안 됨 (`003` FR-011) |
| S-2 | `blog` | `intro` VARCHAR(500) | 그대로 둬도 됨 | 요구사항은 0~200자. 서버가 200자로 검사하므로 칸이 커도 문제없음 |
| S-3 | 전체 | MySQL 문법(`` ` ``, `COMMENT`)과 PostgreSQL 타입(`TIMESTAMPTZ`)이 섞임 | 실제 DDL은 PostgreSQL 기준으로 정리 | DB는 PostgreSQL(가안) |

## 2. 바꿀 모양 (PostgreSQL 예시)

> 팀이 받아들이면 이 모양으로 DDL을 고칩니다. 이름은 바꿔도 됩니다.

```sql
-- R-1
ALTER TABLE users ADD COLUMN failed_login_count INTEGER NOT NULL DEFAULT 0;
ALTER TABLE users ADD COLUMN locked_until TIMESTAMPTZ NULL;

-- R-2 (email, nickname의 기존 UNIQUE는 지운다)
CREATE UNIQUE INDEX uq_users_email_active    ON users (lower(email))    WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX uq_users_nickname_active ON users (lower(nickname)) WHERE deleted_at IS NULL;

-- R-4 (기존 UNIQUE(blog_id, name)은 지운다)
CREATE UNIQUE INDEX uq_category_blog_name ON category (blog_id, lower(name));
ALTER TABLE category ADD CONSTRAINT ck_category_visibility CHECK (visibility IN ('public', 'private'));
ALTER TABLE category ADD COLUMN color_index SMALLINT NOT NULL DEFAULT 0;

-- R-5
ALTER TABLE post ADD COLUMN request_key VARCHAR(36) NULL UNIQUE;

-- R-6
ALTER TABLE post_image ALTER COLUMN post_id DROP NOT NULL;
ALTER TABLE post_image ADD COLUMN users_id BIGINT NOT NULL REFERENCES users (users_id);
ALTER TABLE post_image ADD COLUMN created_at TIMESTAMPTZ NOT NULL;

-- R-7
CREATE TABLE post_daily_stat (
  post_daily_stat_id BIGSERIAL PRIMARY KEY,
  post_id    BIGINT  NOT NULL REFERENCES post (post_id),
  stat_date  DATE    NOT NULL,
  views      INTEGER NOT NULL DEFAULT 0,
  UNIQUE (post_id, stat_date)
);

-- R-8
ALTER TABLE blog ADD COLUMN comments_read_at TIMESTAMPTZ NULL;
```

## 3. 바꾸지 않는 것 (공유만)

| 표 | 내용 | 이유 |
|---|---|---|
| `users` | 탈퇴해도 줄을 지우지 않고 `deleted_at`만 넣는다. 그 사람의 댓글은 "탈퇴한 사용자"로 보인다 | 지금 ERD 그대로 쓸 수 있다 (`002` D-1) |
| `blog` | `users_id` `UNIQUE`(회원 1명 = 블로그 1개) 그대로 | 이번 범위는 1인 1블로그 (`003` D-4) |
| `post` | `topic_id`는 그대로 둔다. 주제는 글마다 고른다 | 2026-10-07 결정 (`003` D-3) |
| `post` | `blog_id` 칸 없이 **분류를 거쳐** 블로그의 글을 찾는다 | 지금은 충분히 빠르다. 느려지면 그때 요청 (`004` D-6) |
| `comment` | `parent_id`(대댓글), `is_secret`(비밀 댓글)은 두되 이번에는 쓰지 않는다 | 다음에 쓸 수 있게 남김 (`005` D-7) |
| `comment_report` | 표는 두되 이번에는 쓰지 않는다 (댓글 신고는 추후) | `005` D-8 |
| 외래 키 | `ON DELETE CASCADE`를 걸지 않고, 글을 지울 때 **서버가 한 묶음으로** 댓글·좋아요·태그 연결·이미지·신고·일별 조회수를 먼저 지운다 | 무엇이 함께 지워지는지 코드에서 분명히 보이게 (`003` D-7, `005` D-11) |

## 4. 팀에 묻고 싶은 것

1. **인기 글 기준**: 대시보드의 인기 글은 "최근 7일 조회수"로 순서를 매기는 것으로 읽었습니다(R-7). 누적 조회수를 생각하셨다면 R-7이 필요 없습니다.

**답을 받은 것 (2026-10-07)**
- `category.visibility`는 **분류를 비공개로 하기 위한 칸**입니다. → 그대로 쓰고, 비공개 분류의 글은 주인만 봅니다. 값이 두 개만 들어가게 `CHECK`만 더해 주세요.
- **주제는 글을 쓸 때 고릅니다.** → `post.topic_id`를 그대로 씁니다. (R-3 철회)

## 5. 이 요청이 받아들여지면

- `specs/`의 `data-model.md`에서 `⚠` 표시를 지우고 실제 DDL로 바꿉니다.
- `001`의 작업 T006(팀 ERD 반영 확인)이 끝나고, DB 표 만들기(T007)를 시작합니다.
- 기능별 API 초안을 합쳐 통합 API 명세서를 만듭니다.
