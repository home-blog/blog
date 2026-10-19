-- 예시 데이터 지우기: demo-content.sql이 넣은 예시 회원(demo.*@example.com)과 그 블로그·글·댓글·좋아요·통계.
-- 진짜 회원이 예시 글에 단 댓글·좋아요도 함께 지운다. 태그 이름(tag 표)은 남긴다 (서버도 글을 지울 때 태그 줄은 남긴다).
-- 실행: deploy/seed/README.md
\set ON_ERROR_STOP on
\if :{?schema}
\else
\set schema public
\endif
set search_path = :"schema";
begin;

create temporary table demo_users on commit drop as
    -- 예시 계정만: demo-content.sql이 넣은 "로그인 불가" 표식(BCrypt 해시가 아닌 값)과 예시 이메일을 모두 만족해야 한다.
    -- 가입으로 만든 계정은 비밀번호가 늘 BCrypt 해시라 이 표식을 가질 수 없다
    select users_id from users where password = '!demo-account-no-login' and email like 'demo.%@example.com';
create temporary table demo_blogs on commit drop as
    select blog_id from blog where users_id in (select users_id from demo_users);
create temporary table demo_posts on commit drop as
    select p.post_id from post p join category c on c.category_id = p.category_id
    where c.blog_id in (select blog_id from demo_blogs);

delete from post_like where post_id in (select post_id from demo_posts) or users_id in (select users_id from demo_users);
-- 0.14.0 답글: 예시 회원의 댓글에 남이 단 답글을 먼저 지운다 (답글이 원댓글을 가리킨다)
delete from comment where parent_id in (select comment_id from comment where users_id in (select users_id from demo_users));
delete from comment where post_id in (select post_id from demo_posts) or users_id in (select users_id from demo_users);
delete from post_tag where post_id in (select post_id from demo_posts);
delete from post_daily_stat where post_id in (select post_id from demo_posts);
delete from post where post_id in (select post_id from demo_posts);
delete from blog_daily_stat where blog_id in (select blog_id from demo_blogs);
delete from category where blog_id in (select blog_id from demo_blogs);
delete from blog where blog_id in (select blog_id from demo_blogs);
delete from users where users_id in (select users_id from demo_users);

commit;
\echo 예시 데이터를 지웠습니다
