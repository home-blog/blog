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
| `SSH_KNOWN_HOSTS` | 서버 지문. 비밀번호를 보내기 전에 진짜 실습 서버인지 확인하는 데 쓴다. 아래 "서버 지문 넣기"대로 **확인한 지문만** 넣는다 | 꼭 |
| `DB_ADDRESS`, `DB_PORT`, `DB_NAME`, `DB_SCHEMA`, `DB_USERNAME`, `DB_PASSWORD` | 실습 DB. 표는 `DB_SCHEMA` 안에 만든다. `DB_ADDRESS`는 `localhost`가 아닌 DB 서버의 실제 주소 | 꼭 |
| `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD` | 인증 메일 (Gmail: `smtp.gmail.com` + 앱 비밀번호) | 없으면 가입이 안 된다 |

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
그다음 강사님께 파일 이름을 알려 적용을 요청한다.

## 배포하기

- **릴리스로**: `main`에 합친다고 바로 배포되지 않는다. 릴리스를 낼 때 그 버전이 배포된다. **릴리스는 작게 자주** (2026-10-12 사용자): 버그 수정은 merge 뒤 바로(끝 숫자 0.4.1), 기능은 화면을 확인하고 "배포해줘" 뒤(가운데 숫자 0.5.0).
- **릴리스 내는 순서** (맨 위 [`CHANGELOG.md`](../CHANGELOG.md)가 기록의 원본):
  1. PR에서 `CHANGELOG.md`에 그 번호의 절(`## [0.4.1] - 날짜`)을 화면·API·운영으로 나눠 쓰고, API가 바뀌었으면 주소 버전(v1 그대로인지, v2를 열었는지)도 적는다.
  2. 같은 PR에서 `backend/pom.xml`의 `<version>`, `frontend/package.json`(그리고 `package-lock.json` 맨 위)의 `version`을 같은 번호로 올린다.
  3. merge한 뒤 Actions → `배포` → Run workflow(브랜치 `main`)에 번호를 넣는다. 확인 → 배포 → 성공하면 태그 `v<번호>`와 GitHub 릴리스를 `CHANGELOG.md`의 그 절로 만든다.
  4. 번호·버전·`CHANGELOG.md`의 절이 다르거나 같은 태그가 이미 있으면 배포 전에 멈춘다.
  - GitHub 화면에서 직접 릴리스(태그 `v<번호>`, 대상 `main`)를 내도 같은 확인을 거쳐 배포된다. 미리보기(pre-release)는 배포하지 않는다. **지난 버전을 기록용으로 새로 낼 때는 미리보기로** 낸다(옛 커밋에는 이 확인이 없어 그대로 배포된다).
- **손으로**: Actions 탭 → `배포` → `Run workflow` (main의 지금 코드를 다시 올린다. 설정·Secrets만 바꿨을 때).

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
