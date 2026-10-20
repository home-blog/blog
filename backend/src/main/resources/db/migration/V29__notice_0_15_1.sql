-- 0.15.1 업데이트 공지: 운영팀 글(쓴 관리자 없음), 고정. 0.15.0 운영팀 글의 고정을 푼다
-- 관리자가 손으로 고친 글(users_id 있음)은 건드리지 않는다

UPDATE notice SET pinned = false WHERE kind = 'update' AND release_version = '0.15.0' AND users_id IS NULL;

INSERT INTO notice (kind, title, content, release_version, pinned, users_id, created_at) VALUES
('update', '누를 때마다 먹 한 방울, 글마당에 이야기가 생겼어요', $$
단추와 링크를 누를 때마다 작은 재미가 생겼어요.

## 누를 때마다 먹 한 방울

- 단추·링크·탭을 누르면 그 자리에 먹 한 방울이 번져요. 단추는 꾹 눌리고요.
- 머리글의 **글마당·공지**는 원고지 칸 머리글자로 바뀌었어요. 마우스를 올리면 형광펜이 칠해지고, 지금 보는 곳에는 붉은 펜 밑줄이 그어져요.

## 우편함

- 우편함이 넓어졌어요. 안내 글이 옆으로 밀려 나가던 문제도 고쳤어요.
- 빈 우편함의 봉투에 마우스를 올려 보세요. 덮개가 열리고 원고지 쪽지가 올라와요.

## 글마당

- 이웃 블로거들의 물음과 이야기가 올라왔어요. 비 오는 부산 여행지, 퇴근 20분 저녁 메뉴, 첫 책장 자랑까지 들러서 한마디 남겨 주세요.
- 주제마다 색이 다른 칩, 차례로 떠오르는 줄, 화면에 들어올 때 쿵 찍히는 "해결" 도장을 더했어요.
$$, '0.15.1', true, NULL, now());

UPDATE notice SET content = btrim(content, E' \n') WHERE release_version = '0.15.1' AND users_id IS NULL;
