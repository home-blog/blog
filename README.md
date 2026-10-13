# MyBlog 코드

> 티스토리 같은 멀티 블로그 서비스(이름은 임시로 MyBlog)의 **코드 저장소**입니다.
> **요구사항·설계·명세 문서는 [home-blog/docs](https://github.com/home-blog/docs)** 에 있습니다 (2026-10-08에 나눔).

| 폴더 | 내용 |
|---|---|
| `backend/` | 서버 (Java 21, Spring Boot 4.1, Maven). 패키지 `com.myblog` 아래 기능별 모듈 |
| `frontend/` | 화면 (React 19, Vite 8, TypeScript) |
| `docker-compose.yml` | 개발용 PostgreSQL(pgvector 이미지), Redis, MinIO(이미지 저장소) |
| `.env.example` | 로컬 접속 정보 견본(DB, Redis, MinIO, 메일). `.env`로 복사해서 쓴다 |
| `.github/`, `.coderabbit.yaml`, `sonar-project.properties` | CI, 자동 코드 리뷰(CodeRabbit), 품질 분석(SonarQube) |

## 코드 실행하기

IntelliJ에서는 오른쪽 위 실행 목록(`.run/` 폴더)에서 고르면 됩니다: `1. DB·Redis·MinIO 켜기` → `MyBlog 전체 (서버+화면)` → http://localhost:5173 (`1.`은 `.env`가 없으면 견본에서 만들어 줍니다)

```bash
# 0. 접속 정보 파일 만들기 (처음 한 번). DB, Redis, MinIO, 메일 값이 모두 여기 있다. git에 올라가지 않는다
cp .env.example .env

# 1. 개발용 DB, Redis, 이미지 저장소(MinIO) 띄우기 (.env를 읽는다)
docker compose up -d --wait

# 2. 서버 (http://localhost:8080). 같은 .env를 읽는다
cd backend && mvn spring-boot:run

# 3. 화면 (http://localhost:5173) — /api 요청은 서버로 넘어갑니다
cd frontend && npm install && npm run dev
```

- 서버 상태 확인: http://localhost:8080/actuator/health
- 테스트 전체: `cd backend && mvn verify` (`.env`가 있고 DB와 Redis가 떠 있어야 합니다)
- 개발 설정에서는 인증 메일을 보내지 않고 서버 로그의 `[개발용 메일]` 줄에 인증번호를 찍습니다.
- 내 메일함(Gmail, 네이버)으로 실제 인증 메일을 받아 보려면 `.env`의 `MAIL_SENDER=smtp`로 바꾸고 `MAIL_HOST`, `MAIL_USERNAME`, `MAIL_PASSWORD`를 채운 뒤 서버를 다시 켭니다. 적는 법은 `.env.example`에 있습니다.

## 작업 방식

- **Git Flow**로 일합니다. `main`과 `develop`에는 직접 올리지 않고 모두 PR로 합칩니다.

  | 브랜치 | 어디서 만들어 | 어디로 합치나 | 하는 일 |
  |---|---|---|---|
  | `main` | — | — | 배포된 코드. 릴리스마다 태그 `v0.x.0` |
  | `develop` | `main` | — | 다음 릴리스를 모으는 곳 (기본 브랜치) |
  | `feature/<내용>` | `develop` | `develop` | 기능 하나 (예: `feature/v0.5-social-login`) |
  | `release/<번호>` | `develop` | `main`, 그다음 `develop` | 릴리스 준비: CHANGELOG 날짜, 마지막 확인. 합치면 배포 |
  | `hotfix/<번호>` | `main` | `main`, 그다음 `develop` | 서비스가 멈추는 급한 버그만 (예: `hotfix/0.4.1`) |

  ```text
  feature/* ──PR──▶ develop ──release/0.5.0──PR──▶ main ──▶ Actions 배포(0.5.0) → 태그 v0.5.0
                       ▲                                 │
                       └─────────── main을 다시 합침 ◀────┘   (hotfix/*도 main → develop 순서)
  ```
- PR마다 자동 검사가 돕니다: **CI**(서버 빌드·테스트, 화면 린트·빌드), **CodeRabbit**(1차 코드 리뷰, 한국어), **SonarQube**(품질·커버리지, 아카데미 서버).
- CI가 통과해야 merge합니다. `develop`과 `main`으로 가는 PR 모두 같은 검사를 거칩니다.
- 무엇을 만들지는 문서 저장소의 `specs/<기능>/tasks.md`, 어떻게는 같은 폴더의 `plan.md`, `contracts/`를 따릅니다.
- 비밀번호, 키 같은 비밀 값은 코드와 설정 파일에 넣지 않습니다. 로컬은 `.env`(git 제외), 배포는 환경 변수나 GitHub Secrets로 넣습니다. (`.env.example`의 값은 개발 전용입니다)
- 실습 서버 배포는 GitHub Actions(`.github/workflows/deploy.yml`)가 합니다. 방법은 [`deploy/README.md`](deploy/README.md)에 있습니다.
