#!/usr/bin/env python3
"""예시 데이터 SQL 만들기: deploy/seed/data/*.json (블로거 한 명씩) → deploy/seed/demo-content.sql

첫 화면이 "몇 년째 운영 중인 블로그 서비스"처럼 보이도록 예시 회원·블로그·분류·글·태그·댓글·좋아요·조회수를 넣는다.
- 예시 회원 이메일은 모두 demo.<key>@example.com 이다 (example.com은 예시용으로 정해진 도메인).
- 예시 회원은 로그인할 수 없다: 비밀번호 칸에 BCrypt 해시가 아닌 값을 넣는다.
- 날짜가 정해진 것(가입일, 글 작성 시각)은 JSON 그대로, 최근 조회수(인기 글)는 SQL을 실행하는 날 기준으로 만든다.
- 같은 JSON이면 늘 같은 SQL이 나온다 (난수 씨앗 고정).

사용: python3 deploy/seed/build_seed.py
"""
import hashlib
import json
import random
from datetime import datetime, timedelta, timezone
from pathlib import Path

HERE = Path(__file__).resolve().parent
DATA = HERE / "data"
OUT = HERE / "demo-content.sql"
KST = timezone(timedelta(hours=9))
# 데이터를 만든 기준 시각. 글·댓글 시각이 이보다 늦지 않게 한다
NOW = datetime(2026, 10, 11, 23, 0, tzinfo=KST)
TOPICS = {"여행", "음식", "취미", "운동", "개발"}
CATEGORY_COLORS = 6  # application.yml category.color-count
NOT_A_HASH = "!demo-account-no-login"


def q(value):
    """SQL 글자 값. 작은따옴표는 두 번 쓴다 (standard_conforming_strings: 역슬래시는 글자 그대로)."""
    if value is None:
        return "null"
    return "'" + str(value).replace("'", "''") + "'"


def ts(dt):
    return q(dt.isoformat())


def rng_for(*parts):
    seed = int(hashlib.sha256("|".join(map(str, parts)).encode()).hexdigest()[:12], 16)
    return random.Random(seed)


def parse_dt(text):
    dt = datetime.fromisoformat(text)
    return dt if dt.tzinfo else dt.replace(tzinfo=KST)


def check(bloggers):
    keys = {b["key"] for b in bloggers}
    nicknames = set()
    for b in bloggers:
        nick = b["nickname"]
        assert 2 <= len(nick) <= 10 and nick.isalnum(), f"닉네임 규칙: {nick}"
        assert nick.lower() not in nicknames, f"닉네임 중복: {nick}"
        nicknames.add(nick.lower())
        assert len(b["blogName"]) <= 30 and len(b.get("blogIntro", "")) <= 200, b["key"]
        assert len(b.get("userIntro", "")) <= 100, b["key"]
        cats = b["categories"]
        assert len({c.lower() for c in cats}) == len(cats) and "미분류" not in cats, b["key"]
        assert all(1 <= len(c) <= 20 for c in cats), b["key"]
        for p in b["posts"]:
            assert p["topic"] in TOPICS, (b["key"], p["title"])
            assert p["category"] in cats, (b["key"], p["title"], p["category"])
            assert 1 <= len(p["title"]) <= 100, p["title"]
            assert 1 <= len(p["content"]) <= 10000, p["title"]
            assert len(p.get("tags", [])) <= 5, p["title"]
            assert all(1 <= len(t) <= 15 for t in p.get("tags", [])), p["title"]
            assert parse_dt(p["createdAt"]) <= NOW, p["title"]
            for c in p.get("comments", []):
                assert c["by"] != b["key"], (p["title"], c["by"])
                assert 1 <= len(c["body"]) <= 500, p["title"]
    missing = {c["by"] for b in bloggers for p in b["posts"] for c in p.get("comments", [])} - keys
    if missing:
        print(f"경고: 아직 없는 블로거의 댓글은 뺍니다: {sorted(missing)}")


def post_ref(email, title):
    return ("(select p.post_id from post p join category c on c.category_id = p.category_id"
            " join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id"
            f" where u.email = {q(email)} and p.title = {q(title)})")


def user_ref(email):
    return f"(select users_id from users where email = {q(email)})"


def main():
    bloggers = [json.loads(f.read_text(encoding="utf-8")) for f in sorted(DATA.glob("*.json"))]
    check(bloggers)
    emails = {b["key"]: f"demo.{b['key']}@example.com" for b in bloggers}
    for b in bloggers:
        for p in b["posts"]:
            p["comments"] = [c for c in p.get("comments", []) if c["by"] in emails]
    lines = []
    out = lines.append

    out("-- 예시 데이터 (자동 생성: deploy/seed/build_seed.py, 원본 deploy/seed/data/*.json). 손으로 고치지 않는다")
    out("-- 실행: deploy/seed/README.md. 한 번만 넣을 수 있다 (이미 있으면 아무것도 바꾸지 않고 멈춘다)")
    out("\\set ON_ERROR_STOP on")
    out("\\if :{?schema}")
    out("\\else")
    out("\\set schema public")
    out("\\endif")
    out("set search_path = :\"schema\";")
    out("begin;")
    out("do $$ begin")
    out("  if exists (select 1 from users where email like 'demo.%@example.com') then")
    out("    raise exception '예시 데이터가 이미 있습니다. 다시 넣으려면 먼저 demo-remove.sql을 실행하세요';")
    out("  end if;")
    out("end $$;")
    out("")

    all_posts = []
    comments = []  # 댓글 단 사람이 먼저 있어야 하므로 회원·글을 모두 넣은 뒤에 넣는다

    def joined_at(b):
        return parse_dt(b["joinedAt"] + ("T10:00:00+09:00" if "T" not in b["joinedAt"] else ""))

    out("-- 예시 회원 (로그인할 수 없는 계정)")
    for b in bloggers:
        out("insert into users (email, password, nickname, intro, created_at) values "
            f"({q(emails[b['key']])}, {q(NOT_A_HASH)}, {q(b['nickname'])}, {q(b.get('userIntro'))}, {ts(joined_at(b))});")
    out("")

    for b in bloggers:
        email = emails[b["key"]]
        joined = joined_at(b)
        out(f"-- {b['nickname']} · {b['blogName']}")
        out(f"insert into blog (users_id, name, intro, comments_read_at) values "
            f"({user_ref(email)}, {q(b['blogName'])}, {q(b.get('blogIntro'))}, {ts(NOW)});")
        blog_ref = f"(select blog_id from blog where users_id = {user_ref(email)})"
        out(f"insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values "
            f"({blog_ref}, '미분류', 'public', true, 1, 0, {ts(joined)});")
        for i, name in enumerate(b["categories"], start=1):
            out(f"insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values "
                f"({blog_ref}, {q(name)}, 'public', false, {i + 1}, {i % CATEGORY_COLORS}, {ts(joined)});")
        for p in sorted(b["posts"], key=lambda x: x["createdAt"]):
            created = parse_dt(p["createdAt"])
            cat_ref = f"(select category_id from category where blog_id = {blog_ref} and name = {q(p['category'])})"
            topic_ref = f"(select topic_id from topic where topic_name = {q(p['topic'])})"
            r = rng_for(b["key"], p["title"])
            age_days = max(1, (NOW - created).days)
            # 오래된 글일수록, 댓글이 많을수록 조회수가 많다 (대략)
            views = int(r.randint(40, 160) * (1 + min(age_days, 900) / 120) + 35 * len(p.get("comments", [])))
            out(f"insert into post (category_id, topic_id, title, content, visibility, views, created_at) values "
                f"({cat_ref}, {topic_ref}, {q(p['title'])}, {q(p['content'])}, 'public', {views}, {ts(created)});")
            ref = post_ref(email, p["title"])
            for tag in dict.fromkeys(t.strip().lower() for t in p.get("tags", []) if t.strip()):
                out(f"insert into tag (name) values ({q(tag)}) on conflict (name) do nothing;")
                out(f"insert into post_tag (post_id, tag_id) values ({ref}, (select tag_id from tag where name = {q(tag)}));")
            for n, c in enumerate(p.get("comments", [])):
                at = min(NOW, created + timedelta(hours=r.randint(1, 40) + n * r.randint(2, 30), minutes=r.randint(0, 59)))
                comments.append(f"insert into comment (users_id, post_id, body, created_at) values "
                                f"({user_ref(emails[c['by']])}, {ref}, {q(c['body'])}, {ts(at)});")
            all_posts.append((b["key"], email, p, created, views))
        out("")

    out("-- 댓글")
    lines.extend(comments)
    out("")

    # 좋아요: 다른 블로거 몇 명이 누른다 (한 사람이 한 글에 한 번)
    out("-- 좋아요")
    keys = [b["key"] for b in bloggers]
    for key, email, p, created, views in all_posts:
        r = rng_for("like", key, p["title"])
        others = [k for k in keys if k != key]
        likers = set(c["by"] for c in p.get("comments", []))
        likers |= set(r.sample(others, k=min(len(others), r.randint(0, 6))))
        for who in sorted(likers):
            at = min(NOW, created + timedelta(hours=r.randint(1, 96)))
            out(f"insert into post_like (post_id, users_id, created_at) values "
                f"({post_ref(email, p['title'])}, {user_ref(emails[who])}, {ts(at)});")
    out("")

    # 최근 7일 조회수: 실행하는 날 기준. 최근 글과 꾸준히 읽히는 글 몇 개가 인기 글이 된다
    out("-- 최근 7일 글 조회수 (실행하는 날 기준, 한국 날짜)")
    ranked = sorted(all_posts, key=lambda x: (x[3] >= NOW - timedelta(days=45), x[4] + rng_for("pop", x[2]["title"]).randint(0, 600)),
                    reverse=True)
    for rank, (key, email, p, created, views) in enumerate(ranked[:24]):
        r = rng_for("daily", key, p["title"])
        for d in range(7):
            daily = max(1, int(r.randint(8, 40) * (1.8 if rank < 6 else 1.0) * (1 + (6 - d) / 10)))
            day = f"(now() at time zone 'Asia/Seoul')::date - {d}"
            out(f"insert into post_daily_stat (post_id, stat_date, views) values "
                f"({post_ref(email, p['title'])}, {day}, {daily});")
    out("")

    # 블로그 일별 방문 (최근 30일): 블로그 관리 화면의 통계가 비어 보이지 않게
    out("-- 블로그 일별 조회·방문 (최근 30일)")
    for b in bloggers:
        email = emails[b["key"]]
        blog_ref = f"(select blog_id from blog where users_id = {user_ref(email)})"
        r = rng_for("blogstat", b["key"])
        base = r.randint(20, 90)
        for d in range(30):
            visitors = max(1, base + r.randint(-15, 25) + (12 if d % 7 in (5, 6) else 0))
            views_d = visitors + r.randint(visitors // 3, visitors)
            day = f"(now() at time zone 'Asia/Seoul')::date - {d}"
            out(f"insert into blog_daily_stat (blog_id, stat_date, views, visitors) values "
                f"({blog_ref}, {day}, {views_d}, {visitors});")
    out("")
    out("commit;")
    out(f"\\echo 예시 데이터를 넣었습니다: 블로거 {len(bloggers)}명, 글 {len(all_posts)}개")

    OUT.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"{OUT.relative_to(HERE.parent.parent)}: 블로거 {len(bloggers)}명, 글 {len(all_posts)}개")


if __name__ == "__main__":
    main()
