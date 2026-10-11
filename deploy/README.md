# 실습 서버에 올리기

구성: 인터넷 → 실습 서버 Nginx(도메인 → `127.0.0.1:<내 포트>`) → **화면 컨테이너**(nginx: 화면 파일을 주고 `/api/`는 서버로 넘김) → **서버 컨테이너**(Spring) → DB·Redis·MinIO 컨테이너.
밖으로 여는 포트는 화면 컨테이너 하나(`WEB_PORT`, 기본 8410)뿐이다. DB·Redis·MinIO는 컨테이너끼리만 쓴다(여럿이 쓰는 서버라서).

> 이 저장소는 공개다. 서버 주소·비밀번호·`.env.deploy` 내용은 어디에도 올리지 않는다.

## 처음 한 번

```bash
# 1. 실습 서버에 접속 (주소·비밀번호는 수업 안내 참고)
# 2. 코드 받기
git clone https://github.com/home-blog/blog.git
cd blog

# 3. 비밀 값 채우기 (git에 올라가지 않는 파일)
cp .env.deploy.example .env.deploy
nano .env.deploy        # 비밀번호·키는 openssl rand로 만든다 (파일 안 설명 참고)

# 4. 켜기 (처음에는 이미지 빌드로 몇 분 걸린다)
bash deploy/deploy.sh

# 5. Nginx 설정 파일 만들기 → 강사님께 파일 이름을 알려 적용 요청
bash deploy/make-nginx-conf.sh <내 도메인> 8410
```

## 고친 코드를 다시 올릴 때

```bash
cd blog && bash deploy/deploy.sh   # git pull → 다시 빌드 → 켜기. DB·이미지는 그대로 남는다
```

## 자주 쓰는 명령

```bash
docker compose -f docker-compose.deploy.yml --env-file .env.deploy ps            # 상태
docker compose -f docker-compose.deploy.yml --env-file .env.deploy logs -f server # 서버 로그
docker compose -f docker-compose.deploy.yml --env-file .env.deploy down           # 끄기 (데이터는 남음)
```

## 알아 둘 것

- **HTTPS**: 도메인에 HTTPS(자물쇠)가 없으면 `.env.deploy`의 `SESSION_COOKIE_SECURE=false`로 해야 로그인이 된다. HTTPS가 붙으면 `true`로 되돌리고 `bash deploy/deploy.sh`.
- **인증 메일**: 배포에서는 SMTP로만 보낸다(인증번호를 로그에 남기지 않는다). `MAIL_*` 값이 없으면 가입이 안 된다.
- `docker compose down -v`는 DB와 이미지를 **지운다**. 쓰지 않는다.
