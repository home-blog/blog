-- 0.15.0 업데이트 공지 (specs/020): 운영팀 글(쓴 관리자 없음), 고정. 0.14.0 운영팀 글의 고정을 푼다
-- 관리자가 손으로 고친 글(users_id 있음)은 건드리지 않는다

UPDATE notice SET pinned = false WHERE kind = 'update' AND release_version = '0.14.0' AND users_id IS NULL;

INSERT INTO notice (kind, title, content, release_version, pinned, users_id, created_at) VALUES
('update', '내 AI 비서가 내 원고를 찾아 읽어요 (MCP)', $$
Claude 같은 AI 비서에게 "내 블로그에서 찾아 줘"라고 물을 수 있게 되었어요. 비서는 글결에서 맞는 **단락을 출처와 함께** 찾아 와서 답해요. 글을 올리는 일은 언제나 내가 합니다.

## AI 비서 연결

- **마이페이지 → AI 비서 연결**에서 열쇠를 만들어요. 이름, 할 수 있는 일(읽기만 / 읽기 + 초안 남기기), 기한(30·90·365일)을 고르면 돼요.
- 열쇠는 만들 때 **한 번만** 보여요. 글결에도 남지 않으니 꼭 적어 두세요. 잃어버리면 끊고 새로 만들면 돼요.
- 붙이는 법(Claude Code 한 줄, Claude Desktop 설정)은 같은 화면에 열쇠를 채워 보여 드려요.

## 비서가 할 수 있는 일

- 글 찾기, **단락 찾기**(출처 주소와 몇 번째 단락인지 함께), 글 읽기
- 내 글 목록, 내 블로그 통계(조회 수·방문자·새 댓글·인기 글)
- 초안 남기기: "읽기 + 초안" 열쇠만. 임시 저장 글로만 남고, 읽고 고쳐 올리는 건 내가 해요.
- 비공개 글은 내 열쇠로만, 남의 비공개 글은 찾지 못해요.

## 우편함을 다시 그렸어요

- 항공우편 띠와 편지봉투 덮개, 소식마다 톱니 우표가 붙은 엽서. 안 읽은 편지는 형광펜 제목, 읽으면 우표 위에 날짜 소인이 찍혀요.
- "전체 / 안 읽음"으로 나눠 볼 수 있어요.
$$, '0.15.0', true, NULL, now());

UPDATE notice SET content = btrim(content, E' \n') WHERE release_version = '0.15.0' AND users_id IS NULL;
