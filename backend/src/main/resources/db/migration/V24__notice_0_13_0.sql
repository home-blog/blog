-- 0.13.0 업데이트 공지 (specs/018): 운영팀 글(쓴 관리자 없음), 고정. 0.12.0 운영팀 글의 고정을 푼다
-- 관리자가 손으로 고친 글(users_id 있음)은 건드리지 않는다

UPDATE notice SET pinned = false WHERE kind = 'update' AND release_version = '0.12.0' AND users_id IS NULL;

INSERT INTO notice (kind, title, content, release_version, pinned, users_id, created_at) VALUES
('update', '신고한 글은 편집실이 살펴봅니다', $$
그동안 신고는 접수만 되고 처리할 곳이 없었어요. 이제 운영자가 일하는 **편집실**이 생겨, 들어온 신고를 하나하나 살펴봅니다.

## 신고하면 어떻게 되나요

- 신고는 편집실 **검토 대기**에 쌓여요. 신고한 사람이 누구인지는 운영자에게도 보이지 않아요.
- 편집실은 글을 읽고 **내림** 또는 **반려** 도장을 찍어요.
- **내림**은 글을 지우지 않고 **비공개로 돌려 잠그는** 것이에요. 글쓴이는 글을 계속 볼 수 있고 비공개로 고칠 수 있지만, 다시 공개로 바꿀 수는 없어요.
- 내림이 되면 글쓴이의 **우편함**에 "편집실" 우표와 함께 사유가 도착해요.

## 글마당 신고

- 글마당 글에도 **신고** 단추가 생겼어요. 편집실이 살펴보고 필요하면 글을 지워요.

모두가 편하게 쓰고 읽을 수 있는 글결이 되도록 함께 지켜 주세요.
$$, '0.13.0', true, NULL, now());

UPDATE notice SET content = btrim(content, E' \n') WHERE release_version = '0.13.0' AND users_id IS NULL;
