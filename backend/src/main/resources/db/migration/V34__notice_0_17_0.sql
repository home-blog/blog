-- 0.17.0 업데이트 공지: 운영팀 글(쓴 관리자 없음), 고정. 0.16.0 운영팀 글의 고정을 푼다
-- 관리자가 손으로 고친 글(users_id 있음)은 건드리지 않는다

UPDATE notice SET pinned = false WHERE kind = 'update' AND release_version = '0.16.0' AND users_id IS NULL;

INSERT INTO notice (kind, title, content, release_version, pinned, users_id, created_at) VALUES
('update', 'Claude 앱에 주소 하나로 글결을 붙여요', $$
이제 열쇠를 만들어 복사하지 않아도 Claude 앱에서 바로 글결을 쓸 수 있어요.

## Claude 앱 커넥터로 붙이기

- Claude 앱(웹·데스크톱·모바일)의 **설정 → 커넥터 → 커스텀 커넥터 추가**에 글결 MCP 주소를 붙여 넣으세요. 주소는 마이페이지 **AI 비서 연결**의 "Claude 앱" 탭에서 복사할 수 있어요.
- 글결 **위임장**이 열려요. 어떤 앱이 무엇을 언제까지 하는지 원고지 칸에 적혀 있고, 읽기만 할지 초안까지 남길지 고른 뒤 허락하면 붉은 인장이 찍혀요.
- 허락하면 Claude가 내 글과 단락을 출처와 함께 찾아 읽고, 허락했다면 초안도 남겨요.

## 연결된 앱

- 마이페이지 **AI 비서 연결**에서 연결된 앱과 마지막으로 쓴 때를 보고, 언제든 끊을 수 있어요.
- 비밀번호를 바꾸면 연결된 앱도 모두 끊겨요.
$$, '0.17.0', true, NULL, now());

UPDATE notice SET content = btrim(content, E' \n') WHERE release_version = '0.17.0' AND users_id IS NULL;
