-- 0.14.0 업데이트 공지 (specs/019): 운영팀 글(쓴 관리자 없음), 고정. 0.13.0 운영팀 글의 고정을 푼다
-- 관리자가 손으로 고친 글(users_id 있음)은 건드리지 않는다

UPDATE notice SET pinned = false WHERE kind = 'update' AND release_version = '0.13.0' AND users_id IS NULL;

INSERT INTO notice (kind, title, content, release_version, pinned, users_id, created_at) VALUES
('update', '댓글에 답글을 달고, 쪽지를 봉해 남겨요', $$
이제 댓글끼리 이야기를 나눌 수 있어요. 남에게 보이고 싶지 않은 말은 **봉한 쪽지**로 남길 수 있고요.

## 답글

- 댓글마다 **답글** 단추가 생겼어요. 답글은 원댓글 아래 붉은 교정선을 따라 붙어요.
- 답글에 다시 답하면 같은 원댓글 아래로 모여요(한 단계만).
- 내 댓글에 누가 답글을 달면 **우편함**에 "답글" 우표가 와요.

## 봉한 쪽지 (비밀 댓글)

- 댓글을 쓸 때 **봉해서 남기기**를 고르면 쓴 사람과 블로그 주인만 읽어요. 다른 사람에게는 봉인 도장만 보여요.
- 봉한 쪽지에 단 답글도 늘 봉해져요. 원댓글을 쓴 사람은 그 답글을 읽을 수 있어요.

## 지운 댓글

- 답글이 달린 댓글을 지우면, 이야기가 끊기지 않게 "지운 댓글입니다" 칸이 남아요. 답글이 모두 지워지면 그 칸도 사라져요.
$$, '0.14.0', true, NULL, now());

UPDATE notice SET content = btrim(content, E' \n') WHERE release_version = '0.14.0' AND users_id IS NULL;
