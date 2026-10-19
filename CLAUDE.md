# CLAUDE.md (코드 저장소)

이 저장소는 MyBlog의 **코드**(`backend/`, `frontend/`)만 있다. **작업 지침의 원본, 지금 상태, 다음 할 일, 명세는 문서 저장소 [home-blog/docs](https://github.com/home-blog/docs)의 `CLAUDE.md`에 있다.** 이 파일은 코드 작업에 꼭 필요한 것만 둔다.

> 마지막 정리: 2026-10-08 (저장소를 문서 `home-blog/docs`와 코드 `home-blog/blog`로 나눔. 이 저장소는 예전 `home-blog/myblog`)

## 세션을 시작하면

1. 이 파일을 읽는다.
2. **문서 저장소도 붙인다**: `add_repo`로 `home-blog/docs`를 붙이고 `/home/user/docs`에 받는다. 그 저장소의 `CLAUDE.md`(6. 지금 상태, 7. 다음 할 일)와 헌법(`.specify/memory/constitution.md`)을 읽는다.
3. 만들 것은 `docs`의 `specs/<기능>/tasks.md`, 방법은 같은 폴더의 `plan.md`, `data-model.md`, `contracts/`를 따른다.
4. 작업을 마치면 `docs`의 `tasks.md` 체크박스와 `CLAUDE.md` 6·7절을 고쳐 `docs`의 `main`에 커밋한다.

## 코드 작성 규칙

- 요청 흐름: 보안 필터 → 컨트롤러 → 서비스(핵심 규칙) → 저장소. 쿼리는 문자열로 이어 붙이지 않는다.
- 설정값(글자 수, 시간, 횟수)은 `application.yml`(`auth.*`, `account.*` 등) 한곳에서 읽는다. 비밀 값은 환경 변수로만. 로컬 접속 정보(DB, Redis, MinIO, 메일)는 루트 `.env` 하나(git 제외, 견본 `.env.example`)를 docker compose와 서버(dev)가 같이 읽는다.
- **테스트 먼저** (2026-10-15 사용자, 강의 "테스트 전략과 품질 관리"): 기능마다 정상값·경계값·예외값 테스트를 같이 쓴다. 서버는 JUnit 5(+MockMvc), 화면은 Vitest + Testing Library + MSW(가짜 서버). 테스트가 실패하면 **테스트가 아니라 구현을 고친다**(테스트가 틀린 경우만 예외, 이유를 커밋에 적는다). 운영에서 버그가 나면 그 상황을 재현하는 테스트부터 더한다. 기준은 문서 저장소 `docs/3-설계/테스트-전략.md`.
- 표는 Flyway(`backend/src/main/resources/db/migration`)로만 만든다. JPA는 `ddl-auto: validate`.
- 모듈 방향(Spring Modulith, `ModularityTest`): `user ← blog ← post ← comment/community/image/board`, 맨 위에 `stats`·`subscription`·`editorial`. 아래 모듈은 위 모듈을 직접 부르지 않고 이벤트나 질문 틀(인터페이스)을 쓴다. `stats`는 다른 모듈의 표를 직접 쓰지 않는다: 방문자 판단만 하고 조회 수는 그 표를 가진 모듈의 입구(`PostViewCounter`·`BoardViewCounter`)로 올린다. `editorial`(편집실)도 신고·글 잠금을 각 모듈의 입구(`PostReports`·`BoardReports`·`PostModeration`)로만 바꾼다.
- 화면 디자인은 **"원고지" 방향**으로 한다 (2026-10-12 사용자: Cohere 방향보다 원고지가 낫다고 해서 PR #40의 화면 변경을 되돌림). 토큰은 `frontend/src/index.css` 한곳: 종이 `--paper`, 먹색 글자 `--ink`, 원고지 칸 선 `--grid`, 강조 `--accent`. `docs/DESIGN.md`(Cohere 스타일)는 쓰지 않는다. Spring 기능은 문서 저장소 `docs/3-설계/기술스택-아키텍처.md` 2.2 표대로 적극적으로 쓴다.
- 이 저장소는 **공개(public)** 다. 비밀번호, 키, 계정 정보, 실습 서버 주소는 넣지 않는다.

## 확인하고 올리기

- 클라우드 세션에서는 Maven과 Docker가 된다. `dockerd`를 켜고 `cp .env.example .env`(처음 한 번), `docker compose up -d --wait` 뒤 저장소 맨 위에서 `(cd backend && mvn verify)`, `(cd frontend && npm ci && npm run lint && npm test && npm run build)`를 **push 전에** 돌린다. 화면은 서버와 `npx vite`를 띄우고 Playwright(`/opt/node-tools/node_modules/playwright`, 브라우저는 미리 설치됨)로 확인할 수 있다.
- 커밋 작성자는 `JaeUng <rnrn4308428@gmail.com>`. 메시지는 한국어로, 무엇을 왜 바꿨는지와 작업 ID(T001 등)를 적는다.
- **PR은 묶어서 적게** 올린다 (2026-10-12 사용자: PR마다 CodeRabbit·SonarQube·CI를 기다리느라 늦어진다). 한 번의 작업(디자인 수정, 기능 추가 여러 개)을 브랜치 하나에 커밋으로 쌓고, 사용자가 로컬(5173)에서 확인한 뒤 끝날 때(또는 "올려"라고 할 때) PR 하나로 올린다. 브랜치 push만으로는 CI·Sonar가 돌지 않는다. 브랜치는 Git Flow를 따른다(아래). 리뷰 수정은 같은 PR에서 한다.
- PR을 올리면 CI 3개(서버, 화면, 품질 분석)와 CodeRabbit을 확인하고, 맞는 지적은 고쳐 스레드에 답한 뒤 해결 표시한다. 모두 통과하면 merge한다 (2026-10-08 사용자: PR 올리기·품질 검사 대응·merge는 알아서). 문서(`*.md`, `docs/`, `specs/`)만 바꾼 PR은 CI가 돌지 않으므로(`paths-ignore`) CodeRabbit 리뷰만 보고 합친다 (README와 같은 기준).
- **Dependabot PR은 직접 merge하지 않는다**. 같은 변경을 내 PR로 반영하면 Dependabot이 자기 PR을 닫는다.
- **Git Flow (2026-10-13 사용자)**: 기본 브랜치는 `develop`. 기능은 `develop`에서 `feature/<내용>`을 만들어 `develop`으로 PR한다. 릴리스는 `develop`에서 `release/<번호>`(CHANGELOG 날짜·버전 확인)를 만들어 `main`으로 PR → merge 뒤 Actions `배포`를 main으로 손 실행(번호 입력) → `main`을 `develop`에 다시 합친다. 급한 버그는 `main`에서 `hotfix/<번호>` → `main` → `develop`. `main`·`develop`에 직접 push하지 않는다. 쉬운 설명은 문서 저장소 `docs/4-가이드/07-브랜치-전략.md`.
- 원격 브랜치 삭제와 새 저장소 만들기는 클라우드 세션에서 막혀 있다.
- 사용자는 Mac의 IntelliJ(`~/Documents/AIGJ_blog_docs` = 이 저장소)로 지켜본다. merge 뒤 `develop`(배포본은 `main`)에서 `Cmd+T`로 받는다. 구현을 마치면 무엇을 만들었는지 짧게 보고한다.
