# 실습 서버에 올리기

**릴리스를 낼 때**(Actions → `배포`에 릴리스 번호를 넣어 손으로 돌리거나, GitHub에서 릴리스를 직접 내면) **GitHub Actions(`.github/workflows/deploy.yml`)가 배포한다.** `main`에 합친다고 바로 배포되지는 않는다(아래 `릴리스로`). 서버에 들어가 있을 필요가 없다.

1. Actions가 서버·화면 Docker 이미지를 만든다.
2. SSH로 실습 서버에 접속해 이미지와 배포 파일을 `~/geulgyeol/`에 올린다. 접속 정보는 GitHub Secrets에서 읽는다.
3. 서버에서 `deploy/remote-deploy.sh`를 실행해 앱 두 벌을 **한 벌씩** 새 이미지로 바꾼다 (무중단 롤링 배포, 아래 "무중단 배포").

구성: 인터넷 → 실습 서버 Nginx(도메인 → `upstream`: `127.0.0.1:8410` a 벌, `127.0.0.1:8411` b 벌) → 벌마다 **화면 컨테이너**(nginx: 화면 파일을 주고 `/api/`는 같은 벌의 서버로 넘김) → **서버 컨테이너**(Spring) → 두 벌이 같이 쓰는 실습 DB(PostgreSQL, 내 스키마) · Redis · MinIO 컨테이너.
여는 포트는 화면 입구 8410·8411뿐이고, 그것도 서버 안(`127.0.0.1`)에서만 받는다. Redis·MinIO는 컨테이너끼리만 쓴다(여럿이 쓰는 서버라서).

> 이 저장소는 공개다. 서버 주소·비밀번호는 GitHub Secrets에만 둔다.

## 준비 (처음 한 번)

**GitHub Secrets** (저장소 Settings → Secrets and variables → Actions)

| 이름 | 내용 | |
|---|---|---|
| `SSH_ADDRESS`, `SSH_PORT`, `SSH_ID`, `SSH_PASSWORD` | 실습 서버 접속 | 꼭 |
| `SSH_KNOWN_HOSTS` | 서버 지문. 비밀번호를 보내기 전에 진짜 실습 서버인지 확인하는 데 쓴다. 아래 "서버 지문 넣기"대로 **확인한 지문만** 넣는다 | 꼭 |
| `DB_ADDRESS`, `DB_PORT`, `DB_NAME`, `DB_SCHEMA`, `DB_USERNAME`, `DB_PASSWORD` | 실습 DB. 표는 `DB_SCHEMA` 안에 만든다. `DB_ADDRESS`는 `localhost`가 아닌 DB 서버의 실제 주소 | 꼭 |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | 인증 메일 (Gmail: `smtp.gmail.com` + 앱 비밀번호) | 없으면 가입이 안 된다 |
| `SOCIAL_GITHUB_CLIENT_ID`, `SOCIAL_GITHUB_CLIENT_SECRET` (GOOGLE, KAKAO, NAVER도 같은 모양) | 소셜 로그인 앱 열쇠 (아래 "소셜 로그인 앱 등록") | 없는 회사는 버튼이 안 보인다 |

**소셜 로그인 앱 등록** (specs/010). 회사마다 개발자 사이트에 앱을 만들고, 받은 Client ID·Secret을 위 Secrets(배포)와 저장소 맨 위 `.env`(로컬)에 넣는다. 콜백(Redirect) 주소는 로컬 `http://localhost:5173/login/oauth2/code/{회사}`, 배포 `http://<내 도메인>/login/oauth2/code/{회사}` (HTTPS가 붙으면 `https://`로 바꾼다).

| 회사 | 어디서 | 주의 |
|---|---|---|
| GitHub (`github`) | Settings → Developer settings → OAuth Apps → New OAuth App | 콜백 주소를 하나만 받는다: 로컬용·배포용 앱을 따로 만든다 |
| Google (`google`) | Google Cloud Console → API 및 서비스 → OAuth 동의 화면, 사용자 인증 정보 → OAuth 클라이언트 ID(웹 애플리케이션) | 배포 주소는 HTTPS가 붙은 뒤에 넣을 수 있다. 로컬(localhost)은 지금도 된다 |
| 카카오 (`kakao`) | Kakao Developers → 내 애플리케이션 → 카카오 로그인 켜기, Redirect URI, 보안 → Client Secret 켜기 | 동의 항목에 닉네임. 이메일은 비즈 앱이어야 받을 수 있고, 없으면 가입할 때 이메일 인증번호로 받는다. REST API 키가 Client ID |
| 네이버 (`naver`) | Naver Developers → 애플리케이션 등록 → 네이버 로그인, 이메일·별명, 서비스 URL과 Callback URL | 검수 전에는 등록한 아이디만 로그인된다 |

**서버 지문 넣기**: `ssh-keyscan`은 받은 지문이 진짜인지 확인해 주지 않는다. 그래서 다른 길로 확인한 지문과 맞춰 본 뒤에 넣는다.

```bash
# 1. 내 컴퓨터가 이미 믿고 접속해 온 지문 (처음 SSH로 접속할 때 yes 하고 저장된 것)
ssh-keygen -F '[<주소>]:<포트>'
# 2. 지금 서버가 보내는 지문
ssh-keyscan -p <포트> <주소>
```
두 결과의 `ssh-ed25519 AAAA...` 부분이 같으면 2번 결과를 그대로 Secret에 넣는다. 다르거나 1번이 비어 있으면 넣지 말고 강사님께 서버 지문(`ssh-keygen -lf /etc/ssh/ssh_host_ed25519_key.pub` 결과)을 받아 `ssh-keygen -lf <(ssh-keyscan -p <포트> <주소>)`와 맞춰 본다.

이미지 저장소 키와 인증번호 키는 첫 배포 때 서버가 스스로 만들어 `~/geulgyeol/.generated.env`(나만 읽기)에 두고 계속 쓴다. **이 파일을 지우면 올린 이미지를 읽지 못한다.**

**도메인 연결** (서버에서 한 번, 첫 배포가 끝난 뒤)

```bash
bash ~/geulgyeol/deploy/make-nginx-conf.sh <내 도메인>   # ~/nginx/<내 도메인>.conf가 생긴다
```
그다음 강사님께 파일 이름을 알려 적용을 요청한다(학원 Nginx는 5분마다 다시 읽는다). 이미 파일이 있으면 덮어쓰지 않는다. 강사님이 HTTPS(Certbot)를 덧붙였을 수 있어서다. 그때는 아래 "학원 Nginx 고치기"대로 필요한 줄만 고친다.

## 배포하기

- **릴리스로**: `main`에 합친다고 바로 배포되지 않는다. 릴리스를 낼 때 그 버전이 배포된다. **로컬에서 어느 정도 해 두고 → 버전을 올려 → 배포** (2026-10-12 사용자): 쓸 만한 묶음이 되면 화면을 확인하고 "배포해줘" 뒤에 낸다. 새 화면·기능·API가 생기면 가운데 숫자(0.6.0), 버그 수정·다듬기·문서·운영 개선은 끝 숫자(0.5.1) (2026-10-13 사용자). 서비스가 멈추는 급한 버그만 따로 바로 낸다.
- **릴리스 내는 순서** (맨 위 [`CHANGELOG.md`](../CHANGELOG.md)가 기록의 원본):
  1. PR마다 `CHANGELOG.md`의 다음 번호 절(`## [0.5.0] - 준비 중`)에 화면·API·운영으로 나눠 쌓고(낼 때 날짜를 적는다), API가 바뀌었으면 주소 버전(v1 그대로인지, v2를 열었는지)도 적는다.
  2. `backend/pom.xml`의 `<version>`, `frontend/package.json`(그리고 `package-lock.json` 맨 위)의 `version`을 같은 번호로 맞춘다.
  3. "배포해줘"를 받으면 Git Flow대로 `develop`에서 `release/<번호>`를 만들어 `CHANGELOG.md`의 `준비 중`을 날짜로 바꾸고 `main`으로 PR을 올려 합친다.
  4. Actions → `배포` → Run workflow(브랜치 `main`)에 번호를 넣는다. 확인 → 배포 → 성공하면 태그 `v<번호>`와 GitHub 릴리스를 `CHANGELOG.md`의 그 절로 만든다.
  5. 배포가 끝나면 `main`을 `develop`에 다시 합친다(PR). 급한 수정(`hotfix/<번호>`)도 `main`에 합치고 배포한 뒤 `develop`에 합친다.
  6. 번호와 `pom.xml`·`package.json`·`package-lock.json`의 버전, `CHANGELOG.md`의 절이 다르거나 같은 태그가 이미 있으면 배포 전에 멈춘다.
  - GitHub 화면에서 직접 릴리스(태그 `v<번호>`, 대상 `main`)를 내도 같은 확인을 거쳐 배포된다. 미리보기(pre-release)는 배포하지 않는다. **지난 버전을 기록용으로 새로 낼 때는 미리보기로** 낸다(옛 커밋에는 이 확인이 없어 그대로 배포된다).
- **손으로**: Actions 탭 → `배포` → `Run workflow` (main의 지금 코드를 다시 올린다. 설정·Secrets만 바꿨을 때).

## 무중단 배포 (specs/011)

앱(화면 + 서버)을 두 벌 같이 띄우고, 학원 Nginx가 `upstream`으로 번갈아 보낸다. 배포는 한 벌씩 바꾼다(롤링).

1. 한 벌(예: a)의 화면 입구를 끈다. 포트가 닫히면 학원 Nginx가 같은 요청을 다른 벌(b)로 다시 보낸다(`proxy_next_upstream`). 처리 중인 요청은 마치고 꺼진다.
2. a 벌의 서버를 새 이미지로 켜고 건강해질 때까지 기다린 뒤 화면 입구를 켠다.
3. `127.0.0.1:8410`으로 직접 확인한다(API 200, `X-Instance: a`, 첫 화면 200). **실패하면 a 벌만 옛 이미지로 되돌리고 멈춘다.** b 벌은 그대로 손님을 받는다.
4. 15초(`SLOT_SETTLE_SECONDS`) 기다린다. 학원 Nginx가 한 번 실패한 벌을 10초(`fail_timeout`) 쉬게 하므로, 그보다 오래 기다려야 a 벌을 다시 쓰기 시작한 뒤에 b 벌을 끈다.
5. b 벌도 같은 순서로 바꾼다.

**지켜야 할 것** (두 버전이 잠깐 같이 돈다)
- **DB는 넓히고 → 좁히기**: 표·칸 추가는 바로 해도 된다. 칸 이름 바꾸기·지우기·`NOT NULL` 추가는 릴리스를 나눈다(새 칸 추가 → 코드가 새 칸을 씀 → 다음 릴리스에서 옛 칸 지우기). 새 서버가 켜질 때 Flyway가 바꾼 표 위에서 옛 서버가 돌기 때문이다.
- **API는 v1 약속을 깨지 않는다**: 깨지는 변경이면 `/api/v2`를 연다 (CHANGELOG 맨 위 "번호 읽는 법").
- **세션에 들어가는 클래스**(`MemberPrincipal` 등)의 `serialVersionUID`를 올리면 다른 버전이 만든 세션을 못 읽어 로그아웃된다. 꼭 바꿔야 하면 릴리스 기록에 적는다.
- **정해진 시각에 도는 일**(`@Scheduled`)은 두 벌에서 다 돈다. 지금 있는 이미지 정리(`OrphanImageCleaner`)는 두 번 돌아도 결과가 같다. 두 번 돌면 안 되는 일을 더할 때는 한쪽만 돌게 잠근다.

**한계**: 한 벌을 끄는 순간(0.1초 안팎) Docker 포트 중계(docker-proxy)가 받은 연결이 끊길 수 있다. GET은 학원 Nginx가 다른 벌로 다시 보내 성공하지만, 이미 보낸 POST는 두 번 저장되지 않도록 다시 보내지 않아 502가 될 수 있다(로컬에서 1초에 70번쯤 보낼 때 벌마다 1개).

## 트래픽 보기

```bash
# 내 컴퓨터에서: 요청마다 어느 벌이 받았는지 (8410 = a, 8411 = b. "8411, 8410"은 8411이 안 돼서 8410으로 다시 보낸 것)
for i in $(seq 10); do curl -s -o /dev/null -D - https://<내 도메인>/api/v1/auth/csrf | grep -iE 'x-upstream|x-instance'; done

# 서버에서: 접속 로그로 벌마다 요청 수 (견본의 log_format을 쓸 때)
grep -o 'upstream=[^ ]*' ~/nginx/logs/<내 도메인>.access.log | sort | uniq -c
tail -f ~/nginx/logs/<내 도메인>.access.log
```

배포 중에 끊기지 않는지 보려면 배포하는 동안 다른 창에서 요청을 계속 보내고 실패 수를 센다.

```bash
ok=0; fail=0
while true; do
  code=$(curl -s -o /dev/null -w '%{http_code}' https://<내 도메인>/api/v1/auth/csrf)
  [ "$code" = 200 ] && ok=$((ok+1)) || { fail=$((fail+1)); echo " 실패 $code"; }
  printf '\r성공 %d / 실패 %d' "$ok" "$fail"; sleep 0.2
done
```

## 학원 Nginx 고치기

이미 있는 `~/nginx/<내 도메인>.conf`는 통째로 덮어쓰지 않는다(강사님이 HTTPS를 덧붙여 두었다). 백업하고 필요한 줄만 고친 뒤 `diff`로 확인한다.

```bash
cd ~/nginx
cp <내 도메인>.conf <내 도메인>.conf.bak-$(date +%Y%m%d)
# 1) 맨 위에 upstream 묶음
{ printf 'upstream geulgyeol {\n    server 127.0.0.1:8410 max_fails=1 fail_timeout=10s;\n    server 127.0.0.1:8411 max_fails=1 fail_timeout=10s;\n}\n\n'
  cat <내 도메인>.conf.bak-$(date +%Y%m%d); } > <내 도메인>.conf
# 2) proxy_pass를 묶음으로 바꾸고, 다른 벌로 다시 보내기·확인용 머리글을 더한다
sed -i 's|^\(\s*\)proxy_pass http://127.0.0.1:8410;|\1proxy_pass http://geulgyeol;\n\1proxy_next_upstream error timeout http_502 http_503;\n\1add_header X-Upstream $upstream_addr always;|' <내 도메인>.conf
diff <내 도메인>.conf.bak-$(date +%Y%m%d) <내 도메인>.conf   # upstream 묶음과 proxy_pass 부분만 나와야 한다
```

되돌릴 때는 백업 파일을 다시 복사한다. 5분 안에 반영된다.

## 서버에서 볼 때 (SSH 접속 후)

```bash
docker ps --filter name=geulgyeol           # 상태 (web-a, server-a, web-b, server-b, redis, minio)
docker logs -f geulgyeol-server-a-1         # a 벌 서버 로그 (나올 때 Ctrl+C). b 벌은 server-b-1
cat ~/geulgyeol/.slots.env                  # 벌마다 지금 이미지와 바로 전 이미지(PREVIOUS_TAG)
bash ~/geulgyeol/deploy/remote-deploy.sh    # 지금 이미지로 한 벌씩 다시 켜기
bash ~/geulgyeol/deploy/remote-deploy.sh <태그>   # 서버에 남은 다른 이미지로 되돌리기 (예: .slots.env의 PREVIOUS_TAG)
```

## 알아 둘 것

- **HTTPS**: 강사님이 Certbot으로 붙여 주셨다(http는 https로 넘어간다, 2026-10-14 확인). 로그인 쿠키를 https에서만 보내려면 저장소 Settings → Variables에 `SESSION_COOKIE_SECURE`=`true`를 만들고 다시 배포한다(없으면 false).
- **DB 스키마**: 서버는 `DB_SCHEMA` 안에만 표를 만든다(Flyway). 그 스키마에 다른 표가 이미 있으면 서버가 켜지지 않는다.
- 같은 Docker를 여럿이 쓰므로 컨테이너·이미지 이름은 모두 `geulgyeol-`로 시작한다. 옛 이미지는 배포 때 내 것만 지운다.
