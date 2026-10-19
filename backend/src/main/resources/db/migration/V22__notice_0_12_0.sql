-- 0.12.0 업데이트 공지 (specs/017): 운영팀 글(쓴 관리자 없음), 고정. 0.11.0 운영팀 글의 고정을 푼다
-- 관리자가 손으로 고친 글(users_id 있음)은 건드리지 않는다

UPDATE notice SET pinned = false WHERE kind = 'update' AND release_version = '0.11.0' AND users_id IS NULL;

INSERT INTO notice (kind, title, content, release_version, pinned, users_id, created_at) VALUES
('update', '글마당이 열렸어요: 묻고, 이야기하고, 채택 도장을 찍어요', $$
블로그 밖에서도 함께 이야기할 곳이 생겼어요. 머리글의 **글마당**으로 들어오세요.

## 글마당

- 여행·음식·취미·운동·개발 **주제 탭**으로 나눠 보고, **이야기**와 **물음** 가운데 골라 볼 수 있어요.
- 로그인하지 않아도 읽을 수 있고, 글쓰기와 댓글은 로그인한 뒤에 할 수 있어요.
- 표에는 주제, 제목과 댓글 수, 쓴 사람, 날짜, 조회 수가 보여요.

## 물음과 채택 도장

- 궁금한 것은 **물음**으로 올려 보세요. 가장 도움이 된 댓글에 붉은 **채택** 도장을 찍을 수 있어요.
- 채택한 댓글은 맨 위로 올라오고, 목록의 물음에는 **해결** 도장이 붙어요. **해결 전**만 골라 볼 수도 있어요.
- 내 댓글이 채택되면 **우편함**으로 알려 드려요. 내 글마당 글에 댓글이 달려도 알려 드려요.

## 첫 화면과 검색

- 첫 화면 아래에 글마당 **최근 글**이 보여요.
- 검색하면 블로그 글 아래에 **글마당 결과**도 따로 보여요.
$$, '0.12.0', true, NULL, now());

UPDATE notice SET content = btrim(content, E' \n') WHERE release_version = '0.12.0' AND users_id IS NULL;
