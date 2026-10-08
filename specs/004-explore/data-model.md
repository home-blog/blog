# Data Model: 글 탐색 (글 목록과 검색)

**Feature**: [spec.md](spec.md) | **Plan**: [plan.md](plan.md) | **Date**: 2026-10-07

> 명세의 `Key Entities`(글, 분류, 블로그)를 **어디서 어떻게 읽을지** 정리합니다. 이 기능은 **읽기만** 하고 아무것도 새로 저장하지 않습니다. 표를 만들고 규칙을 정하는 것은 `003`(블로그·분류·글)의 일입니다.
> 모든 내용은 `가안`입니다. 팀 공통 ERD와 **맞춰야 하거나 ERD에 없는 부분**은 `⚠`로 표시했습니다. 팀 ERD는 합의 없이 고치지 않으므로, `⚠`는 모두 **팀에 요청하거나 물어볼 것**입니다.
> ERD 문법에는 MySQL(`` ` ``, `COMMENT`)과 PostgreSQL(`TIMESTAMPTZ`)이 섞여 있습니다. 실제 DDL은 구현할 때 PostgreSQL 기준으로 정리합니다. (`001` data-model과 같음)

> **2026-10-08 정리**: 팀 공통 ERD에는 기본 기능만 요청하기로 했다. 이 문서에서 ⚠로 표시한 것 중 **팀에 요청하는 것은 [`ERD-변경-요청.md`](../../docs/ERD-변경-요청.md)의 T-1 ~ T-3뿐**이고, 나머지 칸·인덱스·표 추가는 **내 확장(E-1 ~ E-6)** 으로 내 저장소에서 더한다. 본문의 "팀에 요청"은 이 기준으로 읽는다.

## 한눈에 보기

| 명세의 개념 | 저장 위치 | 이 기능이 하는 일 | 종류 |
|---|---|---|---|
| 글 | PostgreSQL `post` | 세기, 최신순으로 10개 읽기, 제목·본문에서 찾기 | 팀 ERD에 있음 (⚠ 블로그 칸 없음, `D-6`) |
| 분류(카테고리) | PostgreSQL `category` | 분류 이름 읽기, 글이 어느 블로그 것인지 잇기, **분류의 공개 여부로 거르기** | 팀 ERD에 있음 (공개 여부 칸을 쓴다, `D-7`) |
| 블로그 | PostgreSQL `blog` | 블로그 이름 읽기, 주인 판단 | 팀 ERD에 있음 |
| 로그인 상태 | PostgreSQL 세션 표 (`001`) | 있으면 회원 번호 읽기 | 팀 ERD 밖 |
| 검색 기록 | — | **남기지 않는다** (명세 밖, research R-6) | — |

## 1. 글 — PostgreSQL `post`

| 칸 | 팀 ERD의 지금 모양 | 이 기능이 쓰는 방법 | 관련 FR |
|---|---|---|---|
| `post_id` | BIGINT, 자동 증가, 기본키 | 작성 시각이 같을 때 **큰 번호가 위**(나중에 만든 글) | FR-001, FR-014 |
| `category_id` | BIGINT, NOT NULL, `category` 참조 | 분류 필터, 블로그로 잇기 | FR-007 |
| `topic_id` | BIGINT, NOT NULL | 쓰지 않는다 (주제는 글마다 고르지만(`003`), 주제별 화면은 이 명세 밖) | — |
| `title` | VARCHAR(100), NOT NULL | 목록·결과에 보여 주기, 검색 대상 | FR-005, FR-011, FR-015 |
| `content` | TEXT, NOT NULL | 앞 100자 미리보기, 검색 대상 | FR-005, FR-011, FR-015 |
| `visibility` | VARCHAR(10), NOT NULL, 기본 `public`. 값은 `public`, `private` (주석) | 방문자·검색에는 `public`만 (분류도 `public`이어야 한다, 2번) | FR-006, FR-011 |
| `views` | INTEGER | 쓰지 않는다 | — |
| `created_at` | TIMESTAMPTZ, NOT NULL | 정렬 기준, "작성일"로 보여 주기 | FR-001, FR-005, FR-014 |
| `updated_at` | TIMESTAMPTZ, NULL | 쓰지 않는다 (정렬은 작성 시각 기준) | — |
| `blog_id` | **없음 ⚠** | 지금은 `category`를 거쳐 잇는다 (`D-6`) | FR-006, FR-007 |

맞춰야 할 것:

- ⚠ **`visibility`의 값이 DB에서 정해져 있지 않다.** 지금은 주석으로만 `public, private`라고 적혀 있다. 오타(`Public`, `publik`)가 들어가면 "공개가 아닌 글"로 숨겨지거나, 반대로 조건을 `<> 'private'`로 쓰면 **비공개가 새어 나간다.** 그래서 이 기능은 **항상 `= 'public'`으로 거른다**(맞는 값만 통과). 그리고 `CHECK (visibility IN ('public', 'private'))`를 팀 ERD에 더해 달라고 요청한다. (SC-001)
- ⚠ 기본값이 `DEFAULT public`처럼 따옴표 없이 적혀 있다. PostgreSQL DDL로 옮길 때 `'public'`으로 고쳐야 한다. (구현 때 확인)
- `created_at`은 `003`이 저장할 때 자동으로 넣고 바뀌지 않는다(`003`, CF-05-7, CF-05-13). 그래서 정렬이 흔들리지 않는다.
- "작성일"을 **어느 시간대 기준 날짜**로 보여 줄지는 정해진 것이 없다. 한국 시간으로 보여 주는 안(가안)을 화면에서 처리한다. `003`의 글 상세와 같은 방식으로 맞춘다.

## 2. 글이 어느 블로그 것인지 — `post → category → blog`

```
post.category_id ──▶ category.category_id
                     category.blog_id ──▶ blog.blog_id
                                          blog.users_id  (블로그 주인 = 이 회원)
                                          blog.name      (검색 결과의 블로그 이름)
```

| 표 | 칸 | 쓰는 곳 | 관련 FR |
|---|---|---|---|
| `category` | `category_id`, `blog_id`, `name` | 분류 이름 보여 주기, 그 블로그의 분류인지 확인, 블로그로 잇기 | FR-005, FR-007, FR-015 |
| `category` | `visibility` | 방문자의 목록과 분류 고르기 목록, 모든 사람의 검색에는 **`public`인 분류만** 쓴다. 주인의 목록에서는 보지 않는다 (`D-7`, 2026-10-07) | FR-006, FR-011 |
| `blog` | `blog_id`, `users_id` | 블로그 있는지 확인, 주인 판단(`users_id == 로그인한 회원`) | FR-006, FR-008 |
| `blog` | `name` | 검색 결과에 보여 주기 | FR-015 |

- **글 목록 (한 블로그)**: `post`를 `category`와 이어서 `category.blog_id = :blogId` 조건으로 읽는다. 분류를 골랐으면 `post.category_id = :categoryId`를 더한다. 이때 그 분류가 `:blogId`의 것인지 **먼저** 확인한다(남의 분류 번호는 `D-2`). 방문자가 **비공개 분류**를 고르면 없는 분류와 똑같이 다룬다(`D-2`를 따른다).
- **검색 (여러 블로그)**: `post`, `category`, `blog`를 이어서 블로그 이름을 함께 읽는다. 10개만 읽으므로 이어 읽는 비용은 작다.
- **블로그가 없을 때**: 없는 `blogId`면 목록을 읽지 않고 "없는 블로그"로 답한다. (contracts 1)
- ⚠ **`D-6`**: `post`에 `blog_id`가 있으면 이 잇기가 필요 없고 색인도 단순해진다. 지금은 ERD를 바꾸지 않는 A안(분류를 거쳐 찾기)으로 쓴다.

## 3. 읽는 조건 정리 (가안)

> 아래는 **조건의 뜻**을 적은 것이고, 실제 쿼리 문장이 아닙니다. `:이름`은 모두 **값으로 따로 전달**합니다. (헌법 IV)

| 요청 | 조건 | 정렬 | 관련 FR |
|---|---|---|---|
| 글 목록 (방문자) | `category.blog_id = :blogId` AND `post.visibility = 'public'` AND `category.visibility = 'public'` [AND `post.category_id = :categoryId`] | `created_at DESC, post_id DESC` | FR-001, FR-006, FR-007 |
| 글 목록 (주인) | `category.blog_id = :blogId` [AND `post.category_id = :categoryId`] | 같음 | FR-006 |
| 분류 고르기 목록 (방문자) | `category.blog_id = :blogId` AND `category.visibility = 'public'` | `sort_order`, `category_id` | FR-006, FR-007 |
| 분류 고르기 목록 (주인) | `category.blog_id = :blogId` | 같음 | FR-006 |
| 검색 (누구나) | `post.visibility = 'public'` AND `category.visibility = 'public'` AND (단어마다 `title ILIKE :w ESCAPE '\'` OR `content ILIKE :w ESCAPE '\'`)를 모두 AND | 같음 | FR-011 ~ FR-014, FR-018 |

- `:w`는 `%` + (`\`, `%`, `_`를 일반 글자로 바꾼 단어) + `%`. (research R-1)
- 같은 조건으로 **개수 세기**와 **10개 읽기**를 한 번씩 한다. (research B-2, R-4)
- 미리보기는 `content` 전체 대신 앞부분만 넉넉히 잘라 읽는 방법을 구현 때 검토한다. (research B-3)

## 4. 인덱스 제안 (모두 `가안`, ⚠ 팀 ERD에 추가 요청)

> 인덱스는 **책의 찾아보기**입니다. 표의 모양은 바꾸지 않고 찾는 속도만 바꿉니다. 그래도 팀이 같이 쓰는 DB이므로 팀에 요청합니다. PostgreSQL은 참조 칸(외래 키)에 인덱스를 **자동으로 만들지 않습니다.**

| 인덱스 (가안) | 칸 | 왜 | 관련 |
|---|---|---|---|
| `idx_post_category_created` | `post (category_id, created_at DESC, post_id DESC)` | 분류를 고른 목록을 최신순으로 바로 읽는다. 블로그 전체 목록도 그 블로그의 분류들로 찾을 때 쓴다 | FR-001, FR-007, SC-009 |
| `idx_post_public_created` | `post (created_at DESC, post_id DESC) WHERE visibility = 'public'` | 검색과 방문자 목록이 공개 글만 최신순으로 훑는다 (부분 인덱스, PostgreSQL 기능). 분류의 공개 여부(`category.visibility = 'public'`)는 `category`와 이은 뒤 거른다 | FR-011, FR-014 |
| `idx_category_blog` | `category (blog_id)` | 블로그의 분류를 찾는다 (분류는 블로그마다 몇 개뿐이라 `visibility`는 인덱스에 넣지 않는다) | FR-006, FR-007 |
| (이미 있음) | `blog (users_id)` UNIQUE (주석) | 주인 판단. 회원당 블로그 1개(`003`, CF-03-1)와 함께 `003`에서 정리한다 | FR-006 |
| (D-5의 B를 고를 때만) | `post` 제목·본문에 trigram 색인(`pg_trgm`, GIN) | `%단어%` 검색을 색인으로 찾는다 | `D-5` |

- **검색의 한계**: `%단어%` 모양은 위의 보통 인덱스로 빨라지지 않는다. 위 인덱스는 **정렬과 거르기**를 도울 뿐이고, "검색 결과 N건"을 세려면 공개 글을 끝까지 훑는다. 그래서 글이 많아지면 `D-5`가 필요하다.
- **`D-6`의 B를 고르면** 첫 줄 대신 `post (blog_id, created_at DESC, post_id DESC)`를 쓴다.
- 인덱스가 실제로 쓰이는지는 구현 뒤에 쿼리 실행 계획(`EXPLAIN`)으로 확인한다. (구현 때 확인)

## 5. 이 기능이 읽거나 쓰는 표 요약

| 표 | 읽기 | 쓰기 | 언제 |
|---|---|---|---|
| `post` | 세기, 10개 읽기, 검색 | — | 글 목록, 검색 |
| `category` | 이름, 블로그로 잇기, 분류 확인, 공개 여부 | — | 글 목록, 검색 |
| `blog` | 있는지, 주인, 이름 | — | 글 목록, 검색 |
| 세션 표 (`001`) | 쿠키가 있으면 회원 번호 | (프레임워크가 마지막 사용 시각을 고칠 수 있음) | 모든 요청 |
| Redis | — | — | 쓰지 않는다 |

## 6. 팀에 요청하거나 물어볼 것 (⚠ 모음)

| # | 무엇을 | 이유 | 관련 |
|---|---|---|---|
| 1 | `post.visibility`에 `CHECK (visibility IN ('public', 'private'))` 추가 | 잘못된 값이 들어가 비공개가 새거나 공개가 숨는 일을 DB가 막는다 | FR-006, FR-011, SC-001 |
| 2 | 위 4번의 인덱스 3개 추가 | 목록 2초(NF-09) | SC-009 |
| 3 | `category.visibility`에 `CHECK (visibility IN ('public', 'private'))` 추가 (`003` data-model 9와 같은 요청) | 이 칸도 방문자에게 보일지를 정하므로 1번과 같은 이유. 이 기능은 항상 `= 'public'`으로 거른다 | `D-7`, FR-006, FR-011 |
| 4 | (재 보고 느릴 때만) `post.blog_id` 추가 | 블로그 전체 목록 속도 | `D-6` |
| 5 | (재 보고 느릴 때만) `pg_trgm` 확장과 색인 | 검색 속도 | `D-5` |
