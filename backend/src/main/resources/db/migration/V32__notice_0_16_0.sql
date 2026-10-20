-- 0.16.0 업데이트 공지: 운영팀 글(쓴 관리자 없음), 고정. 0.15.1 운영팀 글의 고정을 푼다
-- 관리자가 손으로 고친 글(users_id 있음)은 건드리지 않는다

UPDATE notice SET pinned = false WHERE kind = 'update' AND release_version = '0.15.1' AND users_id IS NULL;

INSERT INTO notice (kind, title, content, release_version, pinned, users_id, created_at) VALUES
('update', '블로그마다 엽서함이 생겼어요, 그리고 더 안전한 글결', $$
들른 블로그에 엽서를 남길 수 있어요. 글결을 함께 지키는 방법도 더했어요.

## 방명록, 엽서함

- 블로그 머리의 **방명록** 탭에 엽서가 꽂혀 있어요. 들른 블로그에 몇 줄 적고 우표 다섯 가지(소나무·파도·달·매화·학) 중 하나를 붙여 보내 보세요.
- **비밀 엽서**는 봉투에 넣어 밀랍으로 봉해요. 쓴 사람과 블로그 주인만 읽어요.
- 블로그 주인은 엽서를 뒤집어 **답장**을 써요. 엽서가 오거나 답장이 오면 우편함에 소식이 와요.

## 댓글 신고

- 댓글과 답글에도 **신고** 단추가 생겼어요.
- 서로 다른 회원의 신고가 3건 모이면 편집실이 보기 전에 저절로 가려요. 가린 댓글은 먹줄 그은 원고지 칸으로 보여요.

## 편집실

- 신고된 댓글을 가리거나 반려하고, 필요하면 되돌려요.
- 규칙을 어긴 회원은 기간과 사유를 정해 **이용을 정지**할 수 있어요. 정지되면 로그인과 AI 비서 연결이 모두 끊기고, 기간이 지나면 저절로 풀려요.
$$, '0.16.0', true, NULL, now());

UPDATE notice SET content = btrim(content, E' \n') WHERE release_version = '0.16.0' AND users_id IS NULL;
