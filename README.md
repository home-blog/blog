# MyBlog 코드

> 티스토리 같은 멀티 블로그 서비스(이름은 임시로 MyBlog)의 **코드 저장소**입니다.
> **요구사항·설계·명세 문서는 [home-blog/docs](https://github.com/home-blog/docs)** 에 있습니다 (2026-10-08에 나눔).

| 폴더 | 내용 |
|---|---|
| `backend/` | 서버 (Java 21, Spring Boot 4.1, Maven). 패키지 `com.myblog` 아래 기능별 모듈 |
| `frontend/` | 화면 (React 19, Vite 8, TypeScript) |
| `docker-compose.yml` | 개발용 PostgreSQL(pgvector 이미지), Redis |
| `.github/`, `.coderabbit.yaml`, `sonar-project.properties` | CI, 자동 코드 리뷰(CodeRabbit), 품질 분석(SonarQube) |

## 코드 실행하기

IntelliJ에서는 오른쪽 위 실행 목록(`.run/` 폴더)에서 고르면 됩니다: `1. DB·Redis·MinIO 켜기` → `MyBlog 전체 (서버+화면)` → http://localhost:5173

```bash
# 1. 개발용 DB, Redis, 이미지 저장소(MinIO) 띄우기
docker compose up -d --wait

# 2. 서버 (http://localhost:8080). 이미지 저장소 키는 docker-compose.yml의 개발 전용 값
cd backend && IMAGE_S3_ACCESS_KEY=myblog-dev IMAGE_S3_SECRET_KEY=myblog-dev-secret mvn spring-boot:run

# 3. 화면 (http://localhost:5173) — /api 요청은 서버로 넘어갑니다
cd frontend && npm install && npm run dev
```

- 서버 상태 확인: http://localhost:8080/actuator/health
- 테스트 전체: `cd backend && mvn verify` (DB와 Redis가 떠 있어야 합니다)
- 개발 설정에서는 인증 메일을 보내지 않고 서버 로그의 `[개발용 메일]` 줄에 인증번호를 찍습니다.

## 작업 방식

- `main`에 직접 올리지 않고, 브랜치를 만들어 PR로 올립니다. PR은 기능(사용자 이야기) 하나에 하나입니다.
- PR마다 자동 검사가 돕니다: **CI**(서버 빌드·테스트, 화면 린트·빌드), **CodeRabbit**(1차 코드 리뷰, 한국어), **SonarQube**(품질·커버리지, 아카데미 서버).
- CI가 통과해야 merge합니다.
- 무엇을 만들지는 문서 저장소의 `specs/<기능>/tasks.md`, 어떻게는 같은 폴더의 `plan.md`, `contracts/`를 따릅니다.
- 비밀번호, 키 같은 비밀 값은 코드와 설정 파일에 넣지 않고 환경 변수나 GitHub Secrets로 넣습니다. (`docker-compose.yml`과 `application.yml`의 기본값은 개발 전용입니다)
