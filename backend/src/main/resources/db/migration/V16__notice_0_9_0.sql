-- 0.9.0 업데이트 공지 (specs/014 D-16, D-17): 운영팀 글(쓴 관리자 없음), 고정. 0.8.0 운영팀 글의 고정을 푼다
-- 관리자가 손으로 고친 글(users_id 있음)은 건드리지 않는다

UPDATE notice SET pinned = false WHERE kind = 'update' AND release_version = '0.8.0' AND users_id IS NULL;

INSERT INTO notice (kind, title, content, release_version, pinned, users_id, created_at) VALUES
('update', '빨간 펜을 누구나 쓸 수 있어요', $$
0.8.0에서 소개한 빨간 펜과 비슷한 글은 돈이 드는 바깥 AI가 있어야 해서 그동안 꺼 두었어요. 이번에는 **돈이 들지 않는 방법**으로 빨간 펜을 켰습니다.

## 빨간 펜: 맞춤법 규칙 교정

- 글쓰기 화면 아래의 **빨간 펜**을 누르면, 자주 틀리는 **맞춤법·띄어쓰기·외래어 표기**를 원고지 교정부호로 짚어 줘요.
  - 예: 됬다 → 됐다, 몇일 → 며칠, 할수있다 → 할 수 있다, 메세지 → 메시지
- 글은 **글결 서버 안에서만** 읽고, 바깥 AI로 보내지 않아요. 그래서 동의를 묻지 않고, 횟수 제한도 없어요.
- 틀린 것이 분명한 말만 고쳐요. 뜻에 따라 맞기도 한 말은 건드리지 않아서, 규칙에 없는 곳은 놓칠 수 있어요.
- 고칠 곳마다 받아들이기·넘기기를 고르고, 다 정하면 **"퇴고"** 도장이 찍히는 것은 그대로예요.

## 이 글과 비슷한 글

- 지금은 **태그와 주제가 겹치는 글**을 보여 드려요.
- 뜻으로 고르는 방법(글결 서버에서 도는 무료 AI)도 준비해 두었고, 서버 여유를 확인한 뒤 켤 예정이에요.
$$, '0.9.0', true, NULL, now());

UPDATE notice SET content = btrim(content, E' \n') WHERE release_version = '0.9.0' AND users_id IS NULL;
