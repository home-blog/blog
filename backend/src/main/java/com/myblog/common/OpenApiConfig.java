package com.myblog.common;

import io.swagger.v3.core.jackson.ModelResolver;
import io.swagger.v3.core.jackson.TypeNameResolver;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import java.util.List;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.AntPathMatcher;

/**
 * 자동 API 문서(springdoc)의 머리: API 주소 버전과 릴리스 번호를 같이 보여 준다 (2026-10-12 사용자: 릴리스를 API에도 반영).
 * 문서는 개발 설정에서만 켠다 (springdoc.*.enabled). 바뀐 것의 기록은 저장소 맨 위 CHANGELOG.md.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI geulgyeolApi(@Value("${myblog.release}") String release) {
        return new OpenAPI().info(new Info()
                .title("글결 API")
                .version(ApiVersionConfig.SUPPORTED[ApiVersionConfig.SUPPORTED.length - 1] + " · 릴리스 " + release)
                .description("""
                        여러 사람이 각자 블로그를 쓰고 한곳에서 둘러보는 멀티 블로그 서비스 "글결"의 서버 API입니다.

                        **주소와 버전**
                        - 모든 주소는 `/api/v1/…` 입니다 (이미지 파일 `/api/images/{파일}`만 버전 없이 고정).
                        - API 버전은 릴리스와 따로 갑니다: 칸을 더하는 변경은 v1 그대로, 칸을 빼거나 이름·뜻을 바꾸는 변경만 v2를 엽니다.
                        - 릴리스마다 바뀐 것은 저장소의 CHANGELOG.md에 있습니다.

                        **로그인 (서버 세션)**
                        - 로그인하면 세션 쿠키 `SESSION`이 생기고, 세션은 PostgreSQL에 저장됩니다 (JWT 안 씀). 정한 시간(`auth.session.idle-timeout`) 동안 쓰지 않으면 끝납니다.
                        - 로그아웃: `POST /api/v1/auth/logout` (Spring Security가 처리)
                        - 소셜 로그인: 화면이 `/oauth2/authorization/{github|google|kakao|naver}`로 이동 → 회사 화면 → `/login/oauth2/code/{회사}`로 돌아옴

                        **CSRF**
                        - GET이 아닌 요청에는 쿠키 `XSRF-TOKEN`의 값을 머리글 `X-XSRF-TOKEN`에 실어 보냅니다 (`GET /api/v1/auth/csrf`로 받음).

                        **오류 모양** (모든 API 공통)
                        ```json
                        { "code": "VALIDATION_FAILED", "message": "입력값을 다시 확인해 주세요",
                          "fieldErrors": [ { "field": "title", "code": "TITLE_REQUIRED", "message": "제목을 입력해 주세요" } ] }
                        ```
                        볼 권한이 없는 글·댓글·이미지는 있는지 없는지 알 수 없도록 없는 것과 같은 404로 답합니다."""))
                .tags(List.of(
                        tag("1. 로그인·가입", "이메일 인증번호로 가입하고, 이메일·비밀번호로 로그인한다"),
                        tag("2. 소셜 로그인", "GitHub·Google·카카오·네이버 계정으로 로그인하고 닉네임만 정해 가입한다"),
                        tag("3. 마이페이지", "내 정보, 비밀번호 변경, 탈퇴, 연결된 소셜 계정"),
                        tag("4. 블로그·분류", "회원마다 블로그 하나. 블로그 안의 분류는 비공개로 둘 수 있다"),
                        tag("5. 글", "마크다운 글 쓰기·읽기·수정·삭제. 공개 범위는 공개·비공개"),
                        tag("6. 둘러보기·검색", "첫 화면 글 모음(주제별), 인기 글, 블로그별 목록, 검색, 태그"),
                        tag("7. 댓글·좋아요·신고", "글에 남기는 반응"),
                        tag("8. 이미지", "글에 넣는 사진 올리기·보기 (MinIO에 저장)"),
                        tag("9. 블로그 관리", "내 블로그의 대시보드, 통계, 글·댓글 관리"),
                        tag("10. 공유·검색엔진", "링크 미리보기 카드, RSS, 사이트 지도 (화면이 아니라 로봇이 읽는 주소)"),
                        tag("11. 공지사항", "업데이트 소식과 공지. 읽기는 누구나, 쓰기는 관리자만"),
                        tag("12. 글쓰기 도구", "글쓰기 잔디, 연재, 임시 저장, 예약 발행"),
                        tag("13. AI", "빨간 펜 교정(Claude), 비슷한 글(Voyage 임베딩). 열쇠가 없으면 빨간 펜은 꺼지고 비슷한 글은 태그로 대신한다"),
                        tag("14. 구독·알림", "원고 받아 보기(블로그 구독), 우편함, 원고 편지(새 글 메일)"),
                        tag("15. 글마당", "블로그와 따로 서는 주제별 게시판. 이야기·물음, 댓글, 물음의 채택 도장")))
                .components(new Components()
                        .addSecuritySchemes(SESSION, new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.COOKIE).name("SESSION")
                                .description("로그인하면 생기는 세션 쿠키 (HttpOnly, SameSite=Lax, 배포에서는 Secure)"))
                        .addSecuritySchemes(CSRF, new SecurityScheme().type(SecurityScheme.Type.APIKEY)
                                .in(SecurityScheme.In.HEADER).name("X-XSRF-TOKEN")
                                .description("GET이 아닌 요청에 싣는 CSRF 토큰 (쿠키 XSRF-TOKEN의 값)")));
    }

    /**
     * 문서의 자료 모양 이름을 "바깥 클래스.안 클래스"로 짓는다 (예: PostRequests.Write, CommentRequests.Write).
     * 기본은 안 클래스 이름만 써서, 기능마다 같은 이름(Write, Update, MessageResponse …)이 겹치면 한쪽이 다른 쪽을 덮어
     * 엉뚱한 칸이 문서에 보였다.
     */
    @Bean
    ModelResolver nestedTypeNames() {
        return new ModelResolver(Json.mapper(), new NestedTypeNameResolver());
    }

    static final class NestedTypeNameResolver extends TypeNameResolver {

        @Override
        protected String getNameOfClass(Class<?> type) {
            String name = super.getNameOfClass(type);
            for (Class<?> outer = type.getEnclosingClass(); outer != null; outer = outer.getEnclosingClass()) {
                name = outer.getSimpleName() + "." + name;
            }
            return name;
        }
    }

    /**
     * 문서에 "로그인 필요"(세션 쿠키)와 "CSRF 토큰"을 표시한다. 로그인 없이 부르는 주소는 user/config/SecurityConfig의
     * permitAll 목록과 같다 (그쪽을 바꾸면 여기도 바꾼다). GET이 아닌 요청은 모두 CSRF 토큰을 싣는다.
     */
    @Bean
    OpenApiCustomizer securityMarks() {
        return openApi -> openApi.getPaths().forEach((path, item) -> item.readOperationsMap().forEach((method, operation) -> {
            boolean get = method == PathItem.HttpMethod.GET;
            SecurityRequirement requirement = new SecurityRequirement();
            if (!isPublic(path, get)) {
                requirement.addList(SESSION);
            }
            if (!get) {
                requirement.addList(CSRF);
            }
            if (!requirement.isEmpty()) {
                operation.addSecurityItem(requirement);
            }
        }));
    }

    private static final AntPathMatcher MATCHER = new AntPathMatcher();
    private static final List<String> PUBLIC_ANY_METHOD = List.of("/api/*/auth/**");
    private static final List<String> PUBLIC_GET = List.of("/api/*/blogs/**", "/api/*/posts", "/api/*/posts/popular",
            "/api/*/posts/summary", "/api/*/posts/{postId}", "/api/*/search/**", "/api/*/posts/{postId}/comments",
            "/api/*/tags/**", "/api/*/series/{seriesId}", "/api/*/posts/{postId}/series", "/api/*/posts/{postId}/related", "/api/*/notices", "/api/*/notices/**", "/api/*/board/**", "/api/images/**", "/og", "/og/", "/og/**",
            "/rss.xml", "/blog/*/rss.xml", "/sitemap.xml", "/robots.txt");

    /** 보안 설정으로는 열려 있지만 로그인하지 않았으면 처리기가 401로 답하는 주소 (지금 로그인했는지 묻는 주소). */
    private static final List<String> ANSWERS_401_WITHOUT_LOGIN = List.of("/api/*/auth/me");

    static boolean isPublic(String path, boolean get) {
        if (ANSWERS_401_WITHOUT_LOGIN.stream().anyMatch(pattern -> MATCHER.match(pattern, path))) {
            return false;
        }
        return PUBLIC_ANY_METHOD.stream().anyMatch(pattern -> MATCHER.match(pattern, path))
                || get && PUBLIC_GET.stream().anyMatch(pattern -> MATCHER.match(pattern, path));
    }

    private static Tag tag(String name, String description) {
        return new Tag().name(name).description(description);
    }

    static final String SESSION = "세션 쿠키";
    static final String CSRF = "CSRF 토큰";

    /** 처리기 주소의 {version} 칸을 지금 버전(v1)으로 채워 문서에서 바로 불러 볼 수 있게 한다. */
    @Bean
    OpenApiCustomizer currentVersionPaths() {
        String current = "/api/" + ApiVersionConfig.SUPPORTED[ApiVersionConfig.SUPPORTED.length - 1] + "/";
        return openApi -> {
            Paths paths = new Paths();
            openApi.getPaths().forEach((path, item) -> paths.addPathItem(path.replace("/api/{version}/", current), item));
            openApi.setPaths(paths);
        };
    }
}
