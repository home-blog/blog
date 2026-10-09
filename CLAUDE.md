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
- 설정값(글자 수, 시간, 횟수)은 `application.yml`(`auth.*`, `account.*` 등) 한곳에서 읽는다. 비밀 값은 환경 변수로만.
- 표는 Flyway(`backend/src/main/resources/db/migration`)로만 만든다. JPA는 `ddl-auto: validate`.
- 모듈 방향(Spring Modulith, `ModularityTest`): `user ← blog ← post ← comment/community/image`, `stats`는 읽기만. 아래 모듈은 위 모듈을 직접 부르지 않고 이벤트나 질문 틀(인터페이스)을 쓴다.
- 화면 디자인은 **`docs/DESIGN.md`(Cohere 스타일, 에디토리얼) 규칙을 엄격히 따른다** (2026-10-09 사용자 지시, 예전 "원고지" 방향을 대신함). 토큰은 `frontend/src/index.css` 한곳: 캔버스 `#ffffff`·크림 `#f0eee9`, 글자 `#212121`, 선은 1px `#e5e7eb`, **그림자 금지**, 주 버튼은 `#17171c` 다크 알약·보조는 밑줄 링크, 모서리는 4·8·12(코드)·22·9999px만, 글자 12~72px 단계만, 색 있는 글자·버튼·아이콘 금지. DESIGN.md에 없어 정한 것: 한글 글꼴 Pretendard, 큰 제목 글꼴 Noto Serif KR(CohereText 대신), 글 읽기 폭 760px, 인용구는 왼쪽 2px 선, 오류 표시는 색 대신 굵은 글자·2px 검은 선. Spring 기능은 문서 저장소 `docs/3-설계/기술스택-아키텍처.md` 2.2 표대로 적극적으로 쓴다.
- 이 저장소는 **공개(public)** 다. 비밀번호, 키, 계정 정보, 실습 서버 주소는 넣지 않는다.

## 확인하고 올리기

- 클라우드 세션에서는 Maven과 Docker가 된다. `dockerd`를 켜고 `docker compose up -d --wait` 뒤 `cd backend && mvn verify`, `cd frontend && npm ci && npm run lint && npm run build`를 **push 전에** 돌린다. 화면은 서버와 `npx vite`를 띄우고 Playwright(`/opt/node-tools/node_modules/playwright`, 브라우저는 미리 설치됨)로 확인할 수 있다.
- 커밋 작성자는 `JaeUng <rnrn4308428@gmail.com>`. 메시지는 한국어로, 무엇을 왜 바꿨는지와 작업 ID(T001 등)를 적는다.
- **PR은 기능 단위로** 올린다 (사용자 이야기 하나 = PR 하나). 브랜치는 `feat/<기능>-<내용>`, `fix/...`, `chore/...`. 작은 정리는 다음 기능 PR에 넣는다. 리뷰 수정은 같은 PR에서 한다.
- PR을 올리면 CI 3개(서버, 화면, 품질 분석)와 CodeRabbit을 확인하고, 맞는 지적은 고쳐 스레드에 답한 뒤 해결 표시한다. 모두 통과하면 merge한다 (2026-10-08 사용자: PR 올리기·품질 검사 대응·merge는 알아서).
- **Dependabot PR은 직접 merge하지 않는다**. 같은 변경을 내 PR로 반영하면 Dependabot이 자기 PR을 닫는다.
- 원격 브랜치 삭제와 새 저장소 만들기는 클라우드 세션에서 막혀 있다.
- 사용자는 Mac의 IntelliJ(`~/Documents/AIGJ_blog_docs` = 이 저장소)로 지켜본다. merge 뒤 `main`에서 `Cmd+T`로 받는다. 구현을 마치면 무엇을 만들었는지 짧게 보고한다.
