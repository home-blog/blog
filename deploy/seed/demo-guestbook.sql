-- 방명록 예시 엽서 (자동 생성: deploy/seed/build_seed.py, 원본 deploy/seed/guestbook.json). 손으로 고치지 않는다
-- demo-content.sql 다음에 실행한다. 몇 번 실행해도 같다 (이미 있는 엽서는 넣지 않는다)
\set ON_ERROR_STOP on
\if :{?schema}
\else
\set schema public
\endif
set search_path = :"schema";
begin;
do $$ begin
  if not exists (select 1 from users where password = '!demo-account-no-login' and email like 'demo.%@example.com') then
    raise exception '예시 데이터가 없습니다. 먼저 demo-content.sql을 실행하세요';
  end if;
end $$;

insert into guestbook_entry (blog_id, users_id, body, stamp, is_secret, reply, replied_at, created_at) select b.blog_id, u.users_id, '도마 만드신 글 보고 저도 주말에 공방 체험 예약했어요!
대패밥 냄새가 여기까지 나는 것 같아요 :)', 'pine', false, '체험 가시면 꼭 후기 남겨 주세요. 첫 대패질은 손목에 힘을 빼는 게 반이에요.', now() - interval '3 hours' + interval '40 minutes', now() - interval '3 hours' from blog b join users o on o.users_id = b.users_id, users u where o.email = 'demo.woodwork@example.com' and u.email = 'demo.bakery@example.com' and not exists (select 1 from guestbook_entry e where e.blog_id = b.blog_id and e.users_id = u.users_id and e.body = '도마 만드신 글 보고 저도 주말에 공방 체험 예약했어요!
대패밥 냄새가 여기까지 나는 것 같아요 :)');

insert into guestbook_entry (blog_id, users_id, body, stamp, is_secret, reply, replied_at, created_at) select b.blog_id, u.users_id, '베란다에 둘 화분 받침대를 원목으로 짜 보고 싶은데, 물에 강한 나무가 따로 있을까요?', 'plum', false, '편백이나 오크가 물에 강한 편이에요. 오일 마감을 두 번 해 주면 오래갑니다.', now() - interval '20 hours' + interval '95 minutes', now() - interval '20 hours' from blog b join users o on o.users_id = b.users_id, users u where o.email = 'demo.woodwork@example.com' and u.email = 'demo.plant@example.com' and not exists (select 1 from guestbook_entry e where e.blog_id = b.blog_id and e.users_id = u.users_id and e.body = '베란다에 둘 화분 받침대를 원목으로 짜 보고 싶은데, 물에 강한 나무가 따로 있을까요?');

insert into guestbook_entry (blog_id, users_id, body, stamp, is_secret, reply, replied_at, created_at) select b.blog_id, u.users_id, '다음 달 공방 사진 작업 같이 해 보실래요? 자세한 건 메일로 드릴게요.', 'moon', true, null, null, now() - interval '30 hours' from blog b join users o on o.users_id = b.users_id, users u where o.email = 'demo.woodwork@example.com' and u.email = 'demo.film@example.com' and not exists (select 1 from guestbook_entry e where e.blog_id = b.blog_id and e.users_id = u.users_id and e.body = '다음 달 공방 사진 작업 같이 해 보실래요? 자세한 건 메일로 드릴게요.');

insert into guestbook_entry (blog_id, users_id, body, stamp, is_secret, reply, replied_at, created_at) select b.blog_id, u.users_id, 'useEffect 정리 글 덕분에 회사 코드에서 메모리 누수 하나 잡았습니다. 고마워요!', 'crane', false, '도움이 됐다니 기뻐요. 다음 글은 Suspense 이야기로 찾아갈게요.', now() - interval '6 hours' + interval '25 minutes', now() - interval '6 hours' from blog b join users o on o.users_id = b.users_id, users u where o.email = 'demo.frontnote@example.com' and u.email = 'demo.codecup@example.com' and not exists (select 1 from guestbook_entry e where e.blog_id = b.blog_id and e.users_id = u.users_id and e.body = 'useEffect 정리 글 덕분에 회사 코드에서 메모리 누수 하나 잡았습니다. 고마워요!');

insert into guestbook_entry (blog_id, users_id, body, stamp, is_secret, reply, replied_at, created_at) select b.blog_id, u.users_id, '개발은 잘 모르지만 글이 차분해서 출근길에 하나씩 읽고 있어요.', 'wave', false, null, null, now() - interval '50 hours' from blog b join users o on o.users_id = b.users_id, users u where o.email = 'demo.frontnote@example.com' and u.email = 'demo.runner@example.com' and not exists (select 1 from guestbook_entry e where e.blog_id = b.blog_id and e.users_id = u.users_id and e.body = '개발은 잘 모르지만 글이 차분해서 출근길에 하나씩 읽고 있어요.');

insert into guestbook_entry (blog_id, users_id, body, stamp, is_secret, reply, replied_at, created_at) select b.blog_id, u.users_id, '강릉 반나절 코스 그대로 다녀왔어요. 안목 해변 커피 거리에서 노을까지 완벽했습니다.', 'wave', false, '노을까지 보셨다니 제가 다 뿌듯하네요. 다음엔 겨울 바다 코스도 올려 볼게요!', now() - interval '12 hours' + interval '60 minutes', now() - interval '12 hours' from blog b join users o on o.users_id = b.users_id, users u where o.email = 'demo.halfday@example.com' and u.email = 'demo.sea@example.com' and not exists (select 1 from guestbook_entry e where e.blog_id = b.blog_id and e.users_id = u.users_id and e.body = '강릉 반나절 코스 그대로 다녀왔어요. 안목 해변 커피 거리에서 노을까지 완벽했습니다.');

commit;
\echo 방명록 예시 엽서를 맞췄습니다: 6장
