-- 예시 데이터 (자동 생성: deploy/seed/build_seed.py, 원본 deploy/seed/data/*.json). 손으로 고치지 않는다
-- 실행: deploy/seed/README.md. 한 번만 넣을 수 있다 (이미 있으면 아무것도 바꾸지 않고 멈춘다)
\set ON_ERROR_STOP on
\if :{?schema}
\else
\set schema public
\endif
set search_path = :"schema";
begin;
do $$ begin
  if exists (select 1 from users where password = '!demo-account-no-login' and email like 'demo.%@example.com') then
    raise exception '예시 데이터가 이미 있습니다. 다시 넣으려면 먼저 demo-remove.sql을 실행하세요';
  end if;
end $$;

-- 예시 회원 (로그인할 수 없는 계정)
insert into users (email, password, nickname, intro, created_at) values ('demo.bakery@example.com', '!demo-account-no-login', '오븐하나', '자취방 미니오븐 하나로 시작한 홈베이킹 3년 차. 성공보다 실패를 더 많이 기록합니다.', '2023-05-06T10:00:00+09:00');
insert into users (email, password, nickname, intro, created_at) values ('demo.codecup@example.com', '!demo-account-no-login', '커피한잔코드', '커피 마시면서 로그 읽는 3년 차 백엔드 개발자. Java, Spring, PostgreSQL, Docker.', '2023-02-20T10:00:00+09:00');
insert into users (email, password, nickname, intro, created_at) values ('demo.film@example.com', '!demo-account-no-login', '골목필름', '필름카메라 들고 골목과 계절을 천천히 걷습니다. 36컷에 담는 하루.', '2023-03-25T10:00:00+09:00');
insert into users (email, password, nickname, intro, created_at) values ('demo.frontnote@example.com', '!demo-account-no-login', '프론트노트', '화면 만드는 프론트엔드 개발자. React, TypeScript, CSS, 그리고 키보드로도 쓸 수 있는 화면.', '2023-06-11T10:00:00+09:00');
insert into users (email, password, nickname, intro, created_at) values ('demo.halfday@example.com', '!demo-account-no-login', '반나절여행', '멀리 못 가도 괜찮아. 근교 반나절이랑 가끔 해외 도시를 걷는 사람.', '2023-02-11T10:00:00+09:00');
insert into users (email, password, nickname, intro, created_at) values ('demo.homecook@example.com', '!demo-account-no-login', '퇴근집밥', '맞벌이 부부의 평일 저녁 담당. 30분 안에 차리는 집밥과 냉장고 파먹기를 기록해요.', '2023-02-19T10:00:00+09:00');
insert into users (email, password, nickname, intro, created_at) values ('demo.hometrain@example.com', '!demo-account-no-login', '퇴근홈트', '퇴근하고 매트 한 장으로 버티는 직장인. 맨몸운동과 가끔 헬스장, 그리고 질리지 않는 식단.', '2023-04-02T10:00:00+09:00');
insert into users (email, password, nickname, intro, created_at) values ('demo.mukja@example.com', '!demo-account-no-login', '먹자골목', '동네 밥집과 오래된 노포를 돌며 솔직하게 적는 사람. 광고 없이 내 돈 내고 먹어요.', '2023-06-02T10:00:00+09:00');
insert into users (email, password, nickname, intro, created_at) values ('demo.plant@example.com', '!demo-account-no-login', '베란다집사', '남향 베란다에서 화분 열네 개와 함께 사는 식물집사. 오늘도 손가락으로 흙을 찔러 봅니다.', '2023-05-06T10:00:00+09:00');
insert into users (email, password, nickname, intro, created_at) values ('demo.runner@example.com', '!demo-account-no-login', '새벽러너', '출근 전 새벽에 하천길을 뜁니다. 하프마라톤 서브2가 올해 목표.', '2023-02-26T10:00:00+09:00');
insert into users (email, password, nickname, intro, created_at) values ('demo.sea@example.com', '!demo-account-no-login', '바다마중', '주말마다 바닷가 소도시로 떠나는 30대 직장인. 파도 소리와 항구 아침밥을 좋아해요.', '2023-04-08T10:00:00+09:00');
insert into users (email, password, nickname, intro, created_at) values ('demo.woodwork@example.com', '!demo-account-no-login', '주말목수', '평일엔 사무실, 토요일엔 공방. 손에 남는 걸 만드는 주말 목공 기록을 남겨요.', '2023-04-02T10:00:00+09:00');

-- 오븐하나 · 오븐 하나 베이킹 노트
insert into blog (users_id, name, intro, comments_read_at) values ((select users_id from users where email = 'demo.bakery@example.com'), '오븐 하나 베이킹 노트', '23리터 미니오븐으로 시작한 식빵, 쿠키, 케이크 기록. 레시피는 실측 온도 기준이고, 망친 날도 숨기지 않고 원인과 함께 적어 둔다.', '2026-10-11T23:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), '미분류', 'public', true, 1, 0, '2023-05-06T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), '빵', 'public', false, 2, 1, '2023-05-06T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), '쿠키·구움과자', 'public', false, 3, 2, '2023-05-06T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), '케이크', 'public', false, 4, 3, '2023-05-06T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), '실패 기록', 'public', false, 5, 4, '2023-05-06T10:00:00+09:00');
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')) and name = '빵'), (select topic_id from topic where topic_name = '취미'), '23리터 미니오븐으로 구운 첫 식빵, 손반죽 40분의 기록', '자취방에 23리터짜리 미니오븐을 들였다. 원래는 냉동 피자 데우려고 산 건데, 상자에 그려진 식빵 그림을 보고 괜히 마음이 동했다. 그래서 오늘, 첫 식빵.

## 재료 (1근 틀 기준)

- 강력분 250g
- 설탕 15g, 소금 4g
- 인스턴트 드라이이스트 3g
- 우유 170g (미지근하게)
- 무염버터 20g

## 과정

1. 버터 빼고 다 섞어서 한 덩어리로 만든다.
2. 10분쯤 치대다가 버터를 넣고 다시 치댄다. 처음엔 손에 다 달라붙어서 정말 울고 싶었다.
3. 총 40분 정도 치대니까 반죽을 얇게 늘렸을 때 막이 비쳤다. 이게 글루텐 막이구나.
4. 1차 발효 따뜻한 곳에서 60분. 두 배로 부풀었다.
5. 세 덩이로 나눠 둥글리기, 15분 휴지.
6. 밀대로 밀어 말아서 틀에 넣고 2차 발효 40분. 틀 높이까지 올라올 때까지.
7. 170도 예열, 30분 굽기.

## 결과

윗면이 한쪽만 탔다. 미니오븐은 열선이 가까워서 그런 것 같다. 20분쯤에 **쿠킹포일을 덮었어야 했다.** 그래도 갈라 보니 결이 결대로 찢어져서, 혼자 부엌에서 소리 질렀다.

> 버터 냄새가 방 안에 꽉 찼다. 이 냄새 때문에 계속하게 될 것 같다.

다음 목표는 탄 데 없는 식빵.', 'public', 1115, '2023-05-20T22:10:00+09:00');
insert into tag (name) values ('식빵') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '23리터 미니오븐으로 구운 첫 식빵, 손반죽 40분의 기록'), (select tag_id from tag where name = '식빵'));
insert into tag (name) values ('손반죽') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '23리터 미니오븐으로 구운 첫 식빵, 손반죽 40분의 기록'), (select tag_id from tag where name = '손반죽'));
insert into tag (name) values ('미니오븐') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '23리터 미니오븐으로 구운 첫 식빵, 손반죽 40분의 기록'), (select tag_id from tag where name = '미니오븐'));
insert into tag (name) values ('홈베이킹') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '23리터 미니오븐으로 구운 첫 식빵, 손반죽 40분의 기록'), (select tag_id from tag where name = '홈베이킹'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')) and name = '실패 기록'), (select topic_id from topic where topic_name = '음식'), '버터쿠키가 팬케이크처럼 퍼졌다, 원인 세 가지', '오늘의 실패작. 짤주머니로 예쁘게 짠 버터쿠키가 오븐 안에서 서로 손을 잡고 한 장의 커다란 전병이 됐다. 맛은 있었다. 모양은 없었다.

## 레시피

- 무염버터 100g, 슈가파우더 60g
- 계란 노른자 1개
- 박력분 130g, 아몬드가루 20g
- 바닐라 익스트랙 조금
- 180도 15분

## 내가 찾은 원인

**1. 버터가 너무 녹았다**
실온에 두라길래 낮부터 꺼내 놨다. 보일러를 켜 둔 방이라 거의 녹은 상태였다. 손가락으로 눌렀을 때 쑥 들어가는 정도가 맞는데 나는 버터 수프였다.

**2. 짜고 바로 구웠다**
짠 다음 냉장고에 20~30분 넣어 굳혀야 모양이 산다고 한다. 나는 그 시간이 아까웠다.

**3. 오븐 온도가 낮았다**
오븐 온도계를 사서 재 보니 180도 설정이 실제로는 162도였다. 미니오븐 다이얼은 믿으면 안 된다는 걸 배웠다.

## 다음엔

- 버터는 굽기 1시간 전에만 꺼내기
- 짠 쿠키는 팬째로 냉장 30분
- 오븐은 다이얼 190도로 맞추고 온도계로 확인

퍼진 쿠키는 부숴서 요거트에 뿌려 먹었다. 실패도 나름 쓸모는 있다.', 'public', 623, '2023-11-04T23:30:00+09:00');
insert into tag (name) values ('버터쿠키') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '버터쿠키가 팬케이크처럼 퍼졌다, 원인 세 가지'), (select tag_id from tag where name = '버터쿠키'));
insert into tag (name) values ('실패') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '버터쿠키가 팬케이크처럼 퍼졌다, 원인 세 가지'), (select tag_id from tag where name = '실패'));
insert into tag (name) values ('원인분석') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '버터쿠키가 팬케이크처럼 퍼졌다, 원인 세 가지'), (select tag_id from tag where name = '원인분석'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')) and name = '쿠키·구움과자'), (select topic_id from topic where topic_name = '음식'), '드디어 정착한 초코칩 쿠키, 겉바속쫀 비율', '반년 동안 초코칩 쿠키만 열 번 넘게 구웠다. 친구들이 이제 그만 좀 가져오라고 할 때쯤 내 레시피가 생겼다.

## 재료 (지름 8cm 쿠키 12개)

- 무염버터 115g → **갈색이 될 때까지 태운 브라운버터**
- 흑설탕 100g, 백설탕 50g
- 계란 1개 + 노른자 1개
- 중력분 170g, 베이킹소다 1/2작은술, 소금 1/2작은술
- 다크 초콜릿 150g (칩 말고 판초콜릿을 칼로 다진 것)
- 플레이크 소금 약간

## 순서

1. 버터를 냄비에서 중불로 녹이다가 거품이 가라앉고 고소한 냄새가 나면 불을 끈다. 15분 식힌다.
2. 설탕 두 가지를 넣고 섞고, 계란과 노른자를 넣어 1분 휘핑.
3. 가루류를 체 쳐서 넣고 주걱으로 날가루가 안 보일 정도만 섞는다.
4. 초콜릿을 넣고 반죽을 **냉장고에서 하룻밤** 숙성.
5. 60g씩 떠서 팬에 간격 넓게, 위에 플레이크 소금.
6. 오븐 185도(실측) 11분. 가운데가 덜 익은 것 같을 때 꺼낸다.
7. 팬 위에서 10분 식힌다. 이때 속이 마저 익는다.

## 알게 된 것

- 흑설탕 비율이 높을수록 쫀득해진다.
- 노른자 하나 더 넣는 게 쫀득함의 핵심.
- 판초콜릿을 다지면 크기가 제각각이라 녹은 웅덩이가 생겨서 예쁘다.
- 하룻밤 숙성하면 캐러멜 맛이 확실히 깊어진다. 이건 참을 가치가 있다.

이제 쿠키 상자를 들고 가면 친구들이 다시 반긴다.', 'public', 632, '2024-04-13T21:00:00+09:00');
insert into tag (name) values ('초코칩쿠키') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '드디어 정착한 초코칩 쿠키, 겉바속쫀 비율'), (select tag_id from tag where name = '초코칩쿠키'));
insert into tag (name) values ('쿠키') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '드디어 정착한 초코칩 쿠키, 겉바속쫀 비율'), (select tag_id from tag where name = '쿠키'));
insert into tag (name) values ('레시피') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '드디어 정착한 초코칩 쿠키, 겉바속쫀 비율'), (select tag_id from tag where name = '레시피'));
insert into tag (name) values ('겉바속쫀') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '드디어 정착한 초코칩 쿠키, 겉바속쫀 비율'), (select tag_id from tag where name = '겉바속쫀'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')) and name = '빵'), (select topic_id from topic where topic_name = '음식'), '탕종법 우유식빵, 사흘이 지나도 촉촉했다', '식빵을 구우면 다음 날부터 퍽퍽해지는 게 늘 아쉬웠다. 혼자 사니까 한 근을 하루에 다 먹을 수도 없고. 그래서 이번엔 탕종법.

## 탕종이 뭐냐면

밀가루 일부를 뜨거운 물로 미리 익혀서 풀처럼 만들어 반죽에 넣는 방법이다. 익힌 전분이 수분을 꽉 잡아 줘서 빵이 오래 촉촉하다.

## 탕종 만들기 (전날 밤)

- 강력분 25g + 끓는 물 125g
- 바로 섞어서 걸쭉해지면 랩을 밀착해 덮고 식힌 뒤 냉장 하룻밤

## 본반죽

- 강력분 225g, 설탕 20g, 소금 4g, 드라이이스트 3g
- 탕종 전부, 우유 80g, 계란 25g
- 무염버터 20g

## 과정

1. 버터 빼고 섞어 15분 치대기. 탕종 때문에 반죽이 엄청 질다. **덧가루를 넣고 싶은 걸 참는다.**
2. 버터 넣고 15분 더. 막이 생기면 끝.
3. 1차 발효 70분.
4. 3분할, 둥글리기, 휴지 15분, 성형.
5. 2차 발효 45분, 틀 위로 1cm.
6. 윗면에 우유 바르고 175도 28분. 15분 지났을 때 포일 덮기.

## 비교

일반 식빵이랑 나란히 두고 사흘째에 먹어 봤다. 일반 식빵은 토스트를 해야 먹을 만했는데, 탕종 식빵은 **그냥 뜯어 먹어도 부드러웠다.** 하루 수고가 더 들지만 혼자 사는 사람에겐 이쪽이 정답 같다.

이제 미니오븐 윗면 타는 것도 포일로 잡았다. 1년 반 만이다.', 'public', 522, '2024-10-26T20:40:00+09:00');
insert into tag (name) values ('식빵') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '탕종법 우유식빵, 사흘이 지나도 촉촉했다'), (select tag_id from tag where name = '식빵'));
insert into tag (name) values ('탕종') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '탕종법 우유식빵, 사흘이 지나도 촉촉했다'), (select tag_id from tag where name = '탕종'));
insert into tag (name) values ('우유식빵') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '탕종법 우유식빵, 사흘이 지나도 촉촉했다'), (select tag_id from tag where name = '우유식빵'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')) and name = '쿠키·구움과자'), (select topic_id from topic where topic_name = '음식'), '발렌타인에 구운 휘낭시에와 마들렌, 같은 듯 다른 둘', '회사 동료들한테 나눠 주려고 구움과자를 구웠다. 휘낭시에랑 마들렌, 한 번에 둘 다. 비슷해 보이지만 만들어 보면 성격이 꽤 다르다.

## 휘낭시에 (12개)

- 계란 흰자 100g
- 슈가파우더 90g, 아몬드가루 45g, 박력분 35g
- **브라운버터 90g**
- 꿀 10g

1. 흰자를 거품 내지 않고 풀기만 한다.
2. 가루류를 체 쳐 넣고 섞은 뒤 꿀.
3. 따뜻한 브라운버터를 세 번에 나눠 넣는다.
4. 틀에 80%, 200도 12분.

## 마들렌 (12개)

- 계란 2개, 설탕 80g, 레몬 제스트 1/2개
- 박력분 90g, 베이킹파우더 3g
- 녹인 버터 90g

1. 계란, 설탕, 제스트를 섞고 가루를 넣는다.
2. 녹인 버터를 넣고 **냉장 2시간 휴지.**
3. 버터 바르고 밀가루 턴 틀에 짜서 190도 5분, 170도로 낮춰 6분.

## 다른 점 정리

- **계란**: 휘낭시에는 흰자만, 마들렌은 전란
- **버터**: 휘낭시에는 브라운버터, 마들렌은 그냥 녹인 버터
- **맛**: 휘낭시에는 고소하고 쫀득, 마들렌은 폭신하고 상큼

마들렌 배꼽이 처음으로 봉긋하게 나왔다. 차가운 반죽을 뜨거운 오븐에 넣는 온도 차가 배꼽을 만든다고 한다. 남은 노른자 네 개는 내일 커스터드가 될 예정.', 'public', 847, '2025-02-14T19:50:00+09:00');
insert into tag (name) values ('휘낭시에') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '발렌타인에 구운 휘낭시에와 마들렌, 같은 듯 다른 둘'), (select tag_id from tag where name = '휘낭시에'));
insert into tag (name) values ('마들렌') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '발렌타인에 구운 휘낭시에와 마들렌, 같은 듯 다른 둘'), (select tag_id from tag where name = '마들렌'));
insert into tag (name) values ('구움과자') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '발렌타인에 구운 휘낭시에와 마들렌, 같은 듯 다른 둘'), (select tag_id from tag where name = '구움과자'));
insert into tag (name) values ('선물') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '발렌타인에 구운 휘낭시에와 마들렌, 같은 듯 다른 둘'), (select tag_id from tag where name = '선물'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')) and name = '실패 기록'), (select topic_id from topic where topic_name = '음식'), '한여름 과발효 사건, 반죽이 볼을 넘어왔다', '일요일 오후, 모닝빵 반죽을 해 두고 잠깐 누웠다가 잠들었다. 일어나 보니 반죽이 볼 위로 넘쳐서 랩을 밀어 올리고 있었다. 실내 온도 31도. 여름 베이킹을 너무 얕봤다.

## 원래 계획

- 1차 발효 실온 50분
- 반죽 온도 27도 목표

## 실제로 일어난 일

- 물을 실온 그대로 썼더니 반죽 온도가 이미 30도
- 거기에 실온 31도에서 1시간 40분
- 반죽에서 시큼한 술 냄새가 났다

## 그래도 구워 봤다

그냥 버리기 아까워서 가스를 빼고 성형해서 구웠다. 결과는 예상대로였다. 부풀 힘이 남아 있지 않아서 납작하고, 껍질 색이 허옇고, 속은 구멍이 숭숭. 맛은 시큼했다.

## 여름 베이킹 규칙 (나를 위한 메모)

1. **물은 냉장고 물**을 쓴다. 한여름엔 얼음물 일부도 괜찮다.
2. 이스트를 레시피보다 20% 줄인다.
3. 발효는 시간이 아니라 **크기**로 판단한다. 두 배가 되면 끝.
4. 타이머를 꼭 맞춘다. 눕지 않는다.
5. 차라리 냉장 저온 발효로 돌린다. 밤에 반죽해서 냉장고에 넣고 아침에 굽기.

다음 주에 냉장 발효로 다시 구웠더니 이번엔 멀쩡했다. 실패한 판은 잘게 잘라 마늘버터 바르고 러스크로 구웠다. 이건 의외로 맛있었다.', 'public', 782, '2025-07-06T15:20:00+09:00');
insert into tag (name) values ('과발효') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '한여름 과발효 사건, 반죽이 볼을 넘어왔다'), (select tag_id from tag where name = '과발효'));
insert into tag (name) values ('여름베이킹') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '한여름 과발효 사건, 반죽이 볼을 넘어왔다'), (select tag_id from tag where name = '여름베이킹'));
insert into tag (name) values ('실패') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '한여름 과발효 사건, 반죽이 볼을 넘어왔다'), (select tag_id from tag where name = '실패'));
insert into tag (name) values ('모닝빵') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '한여름 과발효 사건, 반죽이 볼을 넘어왔다'), (select tag_id from tag where name = '모닝빵'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')) and name = '케이크'), (select topic_id from topic where topic_name = '음식'), '크리스마스 이브의 첫 딸기 생크림 케이크', '베이킹 2년 반 만에 처음으로 홀케이크에 도전했다. 지름 15cm, 딸기 생크림. 가족 모임에 들고 가겠다고 큰소리를 쳐 버려서 물러설 데가 없었다.

## 제누와즈 (15cm 1호)

- 계란 2개, 설탕 60g
- 박력분 60g
- 우유 15g + 버터 15g (따뜻하게 녹여 두기)

1. 계란과 설탕을 볼에 넣고 중탕으로 37도까지 데운다.
2. 핸드믹서 고속으로 5분, 저속으로 1분. 반죽을 떨어뜨렸을 때 **리본처럼 쌓였다가 천천히 사라지면** 된다.
3. 박력분을 두 번 체 쳐 넣고 바닥부터 크게 30번 정도 섞는다.
4. 반죽 한 주걱을 우유버터에 먼저 섞고, 그걸 다시 본반죽에 넣는다.
5. 170도 28분. 꺼내자마자 10cm 높이에서 한 번 떨어뜨린다.

## 아이싱

- 생크림 400ml + 설탕 32g, 단단하게 휘핑
- 딸기 한 팩, 반은 슬라이스, 반은 장식용
- 시럽: 물 50g + 설탕 25g

시트를 세 장으로 자르고, 시럽 바르고, 크림 바르고, 딸기 깔고를 반복했다.

## 솔직한 결과

- 시트는 성공. 꺼지지 않고 4.5cm까지 올라왔다.
- 아이싱은 실패에 가까웠다. 옆면이 울퉁불퉁해서 결국 딸기를 옆에도 둘러 붙여 가렸다.

그래도 조카가 "이거 진짜 이모가 만든 거야?" 하고 물었을 때 다 괜찮아졌다. 돌림판은 내년 선물 목록 1순위.', 'public', 641, '2025-12-24T23:10:00+09:00');
insert into tag (name) values ('생크림케이크') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '크리스마스 이브의 첫 딸기 생크림 케이크'), (select tag_id from tag where name = '생크림케이크'));
insert into tag (name) values ('제누와즈') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '크리스마스 이브의 첫 딸기 생크림 케이크'), (select tag_id from tag where name = '제누와즈'));
insert into tag (name) values ('크리스마스') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '크리스마스 이브의 첫 딸기 생크림 케이크'), (select tag_id from tag where name = '크리스마스'));
insert into tag (name) values ('딸기') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '크리스마스 이브의 첫 딸기 생크림 케이크'), (select tag_id from tag where name = '딸기'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')) and name = '케이크'), (select topic_id from topic where topic_name = '음식'), '엄마 생일 당근케이크, 크림치즈 프로스팅 꾸덕하게', '엄마는 생크림보다 당근케이크를 좋아한다. 동네 빵집 ''보리밭제과''에서 매년 사 드렸는데, 올해는 내가 굽겠다고 했다. 생크림 케이크 때보다는 덜 긴장됐다. 당근케이크는 아이싱이 좀 투박해도 그게 멋이니까.

## 시트 (15cm 원형 2개)

- 당근 200g (곱게 채 썰기)
- 계란 2개, 흑설탕 120g
- 식용유 120g
- 중력분 160g, 베이킹파우더 1작은술, 베이킹소다 1/2작은술
- 시나몬 2작은술, 넛맥 약간, 소금 한 꼬집
- 구운 호두 50g, 건포도 30g (럼에 불려 두기)

1. 계란과 흑설탕을 섞고 식용유를 조금씩 넣어 유화시킨다.
2. 가루와 향신료를 체 쳐 넣고 섞는다.
3. 당근, 호두, 건포도를 넣는다.
4. 반씩 나눠서 170도 30분. 꼬치를 찔러 깨끗하면 완성.
5. **완전히 식힌다.** 따뜻할 때 프로스팅하면 다 흘러내린다.

## 크림치즈 프로스팅

- 크림치즈 200g, 무염버터 50g (둘 다 실온)
- 슈가파우더 60g, 레몬즙 1작은술

크림치즈를 너무 오래 휘핑하면 묽어진다. 매끈해질 정도만 섞고 바로 냉장 20분.

## 결과

시트 두 장 사이에 프로스팅을 두껍게, 위에도 대충 올리고 호두 몇 알. 엄마가 한 조각 드시더니 "빵집 거보다 덜 달아서 좋다"고 하셨다. 내년에도 내가 굽는 걸로 정해졌다.', 'public', 188, '2026-05-10T18:30:00+09:00');
insert into tag (name) values ('당근케이크') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '엄마 생일 당근케이크, 크림치즈 프로스팅 꾸덕하게'), (select tag_id from tag where name = '당근케이크'));
insert into tag (name) values ('크림치즈') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '엄마 생일 당근케이크, 크림치즈 프로스팅 꾸덕하게'), (select tag_id from tag where name = '크림치즈'));
insert into tag (name) values ('생일케이크') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '엄마 생일 당근케이크, 크림치즈 프로스팅 꾸덕하게'), (select tag_id from tag where name = '생일케이크'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')) and name = '실패 기록'), (select topic_id from topic where topic_name = '취미'), '베이킹 3년 차, 망친 것들 모아 보니 보이는 것', '3년 전 이맘때 미니오븐을 샀다. 지난주에 드디어 32리터 컨벡션 오븐으로 바꿨는데, 미니오븐을 정리하다가 사진첩을 보니 망친 사진이 성공 사진보다 많았다. 그래서 정리해 본 내 실패의 역사.

## 망친 것 베스트 5

1. **전병이 된 버터쿠키** (2023): 녹은 버터, 낮은 오븐 온도
2. **돌덩이 스콘** (2024): 반죽을 너무 치대서 글루텐이 생겼다
3. **꺼진 쉬폰케이크** (2024): 틀에 기름을 발랐다. 쉬폰 틀은 바르면 안 된다
4. **넘친 모닝빵** (2025): 한여름 과발효
5. **갈라진 바스크 치즈케이크** (2026): 너무 높은 온도에서 너무 오래

## 실패의 공통점

정리해 보니 원인은 크게 세 가지였다.

- **온도**: 버터 온도, 반죽 온도, 오븐 온도. 거의 모든 실패에 온도가 끼어 있다.
- **조급함**: 휴지, 숙성, 식히기를 건너뛴 날은 꼭 뭔가 잘못됐다.
- **레시피를 대충 읽음**: 쉬폰 틀 사건이 대표적이다.

## 3년 동안 산 것 중 정말 쓸모 있던 것

- 오븐 온도계 (만 원 안쪽인데 가장 중요한 도구)
- 디지털 저울 0.1g 단위
- 실리콘 주걱 큰 것, 작은 것
- 스크래퍼

> 실패한 빵도 다 먹었다. 그래서 살이 쪘다.

새 오븐은 위아래 열이 고르게 나와서 아직 한 번도 안 탔다. 그래도 온도계는 계속 넣어 둔다. 3년 동안 배운 가장 큰 교훈은 결국 **내 오븐을 믿지 말 것**이니까.', 'public', 262, '2026-09-20T21:45:00+09:00');
insert into tag (name) values ('실패모음') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '베이킹 3년 차, 망친 것들 모아 보니 보이는 것'), (select tag_id from tag where name = '실패모음'));
insert into tag (name) values ('베이킹일기') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '베이킹 3년 차, 망친 것들 모아 보니 보이는 것'), (select tag_id from tag where name = '베이킹일기'));
insert into tag (name) values ('오븐') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '베이킹 3년 차, 망친 것들 모아 보니 보이는 것'), (select tag_id from tag where name = '오븐'));
insert into tag (name) values ('홈베이킹') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '베이킹 3년 차, 망친 것들 모아 보니 보이는 것'), (select tag_id from tag where name = '홈베이킹'));

-- 커피한잔코드 · 커피 한 잔, 로그 한 줄
insert into blog (users_id, name, intro, comments_read_at) values ((select users_id from users where email = 'demo.codecup@example.com'), '커피 한 잔, 로그 한 줄', '백엔드 개발하면서 밟은 삽질과 공부한 것, 해마다 쓰는 회고를 모아 둡니다. 같은 실수를 두 번 하지 않으려고 씁니다.', '2026-10-11T23:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), '미분류', 'public', true, 1, 0, '2023-02-20T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), '스프링', 'public', false, 2, 1, '2023-02-20T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), '데이터베이스', 'public', false, 3, 2, '2023-02-20T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), '도커', 'public', false, 4, 3, '2023-02-20T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), '회고', 'public', false, 5, 4, '2023-02-20T10:00:00+09:00');
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')) and name = '스프링'), (select topic_id from topic where topic_name = '개발'), '@Transactional이 안 먹던 이유: 같은 클래스 안에서 부르면 프록시를 안 탄다', '입사 첫 해 처음으로 혼자 맡은 배치 작업에서 **롤백이 안 되는** 문제가 났다. 주문 여러 건을 저장하다가 중간에 예외가 나면 앞에서 저장한 건 롤백돼야 하는데, 이미 DB에 들어가 있었다. 분명 `@Transactional`을 붙였는데.

## 문제의 코드

```java
@Service
public class OrderBatchService {

    private final OrderRepository orderRepository;

    public OrderBatchService(OrderRepository orderRepository) {
        this.orderRepository = orderRepository;
    }

    public void placeAll(List<OrderRequest> requests) {
        for (OrderRequest request : requests) {
            saveOrder(request); // 여기!
        }
    }

    @Transactional
    public void saveOrder(OrderRequest request) {
        orderRepository.save(Order.from(request));
        if (request.quantity() <= 0) {
            throw new IllegalArgumentException("수량 오류");
        }
    }
}
```

컨트롤러는 `placeAll()`을 부르고, `placeAll()`이 같은 클래스의 `saveOrder()`를 부른다.

## 원인

스프링의 선언적 트랜잭션은 **프록시**로 동작한다. 다른 빈이 `OrderBatchService`를 주입받으면 실제로는 프록시 객체를 받고, 프록시가 메서드 호출을 가로채서 트랜잭션을 연다.

그런데 `placeAll()` 안에서 `saveOrder()`를 부르는 건 `this.saveOrder()`다. 프록시를 거치지 않고 **원래 객체의 메서드를 직접** 부르니까 `@Transactional`이 아무 일도 안 한다. 게다가 `placeAll()`에는 트랜잭션이 없으니 `save()`는 리포지토리 기본 트랜잭션으로 각각 커밋돼 버렸다.

## 해결

이번 경우는 원래 의도가 "전체를 하나로 묶기"였어서 그냥 바깥 메서드에 붙였다.

```java
@Transactional
public void placeAll(List<OrderRequest> requests) {
    for (OrderRequest request : requests) {
        saveOrder(request);
    }
}
```

건마다 따로 커밋해야 하는 경우였다면 `saveOrder()`를 **다른 빈으로 분리**하는 게 맞다. 자기 자신을 주입받는 방법도 있긴 한데 코드 읽는 사람이 헷갈려서 안 쓰기로 했다.

## 같이 알게 된 것

- `private` 메서드에 붙인 `@Transactional`도 같은 이유로 동작하지 않는다.
- 기본 롤백 대상은 **언체크 예외**(`RuntimeException`, `Error`)다. 체크 예외는 `rollbackFor`를 지정해야 한다.
- 로그로 확인하려면 `logging.level.org.springframework.transaction=DEBUG`를 켜면 트랜잭션 시작/커밋이 찍힌다.

> 어노테이션 붙였다고 끝이 아니라, **누가 어떤 경로로 부르는지**를 봐야 한다.
', 'public', 1421, '2023-03-05T22:10:00+09:00');
insert into tag (name) values ('spring') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '@Transactional이 안 먹던 이유: 같은 클래스 안에서 부르면 프록시를 안 탄다'), (select tag_id from tag where name = 'spring'));
insert into tag (name) values ('transactional') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '@Transactional이 안 먹던 이유: 같은 클래스 안에서 부르면 프록시를 안 탄다'), (select tag_id from tag where name = 'transactional'));
insert into tag (name) values ('aop') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '@Transactional이 안 먹던 이유: 같은 클래스 안에서 부르면 프록시를 안 탄다'), (select tag_id from tag where name = 'aop'));
insert into tag (name) values ('삽질기') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '@Transactional이 안 먹던 이유: 같은 클래스 안에서 부르면 프록시를 안 탄다'), (select tag_id from tag where name = '삽질기'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')) and name = '스프링'), (select topic_id from topic where topic_name = '개발'), 'JPA N+1 문제, fetch join이랑 batch size로 잡은 기록', '팀 목록 API가 느리다는 얘기가 나와서 쿼리 로그를 켜 봤더니 **팀 1번 조회 + 멤버 조회가 팀 수만큼** 찍혀 있었다. 말로만 듣던 N+1을 내가 만들었다.

## 상황

```java
@Entity
public class Team {
    @Id @GeneratedValue
    private Long id;

    private String name;

    @OneToMany(mappedBy = "team")
    private List<Member> members = new ArrayList<>();
}
```

```java
List<Team> teams = teamRepository.findAll();
return teams.stream()
        .map(t -> new TeamResponse(t.getName(), t.getMembers().size()))
        .toList();
```

`@OneToMany`는 기본이 지연 로딩이라 `findAll()`에서는 팀만 가져온다. 그런데 `getMembers().size()`에서 컬렉션을 초기화하면서 **팀마다 select가 한 번씩** 더 나간다. 팀이 50개면 쿼리가 51번.

## 방법 1: fetch join

```java
public interface TeamRepository extends JpaRepository<Team, Long> {

    @Query("select t from Team t join fetch t.members")
    List<Team> findAllWithMembers();
}
```

쿼리 한 번으로 끝난다. 예전에는 컬렉션 fetch join에 `distinct`를 꼭 붙여야 했는데, Hibernate 6부터는 루트 엔티티 중복을 알아서 걸러 준다.

`@EntityGraph`로도 같은 걸 할 수 있다.

```java
@EntityGraph(attributePaths = "members")
@Query("select t from Team t")
List<Team> findAllWithMembersGraph();
```

**문제는 페이징**이었다. 컬렉션을 fetch join 하면서 `Pageable`을 넘기면 Hibernate가 경고를 남기고 **전부 메모리에 올린 뒤 잘라서** 준다. 데이터가 많으면 위험하다.

## 방법 2: batch size

페이징이 필요한 화면은 fetch join 대신 이걸 썼다.

```yaml
spring:
  jpa:
    properties:
      hibernate:
        default_batch_fetch_size: 100
```

이러면 지연 로딩할 때 `where team_id in (?, ?, ...)`로 **최대 100개씩 묶어서** 가져온다. 팀 50개면 팀 조회 1번 + 멤버 조회 1번.

## 정리

| 상황 | 쓴 방법 |
|---|---|
| 페이징 없음, 연관 데이터 꼭 필요 | fetch join / `@EntityGraph` |
| 페이징 있음 + 컬렉션 | `default_batch_fetch_size` |
| 화면에 개수만 필요 | 아예 DTO로 `count` 쿼리 |

결국 이 API는 멤버 수만 필요했어서 마지막 방법으로 갔다. 엔티티를 다 가져올 필요가 없었던 거다.

> 쿼리 로그는 개발 환경에서 항상 켜 두기로 했다. 안 보면 모른다.
', 'public', 1395, '2023-07-23T23:00:00+09:00');
insert into tag (name) values ('jpa') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'JPA N+1 문제, fetch join이랑 batch size로 잡은 기록'), (select tag_id from tag where name = 'jpa'));
insert into tag (name) values ('n+1') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'JPA N+1 문제, fetch join이랑 batch size로 잡은 기록'), (select tag_id from tag where name = 'n+1'));
insert into tag (name) values ('hibernate') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'JPA N+1 문제, fetch join이랑 batch size로 잡은 기록'), (select tag_id from tag where name = 'hibernate'));
insert into tag (name) values ('성능') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'JPA N+1 문제, fetch join이랑 batch size로 잡은 기록'), (select tag_id from tag where name = '성능'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')) and name = '회고'), (select topic_id from topic where topic_name = '개발'), '1년 차 회고: 돌아가는 코드에서 설명할 수 있는 코드로', '올해 2월에 입사했으니 거의 1년이다. 연말이라 그냥 넘기기 아쉬워서 적어 둔다. 기술 정리라기보다 **일기**에 가깝다.

## 한 일

- 사내 주문 관리 서비스 유지보수 (Spring Boot, JPA, PostgreSQL)
- 정산 배치 하나를 처음부터 맡아서 만들었다
- 장애 대응 두 번 참여 (한 번은 내가 원인 제공...)

## 잘한 것

### 모르는 걸 바로 물어봤다
처음 석 달은 질문하는 게 너무 눈치 보였다. 그런데 사수가 "30분 혼자 봐도 모르겠으면 물어봐"라는 기준을 정해 줘서 그 뒤로는 편해졌다. **30분 규칙**은 내년에도 지킨다.

### 블로그를 시작했다
삽질한 걸 적다 보니, 같은 실수를 두 번 하는 일이 줄었다. `@Transactional` 글은 사내 위키에도 옮겨 놨다.

## 아쉬운 것

### 테스트를 미뤘다
"기능 먼저 하고 테스트는 나중에"를 반복했다. 결국 정산 배치에서 경계값 버그가 났는데, 테스트 하나만 있었어도 잡혔을 문제였다.

### 이유를 설명하지 못했다
코드 리뷰에서 "왜 이렇게 했어요?"라는 질문에 "이렇게 하니까 돌아가서요"라고 답한 적이 있다. 그날 꽤 부끄러웠다. 돌아가는 것과 **설명할 수 있는 것**은 다르다.

## 내년 목표

1. 새로 만드는 기능은 테스트부터 (적어도 서비스 계층은)
2. DB 공부: 실행 계획 읽는 법, 인덱스 제대로 이해하기
3. Docker로 로컬 환경 통일해 보기
4. 한 달에 글 하나는 쓰기

> 1년 동안 제일 많이 늘어난 건 코드 실력보다 **모른다고 말하는 속도**인 것 같다.
', 'public', 479, '2023-12-31T21:30:00+09:00');
insert into tag (name) values ('회고') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '1년 차 회고: 돌아가는 코드에서 설명할 수 있는 코드로'), (select tag_id from tag where name = '회고'));
insert into tag (name) values ('주니어개발자') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '1년 차 회고: 돌아가는 코드에서 설명할 수 있는 코드로'), (select tag_id from tag where name = '주니어개발자'));
insert into tag (name) values ('백엔드') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '1년 차 회고: 돌아가는 코드에서 설명할 수 있는 코드로'), (select tag_id from tag where name = '백엔드'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')) and name = '데이터베이스'), (select topic_id from topic where topic_name = '개발'), 'PostgreSQL에서 인덱스 탔는지 EXPLAIN ANALYZE로 확인하기', '작년 회고에서 실행 계획 읽기를 목표로 적었는데, 마침 블로그 글 목록 쿼리가 느리다는 이슈가 와서 제대로 공부해 볼 기회가 생겼다.

## 느린 쿼리

```sql
SELECT id, title, created_at
FROM post
WHERE blog_id = 42
ORDER BY created_at DESC
LIMIT 20;
```

`post` 테이블은 몇백만 건, `blog_id`에는 인덱스가 없었다.

## EXPLAIN과 EXPLAIN ANALYZE

- `EXPLAIN`: 플래너가 **예상한** 계획만 보여 준다. 쿼리는 실행하지 않는다.
- `EXPLAIN ANALYZE`: 쿼리를 **실제로 실행**하고 실제 시간과 행 수를 같이 보여 준다.

그래서 `UPDATE`나 `DELETE`에 `EXPLAIN ANALYZE`를 쓸 땐 트랜잭션 안에서 하고 롤백해야 한다. 진짜로 지워진다.

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, title, created_at
FROM post
WHERE blog_id = 42
ORDER BY created_at DESC
LIMIT 20;
```

결과(줄임):

```text
Limit  (actual time=812.4..812.5 rows=20 loops=1)
  ->  Sort  (actual time=812.4..812.4 rows=20 loops=1)
        Sort Key: created_at DESC
        ->  Seq Scan on post  (actual time=0.03..790.1 rows=1840 loops=1)
              Filter: (blog_id = 42)
              Rows Removed by Filter: 3198160
```

읽는 법은 **안쪽(들여쓰기 깊은 것)부터**. 테이블 전체를 훑고(Seq Scan), 300만 건 넘게 버리고, 남은 걸 정렬한 다음 20개를 자른다.

## 인덱스 추가

조건과 정렬을 같이 커버하도록 복합 인덱스를 만들었다.

```sql
CREATE INDEX CONCURRENTLY idx_post_blog_created
    ON post (blog_id, created_at DESC);
```

운영 DB라서 `CONCURRENTLY`를 붙였다. 테이블에 쓰기 잠금을 오래 걸지 않는 대신 시간이 더 걸리고, **트랜잭션 블록 안에서는 실행할 수 없다**. 마이그레이션 도구에서 쓸 때 이것 때문에 한 번 실패했다.

```text
Limit  (actual time=0.05..0.11 rows=20 loops=1)
  ->  Index Scan using idx_post_blog_created on post  (actual time=0.05..0.10 rows=20 loops=1)
        Index Cond: (blog_id = 42)
```

Sort 단계가 사라졌다. 인덱스가 이미 `created_at DESC` 순서라서 앞에서 20개만 읽으면 끝난다. 812ms → 0.1ms.

## 같이 본 것

- 계획의 **예상 rows**와 **실제 rows**가 크게 다르면 통계가 오래된 것일 수 있다. `ANALYZE post;`로 갱신.
- 작은 테이블은 인덱스가 있어도 Seq Scan이 더 빠를 수 있어서 플래너가 그쪽을 고른다. 이건 정상이다.
- 컬럼 순서가 중요하다. `(created_at, blog_id)`였으면 이 쿼리엔 별 도움이 안 됐을 거다.

> 인덱스를 "추가했다"가 아니라 "**탔다**"를 확인하는 습관.
', 'public', 784, '2024-04-14T20:40:00+09:00');
insert into tag (name) values ('postgresql') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'PostgreSQL에서 인덱스 탔는지 EXPLAIN ANALYZE로 확인하기'), (select tag_id from tag where name = 'postgresql'));
insert into tag (name) values ('인덱스') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'PostgreSQL에서 인덱스 탔는지 EXPLAIN ANALYZE로 확인하기'), (select tag_id from tag where name = '인덱스'));
insert into tag (name) values ('실행계획') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'PostgreSQL에서 인덱스 탔는지 EXPLAIN ANALYZE로 확인하기'), (select tag_id from tag where name = '실행계획'));
insert into tag (name) values ('explain') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'PostgreSQL에서 인덱스 탔는지 EXPLAIN ANALYZE로 확인하기'), (select tag_id from tag where name = 'explain'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')) and name = '도커'), (select topic_id from topic where topic_name = '개발'), 'Docker 컨테이너에서 Spring Boot가 DB에 못 붙던 이유: localhost의 함정', '로컬 환경을 Docker Compose로 통일하는 작업을 하다가 반나절을 날렸다. 앱 컨테이너가 계속 `Connection refused`를 뱉으면서 죽었다.

## 처음 설정

```yaml
services:
  db:
    image: postgres:16
    environment:
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    ports:
      - "5432:5432"

  app:
    build: .
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://localhost:5432/postgres
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
    ports:
      - "8080:8080"
```

IDE에서 앱을 띄울 땐 잘 되던 URL 그대로였다.

## 원인 1: 컨테이너 안의 localhost는 그 컨테이너 자신

컨테이너마다 네트워크 공간이 따로 있어서, `app` 컨테이너 안에서 `localhost`는 **`app` 컨테이너 자신**이다. 거기엔 5432에서 듣는 게 없으니 거절당한 것.

Compose는 서비스들을 같은 네트워크에 묶고 **서비스 이름으로 DNS**를 잡아 준다. 그래서 `localhost` 대신 `db`를 쓰면 된다.

```yaml
SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/postgres
```

참고로 컨테이너끼리는 `ports`가 없어도 통신된다. `ports`는 **호스트(내 노트북)에서 접근**할 때만 필요하다.

## 원인 2: DB가 준비되기 전에 앱이 떴다

주소를 고쳤더니 이번엔 가끔만 실패했다. `depends_on`은 컨테이너 **시작 순서**만 보장하지, PostgreSQL이 접속을 받을 준비가 됐는지는 모른다.

```yaml
services:
  db:
    image: postgres:16
    environment:
      POSTGRES_PASSWORD: ${DB_PASSWORD}
    healthcheck:
      test: ["CMD-SHELL", "pg_isready -U postgres"]
      interval: 5s
      timeout: 3s
      retries: 10

  app:
    build: .
    environment:
      SPRING_DATASOURCE_URL: jdbc:postgresql://db:5432/postgres
      SPRING_DATASOURCE_USERNAME: postgres
      SPRING_DATASOURCE_PASSWORD: ${DB_PASSWORD}
    depends_on:
      db:
        condition: service_healthy
```

`condition: service_healthy`를 주면 헬스체크가 통과할 때까지 앱 시작을 기다린다.

## 디버깅하면서 쓴 명령

```bash
docker compose ps                  # 상태와 health 확인
docker compose logs -f app         # 앱 로그 따라가기
docker compose exec app sh         # 컨테이너 안에 들어가서
getent hosts db                    # 서비스 이름이 풀리는지 확인
```

> "내 컴퓨터에선 되는데"의 내 컴퓨터가 **어디인지**부터 확인하자.
', 'public', 967, '2024-10-20T22:15:00+09:00');
insert into tag (name) values ('docker') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'Docker 컨테이너에서 Spring Boot가 DB에 못 붙던 이유: localhost의 함정'), (select tag_id from tag where name = 'docker'));
insert into tag (name) values ('dockercompose') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'Docker 컨테이너에서 Spring Boot가 DB에 못 붙던 이유: localhost의 함정'), (select tag_id from tag where name = 'dockercompose'));
insert into tag (name) values ('springboot') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'Docker 컨테이너에서 Spring Boot가 DB에 못 붙던 이유: localhost의 함정'), (select tag_id from tag where name = 'springboot'));
insert into tag (name) values ('삽질기') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'Docker 컨테이너에서 Spring Boot가 DB에 못 붙던 이유: localhost의 함정'), (select tag_id from tag where name = '삽질기'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')) and name = '스프링'), (select topic_id from topic where topic_name = '개발'), 'Testcontainers로 통합 테스트 옮긴 후기', '그동안 리포지토리 테스트는 H2 인메모리 DB로 돌렸다. 빠르고 편했는데, 운영은 PostgreSQL이라 **H2에서는 통과하는데 운영에서 터지는** 일이 몇 번 있었다. `jsonb` 컬럼, `ON CONFLICT`, 윈도 함수 같은 것들.

그래서 이번 분기에 통합 테스트를 Testcontainers로 옮겼다.

## 설정

의존성은 `spring-boot-testcontainers`, `testcontainers`의 `junit-jupiter`, `postgresql` 모듈.

```java
@SpringBootTest
@Testcontainers
class PostRepositoryTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");

    @Autowired
    PostRepository postRepository;

    @Test
    void 블로그별_최신글_20개를_가져온다() {
        // given: 테스트 데이터 저장
        // when
        List<Post> posts = postRepository.findTop20ByBlogIdOrderByCreatedAtDesc(1L);
        // then
        assertThat(posts).hasSizeLessThanOrEqualTo(20);
    }
}
```

예전엔 `@DynamicPropertySource`로 URL, 계정, 비밀번호를 하나하나 넣어 줬는데, Spring Boot 3.1부터는 `@ServiceConnection`만 붙이면 접속 정보를 알아서 연결해 준다. 코드가 확 줄었다.

## 느려진 것 대응

컨테이너를 띄우니까 당연히 느려졌다. 처음엔 테스트 클래스마다 컨테이너를 새로 띄워서 전체 테스트가 4분에서 11분이 됐다.

### 컨테이너 공유
`static` 필드 컨테이너를 **추상 베이스 클래스**에 두고 테스트 클래스들이 상속하게 했다. 스프링 테스트 컨텍스트 캐시도 같이 재사용되도록 설정을 통일했다.

```java
@SpringBootTest
@Testcontainers
public abstract class IntegrationTestSupport {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres =
            new PostgreSQLContainer<>("postgres:16-alpine");
}
```

### 단위 테스트와 나누기
서비스 로직 대부분은 리포지토리를 목으로 바꾼 **단위 테스트**로 돌리고, 쿼리가 중요한 것만 통합 테스트로 남겼다.

결과적으로 전체 6분 정도. 예전보다 느리지만 **운영과 같은 DB**에서 검증된다는 안정감이 훨씬 크다.

## 겪은 문제

- CI 러너에서 Docker를 못 찾아서 실패 → Docker가 되는 러너로 바꿈
- 테스트 간 데이터가 섞임 → 각 테스트 끝에 정리하거나 `@Transactional`로 롤백
- Flyway 마이그레이션도 테스트에서 같이 돌아가서, **마이그레이션 오류를 테스트에서 먼저** 잡게 된 건 덤

> "테스트는 통과했는데요"가 의미 있으려면, 테스트 환경이 운영을 닮아야 한다.
', 'public', 313, '2025-02-09T21:00:00+09:00');
insert into tag (name) values ('testcontainers') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'Testcontainers로 통합 테스트 옮긴 후기'), (select tag_id from tag where name = 'testcontainers'));
insert into tag (name) values ('테스트') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'Testcontainers로 통합 테스트 옮긴 후기'), (select tag_id from tag where name = '테스트'));
insert into tag (name) values ('springboot') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'Testcontainers로 통합 테스트 옮긴 후기'), (select tag_id from tag where name = 'springboot'));
insert into tag (name) values ('postgresql') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'Testcontainers로 통합 테스트 옮긴 후기'), (select tag_id from tag where name = 'postgresql'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')) and name = '데이터베이스'), (select topic_id from topic where topic_name = '개발'), 'PostgreSQL 데드락 로그 읽는 법, 이번엔 진짜 데드락이었다', '정산 배치가 새벽에 가끔 실패한다는 알림이 왔다. 로그를 보니 이런 게 있었다.

```text
ERROR:  deadlock detected
DETAIL:  Process 21843 waits for ShareLock on transaction 9817734; blocked by process 21851.
        Process 21851 waits for ShareLock on transaction 9817731; blocked by process 21843.
HINT:  See server log for query details.
```

## 로그 읽기

- **21843**은 트랜잭션 9817734가 끝나길 기다린다. 그 트랜잭션은 21851의 것.
- **21851**은 트랜잭션 9817731이 끝나길 기다린다. 그건 21843의 것.

서로가 서로를 기다리는 원형 대기. PostgreSQL은 이걸 감지하면 **둘 중 하나를 에러로 끝내서** 풀어 준다. 그래서 하나는 실패하고 하나는 성공한다.

`ShareLock on transaction`은 보통 **다른 트랜잭션이 잡고 있는 행을 수정하려고 기다릴 때** 나온다. 행 잠금을 쥔 트랜잭션이 끝나기를 기다리는 것.

서버 로그 쪽에는 각 프로세스가 실행하던 쿼리가 같이 찍혀 있어서 그걸로 범인을 찾았다.

## 원인

두 배치 작업이 같은 계좌 잔액을 갱신하는데, **순서가 달랐다**.

- 작업 A: 계좌 1 → 계좌 2 순서로 `UPDATE`
- 작업 B: 계좌 2 → 계좌 1 순서로 `UPDATE`

A가 1을 잡고, B가 2를 잡은 상태에서 서로 상대 것을 기다리면 끝.

## 해결: 잠그는 순서를 통일

처리할 행을 **항상 같은 순서로** 먼저 잠그게 바꿨다.

```sql
SELECT id
FROM account
WHERE id IN (:ids)
ORDER BY id
FOR UPDATE;
```

JPA에서는 이렇게 썼다.

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select a from Account a where a.id in :ids order by a.id")
List<Account> findAllForUpdate(@Param("ids") Collection<Long> ids);
```

id 오름차순으로 잠그니까 두 작업이 같은 행을 노려도 한쪽이 그냥 기다리기만 하고 원형 대기는 생기지 않는다.

## 같이 손본 설정

- `log_lock_waits = on`: `deadlock_timeout`(기본 1초)보다 오래 잠금을 기다리면 로그를 남긴다. 데드락까지 안 가도 **잠금 대기 자체**를 볼 수 있다.
- 배치 쪽에는 데드락 에러 시 **짧게 한 번 재시도**를 넣었다. 순서를 맞췄어도 다른 경로에서 생길 수 있으니 안전장치로.
- 트랜잭션 범위를 줄였다. 잠금을 오래 쥘수록 부딪칠 확률이 커진다.

> 데드락은 DB가 이상한 게 아니라, 내 코드가 **서로 다른 순서로** 문을 잠근 결과였다.
', 'public', 394, '2025-08-17T22:30:00+09:00');
insert into tag (name) values ('postgresql') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'PostgreSQL 데드락 로그 읽는 법, 이번엔 진짜 데드락이었다'), (select tag_id from tag where name = 'postgresql'));
insert into tag (name) values ('데드락') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'PostgreSQL 데드락 로그 읽는 법, 이번엔 진짜 데드락이었다'), (select tag_id from tag where name = '데드락'));
insert into tag (name) values ('트랜잭션') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'PostgreSQL 데드락 로그 읽는 법, 이번엔 진짜 데드락이었다'), (select tag_id from tag where name = '트랜잭션'));
insert into tag (name) values ('장애') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'PostgreSQL 데드락 로그 읽는 법, 이번엔 진짜 데드락이었다'), (select tag_id from tag where name = '장애'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')) and name = '회고'), (select topic_id from topic where topic_name = '개발'), '2025 회고: 3년 차가 되면서 달라진 것들', '매년 연말에 쓰다가 올해는 바빠서 새해 첫 일요일에 쓴다. 2월이면 3년 차가 된다.

## 올해 한 일

- 주문 서비스에서 **정산 도메인**으로 옮겼다. 돈이 걸린 코드는 무게가 다르다.
- 통합 테스트를 Testcontainers로 옮기는 작업을 주도했다.
- 신입 두 명 온보딩을 맡았다.
- 블로그 글 11개. 목표(한 달 하나)는 아깝게 실패.

## 달라진 것

### 장애를 대하는 태도
1년 차 때는 장애가 나면 일단 겁부터 났다. 올해 데드락 건을 겪으면서 **로그를 처음부터 끝까지 읽는** 습관이 생겼다. 무섭다고 대충 재시작부터 하면 같은 일이 다시 난다.

### 리뷰에서 하는 말
예전엔 "이거 왜 이렇게 했어요?"가 무서운 질문이었는데, 이제는 내가 신입한테 하는 질문이 됐다. 대신 **"이렇게 하면 이런 경우가 걱정돼요"** 처럼 이유를 같이 붙이려고 한다. 1년 차 때 그 질문에 대답 못 했던 기억이 있어서.

### 코드보다 문서
정산 로직은 기획서보다 코드가 진실인 경우가 많았다. 그래서 규칙을 하나 찾을 때마다 위키에 표로 정리했다. 연말에 다른 팀이 그 표를 보고 질문 없이 연동을 끝냈다는 얘기를 듣고 꽤 뿌듯했다.

## 아쉬운 것

- 건강. 작년보다 앉아 있는 시간이 확실히 늘었다. 허리가 신호를 보낸다.
- 새 기술을 거의 못 만졌다. 회사 일만으로 한 해가 지나간 느낌.

## 2026 목표

1. 운영 지표 보는 법 공부: 커넥션 풀, 응답 시간 분포, GC
2. 사이드로 작은 서비스 하나 끝까지 배포해 보기
3. 주 3회 운동 (이웃 블로그 홈트 루틴 따라 하는 중)
4. 글은 개수 말고 **제대로 된 삽질기** 6개

> 3년 차에 필요한 건 더 많이 아는 것보다, 모르는 걸 **빨리 좁혀 가는 방법**인 것 같다.
', 'public', 565, '2026-01-04T20:00:00+09:00');
insert into tag (name) values ('회고') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '2025 회고: 3년 차가 되면서 달라진 것들'), (select tag_id from tag where name = '회고'));
insert into tag (name) values ('3년차') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '2025 회고: 3년 차가 되면서 달라진 것들'), (select tag_id from tag where name = '3년차'));
insert into tag (name) values ('백엔드') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '2025 회고: 3년 차가 되면서 달라진 것들'), (select tag_id from tag where name = '백엔드'));
insert into tag (name) values ('커리어') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '2025 회고: 3년 차가 되면서 달라진 것들'), (select tag_id from tag where name = '커리어'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')) and name = '스프링'), (select topic_id from topic where topic_name = '개발'), '커넥션 풀 고갈 장애 회고: HikariCP 로그를 따라간 길', '지난주 화요일 점심시간에 정산 조회 API가 30초씩 멈추다가 500을 뱉었다. 로그에 찍힌 건 이거였다.

```text
HikariPool-1 - Connection is not available, request timed out after 30000ms.
```

## 무슨 뜻인가

HikariCP 풀에서 커넥션을 빌리려고 했는데, **30초(`connectionTimeout` 기본값) 동안 남는 커넥션이 없었다**는 뜻이다. 풀 크기는 기본값 10 그대로였다.

DB가 느린 건지, 커넥션을 누가 오래 쥐고 있는 건지부터 나눠야 했다. DB 쪽 `pg_stat_activity`를 보니 우리 앱 커넥션 대부분이 `idle in transaction` 상태였다. **트랜잭션을 연 채로 아무것도 안 하고 있다**는 뜻.

## 원인

```java
@Transactional
public SettlementResponse confirm(Long settlementId) {
    Settlement settlement = settlementRepository.findById(settlementId).orElseThrow();
    settlement.confirm();

    // 외부 송금 API 호출 (여기서 몇 초씩 걸림)
    TransferResult result = transferClient.request(settlement.toTransferRequest());

    settlement.markTransferred(result.transactionId());
    return SettlementResponse.from(settlement);
}
```

트랜잭션 안에서 **외부 API를 호출**하고 있었다. 평소엔 200ms 정도라 문제가 없었는데, 그날 외부 API가 느려지면서 호출 하나가 5~10초씩 걸렸다. 그동안 커넥션을 쥐고 있으니 점심 트래픽에 10개가 금방 바닥났다.

## 대응

### 당일
외부 API 호출에 **타임아웃**이 아예 없었다. 급하게 연결/응답 타임아웃을 넣어 배포했다.

### 근본 수정
트랜잭션을 두 개로 쪼개고, 외부 호출은 트랜잭션 **밖**으로 뺐다.

```java
public SettlementResponse confirm(Long settlementId) {
    TransferRequest request = settlementTxService.confirm(settlementId);   // 트랜잭션 1
    TransferResult result = transferClient.request(request);               // 트랜잭션 밖
    return settlementTxService.markTransferred(settlementId, result);      // 트랜잭션 2
}
```

`settlementTxService`는 별도 빈이다. 같은 클래스에 두면 내부 호출이라 `@Transactional`이 안 먹는다는 건 1년 차 때 이미 배웠다.

중간에 실패하면 "확정됐지만 송금 안 된" 상태가 남으니까, 그 상태를 조회해서 재처리하는 배치를 같이 만들었다.

## 같이 켠 설정

```yaml
spring:
  datasource:
    hikari:
      leak-detection-threshold: 5000
  jpa:
    open-in-view: false
```

- `leak-detection-threshold`: 커넥션을 5초 넘게 쥐고 있으면 **빌려 간 위치의 스택 트레이스**를 경고로 남긴다. 다음엔 원인을 훨씬 빨리 찾을 수 있다.
- `open-in-view`: 켜져 있으면 영속성 컨텍스트가 요청 끝까지 살아 있어서, DB 커넥션을 처음 쓴 뒤 **응답이 나갈 때까지** 쥐고 있을 수 있다. 이번 원인은 아니었지만 같이 껐다. 대신 지연 로딩이 컨트롤러에서 터지는 곳 두 군데를 고쳐야 했다.

## 배운 것

- 풀 크기를 늘리는 건 답이 아니었다. 20으로 늘렸으면 20개가 다 묶였을 거다.
- 트랜잭션 안에는 **DB 일만** 둔다.
- 외부 호출에는 **반드시 타임아웃**.

> 30초 타임아웃 로그 한 줄 뒤에, 타임아웃 없는 외부 호출이 숨어 있었다.
', 'public', 168, '2026-09-27T21:45:00+09:00');
insert into tag (name) values ('hikaricp') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '커넥션 풀 고갈 장애 회고: HikariCP 로그를 따라간 길'), (select tag_id from tag where name = 'hikaricp'));
insert into tag (name) values ('커넥션풀') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '커넥션 풀 고갈 장애 회고: HikariCP 로그를 따라간 길'), (select tag_id from tag where name = '커넥션풀'));
insert into tag (name) values ('장애회고') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '커넥션 풀 고갈 장애 회고: HikariCP 로그를 따라간 길'), (select tag_id from tag where name = '장애회고'));
insert into tag (name) values ('springboot') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '커넥션 풀 고갈 장애 회고: HikariCP 로그를 따라간 길'), (select tag_id from tag where name = 'springboot'));

-- 골목필름 · 골목의 36컷
insert into blog (users_id, name, intro, comments_read_at) values ((select users_id from users where email = 'demo.film@example.com'), '골목의 36컷', '중고 수동 카메라로 시작한 필름 생활 기록. 카메라와 필름 이야기, 현상 맡긴 이야기, 골목과 바다를 걸으며 찍은 출사 기록을 남깁니다.', '2026-10-11T23:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), '미분류', 'public', true, 1, 0, '2023-03-25T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), '필름 이야기', 'public', false, 2, 1, '2023-03-25T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), '출사 기록', 'public', false, 3, 2, '2023-03-25T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), '카메라', 'public', false, 4, 3, '2023-03-25T10:00:00+09:00');
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')) and name = '카메라'), (select topic_id from topic where topic_name = '취미'), '첫 필름카메라, 중고 수동 SLR을 들이다', '휴대폰 사진이 지겨워졌다는 핑계로 결국 필름카메라를 샀어요. 동네 중고 거래로 **1970년대 수동 SLR 바디 + 50mm f/1.7 렌즈** 세트를 18만 원에 데려왔습니다.

## 사기 전에 확인한 것

판매하시는 분이 근처 사셔서 직접 만나서 하나씩 만져 봤어요. 인터넷에서 보고 적어 간 체크리스트는 이랬습니다.

- 셔터 속도별로 소리가 달라지는지 (1초, 1/15, 1/1000 세 개만 들어 봐도 감이 와요)
- 렌즈를 형광등에 비춰서 곰팡이, 헤이즈가 없는지
- 미러 쿠션(모르트)이 삭아서 끈적이지 않는지
- 노출계 바늘이 움직이는지 — 이건 배터리를 새로 넣어야 해서 LR44 두 알을 챙겨 갔어요

노출계가 살짝 반 스톱 정도 밝게 잡히는 것 같았는데, 그 정도는 감안하고 쓰기로 했어요.

## 첫 롤

첫 필름은 무난하다는 **컬러 네거티브 ISO 200** 36컷. 집 앞 골목, 출근길 버스 정류장, 주말 시장을 찍었는데 셔터를 누를 때마다 "이게 제대로 찍히긴 한 걸까" 싶어서 괜히 두근거렸어요.

> 찍고 나서 바로 확인할 수 없다는 게 이렇게 사람을 신중하게 만드는구나.

36컷을 다 쓰는 데 3주가 걸렸습니다. 휴대폰으로는 하루에 100장도 찍는데 말이에요. 현상 결과는 다음 글에 올릴게요. 핀이 나간 사진이 반쯤 될 것 같은 예감이 들긴 합니다.', 'public', 1268, '2023-04-08T21:40:00+09:00');
insert into tag (name) values ('필름카메라') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '첫 필름카메라, 중고 수동 SLR을 들이다'), (select tag_id from tag where name = '필름카메라'));
insert into tag (name) values ('중고카메라') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '첫 필름카메라, 중고 수동 SLR을 들이다'), (select tag_id from tag where name = '중고카메라'));
insert into tag (name) values ('입문') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '첫 필름카메라, 중고 수동 SLR을 들이다'), (select tag_id from tag where name = '입문'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')) and name = '출사 기록'), (select topic_id from topic where topic_name = '여행'), '가을 서촌 골목 출사 기록', '토요일 오후, 경복궁역 3번 출구에서 시작해서 서촌 골목을 천천히 걸었어요. 이번엔 **ISO 400 컬러 필름** 한 롤과 흑백 한 롤을 챙겼습니다.

## 걸은 길

1. 경복궁역 → 통인시장 쪽 골목 (점심은 시장에서 간단히)
2. 수성동 계곡까지 올라가는 오르막
3. 내려오면서 한옥 담벼락 골목
4. 해 질 무렵 다시 큰길 쪽으로

총 4시간쯤 걸었고, 걸음 수는 1만 4천 보 정도 찍혔어요.

## 노출 메모

가을 오후 햇빛이 생각보다 빨리 낮아져서 노출을 자주 바꿨어요.

- 오후 2시, 양지: **1/500, f/8**
- 골목 그늘: **1/125, f/4**
- 오후 5시 반, 역광 담벼락: **1/60, f/2.8** — 이건 좀 어둡게 나왔어요

수성동 계곡 쪽은 단풍이 막 들기 시작해서 노랑과 초록이 섞여 있었는데, 컬러 필름이 이런 중간색을 정말 부드럽게 담아 줘요. 디지털로 찍으면 채도가 확 올라가서 오히려 촌스러울 때가 있는데 필름은 한 톤 눌러 주는 느낌이랄까요.

## 아쉬운 점

한옥 처마 아래 고양이를 만났는데 초점 링 돌리는 사이에 가 버렸어요. 수동 초점은 아직 손이 느립니다. 다음엔 **존 포커싱**으로 3m에 미리 맞춰 놓고 다녀 보려고요.', 'public', 1261, '2023-10-21T19:15:00+09:00');
insert into tag (name) values ('서촌') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '가을 서촌 골목 출사 기록'), (select tag_id from tag where name = '서촌'));
insert into tag (name) values ('출사') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '가을 서촌 골목 출사 기록'), (select tag_id from tag where name = '출사'));
insert into tag (name) values ('포트라400') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '가을 서촌 골목 출사 기록'), (select tag_id from tag where name = '포트라400'));
insert into tag (name) values ('가을') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '가을 서촌 골목 출사 기록'), (select tag_id from tag where name = '가을'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')) and name = '필름 이야기'), (select topic_id from topic where topic_name = '취미'), '현상소에 필름 맡기는 법과 스캔 해상도 고르는 기준', '필름 시작하고 1년 가까이 지나니 주변에서 "현상은 어디서, 어떻게 맡겨?"라는 질문을 자주 받아요. 제가 지금까지 열 곳 넘게 맡겨 보며 정리한 기준을 적어 둡니다.

## 맡기는 방법

- **직접 방문**: 바로 맡기고 보통 2~3일 뒤 메일로 스캔본을 받아요.
- **우편 접수**: 필름 통을 지퍼백에 넣고 신청서 적어서 보내요. 왕복 일주일 정도 걸리지만 지방에 사는 친구들은 이 방법을 많이 써요.

필름 통에는 꼭 **매직으로 롤 번호**를 적어요. 여러 롤을 한 번에 맡기면 나중에 어느 날 찍은 건지 헷갈립니다.

## 스캔 해상도

| 용도 | 추천 |
|---|---|
| SNS, 블로그 | 일반 해상도 (긴 변 약 1800px 내외) |
| 작게 인화 (4x6) | 일반으로도 충분 |
| A4 이상 인화, 크롭 많이 할 때 | 고해상도 (긴 변 3000px 이상) |

처음엔 무조건 고해상도로 했는데, 한 롤에 몇천 원씩 차이가 나다 보니 지금은 **마음에 드는 롤만 고해상도**로 다시 스캔 요청해요. 원본 네거티브는 돌려받아서 보관하니까 나중에 언제든 재스캔이 가능하거든요.

## 자주 하는 실수

> 필름을 끝까지 안 감고 뒤뚜껑을 열었다.

저도 한 번 했어요. 마지막 열두 컷이 하얗게 날아갔습니다. 되감기 레버가 헐렁해질 때까지 돌렸는지 꼭 확인하세요.

그리고 **네거티브 슬리브**를 받으면 습기 없는 서랍에 세로로 꽂아 두는 게 좋아요. 앨범처럼 눕혀서 쌓아 두면 오래 지나면 서로 붙기도 한대요.', 'public', 903, '2024-03-16T22:05:00+09:00');
insert into tag (name) values ('현상') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '현상소에 필름 맡기는 법과 스캔 해상도 고르는 기준'), (select tag_id from tag where name = '현상'));
insert into tag (name) values ('스캔') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '현상소에 필름 맡기는 법과 스캔 해상도 고르는 기준'), (select tag_id from tag where name = '스캔'));
insert into tag (name) values ('필름입문') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '현상소에 필름 맡기는 법과 스캔 해상도 고르는 기준'), (select tag_id from tag where name = '필름입문'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')) and name = '필름 이야기'), (select topic_id from topic where topic_name = '취미'), '흑백 필름 1스톱 증감 현상 후기 (ISO 400 → 800)', '여름밤 골목 불빛을 찍고 싶어서 처음으로 **증감 현상(푸시)** 을 해 봤어요. ISO 400 흑백 필름을 **ISO 800으로 놓고 찍은 뒤**, 현상소에 "+1 푸시"로 맡기는 방식입니다.

## 왜 푸시를 했나

밤 골목은 가로등 하나에 의지하는 곳이 많아요. 50mm f/1.7 렌즈를 활짝 열어도 ISO 400이면 셔터가 1/15초 아래로 떨어져서 손떨림이 생기더라고요. ISO 800으로 올리면 **1/30초**는 확보돼요.

## 찍은 조건

- 장소: 을지로 뒷골목, 동네 시장 마감 직후
- 시간: 밤 9시~10시 반
- 노출: 대부분 **1/30, f/1.7**, 간판 불빛 아래는 1/60
- 36컷 중 건진 컷: 22컷

## 결과

> 입자가 굵어지고, 검정이 더 깊어졌다.

딱 이 말이 맞아요. 하늘이나 어두운 벽은 거의 새까맣게 떨어지고, 가로등이 닿은 부분만 하얗게 떠오르는 느낌이에요. 계조는 줄었지만 밤 분위기에는 오히려 잘 맞았습니다.

입자는 휴대폰 화면으로 볼 때는 "적당히 거칠다" 정도인데, 고해상도 스캔을 확대하면 꽤 자글자글해요. 크게 인화할 생각이면 처음부터 ISO 높은 필름을 쓰는 게 나을 것 같아요.

## 다음엔

- 삼각대 없이 **벽이나 전봇대에 몸을 기대고** 찍기 (흔들린 컷이 대부분 이거였어요)
- +2 푸시도 한 번 해 보고 싶은데, 그건 조금 더 밝은 장소에서 시험해 볼 생각입니다', 'public', 1194, '2024-08-03T23:20:00+09:00');
insert into tag (name) values ('흑백필름') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '흑백 필름 1스톱 증감 현상 후기 (ISO 400 → 800)'), (select tag_id from tag where name = '흑백필름'));
insert into tag (name) values ('증감현상') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '흑백 필름 1스톱 증감 현상 후기 (ISO 400 → 800)'), (select tag_id from tag where name = '증감현상'));
insert into tag (name) values ('야간출사') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '흑백 필름 1스톱 증감 현상 후기 (ISO 400 → 800)'), (select tag_id from tag where name = '야간출사'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')) and name = '출사 기록'), (select topic_id from topic where topic_name = '여행'), '군산 1박 2일 필름 여행, 세 롤 다 썼다', '2월 연휴에 군산을 다녀왔어요. 목표는 단 하나, **필름 세 롤 다 쓰고 오기**. 컬러 두 롤, 흑백 한 롤을 챙겼습니다.

## 1일차

용산에서 기차 타고 익산 거쳐 군산에 도착하니 점심 무렵이었어요. 숙소에 짐만 맡기고 바로 근대 거리 쪽으로 걸었습니다.

- 오래된 은행·세관 건물 같은 근대 건축물: 정면보다 **모서리 45도**에서 찍는 게 입체감이 좋았어요
- 적산가옥 골목: 겨울 햇빛이 낮게 들어와서 창살 그림자가 길게 늘어져 있었어요. 이날 베스트 컷.
- 저녁엔 동네 짬뽕집에서 한 그릇. 줄이 길어서 40분 기다렸어요.

## 2일차

아침 일찍 철길마을에 갔어요. 9시 전이라 사람이 거의 없어서 레일 위를 한참 걸으며 찍을 수 있었습니다. 낮은 시점이 재밌어서 **쪼그려 앉아서 레일 소실점**을 넣은 컷을 여러 장 남겼어요.

오후엔 바닷가 쪽으로 가서 흑백으로 갈매기와 방파제를 찍었는데, 바람이 너무 세서 손이 얼었어요. 셔터 버튼 누르는 감각이 둔해져서 몇 장은 의도치 않게 찍혔습니다.

## 여행 필름 팁

1. 필름은 **기내용 가방에** 넣고, 공항 검색대를 지날 일이 있으면 수검사를 요청해요 (이번엔 기차라 해당 없었지만요)
2. 겨울엔 카메라를 외투 안에 넣어 두면 배터리 방전도, 필름 부러짐도 줄어요
3. 롤 하나 끝날 때마다 메모 앱에 **장소, 시간, 날씨**를 적어 두기

세 롤 108컷 중 마음에 드는 건 15장 정도. 그래도 그 15장 덕분에 또 가고 싶어졌어요.', 'public', 612, '2025-02-22T20:30:00+09:00');
insert into tag (name) values ('군산') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '군산 1박 2일 필름 여행, 세 롤 다 썼다'), (select tag_id from tag where name = '군산'));
insert into tag (name) values ('필름여행') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '군산 1박 2일 필름 여행, 세 롤 다 썼다'), (select tag_id from tag where name = '필름여행'));
insert into tag (name) values ('근대골목') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '군산 1박 2일 필름 여행, 세 롤 다 썼다'), (select tag_id from tag where name = '근대골목'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')) and name = '필름 이야기'), (select topic_id from topic where topic_name = '취미'), '장마철 필름 보관, 결국 냉장고 한 칸을 비웠다', '필름값이 계속 오르다 보니 세일할 때 열 롤씩 사 두는 습관이 생겼어요. 그런데 장마가 오니까 걱정이 되더라고요. 우리 집 방 습도가 **75%** 를 찍었거든요.

## 필름이 싫어하는 것

- 높은 온도: 감도와 색이 틀어져요. 특히 컬러 필름이 예민해요.
- 습기: 곰팡이, 끈적임
- 급격한 온도 변화: 결로

## 내가 정한 보관 규칙

**미개봉 필름**은 냉장고 채소칸 옆 한 칸에 넣었어요. 가족에게 "여기는 필름 칸"이라고 선언하고 반찬통 침범 금지 스티커까지 붙였습니다.

- 필름 박스째로 **지퍼백 두 겹**
- 지퍼백 안에 실리카겔 두 봉지
- 박스에 산 날짜와 유통기한을 매직으로 크게

**쓰기 전날 밤**에 꺼내서 지퍼백을 닫은 채로 상온에 반나절 둬요. 바로 열면 차가운 필름 표면에 물방울이 맺히거든요.

## 찍은 뒤의 필름

찍고 나서 현상 맡기기 전까지도 오래 두지 않으려고 해요. 찍은 필름은 잠상이 조금씩 약해진다고 해서, 장마철엔 **2주 안에** 맡기는 걸 원칙으로 했어요.

## 카메라는?

카메라도 습기를 먹어요. 렌즈에 곰팡이 피면 수리비가 카메라값보다 비쌀 수도 있어서, 작은 **제습함**을 하나 샀어요. 습도계를 넣어 두고 **40~50%** 사이를 유지하고 있어요.

> 필름 시작하고 가장 많이 산 건 필름이 아니라 실리카겔이었다.

농담 같지만 진짜입니다.', 'public', 829, '2025-07-12T21:50:00+09:00');
insert into tag (name) values ('필름보관') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '장마철 필름 보관, 결국 냉장고 한 칸을 비웠다'), (select tag_id from tag where name = '필름보관'));
insert into tag (name) values ('장마') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '장마철 필름 보관, 결국 냉장고 한 칸을 비웠다'), (select tag_id from tag where name = '장마'));
insert into tag (name) values ('습기') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '장마철 필름 보관, 결국 냉장고 한 칸을 비웠다'), (select tag_id from tag where name = '습기'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')) and name = '카메라'), (select topic_id from topic where topic_name = '취미'), '하프카메라로 벚꽃 72컷 찍은 날', '올해 봄엔 **하프 프레임 카메라**를 하나 들였어요. 필름 한 컷을 반으로 나눠 찍는 방식이라 36컷 필름 한 롤로 **72컷**을 찍을 수 있어요. 필름값이 부담스러운 요즘 딱이죠.

## 써 보니

- 가볍고 작아요. 외투 주머니에 들어가요.
- 초점은 **존 포커스** (가까이, 중간, 멀리 세 단계 정도)라 빠르게 찍기 좋아요.
- 세로로 찍히는 게 기본이라, 가로로 들면 세로 사진이, 세로로 들면 가로 사진이 나와요. 처음엔 이게 계속 헷갈렸어요.

## 벚꽃 출사

동네 하천변 벚꽃길을 오후 4시쯤 걸었어요. 바람이 불 때마다 꽃잎이 흩날려서 **연속으로 두 컷씩** 찍었는데, 하프카메라는 스캔하면 두 컷이 나란히 붙어서 나오거든요. 그게 의도치 않게 **딥티크(두 장 짝 사진)** 처럼 보여서 재밌었어요.

마음에 든 조합:

1. 왼쪽: 벚꽃 가지 클로즈업 / 오른쪽: 그 아래 벤치에 앉은 사람 뒷모습
2. 왼쪽: 하천 물결 / 오른쪽: 물 위에 떨어진 꽃잎
3. 왼쪽: 자전거 바구니 / 오른쪽: 바구니 속 꽃잎

## 단점

한 컷이 작아서 **입자와 해상도는 확실히 손해**예요. 크게 인화할 사진은 아니고, 일기처럼 남기는 용도로 생각하는 게 맞아요. 그리고 72컷은 생각보다 많아서 한 롤 다 쓰는 데 한 달이 걸렸습니다. 벚꽃으로 시작해서 철쭉으로 끝난 롤이 됐어요.', 'public', 365, '2026-03-28T20:10:00+09:00');
insert into tag (name) values ('하프카메라') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '하프카메라로 벚꽃 72컷 찍은 날'), (select tag_id from tag where name = '하프카메라'));
insert into tag (name) values ('벚꽃') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '하프카메라로 벚꽃 72컷 찍은 날'), (select tag_id from tag where name = '벚꽃'));
insert into tag (name) values ('봄출사') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '하프카메라로 벚꽃 72컷 찍은 날'), (select tag_id from tag where name = '봄출사'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')) and name = '필름 이야기'), (select topic_id from topic where topic_name = '취미'), '노출계 없이 ''써니 16'' 한 달 연습 기록', '노출계가 아예 없는 기계식 카메라를 빌려 쓰게 되면서, 한 달 동안 **써니 16 법칙**만으로 찍어 봤어요.

## 써니 16 법칙이란

> 맑은 날 햇빛 아래에서는 조리개 f/16, 셔터 속도는 1/ISO.

ISO 200 필름이면 **f/16, 1/250초**(가장 가까운 값)가 기본이에요. 여기서 날씨에 따라 조리개를 열어 줍니다.

| 상황 | 조리개 (셔터 1/250 고정) |
|---|---|
| 쨍한 맑은 날, 그림자 선명 | f/16 |
| 살짝 구름, 그림자 흐릿 | f/11 |
| 흐린 날, 그림자 거의 없음 | f/8 |
| 아주 흐림 / 그늘 | f/5.6 |
| 해 질 녘, 실내 창가 | f/4 이하 |

## 한 달 동안 한 연습

1. 출근길마다 휴대폰 노출계 앱 없이 **먼저 내가 값을 추측**해요
2. 그다음 앱으로 측정해서 차이를 메모해요
3. 차이가 1스톱 이내면 성공으로 쳤어요

첫 주 성공률은 50%였는데 넷째 주엔 **85%** 까지 올라갔어요. 제일 많이 틀린 건 **그늘진 골목**이에요. 눈은 금방 적응해서 밝다고 느끼는데, 실제로는 맑은 날 양지보다 2~3스톱이나 어두웠어요.

## 결과물

네거티브 필름은 관용도가 넓어서 1스톱 정도 과다 노출은 거의 티가 안 났어요. 오히려 **부족 노출**이 티가 확 나요. 그래서 애매하면 한 칸 더 여는 쪽으로 습관을 들였습니다.

이제는 노출계 있는 카메라를 들어도 먼저 하늘과 그림자를 한번 보게 돼요. 그게 이번 한 달의 가장 큰 수확이에요.', 'public', 179, '2026-08-23T22:30:00+09:00');
insert into tag (name) values ('써니16') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '노출계 없이 ''써니 16'' 한 달 연습 기록'), (select tag_id from tag where name = '써니16'));
insert into tag (name) values ('노출') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '노출계 없이 ''써니 16'' 한 달 연습 기록'), (select tag_id from tag where name = '노출'));
insert into tag (name) values ('수동카메라') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '노출계 없이 ''써니 16'' 한 달 연습 기록'), (select tag_id from tag where name = '수동카메라'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')) and name = '출사 기록'), (select topic_id from topic where topic_name = '여행'), '부산 영도 흰여울마을, 바다 냄새 나는 필름 산책', '추석 연휴 끝자락에 부산 영도를 다녀왔어요. 이번 카메라는 늘 쓰던 50mm SLR, 필름은 **ISO 400 컬러 두 롤**.

## 흰여울마을

바다를 따라 절벽 위에 집들이 줄지어 있는 마을이에요. 좁은 골목 계단을 오르내리다 보면 갑자기 바다가 확 열리는 지점이 있는데, 그 순간이 너무 좋아서 같은 자리에서만 여섯 컷을 찍었어요.

- 오전 10시: 바다가 너무 밝아서 **1/1000, f/8**까지 조였어요
- 골목 안쪽 하얀 벽: 반사광이 강해서 노출계가 어둡게 잡으려고 해요. **반 스톱 더 열어서** 찍었어요
- 바닷가 산책로로 내려가는 계단: 수평선이 기울지 않게 몇 번이나 다시 맞췄어요

## 작은 에피소드

계단에 앉아 필름을 갈아 끼우고 있는데, 지나가던 할머니가 "요즘도 그런 걸 쓰나" 하시면서 옛날 사진관 이야기를 한참 해 주셨어요. 허락받고 뒷모습을 한 컷 남겼습니다. 이번 여행에서 제일 아끼는 사진이 될 것 같아요.

## 해 질 무렵

오후 5시 반쯤 해안 산책로에서 노을을 기다렸어요. 바다 위 하늘이 분홍에서 보라로 바뀌는 동안 **1/125, f/2.8**에서 시작해 1/30까지 내려가며 찍었는데, 마지막 몇 컷은 흔들렸을 것 같아요.

현상은 이번 주에 맡겼고, 스캔본 받으면 고른 사진들로 다시 올릴게요. 3년째 필름을 찍고 있지만 결과를 기다리는 이 며칠은 여전히 설레요.', 'public', 192, '2026-10-04T21:00:00+09:00');
insert into tag (name) values ('부산') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '부산 영도 흰여울마을, 바다 냄새 나는 필름 산책'), (select tag_id from tag where name = '부산'));
insert into tag (name) values ('영도') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '부산 영도 흰여울마을, 바다 냄새 나는 필름 산책'), (select tag_id from tag where name = '영도'));
insert into tag (name) values ('흰여울마을') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '부산 영도 흰여울마을, 바다 냄새 나는 필름 산책'), (select tag_id from tag where name = '흰여울마을'));
insert into tag (name) values ('필름여행') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '부산 영도 흰여울마을, 바다 냄새 나는 필름 산책'), (select tag_id from tag where name = '필름여행'));

-- 프론트노트 · 프론트 노트
insert into blog (users_id, name, intro, comments_read_at) values ((select users_id from users where email = 'demo.frontnote@example.com'), '프론트 노트', '프론트엔드 개발하면서 배운 리액트, CSS, 접근성 이야기와 주말에 조금씩 만드는 사이드 프로젝트 기록. 가끔 일상도 적어요.', '2026-10-11T23:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), '미분류', 'public', true, 1, 0, '2023-06-11T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), '리액트', 'public', false, 2, 1, '2023-06-11T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), 'CSS와 접근성', 'public', false, 3, 2, '2023-06-11T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), '사이드 프로젝트', 'public', false, 4, 3, '2023-06-11T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), '일상', 'public', false, 5, 4, '2023-06-11T10:00:00+09:00');
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')) and name = '리액트'), (select topic_id from topic where topic_name = '개발'), 'useEffect 의존성 배열 때문에 API를 무한으로 부른 삽질기', '입사하고 처음 맡은 화면에서 목록 페이지를 띄웠더니 **네트워크 탭이 폭포처럼** 흘러내렸어요. 같은 API가 1초에 수십 번 불리고 있었습니다. 백엔드 분이 조용히 메시지를 주셨던 날이에요.

## 문제의 코드

```tsx
function PostList({ category }: { category: string }) {
  const [page, setPage] = useState(1);
  const [posts, setPosts] = useState<Post[]>([]);

  const params = { category, page };

  useEffect(() => {
    fetchPosts(params).then(setPosts);
  }, [params]);

  // ...
}
```

## 왜 무한으로 도나

1. 렌더링할 때마다 `params`는 **새 객체**로 만들어져요.
2. `useEffect`는 의존성을 `Object.is`로 비교해요. 내용이 같아도 객체가 다르면 바뀐 걸로 봐요.
3. 이펙트 실행 → `setPosts` → 다시 렌더링 → 새 `params` → 이펙트 실행 → ...

결국 응답이 올 때마다 다음 요청이 나가는 무한 반복이었어요.

## 고친 코드

의존성에는 **원시값**을 넣고, 객체는 이펙트 안에서 만들었어요.

```tsx
useEffect(() => {
  const controller = new AbortController();

  fetchPosts({ category, page }, { signal: controller.signal })
    .then(setPosts)
    .catch((error) => {
      if (error.name !== ''AbortError'') throw error;
    });

  return () => controller.abort();
}, [category, page]);
```

`AbortController`는 덤이에요. 카테고리를 빠르게 바꿀 때 **이전 요청의 응답이 늦게 도착해서** 화면을 덮어쓰는 문제가 있었는데, 정리 함수에서 이전 요청을 취소하니까 같이 해결됐어요.

## 같이 정리한 것

- 린트의 `react-hooks/exhaustive-deps` 경고는 끄지 말기. 경고가 귀찮게 느껴지면 보통 구조가 이상한 거였어요.
- 객체를 꼭 의존성에 넣어야 하면 `useMemo`로 감싸는 방법도 있어요. 다만 이번처럼 이펙트 안으로 옮기는 게 더 단순했어요.
- 개발 모드의 Strict Mode에서는 이펙트가 **두 번** 실행돼요. 처음엔 이것 때문에 요청이 두 번 나가는 줄 알고 헷갈렸어요.

> 의존성 배열은 "언제 실행할지 정하는 곳"이 아니라 "이펙트가 **무엇을 읽는지** 적는 곳"이라는 말이 이제야 이해됐어요.
', 'public', 894, '2023-06-18T21:00:00+09:00');
insert into tag (name) values ('react') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'useEffect 의존성 배열 때문에 API를 무한으로 부른 삽질기'), (select tag_id from tag where name = 'react'));
insert into tag (name) values ('useeffect') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'useEffect 의존성 배열 때문에 API를 무한으로 부른 삽질기'), (select tag_id from tag where name = 'useeffect'));
insert into tag (name) values ('삽질기') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'useEffect 의존성 배열 때문에 API를 무한으로 부른 삽질기'), (select tag_id from tag where name = '삽질기'));
insert into tag (name) values ('hooks') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'useEffect 의존성 배열 때문에 API를 무한으로 부른 삽질기'), (select tag_id from tag where name = 'hooks'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')) and name = 'CSS와 접근성'), (select topic_id from topic where topic_name = '개발'), 'CSS Grid 카드 목록, auto-fill이랑 auto-fit 차이 이제 안 헷갈리기', '카드 목록 반응형을 만들 때마다 미디어 쿼리를 3~4개씩 쓰고 있었는데, Grid 한 줄로 거의 끝난다는 걸 알고 나서 갈아탔어요. 그런데 `auto-fill`이랑 `auto-fit`이 매번 헷갈려서 정리해 둡니다.

## 기본 모양

```css
.card-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 16px;
}
```

- `minmax(240px, 1fr)`: 카드는 **최소 240px**, 남는 공간이 있으면 균등하게 늘어나요.
- `repeat(auto-fill, ...)`: 컨테이너 너비에 **들어갈 수 있는 만큼** 열을 만들어요.

화면이 넓어지면 열이 늘고, 좁아지면 줄어요. 미디어 쿼리 없이요.

## auto-fill vs auto-fit

차이는 **아이템이 열 수보다 적을 때**만 보여요.

예를 들어 컨테이너에 열이 5개 들어갈 수 있는데 카드가 2장뿐이라면:

- `auto-fill`: 빈 열 3개를 **그대로 남겨 둬요**. 카드 2장은 240px 언저리 크기로 왼쪽에 붙어 있어요.
- `auto-fit`: 빈 열을 **0으로 접어요**. 남은 공간을 카드 2장이 나눠 가져서 엄청 넓어져요.

| 상황 | 추천 |
|---|---|
| 검색 결과처럼 개수가 들쭉날쭉한 목록 | `auto-fill` (카드 크기가 일정해서 덜 어색해요) |
| 항목 수가 거의 고정된 소개 섹션 | `auto-fit` (꽉 차게 늘어나요) |

## 좁은 화면에서 넘칠 때

컨테이너가 240px보다 좁아지면 카드가 삐져나가서 가로 스크롤이 생겨요. 이렇게 막았어요.

```css
.card-list {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(min(240px, 100%), 1fr));
  gap: 16px;
}
```

`min(240px, 100%)`라서 컨테이너가 좁으면 최소 너비가 컨테이너 너비로 줄어들어요.

## 카드 높이 맞추기

Grid는 같은 행의 아이템 높이를 기본으로 맞춰 줘요(`align-items: stretch`). 카드 안에서 버튼을 바닥에 붙이고 싶으면 카드에 `display: flex; flex-direction: column;`을 주고 버튼에 `margin-top: auto`를 줘요.

> 미디어 쿼리 4개가 한 줄이 됐을 때의 쾌감이란...
', 'public', 1379, '2023-11-12T15:30:00+09:00');
insert into tag (name) values ('css') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'CSS Grid 카드 목록, auto-fill이랑 auto-fit 차이 이제 안 헷갈리기'), (select tag_id from tag where name = 'css'));
insert into tag (name) values ('grid') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'CSS Grid 카드 목록, auto-fill이랑 auto-fit 차이 이제 안 헷갈리기'), (select tag_id from tag where name = 'grid'));
insert into tag (name) values ('반응형') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'CSS Grid 카드 목록, auto-fill이랑 auto-fit 차이 이제 안 헷갈리기'), (select tag_id from tag where name = '반응형'));
insert into tag (name) values ('레이아웃') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'CSS Grid 카드 목록, auto-fill이랑 auto-fit 차이 이제 안 헷갈리기'), (select tag_id from tag where name = '레이아웃'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')) and name = 'CSS와 접근성'), (select topic_id from topic where topic_name = '개발'), '버튼 대신 div에 onClick 달면 생기는 일', '사내 접근성 점검을 받았는데 제가 만든 화면에서 지적이 제일 많이 나왔어요. 그중 절반이 **`div`에 `onClick`** 단 것들이었습니다. 마우스로는 잘 되니까 문제라고 생각을 못 했어요.

## 뭐가 문제였나

```tsx
<div className="like-button" onClick={toggleLike}>
  ♥ 좋아요
</div>
```

1. **키보드로 갈 수 없어요.** `Tab`을 눌러도 포커스가 안 가요.
2. 포커스를 줘도 **`Enter`나 `Space`로 눌리지 않아요.**
3. 스크린리더가 이게 **버튼인지 몰라요.** 그냥 글자 "좋아요"로 읽어요.

점검하신 분이 키보드만으로 화면을 써 보는 걸 옆에서 봤는데, 좋아요를 누를 방법이 아예 없었어요.

## 제일 쉬운 해결: 그냥 button

```tsx
<button type="button" className="like-button" onClick={toggleLike} aria-pressed={liked}>
  좋아요
</button>
```

`button`을 쓰면 포커스, `Enter`/`Space` 처리, 역할 안내가 **전부 공짜**예요.

- `type="button"`은 꼭 적어요. `form` 안에 있으면 기본값이 `submit`이라 의도치 않게 폼이 제출돼요.
- 켜짐/꺼짐 상태가 있는 버튼은 `aria-pressed`로 상태를 알려 줘요.
- 하트 아이콘만 있는 버튼이면 `aria-label="좋아요"`처럼 이름을 꼭 붙여요.

## 스타일 때문에 div를 썼던 거라면

버튼 기본 스타일이 싫어서 `div`를 썼던 거였어요. 초기화하면 돼요.

```css
.reset-button {
  appearance: none;
  background: none;
  border: 0;
  padding: 0;
  font: inherit;
  color: inherit;
  cursor: pointer;
}

.reset-button:focus-visible {
  outline: 2px solid currentColor;
  outline-offset: 2px;
}
```

`outline: none`만 하고 끝내면 키보드 사용자는 **어디에 포커스가 있는지** 알 수 없어요. `:focus-visible`로 키보드 포커스일 때만 보이게 하면 마우스 클릭 때는 안 거슬려요.

## 정말 div여야 한다면

`role="button"`, `tabIndex={0}`, 키 이벤트까지 직접 다 해야 해요. 해 보면 왜 `button`을 쓰라는지 알게 돼요.

> 마우스를 내려놓고 `Tab`만으로 내 화면을 한 번 써 보세요. 생각보다 많이 막혀요.
', 'public', 504, '2024-03-03T22:20:00+09:00');
insert into tag (name) values ('접근성') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '버튼 대신 div에 onClick 달면 생기는 일'), (select tag_id from tag where name = '접근성'));
insert into tag (name) values ('a11y') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '버튼 대신 div에 onClick 달면 생기는 일'), (select tag_id from tag where name = 'a11y'));
insert into tag (name) values ('button') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '버튼 대신 div에 onClick 달면 생기는 일'), (select tag_id from tag where name = 'button'));
insert into tag (name) values ('키보드') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '버튼 대신 div에 onClick 달면 생기는 일'), (select tag_id from tag where name = '키보드'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')) and name = '사이드 프로젝트'), (select topic_id from topic where topic_name = '개발'), '사이드 프로젝트: 동네 빵집 지도 만들기 1편, 기획부터 첫 배포까지', '주말마다 동네 빵집을 돌아다니는 게 취미인데, 메모 앱에 적어 둔 빵집 목록이 50곳이 넘어가면서 찾기가 힘들어졌어요. 그래서 **내가 쓰려고** 지도 페이지를 만들기 시작했어요.

## 정한 범위

욕심내면 끝을 못 낼 것 같아서 1차 범위를 작게 잡았어요.

- 지도에 빵집 핀 찍기
- 핀 누르면 이름, 대표 메뉴, 쉬는 날, 한 줄 메모
- "오늘 문 연 곳만" 필터
- 로그인, 댓글, 리뷰 같은 건 **안 함**

데이터는 JSON 파일 하나로 시작했어요. 서버는 나중에 필요해지면요.

## 고른 도구

| 용도 | 고른 것 | 이유 |
|---|---|---|
| 빌드 | Vite + React + TypeScript | 회사에서 쓰는 거라 익숙해서 |
| 지도 | Leaflet + OpenStreetMap 타일 | 키 발급 없이 바로 시작 가능 |
| 배포 | 정적 호스팅 | 서버가 없으니까 |

## 데이터 모양

```ts
type Weekday = ''mon'' | ''tue'' | ''wed'' | ''thu'' | ''fri'' | ''sat'' | ''sun'';

interface Bakery {
  id: string;
  name: string;
  lat: number;
  lng: number;
  signature: string[];
  closedOn: Weekday[];
  memo?: string;
}
```

## "오늘 문 연 곳" 필터

```ts
const WEEKDAYS: Weekday[] = [''sun'', ''mon'', ''tue'', ''wed'', ''thu'', ''fri'', ''sat''];

function isOpenToday(bakery: Bakery, today = new Date()): boolean {
  const weekday = WEEKDAYS[today.getDay()];
  return !bakery.closedOn.includes(weekday);
}
```

`getDay()`는 일요일이 0이라서 배열을 일요일부터 시작했어요. 처음에 월요일부터 적었다가 하루씩 밀려서, 월요일 휴무인 집이 화요일에 닫혀 있다고 나왔어요.

## 해 보니 어려웠던 것

- 핀이 30개 넘으니까 겹쳐서 누르기 힘들었어요. 다음 편에서 묶어 보여 주는 방법을 찾아볼 거예요.
- 모바일에서 지도 스크롤이랑 페이지 스크롤이 싸워요. 지도 높이를 화면의 60%로 고정하고 아래에 목록을 뒀어요.
- 위도/경도는 손으로 찾아 넣는 게 제일 오래 걸렸어요...

> 남이 쓸 서비스가 아니라 **내가 매주 쓸 도구**라서 그런지, 오랜만에 사이드 프로젝트가 재밌어요.
', 'public', 411, '2024-08-25T20:10:00+09:00');
insert into tag (name) values ('사이드프로젝트') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '사이드 프로젝트: 동네 빵집 지도 만들기 1편, 기획부터 첫 배포까지'), (select tag_id from tag where name = '사이드프로젝트'));
insert into tag (name) values ('react') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '사이드 프로젝트: 동네 빵집 지도 만들기 1편, 기획부터 첫 배포까지'), (select tag_id from tag where name = 'react'));
insert into tag (name) values ('leaflet') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '사이드 프로젝트: 동네 빵집 지도 만들기 1편, 기획부터 첫 배포까지'), (select tag_id from tag where name = 'leaflet'));
insert into tag (name) values ('vite') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '사이드 프로젝트: 동네 빵집 지도 만들기 1편, 기획부터 첫 배포까지'), (select tag_id from tag where name = 'vite'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')) and name = '리액트'), (select topic_id from topic where topic_name = '개발'), 'TypeScript 유니온 타입으로 로딩·에러 상태 정리하기', '예전 코드를 보면 이런 상태가 꼭 있었어요.

```ts
const [isLoading, setIsLoading] = useState(false);
const [error, setError] = useState<Error | null>(null);
const [data, setData] = useState<User | null>(null);
```

불리언 세 개를 따로 관리하다 보니 **로딩 중인데 에러도 있는** 이상한 조합이 생겼어요. 재시도할 때 `setError(null)`을 깜빡해서, 데이터가 잘 왔는데 에러 메시지가 같이 떠 있던 버그도 있었고요.

## 상태를 하나로 묶기

```ts
type AsyncState<T> =
  | { status: ''idle'' }
  | { status: ''loading'' }
  | { status: ''success''; data: T }
  | { status: ''error''; error: Error };
```

이렇게 하면 `data`는 **`success`일 때만**, `error`는 **`error`일 때만** 존재해요. 말이 안 되는 조합은 타입에서 아예 만들 수가 없어요.

## 화면에서 쓰기

```tsx
function UserCard({ state }: { state: AsyncState<User> }) {
  switch (state.status) {
    case ''idle'':
      return null;
    case ''loading'':
      return <p>불러오는 중...</p>;
    case ''error'':
      return <p role="alert">{state.error.message}</p>;
    case ''success'':
      return <p>{state.data.nickname}</p>;
    default: {
      const unreachable: never = state;
      return unreachable;
    }
  }
}
```

`case ''success''` 안에서는 TypeScript가 `state`를 알아서 좁혀 줘서 `state.data`에 바로 접근할 수 있어요.

마지막 `default`의 `never`가 핵심이에요. 나중에 누가 `{ status: ''empty'' }`를 추가하고 `case`를 안 만들면, `state`가 `never`가 아니게 되니까 **컴파일 에러**가 나요. 처리를 빠뜨릴 수가 없어요.

## 상태 바꾸는 쪽

```ts
const [state, setState] = useState<AsyncState<User>>({ status: ''idle'' });

async function load(id: string) {
  setState({ status: ''loading'' });
  try {
    const user = await fetchUser(id);
    setState({ status: ''success'', data: user });
  } catch (e) {
    setState({ status: ''error'', error: e instanceof Error ? e : new Error(String(e)) });
  }
}
```

`catch`의 `e`는 `unknown`이라서 `Error`인지 확인하고 넣었어요.

## 그 뒤로

서버 상태는 지금은 데이터 패칭 라이브러리에 맡기는 편이에요. 그래도 폼 제출 상태나 업로드 진행 상태처럼 **직접 관리하는 상태**에는 이 패턴을 계속 써요.

> 불리언이 두 개 넘어가면 유니온 타입을 의심해 보기.
', 'public', 476, '2025-01-19T21:40:00+09:00');
insert into tag (name) values ('typescript') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'TypeScript 유니온 타입으로 로딩·에러 상태 정리하기'), (select tag_id from tag where name = 'typescript'));
insert into tag (name) values ('react') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'TypeScript 유니온 타입으로 로딩·에러 상태 정리하기'), (select tag_id from tag where name = 'react'));
insert into tag (name) values ('상태관리') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'TypeScript 유니온 타입으로 로딩·에러 상태 정리하기'), (select tag_id from tag where name = '상태관리'));
insert into tag (name) values ('유니온타입') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'TypeScript 유니온 타입으로 로딩·에러 상태 정리하기'), (select tag_id from tag where name = '유니온타입'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')) and name = 'CSS와 접근성'), (select topic_id from topic where topic_name = '개발'), '다크 모드, CSS 변수로 한 번에 정리한 방법', '사이드 프로젝트 빵집 지도를 밤에 보다가 눈이 너무 부셔서 다크 모드를 넣었어요. 처음엔 컴포넌트마다 `.dark .card { ... }` 를 붙이다가 금방 감당이 안 돼서, **CSS 변수 한곳**으로 정리했어요.

## 1. 색을 역할 이름으로 정의

색 이름(`--gray-100`)이 아니라 **역할 이름**으로 정했어요.

```css
:root {
  --color-bg: #ffffff;
  --color-surface: #f5f5f4;
  --color-text: #1c1917;
  --color-text-muted: #57534e;
  --color-border: #e7e5e4;
  color-scheme: light;
}
```

컴포넌트는 이 변수만 써요.

```css
.card {
  background: var(--color-surface);
  color: var(--color-text);
  border: 1px solid var(--color-border);
}
```

## 2. 시스템 설정 따라가기

```css
@media (prefers-color-scheme: dark) {
  :root:not([data-theme=''light'']) {
    --color-bg: #1c1917;
    --color-surface: #292524;
    --color-text: #f5f5f4;
    --color-text-muted: #a8a29e;
    --color-border: #44403c;
    color-scheme: dark;
  }
}
```

`color-scheme: dark`를 꼭 같이 줘요. 이걸 빼면 **스크롤바, 폼 입력창, 체크박스**가 여전히 밝은 색으로 나와서 어색해요.

## 3. 사용자가 직접 고르기

시스템은 라이트인데 이 사이트만 어둡게 보고 싶은 사람도 있어서 토글을 만들었어요.

```css
:root[data-theme=''dark''] {
  --color-bg: #1c1917;
  --color-surface: #292524;
  --color-text: #f5f5f4;
  --color-text-muted: #a8a29e;
  --color-border: #44403c;
  color-scheme: dark;
}
```

```ts
function setTheme(theme: ''light'' | ''dark'' | ''system'') {
  const root = document.documentElement;
  if (theme === ''system'') {
    root.removeAttribute(''data-theme'');
  } else {
    root.setAttribute(''data-theme'', theme);
  }
}
```

다크 값을 두 번 적는 게 좀 아쉽지만, 구조가 단순해서 그냥 두었어요.

## 해 보니 챙길 것

- **완전한 검정(#000)** 은 오히려 눈이 피곤했어요. 살짝 따뜻한 짙은 회색이 편해요.
- 다크에서는 글자와 배경의 **대비**를 다시 확인해야 해요. 라이트에서 괜찮던 보조 글자색이 다크에서 안 읽혔어요.
- 지도 타일은 CSS 변수로 안 바뀌어서, 다크 모드일 때만 타일에 약간 어둡게 필터를 줬어요.

> 컴포넌트가 색을 직접 알지 않게 하는 것. 그게 다크 모드의 전부였어요.
', 'public', 654, '2025-06-08T14:00:00+09:00');
insert into tag (name) values ('css') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '다크 모드, CSS 변수로 한 번에 정리한 방법'), (select tag_id from tag where name = 'css'));
insert into tag (name) values ('다크모드') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '다크 모드, CSS 변수로 한 번에 정리한 방법'), (select tag_id from tag where name = '다크모드'));
insert into tag (name) values ('css변수') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '다크 모드, CSS 변수로 한 번에 정리한 방법'), (select tag_id from tag where name = 'css변수'));
insert into tag (name) values ('디자인토큰') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '다크 모드, CSS 변수로 한 번에 정리한 방법'), (select tag_id from tag where name = '디자인토큰'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')) and name = 'CSS와 접근성'), (select topic_id from topic where topic_name = '개발'), '모달 접근성 체크리스트: 포커스, Esc, 그리고 dialog 요소', '회사 디자인 시스템의 모달을 다시 만드는 일을 맡았어요. 예전 모달은 `div`를 화면 위에 띄우는 방식이었는데, 키보드로 써 보니 **모달 뒤의 버튼으로 포커스가 넘어가는** 문제가 있었어요. 다시 만들면서 확인한 것들을 체크리스트로 정리해요.

## 체크리스트

- [ ] 열리면 포커스가 **모달 안**으로 들어간다
- [ ] `Tab`으로 돌아도 **모달 밖으로 나가지 않는다**
- [ ] `Esc`로 닫힌다
- [ ] 닫히면 포커스가 **연 버튼**으로 돌아간다
- [ ] 스크린리더가 모달의 **제목**을 읽는다
- [ ] 모달 뒤의 내용은 스크린리더가 읽지 않는다

예전 모달은 이 중 하나만 됐어요(Esc).

## dialog 요소로 바꾸기

요즘 브라우저는 `<dialog>`와 `showModal()`을 지원해요. 이걸로 위 목록의 상당 부분을 브라우저가 해 줘요.

```tsx
function ConfirmDialog({ open, title, onClose, children }: Props) {
  const ref = useRef<HTMLDialogElement>(null);
  const openerRef = useRef<HTMLElement | null>(null);

  useEffect(() => {
    const dialog = ref.current;
    if (!dialog) return;
    if (open && !dialog.open) {
      openerRef.current = document.activeElement as HTMLElement | null;
      dialog.showModal();
    } else if (!open && dialog.open) {
      dialog.close();
    }
  }, [open]);

  return (
    <dialog
      ref={ref}
      aria-labelledby="confirm-dialog-title"
      onClose={() => {
        onClose();
        openerRef.current?.focus();
      }}
    >
      <h2 id="confirm-dialog-title">{title}</h2>
      {children}
    </dialog>
  );
}
```

`showModal()`로 열면:

- 모달 바깥이 **inert**가 돼서 클릭도, 포커스도, 스크린리더 탐색도 안 돼요.
- `Esc`를 누르면 `cancel` 이벤트가 나고 닫혀요. 닫힐 때 `close` 이벤트가 오니까 React 상태는 `onClose`에서 맞춰요.
- 화면 맨 위 레이어에 그려져서 `z-index` 싸움이 없어져요. 배경은 `::backdrop`으로 꾸며요.

## 직접 챙긴 것

- **포커스 복귀**: 브라우저가 해 주는 경우도 있지만, 확실하게 하려고 연 버튼을 기억해 뒀다가 직접 돌려줬어요.
- **처음 포커스 위치**: 삭제 확인 모달은 `취소` 버튼에 `autoFocus`를 줘요. 실수로 `Enter` 눌러서 지우는 걸 막으려고요.
- **제목 연결**: `aria-labelledby`로 제목을 이어 줘야 "대화 상자, 글 삭제" 처럼 읽혀요.
- **스크롤 잠금**: 모달 뒤 페이지가 스크롤되는 건 따로 막아야 했어요.

```css
dialog::backdrop {
  background: rgb(0 0 0 / 0.4);
}

body:has(dialog[open]) {
  overflow: hidden;
}
```

> 모달은 "띄우는 것"보다 "**닫히고 난 뒤 포커스가 어디 있는지**"가 더 중요했어요.
', 'public', 515, '2025-11-30T21:15:00+09:00');
insert into tag (name) values ('접근성') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '모달 접근성 체크리스트: 포커스, Esc, 그리고 dialog 요소'), (select tag_id from tag where name = '접근성'));
insert into tag (name) values ('모달') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '모달 접근성 체크리스트: 포커스, Esc, 그리고 dialog 요소'), (select tag_id from tag where name = '모달'));
insert into tag (name) values ('dialog') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '모달 접근성 체크리스트: 포커스, Esc, 그리고 dialog 요소'), (select tag_id from tag where name = 'dialog'));
insert into tag (name) values ('a11y') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '모달 접근성 체크리스트: 포커스, Esc, 그리고 dialog 요소'), (select tag_id from tag where name = 'a11y'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')) and name = '일상'), (select topic_id from topic where topic_name = '여행'), '제주 워케이션 일주일, 일하고 걷고 귤 먹은 기록', '회사에 원격 근무 제도가 생겨서 처음으로 **일주일 워케이션**을 다녀왔어요. 제주 동쪽 바닷가 마을에 숙소를 잡았어요. 개발 글은 아니지만 기록해 두고 싶어서 일상 카테고리에 남겨요.

## 준비한 것

- 노트북, 충전기, **휴대용 모니터** (이거 가져간 게 신의 한 수)
- 숙소 고를 때 와이파이 속도 미리 물어보기. 화상 회의가 있어서요.
- 핫스팟 데이터 여유분. 실제로 하루 와이파이가 끊겨서 썼어요.

## 하루 루틴

| 시간 | 한 것 |
|---|---|
| 7:30 | 해안 산책 40분 |
| 9:00 ~ 12:00 | 집중 업무 (회의 없는 시간) |
| 12:00 ~ 13:30 | 동네 식당 점심 |
| 13:30 ~ 18:00 | 회의, 코드 리뷰 |
| 18:00 이후 | 노을 보러 나가기, 저녁 |

평소보다 출퇴근 시간이 없으니까 아침 산책이 가능했어요. 오전 집중 시간에 작업이 진짜 잘 됐어요. 창밖에 바다가 보이는 게 생각보다 효과가 커요.

## 좋았던 것

- 점심 먹고 10분 걸으면 바다가 나와요. 오후 회의 전에 머리 식히기 딱 좋았어요.
- 동네 하나로 반경을 좁혔더니 **관광하느라 지치는 일**이 없었어요.
- 오일장 구경. 귤 한 봉지 사서 일주일 내내 책상 옆에 두고 먹었어요.

## 아쉬웠던 것

- 의자. 숙소 식탁 의자에서 하루 8시간은 허리가 버티질 못해요. 결국 쿠션을 하나 샀어요.
- 일과 휴가의 경계가 흐려져서, 수요일엔 밤 10시까지 일하고 있었어요.
- 날씨. 바람 부는 날엔 산책이 거의 극기 훈련이었어요.

## 다음에 간다면

- 일주일보다는 **열흘**. 첫날 적응, 마지막 날 짐 싸기를 빼면 실제로는 5일이었어요.
- 금요일 오후는 연차 반차 내고 제대로 놀기.

> 장소가 바뀐다고 일이 줄진 않지만, 일 사이사이의 **10분**이 완전히 달라졌어요.
', 'public', 385, '2026-05-17T16:20:00+09:00');
insert into tag (name) values ('워케이션') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '제주 워케이션 일주일, 일하고 걷고 귤 먹은 기록'), (select tag_id from tag where name = '워케이션'));
insert into tag (name) values ('제주') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '제주 워케이션 일주일, 일하고 걷고 귤 먹은 기록'), (select tag_id from tag where name = '제주'));
insert into tag (name) values ('원격근무') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '제주 워케이션 일주일, 일하고 걷고 귤 먹은 기록'), (select tag_id from tag where name = '원격근무'));
insert into tag (name) values ('일상') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '제주 워케이션 일주일, 일하고 걷고 귤 먹은 기록'), (select tag_id from tag where name = '일상'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')) and name = '리액트'), (select topic_id from topic where topic_name = '개발'), 'React 19의 폼 액션과 useActionState, 실제로 써 본 정리', '사이드 프로젝트 빵집 지도에 "빵집 제보하기" 폼을 붙이면서 React 19의 폼 액션을 처음 제대로 써 봤어요. 예전처럼 `onSubmit`, `preventDefault`, 로딩 상태, 에러 상태를 하나하나 만들던 것보다 확실히 코드가 줄었어요.

## 예전 방식

```tsx
const [pending, setPending] = useState(false);
const [message, setMessage] = useState('''');

async function handleSubmit(event: React.FormEvent<HTMLFormElement>) {
  event.preventDefault();
  setPending(true);
  const formData = new FormData(event.currentTarget);
  const result = await submitReport(formData);
  setMessage(result.message);
  setPending(false);
}
```

## useActionState로 바꾸기

```tsx
import { useActionState } from ''react'';

type ReportState = { ok: boolean; message: string };

async function reportAction(
  _prevState: ReportState,
  formData: FormData,
): Promise<ReportState> {
  const name = String(formData.get(''name'') ?? '''').trim();
  if (!name) {
    return { ok: false, message: ''빵집 이름을 적어 주세요.'' };
  }
  await submitReport({ name, memo: String(formData.get(''memo'') ?? '''') });
  return { ok: true, message: ''제보 고마워요! 확인하고 지도에 올릴게요.'' };
}

export function ReportForm() {
  const [state, formAction, isPending] = useActionState(reportAction, {
    ok: false,
    message: '''',
  });

  return (
    <form action={formAction}>
      <label htmlFor="name">빵집 이름</label>
      <input id="name" name="name" />

      <label htmlFor="memo">한 줄 메모</label>
      <textarea id="memo" name="memo" />

      <button type="submit" disabled={isPending}>
        {isPending ? ''보내는 중...'' : ''제보하기''}
      </button>
      <p role="status">{state.message}</p>
    </form>
  );
}
```

- `useActionState(액션, 초기 상태)`는 `[상태, 폼에 넘길 액션, 진행 중 여부]`를 돌려줘요.
- 액션 함수는 **이전 상태**와 `FormData`를 받고, 다음 상태를 돌려줘요.
- `<form action={formAction}>`으로 넘기면 `preventDefault`도 필요 없어요.

## 버튼을 따로 뺐을 때: useFormStatus

제출 버튼을 공통 컴포넌트로 빼면 `isPending`을 props로 내려야 하는데, `useFormStatus`를 쓰면 **가장 가까운 부모 form**의 상태를 읽을 수 있어요.

```tsx
import { useFormStatus } from ''react-dom'';

function SubmitButton({ children }: { children: React.ReactNode }) {
  const { pending } = useFormStatus();
  return (
    <button type="submit" disabled={pending}>
      {children}
    </button>
  );
}
```

주의할 점: `useFormStatus`는 `form`을 렌더링하는 **그 컴포넌트 안에서 부르면 안 되고**, form의 자식 컴포넌트에서 불러야 해요. 처음에 같은 컴포넌트에서 불러서 계속 `pending`이 `false`였어요.

## 써 보고 느낀 점

- 제출이 끝나면 비제어 입력창이 **자동으로 초기화**돼요. 에러가 났을 때 입력한 값이 사라지는 게 싫으면, 상태에 입력값을 같이 담아서 `defaultValue`로 되돌려 줘야 했어요.
- 상태 메시지에 `role="status"`를 주면 스크린리더가 결과를 읽어 줘요.

> 폼은 결국 "보내고, 기다리고, 결과를 보여 주는 것". 그 흐름을 React가 한 덩어리로 묶어 준 느낌이에요.
', 'public', 200, '2026-10-04T22:00:00+09:00');
insert into tag (name) values ('react') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'React 19의 폼 액션과 useActionState, 실제로 써 본 정리'), (select tag_id from tag where name = 'react'));
insert into tag (name) values ('react19') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'React 19의 폼 액션과 useActionState, 실제로 써 본 정리'), (select tag_id from tag where name = 'react19'));
insert into tag (name) values ('폼') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'React 19의 폼 액션과 useActionState, 실제로 써 본 정리'), (select tag_id from tag where name = '폼'));
insert into tag (name) values ('useactionstate') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'React 19의 폼 액션과 useActionState, 실제로 써 본 정리'), (select tag_id from tag where name = 'useactionstate'));

-- 반나절여행 · 반나절이면 충분해
insert into blog (users_id, name, intro, comments_read_at) values ((select users_id from users where email = 'demo.halfday@example.com'), '반나절이면 충분해', '토요일 오전에 출발해서 저녁 전에 돌아오는 근교 반나절 기록, 그리고 1년에 두어 번 떠나는 해외 도시 이야기. 교통편, 걸음 수, 쓴 돈까지 적어 둔다.', '2026-10-11T23:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), '미분류', 'public', true, 1, 0, '2023-02-11T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), '근교 반나절', 'public', false, 2, 1, '2023-02-11T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), '해외 도시', 'public', false, 3, 2, '2023-02-11T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), '여행 준비', 'public', false, 4, 3, '2023-02-11T10:00:00+09:00');
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')) and name = '근교 반나절'), (select topic_id from topic where topic_name = '여행'), '양평 두물머리 반나절, 경의중앙선 타고 다녀옴', '토요일 아침에 눈 떴는데 날씨가 너무 좋아서 즉흥으로 나갔다. 목표는 오후 4시 전에 집에 돌아오기.

## 가는 법

- 경의중앙선 **양수역** 하차 (청량리에서 약 50분)
- 양수역에서 두물머리까지 걸어서 25분 정도
- 자전거 빌려도 되는데 3월이라 바람이 차서 그냥 걸었다

## 두물머리

북한강이랑 남한강이 만나는 곳이라서 두물머리. 이름 붙인 사람 센스 좋다. 큰 느티나무 한 그루가 강 쪽으로 서 있는데, 다들 그 나무 앞에서 사진 찍으려고 줄을 섰다. 나는 줄 옆에서 그냥 나무 뒤통수를 찍었다. 그게 더 마음에 든다.

- 느티나무 → 배다리 → 세미원 쪽 연결 산책로
- **세미원**은 입장료 5천 원. 연꽃 철(7~8월)이 아니라서 이번엔 패스
- 강변 데크길 따라 쭉 걷기만 해도 40분은 금방 간다

## 점심

양수역 근처 작은 국숫집에서 잔치국수 한 그릇. 7천 원. 멸치 육수가 진하고 김가루가 많이 올라가 있었다. 근교 반나절의 핵심은 결국 이런 국수 한 그릇 아닐까.

## 오늘 쓴 돈

| 항목 | 금액 |
|---|---|
| 지하철 왕복 | 약 4,000원 |
| 국수 | 7,000원 |
| 핫도그 (두물머리 명물) | 3,500원 |
| 합계 | 14,500원 |

> 반나절 여행은 ''돌아오는 시간''을 먼저 정해 두면 마음이 편하다.

3시 반에 집 도착. 저녁에 빨래도 했다. 완벽한 토요일.', 'public', 1260, '2023-03-04T18:22:00+09:00');
insert into tag (name) values ('양평') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '양평 두물머리 반나절, 경의중앙선 타고 다녀옴'), (select tag_id from tag where name = '양평'));
insert into tag (name) values ('두물머리') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '양평 두물머리 반나절, 경의중앙선 타고 다녀옴'), (select tag_id from tag where name = '두물머리'));
insert into tag (name) values ('반나절') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '양평 두물머리 반나절, 경의중앙선 타고 다녀옴'), (select tag_id from tag where name = '반나절'));
insert into tag (name) values ('경의중앙선') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '양평 두물머리 반나절, 경의중앙선 타고 다녀옴'), (select tag_id from tag where name = '경의중앙선'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')) and name = '해외 도시'), (select topic_id from topic where topic_name = '여행'), '후쿠오카 2박 3일, 동선만 깔끔하게 정리', '첫 해외 혼자 여행. 가까운 데부터 가 보자 해서 후쿠오카로 정했다. 인천에서 1시간 20분이면 도착이라 제주 가는 거랑 별 차이 없었다.

## 숙소 위치

**하카타역 근처**로 잡았다. 공항에서 지하철로 두 정거장이고, 어디로 가든 하카타에서 출발하니까 편하다. 텐진 쪽도 좋다는데 처음이라 교통 편한 쪽으로.

## 1일차 (오후 도착)

1. 공항 → 하카타역 (지하철 5분)
2. 숙소에 짐 맡기고 캐널시티 구경
3. 나카스 강변 포장마차 거리 — 라멘 한 그릇이랑 꼬치 몇 개
4. 숙소 복귀, 편의점 푸딩

포장마차는 자리가 좁아서 옆 사람이랑 어깨가 닿는다. 옆자리 아저씨가 손짓으로 뭘 시키라고 추천해 줬는데 그게 명란 계란말이였다. 맛있었다.

## 2일차

1. 오전: 다자이후 (니시테츠 전철, 텐진에서 약 40분)
2. 다자이후 텐만구 참배길에서 우메가에모치 먹기
3. 오후: 텐진 돌아와서 오호리 공원 한 바퀴 (약 2km)
4. 저녁: 모츠나베

오호리 공원은 호수 둘레를 사람들이 다 같이 뛰고 걷는 분위기라 좋았다. 벤치에 앉아서 1시간 그냥 멍하니 있었다.

## 3일차

- 오전에 하카타역 지하 쇼핑, 오후 비행기로 귀국

## 쓴 돈 대충

- 항공권 왕복 약 28만 원 (특가)
- 숙소 2박 약 16만 원
- 현지 교통+식비+쇼핑 약 25만 원

> 첫 혼자 해외는 ''많이 보기''보다 ''헤매지 않기''를 목표로 하면 덜 지친다.', 'public', 946, '2023-10-21T22:48:00+09:00');
insert into tag (name) values ('후쿠오카') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '후쿠오카 2박 3일, 동선만 깔끔하게 정리'), (select tag_id from tag where name = '후쿠오카'));
insert into tag (name) values ('일본') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '후쿠오카 2박 3일, 동선만 깔끔하게 정리'), (select tag_id from tag where name = '일본'));
insert into tag (name) values ('2박3일') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '후쿠오카 2박 3일, 동선만 깔끔하게 정리'), (select tag_id from tag where name = '2박3일'));
insert into tag (name) values ('해외여행') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '후쿠오카 2박 3일, 동선만 깔끔하게 정리'), (select tag_id from tag where name = '해외여행'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')) and name = '여행 준비'), (select topic_id from topic where topic_name = '여행'), '기내 수하물 7kg만으로 4박 하기, 짐 싸기 체크리스트', '저가 항공 특가를 잡으면 위탁 수하물 비용이 표값만큼 나올 때가 있다. 그래서 작년부터 **기내 수하물 7kg 하나로만** 다니는 연습을 했다. 4박까지는 충분히 된다.

## 가방

- 기내용 배낭 1개 (40L 이하, 무게 1kg 안쪽)
- 작은 크로스백 1개 (여권, 지갑, 휴대폰). 대부분 항공사는 개인 소지품 하나 따로 허용

## 옷 (겨울 기준이 아니라 봄·가을 기준)

- 상의 3장 (하나는 입고 감)
- 하의 2장 (하나는 입고 감)
- 속옷·양말 4세트 — 빨아서 말리면 3세트도 된다
- 얇은 경량 패딩 or 바람막이 1벌 — **입고 탄다**
- 잠옷 대신 다음 날 입을 티셔츠

무거운 옷은 무조건 입고 타는 게 핵심. 공항에서 좀 덥더라도 참는다.

## 세면도구

- 100ml 이하 용기, 투명 지퍼백 1개에 다 들어가야 함
- 샴푸·바디워시는 호텔 거 쓰고, 클렌징폼이랑 로션만 소분
- 칫솔·치약 여행용

## 전자기기

- 충전기 하나에 포트 여러 개 달린 거 (멀티 충전기)
- 보조배터리는 **반드시 기내**에, 용량 확인 (보통 100Wh 이하)
- 멀티 어댑터

## 빼도 되는 것

- 드라이기 (숙소에 다 있음)
- 책 (전자책으로)
- ''혹시 몰라서'' 챙기는 옷 한 벌 — 이게 제일 무겁다

## 무게 재는 법

출발 전날 체중계에 가방 들고 올라가서 내 몸무게 빼기. 6.5kg 넘으면 뭔가 하나 뺀다. 돌아올 때 기념품 넣을 자리 생각해서 0.5kg은 비워 둔다.

> 짐이 가벼우면 여행 동선이 자유로워진다. 숙소 체크아웃하고도 그냥 메고 돌아다닐 수 있으니까.', 'public', 1082, '2024-02-17T21:15:00+09:00');
insert into tag (name) values ('짐싸기') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '기내 수하물 7kg만으로 4박 하기, 짐 싸기 체크리스트'), (select tag_id from tag where name = '짐싸기'));
insert into tag (name) values ('기내수하물') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '기내 수하물 7kg만으로 4박 하기, 짐 싸기 체크리스트'), (select tag_id from tag where name = '기내수하물'));
insert into tag (name) values ('체크리스트') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '기내 수하물 7kg만으로 4박 하기, 짐 싸기 체크리스트'), (select tag_id from tag where name = '체크리스트'));
insert into tag (name) values ('여행준비') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '기내 수하물 7kg만으로 4박 하기, 짐 싸기 체크리스트'), (select tag_id from tag where name = '여행준비'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')) and name = '근교 반나절'), (select topic_id from topic where topic_name = '여행'), '수원 화성 성곽길 반나절 한 바퀴 걸은 날', '현충일 연휴 둘째 날. 너무 멀리는 가기 싫고 집에만 있기도 싫어서 수원 화성으로.

## 코스

성곽 둘레가 약 5.7km. 한 바퀴 다 돌면 천천히 걸어서 2시간 반 정도 걸렸다.

1. **팔달문**에서 시작 (수원역에서 버스 15분)
2. 서남암문 → 서장대 (팔달산 꼭대기, 여기가 제일 힘들다)
3. 화서문 → 장안문
4. **방화수류정** — 여기서 한참 쉬었다
5. 창룡문 → 다시 팔달문

## 좋았던 곳

**방화수류정**은 성곽 위에 있는 정자인데, 아래에 작은 연못(용연)이 있다. 오후 햇빛이 연못에 비쳐서 반짝이는데 그 앞 잔디밭에 사람들이 돗자리 깔고 앉아 있었다. 나도 편의점에서 산 음료 하나 들고 30분 앉아 있었다.

서장대는 계단이 많지만 올라가면 수원 시내가 다 보인다. 성곽이 도시 한가운데를 감싸고 있는 게 신기하다.

## 꿀팁

- 6월 오후는 그늘이 별로 없다. 모자랑 물 꼭 챙기기
- **시계 방향**으로 돌면 힘든 오르막(서장대)을 체력 있을 때 끝낼 수 있다
- 행궁 쪽은 따로 시간 잡아서 보는 게 좋다. 이번엔 성곽만

## 끝나고

행궁동 골목 작은 카페에서 팥빙수. 성곽 걷고 먹는 빙수는 반칙이다.

> 반나절 여행 중 가성비 1등. 지하철+버스 왕복 5천 원 안쪽, 입장료는 성곽만 걸으면 무료다.', 'public', 893, '2024-06-08T19:40:00+09:00');
insert into tag (name) values ('수원') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '수원 화성 성곽길 반나절 한 바퀴 걸은 날'), (select tag_id from tag where name = '수원'));
insert into tag (name) values ('수원화성') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '수원 화성 성곽길 반나절 한 바퀴 걸은 날'), (select tag_id from tag where name = '수원화성'));
insert into tag (name) values ('성곽길') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '수원 화성 성곽길 반나절 한 바퀴 걸은 날'), (select tag_id from tag where name = '성곽길'));
insert into tag (name) values ('반나절') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '수원 화성 성곽길 반나절 한 바퀴 걸은 날'), (select tag_id from tag where name = '반나절'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')) and name = '해외 도시'), (select topic_id from topic where topic_name = '여행'), '타이베이 4박 5일, 지하철로만 다닌 기록', '설 연휴 앞뒤로 연차를 붙여서 타이베이에 다녀왔다. 이번 여행 규칙: **택시 안 타기, MRT랑 버스로만.**

## 교통카드

이지카드(悠遊卡)를 공항에서 샀다. 충전해서 MRT, 버스, 편의점 결제까지 다 된다. 5일 동안 교통비로 쓴 게 우리 돈으로 3만 원 정도.

## 날짜별 동선

**1일차** — 공항 MRT로 타이베이역 → 숙소(중산역 근처) → 닝샤 야시장

**2일차**
- 오전: 중정기념당, 위병 교대식 (정시마다)
- 오후: 융캉제 골목 걷기, 망고빙수는 겨울이라 딸기빙수로
- 저녁: 단수이선 끝까지 타고 **단수이**에서 노을

**3일차** — 근교 당일치기
- 루이팡역에서 버스로 지우펀
- 오후에 스펀으로 넘어가서 천등 날리기
- 핑시선 기차는 배차가 1시간에 한 대라 시간표 꼭 보기

**4일차**
- 오전: 국립고궁박물원 (MRT 스린역 + 버스)
- 오후: 샹산 등산 — 계단 20분 오르면 타이베이101이 정면으로 보인다
- 저녁: 라오허제 야시장, 후추빵

**5일차** — 오전에 디화제 건어물 거리 구경하고 공항

## 좋았던 것

- MRT 안이 정말 조용하다. 다들 줄 서서 타고 음식도 안 먹는다
- 편의점이 너무 많아서 배고플 틈이 없다
- 샹산 꼭대기에서 본 야경은 이번 여행 1등

## 아쉬웠던 것

지우펀은 사람이 너무 많아서 골목에서 거의 떠밀려 다녔다. 다음엔 평일 오전 일찍 가야겠다.

> 지하철로만 다니니까 오히려 도시의 ''진짜 출퇴근 리듬''을 볼 수 있었다.', 'public', 585, '2025-01-25T23:30:00+09:00');
insert into tag (name) values ('타이베이') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '타이베이 4박 5일, 지하철로만 다닌 기록'), (select tag_id from tag where name = '타이베이'));
insert into tag (name) values ('대만') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '타이베이 4박 5일, 지하철로만 다닌 기록'), (select tag_id from tag where name = '대만'));
insert into tag (name) values ('mrt') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '타이베이 4박 5일, 지하철로만 다닌 기록'), (select tag_id from tag where name = 'mrt'));
insert into tag (name) values ('해외여행') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '타이베이 4박 5일, 지하철로만 다닌 기록'), (select tag_id from tag where name = '해외여행'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')) and name = '근교 반나절'), (select topic_id from topic where topic_name = '여행'), '인천 개항장 골목 반나절, 차이나타운 옆 조용한 길', '인천역 내리면 다들 차이나타운으로 직진하는데, 이번엔 그 옆 **개항장 거리** 위주로 돌아봤다. 1호선 끝까지 타고 가는 것도 은근 여행 기분.

## 걸은 순서

1. 인천역 → 차이나타운 입구 패루 (사진 한 장만)
2. **자유공원** 언덕 — 맥아더 동상 지나 정상까지 10분
3. 자유공원에서 내려와 **청일조계지 경계 계단**
4. 개항장 거리: 옛 은행 건물들 (지금은 박물관·전시관)
5. 인천아트플랫폼 — 오래된 창고를 고친 전시 공간
6. 신포국제시장에서 간식

## 기억에 남은 것

**청일조계지 경계 계단**은 계단 하나를 사이에 두고 왼쪽은 중국식, 오른쪽은 일본식 건물이 서 있다. 100년 전에 이 계단이 경계선이었다고. 계단 위에서 내려다보면 저 멀리 바다가 살짝 보인다.

인천아트플랫폼은 빨간 벽돌 창고 사이에 작은 광장이 있어서 쉬어 가기 좋다. 무료 전시도 하나 보고 나왔다.

## 신포시장 간식

- 닭강정 작은 컵 (5천 원)
- 공갈빵 (2천 원) — 속이 텅 비어 있는데 그게 맛이다
- 쫄면은 배불러서 다음 기회에

## 반나절 메모

- 소요: 오전 11시 도착, 오후 4시 출발
- 걸음 수: 약 14,000보
- 쓴 돈: 교통비 포함 2만 원 안쪽

> 유명한 곳 바로 옆에 조용한 길이 꼭 있다. 그걸 찾는 게 반나절 여행의 재미.', 'public', 868, '2025-05-10T18:05:00+09:00');
insert into tag (name) values ('인천') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '인천 개항장 골목 반나절, 차이나타운 옆 조용한 길'), (select tag_id from tag where name = '인천'));
insert into tag (name) values ('개항장') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '인천 개항장 골목 반나절, 차이나타운 옆 조용한 길'), (select tag_id from tag where name = '개항장'));
insert into tag (name) values ('반나절') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '인천 개항장 골목 반나절, 차이나타운 옆 조용한 길'), (select tag_id from tag where name = '반나절'));
insert into tag (name) values ('골목산책') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '인천 개항장 골목 반나절, 차이나타운 옆 조용한 길'), (select tag_id from tag where name = '골목산책'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')) and name = '근교 반나절'), (select topic_id from topic where topic_name = '운동'), '북한산 우이령길 예약하고 걸은 날, 초보도 괜찮은 숲길', '여행 블로그지만 오늘은 운동 쪽 기록. 요즘 주말마다 근교 걷기를 하는데, 그중에 제일 좋았던 우이령길.

## 우이령길이란

북한산과 도봉산 사이 고개를 넘는 길인데, 오랫동안 출입이 막혀 있다가 열린 길이라 숲이 정말 잘 보존돼 있다. **하루 입장 인원이 정해져 있어서 사전 예약 필수.**

- 예약: 국립공원공단 예약 시스템
- 출발: 우이동(서울 쪽) 또는 교현리(양주 쪽)
- 거리: 약 6.8km 편도
- 소요: 천천히 걸어서 2시간 10분

## 걸어 보니

경사가 거의 없다. 등산이라기보다 **숲속 산책**에 가깝다. 바닥이 흙길이라 무릎에도 편했다.

중간쯤 고갯마루에서 보이는 **오봉** 바위 다섯 개가 이 길의 하이라이트. 바위가 나란히 서 있는 모습이 꼭 누가 하나씩 올려놓은 것 같다.

## 기록

| 항목 | 내용 |
|---|---|
| 거리 | 6.8km |
| 시간 | 2시간 10분 |
| 걸음 수 | 약 11,000보 |
| 평균 심박 | 102 |

심박이 100 언저리로 계속 유지돼서 유산소로 딱 좋은 강도였다.

## 준비물

- 신분증 (입구에서 예약 확인)
- 물 500ml 두 병
- 가벼운 간식 — 중간에 매점 없음
- 모기 기피제 (9월에도 있다)

> 숨이 차지 않는 운동이 오히려 오래 간다. 걷는 게 즐거워야 다음 주에도 나온다.

교현리 쪽으로 내려와서 버스 타고 구파발역으로 돌아왔다.', 'public', 686, '2025-09-20T20:12:00+09:00');
insert into tag (name) values ('북한산') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '북한산 우이령길 예약하고 걸은 날, 초보도 괜찮은 숲길'), (select tag_id from tag where name = '북한산'));
insert into tag (name) values ('우이령길') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '북한산 우이령길 예약하고 걸은 날, 초보도 괜찮은 숲길'), (select tag_id from tag where name = '우이령길'));
insert into tag (name) values ('트레킹') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '북한산 우이령길 예약하고 걸은 날, 초보도 괜찮은 숲길'), (select tag_id from tag where name = '트레킹'));
insert into tag (name) values ('걷기') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '북한산 우이령길 예약하고 걸은 날, 초보도 괜찮은 숲길'), (select tag_id from tag where name = '걷기'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')) and name = '여행 준비'), (select topic_id from topic where topic_name = '여행'), '환전 대신 트래블카드 1년 써 보고 정리한 장단점', '작년 초부터 해외 갈 때 현금 환전을 거의 안 하고 **트래블카드**(외화 충전식 체크카드)만 썼다. 1년 동안 일본 두 번, 대만 한 번, 베트남 한 번. 써 보니 정리해 둘 만한 게 있어서 적어 둔다.

## 이런 점이 좋았다

1. **환율 우대가 크다** — 주요 통화는 앱에서 충전할 때 수수료가 거의 없다
2. **필요한 만큼만 충전** — 남은 돈은 다음 여행 때 그대로 쓰거나 다시 원화로 돌린다
3. **현지 ATM 인출** — 제휴 ATM이면 인출 수수료가 없거나 적다
4. 결제할 때마다 앱 알림이 와서 가계부가 자동으로 된다

## 이런 점은 불편했다

- **소규모 가게, 야시장은 여전히 현금**. 대만 야시장에서는 카드 되는 곳이 절반도 안 됐다
- 해외 결제 첫 승인이 가끔 거절된다. 그럴 땐 앱에서 해외 결제 차단 설정을 다시 확인
- 충전 통화가 없는 나라(베트남 동 같은)는 원화나 달러에서 자동 환산돼서 우대가 줄어든다
- 카드 하나만 믿으면 분실했을 때 큰일. **예비 카드 하나는 꼭 따로** 챙긴다

## 내가 정한 방식

- 출발 전: 트래블카드에 예상 경비 70% 충전
- 현지 도착: 공항 ATM에서 현금 2~3일치만 인출
- 여행 중: 큰 결제는 카드, 시장·택시는 현금
- 귀국 후: 남은 외화는 그대로 두고 다음 여행 때 쓰기

## 1년 결산

현금 환전만 하던 재작년보다 체감상 **여행 경비가 3~4% 정도 덜 들었다**. 큰 차이는 아니지만, 무엇보다 남은 동전 처리 걱정이 없어진 게 제일 좋다.

> 결국 현금 조금 + 카드 두 장. 이게 내 정답이다.', 'public', 273, '2026-03-14T22:26:00+09:00');
insert into tag (name) values ('트래블카드') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '환전 대신 트래블카드 1년 써 보고 정리한 장단점'), (select tag_id from tag where name = '트래블카드'));
insert into tag (name) values ('환전') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '환전 대신 트래블카드 1년 써 보고 정리한 장단점'), (select tag_id from tag where name = '환전'));
insert into tag (name) values ('해외결제') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '환전 대신 트래블카드 1년 써 보고 정리한 장단점'), (select tag_id from tag where name = '해외결제'));
insert into tag (name) values ('여행준비') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '환전 대신 트래블카드 1년 써 보고 정리한 장단점'), (select tag_id from tag where name = '여행준비'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')) and name = '근교 반나절'), (select topic_id from topic where topic_name = '여행'), '가평 자라섬 가을 반나절, ITX 타고 다녀온 일기', '개천절 다음 날, 날씨가 너무 좋아서 아침 9시에 일어나자마자 용산역으로 갔다. ITX-청춘 타면 가평역까지 1시간 조금 넘게 걸린다.

## 가는 법

- 용산역 or 청량리역 → ITX-청춘 → **가평역**
- 가평역에서 자라섬까지 걸어서 20분 (평지라 걷기 좋다)
- 경춘선 전철로 가도 되는데 시간이 좀 더 걸린다

## 자라섬

북한강 안에 있는 섬인데 다리로 연결돼 있어서 그냥 걸어 들어간다. 가을이라 **남도 꽃 정원**에 코스모스랑 구절초가 한창이었다.

- 중도 → 남도 순서로 한 바퀴 약 1시간 반
- 남도 끝 쪽 강변 벤치가 명당. 강 건너 산이 슬슬 물들기 시작했다
- 캠핑장은 예약한 사람만 들어갈 수 있다

바람이 불 때마다 코스모스가 한꺼번에 고개를 숙였다 드는 게 꼭 누가 인사하는 것 같았다. 사진은 몇 장 찍고 대부분 그냥 봤다.

## 점심

가평역 근처 작은 막국숫집에서 들기름 막국수. 9천 원. 들기름 향이 진하고 김가루, 깨가 듬뿍. 반 정도 먹고 육수를 부어 먹으라고 사장님이 알려 주셨는데, 그렇게 먹으니까 전혀 다른 음식이 됐다.

## 오늘의 반나절 기록

- 출발 10:10 → 귀가 16:30
- 걸음 수 약 13,000보
- 쓴 돈 약 2만 8천 원 (ITX 왕복 포함)

> 가을은 짧다. 그래서 반나절이라도 꼭 나가야 한다.

다음 주엔 단풍 들면 남이섬 쪽도 가 볼까 생각 중.', 'public', 169, '2026-10-04T19:48:00+09:00');
insert into tag (name) values ('가평') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '가평 자라섬 가을 반나절, ITX 타고 다녀온 일기'), (select tag_id from tag where name = '가평'));
insert into tag (name) values ('자라섬') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '가평 자라섬 가을 반나절, ITX 타고 다녀온 일기'), (select tag_id from tag where name = '자라섬'));
insert into tag (name) values ('itx') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '가평 자라섬 가을 반나절, ITX 타고 다녀온 일기'), (select tag_id from tag where name = 'itx'));
insert into tag (name) values ('가을나들이') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '가평 자라섬 가을 반나절, ITX 타고 다녀온 일기'), (select tag_id from tag where name = '가을나들이'));

-- 퇴근집밥 · 퇴근 후 30분 부엌
insert into blog (users_id, name, intro, comments_read_at) values ((select users_id from users where email = 'demo.homecook@example.com'), '퇴근 후 30분 부엌', '둘 다 7시 넘어 퇴근하는 부부의 평일 저녁 기록. 일요일 두 시간 손질, 평일 30분 요리, 목요일엔 냉장고 파먹기. 거창하지 않은 집밥 레시피를 모아요.', '2026-10-11T23:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), '미분류', 'public', true, 1, 0, '2023-02-19T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), '평일 집밥', 'public', false, 2, 1, '2023-02-19T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), '반찬', 'public', false, 3, 2, '2023-02-19T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), '냉장고 파먹기', 'public', false, 4, 3, '2023-02-19T10:00:00+09:00');
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')) and name = '평일 집밥'), (select topic_id from topic where topic_name = '음식'), '맞벌이 부부의 평일 집밥, 일요일 두 시간으로 시작해요', '결혼하고 1년 동안 저녁은 거의 배달이었어요. 둘 다 7시 넘어 퇴근하니까 뭘 해 먹을 기운이 없더라고요. 그런데 한 달 배달비를 계산해 보고 둘이 동시에 말이 없어졌어요. 그래서 올해는 **평일 저녁 30분 안에 집밥**을 목표로 정했습니다.

## 핵심은 일요일 두 시간

평일에 30분 안에 끝내려면 결국 손질을 미리 해 두는 수밖에 없더라고요. 일요일 오후 4시쯤 장을 보고 와서 두 시간 동안 이렇게 해요.

- 양파 3개: 반은 채 썰고 반은 깍둑 썰어 지퍼백에 따로
- 대파 한 단: 송송 썰어 키친타월 깐 통에 (일주일 거뜬해요)
- 애호박, 당근: 반달 썰기 해서 통에
- 돼지고기 앞다리 600g: 200g씩 나눠 냉동
- 다진 마늘: 한 번에 200g 사서 얼음틀에 얼리기

## 평일 저녁 루틴

1. 먼저 들어온 사람이 밥솥 예약 확인, 국이나 찌개 물 올리기
2. 늦게 온 사람이 메인 하나 볶기
3. 반찬은 일요일에 만든 것 두 가지 꺼내기

이렇게 역할을 나누니까 30분이 생각보다 넉넉해요. 첫 주는 40분 넘게 걸렸는데 둘째 주부터는 25분 만에 식탁에 앉았어요.

> 완벽한 한 상이 아니라, 오늘도 같이 앉아서 먹는 게 목표예요.

앞으로 여기에 평일에 실제로 해 먹은 메뉴들을 기록해 보려고 해요. 냉장고에 남은 걸로 버티는 날도 솔직하게 쓸게요.', 'public', 1319, '2023-03-05T21:40:00+09:00');
insert into tag (name) values ('집밥') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '맞벌이 부부의 평일 집밥, 일요일 두 시간으로 시작해요'), (select tag_id from tag where name = '집밥'));
insert into tag (name) values ('밀프렙') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '맞벌이 부부의 평일 집밥, 일요일 두 시간으로 시작해요'), (select tag_id from tag where name = '밀프렙'));
insert into tag (name) values ('맞벌이') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '맞벌이 부부의 평일 집밥, 일요일 두 시간으로 시작해요'), (select tag_id from tag where name = '맞벌이'));
insert into tag (name) values ('장보기') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '맞벌이 부부의 평일 집밥, 일요일 두 시간으로 시작해요'), (select tag_id from tag where name = '장보기'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')) and name = '평일 집밥'), (select topic_id from topic where topic_name = '음식'), '애호박 두부 된장찌개, 물 올리고 20분이면 끝', '퇴근하고 제일 자주 끓이는 게 된장찌개예요. 재료가 단순하고 냉장고에 늘 있는 것들이라 실패가 없어요.

## 재료 (2인)

- 물 500ml (쌀뜨물이면 더 좋아요)
- 국물용 멸치 8마리, 다시마 손바닥 반 장
- 된장 2큰술, 고추장 1/2작은술
- 애호박 1/3개, 두부 반 모, 양파 1/4개
- 청양고추 1개, 대파 약간, 다진 마늘 1작은술

## 순서

1. 냄비에 물, 멸치, 다시마를 넣고 센 불. 끓기 시작하면 다시마만 건져요. (여기까지 5분)
2. 그동안 애호박은 반달, 두부는 깍둑, 양파는 사각으로 썰어 둬요.
3. 멸치를 건지고 된장을 체에 풀어 넣어요. 고추장 아주 조금이 국물 색을 예쁘게 해 줘요.
4. 양파, 애호박 먼저 넣고 4분.
5. 두부, 마늘, 청양고추 넣고 3분 더.
6. 불 끄기 직전 대파.

**포인트는 된장을 너무 오래 끓이지 않는 것**이에요. 처음엔 20분 넘게 졸였더니 짜고 텁텁했어요. 된장 넣고 10분 안쪽이 딱 좋더라고요.

남편은 여기에 차돌을 넣자고 매번 그러는데, 그건 월급날에만 해 주기로 했습니다.', 'public', 554, '2023-09-12T20:15:00+09:00');
insert into tag (name) values ('된장찌개') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '애호박 두부 된장찌개, 물 올리고 20분이면 끝'), (select tag_id from tag where name = '된장찌개'));
insert into tag (name) values ('애호박') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '애호박 두부 된장찌개, 물 올리고 20분이면 끝'), (select tag_id from tag where name = '애호박'));
insert into tag (name) values ('20분요리') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '애호박 두부 된장찌개, 물 올리고 20분이면 끝'), (select tag_id from tag where name = '20분요리'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')) and name = '냉장고 파먹기'), (select topic_id from topic where topic_name = '음식'), '냉장고 파먹기 1탄: 자투리 채소 볶음밥이랑 계란국', '목요일쯤 되면 냉장고가 애매해져요. 양파 반 개, 당근 한 토막, 시든 브로콜리 두 송이, 햄 조금. 이럴 때 장 보러 가지 않고 **있는 걸로 끝내는 날**을 정했어요. 이름하여 냉장고 파먹기.

## 오늘 냉장고 상황

- 양파 1/2개, 당근 3cm, 브로콜리 조금
- 김밥 싸고 남은 햄 한 줄
- 계란 4개
- 찬밥 두 공기 (전날 많이 한 밥)

## 볶음밥

1. 채소랑 햄을 전부 5mm 크기로 잘게 썰어요. 크기만 맞추면 뭐든 볶음밥이 돼요.
2. 팬에 식용유 2큰술, 대파 한 줌 넣고 파기름부터.
3. 당근, 양파, 브로콜리, 햄 순서로 넣고 중불 3분.
4. 찬밥을 넣고 주걱으로 누르듯 펴서 볶아요.
5. 굴소스 1큰술, 간장 1/2큰술을 팬 가장자리에 둘러서 살짝 눌어붙게.
6. 마지막에 참기름 약간, 깨.

찬밥이라서 오히려 고슬고슬하게 잘 볶여요.

## 계란국

물 600ml에 멸치 육수 팩 하나 넣고 끓이다가, 국간장 1큰술, 소금 한 꼬집. 계란 2개 풀어서 **젓지 말고 가만히** 30초 둔 다음 저어야 몽글몽글해요. 남은 대파 다 털어 넣으면 끝.

냉장고가 비니까 이상하게 마음이 개운해요. 금요일 장보기 목록도 훨씬 짧아졌어요.', 'public', 1006, '2024-02-27T21:05:00+09:00');
insert into tag (name) values ('냉파') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉장고 파먹기 1탄: 자투리 채소 볶음밥이랑 계란국'), (select tag_id from tag where name = '냉파'));
insert into tag (name) values ('볶음밥') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉장고 파먹기 1탄: 자투리 채소 볶음밥이랑 계란국'), (select tag_id from tag where name = '볶음밥'));
insert into tag (name) values ('계란국') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉장고 파먹기 1탄: 자투리 채소 볶음밥이랑 계란국'), (select tag_id from tag where name = '계란국'));
insert into tag (name) values ('자투리채소') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉장고 파먹기 1탄: 자투리 채소 볶음밥이랑 계란국'), (select tag_id from tag where name = '자투리채소'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')) and name = '반찬'), (select topic_id from topic where topic_name = '음식'), '여름 밑반찬 세 가지: 오이무침, 가지볶음, 감자채볶음', '여름엔 불 앞에 오래 서 있기가 힘들어서 일요일에 반찬 세 개를 한꺼번에 해 둬요. 셋 다 합쳐서 50분 정도 걸려요.

## 1. 오이무침 (5일 보관)

- 오이 2개, 양파 1/4개
- 소금 1/2큰술로 10분 절이기
- 고춧가루 1큰술, 식초 1큰술, 설탕 1/2큰술, 다진 마늘 1/2작은술, 깨

절인 오이는 **손으로 꼭 짜야** 물이 덜 생겨요. 처음에 대충 짰다가 다음 날 반찬통이 물바다였어요.

## 2. 가지볶음 (3일 보관)

- 가지 2개를 길게 반 갈라 어슷썰기
- 팬에 기름을 넉넉히 두르고 가지만 먼저 센 불 3분
- 간장 1.5큰술, 올리고당 1큰술, 다진 마늘 1작은술, 물 2큰술 넣고 1분 더

가지는 기름을 먹어야 맛있는데, 너무 많이 먹이면 느끼해요. 가지 넣기 전에 기름 2큰술이 저희 집 기준이에요.

## 3. 감자채볶음 (4일 보관)

- 감자 2개 채 썰어 찬물에 10분 담가 전분 빼기
- 물기 완전히 닦고 기름 두른 팬에 중불 5분
- 소금 두 꼬집, 후추, 마지막에 채 썬 양파 조금

**전분을 빼야 서로 안 달라붙어요.** 이거 한 단계 건너뛰면 감자전이 됩니다(경험담).

세 개 다 만들어 두면 평일엔 메인 하나만 하면 돼서 저녁 준비가 정말 짧아져요.', 'public', 951, '2024-08-18T16:30:00+09:00');
insert into tag (name) values ('밑반찬') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '여름 밑반찬 세 가지: 오이무침, 가지볶음, 감자채볶음'), (select tag_id from tag where name = '밑반찬'));
insert into tag (name) values ('오이무침') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '여름 밑반찬 세 가지: 오이무침, 가지볶음, 감자채볶음'), (select tag_id from tag where name = '오이무침'));
insert into tag (name) values ('가지볶음') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '여름 밑반찬 세 가지: 오이무침, 가지볶음, 감자채볶음'), (select tag_id from tag where name = '가지볶음'));
insert into tag (name) values ('감자채볶음') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '여름 밑반찬 세 가지: 오이무침, 가지볶음, 감자채볶음'), (select tag_id from tag where name = '감자채볶음'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')) and name = '냉장고 파먹기'), (select topic_id from topic where topic_name = '음식'), '냉동실 정리하다가 만든 돼지고기 김치 짜글이', '냉동실 문이 안 닫혀서 결국 정리를 했어요. 언제 얼렸는지 모르는 돼지고기 앞다리 200g 두 봉지, 신김치 반 포기, 반쯤 남은 두부. 오늘 저녁은 짜글이로 정했습니다.

## 재료 (2~3인)

- 돼지고기 앞다리 400g (해동)
- 신김치 2컵, 김칫국물 4큰술
- 양파 1개, 두부 반 모, 대파 1대
- 고춧가루 1큰술, 고추장 1큰술, 국간장 1큰술, 설탕 1/2작은술
- 물 400ml

## 만들기

1. 냄비에 기름 약간 두르고 돼지고기를 먼저 볶아요. 겉면이 하얗게 되면 김치 투하.
2. 김치가 투명해질 때까지 5분 정도 같이 볶아요. **이 단계가 맛의 반**이에요.
3. 고춧가루, 고추장 넣고 30초만 더 볶다가 물과 김칫국물.
4. 양파 넣고 중불에서 10분 자박하게 졸여요. 국물이 찌개보다 적은 게 짜글이죠.
5. 두부 올리고 3분, 대파 넣고 마무리.

김치가 정말 시었으면 설탕을 조금 더 넣어 주세요. 신맛이 확 줄어요.

## 냉동실 정리 팁

정리하면서 제일 크게 느낀 건 **라벨을 안 붙이면 다 미스터리 봉지가 된다**는 거였어요. 이제 지퍼백에 마스킹테이프 붙이고 날짜랑 무게를 적어요. 고기는 한 달 안에 먹는 걸 원칙으로 했어요.

밥 두 그릇 뚝딱이었고, 남은 국물에 다음 날 라면 사리를 넣었습니다.', 'public', 456, '2025-03-18T20:50:00+09:00');
insert into tag (name) values ('짜글이') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉동실 정리하다가 만든 돼지고기 김치 짜글이'), (select tag_id from tag where name = '짜글이'));
insert into tag (name) values ('김치') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉동실 정리하다가 만든 돼지고기 김치 짜글이'), (select tag_id from tag where name = '김치'));
insert into tag (name) values ('냉동실정리') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉동실 정리하다가 만든 돼지고기 김치 짜글이'), (select tag_id from tag where name = '냉동실정리'));
insert into tag (name) values ('냉파') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉동실 정리하다가 만든 돼지고기 김치 짜글이'), (select tag_id from tag where name = '냉파'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')) and name = '냉장고 파먹기'), (select topic_id from topic where topic_name = '여행'), '속초 1박 2일, 중앙시장에서 사 온 것들로 일주일 버티기', '여름휴가를 늦게 써서 9월 초에 속초를 다녀왔어요. 바다도 좋았지만 사실 저희 부부의 진짜 목적은 **시장 장보기**였어요.

## 첫날

오후 2시쯤 도착해서 영금정 쪽 바다를 한참 걸었어요. 바람이 세서 모자를 두 번이나 잡으러 뛰었네요. 저녁은 숙소 근처 작은 횟집 ''바위섬식당''에서 물회를 먹었는데, 살얼음 육수에 배가 들어 있어서 시원했어요.

## 둘째 날, 중앙시장

아침 일찍 시장에 갔어요. 장바구니 두 개랑 아이스박스를 일부러 챙겨 갔어요.

- 반건조 오징어 5마리
- 가자미 3마리 (손질해서 소금 살짝)
- 명란 젓갈 300g
- 강원도 감자 한 봉지
- 동네 분이 직접 만드셨다는 오징어순대 한 줄

## 돌아와서 일주일

- **월**: 반건조 오징어 버터구이 + 맥주 (출근 전날인데…)
- **화**: 가자미 구이, 감자채볶음
- **수**: 명란 계란말이
- **목**: 감자 넣은 된장찌개, 남은 오징어 볶음
- **금**: 명란 버터 파스타로 마무리

시장에서 사 온 재료는 손질이 이미 돼 있어서 평일 요리가 오히려 빨랐어요. 휴가 끝난 허전함을 일주일 내내 밥상에서 조금씩 달랬습니다.

> 여행지의 맛을 집으로 가져오는 방법은 결국 장바구니인 것 같아요.', 'public', 506, '2025-09-07T19:20:00+09:00');
insert into tag (name) values ('속초') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '속초 1박 2일, 중앙시장에서 사 온 것들로 일주일 버티기'), (select tag_id from tag where name = '속초'));
insert into tag (name) values ('전통시장') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '속초 1박 2일, 중앙시장에서 사 온 것들로 일주일 버티기'), (select tag_id from tag where name = '전통시장'));
insert into tag (name) values ('여행장보기') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '속초 1박 2일, 중앙시장에서 사 온 것들로 일주일 버티기'), (select tag_id from tag where name = '여행장보기'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')) and name = '평일 집밥'), (select topic_id from topic where topic_name = '음식'), '간장 닭다리살 덮밥, 25분 저녁의 정석', '요즘 평일 저녁 1순위 메뉴예요. 닭다리살은 퍽퍽하지 않고, 껍질 쪽을 바삭하게 구우면 남편이 말없이 밥을 더 퍼요.

## 재료 (2인)

- 닭다리살 정육 400g
- 양파 1/2개, 대파 흰 부분 1대
- 소스: 간장 3큰술, 맛술 2큰술, 설탕 1큰술, 물 3큰술, 다진 마늘 1작은술, 생강가루 약간
- 계란 2개, 밥 2공기

## 순서

1. 닭다리살은 두꺼운 부분에 칼집을 내서 두께를 맞추고 소금, 후추.
2. 달군 팬에 **껍질 쪽부터** 기름 없이 올려요. 중불에서 6~7분, 건드리지 않는 게 포인트.
3. 뒤집어서 3분. 나온 기름은 키친타월로 닦아요.
4. 양파, 대파를 팬 빈 곳에 넣고 1분.
5. 소스를 붓고 숟가락으로 끼얹으며 2분 졸여요. 윤기가 돌면 불 끄기.
6. 그동안 반숙 계란 프라이 두 개.
7. 닭은 한입 크기로 썰어 밥 위에, 남은 소스 둘러 주고 계란 올려 마무리.

## 소스는 미리 섞어 둬요

일요일에 소스를 두 배로 만들어 병에 담아 두면 평일엔 붓기만 하면 돼요. 냉장 일주일은 문제없었어요.

배달 앱을 거의 안 열게 된 지 3년째인데, 이 메뉴가 일등 공신이에요.', 'public', 465, '2026-04-14T20:30:00+09:00');
insert into tag (name) values ('덮밥') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '간장 닭다리살 덮밥, 25분 저녁의 정석'), (select tag_id from tag where name = '덮밥'));
insert into tag (name) values ('닭다리살') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '간장 닭다리살 덮밥, 25분 저녁의 정석'), (select tag_id from tag where name = '닭다리살'));
insert into tag (name) values ('간장소스') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '간장 닭다리살 덮밥, 25분 저녁의 정석'), (select tag_id from tag where name = '간장소스'));
insert into tag (name) values ('한그릇') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '간장 닭다리살 덮밥, 25분 저녁의 정석'), (select tag_id from tag where name = '한그릇'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')) and name = '반찬'), (select topic_id from topic where topic_name = '음식'), '일요일 반찬 데이: 마약계란장이랑 메추리알 장조림', '계란 반찬은 아무리 많이 해도 금방 없어져요. 오늘은 한 번 끓인 간장 베이스 하나로 두 가지를 만들었어요.

## 공통 간장물

- 간장 150ml, 물 300ml, 설탕 3큰술, 올리고당 2큰술
- 양파 1/2개, 대파 1대, 마늘 6쪽, 통후추 약간

전부 넣고 10분 끓인 다음 건더기를 건져요. 이걸 반으로 나눠 씁니다.

## 1. 마약계란장

1. 계란 8개를 끓는 물에 넣고 **6분 30초**. 바로 찬물에 담가요.
2. 껍질 벗긴 계란을 통에 담고 식힌 간장물 반을 부어요.
3. 쪽파 송송, 청양고추 1개, 홍고추 1개, 깨 1큰술, 참기름 1작은술.
4. 냉장고에서 반나절이면 먹을 수 있어요.

6분 30초가 노른자가 꾸덕하게 흐르는 딱 그 지점이에요. 7분 넘으면 그냥 삶은 계란이 돼요.

## 2. 메추리알 장조림

1. 깐 메추리알 500g, 꽈리고추 한 줌.
2. 남은 간장물 반에 메추리알 넣고 중불 15분.
3. 꽈리고추 넣고 3분 더.

**꽈리고추는 포크로 구멍을 몇 개 내야** 간이 배어요.

두 통 꽉 채워 두면 일주일 아침이 든든해요. 출근 전에 밥에 계란 하나 얹고 간장물 한 숟갈이면 끝이거든요.', 'public', 277, '2026-07-19T17:10:00+09:00');
insert into tag (name) values ('계란장') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '일요일 반찬 데이: 마약계란장이랑 메추리알 장조림'), (select tag_id from tag where name = '계란장'));
insert into tag (name) values ('장조림') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '일요일 반찬 데이: 마약계란장이랑 메추리알 장조림'), (select tag_id from tag where name = '장조림'));
insert into tag (name) values ('메추리알') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '일요일 반찬 데이: 마약계란장이랑 메추리알 장조림'), (select tag_id from tag where name = '메추리알'));
insert into tag (name) values ('밑반찬') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '일요일 반찬 데이: 마약계란장이랑 메추리알 장조림'), (select tag_id from tag where name = '밑반찬'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')) and name = '냉장고 파먹기'), (select topic_id from topic where topic_name = '음식'), '추석 끝나고 남은 전으로 끓인 전찌개, 연휴 뒤 첫 저녁', '연휴 마지막 날 양가에서 싸 주신 전이 큰 통으로 두 개였어요. 동그랑땡, 동태전, 호박전, 꼬치전까지. 데워 먹는 것도 이틀이면 질려서 오늘은 전찌개를 끓였어요.

## 재료 (2~3인)

- 남은 전 아무거나 한 접시 (저는 12조각 정도)
- 신김치 1컵
- 무 한 토막(3cm), 양파 1/2개, 두부 1/3모
- 멸치 육수 700ml
- 고춧가루 1.5큰술, 국간장 1큰술, 다진 마늘 1작은술, 새우젓 1/2작은술
- 대파, 청양고추

## 만들기

1. 육수에 나박 썬 무를 먼저 넣고 8분 끓여요.
2. 김치, 양파, 고춧가루, 마늘 넣고 5분.
3. 국간장과 새우젓으로 간을 맞춰요. **전에 이미 간이 있으니까 처음엔 싱겁다 싶게.**
4. 전은 마지막에 넣고 3~4분만. 오래 끓이면 부침옷이 다 풀어져요.
5. 두부, 대파, 청양고추 넣고 마무리.

## 오늘의 깨달음

- 동태전은 넣자마자 국물에 맛이 확 우러나요. 하나쯤은 꼭 넣어 주세요.
- 꼬치전의 맛살은 금방 풀어지니까 빼고 넣는 게 나아요.
- 기름진 전 때문에 국물이 무거울 수 있는데, 무를 넉넉히 넣으면 시원해져요.

명절 음식이 냉장고에 쌓여 있으면 은근히 마음이 무거운데, 한 냄비로 해결하니 후련하네요. 이렇게 또 한 번의 냉장고 파먹기 성공입니다.', 'public', 236, '2026-09-29T21:00:00+09:00');
insert into tag (name) values ('전찌개') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '추석 끝나고 남은 전으로 끓인 전찌개, 연휴 뒤 첫 저녁'), (select tag_id from tag where name = '전찌개'));
insert into tag (name) values ('명절음식') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '추석 끝나고 남은 전으로 끓인 전찌개, 연휴 뒤 첫 저녁'), (select tag_id from tag where name = '명절음식'));
insert into tag (name) values ('추석') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '추석 끝나고 남은 전으로 끓인 전찌개, 연휴 뒤 첫 저녁'), (select tag_id from tag where name = '추석'));
insert into tag (name) values ('냉파') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '추석 끝나고 남은 전으로 끓인 전찌개, 연휴 뒤 첫 저녁'), (select tag_id from tag where name = '냉파'));

-- 퇴근홈트 · 퇴근 후 매트 한 장
insert into blog (users_id, name, intro, comments_read_at) values ((select users_id from users where email = 'demo.hometrain@example.com'), '퇴근 후 매트 한 장', '푸시업 0개에서 시작한 직장인의 홈트 기록장. 루틴, 자세, 식단을 그때그때 적어 둡니다. 거창한 몸 말고 오래 하는 운동이 목표예요.', '2026-10-11T23:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), '미분류', 'public', true, 1, 0, '2023-04-02T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), '홈트 루틴', 'public', false, 2, 1, '2023-04-02T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), '운동 기록', 'public', false, 3, 2, '2023-04-02T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), '식단', 'public', false, 4, 3, '2023-04-02T10:00:00+09:00');
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')) and name = '운동 기록'), (select topic_id from topic where topic_name = '운동'), '퇴근하고 홈트 시작한 이유, 그리고 첫 주 기록', '회사 건강검진 결과지를 받았는데 **체지방률**이랑 **허리둘레** 칸에 빨간 글씨가 찍혀 있었어요. 숫자를 보고 나서야 요 몇 년 동안 계단 한 층 올라가도 숨이 찼던 게 기분 탓이 아니었구나 싶더라고요.

헬스장을 끊을까 했는데, 솔직히 퇴근하고 옷 갈아입고 헬스장까지 가는 길에 마음이 바뀔 게 뻔했어요. 그래서 일단 **집에서, 매트 한 장으로** 시작해 보기로 했습니다.

## 첫 주에 한 것

- 월: 스쿼트 10개 × 3세트, 무릎 대고 푸시업 5개 × 3세트
- 화: 쉬고 동네 30분 걷기
- 수: 월요일이랑 똑같이 + 플랭크 20초 × 3
- 목: 근육통 때문에 스트레칭만 15분
- 금: 회식... 건너뜀
- 토: 스쿼트 12개 × 3, 푸시업 6개 × 3, 플랭크 25초 × 3
- 일: 걷기 40분

적어 놓고 보니 별거 없는데, 수요일 아침에 허벅지가 너무 아파서 계단을 옆으로 내려갔어요. 평소에 얼마나 안 썼으면 스쿼트 30개에 이러나 싶었습니다.

## 정한 규칙 세 가지

1. **퇴근하고 집에 오면 옷부터 갈아입는다.** 소파에 앉는 순간 끝이에요.
2. **딱 15분만 한다고 생각한다.** 막상 하면 20분, 25분 하게 되더라고요.
3. **기록은 무조건 남긴다.** 안 한 날도 "안 함"이라고 적기.

> 첫 주의 목표는 잘하는 게 아니라 그냥 매트를 펴는 것.

다음 주에는 푸시업을 무릎 안 대고 한 개라도 해 보는 게 목표예요. 지금은 내려가다가 그대로 바닥에 붙어 버립니다. 기록 블로그로 쭉 써 볼게요.
', 'public', 1183, '2023-04-09T21:40:00+09:00');
insert into tag (name) values ('홈트') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '퇴근하고 홈트 시작한 이유, 그리고 첫 주 기록'), (select tag_id from tag where name = '홈트'));
insert into tag (name) values ('운동시작') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '퇴근하고 홈트 시작한 이유, 그리고 첫 주 기록'), (select tag_id from tag where name = '운동시작'));
insert into tag (name) values ('직장인운동') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '퇴근하고 홈트 시작한 이유, 그리고 첫 주 기록'), (select tag_id from tag where name = '직장인운동'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')) and name = '운동 기록'), (select topic_id from topic where topic_name = '운동'), '푸시업 0개에서 20개까지, 석 달 걸린 과정', '홈트 시작할 때 정자세 푸시업이 **0개**였어요. 내려가면 못 올라왔거든요. 오늘 쉬지 않고 20개를 채워서, 그동안 한 걸 정리해 두려고 합니다.

## 1단계: 벽 푸시업과 책상 푸시업 (4주)

처음엔 바닥에서 하려니까 자세가 다 무너져서 **높이를 올려서** 시작했어요.

- 1~2주: 벽에 손 짚고 15개 × 3세트
- 3~4주: 식탁 모서리 잡고 12개 × 3세트

높이가 낮아질수록 확실히 힘들어져요. 쉬워 보여도 팔꿈치가 몸통에서 45도쯤 벌어지게, 엉덩이가 처지지 않게만 신경 썼습니다.

## 2단계: 무릎 푸시업 + 네거티브 (4주)

- 무릎 대고 10개 × 3세트
- 정자세로 **천천히 내려가기만** 5개 (3~4초 동안 내려가고, 올라올 땐 무릎 대고)

이 네거티브가 진짜 효과가 좋았어요. 내려가는 힘이 생기니까 어느 날 갑자기 한 개가 올라오더라고요. 첫 1개 성공한 날 혼자 거실에서 소리 질렀습니다.

## 3단계: 정자세 개수 늘리기 (5주)

**하루 총 개수**를 목표로 잡았어요. 한 번에 몇 개가 아니라 여러 세트로 나눠서 채우기.

| 주차 | 하루 목표 | 보통 세트 구성 |
|---|---|---|
| 1 | 15개 | 5-4-3-3 |
| 2 | 25개 | 7-6-6-6 |
| 3 | 35개 | 10-9-8-8 |
| 4 | 40개 | 13-10-9-8 |
| 5 | 50개 | 16-12-12-10 |

5주 차 마지막 날 테스트로 한 번에 몇 개 되는지 해 봤는데 20개에서 멈췄어요.

## 해 보니 중요했던 것

- **자세가 무너진 개수는 세지 않기.** 허리 처지면 그 개수는 버려요.
- 손목 아플 때는 푸시업 바 대신 **주먹 쥐고** 했더니 편했어요.
- 주 3~4회면 충분해요. 매일 하던 주에 오히려 개수가 떨어졌습니다.

다음 목표는 다이아몬드 푸시업이랑 30개입니다.
', 'public', 877, '2023-08-20T20:15:00+09:00');
insert into tag (name) values ('푸시업') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '푸시업 0개에서 20개까지, 석 달 걸린 과정'), (select tag_id from tag where name = '푸시업'));
insert into tag (name) values ('맨몸운동') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '푸시업 0개에서 20개까지, 석 달 걸린 과정'), (select tag_id from tag where name = '맨몸운동'));
insert into tag (name) values ('점진적과부하') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '푸시업 0개에서 20개까지, 석 달 걸린 과정'), (select tag_id from tag where name = '점진적과부하'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')) and name = '홈트 루틴'), (select topic_id from topic where topic_name = '운동'), '무릎 안 아픈 스쿼트, 거울 앞에서 확인하는 체크리스트', '스쿼트만 하면 무릎 앞쪽이 시큰거려서 한동안 런지로 바꿔서 했었어요. 그런데 영상 찍어 보니 문제는 운동이 아니라 **제 자세**였습니다. 거울 앞에서 하나씩 확인하는 순서를 정리해 둘게요.

## 서기 전에

- **발 너비**: 골반보다 조금 넓게. 저는 어깨너비 정도가 편했어요.
- **발끝 방향**: 정면이 아니라 바깥으로 15~30도. 사람마다 고관절 모양이 달라서 편한 각도를 찾는 게 먼저예요.
- 발바닥 세 점(엄지발가락 뿌리, 새끼발가락 뿌리, 뒤꿈치)으로 바닥 누르기.

## 내려갈 때

1. **엉덩이를 먼저 뒤로** 빼면서 앉기 시작해요. 무릎부터 꺾으면 체중이 앞으로 쏠려요.
2. 무릎은 **발끝 방향 그대로** 따라가게. 안으로 모이는 게 제일 문제였어요.
3. 상체는 살짝 숙여도 괜찮아요. 허리만 둥글게 말리지 않으면 돼요.
4. 깊이는 허벅지가 바닥과 평행한 정도까지. 처음엔 의자에 엉덩이 살짝 닿는 데까지만 했어요.

> 무릎이 발끝을 넘으면 안 된다는 말을 너무 믿었는데, 그것보다 **무릎이 안쪽으로 무너지지 않는 것**이 저한텐 훨씬 중요했어요.

## 올라올 때

- 발바닥 전체로 바닥을 밀어낸다는 느낌
- 맨 위에서 엉덩이에 살짝 힘 주고 끝내기 (허리 젖히지 않게)

## 그래도 무릎이 불편하면

- **박스 스쿼트**: 의자에 앉았다 일어나기부터
- **고블릿 스쿼트**: 물 채운 2L 페트병을 가슴 앞에 들면 오히려 균형 잡기가 쉬워져요
- 계속 아프면 그냥 쉬고 병원 가기. 참는다고 늘지 않더라고요.

이렇게 바꾸고 나서 두 달째 무릎 통증 없이 하고 있어요. 영상 한 번 찍어 보시는 거 진짜 추천합니다.
', 'public', 921, '2024-01-14T22:05:00+09:00');
insert into tag (name) values ('스쿼트') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '무릎 안 아픈 스쿼트, 거울 앞에서 확인하는 체크리스트'), (select tag_id from tag where name = '스쿼트'));
insert into tag (name) values ('자세교정') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '무릎 안 아픈 스쿼트, 거울 앞에서 확인하는 체크리스트'), (select tag_id from tag where name = '자세교정'));
insert into tag (name) values ('무릎통증') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '무릎 안 아픈 스쿼트, 거울 앞에서 확인하는 체크리스트'), (select tag_id from tag where name = '무릎통증'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')) and name = '식단'), (select topic_id from topic where topic_name = '음식'), '닭가슴살 질릴 때 돌려 먹는 단백질 반찬 5가지', '운동 시작하고 제일 먼저 포기한 게 **닭가슴살만 먹기**였어요. 2주 만에 냉동실에 박스째 남았습니다. 그 뒤로 일요일 저녁에 반찬을 몇 가지 만들어 두고 돌려 먹는데, 1년 넘게 이 방법으로 버티고 있어요.

## 1. 두부 간장 조림

부침용 두부를 도톰하게 잘라 마른 팬에 굽고, 간장·물·다진 마늘·올리고당 조금 넣고 졸이면 끝. 식어도 맛있어서 도시락 반찬으로 최고예요.

## 2. 달걀 장조림

삶은 달걀 10개 기준, 간장 반 컵, 물 한 컵 반, 설탕 약간, 마늘이랑 청양고추. 한 번 끓이고 식혀서 통에 담아 두면 일주일 가요. **아침에 두 알**이 고정 메뉴가 됐어요.

## 3. 돼지 앞다리살 수육

생각보다 기름기가 적은 부위예요. 된장 한 숟갈, 월계수잎, 양파 넣고 40분 정도 삶아요. 얇게 썰어서 소분해 두면 상추에 싸서 저녁으로 먹기 좋아요.

## 4. 참치 양배추 볶음

통조림 참치 기름 빼고, 채 썬 양배추랑 같이 볶다가 간장 약간. 10분 컷이라 반찬 떨어진 평일에 자주 해요.

## 5. 그릭요거트 + 견과류

반찬은 아니지만 간식으로 늘 냉장고에 있어요. 단 게 당길 때 꿀 반 숟갈만 넣어 먹습니다.

## 대충 지키는 원칙

- 끼니마다 **손바닥 하나만큼** 단백질 반찬
- 밥은 줄이지만 아예 빼지는 않기 (운동할 힘이 안 나요)
- 주말 한 끼는 그냥 먹고 싶은 거 먹기

> 식단은 완벽하게 하는 것보다 **질리지 않게** 하는 게 오래가는 방법인 것 같아요.
', 'public', 1192, '2024-06-02T19:30:00+09:00');
insert into tag (name) values ('단백질식단') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '닭가슴살 질릴 때 돌려 먹는 단백질 반찬 5가지'), (select tag_id from tag where name = '단백질식단'));
insert into tag (name) values ('다이어트반찬') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '닭가슴살 질릴 때 돌려 먹는 단백질 반찬 5가지'), (select tag_id from tag where name = '다이어트반찬'));
insert into tag (name) values ('도시락') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '닭가슴살 질릴 때 돌려 먹는 단백질 반찬 5가지'), (select tag_id from tag where name = '도시락'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')) and name = '홈트 루틴'), (select topic_id from topic where topic_name = '운동'), '20분 맨몸 서킷 루틴 (층간소음 걱정 없는 버전)', '아파트 살면서 제일 신경 쓰이는 게 **점프 동작**이에요. 버피 몇 번 하다가 아래층 생각나서 멈춘 적이 한두 번이 아니라, 아예 점프 없는 서킷을 짜서 쓰고 있어요. 요즘 주 3회 하는 루틴 그대로 공유합니다.

## 구성

**40초 운동 / 20초 휴식**, 6가지 동작을 한 바퀴로 해서 3바퀴. 바퀴 사이에는 1분 쉬어요. 준비운동 3분, 마무리 스트레칭까지 하면 딱 25분 정도 걸려요.

1. **스쿼트** – 천천히 내려가고 빠르게 올라오기
2. **푸시업** – 힘들면 무릎 대고
3. **리버스 런지** – 좌우 번갈아. 앞으로 나가는 런지보다 소리가 덜 나요
4. **플랭크 숄더탭** – 골반이 흔들리지 않게 발을 넓게
5. **글루트 브릿지** – 위에서 1초 버티기
6. **마운틴 클라이머 (느리게)** – 무릎을 가슴 쪽으로 당겼다가 **발을 바닥에 살짝 내려놓기**

## 소음 줄이는 팁

- 요가 매트 위에 **퍼즐 매트**를 한 겹 더 깔았어요.
- 양말 말고 맨발이나 실내용 운동화. 양말은 미끄러워서 오히려 쿵 소리가 나요.
- 동작 속도보다 **동작을 끝까지** 하는 데 집중하면 자연스럽게 조용해져요.

## 강도 조절

| 상황 | 바꾸는 방법 |
|---|---|
| 너무 쉬울 때 | 50초 / 10초로 바꾸기, 4바퀴 |
| 피곤한 날 | 2바퀴만, 대신 자세 꼼꼼히 |
| 하체가 뻐근한 날 | 런지 빼고 버드독 넣기 |

타이머는 무료 인터벌 타이머 앱 아무거나 써도 돼요. 저는 휴식 끝나기 3초 전에 삐 소리 나는 게 제일 편했어요.

> 20분이 짧아 보여도 제대로 하면 마지막 바퀴에서는 말이 안 나와요. 처음엔 2바퀴로 시작하세요.
', 'public', 810, '2025-03-16T21:20:00+09:00');
insert into tag (name) values ('서킷트레이닝') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '20분 맨몸 서킷 루틴 (층간소음 걱정 없는 버전)'), (select tag_id from tag where name = '서킷트레이닝'));
insert into tag (name) values ('층간소음') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '20분 맨몸 서킷 루틴 (층간소음 걱정 없는 버전)'), (select tag_id from tag where name = '층간소음'));
insert into tag (name) values ('20분운동') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '20분 맨몸 서킷 루틴 (층간소음 걱정 없는 버전)'), (select tag_id from tag where name = '20분운동'));
insert into tag (name) values ('홈트루틴') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '20분 맨몸 서킷 루틴 (층간소음 걱정 없는 버전)'), (select tag_id from tag where name = '홈트루틴'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')) and name = '운동 기록'), (select topic_id from topic where topic_name = '운동'), '헬스장 3개월 등록 후기: 홈트랑 뭐가 달랐나', '홈트 2년 하고 나니까 정체기가 확 왔어요. 맨몸으로는 하체에 자극 주기가 점점 어려워져서, 회사 근처 헬스장을 **3개월만** 끊어 봤습니다. 다 끝나서 솔직한 후기를 남겨요.

## 좋았던 점

- **하체 운동의 차원이 다르다.** 스쿼트 랙에서 빈 봉(20kg)부터 시작했는데, 맨몸 스쿼트 50개보다 빈 봉 스쿼트 10개가 훨씬 힘들었어요.
- **등 운동**이 드디어 됐어요. 집에서는 당기는 동작을 할 기구가 없어서 늘 아쉬웠는데, 랫풀다운이랑 시티드 로우로 등을 처음 제대로 써 봤습니다.
- 운동하러 **나왔다는 사실** 자체가 집중을 만들어 줘요. 집에서는 중간에 핸드폰을 보게 되거든요.

## 별로였던 점

- 퇴근 시간대(7~8시)에는 기구 기다리는 시간이 길었어요. 스쿼트 랙 한 대에 줄을 서요.
- 오가는 시간까지 하면 운동 1시간에 **총 1시간 40분**이 들어요.
- 월말 야근 주간엔 거의 못 가서 돈이 아까웠어요.

## 3개월 기록

| 종목 | 시작 | 3개월 후 |
|---|---|---|
| 백스쿼트 | 20kg × 10 | 60kg × 8 |
| 루마니안 데드리프트 | 30kg × 10 | 60kg × 10 |
| 랫풀다운 | 25kg × 10 | 45kg × 10 |

## 결론

헬스장은 연장 안 하고, 대신 **집에 턱걸이 바랑 밴드**를 들이기로 했어요. 3개월 동안 배운 건 "당기는 운동이 부족했다"는 거라서, 그 부분만 집에서 채워 볼 생각이에요.

> 홈트냐 헬스장이냐보다, 내가 **지금 빠뜨리고 있는 동작**이 뭔지 아는 게 더 중요했어요.
', 'public', 446, '2025-09-07T10:30:00+09:00');
insert into tag (name) values ('헬스장') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '헬스장 3개월 등록 후기: 홈트랑 뭐가 달랐나'), (select tag_id from tag where name = '헬스장'));
insert into tag (name) values ('웨이트') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '헬스장 3개월 등록 후기: 홈트랑 뭐가 달랐나'), (select tag_id from tag where name = '웨이트'));
insert into tag (name) values ('홈트비교') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '헬스장 3개월 등록 후기: 홈트랑 뭐가 달랐나'), (select tag_id from tag where name = '홈트비교'));
insert into tag (name) values ('후기') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '헬스장 3개월 등록 후기: 홈트랑 뭐가 달랐나'), (select tag_id from tag where name = '후기'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')) and name = '운동 기록'), (select topic_id from topic where topic_name = '운동'), '문틀 턱걸이 바 달고 첫 턱걸이 성공하기까지', '작년 가을에 문틀용 턱걸이 바를 샀어요. 처음 매달렸을 때는 **팔이 1cm도 안 굽혀졌어요.** 그로부터 7개월, 오늘 드디어 반동 없이 턱걸이 1개를 했습니다. 너무 기뻐서 바로 기록 남겨요.

## 바 고를 때 확인한 것

- 문틀 **폭과 두께**부터 재기. 제 방 문은 압축봉 방식이 안 맞아서 문틀에 걸치는 방식으로 샀어요.
- 걸치는 방식은 문틀 위쪽 몰딩이 약하면 자국이 남아요. 고무 패드 꼭 끼우기.
- 처음 쓸 때는 바닥에 발 닿는 높이에서 체중을 조금씩 실어 보면서 확인했어요.

## 했던 순서

### 1. 매달리기 (5주)
- **데드 행**: 그냥 매달려 버티기. 처음엔 15초 버티면 손이 풀렸어요.
- 목표 40초 × 3세트까지 늘렸어요.

### 2. 스캐퓰러 풀업 (6주)
매달린 상태에서 팔은 편 채로 **어깨뼈만 아래로 끌어내리기**. 몸이 2~3cm 올라가는 동작이에요. 처음엔 이게 뭐가 운동인가 싶었는데, 등 근육 쓰는 감각이 여기서 생겼어요.

### 3. 밴드 풀업 (8주)
밴드를 바에 걸고 발이나 무릎을 걸어서 보조받기. 두꺼운 밴드에서 얇은 밴드로 바꾸는 데 한 달 넘게 걸렸어요.

### 4. 네거티브 풀업 (8주)
의자 밟고 올라가서 턱을 바 위에 둔 상태로 시작, **5초 동안 천천히 내려오기** × 5개. 이게 제일 효과가 컸어요. 푸시업 때도 네거티브 덕을 봤는데 똑같더라고요.

## 오늘 상황

네거티브 하다가 장난으로 한번 당겨 봤는데 올라갔어요. 영상 찍어 둘 걸 그랬습니다.

> 3년 전 푸시업 0개였던 사람도 턱걸이 1개는 됩니다. 다음 목표는 연속 3개!
', 'public', 481, '2026-04-19T21:00:00+09:00');
insert into tag (name) values ('턱걸이') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '문틀 턱걸이 바 달고 첫 턱걸이 성공하기까지'), (select tag_id from tag where name = '턱걸이'));
insert into tag (name) values ('풀업') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '문틀 턱걸이 바 달고 첫 턱걸이 성공하기까지'), (select tag_id from tag where name = '풀업'));
insert into tag (name) values ('맨몸운동') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '문틀 턱걸이 바 달고 첫 턱걸이 성공하기까지'), (select tag_id from tag where name = '맨몸운동'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')) and name = '홈트 루틴'), (select topic_id from topic where topic_name = '운동'), '여름 휴가 중 호텔방에서 한 15분 루틴', '4박 5일로 남쪽 바닷가 쪽에 휴가를 다녀왔어요. 놀러 가서까지 운동하냐는 소리를 들었지만, 매일 많이 먹고 많이 앉아 있으니까 오히려 몸이 찌뿌둥해서 **아침 15분**만 하기로 했어요.

## 조건

- 매트 없음 → 수건 한 장 깔기
- 기구 없음 → 의자, 침대 모서리, 물병만
- 아래층 손님 생각해서 점프 없음

## 루틴 (45초 운동 / 15초 휴식, 2바퀴)

1. **불가리안 스플릿 스쿼트** – 뒷발을 침대 모서리에. 좌우 따로 45초씩
2. **의자 딥스** – 의자 앞에 앉아 손 짚고 내려갔다 올라오기
3. **인클라인 푸시업** – 책상 짚고. 낮은 의자가 있으면 디클라인으로
4. **싱글 레그 글루트 브릿지** – 좌우 번갈아
5. **사이드 플랭크** – 좌우 20초씩
6. **버드독** – 천천히 10회씩

호텔 의자는 바퀴 달린 게 많아서 딥스할 때는 **벽에 붙여 놓고** 했어요. 바퀴 의자는 진짜 위험해요.

## 해 보니

- 아침에 15분 하고 나니 하루 종일 덜 피곤했어요. 걷는 일정이 많아서 그런지 하체가 금방 풀렸어요.
- 그래도 마지막 날은 그냥 쉬었습니다. 휴가니까요.
- 바닷가 산책로에서 해 질 녘에 걷는 게 사실 제일 좋은 운동이었어요.

> 여행지에서는 기록 욕심 버리고 **몸만 깨운다**는 느낌이면 충분한 것 같아요.
', 'public', 278, '2026-08-23T08:45:00+09:00');
insert into tag (name) values ('여행운동') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '여름 휴가 중 호텔방에서 한 15분 루틴'), (select tag_id from tag where name = '여행운동'));
insert into tag (name) values ('호텔운동') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '여름 휴가 중 호텔방에서 한 15분 루틴'), (select tag_id from tag where name = '호텔운동'));
insert into tag (name) values ('15분루틴') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '여름 휴가 중 호텔방에서 한 15분 루틴'), (select tag_id from tag where name = '15분루틴'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')) and name = '운동 기록'), (select topic_id from topic where topic_name = '운동'), '홈트 3년 차 회고: 그만두지 않을 수 있었던 이유', '2023년 4월에 블로그를 시작했으니 이번 가을로 홈트 3년 반이에요. 중간에 헬스장도 다녀 보고, 한 달씩 쉰 적도 있지만 결국 다시 매트로 돌아왔어요. 오늘은 숫자보다 **왜 계속할 수 있었는지**를 정리해 보려고요.

## 숫자로 보면

- 체중: 처음 대비 -7kg (마지막 1년은 거의 그대로)
- 푸시업: 0개 → 연속 35개
- 턱걸이: 0개 → 연속 3개 (드디어!)
- 기록장 기준 운동한 날: 대략 주 3.4회

## 계속할 수 있었던 이유

### 1. 기준을 낮게 잡았다
"매일 1시간"이 아니라 **"매트 펴기"** 가 목표였어요. 매트만 펴면 대부분 15분은 하게 돼요.

### 2. 안 한 날도 적었다
기록장에 빈칸을 두지 않았어요. "야근", "두통", "그냥 싫음"까지 다 적었는데, 이게 쌓이니까 **어느 요일에 잘 빠지는지**가 보였어요. 저는 목요일이 제일 약하더라고요. 그래서 목요일은 스트레칭만 하는 날로 아예 바꿨어요.

### 3. 목표를 동작으로 잡았다
체중 목표는 정체기가 오면 힘이 빠지는데, "턱걸이 1개" 같은 목표는 조금씩 다가가는 게 눈에 보여서 오래 버틸 수 있었어요.

### 4. 쉬는 걸 실패로 안 봤다
작년 겨울에 감기로 3주를 쉬었는데, 예전 같으면 거기서 끝났을 거예요. 이번엔 **첫 주 루틴으로 돌아가서** 다시 시작했어요.

## 앞으로

- 4년 차 목표: 한 팔 푸시업 연습 시작, 피스톨 스쿼트 보조 없이 1개
- 식단은 지금처럼 일요일 반찬 만들기 유지

> 3년 전의 저처럼 계단 한 층에 숨이 찬다면, 오늘 매트만 한번 펴 보세요. 그걸로 충분해요.
', 'public', 233, '2026-10-05T21:30:00+09:00');
insert into tag (name) values ('회고') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '홈트 3년 차 회고: 그만두지 않을 수 있었던 이유'), (select tag_id from tag where name = '회고'));
insert into tag (name) values ('운동습관') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '홈트 3년 차 회고: 그만두지 않을 수 있었던 이유'), (select tag_id from tag where name = '운동습관'));
insert into tag (name) values ('홈트3년') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '홈트 3년 차 회고: 그만두지 않을 수 있었던 이유'), (select tag_id from tag where name = '홈트3년'));

-- 먹자골목 · 먹자골목 탐방 일지
insert into blog (users_id, name, intro, comments_read_at) values ((select users_id from users where email = 'demo.mukja@example.com'), '먹자골목 탐방 일지', '걸어서 갈 수 있는 동네 맛집, 수십 년 자리를 지킨 노포, 가끔 카페까지. 내 돈 내고 먹은 집만, 좋은 점과 아쉬운 점을 같이 적어요.', '2026-10-11T23:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), '미분류', 'public', true, 1, 0, '2023-06-02T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), '동네 맛집', 'public', false, 2, 1, '2023-06-02T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), '노포 탐방', 'public', false, 3, 2, '2023-06-02T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), '카페', 'public', false, 4, 3, '2023-06-02T10:00:00+09:00');
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')) and name = '동네 맛집'), (select topic_id from topic where topic_name = '음식'), '우리 동네 ''소문난손칼국수'', 겉절이가 다 한 집 솔직 후기', '블로그 처음 열고 첫 글은 무조건 여기로 쓰겠다고 정해 뒀어요. 이사 온 지 3년째인데, 한 달에 최소 두 번은 가는 동네 칼국수집이에요.

## 기본 정보

- 메뉴: 바지락칼국수 9,000원 / 들깨칼국수 10,000원 / 보리밥 (칼국수 시키면 작은 그릇 무료)
- 자리: 테이블 6개, 점심엔 10분 정도 대기
- 주차: 없어요. 골목이라 걸어오세요

## 칼국수

바지락칼국수 기준으로 말하면, **면은 평범**해요. 손으로 밀었다고 하는데 두께가 고르지 않아서 어떤 건 퍼지고 어떤 건 덜 익은 느낌이 있어요. 국물은 바지락이 꽤 들어가서 시원하긴 한데 간이 조금 세요. 물을 한 번 더 부탁드리면 맞춰 주세요.

## 그런데 겉절이가

이 집을 계속 가는 이유는 솔직히 **겉절이** 때문이에요.

- 매일 아침에 버무린다고 하셨어요
- 배추가 아삭하고 고춧가루가 굵어서 씹을 때 향이 확 올라와요
- 젓갈 맛은 약하고, 마늘이랑 생강이 세요
- 달라고 하면 리필 계속 해 주세요

칼국수 한 젓가락에 겉절이 하나. 이 조합으로 먹으면 앞에서 말한 단점이 다 덮여요.

## 점수 (제 기준 5점 만점)

| 항목 | 점수 |
|---|---|
| 칼국수 | 3.0 |
| 겉절이 | 5.0 |
| 가격 | 4.0 |
| 다시 갈 의향 | 4.5 |

> 동네 맛집은 ''완벽한 집''이 아니라 ''자꾸 생각나는 한 가지''가 있는 집이라고 생각해요.

다음엔 들깨칼국수로 다시 써 볼게요.', 'public', 1124, '2023-06-17T20:35:00+09:00');
insert into tag (name) values ('칼국수') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '우리 동네 ''소문난손칼국수'', 겉절이가 다 한 집 솔직 후기'), (select tag_id from tag where name = '칼국수'));
insert into tag (name) values ('동네맛집') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '우리 동네 ''소문난손칼국수'', 겉절이가 다 한 집 솔직 후기'), (select tag_id from tag where name = '동네맛집'));
insert into tag (name) values ('겉절이') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '우리 동네 ''소문난손칼국수'', 겉절이가 다 한 집 솔직 후기'), (select tag_id from tag where name = '겉절이'));
insert into tag (name) values ('솔직후기') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '우리 동네 ''소문난손칼국수'', 겉절이가 다 한 집 솔직 후기'), (select tag_id from tag where name = '솔직후기'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')) and name = '노포 탐방'), (select topic_id from topic where topic_name = '음식'), '을지로 40년 된 연탄 돼지갈비 노포 ''원조연탄갈비'' 솔직 후기', '회사 동기들이랑 금요일 저녁에 을지로 노포 투어를 했어요. 첫 집이 여기였는데, 사실 이 집 하나로 끝났어요.

## 분위기

- 골목 안쪽, 간판 글씨가 반쯤 지워져 있어요
- 바깥에 드럼통 테이블 5개, 안에 좌식 3개
- 사장님 내외분이 40년 넘게 하셨대요. 벽에 걸린 메뉴판 가격 위에 종이를 몇 번이나 덧붙인 흔적이 있어요

**저녁 6시 반에 갔는데 이미 바깥 자리는 꽉** 찼고 20분 기다렸어요.

## 메뉴와 가격

- 돼지갈비 1인분 (250g) 16,000원
- 공깃밥 1,000원, 된장찌개 3,000원
- 소주·맥주 5,000원

## 맛

**양념이 달지 않아요.** 요즘 돼지갈비가 대부분 꿀 넣은 것처럼 단데, 여기는 간장 맛이 먼저 오고 끝에 살짝 단맛이 따라와요. 연탄불에 구워서 겉에 불맛이 확실히 배요.

다만 솔직히 말하면,

- 고기 두께가 고르지 않아요. 얇은 부분은 금방 타요
- 사장님이 직접 구워 주시는 게 아니라서, 연탄 불 조절을 못하면 반은 태워요
- 반찬은 파절이, 김치, 마늘 끝. 간소해요

## 된장찌개

진짜 숨은 메뉴. 3천 원인데 고기 구운 기름이 조금 들어간 것처럼 구수해요. 고기 다 먹고 밥 한 공기에 이거 하나면 마무리 완벽.

> 노포의 맛은 레시피보다 ''그 자리에서 40년 버틴 시간''이 반은 만드는 것 같아요.

다음엔 사람 적은 평일 이른 저녁에 가 볼 생각이에요. 연탄 불 조절 연습하고 가야겠어요.', 'public', 1099, '2023-11-25T21:58:00+09:00');
insert into tag (name) values ('을지로') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '을지로 40년 된 연탄 돼지갈비 노포 ''원조연탄갈비'' 솔직 후기'), (select tag_id from tag where name = '을지로'));
insert into tag (name) values ('노포') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '을지로 40년 된 연탄 돼지갈비 노포 ''원조연탄갈비'' 솔직 후기'), (select tag_id from tag where name = '노포'));
insert into tag (name) values ('돼지갈비') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '을지로 40년 된 연탄 돼지갈비 노포 ''원조연탄갈비'' 솔직 후기'), (select tag_id from tag where name = '돼지갈비'));
insert into tag (name) values ('연탄구이') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '을지로 40년 된 연탄 돼지갈비 노포 ''원조연탄갈비'' 솔직 후기'), (select tag_id from tag where name = '연탄구이'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')) and name = '카페'), (select topic_id from topic where topic_name = '음식'), '망원동 작은 카페 ''오후세시'' 디저트 3종 비교 후기', '토요일 오후, 망원시장 구경하고 근처 골목을 걷다가 들어간 카페예요. 이름이 ''오후세시''인데 실제로 들어간 시간이 3시 5분이라 괜히 웃겼어요.

## 공간

- 테이블 4개, 창가 자리 2개
- 원목 가구랑 하얀 벽, 음악은 작게
- 노트북 작업하는 분은 없고 다들 수다 떠는 분위기

## 디저트 3종

같이 간 친구랑 하나씩 다 시켜서 나눠 먹었어요.

### 1. 까눌레 (4,000원)

겉이 **진짜 단단하게 캐러멜화**돼 있어요. 포크로 자르면 ''딱'' 소리 날 정도. 속은 촉촉하고 바닐라 향이 진해요. 셋 중 1등.

### 2. 얼그레이 파운드 (5,500원)

향은 좋은데 조금 퍽퍽했어요. 커피 없이 먹으면 목이 막혀요. 크림을 곁들여 주면 좋겠다는 생각.

### 3. 딸기 쇼트케이크 (7,500원)

3월이라 딸기가 실하고 달았어요. 근데 생크림이 너무 가벼워서 금방 녹아내려요. 사진 찍을 시간이 없어요ㅎㅎ

## 커피

라떼 5,000원. 원두는 산미 있는 쪽이랑 고소한 쪽 중에 고를 수 있는데, 저는 고소한 쪽. 디저트가 달아서 산미 있는 걸로 했으면 더 잘 어울렸을 것 같아요.

## 정리

| 메뉴 | 한 줄 평 |
|---|---|
| 까눌레 | 이거 먹으러 또 감 |
| 얼그레이 파운드 | 커피 필수 |
| 딸기 쇼트케이크 | 맛있지만 빨리 먹을 것 |

> 카페 디저트는 하나만 확실하면 다시 가게 돼요. 여기는 까눌레가 그 하나.', 'public', 785, '2024-03-09T17:12:00+09:00');
insert into tag (name) values ('망원동') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '망원동 작은 카페 ''오후세시'' 디저트 3종 비교 후기'), (select tag_id from tag where name = '망원동'));
insert into tag (name) values ('카페') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '망원동 작은 카페 ''오후세시'' 디저트 3종 비교 후기'), (select tag_id from tag where name = '카페'));
insert into tag (name) values ('디저트') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '망원동 작은 카페 ''오후세시'' 디저트 3종 비교 후기'), (select tag_id from tag where name = '디저트'));
insert into tag (name) values ('까눌레') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '망원동 작은 카페 ''오후세시'' 디저트 3종 비교 후기'), (select tag_id from tag where name = '까눌레'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')) and name = '노포 탐방'), (select topic_id from topic where topic_name = '음식'), '대전 중앙시장 순댓국 노포, 줄 서서 먹을 만한가', '대전 출장 끝나고 기차 시간이 2시간 남아서 대전역에서 걸어서 10분 거리인 중앙시장에 갔어요. 시장 안에 50년 됐다는 순댓국집 ''할매순대국''이 있다길래.

## 대기

- 토요일 오전 11시 40분 도착
- 앞에 8팀, 기다린 시간 25분
- 회전은 빨라요. 다들 먹고 바로 일어나요

## 메뉴

- 순댓국 9,000원 (특 11,000원)
- 순대 한 접시 12,000원
- 머리고기 수육 소 15,000원

혼자라서 순댓국 특으로 시켰어요.

## 맛

**국물이 맑아요.** 서울에서 먹던 뽀얗고 진한 국물이 아니라, 맑고 깔끔한 쪽이에요. 처음 한 숟갈은 ''어, 싱겁네'' 싶었는데 들깨가루랑 새우젓을 넣으니까 맛이 확 살아나요.

- 순대: 당면보다 야채와 선지가 많은 스타일. 맛있어요
- 머리고기: 부드러운데 양이 조금 아쉬워요
- 깍두기: 시큼하게 잘 익었어요. 국밥집 깍두기는 이래야죠

## 솔직한 평가

**줄 서서 먹을 만한가?** — 25분이면 괜찮아요. 근데 40분 넘으면 고민해 볼 것 같아요.

맛은 확실히 좋은데, ''인생 순댓국''까지는 아니었어요. 대신 이 시장 분위기, 옆자리 할아버지들이 소주 반 병씩 두고 이야기 나누시는 그 풍경까지 합치면 충분히 갈 만해요.

> 맑은 순댓국은 첫맛보다 마지막 한 숟갈이 더 맛있어요.

기차 시간 20분 남기고 나와서 역 앞 빵집에서 소보로빵 하나 사서 기차에서 먹었어요. 이건 반칙이죠.', 'public', 1250, '2024-09-21T13:47:00+09:00');
insert into tag (name) values ('대전') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '대전 중앙시장 순댓국 노포, 줄 서서 먹을 만한가'), (select tag_id from tag where name = '대전'));
insert into tag (name) values ('순댓국') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '대전 중앙시장 순댓국 노포, 줄 서서 먹을 만한가'), (select tag_id from tag where name = '순댓국'));
insert into tag (name) values ('노포') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '대전 중앙시장 순댓국 노포, 줄 서서 먹을 만한가'), (select tag_id from tag where name = '노포'));
insert into tag (name) values ('시장맛집') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '대전 중앙시장 순댓국 노포, 줄 서서 먹을 만한가'), (select tag_id from tag where name = '시장맛집'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')) and name = '동네 맛집'), (select topic_id from topic where topic_name = '음식'), '동네 중국집 짜장면 4곳 비교 정리 (배달 말고 매장)', '겨울 내내 주말마다 동네 중국집을 하나씩 돌았어요. 규칙은 단 하나, **배달 말고 매장에서, 기본 짜장면만.** 걸어서 15분 안쪽 4곳이에요. 가게 이름은 동네 분들은 아실 테니 별명으로 적을게요.

## 1. 큰길 ''홍성반점'' — 6,500원

- 면: 손으로 뽑은 수타면. 두께가 제각각인데 쫄깃함은 최고
- 소스: 감자랑 양파가 큼직하고, 단맛보다 춘장 맛이 진해요
- 단점: 주말엔 대기 15분

## 2. 시장 옆 ''만리성'' — 6,000원

- 면: 기계면. 부드럽고 무난
- 소스: **제일 달아요.** 아이들 입맛
- 장점: 양이 많고 빨리 나와요

## 3. 골목 ''금룡각'' — 7,000원

- 면: 기계면인데 삶기가 정확해요. 퍼짐이 없어요
- 소스: 돼지고기가 잘게 다져져서 많이 들어가요. 고기 맛이 확실
- 단점: 테이블이 3개뿐

## 4. 새로 생긴 ''청화원'' — 8,000원

- 면: 수타
- 소스: 춘장을 직접 볶는다고 하는데 확실히 향이 달라요. 불맛도 있어요
- 단점: 가격이 제일 비싸고, 양은 제일 적어요

## 순위

| 순위 | 가게 | 한 줄 평 |
|---|---|---|
| 1 | 금룡각 | 면 삶기와 고기, 기본기가 다르다 |
| 2 | 홍성반점 | 수타면 좋아하면 1등 |
| 3 | 청화원 | 맛은 좋은데 가격이 아쉬움 |
| 4 | 만리성 | 배고플 때 빠르고 든든하게 |

> 짜장면은 결국 ''면을 언제 건져 내느냐''의 싸움 같아요. 1등 집은 그걸 정확히 알아요.

다음 겨울 시리즈는 짬뽕으로 해 볼까 해요.', 'public', 990, '2025-02-15T19:30:00+09:00');
insert into tag (name) values ('짜장면') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '동네 중국집 짜장면 4곳 비교 정리 (배달 말고 매장)'), (select tag_id from tag where name = '짜장면'));
insert into tag (name) values ('중국집') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '동네 중국집 짜장면 4곳 비교 정리 (배달 말고 매장)'), (select tag_id from tag where name = '중국집'));
insert into tag (name) values ('동네맛집') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '동네 중국집 짜장면 4곳 비교 정리 (배달 말고 매장)'), (select tag_id from tag where name = '동네맛집'));
insert into tag (name) values ('비교') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '동네 중국집 짜장면 4곳 비교 정리 (배달 말고 매장)'), (select tag_id from tag where name = '비교'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')) and name = '카페'), (select topic_id from topic where topic_name = '음식'), '성수 웨이팅 카페 두 곳, 솔직히 한 곳만 추천해요', '요즘 성수는 카페 하나 가려면 웨이팅이 기본이에요. 지난 토요일에 SNS에서 유명한 두 곳을 연달아 가 봤어요. 결론부터 말하면 **한 곳만 추천**해요.

## A. 창고형 대형 카페 ''그레이웨어하우스''

- 대기: 오후 1시 도착, 원격 줄서기로 50분
- 메뉴: 시그니처 크림라떼 7,500원, 소금빵 4,500원
- 좌석: 넓은데 거의 다 계단식 좌석이라 오래 앉아 있기 불편

**맛**: 크림라떼는 크림이 달고 진해서 첫 모금은 맛있는데, 반쯤 마시면 물려요. 소금빵은 버터 향은 좋은데 식어서 나왔어요.

**솔직히**: 공간 사진 찍으러 가는 곳이에요. 커피나 빵 맛으로는 50분 기다릴 이유가 없었어요.

## B. 골목 안 작은 로스터리 ''한잔의온도''

- 대기: 오후 3시 도착, 현장 대기 15분
- 메뉴: 핸드드립 6,000원, 무화과 타르트 6,500원
- 좌석: 8석 바 테이블

**맛**: 핸드드립은 원두를 세 가지 중에 골라요. 저는 에티오피아 쪽 꽃향 나는 원두로 했는데, 바리스타님이 내려 주시면서 물 온도를 92도로 맞췄다고 알려 주셨어요. 향이 컵 밖까지 퍼져요. 타르트는 무화과가 반으로 잘려서 통째로 올라가 있고, 아래 크림이 달지 않아 커피랑 잘 맞았어요.

**솔직히**: 여기는 기다릴 만해요.

## 정리

| | A | B |
|---|---|---|
| 대기 | 50분 | 15분 |
| 커피 | 3/5 | 5/5 |
| 디저트 | 2.5/5 | 4.5/5 |
| 다시 갈 의향 | 없음 | 매우 있음 |

> 카페는 결국 커피 맛. 공간이 아무리 예뻐도 두 번째 방문은 맛이 결정해요.', 'public', 542, '2025-06-28T21:20:00+09:00');
insert into tag (name) values ('성수동') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '성수 웨이팅 카페 두 곳, 솔직히 한 곳만 추천해요'), (select tag_id from tag where name = '성수동'));
insert into tag (name) values ('카페') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '성수 웨이팅 카페 두 곳, 솔직히 한 곳만 추천해요'), (select tag_id from tag where name = '카페'));
insert into tag (name) values ('웨이팅') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '성수 웨이팅 카페 두 곳, 솔직히 한 곳만 추천해요'), (select tag_id from tag where name = '웨이팅'));
insert into tag (name) values ('솔직후기') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '성수 웨이팅 카페 두 곳, 솔직히 한 곳만 추천해요'), (select tag_id from tag where name = '솔직후기'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')) and name = '노포 탐방'), (select topic_id from topic where topic_name = '음식'), '부산 국밥 노포 수육백반, 겨울이면 생각나는 그 집', '첫눈 소식 들리자마자 생각난 집. 작년 겨울 부산 출장 때 현지 거래처 분이 데려가 주신 범일동 쪽 노포 ''대성돼지국밥''이에요. 이번엔 일부러 주말에 혼자 내려갔어요.

## 가게

- 부산 지하철 1호선 범일역에서 걸어서 7분
- 1970년대부터 했다는데, 지금은 아드님이 이어서 한대요
- 가마솥 두 개에서 하루 종일 국물을 끓여요. 문 열면 김이 확 나와요

## 수육백반 (13,000원)

이 집은 국밥보다 **수육백반**을 추천받았어요.

- 수육 한 접시: 앞다리살이랑 항정살 섞어서
- 맑은 국물 한 그릇: 순수 육수, 건더기 없이
- 밥, 부추무침, 김치, 새우젓, 마늘, 고추

수육이 정말 부드러워요. 비계 부분이 녹아내리는데 느끼하지 않아요. **새우젓에 살짝 찍어서 부추무침이랑 같이** 먹으면 이게 끝이에요.

국물은 맑고 담백해요. 처음엔 소금 안 넣고 마시다가, 반쯤부터 부추무침을 한 줌 넣어서 먹었어요. 국물 맛이 바뀌어요.

## 아쉬운 점

- 김치가 많이 익어서 신맛이 강해요. 호불호 갈릴 듯
- 오후 3시 넘으면 수육이 떨어지는 날이 있대요. 점심에 가세요

## 비교: 서울 돼지국밥

서울에서 먹던 돼지국밥은 국물이 뽀얗고 진한 경우가 많은데, 부산 노포는 오히려 **맑은 쪽**이 많더라고요. 사장님 말로는 "뽀얀 건 뼈를 오래 고아서, 맑은 건 고기로만 내서"래요.

> 추운 날 가마솥 김 속으로 들어가는 그 순간이 노포의 맛 절반이에요.

부산까지 국밥 한 그릇 먹으러 가는 게 사치 같지만, 1년에 한 번은 괜찮잖아요.', 'public', 486, '2025-12-06T22:15:00+09:00');
insert into tag (name) values ('부산') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '부산 국밥 노포 수육백반, 겨울이면 생각나는 그 집'), (select tag_id from tag where name = '부산'));
insert into tag (name) values ('돼지국밥') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '부산 국밥 노포 수육백반, 겨울이면 생각나는 그 집'), (select tag_id from tag where name = '돼지국밥'));
insert into tag (name) values ('수육백반') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '부산 국밥 노포 수육백반, 겨울이면 생각나는 그 집'), (select tag_id from tag where name = '수육백반'));
insert into tag (name) values ('노포') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '부산 국밥 노포 수육백반, 겨울이면 생각나는 그 집'), (select tag_id from tag where name = '노포'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')) and name = '노포 탐방'), (select topic_id from topic where topic_name = '여행'), '전주 1박 하면서 먹은 것만 정리 (한옥마을 밖 위주)', '전주는 세 번째인데 이번엔 일부러 **한옥마을 밖**에서만 먹기로 했어요. 토요일 오전 용산역에서 KTX 1시간 40분.

## 토요일

### 점심 — 남부시장 콩나물국밥

유명한 큰 집 말고, 시장 2층 구석 작은 가게 ''남부콩나물''. 8,000원.

- **남부시장식**: 펄펄 끓이지 않고 따로 데운 국물에 밥을 말아 줘요
- 수란이 따로 나오는데, 국물 몇 숟갈 넣고 김 부숴서 먹는 게 정석이래요
- 오징어 조금 추가 (2,000원) 추천

### 오후 간식 — 시장 안 ''호떡집''

찹쌀 호떡 1,500원. 기름에 튀기듯 굽는 스타일이라 겉이 바삭해요.

### 저녁 — 가맥

전주 하면 **가맥**(가게 맥주)이죠. 동네 슈퍼에서 맥주 시키고 안주를 먹는 곳이에요. 저는 숙소 근처 ''다정슈퍼''라는 작은 가게에 갔어요.

- 병맥주 3,000원
- 황태구이 12,000원 — 이게 진짜예요. 연탄에 살짝 구워서 찢어 주고, 간장 마요 소스에 찍어 먹어요
- 계란말이 8,000원

슈퍼 앞 플라스틱 의자에서 먹는데, 옆 테이블이 다 동네 분들이에요.

## 일요일

### 아침 — 피순대

유명한 집 대신 시장 근처 작은 피순대집 ''순이네''. 피순대 반 접시 10,000원. 선지가 들어가서 진하고 부드러워요. 호불호 있어요. 저는 호.

### 기차 타기 전 — 물짜장

전주 특유의 ''물짜장''. 춘장 대신 간장 베이스에 해산물 넣고 전분으로 걸쭉하게 만든 거예요. 짜장이라기보다 해물 우동 덮밥 느낌. 신기해서 먹어 볼 만해요.

## 1박 식비

약 7만 원 (가맥 포함). 한옥마을 안에서 먹었으면 두 배는 나왔을 거예요.

> 여행지 맛집은 관광지 반 블록 밖에 있다. 이번에도 맞았어요.', 'public', 465, '2026-04-18T21:40:00+09:00');
insert into tag (name) values ('전주') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '전주 1박 하면서 먹은 것만 정리 (한옥마을 밖 위주)'), (select tag_id from tag where name = '전주'));
insert into tag (name) values ('먹방여행') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '전주 1박 하면서 먹은 것만 정리 (한옥마을 밖 위주)'), (select tag_id from tag where name = '먹방여행'));
insert into tag (name) values ('콩나물국밥') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '전주 1박 하면서 먹은 것만 정리 (한옥마을 밖 위주)'), (select tag_id from tag where name = '콩나물국밥'));
insert into tag (name) values ('가맥') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '전주 1박 하면서 먹은 것만 정리 (한옥마을 밖 위주)'), (select tag_id from tag where name = '가맥'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')) and name = '동네 맛집'), (select topic_id from topic where topic_name = '음식'), '회사 근처 백반집 한 달 출석 기록, 매일 바뀌는 반찬 정리', '9월 한 달 동안 점심을 회사 근처 백반집 ''정든밥상''에서만 먹었어요. 동료들이 "질리지 않냐"고 했는데, 반찬이 매일 바뀌어서 하나도 안 질렸어요.

## 기본

- 백반 9,000원 (올해 초 8,000원에서 올랐어요)
- 밥, 국, 반찬 6가지, 메인 1가지
- 국이랑 메인은 매일 바뀌고, 반찬도 절반은 바뀌어요
- 12시 10분 넘으면 자리 없어요. 11시 50분 도착이 정답

## 한 달 동안 나온 메인 (기억나는 것)

- 제육볶음 (4번) — 이 집 메인 중 1등. 불맛 있어요
- 고등어구이 (3번)
- 오징어볶음 (2번)
- 돈가스 (금요일에 2번) — 수제라 두꺼워요
- 닭볶음탕, 동그랑땡, 두부조림, 코다리찜 등

## 국

된장국, 미역국, 콩나물국, 김치찌개, 어묵탕 돌아가면서. **목요일 김치찌개**는 거의 고정이었어요.

## 반찬 중 베스트 3

1. **깻잎지** — 양념이 달지 않고 짭짤해서 밥도둑
2. **멸치볶음** — 아몬드 넣어서 고소해요
3. **가지무침** — 9월 초에만 나왔는데 더 먹고 싶었어요

## 한 달 결산

| 항목 | 내용 |
|---|---|
| 출석 | 21일 |
| 점심값 합계 | 189,000원 |
| 편의점 대비 | 하루 2~3천 원 더 쓰지만 만족도는 훨씬 높음 |
| 체중 변화 | -0.8kg (오히려 빠짐) |

사장님이 2주쯤 지나니까 제 얼굴을 알아보시고 밥을 조금 더 퍼 주셨어요. 마지막 날엔 "다음 달에도 와요" 하시길래 네 하고 나왔어요.

> 매일 가는 밥집이 있다는 건, 매일 하루가 조금 덜 낯설다는 것.

10월에도 계속 출석할 예정이에요.', 'public', 226, '2026-09-19T20:55:00+09:00');
insert into tag (name) values ('백반') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '회사 근처 백반집 한 달 출석 기록, 매일 바뀌는 반찬 정리'), (select tag_id from tag where name = '백반'));
insert into tag (name) values ('점심') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '회사 근처 백반집 한 달 출석 기록, 매일 바뀌는 반찬 정리'), (select tag_id from tag where name = '점심'));
insert into tag (name) values ('직장인') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '회사 근처 백반집 한 달 출석 기록, 매일 바뀌는 반찬 정리'), (select tag_id from tag where name = '직장인'));
insert into tag (name) values ('가성비') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '회사 근처 백반집 한 달 출석 기록, 매일 바뀌는 반찬 정리'), (select tag_id from tag where name = '가성비'));

-- 베란다집사 · 초록 베란다 일지
insert into blog (users_id, name, intro, comments_read_at) values ((select users_id from users where email = 'demo.plant@example.com'), '초록 베란다 일지', '베란다에서 반려식물을 키우며 배운 것들을 기록해요. 물주기와 분갈이, 흙 배합, 그리고 솔직한 실패담까지. 식물 초보도 편하게 읽을 수 있게 씁니다.', '2026-10-11T23:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), '미분류', 'public', true, 1, 0, '2023-05-06T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), '키우는 식물', 'public', false, 2, 1, '2023-05-06T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), '분갈이·흙', 'public', false, 3, 2, '2023-05-06T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), '초보 가이드', 'public', false, 4, 3, '2023-05-06T10:00:00+09:00');
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')) and name = '분갈이·흙'), (select topic_id from topic where topic_name = '취미'), '몬스테라 첫 분갈이, 뿌리가 화분 밖으로 나왔어요', '작년 겨울 꽃집에서 잎 세 장짜리로 데려온 몬스테라가 어느새 잎이 일곱 장이 됐어요. 그런데 며칠 전 보니 **화분 배수 구멍으로 뿌리가 삐져나와** 있더라고요. 분갈이 신호라고 해서 주말에 처음 도전해 봤습니다.

## 준비물

- 기존보다 **지름 3cm 큰** 플라스틱 화분 (15cm → 18cm)
- 분갈이용 상토 5L
- 펄라이트 한 줌, 마사토 조금 (배수층용)
- 신문지, 나무젓가락, 장갑

## 순서

1. 이틀 전에 물을 줘서 흙이 살짝 촉촉한 상태로 만들었어요. 너무 마르면 흙이 부서지고, 너무 젖으면 뿌리가 끊어진대요.
2. 화분 옆을 손으로 꾹꾹 눌러서 흙덩이째 빼냈어요.
3. 뿌리가 화분 모양대로 빙글빙글 돌고 있어서, **아래쪽 뿌리를 손가락으로 살살 풀어** 줬어요.
4. 새 화분 바닥에 마사토 2cm, 그 위에 상토+펄라이트를 섞어 깔고 몬스테라를 앉혔어요.
5. 빈 곳을 흙으로 채우고 나무젓가락으로 콕콕 찔러서 빈 공간을 없앴어요.
6. 배수 구멍으로 물이 흘러나올 때까지 흠뻑 물주기.

## 분갈이 후

> 분갈이 직후에는 직사광선을 피하고 일주일 정도 그늘에서 쉬게 해 주세요.

꽃집 사장님 말씀대로 베란다 안쪽 반그늘에 두었어요. 이틀째 잎 하나가 살짝 처졌는데 나흘째부터 다시 빳빳해졌어요. 휴, 첫 분갈이 성공입니다!', 'public', 1302, '2023-05-20T16:30:00+09:00');
insert into tag (name) values ('몬스테라') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '몬스테라 첫 분갈이, 뿌리가 화분 밖으로 나왔어요'), (select tag_id from tag where name = '몬스테라'));
insert into tag (name) values ('분갈이') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '몬스테라 첫 분갈이, 뿌리가 화분 밖으로 나왔어요'), (select tag_id from tag where name = '분갈이'));
insert into tag (name) values ('식물집사') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '몬스테라 첫 분갈이, 뿌리가 화분 밖으로 나왔어요'), (select tag_id from tag where name = '식물집사'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')) and name = '초보 가이드'), (select topic_id from topic where topic_name = '취미'), '초보 식물집사가 첫 1년 동안 보낸 식물들', '식물 블로그라고 하면 잘 키운 사진만 올라오지만, 오늘은 반성문을 써 보려고 해요. 첫 1년 동안 제 손을 거쳐 간(그리고 떠나 버린) 식물들입니다.

## 1. 다육이 3종 세트 — 사인: 과습

예쁘다고 욕실 창가에 뒀어요. 습기 많은 곳에 매주 물까지 줬으니 뿌리가 물러질 수밖에 없었죠. 다육이는 **잎이 쭈글해질 때** 물을 주는 거였어요.

## 2. 로즈마리 — 사인: 통풍 부족

햇빛은 좋았는데 창문을 거의 안 열었어요. 어느 날 아래쪽 잎이 갈색으로 바스락거리더니 2주 만에 전체가 말라 버렸어요. 허브류는 **바람이 생명**이라는 걸 그때 알았어요.

## 3. 아이비 — 사인: 깍지벌레

잎 뒷면에 하얀 솜 같은 게 붙어 있었는데 먼지인 줄 알고 몇 주를 방치했어요. 알고 보니 벌레였고, 옆 화분까지 옮겨 갔어요.

## 4. 칼라데아 — 사인: 건조

잎 끝이 계속 타들어 가서 물을 더 줬는데, 사실은 **공기가 건조한 게** 문제였어요. 흙은 젖어 있는데 잎은 마르는 이상한 상태가 됐죠.

## 1년 동안 배운 것

- 물주기는 **날짜가 아니라 흙 상태**로 정한다. 손가락 한 마디(2~3cm) 넣어서 말랐을 때.
- 새 식물은 2주 동안 **격리**해서 벌레가 없는지 본다.
- 매일 아침 창문을 30분이라도 연다.
- 잎 뒷면을 일주일에 한 번은 들여다본다.

지금 베란다에는 열한 개 화분이 살아 있어요. 떠난 아이들 덕분이라고 생각하면서, 오늘도 손가락으로 흙을 찔러 봅니다.', 'public', 496, '2023-09-02T21:20:00+09:00');
insert into tag (name) values ('식물실패담') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '초보 식물집사가 첫 1년 동안 보낸 식물들'), (select tag_id from tag where name = '식물실패담'));
insert into tag (name) values ('초보식물') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '초보 식물집사가 첫 1년 동안 보낸 식물들'), (select tag_id from tag where name = '초보식물'));
insert into tag (name) values ('과습') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '초보 식물집사가 첫 1년 동안 보낸 식물들'), (select tag_id from tag where name = '과습'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')) and name = '분갈이·흙'), (select topic_id from topic where topic_name = '취미'), '내가 정착한 흙 배합 비율 (상토 : 펄라이트 : 바크)', '분갈이를 스무 번 넘게 하면서 여러 배합을 시험해 봤어요. 지금 거의 고정해서 쓰는 비율을 정리해 둘게요. 우리 집 환경은 **남향 베란다, 여름엔 습하고 겨울엔 건조한 편**이에요.

## 기본 배합 (관엽식물용)

**상토 5 : 펄라이트 3 : 바크(소립) 2**

- 몬스테라, 필로덴드론, 스킨답서스 같은 아이들에게 써요.
- 바크가 들어가면 흙 사이에 공기층이 생겨서 뿌리가 숨을 잘 쉬어요.
- 물이 화분 아래로 **5초 안에** 빠져나오면 성공이라고 봐요.

## 건조 좋아하는 아이들

**상토 4 : 펄라이트 3 : 마사토(세립) 3**

- 산세베리아, 스투키, 고무나무에 써요.
- 마사토가 무게를 잡아 줘서 키 큰 식물이 덜 넘어져요.

## 촉촉한 걸 좋아하는 아이들

**상토 6 : 펄라이트 2 : 피트모스 2**

- 칼라데아, 고사리류처럼 흙이 바싹 마르면 잎이 타는 아이들.
- 대신 물주기 간격을 조금 길게 잡아요.

## 섞을 때 팁

1. 큰 대야에 재료를 넣고 **물을 조금 뿌려** 가며 섞어요. 마른 펄라이트는 가루가 날려서 마스크 필수예요.
2. 한 번에 넉넉히 섞어서 뚜껑 있는 통에 보관해요. 저는 10L 통 하나를 쓰고 있어요.
3. 바크는 쓰기 전에 **뜨거운 물에 한 번 헹궈요**. 혹시 모를 벌레 알과 먼지를 줄여 줘요.

> 정답 배합은 없고, 우리 집 베란다에 맞는 배합이 있을 뿐이다.

이 비율도 이사 가면 또 바뀌겠죠? 여러분 집 배합도 궁금해요.', 'public', 597, '2024-04-13T15:10:00+09:00');
insert into tag (name) values ('흙배합') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '내가 정착한 흙 배합 비율 (상토 : 펄라이트 : 바크)'), (select tag_id from tag where name = '흙배합'));
insert into tag (name) values ('펄라이트') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '내가 정착한 흙 배합 비율 (상토 : 펄라이트 : 바크)'), (select tag_id from tag where name = '펄라이트'));
insert into tag (name) values ('바크') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '내가 정착한 흙 배합 비율 (상토 : 펄라이트 : 바크)'), (select tag_id from tag where name = '바크'));
insert into tag (name) values ('분갈이') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '내가 정착한 흙 배합 비율 (상토 : 펄라이트 : 바크)'), (select tag_id from tag where name = '분갈이'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')) and name = '초보 가이드'), (select topic_id from topic where topic_name = '취미'), '여름 베란다 과습 막는 물주기 루틴', '장마부터 8월까지는 일 년 중 식물이 제일 많이 죽는 시기예요. 덥고 습해서 흙이 안 마르는데, 사람은 더우니까 "목마르겠다" 하고 물을 더 주게 되거든요. 제 여름 루틴을 공유해요.

## 아침 7시, 출근 전 5분

- 베란다 창문 **양쪽을 열어서 맞바람** 통로를 만들어요.
- 화분 무게를 들어 봐요. 가벼우면 물, 묵직하면 패스.
- 서큘레이터를 **약풍으로 타이머 2시간** 맞춰요.

## 물 주는 기준

1. 손가락 테스트: 겉흙 3cm가 말랐는지
2. 나무젓가락 테스트: 화분 깊이 꽂았다 빼서 흙이 묻어 나오면 아직 젖은 거예요
3. 잎 상태: 살짝 처진 건 목마른 신호지만, **누렇게 처진 건 과습 신호**일 수 있어요

여름엔 물을 **저녁보다 아침에** 줘요. 밤새 젖은 채로 있으면 뿌리가 숨을 못 쉬어요.

## 여름에만 바꾸는 것

- 받침대 물은 **30분 안에** 버린다
- 비 오는 날에는 물을 주지 않는다 (습도 80% 넘는 날)
- 비료는 쉰다. 더위에 지친 뿌리에 비료는 부담이에요
- 플라스틱 화분 중 물이 잘 안 빠지는 건 **토분으로** 옮겼어요

## 올여름 성적

작년엔 여름에만 네 개를 보냈는데, 올해는 아직 한 개도 안 보냈어요! 대신 서큘레이터 전기요금이 조금 늘었습니다.', 'public', 1339, '2024-07-27T20:45:00+09:00');
insert into tag (name) values ('물주기') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '여름 베란다 과습 막는 물주기 루틴'), (select tag_id from tag where name = '물주기'));
insert into tag (name) values ('과습') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '여름 베란다 과습 막는 물주기 루틴'), (select tag_id from tag where name = '과습'));
insert into tag (name) values ('여름식물') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '여름 베란다 과습 막는 물주기 루틴'), (select tag_id from tag where name = '여름식물'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')) and name = '키우는 식물'), (select topic_id from topic where topic_name = '취미'), '겨울 베란다 냉해 입은 알로카시아 살리기 기록', '한파 오던 날 밤, 베란다 창문 쪽에 둔 알로카시아를 깜빡하고 안 들여놨어요. 그날 바깥 기온이 **영하 12도**, 베란다 안쪽 온도계는 **4도**까지 내려갔더라고요.

## 다음 날 아침 상태

- 잎 네 장 중 두 장이 **반투명하게 물러짐**
- 줄기 아래쪽은 아직 단단함
- 흙은 차갑고 축축함

## 응급 처치

1. 바로 거실로 옮겼어요. 단, **히터 바로 앞은 피해서** 서서히 온도를 올렸어요.
2. 물러진 잎 두 장은 소독한 가위로 줄기 밑동에서 잘라냈어요. 그냥 두면 무른 부분이 줄기까지 번진대요.
3. 물은 **완전히 끊었어요**. 흙이 다 마를 때까지 2주 가까이 안 줬어요.
4. 화분 아래 스티로폼 판을 깔아서 바닥 냉기를 막았어요.

## 회복 일지

- **1주차**: 남은 잎 두 장 버팀. 변화 없음.
- **2주차**: 한 장이 노래지기 시작. 마음 아팠지만 그대로 둠.
- **4주차**: 흙 가운데에서 **새순이 뾰족!** 진짜 소리 질렀어요.
- **6주차**: 새 잎이 펼쳐짐. 크기는 예전 잎의 절반 정도.

## 배운 점

> 알로카시아는 덩이줄기(구근)가 살아 있으면 다시 올라온다.

잎이 다 망가져도 줄기 밑이 단단하면 포기하지 마세요. 그리고 겨울엔 **최저 기온 5도 이하로 떨어지는 날** 무조건 실내로! 지금은 휴대폰에 한파 알림을 켜 두고 있어요.', 'public', 858, '2025-01-18T22:00:00+09:00');
insert into tag (name) values ('알로카시아') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '겨울 베란다 냉해 입은 알로카시아 살리기 기록'), (select tag_id from tag where name = '알로카시아'));
insert into tag (name) values ('냉해') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '겨울 베란다 냉해 입은 알로카시아 살리기 기록'), (select tag_id from tag where name = '냉해'));
insert into tag (name) values ('겨울식물') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '겨울 베란다 냉해 입은 알로카시아 살리기 기록'), (select tag_id from tag where name = '겨울식물'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')) and name = '키우는 식물'), (select topic_id from topic where topic_name = '취미'), '스킨답서스 물꽂이 번식 3주 관찰 일기', '길게 늘어진 스킨답서스 줄기를 정리하면서 잘라낸 마디로 물꽂이를 해 봤어요. 하루하루 기록해 둔 걸 정리합니다.

## 준비

- 마디(잎이 붙은 곳, 아래 공기뿌리가 있는 곳)를 기준으로 **한 마디에 잎 하나씩** 잘라서 다섯 개를 준비했어요
- 투명한 유리병에 수돗물을 **하루 받아 둔 물**로 채웠어요
- 공기뿌리가 물에 잠기고, 잎은 물 밖에 나오게 꽂았어요
- 위치: 직사광선 안 드는 밝은 거실 창가

## 관찰 기록

**3일차** — 별 변화 없음. 물이 살짝 탁해져서 갈아 줬어요.

**7일차** — 다섯 개 중 세 개의 공기뿌리 끝이 **하얗게 부풀기 시작**했어요.

**10일차** — 1cm 정도 하얀 뿌리가 나옴! 나머지 두 개는 아직 조용해요.

**14일차** — 먼저 나온 뿌리는 3cm, 잔뿌리까지 생겼어요. 늦게 출발한 두 개도 이제 막 시작.

**21일차** — 뿌리가 **5cm 이상**인 세 개를 흙으로 옮겼어요.

## 흙으로 옮길 때

물에서 자란 뿌리는 흙에 적응하는 데 시간이 걸려요. 그래서 옮긴 뒤 첫 일주일은 **흙을 계속 촉촉하게** 유지했어요. 평소 물주기보다 조금 자주요.

## 정리

- 성공률: 5개 중 5개 (모두 뿌리 내림)
- 물은 **3~4일에 한 번** 갈기
- 뿌리 길이 5cm가 흙으로 옮길 타이밍

새로 생긴 화분 세 개 중 두 개는 회사 동료들에게 나눠 줬어요. 식물 나눔이 이렇게 뿌듯한 일인 줄 몰랐어요.', 'public', 390, '2025-06-07T14:25:00+09:00');
insert into tag (name) values ('스킨답서스') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '스킨답서스 물꽂이 번식 3주 관찰 일기'), (select tag_id from tag where name = '스킨답서스'));
insert into tag (name) values ('물꽂이') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '스킨답서스 물꽂이 번식 3주 관찰 일기'), (select tag_id from tag where name = '물꽂이'));
insert into tag (name) values ('번식') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '스킨답서스 물꽂이 번식 3주 관찰 일기'), (select tag_id from tag where name = '번식'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')) and name = '키우는 식물'), (select topic_id from topic where topic_name = '취미'), '응애 퇴치 일지, 칼라데아와 2주 전쟁', '겨울 난방 때문에 실내가 건조해지면서 결국 **응애**가 왔어요. 칼라데아 잎에 하얀 점이 콕콕 찍히고, 잎 뒷면에 아주 가는 거미줄 같은 게 보이더라고요.

## 확인 방법

흰 종이를 잎 아래 대고 잎을 톡톡 털었더니 **아주 작은 점들이 기어다녔어요**. 돋보기로 보니 확실했습니다.

## 1주차: 물리적 제거

1. 감염된 칼라데아를 바로 **다른 화분에서 떨어진 욕실로 격리**
2. 샤워기 미지근한 물로 **잎 앞뒤를 꼼꼼히** 씻어냈어요. 흙에 물이 너무 들어가지 않게 비닐로 화분 윗면을 감쌌어요.
3. 3일 간격으로 두 번 반복

## 2주차: 약제 + 습도

- 식물용 친환경 살충제를 **잎 뒷면 위주로** 뿌렸어요. 응애는 알 → 성충 주기가 짧아서 **5일 간격으로 3회**가 중요하대요.
- 가습기를 켜서 습도를 **55%** 이상 유지했어요. 응애는 건조한 걸 좋아해요.

## 결과

2주 뒤 새 점이 더 이상 생기지 않았어요. 이미 상한 잎 세 장은 잘라냈고, 새 잎은 깨끗하게 나오고 있어요. 옆에 있던 마란타도 혹시 몰라 한 번 같이 씻었는데 다행히 멀쩡했습니다.

## 다시는 안 겪으려고

> 겨울 실내 습도 40% 이하 = 응애 초대장

- 습도계를 베란다와 거실에 하나씩 놓았어요
- 일주일에 한 번 **분무 대신 잎 닦기** (분무는 금방 말라서 효과가 짧더라고요)
- 새 식물 들일 땐 잎 뒷면 확인 필수', 'public', 515, '2026-02-14T21:35:00+09:00');
insert into tag (name) values ('응애') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '응애 퇴치 일지, 칼라데아와 2주 전쟁'), (select tag_id from tag where name = '응애'));
insert into tag (name) values ('해충') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '응애 퇴치 일지, 칼라데아와 2주 전쟁'), (select tag_id from tag where name = '해충'));
insert into tag (name) values ('칼라데아') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '응애 퇴치 일지, 칼라데아와 2주 전쟁'), (select tag_id from tag where name = '칼라데아'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')) and name = '키우는 식물'), (select topic_id from topic where topic_name = '여행'), '주말 화훼시장 나들이, 올리브나무를 데려왔어요', '날씨가 좋아서 남편이랑 아침 일찍 **양재 화훼시장**에 다녀왔어요. 예전부터 올리브나무를 들이고 싶었는데, 사진으로만 고르기엔 수형이 너무 제각각이라 직접 보고 싶었거든요.

## 가는 길

양재역에서 버스로 갈아타고 15분 정도. **오전 9시 전**에 도착했는데 이미 화분 카트를 끄는 사람들이 꽤 있었어요. 주말 오후엔 주차도 어렵고 사람도 많다고 해서 일찍 나선 게 정답이었어요.

## 둘러본 것

- 관엽 동: 몬스테라, 필로덴드론 희귀종이 가득. 가격표 보고 조용히 내려놨어요.
- 허브·과실수 동: 레몬나무, 블루베리, 올리브. 여기서 한 시간을 보냈어요.
- 자재 동: 토분, 바크, 펄라이트 대용량. 동네보다 확실히 저렴해요.

## 올리브나무 고른 기준

1. **줄기가 곧고 가지가 고르게** 퍼진 것
2. 잎 뒷면이 은빛이 선명하고 벌레 흔적 없는 것
3. 화분 아래로 뿌리가 너무 꽉 차 있지 않은 것

사장님이 "베란다에서 키울 거면 바람 잘 통하게, 겨울엔 꼭 들여놓아라" 하셨어요. 키 80cm짜리를 골랐고, 토분과 바크 한 포대까지 함께 샀어요.

## 점심

근처 국숫집에서 비빔국수 한 그릇. 양손 가득 흙 포대를 들고 버스 타기는 무리라 택시로 돌아왔어요.

지금 올리브나무는 베란다 제일 밝은 자리에 있어요. 우리 집 식구가 열네 개가 됐습니다.', 'public', 405, '2026-05-30T18:50:00+09:00');
insert into tag (name) values ('화훼시장') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '주말 화훼시장 나들이, 올리브나무를 데려왔어요'), (select tag_id from tag where name = '화훼시장'));
insert into tag (name) values ('올리브나무') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '주말 화훼시장 나들이, 올리브나무를 데려왔어요'), (select tag_id from tag where name = '올리브나무'));
insert into tag (name) values ('주말나들이') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '주말 화훼시장 나들이, 올리브나무를 데려왔어요'), (select tag_id from tag where name = '주말나들이'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')) and name = '분갈이·흙'), (select topic_id from topic where topic_name = '취미'), '가을맞이 베란다 대정리와 분갈이 몰아치기', '더위가 한풀 꺾이자마자 베란다 대정리를 했어요. 여름 동안 미뤄 둔 분갈이 네 개를 한꺼번에 하고, 겨울 준비까지 했습니다.

## 분갈이 4종

| 식물 | 이전 → 새 화분 | 흙 배합 |
|---|---|---|
| 몬스테라 (3년차) | 21cm → 25cm | 상토5:펄라이트3:바크2 |
| 고무나무 | 15cm → 18cm | 상토4:펄라이트3:마사토3 |
| 스킨답서스 (물꽂이 출신) | 12cm → 15cm | 기본 배합 |
| 올리브나무 | 토분 그대로, 겉흙만 교체 | 마사토 위주 |

몬스테라는 처음 분갈이했던 그 아이예요. 3년 만에 잎이 열여섯 장이 됐고, 화분에서 빼는 데만 20분이 걸렸어요. 뿌리가 꽉 차서 화분을 결국 가위로 잘라냈어요.

## 가을에 분갈이하는 이유

- 봄보다는 덜 권장되지만, **9월 중순~10월 초**는 아직 따뜻해서 뿌리가 자리 잡을 시간이 있어요
- 한여름은 더위 스트레스, 겨울은 성장이 멈춰서 피해요
- 단, 가을 분갈이는 **화분을 한 치수만** 키워요. 너무 크면 겨울 동안 흙이 안 말라요

## 월동 준비 체크리스트

- [ ] 한파 알림 켜기 (최저 5도)
- [ ] 냉해에 약한 아이들(알로카시아, 칼라데아) 실내 자리 미리 정하기
- [ ] 화분 받침 아래 스티로폼 판 깔기
- [ ] 비료는 10월 말에 마지막
- [ ] 가습기 필터 교체

## 정리하고 나니

여름 내내 무성하게 엉켜 있던 베란다가 훤해졌어요. 마른 잎 한 봉지, 깨진 화분 두 개, 쓰다 남은 흙 세 봉지가 나왔습니다. 블로그를 시작한 지 3년 반, 화분은 열네 개, 아직도 매일 아침 흙을 찔러 봐요.', 'public', 183, '2026-09-26T19:40:00+09:00');
insert into tag (name) values ('가을분갈이') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '가을맞이 베란다 대정리와 분갈이 몰아치기'), (select tag_id from tag where name = '가을분갈이'));
insert into tag (name) values ('베란다정리') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '가을맞이 베란다 대정리와 분갈이 몰아치기'), (select tag_id from tag where name = '베란다정리'));
insert into tag (name) values ('월동준비') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '가을맞이 베란다 대정리와 분갈이 몰아치기'), (select tag_id from tag where name = '월동준비'));

-- 새벽러너 · 05시 30분 러닝 일지
insert into blog (users_id, name, intro, comments_read_at) values ((select users_id from users where email = 'demo.runner@example.com'), '05시 30분 러닝 일지', '건강검진 경고 한 장으로 시작한 직장인 러너의 기록. 새벽 훈련 일지와 페이스·심박 숫자, 부상과 회복, 10km와 하프마라톤 대회 후기, 써 본 러닝 장비를 정리합니다.', '2026-10-11T23:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), '미분류', 'public', true, 1, 0, '2023-02-26T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), '훈련 일지', 'public', false, 2, 1, '2023-02-26T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), '대회 후기', 'public', false, 3, 2, '2023-02-26T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), '러닝 장비', 'public', false, 4, 3, '2023-02-26T10:00:00+09:00');
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')) and name = '훈련 일지'), (select topic_id from topic where topic_name = '운동'), '러닝 3주차, 처음으로 5km를 안 쉬고 뛰었다', '회사 건강검진에서 체지방이랑 혈압 수치가 경계선이라는 말을 듣고 시작한 러닝. 오늘 드디어 **5km를 한 번도 안 걷고** 끝까지 뛰었다.

## 3주 동안 한 것

처음엔 1km도 못 뛰고 숨이 넘어갔다. 그래서 걷기랑 뛰기를 섞는 방식으로 시작했다.

- **1주차**: 1분 뛰고 2분 걷기 × 8세트 (주 3회)
- **2주차**: 3분 뛰고 1분 30초 걷기 × 6세트
- **3주차**: 8분 뛰고 1분 걷기 × 3세트, 그리고 오늘 연속 도전

## 오늘 기록

- 거리: 5.02km
- 시간: 36분 48초
- 평균 페이스: **7''20"/km**
- 평균 심박: 168bpm (좀 높다)
- 장소: 집 앞 하천 산책로, 왕복 코스

마지막 1km는 다리가 무거워서 거의 걷는 속도였는데, 그래도 멈추지 않았다는 게 중요하다.

## 느낀 점

> 숨이 찬 건 폐가 아니라 속도 문제였다.

처음엔 무조건 빨리 뛰려고 했다. 옆에서 할아버지가 걷는 속도로 천천히 뛰는 걸 보고 따라 했더니 오히려 오래 갔다. **옆 사람이랑 대화할 수 있는 속도**가 지금 나한테 맞는 속도인 것 같다.

다음 목표는 5km 35분 안쪽, 그리고 언젠가 10km 대회.', 'public', 1353, '2023-03-11T08:20:00+09:00');
insert into tag (name) values ('러닝입문') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '러닝 3주차, 처음으로 5km를 안 쉬고 뛰었다'), (select tag_id from tag where name = '러닝입문'));
insert into tag (name) values ('5km') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '러닝 3주차, 처음으로 5km를 안 쉬고 뛰었다'), (select tag_id from tag where name = '5km'));
insert into tag (name) values ('새벽러닝') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '러닝 3주차, 처음으로 5km를 안 쉬고 뛰었다'), (select tag_id from tag where name = '새벽러닝'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')) and name = '대회 후기'), (select topic_id from topic where topic_name = '운동'), '첫 10km 대회 후기, 58분 41초', '러닝 시작하고 7개월 만에 첫 10km 대회. 목표는 **1시간 안에 들어오기**였고, 결과는 **58분 41초**. 해냈다.

## 대회 전날

- 저녁: 밥 한 공기 반, 된장국, 계란말이. 기름진 건 피했다.
- 배번호, 칩, 옷핀 네 개 미리 달아 둠
- 밤 10시 30분 취침 (긴장해서 12시 넘어서 잠듦)

## 대회 당일

아침 5시 반 기상, 바나나 한 개랑 식빵 한 장. 출발 1시간 반 전에 먹었다. 집결지에 7시 도착해서 화장실 줄만 25분 섰다. **화장실은 무조건 일찍.**

## 구간 기록

| 구간 | 페이스 | 메모 |
|---|---|---|
| 1~2km | 5''38" | 사람들 따라 너무 빨리 나감 |
| 3~5km | 5''52" | 의식적으로 늦춤 |
| 6~8km | 6''01" | 오르막, 다리 무거움 |
| 9~10km | 5''41" | 마지막에 짜냄 |

## 실수한 것

1. 초반 오버페이스. 출발 신호에 흥분해서 계획보다 20초 빨리 나갔다. 6km 오르막에서 그 대가를 치렀다.
2. 급수대에서 **컵을 꽉 쥐고 마시다 코로** 들어갔다. 다음엔 컵을 접어서 마시자.

## 결승선

마지막 200m에서 누가 "힘내요!" 해 줘서 갑자기 다리가 움직였다. 기록증 받고 나서 메달 사진을 열 장은 찍은 것 같다. 완주 메달은 지금 책상 모니터 옆에 걸려 있다.

다음은 하프. 아직 상상이 안 되지만.', 'public', 1006, '2023-10-29T19:30:00+09:00');
insert into tag (name) values ('10km') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '첫 10km 대회 후기, 58분 41초'), (select tag_id from tag where name = '10km'));
insert into tag (name) values ('첫대회') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '첫 10km 대회 후기, 58분 41초'), (select tag_id from tag where name = '첫대회'));
insert into tag (name) values ('대회후기') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '첫 10km 대회 후기, 58분 41초'), (select tag_id from tag where name = '대회후기'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')) and name = '러닝 장비'), (select topic_id from topic where topic_name = '운동'), '러닝화 세 켤레 돌려 신기, 400km 신어 본 정리', '러닝 1년 차. 처음엔 한 켤레로 버텼는데, 무릎이 시큰거리기 시작하면서 신발을 용도별로 나눴다. 세 켤레 각각 약 400km씩 신어 본 기준으로 적어 둔다.

## 1. 쿠션화 (데일리·회복주)

- 무게: 약 290g
- 드롭: 10mm
- 누적: 430km

밑창이 두꺼워서 착지할 때 푹신하다. **천천히 오래 뛰는 날**, 전날 무리한 다음 날 회복주에 신는다. 단점은 빠르게 뛰면 발이 둔하게 느껴진다는 것.

## 2. 반응형 트레이너 (템포·인터벌)

- 무게: 약 245g
- 드롭: 8mm
- 누적: 380km

튕겨 주는 느낌이 있어서 페이스 5분 초반대 훈련에 딱 좋다. 대회에도 신었다. 400km 넘어가니까 **바깥쪽 뒤꿈치가 눈에 띄게 닳았다.** 내 착지 습관이 보인다.

## 3. 트레일 겸용 (비 오는 날·흙길)

- 무게: 약 300g
- 누적: 210km

비 오는 날 미끄러운 하천 데크에서 이걸 신는다. 접지력 하나는 확실하다.

## 돌려 신으면서 좋아진 점

- 무릎 시큰거림이 확실히 줄었다
- 신발이 하루 이상 **마를 시간**이 생겨서 냄새, 변형이 덜하다
- 훈련 종류에 따라 신발을 고르면서 **그날 훈련 목적을 한 번 더 생각**하게 된다

## 교체 기준

> 600~800km, 또는 밑창보다 미드솔이 먼저 죽는다.

나는 손가락으로 미드솔 옆면을 눌렀을 때 주름이 깊게 남으면 교체를 고민한다. 러닝 앱에 신발별 누적 거리를 등록해 두니 관리하기 편하다.', 'public', 1362, '2024-04-06T21:10:00+09:00');
insert into tag (name) values ('러닝화') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '러닝화 세 켤레 돌려 신기, 400km 신어 본 정리'), (select tag_id from tag where name = '러닝화'));
insert into tag (name) values ('로테이션') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '러닝화 세 켤레 돌려 신기, 400km 신어 본 정리'), (select tag_id from tag where name = '로테이션'));
insert into tag (name) values ('장비리뷰') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '러닝화 세 켤레 돌려 신기, 400km 신어 본 정리'), (select tag_id from tag where name = '장비리뷰'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')) and name = '훈련 일지'), (select topic_id from topic where topic_name = '운동'), '정강이 통증(신스플린트)으로 3주를 쉬었다', '가을 10km 기록 단축을 노리고 8월에 주간 거리를 확 늘렸다가 결국 다쳤다. 오른쪽 **정강이 안쪽**이 뛸 때마다 찌릿하고, 계단 내려갈 때도 아팠다. 정형외과에서 들은 진단은 **신스플린트(정강이 통증 증후군)**.

## 원인 돌아보기

- 7월 주간 거리: 약 22km
- 8월 둘째 주: **38km** (한 주 만에 70% 증가)
- 그중 인터벌 2회, 템포런 1회
- 신발은 400km 넘은 트레이너 그대로

> 주간 거리는 일주일에 10% 이상 늘리지 말 것.

다들 아는 이 말을 무시했다.

## 3주 동안 한 것

**1주차 — 완전 휴식**
- 하루 3번 얼음찜질 15분
- 계단 대신 엘리베이터

**2주차 — 대체 운동**
- 실내 자전거 30분, 저강도
- 종아리 스트레칭, 폼롤러 (정강이 뼈 위는 피하고 종아리 근육만)
- 발가락으로 수건 끌어당기기 3세트 × 20회

**3주차 — 걷기에서 조깅으로**
- 빠르게 걷기 30분 → 통증 없음
- 주말에 **3km 아주 천천히(7분 페이스)**. 통증 없어서 안심

## 다시 시작하면서 바꾼 것

1. 주간 거리 **20km에서 다시 시작**, 매주 2km씩만 증가
2. 닳은 신발 교체
3. 일주일에 2번 **종아리·발목 근력 운동** (카프레이즈 3×15, 한 발 서기 30초)
4. 아스팔트보다 공원 흙길 비중 늘리기

가을 대회 기록 단축은 내년으로 미뤘다. 아쉽지만 오래 뛰려면 이게 맞다.', 'public', 558, '2024-09-14T22:40:00+09:00');
insert into tag (name) values ('신스플린트') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '정강이 통증(신스플린트)으로 3주를 쉬었다'), (select tag_id from tag where name = '신스플린트'));
insert into tag (name) values ('부상') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '정강이 통증(신스플린트)으로 3주를 쉬었다'), (select tag_id from tag where name = '부상'));
insert into tag (name) values ('회복') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '정강이 통증(신스플린트)으로 3주를 쉬었다'), (select tag_id from tag where name = '회복'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')) and name = '훈련 일지'), (select topic_id from topic where topic_name = '운동'), '새벽 5시 반 기상 루틴과 존2 러닝 두 달', '퇴근 후 러닝은 야근이나 약속 때문에 자꾸 밀려서, 올해부터 **새벽으로 옮겼다.** 두 달 해 보고 정리.

## 새벽 루틴

- **05:30** 알람. 폰은 침대에서 손 안 닿는 곳에 둔다
- **05:35** 물 한 컵, 바나나 반 개
- **05:45** 전날 밤 꺼내 둔 옷 입고 동적 스트레칭 5분
- **05:55** 출발
- **06:50** 귀가, 샤워, 출근 준비

전날 밤 **11시 전 취침**이 안 되면 다음 날 무조건 망한다. 결국 새벽 러닝의 핵심은 저녁 습관이었다.

## 존2 러닝이란

최대심박의 약 **60~70% 구간**에서 오래 뛰는 훈련. 숨은 조금 차지만 코로 숨 쉬면서 짧은 문장은 말할 수 있는 정도.

내 기준 (최대심박 약 188):
- 존2: **132~145bpm**
- 이 심박을 지키려면 페이스가 **6''50"~7''10"/km**까지 떨어진다

처음엔 이렇게 느려도 되나 싶어서 답답했다. 동네 할머니들 빠른 걸음에 추월당한 적도 있다.

## 두 달 변화

| 항목 | 1월 첫 주 | 3월 셋째 주 |
|---|---|---|
| 존2 평균 페이스 | 7''08" | **6''31"** |
| 아침 안정 심박 | 64 | 58 |
| 주간 거리 | 20km | 32km |

같은 심박에서 페이스가 30초 넘게 빨라졌다. 몸이 바뀌고 있다는 게 숫자로 보이니까 재밌다.

## 이번 주 계획

- 화·목: 존2 50분
- 토: 존2 롱런 12km
- 일: 쉬거나 가벼운 걷기

올가을엔 하프 마라톤에 신청할 생각이다.', 'public', 506, '2025-03-22T07:45:00+09:00');
insert into tag (name) values ('새벽러닝') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '새벽 5시 반 기상 루틴과 존2 러닝 두 달'), (select tag_id from tag where name = '새벽러닝'));
insert into tag (name) values ('존2') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '새벽 5시 반 기상 루틴과 존2 러닝 두 달'), (select tag_id from tag where name = '존2'));
insert into tag (name) values ('심박훈련') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '새벽 5시 반 기상 루틴과 존2 러닝 두 달'), (select tag_id from tag where name = '심박훈련'));
insert into tag (name) values ('루틴') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '새벽 5시 반 기상 루틴과 존2 러닝 두 달'), (select tag_id from tag where name = '루틴'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')) and name = '대회 후기'), (select topic_id from topic where topic_name = '운동'), '첫 하프마라톤 후기, 2시간 4분 37초', '드디어 첫 하프. 21.0975km. 기록은 **2:04:37.** 목표였던 2시간 10분은 넘었고, 꿈꿨던 서브2는 못 했다. 그래도 끝까지 안 걸었다.

## 준비 기간

- 훈련 기간: 14주
- 최장 거리 훈련: 18km (2주 전)
- 주간 평균: 35~40km
- 레이스 페이스 목표: **5''55"/km**

## 레이스 운영

**0~5km** — 5''52". 출발 그룹이 커서 지그재그로 추월하느라 체력 낭비. 다음엔 내 페이스 그룹 앞쪽에 선다.

**5~10km** — 5''49". 컨디션 최고. "서브2 가능한데?" 착각한 순간.

**10~15km** — 5''55". 첫 에너지젤(9km)이 잘 들어갔다. 두 번째 젤은 15km에.

**15~18km** — 6''02". 왼쪽 햄스트링이 당기기 시작. 급수대마다 물 한 모금, 머리에 한 컵.

**18~21km** — 6''18". 이게 하프의 진짜 시작이었다. 다리가 남의 다리 같았다. 마지막 1km는 결승선 아치가 보일 때까지 숫자만 셌다.

## 잘한 것

- 젤 두 개 계획대로 섭취 (9km, 15km)
- 오르막에서 페이스 대신 **심박 기준**으로 조절
- 새 신발 안 신음. 300km 신은 익숙한 트레이너

## 아쉬운 것

> 10km까지 아낀 체력이 18km 이후에 필요했다.

5~10km에서 신나서 계획보다 빨랐던 게 결국 후반에 돌아왔다. 그리고 롱런을 **18km 한 번밖에** 안 한 것도 컸다고 생각한다.

## 다음 목표

내년 봄 하프 **서브2**. 남은 4분 37초를 줄이러 간다.', 'public', 607, '2025-11-09T20:15:00+09:00');
insert into tag (name) values ('하프마라톤') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '첫 하프마라톤 후기, 2시간 4분 37초'), (select tag_id from tag where name = '하프마라톤'));
insert into tag (name) values ('대회후기') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '첫 하프마라톤 후기, 2시간 4분 37초'), (select tag_id from tag where name = '대회후기'));
insert into tag (name) values ('21km') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '첫 하프마라톤 후기, 2시간 4분 37초'), (select tag_id from tag where name = '21km'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')) and name = '훈련 일지'), (select topic_id from topic where topic_name = '운동'), '하프 서브2를 위한 12주 훈련표 짜기', '봄 하프는 회사 일정 때문에 못 나갔다. 대신 **10월 가을 하프에서 서브2**를 노리기로 하고, 7월 중순부터 시작할 12주 훈련표를 미리 짰다. 지금부터는 기초 체력 유지 기간.

## 목표 페이스

- 하프 2시간 = 평균 **5''41"/km**
- 안전하게 **5''35"** 를 레이스 페이스(RP)로 잡음

## 주간 기본 틀

| 요일 | 훈련 |
|---|---|
| 월 | 휴식 |
| 화 | 인터벌 또는 템포 |
| 수 | 존2 40분 + 근력 |
| 목 | 존2 50분 |
| 금 | 휴식 또는 걷기 |
| 토 | 롱런 |
| 일 | 회복주 30분 |

## 단계별 계획

**1~4주: 기초**
- 롱런 12 → 15km (존2)
- 화요일: 400m × 8 (1''55"~2''00", 사이 200m 조깅)

**5~8주: 강화**
- 롱런 16 → 19km, 마지막 3km는 RP
- 화요일: 템포 8km (5''20"~5''25")

**9~10주: 특화**
- 롱런 **20km 2회** (작년엔 18km 한 번이 최장이었다)
- 화요일: RP 3km × 3 (사이 1km 조깅)

**11~12주: 테이퍼링**
- 주간 거리 30% → 50% 감소
- 강도는 유지, 양만 줄이기

## 작년과 달라진 원칙

1. 롱런 20km 이상을 **최소 두 번**
2. 레이스 전 **젤 섭취 연습**을 롱런마다
3. 주간 거리 증가는 10% 이내 (신스플린트 교훈)
4. 수요일 근력: 스쿼트, 런지, 카프레이즈, 플랭크

계획은 계획일 뿐이지만, 써 두니까 덜 흔들린다.', 'public', 383, '2026-04-18T21:30:00+09:00');
insert into tag (name) values ('서브2') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '하프 서브2를 위한 12주 훈련표 짜기'), (select tag_id from tag where name = '서브2'));
insert into tag (name) values ('훈련계획') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '하프 서브2를 위한 12주 훈련표 짜기'), (select tag_id from tag where name = '훈련계획'));
insert into tag (name) values ('하프마라톤') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '하프 서브2를 위한 12주 훈련표 짜기'), (select tag_id from tag where name = '하프마라톤'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')) and name = '러닝 장비'), (select topic_id from topic where topic_name = '운동'), '여름 새벽 러닝 장비 정리, 모자부터 전해질까지', '본격 훈련 2주차. 새벽 6시인데도 기온 **27도, 습도 85%** 인 날이 이어진다. 여름에 쓰는 장비를 정리해 둔다.

## 머리

- **메쉬 러닝 캡**: 햇빛보다 땀이 눈에 들어가는 걸 막는 용도. 챙이 짧고 접히는 게 좋다.
- 헤어밴드는 땀을 금방 머금어서 30분 넘어가면 오히려 무겁다. 캡으로 정착.

## 상의·하의

- 얇은 기능성 민소매. 면 티는 땀 먹으면 1kg 짜리 갑옷이 된다.
- 하의는 안에 이너가 있는 5인치 쇼츠. **허벅지 쓸림 방지 크림**은 롱런 날 필수.

## 물과 전해질

- **러닝 벨트**에 250ml 소프트 플라스크 두 개
- 60분 넘는 날은 한 병은 물, 한 병은 **전해질 탭 녹인 물**
- 출발 전 체중, 도착 후 체중을 재 봤더니 1시간 반 롱런에서 **1.4kg** 빠졌다. 그만큼은 다시 마셔야 한다.

> 목마르다고 느낄 때는 이미 늦다. 15~20분마다 한두 모금.

## 피부·눈

- 선크림은 해 뜨기 전에 나가도 바른다. 끝날 즈음엔 해가 중천이다.
- 스포츠 선글라스: 새벽엔 안 쓰다가 7시 넘으면 꺼낸다.

## 여름에만 바꾼 훈련

- 페이스 대신 **심박 기준**으로. 같은 페이스여도 여름엔 심박이 10 이상 높다.
- 롱런 코스는 **5km 루프**로 짜서 집 앞을 지나가게 했다. 물 보충도 되고, 컨디션 나쁘면 바로 그만둘 수 있다.
- 인터벌은 해 뜨기 전 트랙에서 끝낸다.

여름은 기록이 안 나오는 계절이다. 지금은 버티는 게 훈련이라고 생각하기로 했다.', 'public', 182, '2026-07-25T22:05:00+09:00');
insert into tag (name) values ('여름러닝') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '여름 새벽 러닝 장비 정리, 모자부터 전해질까지'), (select tag_id from tag where name = '여름러닝'));
insert into tag (name) values ('러닝장비') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '여름 새벽 러닝 장비 정리, 모자부터 전해질까지'), (select tag_id from tag where name = '러닝장비'));
insert into tag (name) values ('전해질') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '여름 새벽 러닝 장비 정리, 모자부터 전해질까지'), (select tag_id from tag where name = '전해질'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')) and name = '훈련 일지'), (select topic_id from topic where topic_name = '운동'), '대회 2주 앞두고 마지막 장거리 20km', '가을 하프까지 정확히 2주. 오늘이 계획표상 **마지막 장거리**였다. 20km, 마지막 5km는 레이스 페이스.

## 오늘 기록

- 거리: 20.1km
- 시간: 1:58:44
- 평균 페이스: 5''54"/km
- 마지막 5km 평균: **5''33"/km**
- 평균 심박: 151bpm
- 기온: 출발 14도, 도착 17도 — 드디어 가을이다

## 구간 메모

**0~15km (존2 + 살짝 위)**
6''05" 전후로 편하게. 7km, 14km에 젤 하나씩. 작년 하프 때처럼 초반에 욕심내지 않으려고 계속 시계만 봤다.

**15~20km (레이스 페이스)**
페이스를 올리니까 다리가 처음엔 무겁다가 2km쯤 지나니 오히려 리듬이 생겼다. 18km에서 작년처럼 다리가 풀릴까 걱정했는데, **이번엔 버텼다.** 롱런 20km를 세 번 한 게 확실히 다르다.

## 12주 돌아보기

- 계획한 훈련 72회 중 **66회** 완료
- 빠진 6회: 출장 3회, 장염 2회, 태풍 1회
- 부상 없음 (이게 제일 크다)
- 주간 최대 거리: 52km

## 남은 2주

1. 이번 주 주간 거리 **35km**, 다음 주 **22km**로 줄이기
2. 화요일 RP 3km × 2만 가볍게
3. 대회 사흘 전부터 탄수화물 비중 늘리기 (밥, 떡, 바나나)
4. 대회 신발은 지금 160km 신은 트레이너 그대로

> 2시간 안쪽 롱런 20km. 같은 거리를 2주 뒤엔 1km 더 길게, 몇 분 더 빨리.

작년엔 막연했던 서브2가 이제 숫자로 보인다. 남은 건 잘 쉬는 것뿐.', 'public', 191, '2026-10-03T10:30:00+09:00');
insert into tag (name) values ('롱런') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '대회 2주 앞두고 마지막 장거리 20km'), (select tag_id from tag where name = '롱런'));
insert into tag (name) values ('20km') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '대회 2주 앞두고 마지막 장거리 20km'), (select tag_id from tag where name = '20km'));
insert into tag (name) values ('테이퍼링') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '대회 2주 앞두고 마지막 장거리 20km'), (select tag_id from tag where name = '테이퍼링'));
insert into tag (name) values ('서브2') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '대회 2주 앞두고 마지막 장거리 20km'), (select tag_id from tag where name = '서브2'));

-- 바다마중 · 바다 마중 가는 길
insert into blog (users_id, name, intro, comments_read_at) values ((select users_id from users where email = 'demo.sea@example.com'), '바다 마중 가는 길', '동해, 남해, 서해, 그리고 작은 섬들. 퇴근 후 기차를 타고 바다를 만나러 간 기록을 모아 둡니다. 배편, 버스 시간, 동네 밥집까지 꼼꼼히 적어요.', '2026-10-11T23:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), '미분류', 'public', true, 1, 0, '2023-04-08T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), '동해', 'public', false, 2, 1, '2023-04-08T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), '남해·서해', 'public', false, 3, 2, '2023-04-08T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), '섬 여행', 'public', false, 4, 3, '2023-04-08T10:00:00+09:00');
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')) and name = '동해'), (select topic_id from topic where topic_name = '여행'), '주문진 새벽 수산시장 구경하고 소돌 아들바위까지 걸은 기록', '금요일 퇴근하고 바로 강릉행 KTX를 탔어요. 강릉역에 밤 10시 반쯤 도착해서 주문진까지는 택시로 30분 정도 걸렸고, 항구 바로 앞 작은 모텔에서 잤어요. 이번 여행의 목적은 딱 하나, **새벽 경매 구경**이었거든요.

## 새벽 5시의 주문진항

알람을 4시 50분에 맞춰 놓고 나갔는데 이미 항구는 불이 환했어요. 오징어 철은 아니라서 가자미, 도루묵, 문어가 대부분이었고요. 경매사 아저씨가 손가락으로 신호하는 걸 한참 보고 있었는데, 무슨 말인지는 하나도 못 알아들었어요. 그래도 그 소리랑 비린내랑 갈매기 소리가 섞인 분위기가 정말 좋았어요.

- 경매는 보통 새벽 5시~6시 사이 (배 들어오는 날에 따라 달라요)
- 관광객은 경매장 안쪽 줄 밖에서만 구경 가능
- 장화 신은 분들 사이로 다니니까 **미끄럽지 않은 신발** 필수

## 소돌 아들바위 공원

해 뜨는 시간에 맞춰 항구에서 북쪽으로 20분쯤 걸어서 소돌 아들바위 공원으로 갔어요. 바위 모양이 신기하게 깎여 있어서 사진 찍는 분들이 많았는데, 아침 6시에는 저 혼자였어요. 파도가 바위 틈으로 들어왔다 나가는 소리를 듣고 있으니까 한 주 동안 쌓인 메일 생각이 조금씩 지워지더라고요.

> 혼자 여행의 좋은 점은 어디서 얼마나 오래 앉아 있을지 아무하고도 상의하지 않아도 된다는 것.

## 아침밥

시장 안쪽 ''바다식당''이라는 작은 집에서 곰치국을 먹었어요. 1만 4천 원이었고 국물이 생각보다 맑아서 놀랐어요. 흐물흐물한 살이 처음엔 낯설었는데 김치랑 같이 먹으니까 금방 한 그릇 비웠어요.

다음에는 오징어 철인 가을에 다시 와 보고 싶어요.', 'public', 1200, '2023-05-20T22:41:00+09:00');
insert into tag (name) values ('주문진') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '주문진 새벽 수산시장 구경하고 소돌 아들바위까지 걸은 기록'), (select tag_id from tag where name = '주문진'));
insert into tag (name) values ('강릉') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '주문진 새벽 수산시장 구경하고 소돌 아들바위까지 걸은 기록'), (select tag_id from tag where name = '강릉'));
insert into tag (name) values ('수산시장') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '주문진 새벽 수산시장 구경하고 소돌 아들바위까지 걸은 기록'), (select tag_id from tag where name = '수산시장'));
insert into tag (name) values ('새벽산책') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '주문진 새벽 수산시장 구경하고 소돌 아들바위까지 걸은 기록'), (select tag_id from tag where name = '새벽산책'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')) and name = '남해·서해'), (select topic_id from topic where topic_name = '여행'), '통영 1박 2일, 동피랑보다 좋았던 서피랑 골목', '추석 연휴 전 주말에 통영을 다녀왔어요. 서울 남부터미널에서 고속버스로 4시간 10분. 길긴 하지만 창밖으로 남해가 보이기 시작하면 다 잊혀요.

## 첫째 날 동선

1. 통영종합버스터미널 도착 (오후 1시)
2. 시내버스로 강구안 이동, 점심으로 충무김밥
3. 동피랑 벽화마을
4. 서피랑 99계단과 서포루
5. 강구안 야경 보고 숙소

동피랑은 유명한 만큼 사람이 정말 많았어요. 좁은 골목에서 사진 찍으려고 줄을 서 있는 걸 보고 조금 지쳐서, 큰 기대 없이 건너편 서피랑으로 넘어갔는데 **여기가 이번 여행의 진짜 발견**이었어요.

## 서피랑 골목이 좋았던 이유

- 사람이 적어요. 토요일 오후 4시였는데 마주친 사람이 열 명도 안 됐어요.
- 99계단 끝 서포루에서 보는 강구안 쪽 풍경이 동피랑보다 넓게 트여 있어요.
- 골목마다 동네 할머니들이 내놓은 화분이 많아서 ''사는 동네'' 느낌이 나요.

서포루 정자에 앉아서 해 지는 걸 40분 넘게 봤어요. 바다 위로 작은 배들이 하나둘 불을 켜는데, 그 순서가 꼭 누가 정해 놓은 것 같았어요.

## 둘째 날

아침에 통영 중앙시장에서 해산물 구경하고, 미륵산 케이블카를 타려고 했는데 바람 때문에 운행이 멈춰서 못 탔어요. 대신 해저터널을 걸어서 지나갔는데 생각보다 짧았어요(483m).

> 통영은 계획한 곳보다 계획 안 한 곳이 더 기억에 남는 도시였어요.

다음엔 꼭 케이블카 타러 다시 가야겠어요.', 'public', 683, '2023-09-16T21:05:00+09:00');
insert into tag (name) values ('통영') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '통영 1박 2일, 동피랑보다 좋았던 서피랑 골목'), (select tag_id from tag where name = '통영'));
insert into tag (name) values ('서피랑') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '통영 1박 2일, 동피랑보다 좋았던 서피랑 골목'), (select tag_id from tag where name = '서피랑'));
insert into tag (name) values ('1박2일') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '통영 1박 2일, 동피랑보다 좋았던 서피랑 골목'), (select tag_id from tag where name = '1박2일'));
insert into tag (name) values ('경남여행') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '통영 1박 2일, 동피랑보다 좋았던 서피랑 골목'), (select tag_id from tag where name = '경남여행'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')) and name = '섬 여행'), (select topic_id from topic where topic_name = '여행'), '욕지도 첫 배 타고 들어가서 섬 한 바퀴 돈 날', '작년 통영 여행 때 터미널에서 ''욕지도'' 배편 시간표를 보고 언젠가 가 봐야지 했는데, 드디어 다녀왔어요.

## 배편 정리

- 출발: 통영항여객선터미널 또는 삼덕항 (저는 삼덕항)
- 소요 시간: 삼덕항 기준 약 1시간
- 첫 배: 오전 6시 대 (계절마다 바뀌니까 꼭 전날 확인)
- 요금: 편도 1만 원 안팎, 왕복으로 사면 편해요
- 신분증 필수! 승선권 살 때 확인해요

전날 통영 시내에서 자고 새벽 5시 반에 택시로 삼덕항까지 갔어요. 배 2층 객실에는 낚시 가방 메신 아저씨들이 대부분이었고, 다들 들어가자마자 바닥에 누워서 주무시더라고요. 저도 따라 누웠다가 엔진 소리에 금방 잠들었어요.

## 섬 일주

욕지도는 일주도로가 약 30km라서 걸어서는 무리고, 저는 항구 앞에서 **전기자전거를 4시간 빌렸어요** (2만 원). 오르막이 꽤 있어서 일반 자전거였으면 중간에 포기했을 거예요.

1. 욕지항 출발, 시계 방향
2. 새천년기념공원 전망대 — 섬 남쪽 바다가 한눈에 보여요
3. 출렁다리와 펠리컨바위 쪽 해안 산책로
4. 덕동해수욕장에서 잠깐 쉬기
5. 다시 욕지항

펠리컨바위 쪽 산책로는 계단이 많아서 자전거는 입구에 세워 두고 걸어야 해요. 왕복 40분 정도. 바위 끝에서 내려다보는 물빛이 진짜 에메랄드색이었어요.

## 먹은 것

욕지도는 **고등어**랑 **고구마**가 유명하대요. 항구 앞 작은 식당에서 고등어 회를 처음 먹어 봤는데 비린 맛이 하나도 없었어요. 돌아오는 배 타기 전에 고구마 막걸리도 한 병 샀고요.

마지막 배가 오후 4시 대라서 생각보다 시간이 빠듯했어요. 다음엔 섬에서 하룻밤 자고 싶어요.', 'public', 1231, '2024-04-27T20:32:00+09:00');
insert into tag (name) values ('욕지도') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '욕지도 첫 배 타고 들어가서 섬 한 바퀴 돈 날'), (select tag_id from tag where name = '욕지도'));
insert into tag (name) values ('통영') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '욕지도 첫 배 타고 들어가서 섬 한 바퀴 돈 날'), (select tag_id from tag where name = '통영'));
insert into tag (name) values ('섬여행') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '욕지도 첫 배 타고 들어가서 섬 한 바퀴 돈 날'), (select tag_id from tag where name = '섬여행'));
insert into tag (name) values ('배편') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '욕지도 첫 배 타고 들어가서 섬 한 바퀴 돈 날'), (select tag_id from tag where name = '배편'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')) and name = '동해'), (select topic_id from topic where topic_name = '여행'), '삼척 장호항 스노클링 후기와 챙겨 가면 좋은 것', '여름휴가 3일 중 하루를 장호항에 썼어요. ''한국의 나폴리''라는 말은 좀 과하다고 생각했는데, 막상 가 보니 물이 정말 맑아서 할 말이 없더라고요.

## 장호항 기본 정보

- 서울에서 자차로 약 3시간 반 (동해고속도로 → 7번 국도)
- 주차는 항구 근처 공영주차장, 성수기 주말엔 오전 9시 전에 차요
- 스노클링 장비 대여: 항구 앞 대여점들, 구명조끼+마스크+오리발 세트 1만 5천 원 정도
- 투명 카누도 있어요 (2인 30분 기준 2~3만 원대)

## 스노클링 해 보니

물 깊이가 허리에서 가슴 정도인 곳이 넓게 있어서 초보도 괜찮았어요. 바위 근처로 가면 작은 물고기 떼가 지나가고, 운 좋으면 성게도 보여요(만지면 안 돼요!). 저는 수영을 잘 못하는데 **구명조끼 입고 엎드려서 둥둥 떠 있기만 해도** 충분히 재밌었어요.

다만 8월 초인데도 물이 생각보다 차가웠어요. 동해는 냉수대가 들어오면 갑자기 수온이 확 떨어진다고 하더라고요. 30분 넘게 있으니까 입술이 파래져서 나왔어요.

## 챙겨 가면 좋은 것

- **아쿠아슈즈**: 바닥이 자갈이랑 바위라 맨발은 아파요
- 래시가드 상하의: 햇볕이 정말 세요
- 방수팩: 휴대폰 넣고 물속 사진 찍기
- 큰 수건이랑 얇은 담요: 물에서 나오면 추워요
- 따뜻한 물 담은 보온병

> 다음엔 냉수대 예보를 꼭 확인하고 가기. 국립해양조사원 누리집에서 볼 수 있어요.

오후엔 근처 용화해변 레일바이크를 타려고 했는데 매진이라 못 탔어요. 성수기에는 미리 예약하세요.', 'public', 1214, '2024-08-10T21:47:00+09:00');
insert into tag (name) values ('삼척') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '삼척 장호항 스노클링 후기와 챙겨 가면 좋은 것'), (select tag_id from tag where name = '삼척'));
insert into tag (name) values ('장호항') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '삼척 장호항 스노클링 후기와 챙겨 가면 좋은 것'), (select tag_id from tag where name = '장호항'));
insert into tag (name) values ('스노클링') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '삼척 장호항 스노클링 후기와 챙겨 가면 좋은 것'), (select tag_id from tag where name = '스노클링'));
insert into tag (name) values ('여름휴가') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '삼척 장호항 스노클링 후기와 챙겨 가면 좋은 것'), (select tag_id from tag where name = '여름휴가'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')) and name = '남해·서해'), (select topic_id from topic where topic_name = '여행'), '군산 근대거리 걷고 선유도까지, 서해 당일치기 코스 정리', '동해만 다니다가 오랜만에 서해 쪽으로 가 봤어요. 용산역에서 군산역까지 KTX 없이 새마을호로 3시간 정도 걸렸어요. 군산역에서 렌터카를 빌렸고요.

## 오전: 군산 근대거리

- 구 군산세관 → 근대역사박물관 → 신흥동 일본식 가옥 → 동국사

전부 걸어서 1시간 반이면 충분해요. 근대역사박물관은 입장료가 2천 원인데 생각보다 전시가 알차서 40분 넘게 봤어요. 옛날 항구 모습을 모형으로 만들어 둔 방이 특히 좋았어요.

점심은 동네 분들 많이 가시는 작은 짬뽕집에 갔어요. 이름은 ''항구반점''. 국물이 고추기름보다 해산물 맛이 강해서 제 취향이었어요.

## 오후: 고군산군도 선유도

군산 시내에서 차로 50분 정도면 새만금 방조제를 지나 선유도까지 가요. 예전엔 배로만 갈 수 있었는데 다리가 놓여서 이제 차로 바로 들어가요.

1. **선유도 해수욕장** — 모래가 곱고 물이 얕게 오래 이어져요
2. **망주봉** 아래 산책로
3. **장자도**까지 걸어서 건너기 (장자교)
4. **대장도** 대장봉 — 왕복 1시간, 계단이 가팔라요

대장봉 정상에서 내려다본 섬들이 정말 좋았어요. 동해는 수평선이 쭉 뻗어 있는데, 서해는 섬이 겹겹이 놓여 있어서 꼭 수묵화 같았어요.

## 해 질 무렵

돌아오는 길에 방조제 중간 휴게소에서 노을을 봤어요. 갯벌 위로 해가 내려앉으니까 물인지 땅인지 모르게 다 주황색이 되더라고요.

> 서해의 좋은 점: 해가 바다로 진다는 것. 동해 사람은 이걸 잊고 살아요.

당일치기로는 조금 빡빡했지만, 렌터카 덕분에 다 볼 수 있었어요.', 'public', 965, '2025-03-22T19:20:00+09:00');
insert into tag (name) values ('군산') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '군산 근대거리 걷고 선유도까지, 서해 당일치기 코스 정리'), (select tag_id from tag where name = '군산'));
insert into tag (name) values ('선유도') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '군산 근대거리 걷고 선유도까지, 서해 당일치기 코스 정리'), (select tag_id from tag where name = '선유도'));
insert into tag (name) values ('당일치기') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '군산 근대거리 걷고 선유도까지, 서해 당일치기 코스 정리'), (select tag_id from tag where name = '당일치기'));
insert into tag (name) values ('서해') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '군산 근대거리 걷고 선유도까지, 서해 당일치기 코스 정리'), (select tag_id from tag where name = '서해'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')) and name = '동해'), (select topic_id from topic where topic_name = '음식'), '속초 갈 때마다 들르는 동명항 근처 먹을 곳 기록', '속초는 1년에 두세 번은 가는 것 같아요. 그런데 매번 중앙시장 닭강정 줄에 서 있는 게 지겨워져서, 요즘은 동명항 쪽에서 해결하고 와요. 그동안 들른 곳을 한 번 정리해 둘게요.

## 1. 동명항 활어센터 2층

1층에서 회를 고르고 2층 식당에서 상차림비를 내고 먹는 방식이에요.

- 광어+우럭 한 접시 (2인 기준) 4~5만 원대
- 상차림비 1인 5천 원 안팎
- 매운탕은 따로 주문

**팁**: 1층 가게마다 가격이 거의 같아서, 저는 그냥 제일 친절하게 설명해 주시는 곳에서 사요.

## 2. 물회 — ''청초물회집''

여름 속초는 물회죠. 여기는 살얼음 육수에 배랑 오이를 넉넉히 넣어 주시고, 소면을 따로 말아 먹어요. 1만 8천 원. 육수가 달지 않아서 좋았어요. 다만 점심시간엔 대기가 20분 정도 있었어요.

## 3. 오징어순대 — 아바이마을 갯배 건너서

갯배는 500원이고 줄 잡아당기는 걸 손님이 같이 해요. 건너가면 오징어순대 집이 쭉 있는데, 저는 ''아바이할머니순대''에서 반반(오징어순대+아바이순대)을 먹었어요. 오징어순대는 계란물 입혀서 부쳐 주는데 그게 제일 맛있어요.

## 4. 영금정 보고 커피

배부르면 동명항 바로 옆 영금정 정자까지 걸어가서 파도 소리 듣다가, 근처 작은 카페에서 아이스 아메리카노 한 잔. 이게 제 속초 루틴이에요.

> 맛집은 결국 ''또 갈 곳''이 몇 개 생기는 것. 속초는 그런 곳이 많아서 좋아요.', 'public', 882, '2025-07-05T22:10:00+09:00');
insert into tag (name) values ('속초') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '속초 갈 때마다 들르는 동명항 근처 먹을 곳 기록'), (select tag_id from tag where name = '속초'));
insert into tag (name) values ('동명항') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '속초 갈 때마다 들르는 동명항 근처 먹을 곳 기록'), (select tag_id from tag where name = '동명항'));
insert into tag (name) values ('물회') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '속초 갈 때마다 들르는 동명항 근처 먹을 곳 기록'), (select tag_id from tag where name = '물회'));
insert into tag (name) values ('오징어순대') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '속초 갈 때마다 들르는 동명항 근처 먹을 곳 기록'), (select tag_id from tag where name = '오징어순대'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')) and name = '남해·서해'), (select topic_id from topic where topic_name = '여행'), '겨울 바다 혼자 보러 간 남해 다랭이마을 일기', '올해 회사에서 유난히 지친 한 해였어요. 연차가 6일 남아 있길래 목요일 금요일을 붙여서 남해에 다녀왔어요. 이번엔 ''아무것도 안 하기''가 목표였어요.

## 가는 길

서울남부터미널 → 남해공용터미널, 버스로 4시간 20분. 남해 터미널에서 다랭이마을까지는 군내버스로 50분 정도 더 들어가요. 버스가 하루에 몇 대 없어서 시간표 사진을 찍어 두는 게 좋아요.

## 다랭이마을

비탈진 산자락에 논이 계단처럼 층층이 쌓여 있고, 그 끝이 바로 바다예요. 11월이라 논은 다 비어 있었지만 그래서 오히려 바다 색이 더 잘 보였어요.

- 마을 위 주차장에서 바닷가까지 내려가는 길 약 20분
- 바닷가 쪽 출렁다리, 바람이 세면 꽤 흔들려요
- 마을 안 민박은 겨울엔 문 닫은 곳이 많아서 미리 전화해 보기

저는 마을 안 작은 민박에서 이틀 잤어요. 방에 창문이 하나 있는데, 그 창이 그대로 바다였어요. 첫날 밤엔 파도 소리 때문에 잠을 못 잘 줄 알았는데 오히려 깊이 잤어요.

## 이틀 동안 한 일

- 아침에 마을 길 한 바퀴 걷기
- 민박 사장님이 주신 유자차 마시기
- 바닷가 바위에 앉아서 책 반 권 읽기
- 저녁엔 멸치쌈밥 먹기 (1인분도 해 주셨어요)

> 바다는 아무 말도 하지 않는데, 이상하게 이야기를 다 들어 준 것 같은 기분이 들어요.

돌아오는 버스에서 휴대폰 사진첩을 보니까 사진이 열두 장밖에 없었어요. 그게 이번 여행이 잘 됐다는 증거 같았어요.', 'public', 527, '2025-11-15T23:02:00+09:00');
insert into tag (name) values ('남해') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '겨울 바다 혼자 보러 간 남해 다랭이마을 일기'), (select tag_id from tag where name = '남해'));
insert into tag (name) values ('다랭이마을') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '겨울 바다 혼자 보러 간 남해 다랭이마을 일기'), (select tag_id from tag where name = '다랭이마을'));
insert into tag (name) values ('겨울여행') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '겨울 바다 혼자 보러 간 남해 다랭이마을 일기'), (select tag_id from tag where name = '겨울여행'));
insert into tag (name) values ('혼자여행') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '겨울 바다 혼자 보러 간 남해 다랭이마을 일기'), (select tag_id from tag where name = '혼자여행'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')) and name = '섬 여행'), (select topic_id from topic where topic_name = '여행'), '가파도 청보리 끝물에 다녀온 기록과 배 예약 팁', '제주 출장이 금요일에 끝나서 주말을 붙여 가파도에 다녀왔어요. 청보리 축제는 4월이라 이미 끝났지만, 5월 말에도 보리밭이 남아 있다고 해서 기대하고 갔어요.

## 배 예약

- 출발: 운진항 (모슬포 남항)
- 소요: 약 15분
- **온라인 예매 필수**. 주말엔 현장에서 표가 거의 없어요
- 들어가는 배를 예약하면 나오는 배 시간이 자동으로 정해지는 방식이라, 섬에 머무는 시간이 2시간 반 정도로 정해져 있어요
- 신분증 꼭 챙기기

저는 오전 9시 배로 들어가서 11시 40분 배로 나왔어요.

## 섬 둘레

가파도는 정말 평평해요. 가장 높은 곳이 20m 정도라고 하더라고요. 둘레가 약 4km라서 걸어서 한 바퀴 1시간 반이면 충분해요. 자전거도 빌릴 수 있는데 저는 걸었어요.

**보리밭 상태**: 끝물이라 초록보다는 노랗게 익어 가는 색이었어요. 그래도 바람이 불 때마다 보리 이삭이 한쪽으로 쏠리는 게 파도 같아서, 바다 옆에 또 바다가 하나 있는 느낌이었어요.

## 좋았던 곳

1. 상동 포구에서 보는 산방산과 한라산
2. 섬 가운데 보리밭 사이 길
3. 하동 쪽 해안길, 현무암 사이로 들어오는 물빛

## 먹은 것

포구 앞 식당에서 보말칼국수를 먹었어요. 1만 2천 원. 보말이 생각보다 많이 들어 있어서 국물이 진했어요. 가파도 막걸리도 있었는데 배 타야 해서 참았어요.

> 섬에 머무는 시간이 정해져 있다는 게 처음엔 아쉬웠는데, 오히려 그래서 한 걸음 한 걸음 아껴 걸었어요.

내년엔 4월 축제 기간에 맞춰 다시 오고 싶어요.', 'public', 284, '2026-05-30T21:38:00+09:00');
insert into tag (name) values ('가파도') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '가파도 청보리 끝물에 다녀온 기록과 배 예약 팁'), (select tag_id from tag where name = '가파도'));
insert into tag (name) values ('제주') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '가파도 청보리 끝물에 다녀온 기록과 배 예약 팁'), (select tag_id from tag where name = '제주'));
insert into tag (name) values ('청보리') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '가파도 청보리 끝물에 다녀온 기록과 배 예약 팁'), (select tag_id from tag where name = '청보리'));
insert into tag (name) values ('섬여행') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '가파도 청보리 끝물에 다녀온 기록과 배 예약 팁'), (select tag_id from tag where name = '섬여행'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')) and name = '동해'), (select topic_id from topic where topic_name = '여행'), '추석 연휴 끝에 다녀온 고성 화진포, 조용한 가을 바다', '추석 연휴 마지막 날, 다들 서울로 올라올 때 반대로 강원도 고성에 갔어요. 하행선이 텅 비어 있어서 양양까지 2시간 반 만에 도착했어요.

## 화진포 해변

고성에서도 북쪽 끝, 통일전망대 가는 길에 있어요. 9월 말이라 해수욕장은 이미 문을 닫았고, 백사장에 저 말고 두 가족 정도만 있었어요.

- 모래가 굵고 밟으면 ''뽀득'' 소리가 나요
- 해변 바로 뒤에 **화진포 호수**가 있어서, 바다와 호수를 한 번에 볼 수 있어요
- 호수 둘레길 약 16km. 저는 일부만 1시간 걸었어요

## 화진포의 성

해변 북쪽 언덕에 ''화진포의 성''이라는 오래된 건물이 있어요. 입장료 3천 원(주변 기념관 통합권). 건물 자체보다 2층 창문에서 내려다보는 바다가 좋아요. 창틀이 액자처럼 바다를 잘라 줘요.

## 거진항에서 늦은 점심

화진포에서 차로 10분 거리 거진항 앞 작은 식당 ''거진할매집''에서 도치알탕을 먹었어요. 원래 겨울 음식이라 메뉴에 없을 줄 알았는데 냉동해 둔 걸로 끓여 주셨어요. 1만 5천 원. 알이 톡톡 터지는 식감이 재밌어요.

## 이번 여행에서 느낀 것

1. 연휴 마지막 날 하행선은 **최고의 비밀 시간**이에요
2. 바다는 성수기가 지나야 비로소 바다 소리가 들려요
3. 강원도 북쪽 끝은 생각보다 가깝다

> 여름 바다가 사람들의 바다라면, 가을 바다는 바다 자신의 것 같아요.

돌아오는 길엔 역시 상행선이 막혀서 5시간 걸렸어요. 그래도 괜찮았어요.', 'public', 229, '2026-09-27T20:15:00+09:00');
insert into tag (name) values ('고성') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '추석 연휴 끝에 다녀온 고성 화진포, 조용한 가을 바다'), (select tag_id from tag where name = '고성'));
insert into tag (name) values ('화진포') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '추석 연휴 끝에 다녀온 고성 화진포, 조용한 가을 바다'), (select tag_id from tag where name = '화진포'));
insert into tag (name) values ('가을바다') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '추석 연휴 끝에 다녀온 고성 화진포, 조용한 가을 바다'), (select tag_id from tag where name = '가을바다'));
insert into tag (name) values ('강원도') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '추석 연휴 끝에 다녀온 고성 화진포, 조용한 가을 바다'), (select tag_id from tag where name = '강원도'));

-- 주말목수 · 토요일의 대패밥
insert into blog (users_id, name, intro, comments_read_at) values ((select users_id from users where email = 'demo.woodwork@example.com'), '토요일의 대패밥', '동네 작은 공방에서 주말마다 도마, 트레이 같은 소품부터 스툴, 협탁, 책장까지 만들어 온 기록. 공구 사용기와 나무 고르는 이야기도 함께 적어요.', '2026-10-11T23:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), '미분류', 'public', true, 1, 0, '2023-04-02T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), '소품 만들기', 'public', false, 2, 1, '2023-04-02T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), '가구', 'public', false, 3, 2, '2023-04-02T10:00:00+09:00');
insert into category (blog_id, name, visibility, is_default, sort_order, color_index, created_at) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), '공구·재료 이야기', 'public', false, 4, 3, '2023-04-02T10:00:00+09:00');
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')) and name = '소품 만들기'), (select topic_id from topic where topic_name = '취미'), '공방 첫날, 편백 도마 하나 만들고 손바닥에 물집 두 개', '회사 일만 하다 보니 손으로 뭔가 만들고 싶다는 생각이 계속 들었어요. 그래서 집에서 버스로 20분 거리에 있는 작은 공방 ''나무곁''의 주말 원데이 클래스를 신청했어요. 첫 과제는 편백 도마였어요.

## 오늘 한 일

1. **재단**: 30 x 20cm, 두께 24mm 편백 판재를 받았어요. 모서리 모양을 연필로 그리고, 선생님이 밴드쏘로 잘라 주셨어요.
2. **대패질**: 손대패로 윗면을 다듬었어요. 처음엔 대패가 나무에 박혀서 안 나가더라고요. 날을 아주 조금만 내야 한다는 걸 세 번 혼나고 알았어요.
3. **샌딩**: 120방 → 220방 → 320방 순서로 사포질. 결 방향으로만 미는 게 포인트예요.
4. **손잡이 구멍**: 드릴 프레스로 지름 25mm 구멍을 뚫었어요.
5. **마감**: 식품용 미네랄 오일을 바르고 15분 뒤 마른 천으로 닦아 냈어요.

## 느낀 점

- 사포질이 생각보다 시간을 다 잡아먹어요. 세 시간 수업 중 한 시간이 사포질이었어요.
- 편백은 향이 정말 좋아요. 공방 문을 열자마자 숲 냄새가 났어요.
- 손바닥에 물집이 두 개 생겼는데 이상하게 기분이 좋았어요.

> 화면 속 일만 하다가, 손에 남는 결과물이 생기니까 묘하게 뿌듯했어요.

집에 와서 도마에 바로 사과를 썰어 먹었어요. 앞으로 주말마다 공방에 다녀 보려고 해요.', 'public', 1132, '2023-04-15T21:30:00+09:00');
insert into tag (name) values ('목공') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '공방 첫날, 편백 도마 하나 만들고 손바닥에 물집 두 개'), (select tag_id from tag where name = '목공'));
insert into tag (name) values ('도마') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '공방 첫날, 편백 도마 하나 만들고 손바닥에 물집 두 개'), (select tag_id from tag where name = '도마'));
insert into tag (name) values ('편백') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '공방 첫날, 편백 도마 하나 만들고 손바닥에 물집 두 개'), (select tag_id from tag where name = '편백'));
insert into tag (name) values ('공방') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '공방 첫날, 편백 도마 하나 만들고 손바닥에 물집 두 개'), (select tag_id from tag where name = '공방'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')) and name = '소품 만들기'), (select topic_id from topic where topic_name = '취미'), '월넛 트레이 만들기, 오일 마감 세 번의 차이', '공방 다닌 지 넉 달째. 이번엔 월넛으로 커피 트레이를 만들었어요. 월넛은 편백보다 단단하고 비싸서 실수할까 봐 손이 덜덜 떨렸어요.

## 크기

- 가로 36cm, 세로 22cm, 두께 18mm
- 안쪽을 깊이 8mm 파내고 테두리 15mm 남기기

## 과정

1. 트리머에 일자 비트를 끼우고 안쪽을 여러 번에 나눠 파냈어요. 한 번에 2mm씩, 총 네 번.
2. 모서리는 R6 라운드 비트로 둥글렸어요.
3. 파낸 바닥은 사포를 블록에 감아서 평평하게 샌딩. 바닥이 울퉁불퉁하면 컵이 흔들려요.
4. 180방 → 240방 → 400방.

## 오일 마감 비교

남은 자투리 세 조각으로 마감을 비교해 봤어요.

- **1회 도포**: 색이 살짝 진해지는 정도. 결이 덜 드러나요.
- **2회 도포**: 월넛 특유의 초콜릿색이 확 살아나요.
- **3회 도포** (사이사이 600방으로 가볍게 샌딩): 표면이 매끈하고 은은한 광이 나요.

결국 트레이는 3회 도포로 했어요. 한 번 바르고 **12시간은 말려야** 해서 완성까지 사흘이 걸렸어요. 바르고 나서 남은 오일은 꼭 닦아 내야 끈적이지 않아요.

## 주의

오일 묻은 천은 뭉쳐 두면 자연발화할 수 있대요. 펼쳐서 말리거나 물에 적셔서 버려야 한대요. 선생님이 몇 번이고 강조하셨어요.

요즘 아침 커피를 이 트레이에 올려 먹는 게 작은 행복이에요.', 'public', 1048, '2023-08-26T20:10:00+09:00');
insert into tag (name) values ('월넛') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '월넛 트레이 만들기, 오일 마감 세 번의 차이'), (select tag_id from tag where name = '월넛'));
insert into tag (name) values ('트레이') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '월넛 트레이 만들기, 오일 마감 세 번의 차이'), (select tag_id from tag where name = '트레이'));
insert into tag (name) values ('오일마감') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '월넛 트레이 만들기, 오일 마감 세 번의 차이'), (select tag_id from tag where name = '오일마감'));
insert into tag (name) values ('목공소품') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '월넛 트레이 만들기, 오일 마감 세 번의 차이'), (select tag_id from tag where name = '목공소품'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')) and name = '공구·재료 이야기'), (select topic_id from topic where topic_name = '취미'), '끌 연마 입문, 숫돌 세 개로 거울면 만들기', '목공 1년 가까이 되니 "공구는 날이 생명"이라는 말이 이제 조금 이해가 돼요. 무딘 끌로 장부를 따면 나무가 깎이는 게 아니라 뜯겨 나가요. 그래서 이번 겨울엔 연마를 제대로 배워 보기로 했어요.

## 준비물

- 숫돌 1000방, 3000방, 6000방
- 숫돌 평면 잡는 다이아몬드 판
- 연마 각도 지그
- 물 담은 통

## 순서

1. **숫돌을 물에 10분 담가요.** 기포가 안 나올 때까지.
2. 다이아몬드 판으로 숫돌 평면부터 잡아요. 숫돌이 움푹 들어가 있으면 날도 둥글게 갈려요.
3. 끌 뒷면부터. 6000방에서 뒷면 끝 2cm 정도를 평평하게 갈아요. 이게 제일 지루하고 제일 중요해요.
4. 지그에 끌을 25도로 물리고 1000방에서 앞면 갈기. 뒷면에 **버(거스러미)** 가 손톱에 걸리면 다음 단계로.
5. 3000방, 6000방으로 올라가면서 같은 동작.
6. 마지막에 30도로 살짝 2차 날을 세워요(마이크로 베벨).
7. 뒷면을 6000방에 몇 번 쓸어서 버를 떼면 끝.

## 확인 방법

종이를 세워서 날을 대 보면 사각사각 잘려 나가요. 팔 털을 깎아 보는 사람도 있는데 저는 무서워서 종이로 만족했어요.

## 한 달 해 보고

- 작업 시작 전에 6000방에서 몇 번 쓸어 주는 것만으로도 날이 오래가요.
- 공방 선생님 말씀: "날을 가는 시간이 아까우면 목공을 하면 안 된다." 맞는 말 같아요.

연마를 하고 나서 처음으로 장부 구멍 벽이 반질반질하게 나왔어요. 이 맛에 다들 숫돌을 모으나 봐요.', 'public', 605, '2024-01-20T16:40:00+09:00');
insert into tag (name) values ('끌') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '끌 연마 입문, 숫돌 세 개로 거울면 만들기'), (select tag_id from tag where name = '끌'));
insert into tag (name) values ('숫돌') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '끌 연마 입문, 숫돌 세 개로 거울면 만들기'), (select tag_id from tag where name = '숫돌'));
insert into tag (name) values ('연마') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '끌 연마 입문, 숫돌 세 개로 거울면 만들기'), (select tag_id from tag where name = '연마'));
insert into tag (name) values ('수공구') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '끌 연마 입문, 숫돌 세 개로 거울면 만들기'), (select tag_id from tag where name = '수공구'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')) and name = '가구'), (select topic_id from topic where topic_name = '취미'), '첫 가구, 장부맞춤 원목 스툴 만든 3주의 기록', '소품만 만들다가 드디어 첫 가구! 화장대 앞에 둘 작은 스툴이에요. 못이나 나사 없이 **장부맞춤**으로만 짜는 게 목표였어요.

## 설계

- 재료: 애쉬(물푸레나무) 판재
- 크기: 상판 34 x 28cm, 높이 45cm
- 다리 40 x 40mm 4개, 가로대 위아래 8개
- 다리와 가로대는 장부맞춤, 상판은 목심으로 고정

## 1주차: 재단과 먹금

다리 네 개 길이를 정확히 맞추는 데만 두 시간이 걸렸어요. 1mm만 달라도 스툴이 흔들리니까요. 그리고 장부 위치를 금긋기 칼로 하나하나 표시했어요. 연필보다 칼로 그은 선이 훨씬 정확해요.

## 2주차: 장부 따기

- 암장부(구멍): 각끌기로 대충 파고 끌로 마무리
- 숫장부(촉): 등대기톱으로 어깨선을 자르고 끌로 다듬기

열여섯 개의 장부 중 세 개는 헐거웠어요. 얇은 나무 조각을 접착제로 붙여서 메웠어요. 선생님이 "다들 그렇게 배워요"라고 위로해 주셨어요.

## 3주차: 조립과 마감

1. 앞뒤 한 면씩 먼저 접착제 바르고 클램프로 조여 하루 건조.
2. 두 면을 가로대로 연결해 다시 하루.
3. 상판은 계절에 따라 나무가 움직이니까 목심 구멍을 살짝 길게 팠어요.
4. 오일 2회 마감.

## 결과

앉아 보니 흔들림이 없어요. 대각선 길이를 재 보니 차이가 1mm 안쪽이었어요. 장부가 꽉 물리는 그 "턱" 소리가 아직도 귀에 남아요.', 'public', 758, '2024-06-15T22:00:00+09:00');
insert into tag (name) values ('스툴') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '첫 가구, 장부맞춤 원목 스툴 만든 3주의 기록'), (select tag_id from tag where name = '스툴'));
insert into tag (name) values ('장부맞춤') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '첫 가구, 장부맞춤 원목 스툴 만든 3주의 기록'), (select tag_id from tag where name = '장부맞춤'));
insert into tag (name) values ('원목가구') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '첫 가구, 장부맞춤 원목 스툴 만든 3주의 기록'), (select tag_id from tag where name = '원목가구'));
insert into tag (name) values ('애쉬') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '첫 가구, 장부맞춤 원목 스툴 만든 3주의 기록'), (select tag_id from tag where name = '애쉬'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')) and name = '공구·재료 이야기'), (select topic_id from topic where topic_name = '취미'), '트리머 1년 사용기, 처음 살 때 알았으면 좋았을 것', '작년 초에 집에서도 작업하고 싶어서 트리머를 하나 샀어요. 1년 동안 써 보고 느낀 점을 정리해 봐요. 특정 제품 추천이 아니라 고를 때 볼 것들 위주예요.

## 고를 때 볼 것

- **콜렛 지름**: 6mm와 1/4인치(6.35mm)가 있어요. 비트를 살 때 꼭 맞춰야 해요. 저는 처음에 이걸 몰라서 비트 세트를 다시 샀어요.
- **속도 조절**: 단단한 나무나 큰 비트는 속도를 낮춰야 타지 않아요. 속도 다이얼은 꼭 있어야 해요.
- **베이스 종류**: 기본 평베이스 말고 각도 베이스, 플런지 베이스가 따로 있어요. 처음엔 평베이스만으로 충분했어요.

## 가장 많이 쓴 비트 4개

1. 일자 비트 6mm: 홈 파기, 트레이 안쪽 파내기
2. 라운드 비트 R3, R6: 모서리 둥글리기
3. 베어링 달린 일자 비트: 템플릿 따라 똑같이 따기
4. 45도 모따기 비트

## 실수했던 것

- **진행 방향**: 트리머는 정해진 방향으로 밀어야 해요. 반대로 밀었다가 공구가 확 끌려가서 깜짝 놀랐어요.
- **한 번에 깊게**: 욕심내서 한 번에 6mm를 팠더니 나무가 탔어요. 2~3mm씩 나눠 파요.
- **집중력 떨어진 상태로 작업**: 저녁 늦게 피곤할 때 하다가 트레이 모서리를 날려 먹었어요.

## 아파트에서 쓸 때

소음이 꽤 커요. 저는 주말 낮 2시~5시에만, 베란다 창 닫고 써요. 집진기 없이 쓰면 톱밥이 온 집에 날려서 결국 공방에 가져가서 하는 일이 많아졌어요.

> 공구는 일을 빨리 해 주지만, 실수도 빨리 만들어 줘요.

보안경, 귀마개, 마스크는 꼭 쓰세요. 이건 1년 동안 한 번도 안 빼먹었어요.', 'public', 676, '2025-01-11T15:30:00+09:00');
insert into tag (name) values ('트리머') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '트리머 1년 사용기, 처음 살 때 알았으면 좋았을 것'), (select tag_id from tag where name = '트리머'));
insert into tag (name) values ('전동공구') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '트리머 1년 사용기, 처음 살 때 알았으면 좋았을 것'), (select tag_id from tag where name = '전동공구'));
insert into tag (name) values ('사용기') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '트리머 1년 사용기, 처음 살 때 알았으면 좋았을 것'), (select tag_id from tag where name = '사용기'));
insert into tag (name) values ('목공입문') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '트리머 1년 사용기, 처음 살 때 알았으면 좋았을 것'), (select tag_id from tag where name = '목공입문'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')) and name = '가구'), (select topic_id from topic where topic_name = '취미'), '서랍 하나 달린 협탁, 서랍 레일 없이 나무로만', '침대 옆에 둘 협탁을 만들었어요. 스툴 이후 두 번째 가구인데, 이번 과제는 **서랍**이었어요. 금속 레일 없이 나무끼리 미끄러지는 서랍을 만들고 싶었어요.

## 설계

- 재료: 몸통은 오크, 서랍 옆판은 단풍나무
- 크기: 가로 40, 깊이 35, 높이 55cm
- 서랍 내부 가로 34 x 깊이 30 x 높이 9cm

## 도브테일 맞춤

서랍 앞판과 옆판은 반숨은 도브테일로 짰어요. 주먹장이라고도 해요. 앞에서 보면 맞춤이 안 보이고 옆에서만 보이는 방식이에요.

1. 옆판에 꼬리 모양을 먼저 그리고 등대기톱으로 켜요.
2. 그 꼬리를 앞판에 대고 칼로 따라 그려요.
3. 앞판 쪽 홈을 끌로 조금씩 파내요.

첫 번째 맞춤은 헐거워서 버렸고, 두 번째에야 손바닥으로 툭툭 쳐서 들어가는 정도가 됐어요.

## 서랍이 부드럽게 움직이려면

- 서랍과 몸통 사이 틈을 **양쪽 0.5mm 정도** 남겨요. 꽉 맞으면 여름에 나무가 부풀어서 안 열려요.
- 서랍이 닿는 면에 양초를 문질러 줘요. 이게 놀라울 정도로 효과가 좋아요.
- 서랍 바닥판은 홈에 끼우기만 하고 접착하지 않아요. 나무가 움직일 자리를 남겨 두는 거예요.

## 완성

마지막 오일을 바르고 서랍을 밀어 넣었을 때 안의 공기가 빠지면서 "슉" 하고 들어가는 소리가 났어요. 그 소리 들으려고 하루에 몇 번씩 여닫고 있어요.', 'public', 886, '2025-05-24T21:20:00+09:00');
insert into tag (name) values ('협탁') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '서랍 하나 달린 협탁, 서랍 레일 없이 나무로만'), (select tag_id from tag where name = '협탁'));
insert into tag (name) values ('서랍') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '서랍 하나 달린 협탁, 서랍 레일 없이 나무로만'), (select tag_id from tag where name = '서랍'));
insert into tag (name) values ('도브테일') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '서랍 하나 달린 협탁, 서랍 레일 없이 나무로만'), (select tag_id from tag where name = '도브테일'));
insert into tag (name) values ('원목가구') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '서랍 하나 달린 협탁, 서랍 레일 없이 나무로만'), (select tag_id from tag where name = '원목가구'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')) and name = '공구·재료 이야기'), (select topic_id from topic where topic_name = '여행'), '양평 목재소 다녀온 날, 판재 고르는 법 배운 기록', '공방 선생님과 회원 넷이서 양평에 있는 작은 목재소 ''물소리목재''에 다녀왔어요. 늘 공방에 있는 나무만 쓰다가 처음으로 직접 고르러 간 날이에요. 가는 길에 강변 단풍이 한창이라 반은 나들이였어요.

## 아침

8시에 공방 앞에서 승합차로 출발했어요. 중간에 휴게소에서 호두과자를 사 먹고, 10시쯤 도착. 목재소 마당에 통나무가 쌓여 있고 제재기 소리가 계속 났어요.

## 판재 고르는 법

사장님이 직접 알려 주신 것들이에요.

- **결 보기**: 마구리(나무 끝 단면)를 보고 나이테가 세로로 서 있는 판이 덜 휘어요.
- **함수율**: 수분 측정기로 재서 10% 안쪽인 걸 골라요. 덜 마른 나무는 집에 가서 휘어요.
- **옹이**: 살아 있는 옹이는 괜찮지만, 검고 빠질 것 같은 죽은 옹이는 피해요.
- **한쪽 눈 감고 보기**: 판재를 눈높이로 들고 길이 방향으로 보면 휨이 보여요.

## 오늘 산 것

- 체리 판재 2장 (책장 만들 예정)
- 호두나무 자투리 한 묶음 (소품용)

## 점심과 돌아오는 길

목재소 근처 ''두물국수''에서 잔치국수랑 메밀전을 먹었어요. 돌아오는 길엔 두물머리에 잠깐 들러 강변을 걸었어요. 나무 냄새 묻은 옷 그대로 물안개 보는 기분이 좋았어요.

차 트렁크에 체리 판재를 싣고 오면서 무엇을 만들지 계속 이야기했어요. 재료를 직접 고르니까 벌써 그 나무에 정이 들어요.', 'public', 421, '2025-11-08T19:40:00+09:00');
insert into tag (name) values ('양평') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '양평 목재소 다녀온 날, 판재 고르는 법 배운 기록'), (select tag_id from tag where name = '양평'));
insert into tag (name) values ('목재소') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '양평 목재소 다녀온 날, 판재 고르는 법 배운 기록'), (select tag_id from tag where name = '목재소'));
insert into tag (name) values ('판재') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '양평 목재소 다녀온 날, 판재 고르는 법 배운 기록'), (select tag_id from tag where name = '판재'));
insert into tag (name) values ('나들이') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '양평 목재소 다녀온 날, 판재 고르는 법 배운 기록'), (select tag_id from tag where name = '나들이'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')) and name = '가구'), (select topic_id from topic where topic_name = '취미'), '체리 판재로 만든 4단 책장, 반년 걸린 이유', '작년 11월 양평에서 사 온 체리 판재가 드디어 책장이 됐어요. 주말마다 조금씩, 거의 반년 걸렸어요.

## 왜 이렇게 오래 걸렸나

1. **나무 길들이기 2주**: 집으로 가져온 판재를 바로 쓰지 않고 공방 환경에 적응시켰어요.
2. **설계만 한 달**: 책 무게를 생각하니 선반 두께, 지지 방식이 다 고민이었어요.
3. **선반 처짐 테스트**: 자투리로 같은 두께 선반을 만들고 책을 올려 2주 동안 처지는지 봤어요.

## 최종 설계

- 크기: 가로 80, 깊이 28, 높이 140cm
- 옆판 22mm, 선반 20mm
- 선반은 옆판에 **통맞춤(다도)** 홈을 파서 끼우기
- 뒤판은 6mm 합판을 홈에 끼워 뒤틀림 방지
- 맨 위와 맨 아래 선반만 접착, 가운데 두 칸은 높이 조절용 선반핀

## 작업에서 어려웠던 것

- 다도 홈 네 개를 양쪽 옆판에 **똑같은 높이로** 파는 것. 두 옆판을 나란히 붙여 놓고 한꺼번에 금을 그었어요.
- 체리는 트리머를 쓰면 타기 쉬워요. 속도를 낮추고 얕게 여러 번.
- 조립할 때 클램프가 모자라서 공방 회원들 것을 빌렸어요.

## 체리 나무의 매력

처음엔 연한 분홍빛인데 **햇빛을 받으면 몇 달에 걸쳐 진한 적갈색으로** 변한대요. 그래서 마감은 투명 오일만 했어요. 어떻게 변해 갈지 지켜보려고요.

책을 다 꽂고 나니 방이 갑자기 서재처럼 보였어요. 반년이 아깝지 않아요.', 'public', 442, '2026-03-21T20:50:00+09:00');
insert into tag (name) values ('책장') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '체리 판재로 만든 4단 책장, 반년 걸린 이유'), (select tag_id from tag where name = '책장'));
insert into tag (name) values ('체리') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '체리 판재로 만든 4단 책장, 반년 걸린 이유'), (select tag_id from tag where name = '체리'));
insert into tag (name) values ('원목가구') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '체리 판재로 만든 4단 책장, 반년 걸린 이유'), (select tag_id from tag where name = '원목가구'));
insert into tag (name) values ('가구만들기') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '체리 판재로 만든 4단 책장, 반년 걸린 이유'), (select tag_id from tag where name = '가구만들기'));
insert into post (category_id, topic_id, title, content, visibility, views, created_at) values ((select category_id from category where blog_id = (select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')) and name = '소품 만들기'), (select topic_id from topic where topic_name = '취미'), '캠핑용 접이식 테이블, 주말 4번에 끝낸 제작기', '가을 캠핑 시즌이 와서 작은 접이식 테이블을 만들었어요. 시판 알루미늄 테이블은 가볍지만 덜 예뻐서, 가볍고 예쁜 걸 목표로 했어요. 주말 네 번 만에 끝났어요.

## 설계

- 상판: 가로 60 x 세로 40cm, 폭 5cm 오동나무 각재 8줄을 띄워 붙이기
- 다리: 너도밤나무, X자로 교차해 접히는 구조
- 무게 목표 3kg 이하

## 1주차: 상판

오동나무는 정말 가벼워요. 대신 무르니까 모서리를 R3로 둥글려 찍힘을 줄였어요. 각재 사이 5mm씩 띄워서 아래 가로대에 나사로 고정했어요. 물이 고이지 않게 하려고요.

## 2주차: 다리

X자로 교차하는 지점에 지름 8mm 볼트를 넣어 축을 만들었어요. 두 다리가 정확히 같은 위치에서 교차해야 테이블이 수평이 돼요. 그래서 두 다리를 겹쳐 놓고 한 번에 구멍을 뚫었어요.

## 3주차: 잠금 장치

펼쳤을 때 접히지 않게 상판 아래에 홈을 파서 다리 끝이 걸리도록 했어요. 처음엔 홈이 너무 얕아서 팔꿈치를 대자 테이블이 주저앉았어요. 홈을 6mm에서 12mm로 깊게 다시 팠어요.

## 4주차: 마감

야외에서 쓸 거라 오일 대신 **우레탄 바니시** 3회. 비 맞아도 버티게요.

## 결과

- 무게 2.7kg, 목표 달성
- 접으면 두께 6cm
- 지난 주말 가평 캠핑장에서 첫 개시

옆 텐트 분이 어디서 샀냐고 물어보셔서, 만들었다고 대답할 때 좀 많이 뿌듯했어요. 3년 반 전 물집 잡힌 손으로 도마 하나 만들던 때를 생각하면 신기해요.', 'public', 209, '2026-09-26T21:15:00+09:00');
insert into tag (name) values ('캠핑테이블') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '캠핑용 접이식 테이블, 주말 4번에 끝낸 제작기'), (select tag_id from tag where name = '캠핑테이블'));
insert into tag (name) values ('접이식') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '캠핑용 접이식 테이블, 주말 4번에 끝낸 제작기'), (select tag_id from tag where name = '접이식'));
insert into tag (name) values ('목공') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '캠핑용 접이식 테이블, 주말 4번에 끝낸 제작기'), (select tag_id from tag where name = '목공'));
insert into tag (name) values ('주말작업') on conflict (name) do nothing;
insert into post_tag (post_id, tag_id) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '캠핑용 접이식 테이블, 주말 4번에 끝낸 제작기'), (select tag_id from tag where name = '주말작업'));

-- 댓글
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.homecook@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '23리터 미니오븐으로 구운 첫 식빵, 손반죽 40분의 기록'), '손반죽 40분이라니 대단해요! 갓 구운 식빵 냄새 상상만 해도 행복해요.', '2023-05-21T14:04:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.woodwork@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '23리터 미니오븐으로 구운 첫 식빵, 손반죽 40분의 기록'), '결대로 찢어지는 순간의 기쁨, 뭔가 만드는 사람은 다 아는 그 느낌이죠.', '2023-05-22T14:50:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.codecup@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '버터쿠키가 팬케이크처럼 퍼졌다, 원인 세 가지'), '온도계로 재 보니 18도 차이ㅋㅋ 디버깅 과정 보는 것 같아요.', '2023-11-06T12:18:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.film@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '버터쿠키가 팬케이크처럼 퍼졌다, 원인 세 가지'), '버터 수프라는 표현에 빵 터졌어요. 요거트 토핑 활용 좋네요.', '2023-11-06T19:15:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.homecook@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '버터쿠키가 팬케이크처럼 퍼졌다, 원인 세 가지'), '오븐 다이얼 믿지 말라는 거 저도 동감이에요. 저희 집 것도 15도쯤 낮더라고요.', '2023-11-07T01:25:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.mukja@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '드디어 정착한 초코칩 쿠키, 겉바속쫀 비율'), '브라운버터에 플레이크 소금이라니, 이건 카페 쿠키 레시피네요. 저장합니다!', '2024-04-15T07:26:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.plant@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '드디어 정착한 초코칩 쿠키, 겉바속쫀 비율'), '하룻밤 숙성 참을 가치 있다는 말 믿고 이번 주말에 도전해볼게요.', '2024-04-15T14:09:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.frontnote@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '드디어 정착한 초코칩 쿠키, 겉바속쫀 비율'), '열 번 넘게 반복해서 찾은 레시피라 더 믿음이 가요.', '2024-04-14T22:16:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.homecook@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '탕종법 우유식빵, 사흘이 지나도 촉촉했다'), '탕종법 설명이 정말 쉽게 되어 있네요. 저희도 식빵 남으면 늘 아까웠는데 도전해볼게요.', '2024-10-28T02:12:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.halfday@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '탕종법 우유식빵, 사흘이 지나도 촉촉했다'), '덧가루 참는 거 진짜 어렵죠ㅋㅋ 사흘 비교 실험까지 하시다니 멋져요.', '2024-10-29T04:10:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.film@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '발렌타인에 구운 휘낭시에와 마들렌, 같은 듯 다른 둘'), '마들렌 배꼽 성공 축하해요! 다른 점 정리 덕분에 차이가 한눈에 보여요.', '2025-02-16T10:45:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.frontnote@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '발렌타인에 구운 휘낭시에와 마들렌, 같은 듯 다른 둘'), '남은 노른자로 커스터드까지, 재료 남기지 않는 설계 멋지네요.', '2025-02-16T03:09:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.runner@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '발렌타인에 구운 휘낭시에와 마들렌, 같은 듯 다른 둘'), '회사 동료분들 부럽습니다. 저도 이런 동료 있었으면ㅋㅋ', '2025-02-15T13:44:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.plant@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '한여름 과발효 사건, 반죽이 볼을 넘어왔다'), '식물도 여름엔 물 주는 법이 달라지는데 빵도 그렇군요. 31도면 반죽이 신났겠어요.', '2025-07-07T04:54:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.homecook@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '한여름 과발효 사건, 반죽이 볼을 넘어왔다'), '러스크로 살려낸 거 대단해요. 냉장 발효 팁 좋네요.', '2025-07-07T21:05:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.woodwork@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '크리스마스 이브의 첫 딸기 생크림 케이크'), '돌림판 있으면 확실히 달라요. 옆면을 딸기로 가린 순발력이 오히려 더 예뻐 보였을 것 같아요.', '2025-12-25T17:07:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.mukja@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '크리스마스 이브의 첫 딸기 생크림 케이크'), '첫 홀케이크에 4.5cm면 대성공이에요! 조카분 반응 최고네요.', '2025-12-27T01:08:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.sea@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '크리스마스 이브의 첫 딸기 생크림 케이크'), '크리스마스 이브에 이런 글 읽으니 마음이 따뜻해져요.', '2025-12-26T16:34:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.homecook@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '엄마 생일 당근케이크, 크림치즈 프로스팅 꾸덕하게'), '어머니께 이보다 좋은 선물이 있을까요. 덜 달아서 좋다는 말이 최고의 칭찬이네요.', '2026-05-11T10:48:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.halfday@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '엄마 생일 당근케이크, 크림치즈 프로스팅 꾸덕하게'), '건포도 럼에 불리는 디테일 좋아요. 당근케이크 너무 먹고 싶어지네요.', '2026-05-12T18:27:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.codecup@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '베이킹 3년 차, 망친 것들 모아 보니 보이는 것'), '실패 원인을 패턴으로 묶은 게 회고 문서 같아요. 온도, 조급함, 문서 대충 읽기는 개발에도 똑같네요.', '2026-09-22T14:01:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.woodwork@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '베이킹 3년 차, 망친 것들 모아 보니 보이는 것'), '목공도 조급하면 꼭 사고가 나요. 3년 축하드려요!', '2026-09-22T04:59:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.hometrain@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '베이킹 3년 차, 망친 것들 모아 보니 보이는 것'), '마지막 인용구에서 웃었어요ㅋㅋ 같이 운동하시죠.', '2026-09-22T12:33:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.mukja@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '베이킹 3년 차, 망친 것들 모아 보니 보이는 것'), '새 오븐 첫 작품도 기대할게요!', '2026-09-25T00:26:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.frontnote@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '@Transactional이 안 먹던 이유: 같은 클래스 안에서 부르면 프록시를 안 탄다'), '프록시라서 내부 호출은 안 탄다는 거, 리액트에서 클로저 때문에 옛날 값 잡히는 거랑 비슷한 느낌으로 헷갈리네요ㅋㅋ 정리 감사합니다.', '2023-03-07T12:03:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.hometrain@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '@Transactional이 안 먹던 이유: 같은 클래스 안에서 부르면 프록시를 안 탄다'), '개발은 잘 모르지만 ''누가 어떤 경로로 부르는지 봐야 한다''는 말은 왠지 운동 자세에도 통하는 것 같아요.', '2023-03-07T06:24:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.frontnote@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'JPA N+1 문제, fetch join이랑 batch size로 잡은 기록'), '프론트에서 볼 땐 그냥 API 하나가 느린 건데 뒤에서 51번 돌고 있었군요... 개수만 필요하면 DTO로 가는 거 공감합니다.', '2023-07-25T04:37:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.frontnote@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '1년 차 회고: 돌아가는 코드에서 설명할 수 있는 코드로'), '''이렇게 하니까 돌아가서요'' 저도 해 본 대답이라 뜨끔하네요. 1년 수고 많으셨어요!', '2024-01-01T22:47:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.film@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '1년 차 회고: 돌아가는 코드에서 설명할 수 있는 코드로'), '30분 규칙 좋네요. 개발 아니어도 쓸 수 있을 것 같아요.', '2024-01-02T17:06:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.hometrain@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '1년 차 회고: 돌아가는 코드에서 설명할 수 있는 코드로'), '한 달에 글 하나 목표 응원합니다. 기록이 쌓이면 진짜 힘이 돼요.', '2024-01-04T03:29:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.frontnote@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'PostgreSQL에서 인덱스 탔는지 EXPLAIN ANALYZE로 확인하기'), 'Sort 단계가 사라지는 부분 보니까 속이 다 시원하네요. 812ms에서 0.1ms라니...', '2024-04-15T21:51:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.halfday@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'PostgreSQL에서 인덱스 탔는지 EXPLAIN ANALYZE로 확인하기'), 'CONCURRENTLY는 트랜잭션 안에서 안 된다는 거 메모해 둡니다. 저도 언젠가 밟을 것 같아서요.', '2024-04-16T06:34:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.frontnote@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'Docker 컨테이너에서 Spring Boot가 DB에 못 붙던 이유: localhost의 함정'), '프론트 개발 서버에서 API 프록시 잡을 때도 똑같은 걸로 헤맨 적 있어요. 컨테이너 안 localhost 진짜 함정이에요.', '2024-10-22T08:07:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.woodwork@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'Docker 컨테이너에서 Spring Boot가 DB에 못 붙던 이유: localhost의 함정'), '무슨 말인지 반쯤 알아들었는데 ''내 컴퓨터가 어디인지부터''라는 마지막 줄이 너무 웃기고 공감돼요.', '2024-10-22T04:20:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.frontnote@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'Testcontainers로 통합 테스트 옮긴 후기'), '프론트에서 jsdom이랑 실제 브라우저 차이 때문에 고생한 거랑 비슷하네요. 결국 진짜 환경에서 돌려야 안심이 되더라고요.', '2025-02-10T20:24:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.halfday@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'Testcontainers로 통합 테스트 옮긴 후기'), '4분에서 11분 됐을 때 식은땀 났을 것 같아요ㅋㅋ 베이스 클래스로 공유하는 거 참고합니다.', '2025-02-11T12:13:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.frontnote@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'PostgreSQL 데드락 로그 읽는 법, 이번엔 진짜 데드락이었다'), '로그 한 줄씩 해석해 주시니까 처음으로 데드락 로그가 읽히네요.', '2025-08-18T09:26:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.film@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'PostgreSQL 데드락 로그 읽는 법, 이번엔 진짜 데드락이었다'), '문을 서로 다른 순서로 잠근다는 비유가 머리에 쏙 들어와요.', '2025-08-20T03:11:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.hometrain@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '2025 회고: 3년 차가 되면서 달라진 것들'), '앗 제 루틴 따라 하신다니 영광이에요! 허리 신호 올 땐 글루트 브릿지랑 버드독부터 해 보세요.', '2026-01-04T22:57:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.frontnote@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '2025 회고: 3년 차가 되면서 달라진 것들'), '정산 규칙 표로 정리한 거 너무 좋은 습관이네요. 올해도 삽질기 기대할게요.', '2026-01-06T20:59:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.runner@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '2025 회고: 3년 차가 되면서 달라진 것들'), '주 3회 운동 목표 같이 지켜 봐요. 3년 차 축하드립니다!', '2026-01-06T04:14:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.frontnote@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '커넥션 풀 고갈 장애 회고: HikariCP 로그를 따라간 길'), '프론트에서는 그냥 ''서버가 느려요''로 끝나는데 이런 일이 있었군요. 트랜잭션 안에는 DB 일만, 기억해 둘게요.', '2026-09-29T03:02:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.halfday@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '커넥션 풀 고갈 장애 회고: HikariCP 로그를 따라간 길'), '풀 크기 늘리면 20개가 다 묶였을 거라는 말에 고개 끄덕였어요.', '2026-09-28T19:56:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.hometrain@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '커넥션 풀 고갈 장애 회고: HikariCP 로그를 따라간 길'), '1년 차 때 글이 여기서 다시 나오니까 성장 일지 보는 것 같아요.', '2026-09-29T17:17:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.sea@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '첫 필름카메라, 중고 수동 SLR을 들이다'), '필름 감성 너무 좋아요! 첫 롤 결과 궁금해서 기다릴게요.', '2023-04-09T04:23:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.woodwork@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '첫 필름카메라, 중고 수동 SLR을 들이다'), 'LR44 챙겨 가신 거 센스 있네요. 저도 오래된 공구 살 때 비슷하게 체크리스트 만들어 가요.', '2023-04-11T00:57:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.halfday@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '가을 서촌 골목 출사 기록'), '서촌 저도 좋아하는 코스예요. 수성동 계곡 단풍은 11월 초가 진짜 절정이더라고요.', '2023-10-23T05:06:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.plant@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '가을 서촌 골목 출사 기록'), '한옥 담벼락이랑 필름 색감 조합 상상만 해도 좋네요.', '2023-10-22T12:44:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.runner@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '가을 서촌 골목 출사 기록'), '1만 4천 보면 거의 10km 가까이 걸으셨네요 ㅎㅎ', '2023-10-22T23:18:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.codecup@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '현상소에 필름 맡기는 법과 스캔 해상도 고르는 기준'), '롤 번호 적는 거 완전 공감이요. 파일명 규칙 안 정하면 나중에 지옥이죠.', '2024-03-17T21:18:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.bakery@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '현상소에 필름 맡기는 법과 스캔 해상도 고르는 기준'), '되감기 실수 이야기 보니까 오븐 문 미리 열었다 망한 케이크가 생각나네요 ㅠㅠ', '2024-03-18T21:24:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.frontnote@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '흑백 필름 1스톱 증감 현상 후기 (ISO 400 → 800)'), '검정이 깊어진다는 표현 좋네요. 밤 골목 사진 꼭 보고 싶어요.', '2024-08-04T11:41:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.sea@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '흑백 필름 1스톱 증감 현상 후기 (ISO 400 → 800)'), '을지로 밤 풍경이랑 흑백 조합은 반칙이죠.', '2024-08-04T18:39:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.halfday@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '군산 1박 2일 필름 여행, 세 롤 다 썼다'), '군산 철길마을 아침 일찍 가신 거 진짜 잘하셨어요. 낮엔 사람 너무 많더라고요.', '2025-02-23T11:12:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.mukja@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '군산 1박 2일 필름 여행, 세 롤 다 썼다'), '짬뽕집 40분이면 양반이에요 ㅋㅋ 저 갔을 땐 한 시간 넘게 기다렸어요.', '2025-02-23T18:30:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.runner@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '군산 1박 2일 필름 여행, 세 롤 다 썼다'), '철길 소실점 구도 상상되네요. 사진 올려 주세요!', '2025-02-24T08:49:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.plant@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '장마철 필름 보관, 결국 냉장고 한 칸을 비웠다'), '저는 반대로 습도 올리느라 가습기 돌리는데 ㅎㅎ 식물이랑 필름은 같은 방에 두면 안 되겠네요.', '2025-07-13T09:54:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.bakery@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '장마철 필름 보관, 결국 냉장고 한 칸을 비웠다'), '냉장고 칸 선언 공감해요. 저도 버터 칸 따로 있어요.', '2025-07-14T10:38:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.homecook@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '장마철 필름 보관, 결국 냉장고 한 칸을 비웠다'), '반찬통 침범 금지 스티커에서 웃었어요 ㅋㅋ', '2025-07-14T23:24:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.sea@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '하프카메라로 벚꽃 72컷 찍은 날'), '두 컷 붙어서 나오는 게 오히려 이야기가 생기네요. 저도 하나 사고 싶어졌어요.', '2026-03-30T03:32:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.frontnote@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '하프카메라로 벚꽃 72컷 찍은 날'), '가로세로 반대인 거 처음엔 진짜 헷갈릴 것 같아요 ㅋㅋ', '2026-03-30T09:07:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.codecup@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '노출계 없이 ''써니 16'' 한 달 연습 기록'), '추측 → 측정 → 오차 기록, 완전 실험 설계네요. 85%면 대단해요.', '2026-08-24T09:18:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.woodwork@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '노출계 없이 ''써니 16'' 한 달 연습 기록'), '그늘이 생각보다 어둡다는 거 공방에서 조명 달 때도 똑같이 느껴요.', '2026-08-24T18:12:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.runner@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '노출계 없이 ''써니 16'' 한 달 연습 기록'), '저도 페이스 감으로 맞추기 연습하는데 비슷한 방식이라 반갑네요.', '2026-08-27T01:58:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.sea@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '부산 영도 흰여울마을, 바다 냄새 나는 필름 산책'), '흰여울마을 골목에서 바다 열리는 그 지점 저도 알아요! 진짜 숨 멈추게 되죠.', '2026-10-05T10:19:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.halfday@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '부산 영도 흰여울마을, 바다 냄새 나는 필름 산책'), '할머니 에피소드 너무 따뜻하네요. 스캔본 기다릴게요.', '2026-10-06T09:05:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.mukja@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '부산 영도 흰여울마을, 바다 냄새 나는 필름 산책'), '영도 가셨으면 근처 조개구이도 드셨나요? ㅎㅎ', '2026-10-06T17:31:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.codecup@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'useEffect 의존성 배열 때문에 API를 무한으로 부른 삽질기'), '그날 메시지 보낸 백엔드 개발자 입장도 상상되네요ㅋㅋ 서버 로그에 같은 요청이 쭉 찍히면 깜짝 놀라요.', '2023-06-20T04:22:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.hometrain@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'useEffect 의존성 배열 때문에 API를 무한으로 부른 삽질기'), '개발 글은 잘 모르는데 ''폭포처럼 흘러내렸다''는 표현이 너무 생생해요.', '2023-06-20T23:23:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.woodwork@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'CSS Grid 카드 목록, auto-fill이랑 auto-fit 차이 이제 안 헷갈리기'), '카드 2장일 때 차이 설명이 딱 와닿네요. 제 공방 작품 목록 페이지가 auto-fit이라 이상하게 넓었던 거였어요!', '2023-11-14T00:40:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.codecup@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'CSS Grid 카드 목록, auto-fill이랑 auto-fit 차이 이제 안 헷갈리기'), 'min(240px, 100%) 트릭 좋네요. 관리자 화면 만들 때 써먹겠습니다.', '2023-11-13T19:20:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.codecup@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '버튼 대신 div에 onClick 달면 생기는 일'), 'type="button" 안 적어서 폼 제출되는 거 백엔드에서 이상한 요청으로 본 적 있어요ㅋㅋ', '2024-03-05T04:37:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.film@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '버튼 대신 div에 onClick 달면 생기는 일'), 'Tab만으로 써 보기 해 봤는데 진짜 막히는 데가 많네요. 좋은 습관 배워 갑니다.', '2024-03-04T10:52:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.plant@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '버튼 대신 div에 onClick 달면 생기는 일'), '아이콘만 있는 버튼에 이름 붙이는 거, 저처럼 화면 확대해서 보는 사람한테도 도움 돼요.', '2024-03-05T12:02:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.bakery@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '사이드 프로젝트: 동네 빵집 지도 만들기 1편, 기획부터 첫 배포까지'), '빵집 지도라니 너무 반가워요! 쉬는 날 필터는 진짜 필요하죠. 헛걸음한 적이 한두 번이 아니라서요.', '2024-08-26T23:06:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.mukja@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '사이드 프로젝트: 동네 빵집 지도 만들기 1편, 기획부터 첫 배포까지'), '완성되면 공유해 주세요. 빵지순례 동선 짤 때 쓰고 싶어요.', '2024-08-26T06:23:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.codecup@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '사이드 프로젝트: 동네 빵집 지도 만들기 1편, 기획부터 첫 배포까지'), 'getDay() 일요일 0 함정 저도 백엔드에서 요일 계산하다 밟은 적 있어요ㅋㅋ', '2024-08-28T04:49:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.codecup@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'TypeScript 유니온 타입으로 로딩·에러 상태 정리하기'), 'never로 빠뜨린 케이스 잡는 거 자바 sealed interface + switch랑 느낌이 비슷하네요. 깔끔합니다.', '2025-01-20T01:57:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.halfday@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'TypeScript 유니온 타입으로 로딩·에러 상태 정리하기'), '로딩 중인데 에러도 떠 있는 화면 본 적 있어요ㅋㅋ 이런 이유였군요.', '2025-01-21T11:41:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.bakery@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '다크 모드, CSS 변수로 한 번에 정리한 방법'), '빵집 지도 다크 모드까지! 밤에 내일 갈 빵집 찾을 때 딱이겠어요.', '2025-06-09T15:15:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.codecup@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '다크 모드, CSS 변수로 한 번에 정리한 방법'), 'color-scheme 안 주면 스크롤바만 하얗게 남는 거 관리자 화면에서 본 적 있네요.', '2025-06-09T18:50:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.plant@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '모달 접근성 체크리스트: 포커스, Esc, 그리고 dialog 요소'), '닫히고 나서 포커스가 맨 위로 튀는 사이트 정말 많아요. 이런 글 반갑습니다.', '2025-12-01T23:05:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.codecup@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '모달 접근성 체크리스트: 포커스, Esc, 그리고 dialog 요소'), '삭제 확인에서 취소에 autoFocus 주는 거 디테일 좋네요. 운영자 화면에도 적용해 달라고 해야겠어요.', '2025-12-02T09:24:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.sea@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '제주 워케이션 일주일, 일하고 걷고 귤 먹은 기록'), '동쪽 바닷가 마을 저도 좋아하는 곳이에요. 바람 부는 날 산책 극기 훈련 공감합니다ㅋㅋ', '2026-05-18T20:22:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.halfday@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '제주 워케이션 일주일, 일하고 걷고 귤 먹은 기록'), '열흘이 적당하다는 말 메모해 둘게요. 저도 워케이션 가 보고 싶어요.', '2026-05-18T16:43:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.hometrain@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '제주 워케이션 일주일, 일하고 걷고 귤 먹은 기록'), '식탁 의자 8시간은 진짜 허리 망가져요. 쉬는 시간에 스트레칭 꼭 하세요!', '2026-05-20T00:19:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.bakery@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'React 19의 폼 액션과 useActionState, 실제로 써 본 정리'), '제보하기 생겼다니 기대돼요! 저희 동네 소금빵 집 꼭 제보할게요.', '2026-10-05T06:24:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.codecup@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'React 19의 폼 액션과 useActionState, 실제로 써 본 정리'), '서버 쪽 검증은 따로 하시죠? 프론트 검증만 믿으면 안 되는 건 같이 강조하고 싶어요ㅋㅋ', '2026-10-07T03:59:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.mukja@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'React 19의 폼 액션과 useActionState, 실제로 써 본 정리'), '빵집 지도 계속 업데이트되는 거 보니까 좋네요. 언젠가 꼭 써 볼게요.', '2026-10-07T22:35:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.sea@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '양평 두물머리 반나절, 경의중앙선 타고 다녀옴'), '느티나무 뒤통수 사진이라니ㅎㅎ 저도 그런 사진이 더 좋더라고요.', '2023-03-06T10:07:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.runner@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '양평 두물머리 반나절, 경의중앙선 타고 다녀옴'), '양수역에서 두물머리까지 강변길 러닝 코스로도 좋아요!', '2023-03-07T00:18:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.mukja@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '후쿠오카 2박 3일, 동선만 깔끔하게 정리'), '명란 계란말이 추천해 준 아저씨 정체가 궁금하네요ㅎㅎ 포장마차 꼭 가 보고 싶어요.', '2023-10-23T13:28:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.runner@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '후쿠오카 2박 3일, 동선만 깔끔하게 정리'), '오호리 공원 러닝 성지죠. 2km 한 바퀴 딱 좋아요.', '2023-10-23T04:35:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.sea@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '후쿠오카 2박 3일, 동선만 깔끔하게 정리'), '첫 해외는 헤매지 않기가 목표라는 말 완전 공감해요.', '2023-10-22T16:33:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.hometrain@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '기내 수하물 7kg만으로 4박 하기, 짐 싸기 체크리스트'), '체중계로 가방 무게 재는 방법 저도 써요. 제일 정확해요.', '2024-02-19T05:41:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.sea@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '기내 수하물 7kg만으로 4박 하기, 짐 싸기 체크리스트'), '''혹시 몰라서'' 챙기는 옷이 제일 무겁다는 말에 뜨끔했어요...', '2024-02-18T13:46:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.frontnote@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '기내 수하물 7kg만으로 4박 하기, 짐 싸기 체크리스트'), '체크리스트 그대로 메모장에 복사해 갑니다!', '2024-02-19T18:59:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.runner@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '수원 화성 성곽길 반나절 한 바퀴 걸은 날'), '성곽 한 바퀴 러닝도 해 봤는데 서장대 오르막에서 다리 풀렸어요ㅋㅋ', '2024-06-09T08:19:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.film@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '수원 화성 성곽길 반나절 한 바퀴 걸은 날'), '방화수류정 오후 빛 정말 예쁘죠. 사진 찍기 좋은 시간대예요.', '2024-06-10T12:06:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.mukja@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '수원 화성 성곽길 반나절 한 바퀴 걸은 날'), '행궁동 팥빙수 집 어디인지 궁금하네요!', '2024-06-11T03:15:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.sea@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '타이베이 4박 5일, 지하철로만 다닌 기록'), '단수이 노을 정말 좋죠. 바다랑 강이 만나는 곳이라 색이 특이해요.', '2025-01-26T20:53:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.mukja@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '타이베이 4박 5일, 지하철로만 다닌 기록'), '후추빵은 화덕에서 막 꺼낸 거 먹으면 입천장 조심해야 해요ㅎㅎ', '2025-01-27T13:29:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.codecup@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '타이베이 4박 5일, 지하철로만 다닌 기록'), '샹산 야경 저도 1등이었어요. 계단이 생각보다 빡세지만요.', '2025-01-27T20:28:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.hometrain@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '타이베이 4박 5일, 지하철로만 다닌 기록'), '택시 안 타기 규칙 멋지네요. 걸음 수 엄청 나왔겠어요.', '2025-01-29T08:29:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.film@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '인천 개항장 골목 반나절, 차이나타운 옆 조용한 길'), '청일조계지 계단은 필름으로 찍기 좋은 곳이에요. 양쪽 건물 대비가 재밌어요.', '2025-05-12T09:25:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.mukja@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '인천 개항장 골목 반나절, 차이나타운 옆 조용한 길'), '신포시장 공갈빵 오랜만에 생각나네요. 텅 빈 게 맛이라는 말 맞아요.', '2025-05-12T06:13:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.runner@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '북한산 우이령길 예약하고 걸은 날, 초보도 괜찮은 숲길'), '심박 100 유지면 존2 운동이네요. 걷기로 이 정도면 아주 좋아요!', '2025-09-22T06:33:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.hometrain@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '북한산 우이령길 예약하고 걸은 날, 초보도 괜찮은 숲길'), '예약제 숲길이라니 조용해서 좋겠어요. 다음 주말 후보로 저장해요.', '2025-09-22T06:15:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.plant@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '북한산 우이령길 예약하고 걸은 날, 초보도 괜찮은 숲길'), '보존된 숲길은 공기부터 다르죠. 이끼 사진도 찍으셨는지 궁금해요.', '2025-09-22T05:18:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.codecup@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '환전 대신 트래블카드 1년 써 보고 정리한 장단점'), '앱 알림으로 가계부 되는 거 진짜 편하죠. 예비 카드 꼭 따로 챙기는 거 동의합니다.', '2026-03-16T04:40:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.sea@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '환전 대신 트래블카드 1년 써 보고 정리한 장단점'), '야시장은 아직 현금이 필요하군요. 대만 갈 때 참고할게요.', '2026-03-17T07:59:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.mukja@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '환전 대신 트래블카드 1년 써 보고 정리한 장단점'), '남은 동전 처리 걱정 없어진 게 제일 좋다는 말 너무 공감ㅋㅋ', '2026-03-17T15:47:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.sea@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '가평 자라섬 가을 반나절, ITX 타고 다녀온 일기'), '들기름 막국수 반쯤 먹고 육수 붓는 법 저도 해 봐야겠어요.', '2026-10-05T22:00:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.plant@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '가평 자라섬 가을 반나절, ITX 타고 다녀온 일기'), '구절초 한창일 때 가셨네요. 코스모스가 인사하는 것 같다는 표현 좋아요.', '2026-10-05T20:48:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.runner@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '가평 자라섬 가을 반나절, ITX 타고 다녀온 일기'), '가평역에서 자라섬 평지 20분이면 가볍게 뛰어도 되겠어요.', '2026-10-07T04:57:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.mukja@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '맞벌이 부부의 평일 집밥, 일요일 두 시간으로 시작해요'), '배달비 계산하고 말이 없어지는 거 너무 공감돼요ㅋㅋ 대파 키친타월 팁 바로 따라 해볼게요.', '2023-03-07T13:25:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.runner@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '맞벌이 부부의 평일 집밥, 일요일 두 시간으로 시작해요'), '역할 나누는 거 좋네요. 저희 집은 둘 다 부엌에 서 있다가 부딪혀서 싸우거든요.', '2023-03-06T11:07:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.hometrain@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '애호박 두부 된장찌개, 물 올리고 20분이면 끝'), '두부 넉넉히 넣으면 단백질 반찬으로도 좋겠네요. 월급날 차돌 규칙 귀여워요ㅋㅋ', '2023-09-13T01:20:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.bakery@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '애호박 두부 된장찌개, 물 올리고 20분이면 끝'), '된장 체에 푸는 거 몰랐어요! 늘 덩어리가 남았는데 다음엔 그렇게 해볼게요.', '2023-09-14T13:55:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.plant@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉장고 파먹기 1탄: 자투리 채소 볶음밥이랑 계란국'), '계란 풀고 30초 기다리기! 늘 바로 저어서 뿌옇게 됐었는데 이유가 있었네요.', '2024-02-28T09:16:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.codecup@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉장고 파먹기 1탄: 자투리 채소 볶음밥이랑 계란국'), '목요일 냉장고 애매한 거 정말 공감합니다. 자취생인데 이 방식 써먹을게요.', '2024-03-01T10:21:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.sea@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉장고 파먹기 1탄: 자투리 채소 볶음밥이랑 계란국'), '굴소스를 가장자리에 두르는 거 좋은 팁이네요. 불맛 비슷하게 날 것 같아요.', '2024-03-01T04:06:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.halfday@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '여름 밑반찬 세 가지: 오이무침, 가지볶음, 감자채볶음'), '감자전이 된다는 경험담에 웃었어요ㅋㅋ 저도 딱 그랬거든요.', '2024-08-18T18:58:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.woodwork@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '여름 밑반찬 세 가지: 오이무침, 가지볶음, 감자채볶음'), '반찬통 세 개 나란히 있는 거 상상만 해도 든든하네요. 오이무침 레시피 저장합니다.', '2024-08-20T00:22:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.mukja@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉동실 정리하다가 만든 돼지고기 김치 짜글이'), '짜글이에 라면 사리라니 그건 반칙이에요. 오늘 저녁 메뉴 정해졌습니다.', '2025-03-19T03:30:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.film@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉동실 정리하다가 만든 돼지고기 김치 짜글이'), '미스터리 봉지ㅋㅋ 저희 집 냉동실에도 몇 개 있어요. 라벨 붙이기 실천해야겠어요.', '2025-03-20T07:36:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.frontnote@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉동실 정리하다가 만든 돼지고기 김치 짜글이'), '김치 오래 볶는 단계가 핵심이라는 거 메모해 둡니다.', '2025-03-21T15:54:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.sea@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '속초 1박 2일, 중앙시장에서 사 온 것들로 일주일 버티기'), '속초 중앙시장 아이스박스 들고 가는 분 처음 봐요! 다음 속초 여행 때 저도 챙겨 가야겠어요.', '2025-09-09T00:23:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.halfday@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '속초 1박 2일, 중앙시장에서 사 온 것들로 일주일 버티기'), '물회에 배 들어간 거 정말 맛있죠. 영금정 바람 엄청 센 거 인정합니다.', '2025-09-09T05:30:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.hometrain@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '간장 닭다리살 덮밥, 25분 저녁의 정석'), '닭다리살 껍질 바삭 굽기 최고죠. 운동 끝나고 먹기 딱 좋은 메뉴네요.', '2026-04-15T18:32:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.codecup@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '간장 닭다리살 덮밥, 25분 저녁의 정석'), '소스 미리 병에 만들어 두는 거 진짜 좋은 아이디어예요. 이번 주말에 해봅니다.', '2026-04-16T00:29:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.bakery@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '간장 닭다리살 덮밥, 25분 저녁의 정석'), '말없이 밥 더 푸는 남편분 모습이 그려져요ㅋㅋ', '2026-04-16T16:59:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.plant@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '일요일 반찬 데이: 마약계란장이랑 메추리알 장조림'), '6분 30초 메모했어요. 늘 7분 넘겨서 퍽퍽했는데 이거였네요.', '2026-07-19T18:56:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.runner@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '일요일 반찬 데이: 마약계란장이랑 메추리알 장조림'), '아침 러닝 끝나고 계란장 밥 한 그릇이면 최고겠어요.', '2026-07-21T10:48:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.mukja@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '추석 끝나고 남은 전으로 끓인 전찌개, 연휴 뒤 첫 저녁'), '전찌개는 명절 끝의 진짜 별미죠. 맛살은 빼라는 팁 공감합니다.', '2026-09-29T22:29:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.film@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '추석 끝나고 남은 전으로 끓인 전찌개, 연휴 뒤 첫 저녁'), '저희 집도 지금 전 한 통 남아 있어요. 오늘 바로 끓여볼게요!', '2026-10-02T03:21:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.woodwork@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '추석 끝나고 남은 전으로 끓인 전찌개, 연휴 뒤 첫 저녁'), '무 넉넉히 넣으면 시원해진다는 거 좋네요. 어머니가 하시던 맛이 생각나요.', '2026-10-03T03:46:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.sea@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '추석 끝나고 남은 전으로 끓인 전찌개, 연휴 뒤 첫 저녁'), '연휴 뒤 첫 저녁으로 딱이네요. 레시피 감사해요.', '2026-10-02T22:48:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.runner@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '퇴근하고 홈트 시작한 이유, 그리고 첫 주 기록'), '옷부터 갈아입는다는 규칙 진짜 공감해요. 저도 러닝화 먼저 신어 두면 어떻게든 나가게 되더라고요. 응원합니다!', '2023-04-10T07:48:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.codecup@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '퇴근하고 홈트 시작한 이유, 그리고 첫 주 기록'), '건강검진 빨간 글씨 남 일 같지 않네요... 저도 이번 주부터 15분만 따라 해 볼게요.', '2023-04-11T12:16:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.bakery@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '푸시업 0개에서 20개까지, 석 달 걸린 과정'), '하루 총 개수로 목표 잡는 거 좋네요. 한 번에 몇 개만 생각하면 늘 지치던데 표 보고 따라 해 볼게요.', '2023-08-21T04:20:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.film@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '푸시업 0개에서 20개까지, 석 달 걸린 과정'), '첫 1개 성공했을 때 소리 지르신 거 상상돼요ㅋㅋ 축하드려요!', '2023-08-22T13:55:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.runner@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '무릎 안 아픈 스쿼트, 거울 앞에서 확인하는 체크리스트'), '러너들도 무릎 안으로 모이는 거 때문에 많이 고생해요. 고블릿 스쿼트 보강운동으로 넣어 봐야겠어요.', '2024-01-16T04:28:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.woodwork@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '무릎 안 아픈 스쿼트, 거울 앞에서 확인하는 체크리스트'), '작업할 때 쪼그려 앉는 일이 많아서 무릎이 늘 걱정이었는데 엉덩이 먼저 빼는 거 의식해 볼게요.', '2024-01-16T23:19:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.halfday@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '무릎 안 아픈 스쿼트, 거울 앞에서 확인하는 체크리스트'), '영상 찍어 보라는 말에 찔렸어요ㅋㅋ 오늘 해 봅니다', '2024-01-17T04:05:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.homecook@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '닭가슴살 질릴 때 돌려 먹는 단백질 반찬 5가지'), '달걀 장조림에 청양고추 넣는 거 좋아요! 저는 꽈리고추도 같이 넣는데 그것도 추천드려요.', '2024-06-03T19:26:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.mukja@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '닭가슴살 질릴 때 돌려 먹는 단백질 반찬 5가지'), '앞다리살 수육 기름 적은 거 몰랐네요. 이번 주말에 해 봐야겠다', '2024-06-04T08:16:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.plant@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '닭가슴살 질릴 때 돌려 먹는 단백질 반찬 5가지'), '일요일에 반찬 만들어 두는 습관 저도 들이고 싶어요. 두부 조림부터 도전!', '2024-06-04T06:40:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.halfday@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '20분 맨몸 서킷 루틴 (층간소음 걱정 없는 버전)'), '층간소음 버전 너무 반가워요! 버피 때문에 홈트 포기했었는데 이걸로 다시 시작해 볼게요.', '2025-03-17T04:48:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.frontnote@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '20분 맨몸 서킷 루틴 (층간소음 걱정 없는 버전)'), '퇴근하고 바로 따라 해 봤는데 숄더탭에서 골반이 엄청 흔들리네요ㅋㅋ 3바퀴 목표로 해 보겠습니다.', '2025-03-19T06:13:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.runner@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '20분 맨몸 서킷 루틴 (층간소음 걱정 없는 버전)'), '러닝 안 하는 날 보강으로 딱이네요. 저장해 둡니다.', '2025-03-17T08:37:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.codecup@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '헬스장 3개월 등록 후기: 홈트랑 뭐가 달랐나'), '오가는 시간 합치면 1시간 40분... 이게 제가 헬스장 3번 끊고 3번 다 날린 이유입니다ㅠ', '2025-09-08T10:24:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.runner@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '헬스장 3개월 등록 후기: 홈트랑 뭐가 달랐나'), '당기는 운동 부족하다는 거 정말 맞는 말이에요. 저도 러닝만 하다가 어깨가 말려서 고생했어요.', '2025-09-08T18:03:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.woodwork@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '문틀 턱걸이 바 달고 첫 턱걸이 성공하기까지'), '문틀 몰딩 자국 얘기 현실적이네요. 걸이 부분에 펠트 한 겹 덧대도 괜찮아요.', '2026-04-20T11:29:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.plant@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '문틀 턱걸이 바 달고 첫 턱걸이 성공하기까지'), '7개월 꾸준히 한 게 대단해요. 저는 데드 행 15초부터 시작합니다...', '2026-04-21T00:33:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.frontnote@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '문틀 턱걸이 바 달고 첫 턱걸이 성공하기까지'), '네거티브 진짜 최고인 듯해요. 축하드려요!', '2026-04-23T12:40:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.sea@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '여름 휴가 중 호텔방에서 한 15분 루틴'), '남쪽 바닷가 산책로 해 질 녘에 걷는 거 최고죠. 다음 여행 때 이 루틴 챙겨 갈게요.', '2026-08-24T03:00:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.halfday@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '여름 휴가 중 호텔방에서 한 15분 루틴'), '바퀴 의자 딥스 얘기 웃프네요ㅋㅋ 저도 예전에 미끄러질 뻔했어요.', '2026-08-24T17:46:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.runner@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '홈트 3년 차 회고: 그만두지 않을 수 있었던 이유'), '안 한 날도 적는다는 거 정말 좋은 방법이네요. 저도 러닝 기록에 쉰 이유까지 적어 봐야겠어요. 3년 반 축하드려요!', '2026-10-06T04:08:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.codecup@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '홈트 3년 차 회고: 그만두지 않을 수 있었던 이유'), '목요일이 약한 요일인 거 저랑 똑같네요ㅋㅋ 회고 잘 읽었습니다.', '2026-10-07T07:35:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.bakery@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '홈트 3년 차 회고: 그만두지 않을 수 있었던 이유'), '처음 글부터 쭉 봤는데 턱걸이까지 오신 거 보니 괜히 뭉클하네요.', '2026-10-06T21:56:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.homecook@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '홈트 3년 차 회고: 그만두지 않을 수 있었던 이유'), '일요일 반찬 만들기 계속하신다니 반가워요. 다음엔 레시피도 더 올려 주세요!', '2026-10-10T14:22:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.homecook@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '우리 동네 ''소문난손칼국수'', 겉절이가 다 한 집 솔직 후기'), '겉절이 하나로 단골 되는 거 너무 이해돼요. 마늘 생강 센 스타일 저도 좋아해요.', '2023-06-18T23:13:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.halfday@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '우리 동네 ''소문난손칼국수'', 겉절이가 다 한 집 솔직 후기'), '점수표 좋네요! 앞으로 글 기대할게요.', '2023-06-19T11:49:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.film@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '을지로 40년 된 연탄 돼지갈비 노포 ''원조연탄갈비'' 솔직 후기'), '을지로 노포 골목 분위기는 사진으로 담기 정말 좋죠. 드럼통 테이블 최고.', '2023-11-27T02:24:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.codecup@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '을지로 40년 된 연탄 돼지갈비 노포 ''원조연탄갈비'' 솔직 후기'), '회사 근처라 자주 지나가는데 다음에 꼭 가 볼게요. 된장찌개 메모!', '2023-11-27T04:19:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.sea@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '을지로 40년 된 연탄 돼지갈비 노포 ''원조연탄갈비'' 솔직 후기'), '가격표 위에 종이 덧붙인 흔적이라니, 그게 진짜 노포의 증거네요.', '2023-11-26T06:00:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.bakery@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '망원동 작은 카페 ''오후세시'' 디저트 3종 비교 후기'), '까눌레 겉이 딱 소리 날 정도면 굽기 제대로 하신 거예요. 저도 먹어 보고 싶네요.', '2024-03-11T03:01:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.halfday@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '망원동 작은 카페 ''오후세시'' 디저트 3종 비교 후기'), '망원시장 갔다가 들르기 딱 좋겠어요. 저장!', '2024-03-10T11:27:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.frontnote@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '망원동 작은 카페 ''오후세시'' 디저트 3종 비교 후기'), '표로 한 줄 평 정리한 거 너무 보기 편해요.', '2024-03-10T19:29:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.halfday@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '대전 중앙시장 순댓국 노포, 줄 서서 먹을 만한가'), '대전역에서 걸어서 10분이면 기차 시간 남을 때 딱이네요.', '2024-09-21T19:30:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.homecook@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '대전 중앙시장 순댓국 노포, 줄 서서 먹을 만한가'), '맑은 국물에 들깨랑 새우젓 넣는 조합 좋아요. 집에서도 응용해 볼게요.', '2024-09-22T04:43:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.sea@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '대전 중앙시장 순댓국 노포, 줄 서서 먹을 만한가'), '옆자리 할아버지들 풍경까지 맛이라는 말 공감해요.', '2024-09-23T18:18:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.homecook@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '동네 중국집 짜장면 4곳 비교 정리 (배달 말고 매장)'), '면 건지는 타이밍 싸움이라는 말 정답이에요. 집에서 하면 꼭 퍼지더라고요.', '2025-02-17T04:36:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.codecup@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '동네 중국집 짜장면 4곳 비교 정리 (배달 말고 매장)'), '4곳 비교표 너무 좋네요. 짬뽕 시리즈 기대합니다!', '2025-02-17T02:26:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.runner@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '동네 중국집 짜장면 4곳 비교 정리 (배달 말고 매장)'), '주말마다 짜장면이라니 부럽습니다... 저는 달리고 나서 먹어야겠어요ㅋㅋ', '2025-02-17T18:33:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.bakery@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '성수 웨이팅 카페 두 곳, 솔직히 한 곳만 추천해요'), '소금빵이 식어서 나오면 진짜 아쉽죠. 버터 향이 반은 날아가요.', '2025-06-29T04:34:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.frontnote@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '성수 웨이팅 카페 두 곳, 솔직히 한 곳만 추천해요'), '공간 사진 찍으러 가는 곳이라는 말 정확하네요ㅋㅋ B 저장합니다.', '2025-06-29T17:13:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.film@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '성수 웨이팅 카페 두 곳, 솔직히 한 곳만 추천해요'), '작은 로스터리 바 테이블 분위기 좋아요. 사진 찍기도 좋을 것 같아요.', '2025-06-30T05:41:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.sea@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '부산 국밥 노포 수육백반, 겨울이면 생각나는 그 집'), '범일동 쪽이면 다음 부산 여행 동선에 넣을 수 있겠어요. 수육백반 꼭 먹어 볼게요.', '2025-12-07T00:32:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.homecook@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '부산 국밥 노포 수육백반, 겨울이면 생각나는 그 집'), '고기로만 낸 맑은 육수 설명이 확 와닿네요. 부추 넣는 팁도 메모.', '2025-12-07T13:06:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.hometrain@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '부산 국밥 노포 수육백반, 겨울이면 생각나는 그 집'), '단백질 보충은 수육백반으로...ㅎㅎ 추운 날 생각나는 글이에요.', '2025-12-09T12:02:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.halfday@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '부산 국밥 노포 수육백반, 겨울이면 생각나는 그 집'), '국밥 한 그릇 먹으러 부산 가는 거 전혀 사치 아니에요!', '2025-12-08T03:39:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.halfday@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '전주 1박 하면서 먹은 것만 정리 (한옥마을 밖 위주)'), '가맥 황태구이 진짜 맛있죠! 저도 전주 가면 무조건 가요.', '2026-04-19T09:37:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.sea@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '전주 1박 하면서 먹은 것만 정리 (한옥마을 밖 위주)'), '관광지 반 블록 밖 법칙 저도 바닷가 갈 때 늘 써요ㅎㅎ', '2026-04-20T20:34:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.homecook@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '전주 1박 하면서 먹은 것만 정리 (한옥마을 밖 위주)'), '남부시장식 콩나물국밥 수란 먹는 법 처음 알았어요.', '2026-04-21T13:07:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.codecup@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '회사 근처 백반집 한 달 출석 기록, 매일 바뀌는 반찬 정리'), '11시 50분 도착이 정답이라는 거 직장인이면 다 공감해요ㅋㅋ', '2026-09-20T14:34:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.homecook@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '회사 근처 백반집 한 달 출석 기록, 매일 바뀌는 반찬 정리'), '깻잎지 달지 않고 짭짤한 스타일 비법 궁금해요. 사장님께 여쭤봐 주세요!', '2026-09-21T09:20:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.hometrain@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '회사 근처 백반집 한 달 출석 기록, 매일 바뀌는 반찬 정리'), '백반 한 달에 -0.8kg이면 균형 잡힌 식사 효과네요.', '2026-09-21T18:48:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.plant@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '회사 근처 백반집 한 달 출석 기록, 매일 바뀌는 반찬 정리'), '사장님이 밥 더 퍼 주시는 장면이 너무 따뜻해요.', '2026-09-23T18:37:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.woodwork@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '몬스테라 첫 분갈이, 뿌리가 화분 밖으로 나왔어요'), '뿌리 풀어 주는 과정 사진 없어도 상상되네요. 다음엔 나무 화분대 하나 만들어 드리고 싶어요.', '2023-05-21T05:13:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.homecook@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '몬스테라 첫 분갈이, 뿌리가 화분 밖으로 나왔어요'), '나무젓가락 콕콕이 포인트군요. 저도 다음 주에 해 봐야겠어요.', '2023-05-21T23:19:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.film@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '초보 식물집사가 첫 1년 동안 보낸 식물들'), '실패담이 제일 도움 돼요. 저도 첫 해에 다육이 두 개 보냈어요...', '2023-09-03T22:48:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.codecup@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '초보 식물집사가 첫 1년 동안 보낸 식물들'), '날짜가 아니라 상태로 판단하기, 개발에서도 똑같은 교훈이네요 ㅎㅎ', '2023-09-04T22:23:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.hometrain@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '초보 식물집사가 첫 1년 동안 보낸 식물들'), '격리 2주라니 식물계도 철저하네요.', '2023-09-05T02:31:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.bakery@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '내가 정착한 흙 배합 비율 (상토 : 펄라이트 : 바크)'), '베이킹 배합표 보는 느낌이에요 ㅎㅎ 비율 정리 깔끔해요.', '2024-04-14T09:50:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.woodwork@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '내가 정착한 흙 배합 비율 (상토 : 펄라이트 : 바크)'), '물 뿌려 가며 섞기 팁 좋네요. 톱밥 다룰 때랑 비슷해요.', '2024-04-14T09:40:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.runner@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '여름 베란다 과습 막는 물주기 루틴'), '화분 무게로 판단하는 거 신기해요. 매일 들어 보면 감이 오나 봐요.', '2024-07-28T01:31:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.hometrain@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '여름 베란다 과습 막는 물주기 루틴'), '아침 루틴에 서큘레이터까지, 저보다 부지런하세요 ㅋㅋ', '2024-07-29T11:44:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.film@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '여름 베란다 과습 막는 물주기 루틴'), '저는 필름 때문에 습기랑 싸우는데 식물도 똑같네요.', '2024-07-31T09:47:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.sea@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '겨울 베란다 냉해 입은 알로카시아 살리기 기록'), '새순 나온 순간 진짜 감동이었겠어요. 포기 안 하신 거 대단해요.', '2025-01-19T23:19:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.woodwork@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '겨울 베란다 냉해 입은 알로카시아 살리기 기록'), '스티로폼 판 대신 원목 받침에 펠트 붙여도 냉기 막는 데 좋아요.', '2025-01-20T20:15:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.frontnote@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '스킨답서스 물꽂이 번식 3주 관찰 일기'), '일자별 기록 너무 좋아요. 저도 회사 책상에 하나 키워 보고 싶네요.', '2025-06-08T05:39:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.halfday@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '스킨답서스 물꽂이 번식 3주 관찰 일기'), '회사 동료분들 부럽다 ㅎㅎ 식물 나눔 따뜻해요.', '2025-06-09T05:00:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.bakery@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '스킨답서스 물꽂이 번식 3주 관찰 일기'), '21일 관찰이라니, 발효종 키우는 거랑 비슷한 설렘이 있네요.', '2025-06-08T06:49:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.homecook@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '응애 퇴치 일지, 칼라데아와 2주 전쟁'), '흰 종이 대고 털어 보는 방법 처음 알았어요. 우리 집 화분도 확인해 봐야겠어요.', '2026-02-15T04:26:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.codecup@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '응애 퇴치 일지, 칼라데아와 2주 전쟁'), '5일 간격 3회… 버그 재발 방지 테스트 같네요 ㅋㅋ', '2026-02-15T18:11:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.halfday@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '주말 화훼시장 나들이, 올리브나무를 데려왔어요'), '화훼시장 아침 나들이 코스 좋네요! 저도 반나절 코스로 가 봐야겠어요.', '2026-05-31T18:07:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.film@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '주말 화훼시장 나들이, 올리브나무를 데려왔어요'), '화훼시장 비닐하우스 안 빛이 사진 찍기 좋던데, 다음엔 카메라 들고 가 보세요.', '2026-06-01T11:20:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.mukja@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '주말 화훼시장 나들이, 올리브나무를 데려왔어요'), '화훼시장 다음엔 국수, 완벽한 동선이에요.', '2026-06-01T12:40:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.woodwork@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '가을맞이 베란다 대정리와 분갈이 몰아치기'), '몬스테라 3년 성장기 감동이에요. 화분 자르는 장면 상상돼요 ㅋㅋ', '2026-09-27T05:48:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.film@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '가을맞이 베란다 대정리와 분갈이 몰아치기'), '체크리스트 저도 따라 해야겠어요. 필름 냉장고 옆에 화분 자리를...', '2026-09-28T19:01:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.runner@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '가을맞이 베란다 대정리와 분갈이 몰아치기'), '3년 꾸준함이 제일 대단한 것 같아요.', '2026-09-28T07:50:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.hometrain@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '러닝 3주차, 처음으로 5km를 안 쉬고 뛰었다'), '인터벌로 차근차근 올리신 거 정석이에요! 대화 가능한 속도 진짜 중요해요.', '2023-03-12T17:10:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.plant@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '러닝 3주차, 처음으로 5km를 안 쉬고 뛰었다'), '3주 만에 5km라니 멋져요. 저도 자극받고 갑니다.', '2023-03-13T03:37:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.hometrain@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '첫 10km 대회 후기, 58분 41초'), '첫 대회 서브 60 축하드려요! 컵 접어서 마시는 팁 저도 실수해서 배웠어요 ㅋㅋ', '2023-10-31T05:20:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.mukja@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '첫 10km 대회 후기, 58분 41초'), '대회 전날 식단 현실적이라 좋네요. 계란말이 최고.', '2023-10-31T10:26:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.frontnote@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '첫 10km 대회 후기, 58분 41초'), '구간 기록 표로 정리하신 거 보기 좋아요. 다음 하프도 응원해요!', '2023-11-02T07:36:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.hometrain@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '러닝화 세 켤레 돌려 신기, 400km 신어 본 정리'), '신발 돌려 신기 진짜 추천해요. 회복주용 쿠션화 하나 있으면 확실히 다르죠.', '2024-04-07T08:09:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.woodwork@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '러닝화 세 켤레 돌려 신기, 400km 신어 본 정리'), '닳은 모양으로 습관을 본다는 게 재밌네요. 대패 날도 비슷해요.', '2024-04-07T18:49:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.hometrain@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '정강이 통증(신스플린트)으로 3주를 쉬었다'), '카프레이즈 꾸준히 하시면 재발 확실히 줄어요. 쾌차하세요!', '2024-09-15T04:20:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.plant@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '정강이 통증(신스플린트)으로 3주를 쉬었다'), '10% 규칙 처음 알았어요. 무리하지 마세요 ㅠㅠ', '2024-09-16T15:46:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.codecup@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '정강이 통증(신스플린트)으로 3주를 쉬었다'), '한 주 70% 증가... 저도 야근 몰아치다 번아웃 왔던 거랑 비슷하네요.', '2024-09-17T12:09:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.hometrain@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '새벽 5시 반 기상 루틴과 존2 러닝 두 달'), '존2 처음엔 진짜 답답하죠 ㅋㅋ 안정 심박 6이나 떨어진 거 대단해요.', '2025-03-23T07:20:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.codecup@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '새벽 5시 반 기상 루틴과 존2 러닝 두 달'), '전날 밤 습관이 핵심이라는 말 공감해요. 저도 새벽 코딩하려다 실패한 이유가 그거였어요.', '2025-03-23T14:20:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.film@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '새벽 5시 반 기상 루틴과 존2 러닝 두 달'), '새벽 하천 길 풍경 좋을 것 같아요. 언젠가 필름으로 담아 보고 싶네요.', '2025-03-26T02:08:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.hometrain@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '첫 하프마라톤 후기, 2시간 4분 37초'), '첫 하프 완주 축하드려요! 18km 이후가 진짜 레이스라는 말 너무 공감이요.', '2025-11-10T17:00:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.halfday@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '첫 하프마라톤 후기, 2시간 4분 37초'), '결승선 아치 보일 때까지 숫자만 세는 거... 상상만 해도 다리가 아파요. 고생하셨어요!', '2025-11-12T11:45:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.mukja@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '첫 하프마라톤 후기, 2시간 4분 37초'), '대회 끝나고 뭐 드셨는지도 궁금해요 ㅎㅎ', '2025-11-11T01:28:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.sea@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '첫 하프마라톤 후기, 2시간 4분 37초'), '끝까지 안 걸으셨다는 게 제일 멋져요.', '2025-11-14T11:27:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.hometrain@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '하프 서브2를 위한 12주 훈련표 짜기'), '수요일 근력 넣으신 거 좋아요. 런지는 한 발씩 천천히 하시면 무릎 안정에도 좋아요.', '2026-04-19T16:59:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.codecup@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '하프 서브2를 위한 12주 훈련표 짜기'), '스프린트 계획표 같아서 보기 좋네요. 테이퍼링은 릴리스 전 코드 프리즈 느낌 ㅋㅋ', '2026-04-19T18:29:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.hometrain@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '여름 새벽 러닝 장비 정리, 모자부터 전해질까지'), '체중 재서 수분 손실 확인하는 거 정말 좋은 습관이에요.', '2026-07-27T00:30:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.sea@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '여름 새벽 러닝 장비 정리, 모자부터 전해질까지'), '5km 루프 코스 아이디어 좋네요. 여름엔 무리하지 마세요!', '2026-07-26T09:04:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.bakery@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '여름 새벽 러닝 장비 정리, 모자부터 전해질까지'), '면 티 갑옷 표현에 웃었어요 ㅋㅋ', '2026-07-27T22:09:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.hometrain@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '대회 2주 앞두고 마지막 장거리 20km'), '부상 없이 12주 완주가 진짜 큰 성과예요. 대회 날 좋은 기록 응원합니다!', '2026-10-03T17:54:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.plant@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '대회 2주 앞두고 마지막 장거리 20km'), '66/72 꾸준함 대단해요. 서브2 꼭 하실 거예요!', '2026-10-04T02:07:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.frontnote@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '대회 2주 앞두고 마지막 장거리 20km'), '작년 하프 후기부터 쭉 봤는데 숫자가 확 달라졌네요. 화이팅!', '2026-10-05T23:31:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.film@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '대회 2주 앞두고 마지막 장거리 20km'), '대회 날 결승선 사진 찍어 드리고 싶네요 ㅎㅎ 응원해요.', '2026-10-06T21:08:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.halfday@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '주문진 새벽 수산시장 구경하고 소돌 아들바위까지 걸은 기록'), '새벽 경매 구경 저도 버킷리스트예요. 4시 50분 알람이라니 진심이시네요ㅎㅎ', '2023-05-22T06:02:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.mukja@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '주문진 새벽 수산시장 구경하고 소돌 아들바위까지 걸은 기록'), '곰치국 맑은 국물 스타일 좋아해요. 김치 넣고 끓이는 집도 있는데 둘 다 맛있어요.', '2023-05-22T12:59:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.halfday@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '통영 1박 2일, 동피랑보다 좋았던 서피랑 골목'), '동피랑만 가 봤는데 서피랑은 몰랐어요! 다음에 꼭 넘어가 볼게요.', '2023-09-18T08:24:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.film@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '통영 1박 2일, 동피랑보다 좋았던 서피랑 골목'), '배들이 하나둘 불을 켜는 장면, 필름으로 찍으면 진짜 예쁠 것 같아요.', '2023-09-17T13:47:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.runner@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '통영 1박 2일, 동피랑보다 좋았던 서피랑 골목'), '99계단은 계단 훈련 코스로도 좋겠네요ㅎㅎ', '2023-09-19T07:14:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.mukja@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '욕지도 첫 배 타고 들어가서 섬 한 바퀴 돈 날'), '고등어 회 비린 맛 없는 건 정말 산지라서 가능한 거죠. 부럽습니다.', '2024-04-28T21:37:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.runner@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '욕지도 첫 배 타고 들어가서 섬 한 바퀴 돈 날'), '30km 일주면 러닝으로도 한번 도전해 보고 싶네요. 오르막이 걱정이지만요.', '2024-04-29T18:57:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.hometrain@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '삼척 장호항 스노클링 후기와 챙겨 가면 좋은 것'), '냉수대 얘기는 처음 알았어요. 수온 확인하는 습관 들여야겠네요.', '2024-08-11T23:13:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.halfday@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '삼척 장호항 스노클링 후기와 챙겨 가면 좋은 것'), '아쿠아슈즈 진짜 필수죠. 저는 작년에 맨발로 들어갔다가 발바닥 다 긁혔어요ㅠ', '2024-08-12T13:45:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.plant@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '삼척 장호항 스노클링 후기와 챙겨 가면 좋은 것'), '물 맑은 바다 사진만 봐도 시원해지네요.', '2024-08-12T17:41:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.halfday@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '군산 근대거리 걷고 선유도까지, 서해 당일치기 코스 정리'), '대장봉 계단 진짜 가파르죠! 저는 중간에 세 번 쉬었어요.', '2025-03-23T10:32:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.mukja@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '군산 근대거리 걷고 선유도까지, 서해 당일치기 코스 정리'), '군산은 짬뽕 투어만 해도 하루가 모자라요. 항구반점 메모해 둘게요.', '2025-03-24T23:44:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.film@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '군산 근대거리 걷고 선유도까지, 서해 당일치기 코스 정리'), '섬이 겹겹이 놓인 수묵화 같은 풍경이라는 표현이 좋네요.', '2025-03-25T08:49:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.mukja@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '속초 갈 때마다 들르는 동명항 근처 먹을 곳 기록'), '오징어순대는 계란물 입혀 부친 게 진리죠. 갯배 줄 당기는 것도 은근 재밌고요.', '2025-07-06T15:30:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.homecook@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '속초 갈 때마다 들르는 동명항 근처 먹을 곳 기록'), '물회 육수 달지 않은 집 귀한데요. 집에서 따라 만들어 보고 싶어요.', '2025-07-07T15:43:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.bakery@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '속초 갈 때마다 들르는 동명항 근처 먹을 곳 기록'), '속초 갈 일 있으면 이 글 다시 보러 올게요!', '2025-07-08T08:51:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.plant@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '겨울 바다 혼자 보러 간 남해 다랭이마을 일기'), '창문이 그대로 바다인 방이라니... 읽기만 해도 마음이 편해지네요.', '2025-11-16T13:25:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.woodwork@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '겨울 바다 혼자 보러 간 남해 다랭이마을 일기'), '사진 열두 장이 증거라는 말 공감해요. 좋은 시간은 기록할 틈이 없더라고요.', '2025-11-17T04:46:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.halfday@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '겨울 바다 혼자 보러 간 남해 다랭이마을 일기'), '남해 군내버스 시간표 진짜 중요하죠. 저도 한 번 놓쳐서 한 시간 기다렸어요.', '2025-11-18T22:52:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.runner@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '가파도 청보리 끝물에 다녀온 기록과 배 예약 팁'), '둘레 4km 평지면 아침 러닝 코스로 딱이네요.', '2026-05-31T08:28:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.halfday@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '가파도 청보리 끝물에 다녀온 기록과 배 예약 팁'), '가파도 배 예약 방식 처음 알았어요. 머무는 시간이 정해져 있군요!', '2026-06-01T19:49:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.film@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '가파도 청보리 끝물에 다녀온 기록과 배 예약 팁'), '바다 옆에 또 바다가 하나 있다는 표현 너무 좋아요. 보리밭 사진 보고 싶네요.', '2026-05-31T06:22:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.halfday@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '추석 연휴 끝에 다녀온 고성 화진포, 조용한 가을 바다'), '연휴 마지막 날 하행선 꿀팁 저장합니다. 상행선 5시간은 슬프지만요ㅎㅎ', '2026-09-28T09:06:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.mukja@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '추석 연휴 끝에 다녀온 고성 화진포, 조용한 가을 바다'), '도치알탕 냉동으로라도 끓여 주시는 집이면 단골 될 만하네요.', '2026-09-29T01:21:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.plant@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '추석 연휴 끝에 다녀온 고성 화진포, 조용한 가을 바다'), '가을 바다는 바다 자신의 것이라는 문장 오래 기억할 것 같아요.', '2026-09-28T04:37:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.hometrain@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '추석 연휴 끝에 다녀온 고성 화진포, 조용한 가을 바다'), '호수 둘레길 16km 다음엔 끝까지 걸어 보세요!', '2026-09-30T13:14:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.homecook@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '공방 첫날, 편백 도마 하나 만들고 손바닥에 물집 두 개'), '편백 도마라니 부러워요. 요리하는 입장에서 손잡이 구멍 있는 도마가 정말 편하거든요.', '2023-04-16T23:24:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.bakery@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '공방 첫날, 편백 도마 하나 만들고 손바닥에 물집 두 개'), '물집이 기분 좋은 그 느낌 알아요. 첫 식빵 치댈 때 저도 그랬어요.', '2023-04-17T18:26:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.film@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '월넛 트레이 만들기, 오일 마감 세 번의 차이'), '자투리로 마감 비교한 거 너무 좋아요. 사진 없이 글로만 봐도 색이 상상돼요.', '2023-08-27T04:13:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.codecup@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '월넛 트레이 만들기, 오일 마감 세 번의 차이'), '오일 천 자연발화는 처음 알았어요. 안전 팁 감사합니다.', '2023-08-28T11:47:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.plant@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '월넛 트레이 만들기, 오일 마감 세 번의 차이'), '월넛 트레이에 화분 올려 두면 예쁠 것 같아요.', '2023-08-28T18:27:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.homecook@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '끌 연마 입문, 숫돌 세 개로 거울면 만들기'), '부엌칼도 이 방법으로 갈 수 있을까요? 날을 가는 시간 이야기 와닿네요.', '2024-01-22T03:51:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.frontnote@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '끌 연마 입문, 숫돌 세 개로 거울면 만들기'), '뒷면부터 평평하게 잡는 게 제일 중요하다는 게 기초 다지기 같아요.', '2024-01-22T21:47:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.hometrain@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '첫 가구, 장부맞춤 원목 스툴 만든 3주의 기록'), '못 없이 짜서 흔들림이 없다니 신기해요. 운동할 때 앉는 스툴로도 튼튼하겠어요.', '2024-06-17T09:05:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.bakery@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '첫 가구, 장부맞춤 원목 스툴 만든 3주의 기록'), '다들 그렇게 배워요라는 말에 위로받고 갑니다. 저도 실패 기록 많이 쓰는데 공감돼요.', '2024-06-18T08:49:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.mukja@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '첫 가구, 장부맞춤 원목 스툴 만든 3주의 기록'), '3주 동안 만든 첫 가구라니 정이 많이 가겠어요.', '2024-06-18T05:53:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.codecup@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '트리머 1년 사용기, 처음 살 때 알았으면 좋았을 것'), '콜렛 지름 몰라서 비트 다시 산 이야기 공감돼요. 호환성은 어디서나 함정이네요.', '2025-01-12T19:26:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.runner@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '트리머 1년 사용기, 처음 살 때 알았으면 좋았을 것'), '공구는 실수도 빨리 만들어 준다는 말 명언이에요.', '2025-01-12T23:43:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.frontnote@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '서랍 하나 달린 협탁, 서랍 레일 없이 나무로만'), '나무가 움직일 자리를 남겨 두는 설계라니, 여유를 두는 게 결국 오래 가는 비결이네요.', '2025-05-25T10:57:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.sea@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '서랍 하나 달린 협탁, 서랍 레일 없이 나무로만'), '양초 문지르기 팁은 집 서랍장에도 써먹어야겠어요.', '2025-05-26T23:38:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.halfday@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '서랍 하나 달린 협탁, 서랍 레일 없이 나무로만'), '슉 소리 듣고 싶어서 여닫는 거 너무 귀여워요ㅋㅋ', '2025-05-26T03:29:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.sea@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '양평 목재소 다녀온 날, 판재 고르는 법 배운 기록'), '두물머리 늦가을 풍경 정말 좋죠. 목재소 나들이라니 새로운 여행 코스네요.', '2025-11-09T14:06:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.plant@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '양평 목재소 다녀온 날, 판재 고르는 법 배운 기록'), '나이테 방향으로 휨을 판단하는 거 신기해요. 나무도 살아 있는 재료구나 싶어요.', '2025-11-10T08:58:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.mukja@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '양평 목재소 다녀온 날, 판재 고르는 법 배운 기록'), '잔치국수에 메밀전 조합 최고예요. 양평 가면 꼭 들러 볼게요.', '2025-11-10T05:19:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.film@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '체리 판재로 만든 4단 책장, 반년 걸린 이유'), '몇 달에 걸쳐 색이 변한다니, 시간이 같이 완성하는 가구네요.', '2026-03-22T22:26:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.codecup@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '체리 판재로 만든 4단 책장, 반년 걸린 이유'), '선반 처짐을 미리 테스트하신 거 진짜 꼼꼼하세요. 저도 책 무게 때문에 휜 선반 있어요.', '2026-03-22T10:51:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.homecook@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '체리 판재로 만든 4단 책장, 반년 걸린 이유'), '반년 들인 책장이라니 대대로 물려줄 만하네요.', '2026-03-23T19:54:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.sea@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '캠핑용 접이식 테이블, 주말 4번에 끝낸 제작기'), '어디서 샀냐는 질문이 최고의 칭찬이죠! 가평 캠핑 날씨 좋았나요?', '2026-09-28T05:12:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.runner@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '캠핑용 접이식 테이블, 주말 4번에 끝낸 제작기'), '2.7kg면 백패킹에도 들고 갈 만하겠어요. 설계도 공유해 주시면 좋겠어요.', '2026-09-28T19:18:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.bakery@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '캠핑용 접이식 테이블, 주말 4번에 끝낸 제작기'), '도마에서 캠핑 테이블까지, 3년 반의 성장이 보여서 괜히 뭉클해요.', '2026-09-28T12:01:00+09:00');
insert into comment (users_id, post_id, body, created_at) values ((select users_id from users where email = 'demo.hometrain@example.com'), (select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '캠핑용 접이식 테이블, 주말 4번에 끝낸 제작기'), '팔꿈치에 주저앉은 이야기 웃겼어요ㅋㅋ 12mm로 해결하셨다니 다행이에요.', '2026-09-27T14:27:00+09:00');

-- 좋아요
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '23리터 미니오븐으로 구운 첫 식빵, 손반죽 40분의 기록'), (select users_id from users where email = 'demo.homecook@example.com'), '2023-05-22T19:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '23리터 미니오븐으로 구운 첫 식빵, 손반죽 40분의 기록'), (select users_id from users where email = 'demo.hometrain@example.com'), '2023-05-24T09:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '23리터 미니오븐으로 구운 첫 식빵, 손반죽 40분의 기록'), (select users_id from users where email = 'demo.woodwork@example.com'), '2023-05-22T06:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '버터쿠키가 팬케이크처럼 퍼졌다, 원인 세 가지'), (select users_id from users where email = 'demo.codecup@example.com'), '2023-11-06T06:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '버터쿠키가 팬케이크처럼 퍼졌다, 원인 세 가지'), (select users_id from users where email = 'demo.film@example.com'), '2023-11-08T00:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '버터쿠키가 팬케이크처럼 퍼졌다, 원인 세 가지'), (select users_id from users where email = 'demo.homecook@example.com'), '2023-11-07T12:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '버터쿠키가 팬케이크처럼 퍼졌다, 원인 세 가지'), (select users_id from users where email = 'demo.runner@example.com'), '2023-11-05T23:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '드디어 정착한 초코칩 쿠키, 겉바속쫀 비율'), (select users_id from users where email = 'demo.frontnote@example.com'), '2024-04-13T22:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '드디어 정착한 초코칩 쿠키, 겉바속쫀 비율'), (select users_id from users where email = 'demo.halfday@example.com'), '2024-04-17T15:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '드디어 정착한 초코칩 쿠키, 겉바속쫀 비율'), (select users_id from users where email = 'demo.mukja@example.com'), '2024-04-14T18:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '드디어 정착한 초코칩 쿠키, 겉바속쫀 비율'), (select users_id from users where email = 'demo.plant@example.com'), '2024-04-15T00:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '탕종법 우유식빵, 사흘이 지나도 촉촉했다'), (select users_id from users where email = 'demo.frontnote@example.com'), '2024-10-28T10:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '탕종법 우유식빵, 사흘이 지나도 촉촉했다'), (select users_id from users where email = 'demo.halfday@example.com'), '2024-10-30T03:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '탕종법 우유식빵, 사흘이 지나도 촉촉했다'), (select users_id from users where email = 'demo.homecook@example.com'), '2024-10-27T06:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '탕종법 우유식빵, 사흘이 지나도 촉촉했다'), (select users_id from users where email = 'demo.hometrain@example.com'), '2024-10-28T20:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '탕종법 우유식빵, 사흘이 지나도 촉촉했다'), (select users_id from users where email = 'demo.mukja@example.com'), '2024-10-29T00:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '탕종법 우유식빵, 사흘이 지나도 촉촉했다'), (select users_id from users where email = 'demo.runner@example.com'), '2024-10-29T23:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '탕종법 우유식빵, 사흘이 지나도 촉촉했다'), (select users_id from users where email = 'demo.woodwork@example.com'), '2024-10-27T14:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '발렌타인에 구운 휘낭시에와 마들렌, 같은 듯 다른 둘'), (select users_id from users where email = 'demo.film@example.com'), '2025-02-15T13:50:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '발렌타인에 구운 휘낭시에와 마들렌, 같은 듯 다른 둘'), (select users_id from users where email = 'demo.frontnote@example.com'), '2025-02-18T10:50:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '발렌타인에 구운 휘낭시에와 마들렌, 같은 듯 다른 둘'), (select users_id from users where email = 'demo.halfday@example.com'), '2025-02-17T18:50:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '발렌타인에 구운 휘낭시에와 마들렌, 같은 듯 다른 둘'), (select users_id from users where email = 'demo.runner@example.com'), '2025-02-15T18:50:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '한여름 과발효 사건, 반죽이 볼을 넘어왔다'), (select users_id from users where email = 'demo.codecup@example.com'), '2025-07-09T17:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '한여름 과발효 사건, 반죽이 볼을 넘어왔다'), (select users_id from users where email = 'demo.film@example.com'), '2025-07-10T01:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '한여름 과발효 사건, 반죽이 볼을 넘어왔다'), (select users_id from users where email = 'demo.frontnote@example.com'), '2025-07-07T07:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '한여름 과발효 사건, 반죽이 볼을 넘어왔다'), (select users_id from users where email = 'demo.halfday@example.com'), '2025-07-10T04:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '한여름 과발효 사건, 반죽이 볼을 넘어왔다'), (select users_id from users where email = 'demo.homecook@example.com'), '2025-07-07T22:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '한여름 과발효 사건, 반죽이 볼을 넘어왔다'), (select users_id from users where email = 'demo.hometrain@example.com'), '2025-07-10T04:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '한여름 과발효 사건, 반죽이 볼을 넘어왔다'), (select users_id from users where email = 'demo.plant@example.com'), '2025-07-08T22:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '크리스마스 이브의 첫 딸기 생크림 케이크'), (select users_id from users where email = 'demo.hometrain@example.com'), '2025-12-28T03:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '크리스마스 이브의 첫 딸기 생크림 케이크'), (select users_id from users where email = 'demo.mukja@example.com'), '2025-12-28T01:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '크리스마스 이브의 첫 딸기 생크림 케이크'), (select users_id from users where email = 'demo.sea@example.com'), '2025-12-25T18:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '크리스마스 이브의 첫 딸기 생크림 케이크'), (select users_id from users where email = 'demo.woodwork@example.com'), '2025-12-25T12:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '엄마 생일 당근케이크, 크림치즈 프로스팅 꾸덕하게'), (select users_id from users where email = 'demo.halfday@example.com'), '2026-05-11T09:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '엄마 생일 당근케이크, 크림치즈 프로스팅 꾸덕하게'), (select users_id from users where email = 'demo.homecook@example.com'), '2026-05-13T16:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '베이킹 3년 차, 망친 것들 모아 보니 보이는 것'), (select users_id from users where email = 'demo.codecup@example.com'), '2026-09-22T17:45:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '베이킹 3년 차, 망친 것들 모아 보니 보이는 것'), (select users_id from users where email = 'demo.halfday@example.com'), '2026-09-20T23:45:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '베이킹 3년 차, 망친 것들 모아 보니 보이는 것'), (select users_id from users where email = 'demo.hometrain@example.com'), '2026-09-21T16:45:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '베이킹 3년 차, 망친 것들 모아 보니 보이는 것'), (select users_id from users where email = 'demo.mukja@example.com'), '2026-09-22T23:45:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '베이킹 3년 차, 망친 것들 모아 보니 보이는 것'), (select users_id from users where email = 'demo.runner@example.com'), '2026-09-21T18:45:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '베이킹 3년 차, 망친 것들 모아 보니 보이는 것'), (select users_id from users where email = 'demo.woodwork@example.com'), '2026-09-24T15:45:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '@Transactional이 안 먹던 이유: 같은 클래스 안에서 부르면 프록시를 안 탄다'), (select users_id from users where email = 'demo.bakery@example.com'), '2023-03-07T17:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '@Transactional이 안 먹던 이유: 같은 클래스 안에서 부르면 프록시를 안 탄다'), (select users_id from users where email = 'demo.film@example.com'), '2023-03-06T14:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '@Transactional이 안 먹던 이유: 같은 클래스 안에서 부르면 프록시를 안 탄다'), (select users_id from users where email = 'demo.frontnote@example.com'), '2023-03-07T17:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '@Transactional이 안 먹던 이유: 같은 클래스 안에서 부르면 프록시를 안 탄다'), (select users_id from users where email = 'demo.halfday@example.com'), '2023-03-07T00:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '@Transactional이 안 먹던 이유: 같은 클래스 안에서 부르면 프록시를 안 탄다'), (select users_id from users where email = 'demo.homecook@example.com'), '2023-03-08T15:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '@Transactional이 안 먹던 이유: 같은 클래스 안에서 부르면 프록시를 안 탄다'), (select users_id from users where email = 'demo.hometrain@example.com'), '2023-03-07T01:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '@Transactional이 안 먹던 이유: 같은 클래스 안에서 부르면 프록시를 안 탄다'), (select users_id from users where email = 'demo.sea@example.com'), '2023-03-07T13:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '@Transactional이 안 먹던 이유: 같은 클래스 안에서 부르면 프록시를 안 탄다'), (select users_id from users where email = 'demo.woodwork@example.com'), '2023-03-06T06:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'JPA N+1 문제, fetch join이랑 batch size로 잡은 기록'), (select users_id from users where email = 'demo.bakery@example.com'), '2023-07-25T04:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'JPA N+1 문제, fetch join이랑 batch size로 잡은 기록'), (select users_id from users where email = 'demo.frontnote@example.com'), '2023-07-25T06:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '1년 차 회고: 돌아가는 코드에서 설명할 수 있는 코드로'), (select users_id from users where email = 'demo.bakery@example.com'), '2024-01-03T17:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '1년 차 회고: 돌아가는 코드에서 설명할 수 있는 코드로'), (select users_id from users where email = 'demo.film@example.com'), '2024-01-01T21:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '1년 차 회고: 돌아가는 코드에서 설명할 수 있는 코드로'), (select users_id from users where email = 'demo.frontnote@example.com'), '2024-01-01T21:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '1년 차 회고: 돌아가는 코드에서 설명할 수 있는 코드로'), (select users_id from users where email = 'demo.halfday@example.com'), '2024-01-01T18:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '1년 차 회고: 돌아가는 코드에서 설명할 수 있는 코드로'), (select users_id from users where email = 'demo.hometrain@example.com'), '2024-01-01T09:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '1년 차 회고: 돌아가는 코드에서 설명할 수 있는 코드로'), (select users_id from users where email = 'demo.mukja@example.com'), '2024-01-02T02:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '1년 차 회고: 돌아가는 코드에서 설명할 수 있는 코드로'), (select users_id from users where email = 'demo.runner@example.com'), '2024-01-04T06:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'PostgreSQL에서 인덱스 탔는지 EXPLAIN ANALYZE로 확인하기'), (select users_id from users where email = 'demo.film@example.com'), '2024-04-15T15:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'PostgreSQL에서 인덱스 탔는지 EXPLAIN ANALYZE로 확인하기'), (select users_id from users where email = 'demo.frontnote@example.com'), '2024-04-14T22:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'PostgreSQL에서 인덱스 탔는지 EXPLAIN ANALYZE로 확인하기'), (select users_id from users where email = 'demo.halfday@example.com'), '2024-04-18T00:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'Docker 컨테이너에서 Spring Boot가 DB에 못 붙던 이유: localhost의 함정'), (select users_id from users where email = 'demo.frontnote@example.com'), '2024-10-21T05:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'Docker 컨테이너에서 Spring Boot가 DB에 못 붙던 이유: localhost의 함정'), (select users_id from users where email = 'demo.homecook@example.com'), '2024-10-22T08:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'Docker 컨테이너에서 Spring Boot가 DB에 못 붙던 이유: localhost의 함정'), (select users_id from users where email = 'demo.hometrain@example.com'), '2024-10-24T07:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'Docker 컨테이너에서 Spring Boot가 DB에 못 붙던 이유: localhost의 함정'), (select users_id from users where email = 'demo.sea@example.com'), '2024-10-24T20:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'Docker 컨테이너에서 Spring Boot가 DB에 못 붙던 이유: localhost의 함정'), (select users_id from users where email = 'demo.woodwork@example.com'), '2024-10-21T17:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'Testcontainers로 통합 테스트 옮긴 후기'), (select users_id from users where email = 'demo.film@example.com'), '2025-02-13T14:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'Testcontainers로 통합 테스트 옮긴 후기'), (select users_id from users where email = 'demo.frontnote@example.com'), '2025-02-11T11:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'Testcontainers로 통합 테스트 옮긴 후기'), (select users_id from users where email = 'demo.halfday@example.com'), '2025-02-11T06:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'PostgreSQL 데드락 로그 읽는 법, 이번엔 진짜 데드락이었다'), (select users_id from users where email = 'demo.film@example.com'), '2025-08-21T18:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'PostgreSQL 데드락 로그 읽는 법, 이번엔 진짜 데드락이었다'), (select users_id from users where email = 'demo.frontnote@example.com'), '2025-08-21T15:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '2025 회고: 3년 차가 되면서 달라진 것들'), (select users_id from users where email = 'demo.film@example.com'), '2026-01-08T12:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '2025 회고: 3년 차가 되면서 달라진 것들'), (select users_id from users where email = 'demo.frontnote@example.com'), '2026-01-07T00:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '2025 회고: 3년 차가 되면서 달라진 것들'), (select users_id from users where email = 'demo.halfday@example.com'), '2026-01-07T01:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '2025 회고: 3년 차가 되면서 달라진 것들'), (select users_id from users where email = 'demo.homecook@example.com'), '2026-01-06T14:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '2025 회고: 3년 차가 되면서 달라진 것들'), (select users_id from users where email = 'demo.hometrain@example.com'), '2026-01-05T20:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '2025 회고: 3년 차가 되면서 달라진 것들'), (select users_id from users where email = 'demo.mukja@example.com'), '2026-01-05T10:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '2025 회고: 3년 차가 되면서 달라진 것들'), (select users_id from users where email = 'demo.runner@example.com'), '2026-01-06T09:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '2025 회고: 3년 차가 되면서 달라진 것들'), (select users_id from users where email = 'demo.woodwork@example.com'), '2026-01-08T19:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '커넥션 풀 고갈 장애 회고: HikariCP 로그를 따라간 길'), (select users_id from users where email = 'demo.frontnote@example.com'), '2026-09-28T19:45:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '커넥션 풀 고갈 장애 회고: HikariCP 로그를 따라간 길'), (select users_id from users where email = 'demo.halfday@example.com'), '2026-09-28T22:45:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '커넥션 풀 고갈 장애 회고: HikariCP 로그를 따라간 길'), (select users_id from users where email = 'demo.hometrain@example.com'), '2026-09-28T19:45:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '첫 필름카메라, 중고 수동 SLR을 들이다'), (select users_id from users where email = 'demo.bakery@example.com'), '2023-04-10T08:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '첫 필름카메라, 중고 수동 SLR을 들이다'), (select users_id from users where email = 'demo.codecup@example.com'), '2023-04-10T07:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '첫 필름카메라, 중고 수동 SLR을 들이다'), (select users_id from users where email = 'demo.homecook@example.com'), '2023-04-10T00:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '첫 필름카메라, 중고 수동 SLR을 들이다'), (select users_id from users where email = 'demo.hometrain@example.com'), '2023-04-10T10:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '첫 필름카메라, 중고 수동 SLR을 들이다'), (select users_id from users where email = 'demo.mukja@example.com'), '2023-04-11T04:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '첫 필름카메라, 중고 수동 SLR을 들이다'), (select users_id from users where email = 'demo.sea@example.com'), '2023-04-09T04:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '첫 필름카메라, 중고 수동 SLR을 들이다'), (select users_id from users where email = 'demo.woodwork@example.com'), '2023-04-10T07:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '가을 서촌 골목 출사 기록'), (select users_id from users where email = 'demo.halfday@example.com'), '2023-10-24T23:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '가을 서촌 골목 출사 기록'), (select users_id from users where email = 'demo.homecook@example.com'), '2023-10-24T13:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '가을 서촌 골목 출사 기록'), (select users_id from users where email = 'demo.plant@example.com'), '2023-10-22T12:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '가을 서촌 골목 출사 기록'), (select users_id from users where email = 'demo.runner@example.com'), '2023-10-22T00:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '가을 서촌 골목 출사 기록'), (select users_id from users where email = 'demo.sea@example.com'), '2023-10-24T22:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '현상소에 필름 맡기는 법과 스캔 해상도 고르는 기준'), (select users_id from users where email = 'demo.bakery@example.com'), '2024-03-19T06:05:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '현상소에 필름 맡기는 법과 스캔 해상도 고르는 기준'), (select users_id from users where email = 'demo.codecup@example.com'), '2024-03-20T15:05:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '현상소에 필름 맡기는 법과 스캔 해상도 고르는 기준'), (select users_id from users where email = 'demo.frontnote@example.com'), '2024-03-19T17:05:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '현상소에 필름 맡기는 법과 스캔 해상도 고르는 기준'), (select users_id from users where email = 'demo.halfday@example.com'), '2024-03-19T19:05:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '현상소에 필름 맡기는 법과 스캔 해상도 고르는 기준'), (select users_id from users where email = 'demo.plant@example.com'), '2024-03-19T17:05:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '흑백 필름 1스톱 증감 현상 후기 (ISO 400 → 800)'), (select users_id from users where email = 'demo.frontnote@example.com'), '2024-08-06T10:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '흑백 필름 1스톱 증감 현상 후기 (ISO 400 → 800)'), (select users_id from users where email = 'demo.runner@example.com'), '2024-08-04T23:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '흑백 필름 1스톱 증감 현상 후기 (ISO 400 → 800)'), (select users_id from users where email = 'demo.sea@example.com'), '2024-08-04T14:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '군산 1박 2일 필름 여행, 세 롤 다 썼다'), (select users_id from users where email = 'demo.bakery@example.com'), '2025-02-26T02:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '군산 1박 2일 필름 여행, 세 롤 다 썼다'), (select users_id from users where email = 'demo.halfday@example.com'), '2025-02-23T02:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '군산 1박 2일 필름 여행, 세 롤 다 썼다'), (select users_id from users where email = 'demo.mukja@example.com'), '2025-02-26T17:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '군산 1박 2일 필름 여행, 세 롤 다 썼다'), (select users_id from users where email = 'demo.runner@example.com'), '2025-02-24T10:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '장마철 필름 보관, 결국 냉장고 한 칸을 비웠다'), (select users_id from users where email = 'demo.bakery@example.com'), '2025-07-16T15:50:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '장마철 필름 보관, 결국 냉장고 한 칸을 비웠다'), (select users_id from users where email = 'demo.frontnote@example.com'), '2025-07-13T23:50:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '장마철 필름 보관, 결국 냉장고 한 칸을 비웠다'), (select users_id from users where email = 'demo.homecook@example.com'), '2025-07-16T20:50:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '장마철 필름 보관, 결국 냉장고 한 칸을 비웠다'), (select users_id from users where email = 'demo.hometrain@example.com'), '2025-07-13T23:50:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '장마철 필름 보관, 결국 냉장고 한 칸을 비웠다'), (select users_id from users where email = 'demo.plant@example.com'), '2025-07-13T04:50:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '장마철 필름 보관, 결국 냉장고 한 칸을 비웠다'), (select users_id from users where email = 'demo.sea@example.com'), '2025-07-16T07:50:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '하프카메라로 벚꽃 72컷 찍은 날'), (select users_id from users where email = 'demo.frontnote@example.com'), '2026-04-01T04:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '하프카메라로 벚꽃 72컷 찍은 날'), (select users_id from users where email = 'demo.halfday@example.com'), '2026-03-30T09:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '하프카메라로 벚꽃 72컷 찍은 날'), (select users_id from users where email = 'demo.sea@example.com'), '2026-03-31T22:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '노출계 없이 ''써니 16'' 한 달 연습 기록'), (select users_id from users where email = 'demo.codecup@example.com'), '2026-08-26T17:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '노출계 없이 ''써니 16'' 한 달 연습 기록'), (select users_id from users where email = 'demo.runner@example.com'), '2026-08-27T09:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '노출계 없이 ''써니 16'' 한 달 연습 기록'), (select users_id from users where email = 'demo.woodwork@example.com'), '2026-08-24T06:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '부산 영도 흰여울마을, 바다 냄새 나는 필름 산책'), (select users_id from users where email = 'demo.halfday@example.com'), '2026-10-06T03:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '부산 영도 흰여울마을, 바다 냄새 나는 필름 산책'), (select users_id from users where email = 'demo.homecook@example.com'), '2026-10-08T04:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '부산 영도 흰여울마을, 바다 냄새 나는 필름 산책'), (select users_id from users where email = 'demo.mukja@example.com'), '2026-10-07T17:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '부산 영도 흰여울마을, 바다 냄새 나는 필름 산책'), (select users_id from users where email = 'demo.sea@example.com'), '2026-10-07T05:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '부산 영도 흰여울마을, 바다 냄새 나는 필름 산책'), (select users_id from users where email = 'demo.woodwork@example.com'), '2026-10-05T14:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'useEffect 의존성 배열 때문에 API를 무한으로 부른 삽질기'), (select users_id from users where email = 'demo.codecup@example.com'), '2023-06-19T01:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'useEffect 의존성 배열 때문에 API를 무한으로 부른 삽질기'), (select users_id from users where email = 'demo.film@example.com'), '2023-06-21T16:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'useEffect 의존성 배열 때문에 API를 무한으로 부른 삽질기'), (select users_id from users where email = 'demo.homecook@example.com'), '2023-06-20T06:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'useEffect 의존성 배열 때문에 API를 무한으로 부른 삽질기'), (select users_id from users where email = 'demo.hometrain@example.com'), '2023-06-20T22:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'useEffect 의존성 배열 때문에 API를 무한으로 부른 삽질기'), (select users_id from users where email = 'demo.mukja@example.com'), '2023-06-22T00:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'useEffect 의존성 배열 때문에 API를 무한으로 부른 삽질기'), (select users_id from users where email = 'demo.plant@example.com'), '2023-06-19T08:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'useEffect 의존성 배열 때문에 API를 무한으로 부른 삽질기'), (select users_id from users where email = 'demo.sea@example.com'), '2023-06-20T00:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'CSS Grid 카드 목록, auto-fill이랑 auto-fit 차이 이제 안 헷갈리기'), (select users_id from users where email = 'demo.bakery@example.com'), '2023-11-16T00:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'CSS Grid 카드 목록, auto-fill이랑 auto-fit 차이 이제 안 헷갈리기'), (select users_id from users where email = 'demo.codecup@example.com'), '2023-11-14T09:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'CSS Grid 카드 목록, auto-fill이랑 auto-fit 차이 이제 안 헷갈리기'), (select users_id from users where email = 'demo.film@example.com'), '2023-11-13T07:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'CSS Grid 카드 목록, auto-fill이랑 auto-fit 차이 이제 안 헷갈리기'), (select users_id from users where email = 'demo.homecook@example.com'), '2023-11-15T18:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'CSS Grid 카드 목록, auto-fill이랑 auto-fit 차이 이제 안 헷갈리기'), (select users_id from users where email = 'demo.mukja@example.com'), '2023-11-16T12:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'CSS Grid 카드 목록, auto-fill이랑 auto-fit 차이 이제 안 헷갈리기'), (select users_id from users where email = 'demo.woodwork@example.com'), '2023-11-13T09:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '버튼 대신 div에 onClick 달면 생기는 일'), (select users_id from users where email = 'demo.codecup@example.com'), '2024-03-04T10:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '버튼 대신 div에 onClick 달면 생기는 일'), (select users_id from users where email = 'demo.film@example.com'), '2024-03-07T13:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '버튼 대신 div에 onClick 달면 생기는 일'), (select users_id from users where email = 'demo.homecook@example.com'), '2024-03-06T07:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '버튼 대신 div에 onClick 달면 생기는 일'), (select users_id from users where email = 'demo.hometrain@example.com'), '2024-03-04T03:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '버튼 대신 div에 onClick 달면 생기는 일'), (select users_id from users where email = 'demo.plant@example.com'), '2024-03-06T18:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '버튼 대신 div에 onClick 달면 생기는 일'), (select users_id from users where email = 'demo.runner@example.com'), '2024-03-05T00:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '사이드 프로젝트: 동네 빵집 지도 만들기 1편, 기획부터 첫 배포까지'), (select users_id from users where email = 'demo.bakery@example.com'), '2024-08-26T04:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '사이드 프로젝트: 동네 빵집 지도 만들기 1편, 기획부터 첫 배포까지'), (select users_id from users where email = 'demo.codecup@example.com'), '2024-08-29T15:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '사이드 프로젝트: 동네 빵집 지도 만들기 1편, 기획부터 첫 배포까지'), (select users_id from users where email = 'demo.mukja@example.com'), '2024-08-29T14:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'TypeScript 유니온 타입으로 로딩·에러 상태 정리하기'), (select users_id from users where email = 'demo.bakery@example.com'), '2025-01-20T08:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'TypeScript 유니온 타입으로 로딩·에러 상태 정리하기'), (select users_id from users where email = 'demo.codecup@example.com'), '2025-01-23T08:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'TypeScript 유니온 타입으로 로딩·에러 상태 정리하기'), (select users_id from users where email = 'demo.halfday@example.com'), '2025-01-22T07:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'TypeScript 유니온 타입으로 로딩·에러 상태 정리하기'), (select users_id from users where email = 'demo.homecook@example.com'), '2025-01-23T04:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'TypeScript 유니온 타입으로 로딩·에러 상태 정리하기'), (select users_id from users where email = 'demo.hometrain@example.com'), '2025-01-20T06:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'TypeScript 유니온 타입으로 로딩·에러 상태 정리하기'), (select users_id from users where email = 'demo.runner@example.com'), '2025-01-21T21:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '다크 모드, CSS 변수로 한 번에 정리한 방법'), (select users_id from users where email = 'demo.bakery@example.com'), '2025-06-12T03:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '다크 모드, CSS 변수로 한 번에 정리한 방법'), (select users_id from users where email = 'demo.codecup@example.com'), '2025-06-09T14:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '다크 모드, CSS 변수로 한 번에 정리한 방법'), (select users_id from users where email = 'demo.homecook@example.com'), '2025-06-12T11:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '다크 모드, CSS 변수로 한 번에 정리한 방법'), (select users_id from users where email = 'demo.plant@example.com'), '2025-06-08T23:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '모달 접근성 체크리스트: 포커스, Esc, 그리고 dialog 요소'), (select users_id from users where email = 'demo.bakery@example.com'), '2025-12-01T11:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '모달 접근성 체크리스트: 포커스, Esc, 그리고 dialog 요소'), (select users_id from users where email = 'demo.codecup@example.com'), '2025-12-03T13:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '모달 접근성 체크리스트: 포커스, Esc, 그리고 dialog 요소'), (select users_id from users where email = 'demo.mukja@example.com'), '2025-12-04T12:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '모달 접근성 체크리스트: 포커스, Esc, 그리고 dialog 요소'), (select users_id from users where email = 'demo.plant@example.com'), '2025-12-03T09:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '모달 접근성 체크리스트: 포커스, Esc, 그리고 dialog 요소'), (select users_id from users where email = 'demo.runner@example.com'), '2025-12-04T07:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '제주 워케이션 일주일, 일하고 걷고 귤 먹은 기록'), (select users_id from users where email = 'demo.bakery@example.com'), '2026-05-21T07:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '제주 워케이션 일주일, 일하고 걷고 귤 먹은 기록'), (select users_id from users where email = 'demo.halfday@example.com'), '2026-05-20T22:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '제주 워케이션 일주일, 일하고 걷고 귤 먹은 기록'), (select users_id from users where email = 'demo.hometrain@example.com'), '2026-05-17T21:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '제주 워케이션 일주일, 일하고 걷고 귤 먹은 기록'), (select users_id from users where email = 'demo.mukja@example.com'), '2026-05-18T03:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '제주 워케이션 일주일, 일하고 걷고 귤 먹은 기록'), (select users_id from users where email = 'demo.plant@example.com'), '2026-05-21T10:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '제주 워케이션 일주일, 일하고 걷고 귤 먹은 기록'), (select users_id from users where email = 'demo.sea@example.com'), '2026-05-18T05:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = '제주 워케이션 일주일, 일하고 걷고 귤 먹은 기록'), (select users_id from users where email = 'demo.woodwork@example.com'), '2026-05-21T10:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'React 19의 폼 액션과 useActionState, 실제로 써 본 정리'), (select users_id from users where email = 'demo.bakery@example.com'), '2026-10-06T05:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'React 19의 폼 액션과 useActionState, 실제로 써 본 정리'), (select users_id from users where email = 'demo.codecup@example.com'), '2026-10-07T23:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'React 19의 폼 액션과 useActionState, 실제로 써 본 정리'), (select users_id from users where email = 'demo.mukja@example.com'), '2026-10-06T13:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '양평 두물머리 반나절, 경의중앙선 타고 다녀옴'), (select users_id from users where email = 'demo.codecup@example.com'), '2023-03-07T04:22:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '양평 두물머리 반나절, 경의중앙선 타고 다녀옴'), (select users_id from users where email = 'demo.homecook@example.com'), '2023-03-05T14:22:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '양평 두물머리 반나절, 경의중앙선 타고 다녀옴'), (select users_id from users where email = 'demo.runner@example.com'), '2023-03-07T08:22:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '양평 두물머리 반나절, 경의중앙선 타고 다녀옴'), (select users_id from users where email = 'demo.sea@example.com'), '2023-03-07T04:22:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '양평 두물머리 반나절, 경의중앙선 타고 다녀옴'), (select users_id from users where email = 'demo.woodwork@example.com'), '2023-03-08T17:22:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '후쿠오카 2박 3일, 동선만 깔끔하게 정리'), (select users_id from users where email = 'demo.bakery@example.com'), '2023-10-23T07:48:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '후쿠오카 2박 3일, 동선만 깔끔하게 정리'), (select users_id from users where email = 'demo.mukja@example.com'), '2023-10-24T02:48:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '후쿠오카 2박 3일, 동선만 깔끔하게 정리'), (select users_id from users where email = 'demo.runner@example.com'), '2023-10-25T07:48:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '후쿠오카 2박 3일, 동선만 깔끔하게 정리'), (select users_id from users where email = 'demo.sea@example.com'), '2023-10-23T17:48:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '기내 수하물 7kg만으로 4박 하기, 짐 싸기 체크리스트'), (select users_id from users where email = 'demo.frontnote@example.com'), '2024-02-18T09:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '기내 수하물 7kg만으로 4박 하기, 짐 싸기 체크리스트'), (select users_id from users where email = 'demo.hometrain@example.com'), '2024-02-18T20:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '기내 수하물 7kg만으로 4박 하기, 짐 싸기 체크리스트'), (select users_id from users where email = 'demo.sea@example.com'), '2024-02-18T03:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '수원 화성 성곽길 반나절 한 바퀴 걸은 날'), (select users_id from users where email = 'demo.film@example.com'), '2024-06-11T06:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '수원 화성 성곽길 반나절 한 바퀴 걸은 날'), (select users_id from users where email = 'demo.homecook@example.com'), '2024-06-11T04:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '수원 화성 성곽길 반나절 한 바퀴 걸은 날'), (select users_id from users where email = 'demo.mukja@example.com'), '2024-06-12T05:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '수원 화성 성곽길 반나절 한 바퀴 걸은 날'), (select users_id from users where email = 'demo.plant@example.com'), '2024-06-11T04:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '수원 화성 성곽길 반나절 한 바퀴 걸은 날'), (select users_id from users where email = 'demo.runner@example.com'), '2024-06-12T07:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '수원 화성 성곽길 반나절 한 바퀴 걸은 날'), (select users_id from users where email = 'demo.sea@example.com'), '2024-06-10T10:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '타이베이 4박 5일, 지하철로만 다닌 기록'), (select users_id from users where email = 'demo.codecup@example.com'), '2025-01-27T11:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '타이베이 4박 5일, 지하철로만 다닌 기록'), (select users_id from users where email = 'demo.homecook@example.com'), '2025-01-28T05:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '타이베이 4박 5일, 지하철로만 다닌 기록'), (select users_id from users where email = 'demo.hometrain@example.com'), '2025-01-26T14:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '타이베이 4박 5일, 지하철로만 다닌 기록'), (select users_id from users where email = 'demo.mukja@example.com'), '2025-01-29T18:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '타이베이 4박 5일, 지하철로만 다닌 기록'), (select users_id from users where email = 'demo.sea@example.com'), '2025-01-28T05:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '인천 개항장 골목 반나절, 차이나타운 옆 조용한 길'), (select users_id from users where email = 'demo.bakery@example.com'), '2025-05-12T10:05:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '인천 개항장 골목 반나절, 차이나타운 옆 조용한 길'), (select users_id from users where email = 'demo.film@example.com'), '2025-05-14T08:05:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '인천 개항장 골목 반나절, 차이나타운 옆 조용한 길'), (select users_id from users where email = 'demo.homecook@example.com'), '2025-05-12T14:05:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '인천 개항장 골목 반나절, 차이나타운 옆 조용한 길'), (select users_id from users where email = 'demo.mukja@example.com'), '2025-05-12T06:05:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '인천 개항장 골목 반나절, 차이나타운 옆 조용한 길'), (select users_id from users where email = 'demo.runner@example.com'), '2025-05-13T00:05:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '인천 개항장 골목 반나절, 차이나타운 옆 조용한 길'), (select users_id from users where email = 'demo.sea@example.com'), '2025-05-12T03:05:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '인천 개항장 골목 반나절, 차이나타운 옆 조용한 길'), (select users_id from users where email = 'demo.woodwork@example.com'), '2025-05-11T22:05:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '북한산 우이령길 예약하고 걸은 날, 초보도 괜찮은 숲길'), (select users_id from users where email = 'demo.hometrain@example.com'), '2025-09-23T13:12:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '북한산 우이령길 예약하고 걸은 날, 초보도 괜찮은 숲길'), (select users_id from users where email = 'demo.plant@example.com'), '2025-09-22T16:12:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '북한산 우이령길 예약하고 걸은 날, 초보도 괜찮은 숲길'), (select users_id from users where email = 'demo.runner@example.com'), '2025-09-24T06:12:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '북한산 우이령길 예약하고 걸은 날, 초보도 괜찮은 숲길'), (select users_id from users where email = 'demo.sea@example.com'), '2025-09-23T18:12:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '환전 대신 트래블카드 1년 써 보고 정리한 장단점'), (select users_id from users where email = 'demo.codecup@example.com'), '2026-03-17T14:26:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '환전 대신 트래블카드 1년 써 보고 정리한 장단점'), (select users_id from users where email = 'demo.film@example.com'), '2026-03-15T15:26:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '환전 대신 트래블카드 1년 써 보고 정리한 장단점'), (select users_id from users where email = 'demo.homecook@example.com'), '2026-03-16T03:26:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '환전 대신 트래블카드 1년 써 보고 정리한 장단점'), (select users_id from users where email = 'demo.hometrain@example.com'), '2026-03-15T00:26:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '환전 대신 트래블카드 1년 써 보고 정리한 장단점'), (select users_id from users where email = 'demo.mukja@example.com'), '2026-03-16T20:26:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '환전 대신 트래블카드 1년 써 보고 정리한 장단점'), (select users_id from users where email = 'demo.sea@example.com'), '2026-03-15T13:26:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '환전 대신 트래블카드 1년 써 보고 정리한 장단점'), (select users_id from users where email = 'demo.woodwork@example.com'), '2026-03-16T01:26:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '가평 자라섬 가을 반나절, ITX 타고 다녀온 일기'), (select users_id from users where email = 'demo.frontnote@example.com'), '2026-10-06T21:48:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '가평 자라섬 가을 반나절, ITX 타고 다녀온 일기'), (select users_id from users where email = 'demo.plant@example.com'), '2026-10-05T13:48:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '가평 자라섬 가을 반나절, ITX 타고 다녀온 일기'), (select users_id from users where email = 'demo.runner@example.com'), '2026-10-07T23:48:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '가평 자라섬 가을 반나절, ITX 타고 다녀온 일기'), (select users_id from users where email = 'demo.sea@example.com'), '2026-10-05T05:48:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '맞벌이 부부의 평일 집밥, 일요일 두 시간으로 시작해요'), (select users_id from users where email = 'demo.bakery@example.com'), '2023-03-08T06:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '맞벌이 부부의 평일 집밥, 일요일 두 시간으로 시작해요'), (select users_id from users where email = 'demo.film@example.com'), '2023-03-07T03:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '맞벌이 부부의 평일 집밥, 일요일 두 시간으로 시작해요'), (select users_id from users where email = 'demo.mukja@example.com'), '2023-03-07T12:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '맞벌이 부부의 평일 집밥, 일요일 두 시간으로 시작해요'), (select users_id from users where email = 'demo.plant@example.com'), '2023-03-06T21:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '맞벌이 부부의 평일 집밥, 일요일 두 시간으로 시작해요'), (select users_id from users where email = 'demo.runner@example.com'), '2023-03-06T07:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '맞벌이 부부의 평일 집밥, 일요일 두 시간으로 시작해요'), (select users_id from users where email = 'demo.woodwork@example.com'), '2023-03-06T03:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '애호박 두부 된장찌개, 물 올리고 20분이면 끝'), (select users_id from users where email = 'demo.bakery@example.com'), '2023-09-16T13:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '애호박 두부 된장찌개, 물 올리고 20분이면 끝'), (select users_id from users where email = 'demo.codecup@example.com'), '2023-09-16T15:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '애호박 두부 된장찌개, 물 올리고 20분이면 끝'), (select users_id from users where email = 'demo.hometrain@example.com'), '2023-09-14T22:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉장고 파먹기 1탄: 자투리 채소 볶음밥이랑 계란국'), (select users_id from users where email = 'demo.codecup@example.com'), '2024-03-01T15:05:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉장고 파먹기 1탄: 자투리 채소 볶음밥이랑 계란국'), (select users_id from users where email = 'demo.frontnote@example.com'), '2024-02-28T06:05:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉장고 파먹기 1탄: 자투리 채소 볶음밥이랑 계란국'), (select users_id from users where email = 'demo.hometrain@example.com'), '2024-03-01T12:05:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉장고 파먹기 1탄: 자투리 채소 볶음밥이랑 계란국'), (select users_id from users where email = 'demo.mukja@example.com'), '2024-03-01T01:05:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉장고 파먹기 1탄: 자투리 채소 볶음밥이랑 계란국'), (select users_id from users where email = 'demo.plant@example.com'), '2024-02-29T18:05:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉장고 파먹기 1탄: 자투리 채소 볶음밥이랑 계란국'), (select users_id from users where email = 'demo.sea@example.com'), '2024-02-28T00:05:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '여름 밑반찬 세 가지: 오이무침, 가지볶음, 감자채볶음'), (select users_id from users where email = 'demo.bakery@example.com'), '2024-08-22T10:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '여름 밑반찬 세 가지: 오이무침, 가지볶음, 감자채볶음'), (select users_id from users where email = 'demo.frontnote@example.com'), '2024-08-20T10:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '여름 밑반찬 세 가지: 오이무침, 가지볶음, 감자채볶음'), (select users_id from users where email = 'demo.halfday@example.com'), '2024-08-21T14:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '여름 밑반찬 세 가지: 오이무침, 가지볶음, 감자채볶음'), (select users_id from users where email = 'demo.hometrain@example.com'), '2024-08-22T05:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '여름 밑반찬 세 가지: 오이무침, 가지볶음, 감자채볶음'), (select users_id from users where email = 'demo.plant@example.com'), '2024-08-19T05:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '여름 밑반찬 세 가지: 오이무침, 가지볶음, 감자채볶음'), (select users_id from users where email = 'demo.woodwork@example.com'), '2024-08-21T03:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉동실 정리하다가 만든 돼지고기 김치 짜글이'), (select users_id from users where email = 'demo.film@example.com'), '2025-03-22T18:50:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉동실 정리하다가 만든 돼지고기 김치 짜글이'), (select users_id from users where email = 'demo.frontnote@example.com'), '2025-03-21T05:50:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉동실 정리하다가 만든 돼지고기 김치 짜글이'), (select users_id from users where email = 'demo.halfday@example.com'), '2025-03-21T11:50:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉동실 정리하다가 만든 돼지고기 김치 짜글이'), (select users_id from users where email = 'demo.hometrain@example.com'), '2025-03-19T02:50:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉동실 정리하다가 만든 돼지고기 김치 짜글이'), (select users_id from users where email = 'demo.mukja@example.com'), '2025-03-22T11:50:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉동실 정리하다가 만든 돼지고기 김치 짜글이'), (select users_id from users where email = 'demo.plant@example.com'), '2025-03-19T03:50:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉동실 정리하다가 만든 돼지고기 김치 짜글이'), (select users_id from users where email = 'demo.sea@example.com'), '2025-03-22T05:50:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '속초 1박 2일, 중앙시장에서 사 온 것들로 일주일 버티기'), (select users_id from users where email = 'demo.bakery@example.com'), '2025-09-11T13:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '속초 1박 2일, 중앙시장에서 사 온 것들로 일주일 버티기'), (select users_id from users where email = 'demo.film@example.com'), '2025-09-08T23:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '속초 1박 2일, 중앙시장에서 사 온 것들로 일주일 버티기'), (select users_id from users where email = 'demo.halfday@example.com'), '2025-09-09T06:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '속초 1박 2일, 중앙시장에서 사 온 것들로 일주일 버티기'), (select users_id from users where email = 'demo.hometrain@example.com'), '2025-09-10T15:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '속초 1박 2일, 중앙시장에서 사 온 것들로 일주일 버티기'), (select users_id from users where email = 'demo.sea@example.com'), '2025-09-09T21:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '간장 닭다리살 덮밥, 25분 저녁의 정석'), (select users_id from users where email = 'demo.bakery@example.com'), '2026-04-16T02:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '간장 닭다리살 덮밥, 25분 저녁의 정석'), (select users_id from users where email = 'demo.codecup@example.com'), '2026-04-16T20:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '간장 닭다리살 덮밥, 25분 저녁의 정석'), (select users_id from users where email = 'demo.halfday@example.com'), '2026-04-15T11:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '간장 닭다리살 덮밥, 25분 저녁의 정석'), (select users_id from users where email = 'demo.hometrain@example.com'), '2026-04-16T17:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '간장 닭다리살 덮밥, 25분 저녁의 정석'), (select users_id from users where email = 'demo.mukja@example.com'), '2026-04-17T11:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '일요일 반찬 데이: 마약계란장이랑 메추리알 장조림'), (select users_id from users where email = 'demo.bakery@example.com'), '2026-07-22T10:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '일요일 반찬 데이: 마약계란장이랑 메추리알 장조림'), (select users_id from users where email = 'demo.film@example.com'), '2026-07-23T14:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '일요일 반찬 데이: 마약계란장이랑 메추리알 장조림'), (select users_id from users where email = 'demo.halfday@example.com'), '2026-07-19T21:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '일요일 반찬 데이: 마약계란장이랑 메추리알 장조림'), (select users_id from users where email = 'demo.plant@example.com'), '2026-07-22T11:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '일요일 반찬 데이: 마약계란장이랑 메추리알 장조림'), (select users_id from users where email = 'demo.runner@example.com'), '2026-07-23T03:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '추석 끝나고 남은 전으로 끓인 전찌개, 연휴 뒤 첫 저녁'), (select users_id from users where email = 'demo.film@example.com'), '2026-09-30T18:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '추석 끝나고 남은 전으로 끓인 전찌개, 연휴 뒤 첫 저녁'), (select users_id from users where email = 'demo.frontnote@example.com'), '2026-10-01T17:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '추석 끝나고 남은 전으로 끓인 전찌개, 연휴 뒤 첫 저녁'), (select users_id from users where email = 'demo.mukja@example.com'), '2026-10-01T03:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '추석 끝나고 남은 전으로 끓인 전찌개, 연휴 뒤 첫 저녁'), (select users_id from users where email = 'demo.sea@example.com'), '2026-09-30T06:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '추석 끝나고 남은 전으로 끓인 전찌개, 연휴 뒤 첫 저녁'), (select users_id from users where email = 'demo.woodwork@example.com'), '2026-10-03T11:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '퇴근하고 홈트 시작한 이유, 그리고 첫 주 기록'), (select users_id from users where email = 'demo.bakery@example.com'), '2023-04-11T11:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '퇴근하고 홈트 시작한 이유, 그리고 첫 주 기록'), (select users_id from users where email = 'demo.codecup@example.com'), '2023-04-12T09:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '퇴근하고 홈트 시작한 이유, 그리고 첫 주 기록'), (select users_id from users where email = 'demo.frontnote@example.com'), '2023-04-10T16:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '퇴근하고 홈트 시작한 이유, 그리고 첫 주 기록'), (select users_id from users where email = 'demo.halfday@example.com'), '2023-04-13T14:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '퇴근하고 홈트 시작한 이유, 그리고 첫 주 기록'), (select users_id from users where email = 'demo.runner@example.com'), '2023-04-11T02:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '퇴근하고 홈트 시작한 이유, 그리고 첫 주 기록'), (select users_id from users where email = 'demo.sea@example.com'), '2023-04-13T12:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '푸시업 0개에서 20개까지, 석 달 걸린 과정'), (select users_id from users where email = 'demo.bakery@example.com'), '2023-08-21T18:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '푸시업 0개에서 20개까지, 석 달 걸린 과정'), (select users_id from users where email = 'demo.film@example.com'), '2023-08-23T21:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '푸시업 0개에서 20개까지, 석 달 걸린 과정'), (select users_id from users where email = 'demo.halfday@example.com'), '2023-08-23T17:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '푸시업 0개에서 20개까지, 석 달 걸린 과정'), (select users_id from users where email = 'demo.sea@example.com'), '2023-08-22T04:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '푸시업 0개에서 20개까지, 석 달 걸린 과정'), (select users_id from users where email = 'demo.woodwork@example.com'), '2023-08-22T14:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '무릎 안 아픈 스쿼트, 거울 앞에서 확인하는 체크리스트'), (select users_id from users where email = 'demo.codecup@example.com'), '2024-01-15T11:05:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '무릎 안 아픈 스쿼트, 거울 앞에서 확인하는 체크리스트'), (select users_id from users where email = 'demo.film@example.com'), '2024-01-18T11:05:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '무릎 안 아픈 스쿼트, 거울 앞에서 확인하는 체크리스트'), (select users_id from users where email = 'demo.frontnote@example.com'), '2024-01-15T03:05:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '무릎 안 아픈 스쿼트, 거울 앞에서 확인하는 체크리스트'), (select users_id from users where email = 'demo.halfday@example.com'), '2024-01-15T11:05:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '무릎 안 아픈 스쿼트, 거울 앞에서 확인하는 체크리스트'), (select users_id from users where email = 'demo.runner@example.com'), '2024-01-16T03:05:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '무릎 안 아픈 스쿼트, 거울 앞에서 확인하는 체크리스트'), (select users_id from users where email = 'demo.woodwork@example.com'), '2024-01-16T22:05:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '닭가슴살 질릴 때 돌려 먹는 단백질 반찬 5가지'), (select users_id from users where email = 'demo.homecook@example.com'), '2024-06-05T15:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '닭가슴살 질릴 때 돌려 먹는 단백질 반찬 5가지'), (select users_id from users where email = 'demo.mukja@example.com'), '2024-06-03T10:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '닭가슴살 질릴 때 돌려 먹는 단백질 반찬 5가지'), (select users_id from users where email = 'demo.plant@example.com'), '2024-06-03T02:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '닭가슴살 질릴 때 돌려 먹는 단백질 반찬 5가지'), (select users_id from users where email = 'demo.runner@example.com'), '2024-06-04T15:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '20분 맨몸 서킷 루틴 (층간소음 걱정 없는 버전)'), (select users_id from users where email = 'demo.frontnote@example.com'), '2025-03-19T03:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '20분 맨몸 서킷 루틴 (층간소음 걱정 없는 버전)'), (select users_id from users where email = 'demo.halfday@example.com'), '2025-03-18T20:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '20분 맨몸 서킷 루틴 (층간소음 걱정 없는 버전)'), (select users_id from users where email = 'demo.runner@example.com'), '2025-03-18T12:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '헬스장 3개월 등록 후기: 홈트랑 뭐가 달랐나'), (select users_id from users where email = 'demo.bakery@example.com'), '2025-09-10T12:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '헬스장 3개월 등록 후기: 홈트랑 뭐가 달랐나'), (select users_id from users where email = 'demo.codecup@example.com'), '2025-09-09T16:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '헬스장 3개월 등록 후기: 홈트랑 뭐가 달랐나'), (select users_id from users where email = 'demo.homecook@example.com'), '2025-09-09T12:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '헬스장 3개월 등록 후기: 홈트랑 뭐가 달랐나'), (select users_id from users where email = 'demo.mukja@example.com'), '2025-09-07T13:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '헬스장 3개월 등록 후기: 홈트랑 뭐가 달랐나'), (select users_id from users where email = 'demo.runner@example.com'), '2025-09-07T17:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '헬스장 3개월 등록 후기: 홈트랑 뭐가 달랐나'), (select users_id from users where email = 'demo.sea@example.com'), '2025-09-07T21:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '문틀 턱걸이 바 달고 첫 턱걸이 성공하기까지'), (select users_id from users where email = 'demo.frontnote@example.com'), '2026-04-21T16:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '문틀 턱걸이 바 달고 첫 턱걸이 성공하기까지'), (select users_id from users where email = 'demo.plant@example.com'), '2026-04-20T19:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '문틀 턱걸이 바 달고 첫 턱걸이 성공하기까지'), (select users_id from users where email = 'demo.woodwork@example.com'), '2026-04-23T08:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '여름 휴가 중 호텔방에서 한 15분 루틴'), (select users_id from users where email = 'demo.bakery@example.com'), '2026-08-25T13:45:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '여름 휴가 중 호텔방에서 한 15분 루틴'), (select users_id from users where email = 'demo.halfday@example.com'), '2026-08-23T23:45:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '여름 휴가 중 호텔방에서 한 15분 루틴'), (select users_id from users where email = 'demo.runner@example.com'), '2026-08-25T22:45:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '여름 휴가 중 호텔방에서 한 15분 루틴'), (select users_id from users where email = 'demo.sea@example.com'), '2026-08-24T03:45:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '홈트 3년 차 회고: 그만두지 않을 수 있었던 이유'), (select users_id from users where email = 'demo.bakery@example.com'), '2026-10-09T09:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '홈트 3년 차 회고: 그만두지 않을 수 있었던 이유'), (select users_id from users where email = 'demo.codecup@example.com'), '2026-10-09T03:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '홈트 3년 차 회고: 그만두지 않을 수 있었던 이유'), (select users_id from users where email = 'demo.film@example.com'), '2026-10-06T15:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '홈트 3년 차 회고: 그만두지 않을 수 있었던 이유'), (select users_id from users where email = 'demo.halfday@example.com'), '2026-10-08T16:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '홈트 3년 차 회고: 그만두지 않을 수 있었던 이유'), (select users_id from users where email = 'demo.homecook@example.com'), '2026-10-09T04:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '홈트 3년 차 회고: 그만두지 않을 수 있었던 이유'), (select users_id from users where email = 'demo.mukja@example.com'), '2026-10-09T01:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '홈트 3년 차 회고: 그만두지 않을 수 있었던 이유'), (select users_id from users where email = 'demo.plant@example.com'), '2026-10-09T10:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '홈트 3년 차 회고: 그만두지 않을 수 있었던 이유'), (select users_id from users where email = 'demo.runner@example.com'), '2026-10-09T14:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '우리 동네 ''소문난손칼국수'', 겉절이가 다 한 집 솔직 후기'), (select users_id from users where email = 'demo.halfday@example.com'), '2023-06-21T15:35:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '우리 동네 ''소문난손칼국수'', 겉절이가 다 한 집 솔직 후기'), (select users_id from users where email = 'demo.homecook@example.com'), '2023-06-18T16:35:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '을지로 40년 된 연탄 돼지갈비 노포 ''원조연탄갈비'' 솔직 후기'), (select users_id from users where email = 'demo.codecup@example.com'), '2023-11-26T08:58:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '을지로 40년 된 연탄 돼지갈비 노포 ''원조연탄갈비'' 솔직 후기'), (select users_id from users where email = 'demo.film@example.com'), '2023-11-29T04:58:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '을지로 40년 된 연탄 돼지갈비 노포 ''원조연탄갈비'' 솔직 후기'), (select users_id from users where email = 'demo.sea@example.com'), '2023-11-28T06:58:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '망원동 작은 카페 ''오후세시'' 디저트 3종 비교 후기'), (select users_id from users where email = 'demo.bakery@example.com'), '2024-03-12T20:12:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '망원동 작은 카페 ''오후세시'' 디저트 3종 비교 후기'), (select users_id from users where email = 'demo.frontnote@example.com'), '2024-03-13T10:12:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '망원동 작은 카페 ''오후세시'' 디저트 3종 비교 후기'), (select users_id from users where email = 'demo.halfday@example.com'), '2024-03-13T13:12:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '망원동 작은 카페 ''오후세시'' 디저트 3종 비교 후기'), (select users_id from users where email = 'demo.homecook@example.com'), '2024-03-09T18:12:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '망원동 작은 카페 ''오후세시'' 디저트 3종 비교 후기'), (select users_id from users where email = 'demo.hometrain@example.com'), '2024-03-11T23:12:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '망원동 작은 카페 ''오후세시'' 디저트 3종 비교 후기'), (select users_id from users where email = 'demo.plant@example.com'), '2024-03-12T00:12:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '대전 중앙시장 순댓국 노포, 줄 서서 먹을 만한가'), (select users_id from users where email = 'demo.bakery@example.com'), '2024-09-22T15:47:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '대전 중앙시장 순댓국 노포, 줄 서서 먹을 만한가'), (select users_id from users where email = 'demo.film@example.com'), '2024-09-24T10:47:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '대전 중앙시장 순댓국 노포, 줄 서서 먹을 만한가'), (select users_id from users where email = 'demo.frontnote@example.com'), '2024-09-24T07:47:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '대전 중앙시장 순댓국 노포, 줄 서서 먹을 만한가'), (select users_id from users where email = 'demo.halfday@example.com'), '2024-09-22T18:47:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '대전 중앙시장 순댓국 노포, 줄 서서 먹을 만한가'), (select users_id from users where email = 'demo.homecook@example.com'), '2024-09-23T21:47:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '대전 중앙시장 순댓국 노포, 줄 서서 먹을 만한가'), (select users_id from users where email = 'demo.runner@example.com'), '2024-09-22T11:47:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '대전 중앙시장 순댓국 노포, 줄 서서 먹을 만한가'), (select users_id from users where email = 'demo.sea@example.com'), '2024-09-22T00:47:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '대전 중앙시장 순댓국 노포, 줄 서서 먹을 만한가'), (select users_id from users where email = 'demo.woodwork@example.com'), '2024-09-21T18:47:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '동네 중국집 짜장면 4곳 비교 정리 (배달 말고 매장)'), (select users_id from users where email = 'demo.bakery@example.com'), '2025-02-19T14:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '동네 중국집 짜장면 4곳 비교 정리 (배달 말고 매장)'), (select users_id from users where email = 'demo.codecup@example.com'), '2025-02-18T00:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '동네 중국집 짜장면 4곳 비교 정리 (배달 말고 매장)'), (select users_id from users where email = 'demo.homecook@example.com'), '2025-02-19T12:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '동네 중국집 짜장면 4곳 비교 정리 (배달 말고 매장)'), (select users_id from users where email = 'demo.hometrain@example.com'), '2025-02-19T04:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '동네 중국집 짜장면 4곳 비교 정리 (배달 말고 매장)'), (select users_id from users where email = 'demo.runner@example.com'), '2025-02-17T19:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '성수 웨이팅 카페 두 곳, 솔직히 한 곳만 추천해요'), (select users_id from users where email = 'demo.bakery@example.com'), '2025-07-02T03:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '성수 웨이팅 카페 두 곳, 솔직히 한 곳만 추천해요'), (select users_id from users where email = 'demo.film@example.com'), '2025-07-02T06:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '성수 웨이팅 카페 두 곳, 솔직히 한 곳만 추천해요'), (select users_id from users where email = 'demo.frontnote@example.com'), '2025-07-02T14:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '부산 국밥 노포 수육백반, 겨울이면 생각나는 그 집'), (select users_id from users where email = 'demo.halfday@example.com'), '2025-12-09T23:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '부산 국밥 노포 수육백반, 겨울이면 생각나는 그 집'), (select users_id from users where email = 'demo.homecook@example.com'), '2025-12-07T15:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '부산 국밥 노포 수육백반, 겨울이면 생각나는 그 집'), (select users_id from users where email = 'demo.hometrain@example.com'), '2025-12-08T19:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '부산 국밥 노포 수육백반, 겨울이면 생각나는 그 집'), (select users_id from users where email = 'demo.runner@example.com'), '2025-12-10T02:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '부산 국밥 노포 수육백반, 겨울이면 생각나는 그 집'), (select users_id from users where email = 'demo.sea@example.com'), '2025-12-10T17:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '전주 1박 하면서 먹은 것만 정리 (한옥마을 밖 위주)'), (select users_id from users where email = 'demo.frontnote@example.com'), '2026-04-19T04:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '전주 1박 하면서 먹은 것만 정리 (한옥마을 밖 위주)'), (select users_id from users where email = 'demo.halfday@example.com'), '2026-04-22T01:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '전주 1박 하면서 먹은 것만 정리 (한옥마을 밖 위주)'), (select users_id from users where email = 'demo.homecook@example.com'), '2026-04-19T12:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '전주 1박 하면서 먹은 것만 정리 (한옥마을 밖 위주)'), (select users_id from users where email = 'demo.hometrain@example.com'), '2026-04-20T20:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '전주 1박 하면서 먹은 것만 정리 (한옥마을 밖 위주)'), (select users_id from users where email = 'demo.sea@example.com'), '2026-04-21T02:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '전주 1박 하면서 먹은 것만 정리 (한옥마을 밖 위주)'), (select users_id from users where email = 'demo.woodwork@example.com'), '2026-04-18T22:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '회사 근처 백반집 한 달 출석 기록, 매일 바뀌는 반찬 정리'), (select users_id from users where email = 'demo.bakery@example.com'), '2026-09-21T08:55:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '회사 근처 백반집 한 달 출석 기록, 매일 바뀌는 반찬 정리'), (select users_id from users where email = 'demo.codecup@example.com'), '2026-09-20T22:55:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '회사 근처 백반집 한 달 출석 기록, 매일 바뀌는 반찬 정리'), (select users_id from users where email = 'demo.homecook@example.com'), '2026-09-21T03:55:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '회사 근처 백반집 한 달 출석 기록, 매일 바뀌는 반찬 정리'), (select users_id from users where email = 'demo.hometrain@example.com'), '2026-09-21T17:55:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '회사 근처 백반집 한 달 출석 기록, 매일 바뀌는 반찬 정리'), (select users_id from users where email = 'demo.plant@example.com'), '2026-09-20T11:55:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '회사 근처 백반집 한 달 출석 기록, 매일 바뀌는 반찬 정리'), (select users_id from users where email = 'demo.sea@example.com'), '2026-09-23T02:55:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '회사 근처 백반집 한 달 출석 기록, 매일 바뀌는 반찬 정리'), (select users_id from users where email = 'demo.woodwork@example.com'), '2026-09-21T14:55:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '몬스테라 첫 분갈이, 뿌리가 화분 밖으로 나왔어요'), (select users_id from users where email = 'demo.homecook@example.com'), '2023-05-22T16:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '몬스테라 첫 분갈이, 뿌리가 화분 밖으로 나왔어요'), (select users_id from users where email = 'demo.woodwork@example.com'), '2023-05-24T12:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '초보 식물집사가 첫 1년 동안 보낸 식물들'), (select users_id from users where email = 'demo.codecup@example.com'), '2023-09-03T19:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '초보 식물집사가 첫 1년 동안 보낸 식물들'), (select users_id from users where email = 'demo.film@example.com'), '2023-09-04T09:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '초보 식물집사가 첫 1년 동안 보낸 식물들'), (select users_id from users where email = 'demo.hometrain@example.com'), '2023-09-05T23:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '내가 정착한 흙 배합 비율 (상토 : 펄라이트 : 바크)'), (select users_id from users where email = 'demo.bakery@example.com'), '2024-04-14T17:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '내가 정착한 흙 배합 비율 (상토 : 펄라이트 : 바크)'), (select users_id from users where email = 'demo.homecook@example.com'), '2024-04-14T12:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '내가 정착한 흙 배합 비율 (상토 : 펄라이트 : 바크)'), (select users_id from users where email = 'demo.runner@example.com'), '2024-04-16T21:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '내가 정착한 흙 배합 비율 (상토 : 펄라이트 : 바크)'), (select users_id from users where email = 'demo.woodwork@example.com'), '2024-04-17T05:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '여름 베란다 과습 막는 물주기 루틴'), (select users_id from users where email = 'demo.bakery@example.com'), '2024-07-30T13:45:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '여름 베란다 과습 막는 물주기 루틴'), (select users_id from users where email = 'demo.film@example.com'), '2024-07-28T20:45:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '여름 베란다 과습 막는 물주기 루틴'), (select users_id from users where email = 'demo.frontnote@example.com'), '2024-07-28T04:45:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '여름 베란다 과습 막는 물주기 루틴'), (select users_id from users where email = 'demo.homecook@example.com'), '2024-07-29T13:45:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '여름 베란다 과습 막는 물주기 루틴'), (select users_id from users where email = 'demo.hometrain@example.com'), '2024-07-31T05:45:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '여름 베란다 과습 막는 물주기 루틴'), (select users_id from users where email = 'demo.runner@example.com'), '2024-07-29T02:45:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '여름 베란다 과습 막는 물주기 루틴'), (select users_id from users where email = 'demo.sea@example.com'), '2024-07-30T12:45:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '겨울 베란다 냉해 입은 알로카시아 살리기 기록'), (select users_id from users where email = 'demo.film@example.com'), '2025-01-19T13:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '겨울 베란다 냉해 입은 알로카시아 살리기 기록'), (select users_id from users where email = 'demo.sea@example.com'), '2025-01-20T17:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '겨울 베란다 냉해 입은 알로카시아 살리기 기록'), (select users_id from users where email = 'demo.woodwork@example.com'), '2025-01-22T06:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '스킨답서스 물꽂이 번식 3주 관찰 일기'), (select users_id from users where email = 'demo.bakery@example.com'), '2025-06-10T20:25:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '스킨답서스 물꽂이 번식 3주 관찰 일기'), (select users_id from users where email = 'demo.film@example.com'), '2025-06-07T22:25:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '스킨답서스 물꽂이 번식 3주 관찰 일기'), (select users_id from users where email = 'demo.frontnote@example.com'), '2025-06-09T16:25:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '스킨답서스 물꽂이 번식 3주 관찰 일기'), (select users_id from users where email = 'demo.halfday@example.com'), '2025-06-11T14:25:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '스킨답서스 물꽂이 번식 3주 관찰 일기'), (select users_id from users where email = 'demo.runner@example.com'), '2025-06-10T22:25:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '스킨답서스 물꽂이 번식 3주 관찰 일기'), (select users_id from users where email = 'demo.sea@example.com'), '2025-06-10T18:25:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '스킨답서스 물꽂이 번식 3주 관찰 일기'), (select users_id from users where email = 'demo.woodwork@example.com'), '2025-06-10T21:25:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '응애 퇴치 일지, 칼라데아와 2주 전쟁'), (select users_id from users where email = 'demo.codecup@example.com'), '2026-02-15T01:35:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '응애 퇴치 일지, 칼라데아와 2주 전쟁'), (select users_id from users where email = 'demo.frontnote@example.com'), '2026-02-15T19:35:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '응애 퇴치 일지, 칼라데아와 2주 전쟁'), (select users_id from users where email = 'demo.homecook@example.com'), '2026-02-18T02:35:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '응애 퇴치 일지, 칼라데아와 2주 전쟁'), (select users_id from users where email = 'demo.woodwork@example.com'), '2026-02-17T21:35:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '주말 화훼시장 나들이, 올리브나무를 데려왔어요'), (select users_id from users where email = 'demo.codecup@example.com'), '2026-06-02T08:50:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '주말 화훼시장 나들이, 올리브나무를 데려왔어요'), (select users_id from users where email = 'demo.film@example.com'), '2026-06-01T18:50:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '주말 화훼시장 나들이, 올리브나무를 데려왔어요'), (select users_id from users where email = 'demo.halfday@example.com'), '2026-05-31T20:50:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '주말 화훼시장 나들이, 올리브나무를 데려왔어요'), (select users_id from users where email = 'demo.mukja@example.com'), '2026-06-01T04:50:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '주말 화훼시장 나들이, 올리브나무를 데려왔어요'), (select users_id from users where email = 'demo.runner@example.com'), '2026-06-03T14:50:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '주말 화훼시장 나들이, 올리브나무를 데려왔어요'), (select users_id from users where email = 'demo.sea@example.com'), '2026-06-02T06:50:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '가을맞이 베란다 대정리와 분갈이 몰아치기'), (select users_id from users where email = 'demo.bakery@example.com'), '2026-09-27T16:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '가을맞이 베란다 대정리와 분갈이 몰아치기'), (select users_id from users where email = 'demo.film@example.com'), '2026-09-30T14:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '가을맞이 베란다 대정리와 분갈이 몰아치기'), (select users_id from users where email = 'demo.frontnote@example.com'), '2026-09-27T03:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '가을맞이 베란다 대정리와 분갈이 몰아치기'), (select users_id from users where email = 'demo.homecook@example.com'), '2026-09-30T04:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '가을맞이 베란다 대정리와 분갈이 몰아치기'), (select users_id from users where email = 'demo.hometrain@example.com'), '2026-09-29T03:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '가을맞이 베란다 대정리와 분갈이 몰아치기'), (select users_id from users where email = 'demo.mukja@example.com'), '2026-09-26T20:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '가을맞이 베란다 대정리와 분갈이 몰아치기'), (select users_id from users where email = 'demo.runner@example.com'), '2026-09-27T05:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '가을맞이 베란다 대정리와 분갈이 몰아치기'), (select users_id from users where email = 'demo.sea@example.com'), '2026-09-28T05:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '가을맞이 베란다 대정리와 분갈이 몰아치기'), (select users_id from users where email = 'demo.woodwork@example.com'), '2026-09-29T07:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '러닝 3주차, 처음으로 5km를 안 쉬고 뛰었다'), (select users_id from users where email = 'demo.halfday@example.com'), '2023-03-11T13:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '러닝 3주차, 처음으로 5km를 안 쉬고 뛰었다'), (select users_id from users where email = 'demo.homecook@example.com'), '2023-03-14T15:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '러닝 3주차, 처음으로 5km를 안 쉬고 뛰었다'), (select users_id from users where email = 'demo.hometrain@example.com'), '2023-03-15T07:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '러닝 3주차, 처음으로 5km를 안 쉬고 뛰었다'), (select users_id from users where email = 'demo.plant@example.com'), '2023-03-14T13:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '첫 10km 대회 후기, 58분 41초'), (select users_id from users where email = 'demo.frontnote@example.com'), '2023-11-02T12:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '첫 10km 대회 후기, 58분 41초'), (select users_id from users where email = 'demo.hometrain@example.com'), '2023-10-30T07:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '첫 10km 대회 후기, 58분 41초'), (select users_id from users where email = 'demo.mukja@example.com'), '2023-10-31T12:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '러닝화 세 켤레 돌려 신기, 400km 신어 본 정리'), (select users_id from users where email = 'demo.bakery@example.com'), '2024-04-08T09:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '러닝화 세 켤레 돌려 신기, 400km 신어 본 정리'), (select users_id from users where email = 'demo.codecup@example.com'), '2024-04-10T19:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '러닝화 세 켤레 돌려 신기, 400km 신어 본 정리'), (select users_id from users where email = 'demo.halfday@example.com'), '2024-04-09T15:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '러닝화 세 켤레 돌려 신기, 400km 신어 본 정리'), (select users_id from users where email = 'demo.hometrain@example.com'), '2024-04-10T01:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '러닝화 세 켤레 돌려 신기, 400km 신어 본 정리'), (select users_id from users where email = 'demo.sea@example.com'), '2024-04-08T11:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '러닝화 세 켤레 돌려 신기, 400km 신어 본 정리'), (select users_id from users where email = 'demo.woodwork@example.com'), '2024-04-10T07:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '정강이 통증(신스플린트)으로 3주를 쉬었다'), (select users_id from users where email = 'demo.codecup@example.com'), '2024-09-15T21:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '정강이 통증(신스플린트)으로 3주를 쉬었다'), (select users_id from users where email = 'demo.halfday@example.com'), '2024-09-18T01:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '정강이 통증(신스플린트)으로 3주를 쉬었다'), (select users_id from users where email = 'demo.homecook@example.com'), '2024-09-15T08:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '정강이 통증(신스플린트)으로 3주를 쉬었다'), (select users_id from users where email = 'demo.hometrain@example.com'), '2024-09-18T12:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '정강이 통증(신스플린트)으로 3주를 쉬었다'), (select users_id from users where email = 'demo.plant@example.com'), '2024-09-17T18:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '정강이 통증(신스플린트)으로 3주를 쉬었다'), (select users_id from users where email = 'demo.woodwork@example.com'), '2024-09-16T23:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '새벽 5시 반 기상 루틴과 존2 러닝 두 달'), (select users_id from users where email = 'demo.codecup@example.com'), '2025-03-24T08:45:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '새벽 5시 반 기상 루틴과 존2 러닝 두 달'), (select users_id from users where email = 'demo.film@example.com'), '2025-03-24T19:45:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '새벽 5시 반 기상 루틴과 존2 러닝 두 달'), (select users_id from users where email = 'demo.frontnote@example.com'), '2025-03-24T20:45:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '새벽 5시 반 기상 루틴과 존2 러닝 두 달'), (select users_id from users where email = 'demo.hometrain@example.com'), '2025-03-22T15:45:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '첫 하프마라톤 후기, 2시간 4분 37초'), (select users_id from users where email = 'demo.codecup@example.com'), '2025-11-10T03:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '첫 하프마라톤 후기, 2시간 4분 37초'), (select users_id from users where email = 'demo.frontnote@example.com'), '2025-11-10T15:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '첫 하프마라톤 후기, 2시간 4분 37초'), (select users_id from users where email = 'demo.halfday@example.com'), '2025-11-12T03:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '첫 하프마라톤 후기, 2시간 4분 37초'), (select users_id from users where email = 'demo.hometrain@example.com'), '2025-11-10T08:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '첫 하프마라톤 후기, 2시간 4분 37초'), (select users_id from users where email = 'demo.mukja@example.com'), '2025-11-11T21:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '첫 하프마라톤 후기, 2시간 4분 37초'), (select users_id from users where email = 'demo.plant@example.com'), '2025-11-09T22:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '첫 하프마라톤 후기, 2시간 4분 37초'), (select users_id from users where email = 'demo.sea@example.com'), '2025-11-13T07:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '첫 하프마라톤 후기, 2시간 4분 37초'), (select users_id from users where email = 'demo.woodwork@example.com'), '2025-11-11T19:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '하프 서브2를 위한 12주 훈련표 짜기'), (select users_id from users where email = 'demo.codecup@example.com'), '2026-04-22T08:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '하프 서브2를 위한 12주 훈련표 짜기'), (select users_id from users where email = 'demo.hometrain@example.com'), '2026-04-22T19:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '하프 서브2를 위한 12주 훈련표 짜기'), (select users_id from users where email = 'demo.sea@example.com'), '2026-04-22T16:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '여름 새벽 러닝 장비 정리, 모자부터 전해질까지'), (select users_id from users where email = 'demo.bakery@example.com'), '2026-07-28T04:05:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '여름 새벽 러닝 장비 정리, 모자부터 전해질까지'), (select users_id from users where email = 'demo.frontnote@example.com'), '2026-07-28T08:05:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '여름 새벽 러닝 장비 정리, 모자부터 전해질까지'), (select users_id from users where email = 'demo.hometrain@example.com'), '2026-07-27T21:05:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '여름 새벽 러닝 장비 정리, 모자부터 전해질까지'), (select users_id from users where email = 'demo.sea@example.com'), '2026-07-27T04:05:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '여름 새벽 러닝 장비 정리, 모자부터 전해질까지'), (select users_id from users where email = 'demo.woodwork@example.com'), '2026-07-26T16:05:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '대회 2주 앞두고 마지막 장거리 20km'), (select users_id from users where email = 'demo.bakery@example.com'), '2026-10-05T00:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '대회 2주 앞두고 마지막 장거리 20km'), (select users_id from users where email = 'demo.codecup@example.com'), '2026-10-07T01:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '대회 2주 앞두고 마지막 장거리 20km'), (select users_id from users where email = 'demo.film@example.com'), '2026-10-07T08:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '대회 2주 앞두고 마지막 장거리 20km'), (select users_id from users where email = 'demo.frontnote@example.com'), '2026-10-04T18:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '대회 2주 앞두고 마지막 장거리 20km'), (select users_id from users where email = 'demo.hometrain@example.com'), '2026-10-04T07:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '대회 2주 앞두고 마지막 장거리 20km'), (select users_id from users where email = 'demo.mukja@example.com'), '2026-10-06T05:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '대회 2주 앞두고 마지막 장거리 20km'), (select users_id from users where email = 'demo.plant@example.com'), '2026-10-04T19:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '대회 2주 앞두고 마지막 장거리 20km'), (select users_id from users where email = 'demo.sea@example.com'), '2026-10-04T12:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '주문진 새벽 수산시장 구경하고 소돌 아들바위까지 걸은 기록'), (select users_id from users where email = 'demo.bakery@example.com'), '2023-05-22T03:41:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '주문진 새벽 수산시장 구경하고 소돌 아들바위까지 걸은 기록'), (select users_id from users where email = 'demo.halfday@example.com'), '2023-05-22T02:41:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '주문진 새벽 수산시장 구경하고 소돌 아들바위까지 걸은 기록'), (select users_id from users where email = 'demo.mukja@example.com'), '2023-05-21T18:41:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '통영 1박 2일, 동피랑보다 좋았던 서피랑 골목'), (select users_id from users where email = 'demo.film@example.com'), '2023-09-20T19:05:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '통영 1박 2일, 동피랑보다 좋았던 서피랑 골목'), (select users_id from users where email = 'demo.halfday@example.com'), '2023-09-20T20:05:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '통영 1박 2일, 동피랑보다 좋았던 서피랑 골목'), (select users_id from users where email = 'demo.runner@example.com'), '2023-09-17T14:05:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '욕지도 첫 배 타고 들어가서 섬 한 바퀴 돈 날'), (select users_id from users where email = 'demo.halfday@example.com'), '2024-04-30T12:32:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '욕지도 첫 배 타고 들어가서 섬 한 바퀴 돈 날'), (select users_id from users where email = 'demo.mukja@example.com'), '2024-05-01T20:32:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '욕지도 첫 배 타고 들어가서 섬 한 바퀴 돈 날'), (select users_id from users where email = 'demo.runner@example.com'), '2024-05-01T16:32:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '삼척 장호항 스노클링 후기와 챙겨 가면 좋은 것'), (select users_id from users where email = 'demo.frontnote@example.com'), '2024-08-10T23:47:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '삼척 장호항 스노클링 후기와 챙겨 가면 좋은 것'), (select users_id from users where email = 'demo.halfday@example.com'), '2024-08-13T05:47:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '삼척 장호항 스노클링 후기와 챙겨 가면 좋은 것'), (select users_id from users where email = 'demo.hometrain@example.com'), '2024-08-11T22:47:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '삼척 장호항 스노클링 후기와 챙겨 가면 좋은 것'), (select users_id from users where email = 'demo.mukja@example.com'), '2024-08-11T02:47:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '삼척 장호항 스노클링 후기와 챙겨 가면 좋은 것'), (select users_id from users where email = 'demo.plant@example.com'), '2024-08-12T16:47:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '군산 근대거리 걷고 선유도까지, 서해 당일치기 코스 정리'), (select users_id from users where email = 'demo.bakery@example.com'), '2025-03-24T08:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '군산 근대거리 걷고 선유도까지, 서해 당일치기 코스 정리'), (select users_id from users where email = 'demo.film@example.com'), '2025-03-23T00:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '군산 근대거리 걷고 선유도까지, 서해 당일치기 코스 정리'), (select users_id from users where email = 'demo.halfday@example.com'), '2025-03-24T01:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '군산 근대거리 걷고 선유도까지, 서해 당일치기 코스 정리'), (select users_id from users where email = 'demo.homecook@example.com'), '2025-03-23T18:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '군산 근대거리 걷고 선유도까지, 서해 당일치기 코스 정리'), (select users_id from users where email = 'demo.mukja@example.com'), '2025-03-26T08:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '군산 근대거리 걷고 선유도까지, 서해 당일치기 코스 정리'), (select users_id from users where email = 'demo.plant@example.com'), '2025-03-24T02:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '속초 갈 때마다 들르는 동명항 근처 먹을 곳 기록'), (select users_id from users where email = 'demo.bakery@example.com'), '2025-07-07T10:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '속초 갈 때마다 들르는 동명항 근처 먹을 곳 기록'), (select users_id from users where email = 'demo.frontnote@example.com'), '2025-07-06T14:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '속초 갈 때마다 들르는 동명항 근처 먹을 곳 기록'), (select users_id from users where email = 'demo.halfday@example.com'), '2025-07-08T04:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '속초 갈 때마다 들르는 동명항 근처 먹을 곳 기록'), (select users_id from users where email = 'demo.homecook@example.com'), '2025-07-07T18:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '속초 갈 때마다 들르는 동명항 근처 먹을 곳 기록'), (select users_id from users where email = 'demo.mukja@example.com'), '2025-07-09T19:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '겨울 바다 혼자 보러 간 남해 다랭이마을 일기'), (select users_id from users where email = 'demo.film@example.com'), '2025-11-19T14:02:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '겨울 바다 혼자 보러 간 남해 다랭이마을 일기'), (select users_id from users where email = 'demo.frontnote@example.com'), '2025-11-18T05:02:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '겨울 바다 혼자 보러 간 남해 다랭이마을 일기'), (select users_id from users where email = 'demo.halfday@example.com'), '2025-11-17T06:02:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '겨울 바다 혼자 보러 간 남해 다랭이마을 일기'), (select users_id from users where email = 'demo.mukja@example.com'), '2025-11-17T21:02:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '겨울 바다 혼자 보러 간 남해 다랭이마을 일기'), (select users_id from users where email = 'demo.plant@example.com'), '2025-11-18T01:02:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '겨울 바다 혼자 보러 간 남해 다랭이마을 일기'), (select users_id from users where email = 'demo.woodwork@example.com'), '2025-11-17T21:02:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '가파도 청보리 끝물에 다녀온 기록과 배 예약 팁'), (select users_id from users where email = 'demo.codecup@example.com'), '2026-06-01T05:38:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '가파도 청보리 끝물에 다녀온 기록과 배 예약 팁'), (select users_id from users where email = 'demo.film@example.com'), '2026-06-02T17:38:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '가파도 청보리 끝물에 다녀온 기록과 배 예약 팁'), (select users_id from users where email = 'demo.halfday@example.com'), '2026-06-02T02:38:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '가파도 청보리 끝물에 다녀온 기록과 배 예약 팁'), (select users_id from users where email = 'demo.runner@example.com'), '2026-05-31T00:38:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '추석 연휴 끝에 다녀온 고성 화진포, 조용한 가을 바다'), (select users_id from users where email = 'demo.halfday@example.com'), '2026-09-30T13:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '추석 연휴 끝에 다녀온 고성 화진포, 조용한 가을 바다'), (select users_id from users where email = 'demo.hometrain@example.com'), '2026-10-01T13:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '추석 연휴 끝에 다녀온 고성 화진포, 조용한 가을 바다'), (select users_id from users where email = 'demo.mukja@example.com'), '2026-09-29T11:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '추석 연휴 끝에 다녀온 고성 화진포, 조용한 가을 바다'), (select users_id from users where email = 'demo.plant@example.com'), '2026-10-01T00:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '추석 연휴 끝에 다녀온 고성 화진포, 조용한 가을 바다'), (select users_id from users where email = 'demo.woodwork@example.com'), '2026-10-01T06:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '공방 첫날, 편백 도마 하나 만들고 손바닥에 물집 두 개'), (select users_id from users where email = 'demo.bakery@example.com'), '2023-04-16T05:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '공방 첫날, 편백 도마 하나 만들고 손바닥에 물집 두 개'), (select users_id from users where email = 'demo.homecook@example.com'), '2023-04-19T05:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '월넛 트레이 만들기, 오일 마감 세 번의 차이'), (select users_id from users where email = 'demo.bakery@example.com'), '2023-08-27T15:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '월넛 트레이 만들기, 오일 마감 세 번의 차이'), (select users_id from users where email = 'demo.codecup@example.com'), '2023-08-30T12:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '월넛 트레이 만들기, 오일 마감 세 번의 차이'), (select users_id from users where email = 'demo.film@example.com'), '2023-08-26T21:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '월넛 트레이 만들기, 오일 마감 세 번의 차이'), (select users_id from users where email = 'demo.frontnote@example.com'), '2023-08-27T06:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '월넛 트레이 만들기, 오일 마감 세 번의 차이'), (select users_id from users where email = 'demo.mukja@example.com'), '2023-08-29T04:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '월넛 트레이 만들기, 오일 마감 세 번의 차이'), (select users_id from users where email = 'demo.plant@example.com'), '2023-08-28T08:10:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '끌 연마 입문, 숫돌 세 개로 거울면 만들기'), (select users_id from users where email = 'demo.bakery@example.com'), '2024-01-23T17:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '끌 연마 입문, 숫돌 세 개로 거울면 만들기'), (select users_id from users where email = 'demo.frontnote@example.com'), '2024-01-24T08:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '끌 연마 입문, 숫돌 세 개로 거울면 만들기'), (select users_id from users where email = 'demo.homecook@example.com'), '2024-01-21T16:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '끌 연마 입문, 숫돌 세 개로 거울면 만들기'), (select users_id from users where email = 'demo.hometrain@example.com'), '2024-01-22T09:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '끌 연마 입문, 숫돌 세 개로 거울면 만들기'), (select users_id from users where email = 'demo.mukja@example.com'), '2024-01-21T02:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '끌 연마 입문, 숫돌 세 개로 거울면 만들기'), (select users_id from users where email = 'demo.runner@example.com'), '2024-01-20T21:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '첫 가구, 장부맞춤 원목 스툴 만든 3주의 기록'), (select users_id from users where email = 'demo.bakery@example.com'), '2024-06-19T14:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '첫 가구, 장부맞춤 원목 스툴 만든 3주의 기록'), (select users_id from users where email = 'demo.frontnote@example.com'), '2024-06-17T14:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '첫 가구, 장부맞춤 원목 스툴 만든 3주의 기록'), (select users_id from users where email = 'demo.halfday@example.com'), '2024-06-16T06:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '첫 가구, 장부맞춤 원목 스툴 만든 3주의 기록'), (select users_id from users where email = 'demo.homecook@example.com'), '2024-06-16T10:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '첫 가구, 장부맞춤 원목 스툴 만든 3주의 기록'), (select users_id from users where email = 'demo.hometrain@example.com'), '2024-06-16T16:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '첫 가구, 장부맞춤 원목 스툴 만든 3주의 기록'), (select users_id from users where email = 'demo.mukja@example.com'), '2024-06-17T19:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '첫 가구, 장부맞춤 원목 스툴 만든 3주의 기록'), (select users_id from users where email = 'demo.sea@example.com'), '2024-06-16T04:00:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '트리머 1년 사용기, 처음 살 때 알았으면 좋았을 것'), (select users_id from users where email = 'demo.codecup@example.com'), '2025-01-12T02:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '트리머 1년 사용기, 처음 살 때 알았으면 좋았을 것'), (select users_id from users where email = 'demo.frontnote@example.com'), '2025-01-14T13:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '트리머 1년 사용기, 처음 살 때 알았으면 좋았을 것'), (select users_id from users where email = 'demo.halfday@example.com'), '2025-01-15T03:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '트리머 1년 사용기, 처음 살 때 알았으면 좋았을 것'), (select users_id from users where email = 'demo.homecook@example.com'), '2025-01-12T01:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '트리머 1년 사용기, 처음 살 때 알았으면 좋았을 것'), (select users_id from users where email = 'demo.runner@example.com'), '2025-01-13T13:30:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '서랍 하나 달린 협탁, 서랍 레일 없이 나무로만'), (select users_id from users where email = 'demo.codecup@example.com'), '2025-05-26T04:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '서랍 하나 달린 협탁, 서랍 레일 없이 나무로만'), (select users_id from users where email = 'demo.film@example.com'), '2025-05-27T17:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '서랍 하나 달린 협탁, 서랍 레일 없이 나무로만'), (select users_id from users where email = 'demo.frontnote@example.com'), '2025-05-27T01:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '서랍 하나 달린 협탁, 서랍 레일 없이 나무로만'), (select users_id from users where email = 'demo.halfday@example.com'), '2025-05-27T05:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '서랍 하나 달린 협탁, 서랍 레일 없이 나무로만'), (select users_id from users where email = 'demo.plant@example.com'), '2025-05-26T01:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '서랍 하나 달린 협탁, 서랍 레일 없이 나무로만'), (select users_id from users where email = 'demo.sea@example.com'), '2025-05-26T17:20:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '양평 목재소 다녀온 날, 판재 고르는 법 배운 기록'), (select users_id from users where email = 'demo.frontnote@example.com'), '2025-11-11T06:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '양평 목재소 다녀온 날, 판재 고르는 법 배운 기록'), (select users_id from users where email = 'demo.homecook@example.com'), '2025-11-11T18:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '양평 목재소 다녀온 날, 판재 고르는 법 배운 기록'), (select users_id from users where email = 'demo.hometrain@example.com'), '2025-11-11T20:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '양평 목재소 다녀온 날, 판재 고르는 법 배운 기록'), (select users_id from users where email = 'demo.mukja@example.com'), '2025-11-10T01:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '양평 목재소 다녀온 날, 판재 고르는 법 배운 기록'), (select users_id from users where email = 'demo.plant@example.com'), '2025-11-12T12:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '양평 목재소 다녀온 날, 판재 고르는 법 배운 기록'), (select users_id from users where email = 'demo.sea@example.com'), '2025-11-10T05:40:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '체리 판재로 만든 4단 책장, 반년 걸린 이유'), (select users_id from users where email = 'demo.codecup@example.com'), '2026-03-25T20:50:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '체리 판재로 만든 4단 책장, 반년 걸린 이유'), (select users_id from users where email = 'demo.film@example.com'), '2026-03-24T08:50:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '체리 판재로 만든 4단 책장, 반년 걸린 이유'), (select users_id from users where email = 'demo.halfday@example.com'), '2026-03-23T09:50:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '체리 판재로 만든 4단 책장, 반년 걸린 이유'), (select users_id from users where email = 'demo.homecook@example.com'), '2026-03-22T08:50:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '체리 판재로 만든 4단 책장, 반년 걸린 이유'), (select users_id from users where email = 'demo.hometrain@example.com'), '2026-03-22T08:50:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '체리 판재로 만든 4단 책장, 반년 걸린 이유'), (select users_id from users where email = 'demo.plant@example.com'), '2026-03-22T03:50:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '체리 판재로 만든 4단 책장, 반년 걸린 이유'), (select users_id from users where email = 'demo.sea@example.com'), '2026-03-25T16:50:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '캠핑용 접이식 테이블, 주말 4번에 끝낸 제작기'), (select users_id from users where email = 'demo.bakery@example.com'), '2026-09-29T04:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '캠핑용 접이식 테이블, 주말 4번에 끝낸 제작기'), (select users_id from users where email = 'demo.film@example.com'), '2026-09-28T04:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '캠핑용 접이식 테이블, 주말 4번에 끝낸 제작기'), (select users_id from users where email = 'demo.halfday@example.com'), '2026-09-27T22:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '캠핑용 접이식 테이블, 주말 4번에 끝낸 제작기'), (select users_id from users where email = 'demo.hometrain@example.com'), '2026-09-27T15:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '캠핑용 접이식 테이블, 주말 4번에 끝낸 제작기'), (select users_id from users where email = 'demo.mukja@example.com'), '2026-09-30T02:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '캠핑용 접이식 테이블, 주말 4번에 끝낸 제작기'), (select users_id from users where email = 'demo.plant@example.com'), '2026-09-27T04:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '캠핑용 접이식 테이블, 주말 4번에 끝낸 제작기'), (select users_id from users where email = 'demo.runner@example.com'), '2026-09-28T14:15:00+09:00');
insert into post_like (post_id, users_id, created_at) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '캠핑용 접이식 테이블, 주말 4번에 끝낸 제작기'), (select users_id from users where email = 'demo.sea@example.com'), '2026-09-27T23:15:00+09:00');

-- 최근 7일 글 조회수 (실행하는 날 기준, 한국 날짜)
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '커넥션 풀 고갈 장애 회고: HikariCP 로그를 따라간 길'), (now() at time zone 'Asia/Seoul')::date - 0, 89);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '커넥션 풀 고갈 장애 회고: HikariCP 로그를 따라간 길'), (now() at time zone 'Asia/Seoul')::date - 1, 37);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '커넥션 풀 고갈 장애 회고: HikariCP 로그를 따라간 길'), (now() at time zone 'Asia/Seoul')::date - 2, 32);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '커넥션 풀 고갈 장애 회고: HikariCP 로그를 따라간 길'), (now() at time zone 'Asia/Seoul')::date - 3, 77);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '커넥션 풀 고갈 장애 회고: HikariCP 로그를 따라간 길'), (now() at time zone 'Asia/Seoul')::date - 4, 82);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '커넥션 풀 고갈 장애 회고: HikariCP 로그를 따라간 길'), (now() at time zone 'Asia/Seoul')::date - 5, 69);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '커넥션 풀 고갈 장애 회고: HikariCP 로그를 따라간 길'), (now() at time zone 'Asia/Seoul')::date - 6, 18);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '추석 끝나고 남은 전으로 끓인 전찌개, 연휴 뒤 첫 저녁'), (now() at time zone 'Asia/Seoul')::date - 0, 89);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '추석 끝나고 남은 전으로 끓인 전찌개, 연휴 뒤 첫 저녁'), (now() at time zone 'Asia/Seoul')::date - 1, 35);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '추석 끝나고 남은 전으로 끓인 전찌개, 연휴 뒤 첫 저녁'), (now() at time zone 'Asia/Seoul')::date - 2, 90);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '추석 끝나고 남은 전으로 끓인 전찌개, 연휴 뒤 첫 저녁'), (now() at time zone 'Asia/Seoul')::date - 3, 21);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '추석 끝나고 남은 전으로 끓인 전찌개, 연휴 뒤 첫 저녁'), (now() at time zone 'Asia/Seoul')::date - 4, 86);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '추석 끝나고 남은 전으로 끓인 전찌개, 연휴 뒤 첫 저녁'), (now() at time zone 'Asia/Seoul')::date - 5, 25);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '추석 끝나고 남은 전으로 끓인 전찌개, 연휴 뒤 첫 저녁'), (now() at time zone 'Asia/Seoul')::date - 6, 25);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '대회 2주 앞두고 마지막 장거리 20km'), (now() at time zone 'Asia/Seoul')::date - 0, 72);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '대회 2주 앞두고 마지막 장거리 20km'), (now() at time zone 'Asia/Seoul')::date - 1, 108);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '대회 2주 앞두고 마지막 장거리 20km'), (now() at time zone 'Asia/Seoul')::date - 2, 70);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '대회 2주 앞두고 마지막 장거리 20km'), (now() at time zone 'Asia/Seoul')::date - 3, 42);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '대회 2주 앞두고 마지막 장거리 20km'), (now() at time zone 'Asia/Seoul')::date - 4, 75);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '대회 2주 앞두고 마지막 장거리 20km'), (now() at time zone 'Asia/Seoul')::date - 5, 43);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '대회 2주 앞두고 마지막 장거리 20km'), (now() at time zone 'Asia/Seoul')::date - 6, 72);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '가평 자라섬 가을 반나절, ITX 타고 다녀온 일기'), (now() at time zone 'Asia/Seoul')::date - 0, 109);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '가평 자라섬 가을 반나절, ITX 타고 다녀온 일기'), (now() at time zone 'Asia/Seoul')::date - 1, 105);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '가평 자라섬 가을 반나절, ITX 타고 다녀온 일기'), (now() at time zone 'Asia/Seoul')::date - 2, 73);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '가평 자라섬 가을 반나절, ITX 타고 다녀온 일기'), (now() at time zone 'Asia/Seoul')::date - 3, 58);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '가평 자라섬 가을 반나절, ITX 타고 다녀온 일기'), (now() at time zone 'Asia/Seoul')::date - 4, 62);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '가평 자라섬 가을 반나절, ITX 타고 다녀온 일기'), (now() at time zone 'Asia/Seoul')::date - 5, 33);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.halfday@example.com' and p.title = '가평 자라섬 가을 반나절, ITX 타고 다녀온 일기'), (now() at time zone 'Asia/Seoul')::date - 6, 54);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'React 19의 폼 액션과 useActionState, 실제로 써 본 정리'), (now() at time zone 'Asia/Seoul')::date - 0, 74);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'React 19의 폼 액션과 useActionState, 실제로 써 본 정리'), (now() at time zone 'Asia/Seoul')::date - 1, 54);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'React 19의 폼 액션과 useActionState, 실제로 써 본 정리'), (now() at time zone 'Asia/Seoul')::date - 2, 88);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'React 19의 폼 액션과 useActionState, 실제로 써 본 정리'), (now() at time zone 'Asia/Seoul')::date - 3, 39);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'React 19의 폼 액션과 useActionState, 실제로 써 본 정리'), (now() at time zone 'Asia/Seoul')::date - 4, 43);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'React 19의 폼 액션과 useActionState, 실제로 써 본 정리'), (now() at time zone 'Asia/Seoul')::date - 5, 19);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.frontnote@example.com' and p.title = 'React 19의 폼 액션과 useActionState, 실제로 써 본 정리'), (now() at time zone 'Asia/Seoul')::date - 6, 28);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '캠핑용 접이식 테이블, 주말 4번에 끝낸 제작기'), (now() at time zone 'Asia/Seoul')::date - 0, 40);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '캠핑용 접이식 테이블, 주말 4번에 끝낸 제작기'), (now() at time zone 'Asia/Seoul')::date - 1, 86);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '캠핑용 접이식 테이블, 주말 4번에 끝낸 제작기'), (now() at time zone 'Asia/Seoul')::date - 2, 55);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '캠핑용 접이식 테이블, 주말 4번에 끝낸 제작기'), (now() at time zone 'Asia/Seoul')::date - 3, 18);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '캠핑용 접이식 테이블, 주말 4번에 끝낸 제작기'), (now() at time zone 'Asia/Seoul')::date - 4, 45);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '캠핑용 접이식 테이블, 주말 4번에 끝낸 제작기'), (now() at time zone 'Asia/Seoul')::date - 5, 41);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '캠핑용 접이식 테이블, 주말 4번에 끝낸 제작기'), (now() at time zone 'Asia/Seoul')::date - 6, 50);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '홈트 3년 차 회고: 그만두지 않을 수 있었던 이유'), (now() at time zone 'Asia/Seoul')::date - 0, 56);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '홈트 3년 차 회고: 그만두지 않을 수 있었던 이유'), (now() at time zone 'Asia/Seoul')::date - 1, 43);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '홈트 3년 차 회고: 그만두지 않을 수 있었던 이유'), (now() at time zone 'Asia/Seoul')::date - 2, 15);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '홈트 3년 차 회고: 그만두지 않을 수 있었던 이유'), (now() at time zone 'Asia/Seoul')::date - 3, 27);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '홈트 3년 차 회고: 그만두지 않을 수 있었던 이유'), (now() at time zone 'Asia/Seoul')::date - 4, 28);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '홈트 3년 차 회고: 그만두지 않을 수 있었던 이유'), (now() at time zone 'Asia/Seoul')::date - 5, 44);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.hometrain@example.com' and p.title = '홈트 3년 차 회고: 그만두지 않을 수 있었던 이유'), (now() at time zone 'Asia/Seoul')::date - 6, 29);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '베이킹 3년 차, 망친 것들 모아 보니 보이는 것'), (now() at time zone 'Asia/Seoul')::date - 0, 30);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '베이킹 3년 차, 망친 것들 모아 보니 보이는 것'), (now() at time zone 'Asia/Seoul')::date - 1, 37);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '베이킹 3년 차, 망친 것들 모아 보니 보이는 것'), (now() at time zone 'Asia/Seoul')::date - 2, 11);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '베이킹 3년 차, 망친 것들 모아 보니 보이는 것'), (now() at time zone 'Asia/Seoul')::date - 3, 24);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '베이킹 3년 차, 망친 것들 모아 보니 보이는 것'), (now() at time zone 'Asia/Seoul')::date - 4, 40);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '베이킹 3년 차, 망친 것들 모아 보니 보이는 것'), (now() at time zone 'Asia/Seoul')::date - 5, 14);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.bakery@example.com' and p.title = '베이킹 3년 차, 망친 것들 모아 보니 보이는 것'), (now() at time zone 'Asia/Seoul')::date - 6, 17);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '가을맞이 베란다 대정리와 분갈이 몰아치기'), (now() at time zone 'Asia/Seoul')::date - 0, 17);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '가을맞이 베란다 대정리와 분갈이 몰아치기'), (now() at time zone 'Asia/Seoul')::date - 1, 28);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '가을맞이 베란다 대정리와 분갈이 몰아치기'), (now() at time zone 'Asia/Seoul')::date - 2, 37);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '가을맞이 베란다 대정리와 분갈이 몰아치기'), (now() at time zone 'Asia/Seoul')::date - 3, 19);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '가을맞이 베란다 대정리와 분갈이 몰아치기'), (now() at time zone 'Asia/Seoul')::date - 4, 26);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '가을맞이 베란다 대정리와 분갈이 몰아치기'), (now() at time zone 'Asia/Seoul')::date - 5, 9);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '가을맞이 베란다 대정리와 분갈이 몰아치기'), (now() at time zone 'Asia/Seoul')::date - 6, 18);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '회사 근처 백반집 한 달 출석 기록, 매일 바뀌는 반찬 정리'), (now() at time zone 'Asia/Seoul')::date - 0, 20);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '회사 근처 백반집 한 달 출석 기록, 매일 바뀌는 반찬 정리'), (now() at time zone 'Asia/Seoul')::date - 1, 43);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '회사 근처 백반집 한 달 출석 기록, 매일 바뀌는 반찬 정리'), (now() at time zone 'Asia/Seoul')::date - 2, 49);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '회사 근처 백반집 한 달 출석 기록, 매일 바뀌는 반찬 정리'), (now() at time zone 'Asia/Seoul')::date - 3, 44);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '회사 근처 백반집 한 달 출석 기록, 매일 바뀌는 반찬 정리'), (now() at time zone 'Asia/Seoul')::date - 4, 10);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '회사 근처 백반집 한 달 출석 기록, 매일 바뀌는 반찬 정리'), (now() at time zone 'Asia/Seoul')::date - 5, 31);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '회사 근처 백반집 한 달 출석 기록, 매일 바뀌는 반찬 정리'), (now() at time zone 'Asia/Seoul')::date - 6, 19);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '추석 연휴 끝에 다녀온 고성 화진포, 조용한 가을 바다'), (now() at time zone 'Asia/Seoul')::date - 0, 40);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '추석 연휴 끝에 다녀온 고성 화진포, 조용한 가을 바다'), (now() at time zone 'Asia/Seoul')::date - 1, 43);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '추석 연휴 끝에 다녀온 고성 화진포, 조용한 가을 바다'), (now() at time zone 'Asia/Seoul')::date - 2, 16);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '추석 연휴 끝에 다녀온 고성 화진포, 조용한 가을 바다'), (now() at time zone 'Asia/Seoul')::date - 3, 40);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '추석 연휴 끝에 다녀온 고성 화진포, 조용한 가을 바다'), (now() at time zone 'Asia/Seoul')::date - 4, 30);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '추석 연휴 끝에 다녀온 고성 화진포, 조용한 가을 바다'), (now() at time zone 'Asia/Seoul')::date - 5, 40);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '추석 연휴 끝에 다녀온 고성 화진포, 조용한 가을 바다'), (now() at time zone 'Asia/Seoul')::date - 6, 12);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '부산 영도 흰여울마을, 바다 냄새 나는 필름 산책'), (now() at time zone 'Asia/Seoul')::date - 0, 25);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '부산 영도 흰여울마을, 바다 냄새 나는 필름 산책'), (now() at time zone 'Asia/Seoul')::date - 1, 45);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '부산 영도 흰여울마을, 바다 냄새 나는 필름 산책'), (now() at time zone 'Asia/Seoul')::date - 2, 54);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '부산 영도 흰여울마을, 바다 냄새 나는 필름 산책'), (now() at time zone 'Asia/Seoul')::date - 3, 23);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '부산 영도 흰여울마을, 바다 냄새 나는 필름 산책'), (now() at time zone 'Asia/Seoul')::date - 4, 9);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '부산 영도 흰여울마을, 바다 냄새 나는 필름 산책'), (now() at time zone 'Asia/Seoul')::date - 5, 9);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '부산 영도 흰여울마을, 바다 냄새 나는 필름 산책'), (now() at time zone 'Asia/Seoul')::date - 6, 10);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '@Transactional이 안 먹던 이유: 같은 클래스 안에서 부르면 프록시를 안 탄다'), (now() at time zone 'Asia/Seoul')::date - 0, 35);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '@Transactional이 안 먹던 이유: 같은 클래스 안에서 부르면 프록시를 안 탄다'), (now() at time zone 'Asia/Seoul')::date - 1, 25);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '@Transactional이 안 먹던 이유: 같은 클래스 안에서 부르면 프록시를 안 탄다'), (now() at time zone 'Asia/Seoul')::date - 2, 32);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '@Transactional이 안 먹던 이유: 같은 클래스 안에서 부르면 프록시를 안 탄다'), (now() at time zone 'Asia/Seoul')::date - 3, 15);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '@Transactional이 안 먹던 이유: 같은 클래스 안에서 부르면 프록시를 안 탄다'), (now() at time zone 'Asia/Seoul')::date - 4, 18);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '@Transactional이 안 먹던 이유: 같은 클래스 안에서 부르면 프록시를 안 탄다'), (now() at time zone 'Asia/Seoul')::date - 5, 18);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = '@Transactional이 안 먹던 이유: 같은 클래스 안에서 부르면 프록시를 안 탄다'), (now() at time zone 'Asia/Seoul')::date - 6, 36);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'JPA N+1 문제, fetch join이랑 batch size로 잡은 기록'), (now() at time zone 'Asia/Seoul')::date - 0, 59);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'JPA N+1 문제, fetch join이랑 batch size로 잡은 기록'), (now() at time zone 'Asia/Seoul')::date - 1, 37);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'JPA N+1 문제, fetch join이랑 batch size로 잡은 기록'), (now() at time zone 'Asia/Seoul')::date - 2, 46);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'JPA N+1 문제, fetch join이랑 batch size로 잡은 기록'), (now() at time zone 'Asia/Seoul')::date - 3, 23);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'JPA N+1 문제, fetch join이랑 batch size로 잡은 기록'), (now() at time zone 'Asia/Seoul')::date - 4, 13);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'JPA N+1 문제, fetch join이랑 batch size로 잡은 기록'), (now() at time zone 'Asia/Seoul')::date - 5, 36);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.codecup@example.com' and p.title = 'JPA N+1 문제, fetch join이랑 batch size로 잡은 기록'), (now() at time zone 'Asia/Seoul')::date - 6, 29);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '맞벌이 부부의 평일 집밥, 일요일 두 시간으로 시작해요'), (now() at time zone 'Asia/Seoul')::date - 0, 12);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '맞벌이 부부의 평일 집밥, 일요일 두 시간으로 시작해요'), (now() at time zone 'Asia/Seoul')::date - 1, 18);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '맞벌이 부부의 평일 집밥, 일요일 두 시간으로 시작해요'), (now() at time zone 'Asia/Seoul')::date - 2, 18);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '맞벌이 부부의 평일 집밥, 일요일 두 시간으로 시작해요'), (now() at time zone 'Asia/Seoul')::date - 3, 46);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '맞벌이 부부의 평일 집밥, 일요일 두 시간으로 시작해요'), (now() at time zone 'Asia/Seoul')::date - 4, 46);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '맞벌이 부부의 평일 집밥, 일요일 두 시간으로 시작해요'), (now() at time zone 'Asia/Seoul')::date - 5, 28);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '맞벌이 부부의 평일 집밥, 일요일 두 시간으로 시작해요'), (now() at time zone 'Asia/Seoul')::date - 6, 29);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '러닝화 세 켤레 돌려 신기, 400km 신어 본 정리'), (now() at time zone 'Asia/Seoul')::date - 0, 12);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '러닝화 세 켤레 돌려 신기, 400km 신어 본 정리'), (now() at time zone 'Asia/Seoul')::date - 1, 40);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '러닝화 세 켤레 돌려 신기, 400km 신어 본 정리'), (now() at time zone 'Asia/Seoul')::date - 2, 50);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '러닝화 세 켤레 돌려 신기, 400km 신어 본 정리'), (now() at time zone 'Asia/Seoul')::date - 3, 27);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '러닝화 세 켤레 돌려 신기, 400km 신어 본 정리'), (now() at time zone 'Asia/Seoul')::date - 4, 32);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '러닝화 세 켤레 돌려 신기, 400km 신어 본 정리'), (now() at time zone 'Asia/Seoul')::date - 5, 17);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.runner@example.com' and p.title = '러닝화 세 켤레 돌려 신기, 400km 신어 본 정리'), (now() at time zone 'Asia/Seoul')::date - 6, 15);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '대전 중앙시장 순댓국 노포, 줄 서서 먹을 만한가'), (now() at time zone 'Asia/Seoul')::date - 0, 24);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '대전 중앙시장 순댓국 노포, 줄 서서 먹을 만한가'), (now() at time zone 'Asia/Seoul')::date - 1, 30);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '대전 중앙시장 순댓국 노포, 줄 서서 먹을 만한가'), (now() at time zone 'Asia/Seoul')::date - 2, 36);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '대전 중앙시장 순댓국 노포, 줄 서서 먹을 만한가'), (now() at time zone 'Asia/Seoul')::date - 3, 22);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '대전 중앙시장 순댓국 노포, 줄 서서 먹을 만한가'), (now() at time zone 'Asia/Seoul')::date - 4, 48);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '대전 중앙시장 순댓국 노포, 줄 서서 먹을 만한가'), (now() at time zone 'Asia/Seoul')::date - 5, 13);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.mukja@example.com' and p.title = '대전 중앙시장 순댓국 노포, 줄 서서 먹을 만한가'), (now() at time zone 'Asia/Seoul')::date - 6, 20);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '공방 첫날, 편백 도마 하나 만들고 손바닥에 물집 두 개'), (now() at time zone 'Asia/Seoul')::date - 0, 54);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '공방 첫날, 편백 도마 하나 만들고 손바닥에 물집 두 개'), (now() at time zone 'Asia/Seoul')::date - 1, 58);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '공방 첫날, 편백 도마 하나 만들고 손바닥에 물집 두 개'), (now() at time zone 'Asia/Seoul')::date - 2, 39);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '공방 첫날, 편백 도마 하나 만들고 손바닥에 물집 두 개'), (now() at time zone 'Asia/Seoul')::date - 3, 16);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '공방 첫날, 편백 도마 하나 만들고 손바닥에 물집 두 개'), (now() at time zone 'Asia/Seoul')::date - 4, 30);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '공방 첫날, 편백 도마 하나 만들고 손바닥에 물집 두 개'), (now() at time zone 'Asia/Seoul')::date - 5, 16);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '공방 첫날, 편백 도마 하나 만들고 손바닥에 물집 두 개'), (now() at time zone 'Asia/Seoul')::date - 6, 14);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '여름 베란다 과습 막는 물주기 루틴'), (now() at time zone 'Asia/Seoul')::date - 0, 43);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '여름 베란다 과습 막는 물주기 루틴'), (now() at time zone 'Asia/Seoul')::date - 1, 43);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '여름 베란다 과습 막는 물주기 루틴'), (now() at time zone 'Asia/Seoul')::date - 2, 47);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '여름 베란다 과습 막는 물주기 루틴'), (now() at time zone 'Asia/Seoul')::date - 3, 32);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '여름 베란다 과습 막는 물주기 루틴'), (now() at time zone 'Asia/Seoul')::date - 4, 27);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '여름 베란다 과습 막는 물주기 루틴'), (now() at time zone 'Asia/Seoul')::date - 5, 39);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.plant@example.com' and p.title = '여름 베란다 과습 막는 물주기 루틴'), (now() at time zone 'Asia/Seoul')::date - 6, 27);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '가을 서촌 골목 출사 기록'), (now() at time zone 'Asia/Seoul')::date - 0, 60);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '가을 서촌 골목 출사 기록'), (now() at time zone 'Asia/Seoul')::date - 1, 13);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '가을 서촌 골목 출사 기록'), (now() at time zone 'Asia/Seoul')::date - 2, 23);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '가을 서촌 골목 출사 기록'), (now() at time zone 'Asia/Seoul')::date - 3, 45);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '가을 서촌 골목 출사 기록'), (now() at time zone 'Asia/Seoul')::date - 4, 40);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '가을 서촌 골목 출사 기록'), (now() at time zone 'Asia/Seoul')::date - 5, 17);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '가을 서촌 골목 출사 기록'), (now() at time zone 'Asia/Seoul')::date - 6, 27);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '욕지도 첫 배 타고 들어가서 섬 한 바퀴 돈 날'), (now() at time zone 'Asia/Seoul')::date - 0, 30);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '욕지도 첫 배 타고 들어가서 섬 한 바퀴 돈 날'), (now() at time zone 'Asia/Seoul')::date - 1, 33);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '욕지도 첫 배 타고 들어가서 섬 한 바퀴 돈 날'), (now() at time zone 'Asia/Seoul')::date - 2, 39);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '욕지도 첫 배 타고 들어가서 섬 한 바퀴 돈 날'), (now() at time zone 'Asia/Seoul')::date - 3, 11);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '욕지도 첫 배 타고 들어가서 섬 한 바퀴 돈 날'), (now() at time zone 'Asia/Seoul')::date - 4, 22);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '욕지도 첫 배 타고 들어가서 섬 한 바퀴 돈 날'), (now() at time zone 'Asia/Seoul')::date - 5, 20);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.sea@example.com' and p.title = '욕지도 첫 배 타고 들어가서 섬 한 바퀴 돈 날'), (now() at time zone 'Asia/Seoul')::date - 6, 40);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '월넛 트레이 만들기, 오일 마감 세 번의 차이'), (now() at time zone 'Asia/Seoul')::date - 0, 62);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '월넛 트레이 만들기, 오일 마감 세 번의 차이'), (now() at time zone 'Asia/Seoul')::date - 1, 36);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '월넛 트레이 만들기, 오일 마감 세 번의 차이'), (now() at time zone 'Asia/Seoul')::date - 2, 33);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '월넛 트레이 만들기, 오일 마감 세 번의 차이'), (now() at time zone 'Asia/Seoul')::date - 3, 15);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '월넛 트레이 만들기, 오일 마감 세 번의 차이'), (now() at time zone 'Asia/Seoul')::date - 4, 32);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '월넛 트레이 만들기, 오일 마감 세 번의 차이'), (now() at time zone 'Asia/Seoul')::date - 5, 31);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.woodwork@example.com' and p.title = '월넛 트레이 만들기, 오일 마감 세 번의 차이'), (now() at time zone 'Asia/Seoul')::date - 6, 36);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉장고 파먹기 1탄: 자투리 채소 볶음밥이랑 계란국'), (now() at time zone 'Asia/Seoul')::date - 0, 59);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉장고 파먹기 1탄: 자투리 채소 볶음밥이랑 계란국'), (now() at time zone 'Asia/Seoul')::date - 1, 40);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉장고 파먹기 1탄: 자투리 채소 볶음밥이랑 계란국'), (now() at time zone 'Asia/Seoul')::date - 2, 47);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉장고 파먹기 1탄: 자투리 채소 볶음밥이랑 계란국'), (now() at time zone 'Asia/Seoul')::date - 3, 23);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉장고 파먹기 1탄: 자투리 채소 볶음밥이랑 계란국'), (now() at time zone 'Asia/Seoul')::date - 4, 37);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉장고 파먹기 1탄: 자투리 채소 볶음밥이랑 계란국'), (now() at time zone 'Asia/Seoul')::date - 5, 18);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.homecook@example.com' and p.title = '냉장고 파먹기 1탄: 자투리 채소 볶음밥이랑 계란국'), (now() at time zone 'Asia/Seoul')::date - 6, 31);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '첫 필름카메라, 중고 수동 SLR을 들이다'), (now() at time zone 'Asia/Seoul')::date - 0, 54);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '첫 필름카메라, 중고 수동 SLR을 들이다'), (now() at time zone 'Asia/Seoul')::date - 1, 42);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '첫 필름카메라, 중고 수동 SLR을 들이다'), (now() at time zone 'Asia/Seoul')::date - 2, 15);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '첫 필름카메라, 중고 수동 SLR을 들이다'), (now() at time zone 'Asia/Seoul')::date - 3, 48);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '첫 필름카메라, 중고 수동 SLR을 들이다'), (now() at time zone 'Asia/Seoul')::date - 4, 33);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '첫 필름카메라, 중고 수동 SLR을 들이다'), (now() at time zone 'Asia/Seoul')::date - 5, 38);
insert into post_daily_stat (post_id, stat_date, views) values ((select p.post_id from post p join category c on c.category_id = p.category_id join blog b on b.blog_id = c.blog_id join users u on u.users_id = b.users_id where u.email = 'demo.film@example.com' and p.title = '첫 필름카메라, 중고 수동 SLR을 들이다'), (now() at time zone 'Asia/Seoul')::date - 6, 30);

-- 블로그 일별 조회·방문 (최근 30일)
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), (now() at time zone 'Asia/Seoul')::date - 0, 22, 14);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), (now() at time zone 'Asia/Seoul')::date - 1, 74, 54);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), (now() at time zone 'Asia/Seoul')::date - 2, 79, 40);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), (now() at time zone 'Asia/Seoul')::date - 3, 79, 52);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), (now() at time zone 'Asia/Seoul')::date - 4, 83, 47);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), (now() at time zone 'Asia/Seoul')::date - 5, 72, 42);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), (now() at time zone 'Asia/Seoul')::date - 6, 70, 42);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), (now() at time zone 'Asia/Seoul')::date - 7, 73, 40);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), (now() at time zone 'Asia/Seoul')::date - 8, 65, 47);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), (now() at time zone 'Asia/Seoul')::date - 9, 42, 26);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), (now() at time zone 'Asia/Seoul')::date - 10, 65, 48);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), (now() at time zone 'Asia/Seoul')::date - 11, 62, 45);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), (now() at time zone 'Asia/Seoul')::date - 12, 41, 28);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), (now() at time zone 'Asia/Seoul')::date - 13, 115, 61);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), (now() at time zone 'Asia/Seoul')::date - 14, 49, 37);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), (now() at time zone 'Asia/Seoul')::date - 15, 68, 48);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), (now() at time zone 'Asia/Seoul')::date - 16, 56, 42);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), (now() at time zone 'Asia/Seoul')::date - 17, 72, 37);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), (now() at time zone 'Asia/Seoul')::date - 18, 67, 35);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), (now() at time zone 'Asia/Seoul')::date - 19, 76, 43);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), (now() at time zone 'Asia/Seoul')::date - 20, 64, 41);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), (now() at time zone 'Asia/Seoul')::date - 21, 46, 27);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), (now() at time zone 'Asia/Seoul')::date - 22, 72, 44);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), (now() at time zone 'Asia/Seoul')::date - 23, 24, 17);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), (now() at time zone 'Asia/Seoul')::date - 24, 90, 53);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), (now() at time zone 'Asia/Seoul')::date - 25, 57, 39);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), (now() at time zone 'Asia/Seoul')::date - 26, 36, 27);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), (now() at time zone 'Asia/Seoul')::date - 27, 59, 43);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), (now() at time zone 'Asia/Seoul')::date - 28, 73, 51);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.bakery@example.com')), (now() at time zone 'Asia/Seoul')::date - 29, 72, 43);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), (now() at time zone 'Asia/Seoul')::date - 0, 46, 26);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), (now() at time zone 'Asia/Seoul')::date - 1, 75, 46);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), (now() at time zone 'Asia/Seoul')::date - 2, 117, 65);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), (now() at time zone 'Asia/Seoul')::date - 3, 72, 45);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), (now() at time zone 'Asia/Seoul')::date - 4, 70, 46);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), (now() at time zone 'Asia/Seoul')::date - 5, 65, 43);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), (now() at time zone 'Asia/Seoul')::date - 6, 91, 62);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), (now() at time zone 'Asia/Seoul')::date - 7, 83, 42);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), (now() at time zone 'Asia/Seoul')::date - 8, 75, 41);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), (now() at time zone 'Asia/Seoul')::date - 9, 96, 51);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), (now() at time zone 'Asia/Seoul')::date - 10, 98, 50);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), (now() at time zone 'Asia/Seoul')::date - 11, 84, 51);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), (now() at time zone 'Asia/Seoul')::date - 12, 83, 49);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), (now() at time zone 'Asia/Seoul')::date - 13, 90, 53);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), (now() at time zone 'Asia/Seoul')::date - 14, 88, 63);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), (now() at time zone 'Asia/Seoul')::date - 15, 101, 65);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), (now() at time zone 'Asia/Seoul')::date - 16, 51, 28);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), (now() at time zone 'Asia/Seoul')::date - 17, 94, 49);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), (now() at time zone 'Asia/Seoul')::date - 18, 105, 64);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), (now() at time zone 'Asia/Seoul')::date - 19, 60, 38);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), (now() at time zone 'Asia/Seoul')::date - 20, 126, 67);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), (now() at time zone 'Asia/Seoul')::date - 21, 72, 39);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), (now() at time zone 'Asia/Seoul')::date - 22, 66, 36);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), (now() at time zone 'Asia/Seoul')::date - 23, 75, 42);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), (now() at time zone 'Asia/Seoul')::date - 24, 94, 48);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), (now() at time zone 'Asia/Seoul')::date - 25, 111, 63);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), (now() at time zone 'Asia/Seoul')::date - 26, 91, 54);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), (now() at time zone 'Asia/Seoul')::date - 27, 92, 46);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), (now() at time zone 'Asia/Seoul')::date - 28, 85, 56);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.codecup@example.com')), (now() at time zone 'Asia/Seoul')::date - 29, 51, 34);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), (now() at time zone 'Asia/Seoul')::date - 0, 23, 16);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), (now() at time zone 'Asia/Seoul')::date - 1, 41, 23);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), (now() at time zone 'Asia/Seoul')::date - 2, 78, 54);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), (now() at time zone 'Asia/Seoul')::date - 3, 72, 36);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), (now() at time zone 'Asia/Seoul')::date - 4, 95, 50);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), (now() at time zone 'Asia/Seoul')::date - 5, 84, 50);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), (now() at time zone 'Asia/Seoul')::date - 6, 99, 53);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), (now() at time zone 'Asia/Seoul')::date - 7, 47, 29);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), (now() at time zone 'Asia/Seoul')::date - 8, 42, 30);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), (now() at time zone 'Asia/Seoul')::date - 9, 62, 45);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), (now() at time zone 'Asia/Seoul')::date - 10, 72, 50);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), (now() at time zone 'Asia/Seoul')::date - 11, 102, 53);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), (now() at time zone 'Asia/Seoul')::date - 12, 56, 41);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), (now() at time zone 'Asia/Seoul')::date - 13, 49, 27);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), (now() at time zone 'Asia/Seoul')::date - 14, 39, 23);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), (now() at time zone 'Asia/Seoul')::date - 15, 68, 46);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), (now() at time zone 'Asia/Seoul')::date - 16, 70, 44);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), (now() at time zone 'Asia/Seoul')::date - 17, 47, 31);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), (now() at time zone 'Asia/Seoul')::date - 18, 65, 40);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), (now() at time zone 'Asia/Seoul')::date - 19, 88, 61);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), (now() at time zone 'Asia/Seoul')::date - 20, 93, 50);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), (now() at time zone 'Asia/Seoul')::date - 21, 47, 33);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), (now() at time zone 'Asia/Seoul')::date - 22, 75, 49);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), (now() at time zone 'Asia/Seoul')::date - 23, 81, 52);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), (now() at time zone 'Asia/Seoul')::date - 24, 82, 52);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), (now() at time zone 'Asia/Seoul')::date - 25, 101, 54);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), (now() at time zone 'Asia/Seoul')::date - 26, 54, 27);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), (now() at time zone 'Asia/Seoul')::date - 27, 53, 27);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), (now() at time zone 'Asia/Seoul')::date - 28, 95, 53);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.film@example.com')), (now() at time zone 'Asia/Seoul')::date - 29, 33, 19);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), (now() at time zone 'Asia/Seoul')::date - 0, 111, 82);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), (now() at time zone 'Asia/Seoul')::date - 1, 119, 79);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), (now() at time zone 'Asia/Seoul')::date - 2, 101, 63);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), (now() at time zone 'Asia/Seoul')::date - 3, 122, 66);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), (now() at time zone 'Asia/Seoul')::date - 4, 107, 71);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), (now() at time zone 'Asia/Seoul')::date - 5, 205, 111);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), (now() at time zone 'Asia/Seoul')::date - 6, 213, 109);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), (now() at time zone 'Asia/Seoul')::date - 7, 107, 70);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), (now() at time zone 'Asia/Seoul')::date - 8, 115, 77);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), (now() at time zone 'Asia/Seoul')::date - 9, 90, 62);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), (now() at time zone 'Asia/Seoul')::date - 10, 125, 65);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), (now() at time zone 'Asia/Seoul')::date - 11, 133, 96);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), (now() at time zone 'Asia/Seoul')::date - 12, 114, 83);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), (now() at time zone 'Asia/Seoul')::date - 13, 164, 88);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), (now() at time zone 'Asia/Seoul')::date - 14, 154, 97);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), (now() at time zone 'Asia/Seoul')::date - 15, 124, 92);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), (now() at time zone 'Asia/Seoul')::date - 16, 170, 91);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), (now() at time zone 'Asia/Seoul')::date - 17, 114, 68);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), (now() at time zone 'Asia/Seoul')::date - 18, 122, 91);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), (now() at time zone 'Asia/Seoul')::date - 19, 167, 105);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), (now() at time zone 'Asia/Seoul')::date - 20, 142, 80);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), (now() at time zone 'Asia/Seoul')::date - 21, 147, 101);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), (now() at time zone 'Asia/Seoul')::date - 22, 154, 91);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), (now() at time zone 'Asia/Seoul')::date - 23, 162, 88);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), (now() at time zone 'Asia/Seoul')::date - 24, 139, 101);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), (now() at time zone 'Asia/Seoul')::date - 25, 190, 100);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), (now() at time zone 'Asia/Seoul')::date - 26, 166, 109);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), (now() at time zone 'Asia/Seoul')::date - 27, 138, 80);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), (now() at time zone 'Asia/Seoul')::date - 28, 134, 97);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.frontnote@example.com')), (now() at time zone 'Asia/Seoul')::date - 29, 131, 82);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), (now() at time zone 'Asia/Seoul')::date - 0, 73, 48);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), (now() at time zone 'Asia/Seoul')::date - 1, 72, 38);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), (now() at time zone 'Asia/Seoul')::date - 2, 76, 56);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), (now() at time zone 'Asia/Seoul')::date - 3, 48, 27);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), (now() at time zone 'Asia/Seoul')::date - 4, 48, 24);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), (now() at time zone 'Asia/Seoul')::date - 5, 90, 51);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), (now() at time zone 'Asia/Seoul')::date - 6, 80, 47);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), (now() at time zone 'Asia/Seoul')::date - 7, 56, 35);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), (now() at time zone 'Asia/Seoul')::date - 8, 23, 17);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), (now() at time zone 'Asia/Seoul')::date - 9, 35, 21);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), (now() at time zone 'Asia/Seoul')::date - 10, 42, 26);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), (now() at time zone 'Asia/Seoul')::date - 11, 57, 29);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), (now() at time zone 'Asia/Seoul')::date - 12, 91, 68);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), (now() at time zone 'Asia/Seoul')::date - 13, 101, 56);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), (now() at time zone 'Asia/Seoul')::date - 14, 31, 20);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), (now() at time zone 'Asia/Seoul')::date - 15, 66, 48);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), (now() at time zone 'Asia/Seoul')::date - 16, 54, 30);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), (now() at time zone 'Asia/Seoul')::date - 17, 66, 42);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), (now() at time zone 'Asia/Seoul')::date - 18, 49, 25);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), (now() at time zone 'Asia/Seoul')::date - 19, 74, 47);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), (now() at time zone 'Asia/Seoul')::date - 20, 83, 46);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), (now() at time zone 'Asia/Seoul')::date - 21, 26, 20);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), (now() at time zone 'Asia/Seoul')::date - 22, 54, 40);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), (now() at time zone 'Asia/Seoul')::date - 23, 29, 19);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), (now() at time zone 'Asia/Seoul')::date - 24, 79, 49);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), (now() at time zone 'Asia/Seoul')::date - 25, 79, 49);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), (now() at time zone 'Asia/Seoul')::date - 26, 68, 37);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), (now() at time zone 'Asia/Seoul')::date - 27, 90, 54);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), (now() at time zone 'Asia/Seoul')::date - 28, 78, 51);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.halfday@example.com')), (now() at time zone 'Asia/Seoul')::date - 29, 52, 37);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), (now() at time zone 'Asia/Seoul')::date - 0, 59, 40);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), (now() at time zone 'Asia/Seoul')::date - 1, 30, 20);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), (now() at time zone 'Asia/Seoul')::date - 2, 105, 56);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), (now() at time zone 'Asia/Seoul')::date - 3, 42, 29);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), (now() at time zone 'Asia/Seoul')::date - 4, 106, 58);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), (now() at time zone 'Asia/Seoul')::date - 5, 89, 54);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), (now() at time zone 'Asia/Seoul')::date - 6, 75, 50);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), (now() at time zone 'Asia/Seoul')::date - 7, 35, 21);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), (now() at time zone 'Asia/Seoul')::date - 8, 68, 51);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), (now() at time zone 'Asia/Seoul')::date - 9, 96, 48);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), (now() at time zone 'Asia/Seoul')::date - 10, 113, 60);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), (now() at time zone 'Asia/Seoul')::date - 11, 44, 23);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), (now() at time zone 'Asia/Seoul')::date - 12, 83, 48);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), (now() at time zone 'Asia/Seoul')::date - 13, 58, 42);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), (now() at time zone 'Asia/Seoul')::date - 14, 52, 35);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), (now() at time zone 'Asia/Seoul')::date - 15, 77, 41);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), (now() at time zone 'Asia/Seoul')::date - 16, 52, 29);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), (now() at time zone 'Asia/Seoul')::date - 17, 83, 57);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), (now() at time zone 'Asia/Seoul')::date - 18, 42, 31);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), (now() at time zone 'Asia/Seoul')::date - 19, 65, 45);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), (now() at time zone 'Asia/Seoul')::date - 20, 95, 63);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), (now() at time zone 'Asia/Seoul')::date - 21, 53, 39);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), (now() at time zone 'Asia/Seoul')::date - 22, 49, 37);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), (now() at time zone 'Asia/Seoul')::date - 23, 55, 33);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), (now() at time zone 'Asia/Seoul')::date - 24, 90, 57);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), (now() at time zone 'Asia/Seoul')::date - 25, 77, 52);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), (now() at time zone 'Asia/Seoul')::date - 26, 101, 58);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), (now() at time zone 'Asia/Seoul')::date - 27, 46, 33);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), (now() at time zone 'Asia/Seoul')::date - 28, 48, 26);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.homecook@example.com')), (now() at time zone 'Asia/Seoul')::date - 29, 53, 32);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), (now() at time zone 'Asia/Seoul')::date - 0, 32, 16);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), (now() at time zone 'Asia/Seoul')::date - 1, 89, 45);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), (now() at time zone 'Asia/Seoul')::date - 2, 28, 21);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), (now() at time zone 'Asia/Seoul')::date - 3, 17, 11);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), (now() at time zone 'Asia/Seoul')::date - 4, 22, 16);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), (now() at time zone 'Asia/Seoul')::date - 5, 71, 36);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), (now() at time zone 'Asia/Seoul')::date - 6, 106, 56);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), (now() at time zone 'Asia/Seoul')::date - 7, 37, 27);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), (now() at time zone 'Asia/Seoul')::date - 8, 85, 44);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), (now() at time zone 'Asia/Seoul')::date - 9, 58, 41);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), (now() at time zone 'Asia/Seoul')::date - 10, 11, 8);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), (now() at time zone 'Asia/Seoul')::date - 11, 37, 26);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), (now() at time zone 'Asia/Seoul')::date - 12, 35, 21);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), (now() at time zone 'Asia/Seoul')::date - 13, 92, 50);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), (now() at time zone 'Asia/Seoul')::date - 14, 45, 25);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), (now() at time zone 'Asia/Seoul')::date - 15, 59, 31);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), (now() at time zone 'Asia/Seoul')::date - 16, 77, 43);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), (now() at time zone 'Asia/Seoul')::date - 17, 61, 35);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), (now() at time zone 'Asia/Seoul')::date - 18, 45, 24);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), (now() at time zone 'Asia/Seoul')::date - 19, 51, 33);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), (now() at time zone 'Asia/Seoul')::date - 20, 85, 57);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), (now() at time zone 'Asia/Seoul')::date - 21, 60, 42);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), (now() at time zone 'Asia/Seoul')::date - 22, 75, 40);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), (now() at time zone 'Asia/Seoul')::date - 23, 60, 42);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), (now() at time zone 'Asia/Seoul')::date - 24, 43, 28);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), (now() at time zone 'Asia/Seoul')::date - 25, 17, 10);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), (now() at time zone 'Asia/Seoul')::date - 26, 106, 60);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), (now() at time zone 'Asia/Seoul')::date - 27, 41, 22);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), (now() at time zone 'Asia/Seoul')::date - 28, 33, 22);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.hometrain@example.com')), (now() at time zone 'Asia/Seoul')::date - 29, 19, 12);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), (now() at time zone 'Asia/Seoul')::date - 0, 58, 41);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), (now() at time zone 'Asia/Seoul')::date - 1, 87, 62);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), (now() at time zone 'Asia/Seoul')::date - 2, 49, 32);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), (now() at time zone 'Asia/Seoul')::date - 3, 58, 42);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), (now() at time zone 'Asia/Seoul')::date - 4, 99, 56);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), (now() at time zone 'Asia/Seoul')::date - 5, 105, 60);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), (now() at time zone 'Asia/Seoul')::date - 6, 109, 75);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), (now() at time zone 'Asia/Seoul')::date - 7, 99, 61);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), (now() at time zone 'Asia/Seoul')::date - 8, 62, 42);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), (now() at time zone 'Asia/Seoul')::date - 9, 89, 51);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), (now() at time zone 'Asia/Seoul')::date - 10, 88, 54);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), (now() at time zone 'Asia/Seoul')::date - 11, 80, 45);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), (now() at time zone 'Asia/Seoul')::date - 12, 117, 63);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), (now() at time zone 'Asia/Seoul')::date - 13, 132, 70);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), (now() at time zone 'Asia/Seoul')::date - 14, 98, 61);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), (now() at time zone 'Asia/Seoul')::date - 15, 83, 61);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), (now() at time zone 'Asia/Seoul')::date - 16, 74, 48);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), (now() at time zone 'Asia/Seoul')::date - 17, 110, 59);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), (now() at time zone 'Asia/Seoul')::date - 18, 40, 25);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), (now() at time zone 'Asia/Seoul')::date - 19, 123, 70);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), (now() at time zone 'Asia/Seoul')::date - 20, 54, 39);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), (now() at time zone 'Asia/Seoul')::date - 21, 89, 56);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), (now() at time zone 'Asia/Seoul')::date - 22, 90, 48);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), (now() at time zone 'Asia/Seoul')::date - 23, 59, 33);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), (now() at time zone 'Asia/Seoul')::date - 24, 49, 34);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), (now() at time zone 'Asia/Seoul')::date - 25, 57, 33);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), (now() at time zone 'Asia/Seoul')::date - 26, 66, 40);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), (now() at time zone 'Asia/Seoul')::date - 27, 113, 65);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), (now() at time zone 'Asia/Seoul')::date - 28, 53, 30);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.mukja@example.com')), (now() at time zone 'Asia/Seoul')::date - 29, 84, 58);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), (now() at time zone 'Asia/Seoul')::date - 0, 31, 21);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), (now() at time zone 'Asia/Seoul')::date - 1, 76, 54);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), (now() at time zone 'Asia/Seoul')::date - 2, 73, 39);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), (now() at time zone 'Asia/Seoul')::date - 3, 81, 49);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), (now() at time zone 'Asia/Seoul')::date - 4, 76, 52);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), (now() at time zone 'Asia/Seoul')::date - 5, 98, 59);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), (now() at time zone 'Asia/Seoul')::date - 6, 90, 47);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), (now() at time zone 'Asia/Seoul')::date - 7, 41, 23);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), (now() at time zone 'Asia/Seoul')::date - 8, 60, 41);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), (now() at time zone 'Asia/Seoul')::date - 9, 33, 18);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), (now() at time zone 'Asia/Seoul')::date - 10, 35, 20);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), (now() at time zone 'Asia/Seoul')::date - 11, 58, 43);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), (now() at time zone 'Asia/Seoul')::date - 12, 91, 63);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), (now() at time zone 'Asia/Seoul')::date - 13, 54, 28);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), (now() at time zone 'Asia/Seoul')::date - 14, 57, 40);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), (now() at time zone 'Asia/Seoul')::date - 15, 54, 38);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), (now() at time zone 'Asia/Seoul')::date - 16, 68, 48);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), (now() at time zone 'Asia/Seoul')::date - 17, 94, 53);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), (now() at time zone 'Asia/Seoul')::date - 18, 64, 37);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), (now() at time zone 'Asia/Seoul')::date - 19, 87, 60);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), (now() at time zone 'Asia/Seoul')::date - 20, 110, 55);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), (now() at time zone 'Asia/Seoul')::date - 21, 102, 52);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), (now() at time zone 'Asia/Seoul')::date - 22, 30, 23);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), (now() at time zone 'Asia/Seoul')::date - 23, 77, 49);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), (now() at time zone 'Asia/Seoul')::date - 24, 35, 23);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), (now() at time zone 'Asia/Seoul')::date - 25, 35, 20);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), (now() at time zone 'Asia/Seoul')::date - 26, 79, 53);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), (now() at time zone 'Asia/Seoul')::date - 27, 97, 54);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), (now() at time zone 'Asia/Seoul')::date - 28, 53, 30);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.plant@example.com')), (now() at time zone 'Asia/Seoul')::date - 29, 57, 42);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), (now() at time zone 'Asia/Seoul')::date - 0, 131, 71);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), (now() at time zone 'Asia/Seoul')::date - 1, 107, 72);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), (now() at time zone 'Asia/Seoul')::date - 2, 128, 79);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), (now() at time zone 'Asia/Seoul')::date - 3, 87, 51);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), (now() at time zone 'Asia/Seoul')::date - 4, 163, 85);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), (now() at time zone 'Asia/Seoul')::date - 5, 122, 65);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), (now() at time zone 'Asia/Seoul')::date - 6, 88, 63);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), (now() at time zone 'Asia/Seoul')::date - 7, 77, 51);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), (now() at time zone 'Asia/Seoul')::date - 8, 168, 88);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), (now() at time zone 'Asia/Seoul')::date - 9, 113, 64);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), (now() at time zone 'Asia/Seoul')::date - 10, 141, 79);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), (now() at time zone 'Asia/Seoul')::date - 11, 132, 77);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), (now() at time zone 'Asia/Seoul')::date - 12, 159, 100);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), (now() at time zone 'Asia/Seoul')::date - 13, 154, 80);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), (now() at time zone 'Asia/Seoul')::date - 14, 133, 84);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), (now() at time zone 'Asia/Seoul')::date - 15, 96, 53);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), (now() at time zone 'Asia/Seoul')::date - 16, 91, 54);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), (now() at time zone 'Asia/Seoul')::date - 17, 131, 67);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), (now() at time zone 'Asia/Seoul')::date - 18, 97, 70);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), (now() at time zone 'Asia/Seoul')::date - 19, 174, 97);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), (now() at time zone 'Asia/Seoul')::date - 20, 161, 82);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), (now() at time zone 'Asia/Seoul')::date - 21, 139, 73);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), (now() at time zone 'Asia/Seoul')::date - 22, 92, 67);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), (now() at time zone 'Asia/Seoul')::date - 23, 85, 54);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), (now() at time zone 'Asia/Seoul')::date - 24, 73, 53);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), (now() at time zone 'Asia/Seoul')::date - 25, 137, 76);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), (now() at time zone 'Asia/Seoul')::date - 26, 110, 66);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), (now() at time zone 'Asia/Seoul')::date - 27, 128, 78);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), (now() at time zone 'Asia/Seoul')::date - 28, 155, 88);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.runner@example.com')), (now() at time zone 'Asia/Seoul')::date - 29, 138, 75);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), (now() at time zone 'Asia/Seoul')::date - 0, 101, 62);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), (now() at time zone 'Asia/Seoul')::date - 1, 111, 61);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), (now() at time zone 'Asia/Seoul')::date - 2, 88, 60);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), (now() at time zone 'Asia/Seoul')::date - 3, 156, 85);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), (now() at time zone 'Asia/Seoul')::date - 4, 78, 52);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), (now() at time zone 'Asia/Seoul')::date - 5, 128, 94);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), (now() at time zone 'Asia/Seoul')::date - 6, 110, 64);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), (now() at time zone 'Asia/Seoul')::date - 7, 89, 59);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), (now() at time zone 'Asia/Seoul')::date - 8, 118, 68);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), (now() at time zone 'Asia/Seoul')::date - 9, 113, 74);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), (now() at time zone 'Asia/Seoul')::date - 10, 95, 55);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), (now() at time zone 'Asia/Seoul')::date - 11, 104, 55);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), (now() at time zone 'Asia/Seoul')::date - 12, 109, 80);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), (now() at time zone 'Asia/Seoul')::date - 13, 195, 101);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), (now() at time zone 'Asia/Seoul')::date - 14, 98, 63);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), (now() at time zone 'Asia/Seoul')::date - 15, 84, 51);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), (now() at time zone 'Asia/Seoul')::date - 16, 94, 51);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), (now() at time zone 'Asia/Seoul')::date - 17, 92, 65);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), (now() at time zone 'Asia/Seoul')::date - 18, 121, 66);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), (now() at time zone 'Asia/Seoul')::date - 19, 143, 78);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), (now() at time zone 'Asia/Seoul')::date - 20, 121, 64);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), (now() at time zone 'Asia/Seoul')::date - 21, 125, 83);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), (now() at time zone 'Asia/Seoul')::date - 22, 84, 60);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), (now() at time zone 'Asia/Seoul')::date - 23, 119, 84);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), (now() at time zone 'Asia/Seoul')::date - 24, 92, 51);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), (now() at time zone 'Asia/Seoul')::date - 25, 129, 84);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), (now() at time zone 'Asia/Seoul')::date - 26, 151, 79);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), (now() at time zone 'Asia/Seoul')::date - 27, 125, 80);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), (now() at time zone 'Asia/Seoul')::date - 28, 83, 52);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.sea@example.com')), (now() at time zone 'Asia/Seoul')::date - 29, 121, 68);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), (now() at time zone 'Asia/Seoul')::date - 0, 66, 43);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), (now() at time zone 'Asia/Seoul')::date - 1, 54, 38);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), (now() at time zone 'Asia/Seoul')::date - 2, 58, 36);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), (now() at time zone 'Asia/Seoul')::date - 3, 24, 16);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), (now() at time zone 'Asia/Seoul')::date - 4, 54, 33);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), (now() at time zone 'Asia/Seoul')::date - 5, 70, 45);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), (now() at time zone 'Asia/Seoul')::date - 6, 75, 54);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), (now() at time zone 'Asia/Seoul')::date - 7, 38, 23);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), (now() at time zone 'Asia/Seoul')::date - 8, 22, 17);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), (now() at time zone 'Asia/Seoul')::date - 9, 87, 47);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), (now() at time zone 'Asia/Seoul')::date - 10, 38, 20);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), (now() at time zone 'Asia/Seoul')::date - 11, 21, 12);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), (now() at time zone 'Asia/Seoul')::date - 12, 45, 28);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), (now() at time zone 'Asia/Seoul')::date - 13, 87, 50);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), (now() at time zone 'Asia/Seoul')::date - 14, 59, 44);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), (now() at time zone 'Asia/Seoul')::date - 15, 28, 19);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), (now() at time zone 'Asia/Seoul')::date - 16, 32, 19);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), (now() at time zone 'Asia/Seoul')::date - 17, 44, 23);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), (now() at time zone 'Asia/Seoul')::date - 18, 21, 12);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), (now() at time zone 'Asia/Seoul')::date - 19, 55, 33);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), (now() at time zone 'Asia/Seoul')::date - 20, 60, 43);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), (now() at time zone 'Asia/Seoul')::date - 21, 47, 29);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), (now() at time zone 'Asia/Seoul')::date - 22, 33, 25);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), (now() at time zone 'Asia/Seoul')::date - 23, 35, 25);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), (now() at time zone 'Asia/Seoul')::date - 24, 49, 35);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), (now() at time zone 'Asia/Seoul')::date - 25, 48, 28);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), (now() at time zone 'Asia/Seoul')::date - 26, 74, 52);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), (now() at time zone 'Asia/Seoul')::date - 27, 49, 27);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), (now() at time zone 'Asia/Seoul')::date - 28, 32, 18);
insert into blog_daily_stat (blog_id, stat_date, views, visitors) values ((select blog_id from blog where users_id = (select users_id from users where email = 'demo.woodwork@example.com')), (now() at time zone 'Asia/Seoul')::date - 29, 64, 32);

commit;
\echo 예시 데이터를 넣었습니다: 블로거 12명, 글 108개
