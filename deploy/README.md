# 실습 서버에 올리기

`main`에 코드가 합쳐지고 CI가 통과하면 **GitHub Actions(`.github/workflows/deploy.yml`)가 알아서 배포한다.** 서버에 들어가 있을 필요가 없다.

1. Actions가 서버·화면 Docker 이미지를 만든다.
2. SSH로 실습 서버에 접속해 이미지와 배포 파일을 `~/geulgyeol/`에 올린다. 접속 정보는 GitHub Secrets에서 읽는다.
3. 서버에서 `deploy/remote-deploy.sh`를 실행해 컨테이너를 새로 켠다.

구성: 인터넷 → 실습 서버 Nginx(도메인 → `127.0.0.1:8410`) → **화면 컨테이너**(nginx: 화면 파일을 주고 `/api/`는 서버로 넘김) → **서버 컨테이너**(Spring) → 실습 DB(PostgreSQL, 내 스키마) · Redis · MinIO 컨테이너.
여는 포트는 화면 입구 8410 하나뿐이고, 그것도 서버 안(`127.0.0.1`)에서만 받는다. Redis·MinIO는 컨테이너끼리만 쓴다(여럿이 쓰는 서버라서).

> 이 저장소는 공개다. 서버 주소·비밀번호는 GitHub Secrets에만 둔다.

## 준비 (처음 한 번)

**GitHub Secrets** (저장소 Settings → Secrets and variables → Actions)

| 이름 | 내용 | |
|---|---|---|
| `SSH_ADDRESS`, `SSH_PORT`, `SSH_ID`, `SSH_PASSWORD` | 실습 서버 접속 | 꼭 |
| `SSH_KNOWN_HOSTS` | 서버 지문. 내 컴퓨터 터미널에서 `ssh-keyscan -p <포트> <주소>`를 실행해 나온 줄을 그대로 넣는다. 비밀번호를 보내기 전에 진짜 실습 서버인지 확인하는 데 쓴다 | 꼭 |
| `DB_ADDRESS`, `DB_PORT`, `DB_NAME`, `DB_SCHEMA`, `DB_USERNAME`, `DB_PASSWORD` | 실습 DB. 표는 `DB_SCHEMA` 안에 만든다. `DB_ADDRESS`는 `localhost`가 아닌 DB 서버의 실제 주소 | 꼭 |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | 인증 메일 (Gmail: `smtp.gmail.com` + 앱 비밀번호) | 없으면 가입이 안 된다 |

이미지 저장소 키와 인증번호 키는 첫 배포 때 서버가 스스로 만들어 `~/geulgyeol/.generated.env`(나만 읽기)에 두고 계속 쓴다. **이 파일을 지우면 올린 이미지를 읽지 못한다.**

**도메인 연결** (서버에서 한 번, 첫 배포가 끝난 뒤)

```bash
bash ~/geulgyeol/deploy/make-nginx-conf.sh <내 도메인>   # ~/nginx/<내 도메인>.conf가 생긴다
```
그다음 강사님께 파일 이름을 알려 적용을 요청한다.

## 배포하기

- **자동**: `main`에 PR을 합치면 CI 다음에 배포가 돈다. GitHub의 Actions 탭 → `배포`에서 진행을 본다.
- **손으로**: Actions 탭 → `배포` → `Run workflow`.

## 서버에서 볼 때 (SSH 접속 후)

```bash
docker ps --filter name=geulgyeol         # 상태
docker logs -f geulgyeol-server-1         # 서버 로그 (나올 때 Ctrl+C)
bash ~/geulgyeol/deploy/remote-deploy.sh  # 지금 이미지로 다시 켜기
```

## 알아 둘 것

- **HTTPS**: 지금은 http라서 로그인 쿠키를 http에서도 보낸다(`SESSION_COOKIE_SECURE=false`). 도메인에 HTTPS(자물쇠)가 붙으면 저장소 Settings → Variables에 `SESSION_COOKIE_SECURE`=`true`를 만들고 다시 배포한다.
- **DB 스키마**: 서버는 `DB_SCHEMA` 안에만 표를 만든다(Flyway). 그 스키마에 다른 표가 이미 있으면 서버가 켜지지 않는다.
- 같은 Docker를 여럿이 쓰므로 컨테이너·이미지 이름은 모두 `geulgyeol-`로 시작한다. 옛 이미지는 배포 때 내 것만 지운다.
