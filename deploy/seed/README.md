# 예시 데이터 (첫 화면을 채우는 글)

첫 화면이 "몇 년째 운영 중인 블로그 서비스"처럼 보이도록 예시 블로거 12명의 블로그·글 108개·댓글·좋아요·태그·조회수를 넣는다.

- 예시 회원 이메일은 모두 `demo.<이름>@example.com` 이고, **로그인할 수 없는 계정**이다 (비밀번호 칸에 해시가 아닌 값).
- 글 작성 시각은 2023년부터 2026-10-10까지 흩어져 있고, **최근 7일 조회수(이번 주 인기 글)와 블로그 통계(최근 30일)는 넣는 날 기준**으로 만든다.
- **한 번만 넣을 수 있다.** 이미 있으면 아무것도 바꾸지 않고 멈춘다. 다시 넣으려면 먼저 지운다.
- 글 26개에는 **사진**이 한 장씩 들어간다(글 카드 썸네일). 사진은 [Lorem Picsum](https://picsum.photos)(Unsplash 사진, 무료 사용)의 주소를 그대로 건다. 사진은 따로 `demo-photos.sql`로 넣어서 이미 예시 글이 있는 DB에도 덧붙일 수 있다(몇 번 해도 같다).

| 파일 | 내용 |
|---|---|
| `data/*.json` | 블로거 한 명씩: 닉네임, 블로그, 분류, 글(마크다운), 댓글 |
| `photos.json` | 사진을 넣을 글과 Lorem Picsum 사진 번호, 설명 |
| `build_seed.py` | JSON → `demo-content.sql`, `demo-photos.sql`, `demo-board.sql` (같은 JSON이면 늘 같은 SQL) |
| `demo-content.sql` | 넣기 (자동 생성, 손으로 고치지 않는다) |
| `demo-photos.sql` | 예시 글에 사진 넣기 (자동 생성. 글이 처음 모양 그대로일 때만 바꾼다) |
| `board.json` | 글마당 예시 글 10개(물음·이야기, 댓글, 채택). 시각은 넣는 순간 기준 몇 시간 전 |
| `demo-board.sql` | 글마당 예시 글 넣기 (자동 생성. 이미 있는 글·댓글은 넣지 않아 몇 번 해도 같다) |
| `demo-remove.sql` | 지우기 (예시 회원과 그 글·댓글·좋아요·통계, 예시 글에 달린 진짜 댓글·좋아요도) |
| `run.sh` | 실습 서버에서 넣기·지우기 |

## 실습 서버에 넣기

**GitHub에서**: Actions → `예시 데이터` → Run workflow → `add`(넣기, 사진·글마당까지), `photos`(이미 넣은 글에 사진만), `board`(이미 넣은 예시 회원으로 글마당 예시 글만) 또는 `remove`(지우기). 배포가 한 번은 끝나 있어야 한다.

**서버에서 직접**: 배포(GitHub Actions)가 끝나면 이 폴더가 서버의 `~/geulgyeol/deploy/seed/`에 올라가 있다. SSH로 들어가서:

```bash
bash ~/geulgyeol/deploy/seed/run.sh add      # 넣기 (사진까지)
bash ~/geulgyeol/deploy/seed/run.sh photos   # 이미 넣은 글에 사진만
bash ~/geulgyeol/deploy/seed/run.sh board    # 글마당 예시 글만 (몇 번 해도 같다)
bash ~/geulgyeol/deploy/seed/run.sh remove   # 지우기
```

## 내 컴퓨터(개발 DB)에 넣기

**자동**: 개발 서버(dev 설정, IntelliJ "2. 서버")를 켤 때 예시 글이 없으면 저절로 넣고, 사진·글마당 예시 글은 켤 때마다 맞춘다(`DevDemoSeed`). 끄려면 `.env`에 `DEMO_SEED=false`.

손으로 넣거나 지우려면:

```bash
docker compose up -d --wait
docker compose exec -T postgres psql -q -U myblog -d myblog < deploy/seed/demo-content.sql
docker compose exec -T postgres psql -q -U myblog -d myblog < deploy/seed/demo-photos.sql
docker compose exec -T postgres psql -q -U myblog -d myblog < deploy/seed/demo-board.sql
docker compose exec -T postgres psql -q -U myblog -d myblog < deploy/seed/demo-remove.sql   # 지우기
```

## 글을 고치거나 더하려면

`data/*.json`·`photos.json`·`board.json`을 고친 뒤 `python3 deploy/seed/build_seed.py`로 SQL을 다시 만든다. 이미 넣었다면 지우고 다시 넣는다. 글마당 글은 (쓴 사람, 제목)으로 같은 글을 찾으므로, `board.json`의 제목만 바꿔 `board`를 다시 돌리면 옛 글이 남은 채 새 글이 더해진다.
