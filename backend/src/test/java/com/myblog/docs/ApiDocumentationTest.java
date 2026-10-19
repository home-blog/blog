package com.myblog.docs;

import static org.springframework.restdocs.headers.HeaderDocumentation.headerWithName;
import static org.springframework.restdocs.headers.HeaderDocumentation.requestHeaders;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.document;
import static org.springframework.restdocs.mockmvc.MockMvcRestDocumentation.documentationConfiguration;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.modifyHeaders;
import static org.springframework.restdocs.operation.preprocess.Preprocessors.prettyPrint;
import static org.springframework.restdocs.payload.PayloadDocumentation.fieldWithPath;
import static org.springframework.restdocs.payload.PayloadDocumentation.requestFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.responseFields;
import static org.springframework.restdocs.payload.PayloadDocumentation.subsectionWithPath;
import static org.springframework.restdocs.request.RequestDocumentation.parameterWithName;
import static org.springframework.restdocs.request.RequestDocumentation.pathParameters;
import static org.springframework.restdocs.request.RequestDocumentation.queryParameters;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.myblog.ai.service.Proofreader;
import com.myblog.ai.service.Proofreader.Suggestion;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.restdocs.RestDocumentationContextProvider;
import org.springframework.restdocs.RestDocumentationExtension;
import org.springframework.restdocs.mockmvc.RestDocumentationRequestBuilders;
import org.springframework.restdocs.payload.FieldDescriptor;
import org.springframework.restdocs.payload.JsonFieldType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * API 문서 (Spring REST Docs). 실제로 요청을 보내고 받은 응답으로 문서 조각을 만든다 (target/generated-snippets).
 * 문서에 적지 않은 칸이 응답에 생기거나 적은 칸이 사라지면 이 테스트가 실패해 문서가 코드와 어긋나지 않는다.
 * HTML은 mvn -Pdocs verify → target/generated-docs/index.html (src/docs/asciidoc/index.adoc).
 *
 * 실제 화면과 같은 CSRF 흐름(쿠키 XSRF-TOKEN → 머리글 X-XSRF-TOKEN)을 쓰려고 진짜 CSRF 저장소로 띄운다 (SecurityAndCsrfTest와 같은 설정).
 */
@SpringBootTest
@Transactional
@Import(TestMembers.class)
@TestPropertySource(properties = "myblog.test.context=real-csrf")
@ExtendWith(RestDocumentationExtension.class)
class ApiDocumentationTest {

    private static final String CSRF_HEADER = "X-XSRF-TOKEN";

    @Autowired
    private TestMembers members;

    /** 빨간 펜의 AI는 가짜로 바꿔 끼운다 (실제 Claude API를 부르지 않는다) */
    @MockitoBean
    private Proofreader proofreader;

    @Autowired
    private StringRedisTemplate redis;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;

    @BeforeEach
    void setUp(WebApplicationContext context, RestDocumentationContextProvider restDocumentation) {
        mvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .apply(documentationConfiguration(restDocumentation)
                        .uris().withScheme("https").withHost("geulgyeol.java21.net").withPort(443)
                        .and().operationPreprocessors()
                        .withRequestDefaults(prettyPrint())
                        .withResponseDefaults(prettyPrint(), modifyHeaders().remove("Vary").remove("X-Content-Type-Options")
                                .remove("X-XSS-Protection").remove("Cache-Control").remove("Pragma").remove("Expires")
                                .remove("X-Frame-Options").remove("X-Instance")))
                .build();
    }

    // ── 1. 로그인 ──

    @Test
    void CSRF_토큰_받기() throws Exception {
        mvc.perform(RestDocumentationRequestBuilders.get("/api/v1/auth/csrf"))
                .andExpect(status().isOk())
                .andDo(document("auth-csrf", responseFields(
                        fieldWithPath("headerName").description("토큰을 실을 머리글 이름 (`X-XSRF-TOKEN`)"),
                        fieldWithPath("token").description("머리글에 실을 값. 같은 값이 쿠키 `XSRF-TOKEN`에도 들어온다"))));
    }

    @Test
    void 로그인() throws Exception {
        Member member = members.register("docs");
        Csrf csrf = csrf(null);
        mvc.perform(RestDocumentationRequestBuilders.post("/api/v1/auth/login")
                        .cookie(csrf.cookie()).header(CSRF_HEADER, csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", member.user().getEmail(), "password", TestMembers.PASSWORD)))
                .andExpect(status().isOk())
                .andDo(document("auth-login",
                        requestHeaders(csrfHeader()),
                        requestFields(
                                fieldWithPath("email").description("가입한 이메일"),
                                fieldWithPath("password").description("비밀번호. 정한 횟수만큼 틀리면 정한 시간 동안 잠긴다 (`auth.login.*`)")),
                        responseFields(memberFields())));
    }

    @Test
    void 지금_로그인한_회원() throws Exception {
        Member member = members.register("docs");
        MockHttpSession session = login(member);
        mvc.perform(RestDocumentationRequestBuilders.get("/api/v1/auth/me").session(session))
                .andExpect(status().isOk())
                .andDo(document("auth-me", responseFields(memberFields())));
    }

    @Test
    void 로그아웃() throws Exception {
        MockHttpSession session = login(members.register("docs"));
        mvc.perform(withCsrf(RestDocumentationRequestBuilders.post("/api/v1/auth/logout"), session))
                .andExpect(status().isNoContent())
                .andDo(document("auth-logout", requestHeaders(csrfHeader())));
    }

    // ── 2. 블로그·분류 ──

    @Test
    void 블로그_정보() throws Exception {
        Member member = members.register("docs");
        mvc.perform(RestDocumentationRequestBuilders.get("/api/v1/blogs/{blogId}", member.blogId()))
                .andExpect(status().isOk())
                .andDo(document("blog-read",
                        pathParameters(parameterWithName("blogId").description("블로그 번호")),
                        responseFields(
                                fieldWithPath("blogId").description("블로그 번호"),
                                fieldWithPath("name").description("블로그 이름 (1~30자)"),
                                fieldWithPath("intro").description("블로그 소개 (0~200자)").optional().type(JsonFieldType.STRING),
                                fieldWithPath("isOwner").description("보는 사람이 이 블로그의 주인인지"),
                                fieldWithPath("subscriberCount").description("이 블로그를 받아 보는 사람 수 (0.11.0)"),
                                fieldWithPath("subscribedByMe").description("보는 사람이 받아 보는지 (로그인하지 않았으면 false)"),
                                fieldWithPath("subscriptionEmail").description("보는 사람이 새 원고를 메일로도 받는지"))));
    }

    @Test
    void 분류_목록() throws Exception {
        Member member = members.register("docs");
        mvc.perform(RestDocumentationRequestBuilders.get("/api/v1/blogs/{blogId}/categories", member.blogId()))
                .andExpect(status().isOk())
                .andDo(document("category-list",
                        pathParameters(parameterWithName("blogId").description("블로그 번호")),
                        responseFields(fieldWithPath("categories[]").description("분류 목록 (주인이 정한 순서). 방문자에게는 비공개 분류가 빠진다"))
                                .andWithPrefix("categories[].", categoryFields())));
    }

    @Test
    void 분류_추가() throws Exception {
        MockHttpSession session = login(members.register("docs"));
        mvc.perform(withCsrf(RestDocumentationRequestBuilders.post("/api/v1/me/blog/categories"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "여행 기록", "visibility", "public")))
                .andExpect(status().isCreated())
                .andDo(document("category-create",
                        requestHeaders(csrfHeader()),
                        requestFields(
                                fieldWithPath("name").description("분류 이름 (1~20자, 내 블로그 안에서 겹치지 않게)"),
                                fieldWithPath("visibility").description("`public` 또는 `private`. 비공개 분류의 글은 주인만 본다")),
                        responseFields(categoryFields())));
    }

    // ── 3. 글 ──

    @Test
    void 글_쓰기() throws Exception {
        Member member = members.register("docs");
        MockHttpSession session = login(member);
        mvc.perform(withCsrf(RestDocumentationRequestBuilders.post("/api/v1/posts"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postJson(member, "제주 올레 7코스 걷기", "## 걷기 좋은 날\n\n바다를 보며 **3시간** 걸었다.",
                                "\"requestKey\":\"9f1c2e7a-4b3d-4c8e-9a51-2d6f0b7e1c33\",\"tags\":[\"제주\",\"걷기\"]")))
                .andExpect(status().isCreated())
                .andDo(document("post-create",
                        requestHeaders(csrfHeader()),
                        requestFields(postFields()).and(
                                fieldWithPath("requestKey").description("글쓰기 화면을 열 때 만든 1회용 번호 (36자까지). 같은 번호로 다시 보내면 새 글을 만들지 않고 200으로 같은 글 번호를 돌려준다").optional()),
                        responseFields(fieldWithPath("postId").description("글 번호"))));
    }

    @Test
    void 글_읽기() throws Exception {
        Member member = members.register("docs");
        Long postId = writePost(member, "제주 올레 7코스 걷기");
        mvc.perform(RestDocumentationRequestBuilders.get("/api/v1/posts/{postId}", postId))
                .andExpect(status().isOk())
                .andDo(document("post-read",
                        pathParameters(parameterWithName("postId").description("글 번호")),
                        responseFields(
                                fieldWithPath("postId").description("글 번호"),
                                fieldWithPath("blogId").description("블로그 번호"),
                                fieldWithPath("blogName").description("블로그 이름"),
                                fieldWithPath("category.categoryId").description("분류 번호"),
                                fieldWithPath("category.name").description("분류 이름"),
                                fieldWithPath("category.visibility").description("분류 공개 범위"),
                                fieldWithPath("topic.topicId").description("주제 번호"),
                                fieldWithPath("topic.name").description("주제 (여행·음식·취미·운동·개발)"),
                                fieldWithPath("title").description("제목"),
                                fieldWithPath("content").description("본문 (마크다운. HTML은 글자 그대로 보인다)"),
                                fieldWithPath("visibility").description("`public` 또는 `private`"),
                                fieldWithPath("createdAt").description("쓴 시각 (UTC, ISO 8601)"),
                                fieldWithPath("updatedAt").description("고친 시각. 고친 적 없으면 null").optional().type(JsonFieldType.STRING),
                                fieldWithPath("prevPostId").description("같은 블로그의 이전 글 번호 (보는 사람이 볼 수 있는 글 중)").optional().type(JsonFieldType.NUMBER),
                                fieldWithPath("nextPostId").description("다음 글 번호").optional().type(JsonFieldType.NUMBER),
                                fieldWithPath("isOwner").description("보는 사람이 글쓴이인지 (수정·삭제 버튼)"),
                                fieldWithPath("commentCount").description("댓글 수"),
                                fieldWithPath("likeCount").description("좋아요 수"),
                                fieldWithPath("likedByMe").description("보는 사람이 좋아요를 눌렀는지"),
                                fieldWithPath("tags[]").description("태그 (0~5개)"),
                                fieldWithPath("sheets").description("200자 원고지로 몇 매인지 (올림)"),
                                fieldWithPath("readMinutes").description("읽는 데 걸리는 분 (1분에 500자, 올림)"))));
    }

    @Test
    void 글_수정() throws Exception {
        Member member = members.register("docs");
        MockHttpSession session = login(member);
        Long postId = writePost(member, "제주 올레 7코스 걷기");
        mvc.perform(withCsrf(RestDocumentationRequestBuilders.put("/api/v1/posts/{postId}", postId), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postJson(member, "제주 올레 7코스 걷기 (사진 추가)", "## 걷기 좋은 날\n\n바다를 보며 **3시간** 걸었다.\n\n사진을 더했다.",
                                "\"tags\":[\"제주\",\"걷기\",\"바다\"]")))
                .andExpect(status().isOk())
                .andDo(document("post-update",
                        requestHeaders(csrfHeader()),
                        pathParameters(parameterWithName("postId").description("글 번호 (내 글만)")),
                        requestFields(postFields()),
                        responseFields(
                                fieldWithPath("postId").description("글 번호"),
                                fieldWithPath("changed").description("실제로 바뀐 것이 있는지. 같은 내용이면 false이고 고친 시각도 그대로"),
                                fieldWithPath("updatedAt").description("고친 시각").optional().type(JsonFieldType.STRING))));
    }

    @Test
    void 글_삭제() throws Exception {
        Member member = members.register("docs");
        MockHttpSession session = login(member);
        Long postId = writePost(member, "지울 글");
        mvc.perform(withCsrf(RestDocumentationRequestBuilders.delete("/api/v1/posts/{postId}", postId), session))
                .andExpect(status().isNoContent())
                .andDo(document("post-delete",
                        requestHeaders(csrfHeader()),
                        pathParameters(parameterWithName("postId").description("글 번호 (내 글만). 댓글·좋아요·이미지·태그도 함께 지운다"))));
    }

    // ── 4. 둘러보기·검색 ──

    @Test
    void 한_블로그의_글_목록() throws Exception {
        Member member = members.register("docs");
        writePost(member, "제주 올레 7코스 걷기");
        writePost(member, "성산일출봉 새벽 산책");
        mvc.perform(RestDocumentationRequestBuilders.get("/api/v1/blogs/{blogId}/posts", member.blogId()).param("page", "1"))
                .andExpect(status().isOk())
                .andDo(document("blog-posts",
                        pathParameters(parameterWithName("blogId").description("블로그 번호")),
                        queryParameters(
                                parameterWithName("page").description("쪽 번호 (1부터, 숫자가 아니면 1쪽)").optional(),
                                parameterWithName("categoryId").description("이 분류의 글만").optional()),
                        responseFields(pageFields(
                                fieldWithPath("blogId").description("블로그 번호"),
                                fieldWithPath("isOwner").description("보는 사람이 주인인지 (주인은 비공개 글까지 본다)"),
                                fieldWithPath("categoryId").description("고른 분류 번호. 없으면 null").optional().type(JsonFieldType.NUMBER)))
                                .and(fieldWithPath("posts[]").description("글 목록 (최신순)"))
                                .andWithPrefix("posts[].", listPostFields())
                                .and(fieldWithPath("posts[].visibility").description("`public` 또는 `private`"),
                                        fieldWithPath("posts[].sheets").description("원고지 매수"),
                                        fieldWithPath("posts[].readMinutes").description("읽는 시간(분)"))));
    }

    @Test
    void 글_검색() throws Exception {
        Member member = members.register("docs");
        writePost(member, "글결문서 예시: 제주 올레 7코스");
        mvc.perform(RestDocumentationRequestBuilders.get("/api/v1/search/posts").param("q", "글결문서").param("page", "1"))
                .andExpect(status().isOk())
                .andDo(document("post-search",
                        queryParameters(
                                parameterWithName("q").description("검색어 (2자 이상). 공개 글의 제목·본문에서 찾는다"),
                                parameterWithName("page").description("쪽 번호 (1부터)").optional()),
                        responseFields(pageFields(fieldWithPath("keyword").description("다듬은 검색어")))
                                .and(fieldWithPath("results[]").description("찾은 글 (최신순)"))
                                .andWithPrefix("results[].", listPostFields())
                                .and(fieldWithPath("results[].blogId").description("블로그 번호"),
                                        fieldWithPath("results[].blogName").description("블로그 이름"))));
    }

    @Test
    void 첫_화면_분량() throws Exception {
        mvc.perform(RestDocumentationRequestBuilders.get("/api/v1/posts/summary"))
                .andExpect(status().isOk())
                .andDo(document("post-summary", responseFields(
                        fieldWithPath("bloggerCount").description("공개 글이 있는 블로그 수"),
                        fieldWithPath("postCount").description("공개 글 수"),
                        fieldWithPath("sheets").description("공개 글 전체의 원고지 매수 (글마다 200자에 1매로 올림해 더함, 5분 동안 캐시)"))));
    }

    // ── 5. 댓글·좋아요 ──

    @Test
    void 댓글_쓰기() throws Exception {
        Member writer = members.register("docs");
        Long postId = writePost(writer, "제주 올레 7코스 걷기");
        MockHttpSession reader = login(members.register("read"));
        mvc.perform(withCsrf(RestDocumentationRequestBuilders.post("/api/v1/posts/{postId}/comments", postId), reader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("body", "저도 지난달에 걸었는데 바다 색이 정말 좋았어요.")))
                .andExpect(status().isCreated())
                .andDo(document("comment-create",
                        requestHeaders(csrfHeader()),
                        pathParameters(parameterWithName("postId").description("글 번호")),
                        requestFields(fieldWithPath("body").description("댓글 (1~500자). 같은 회원은 5초에 한 번. 수정은 없다")),
                        responseFields(commentFields())));
    }

    @Test
    void 답글_쓰기() throws Exception {
        Member writer = members.register("docs");
        Long postId = writePost(writer, "제주 올레 7코스 걷기");
        MockHttpSession reader = login(members.register("read"));
        String created = mvc.perform(withCsrf(RestDocumentationRequestBuilders.post("/api/v1/posts/{postId}/comments", postId), reader)
                        .contentType(MediaType.APPLICATION_JSON).content(json("body", "7코스 출발은 어디가 좋을까요?")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Number parentId = JsonPath.read(created, "$.id");
        mvc.perform(withCsrf(RestDocumentationRequestBuilders.post("/api/v1/posts/{postId}/comments", postId), login(writer))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"body\":\"외돌개 쪽에서 출발하면 오후 햇빛이 등 뒤라 편해요.\",\"parentId\":" + parentId
                                + ",\"secret\":true}"))
                .andExpect(status().isCreated())
                .andDo(document("comment-reply",
                        requestHeaders(csrfHeader()),
                        pathParameters(parameterWithName("postId").description("글 번호")),
                        requestFields(
                                fieldWithPath("body").description("답글 (1~500자)"),
                                fieldWithPath("parentId").description("답글을 달 댓글 번호. 답글을 가리키면 그 원댓글에 붙는다(한 단계). "
                                        + "없음·다른 글·지운 칸·읽을 수 없는 비밀 댓글이면 404"),
                                fieldWithPath("secret").description("비밀 댓글(봉한 쪽지)로 남길지. 빼면 거짓. 비밀 댓글에 단 답글은 늘 비밀")
                                        .optional().type(JsonFieldType.BOOLEAN)),
                        responseFields(commentFields())));
    }

    @Test
    void 댓글_목록() throws Exception {
        Member writer = members.register("docs");
        Long postId = writePost(writer, "제주 올레 7코스 걷기");
        MockHttpSession reader = login(members.register("read"));
        mvc.perform(withCsrf(RestDocumentationRequestBuilders.post("/api/v1/posts/{postId}/comments", postId), reader)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("body", "저도 지난달에 걸었는데 바다 색이 정말 좋았어요.")))
                .andExpect(status().isCreated());
        mvc.perform(RestDocumentationRequestBuilders.get("/api/v1/posts/{postId}/comments", postId))
                .andExpect(status().isOk())
                .andDo(document("comment-list",
                        pathParameters(parameterWithName("postId").description("글 번호")),
                        responseFields(
                                fieldWithPath("count").description("댓글 수"),
                                fieldWithPath("comments[]").description("댓글 (쓴 순서)"))
                                .andWithPrefix("comments[].", commentFields())));
    }

    @Test
    void 댓글_삭제() throws Exception {
        Member writer = members.register("docs");
        Long postId = writePost(writer, "제주 올레 7코스 걷기");
        MockHttpSession reader = login(members.register("read"));
        String created = mvc.perform(withCsrf(RestDocumentationRequestBuilders.post("/api/v1/posts/{postId}/comments", postId), reader)
                        .contentType(MediaType.APPLICATION_JSON).content(json("body", "지울 댓글")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Number commentId = JsonPath.read(created, "$.id");
        mvc.perform(withCsrf(RestDocumentationRequestBuilders.delete("/api/v1/comments/{commentId}", commentId), reader))
                .andExpect(status().isNoContent())
                .andDo(document("comment-delete",
                        requestHeaders(csrfHeader()),
                        pathParameters(parameterWithName("commentId").description("댓글 번호. 쓴 사람이나 그 블로그 주인만 지운다 (아니면 404)"))));
    }

    @Test
    void 좋아요() throws Exception {
        Member writer = members.register("docs");
        Long postId = writePost(writer, "제주 올레 7코스 걷기");
        MockHttpSession reader = login(members.register("read"));
        mvc.perform(withCsrf(RestDocumentationRequestBuilders.put("/api/v1/posts/{postId}/like", postId), reader))
                .andExpect(status().isOk())
                .andDo(document("post-like",
                        requestHeaders(csrfHeader()),
                        pathParameters(parameterWithName("postId").description("글 번호. 자기 글에는 누를 수 없다")),
                        responseFields(
                                fieldWithPath("likeCount").description("좋아요 수"),
                                fieldWithPath("likedByMe").description("내가 눌렀는지 (여러 번 눌러도 한 번으로 센다)"))));
    }

    // ── 6. 블로그 관리 ──

    @Test
    void 대시보드() throws Exception {
        Member member = members.register("docs");
        MockHttpSession session = login(member);
        writePost(member, "제주 올레 7코스 걷기");
        mvc.perform(RestDocumentationRequestBuilders.get("/api/v1/manage/dashboard").session(session))
                .andExpect(status().isOk())
                .andDo(document("manage-dashboard", responseFields(
                        fieldWithPath("views.today").description("오늘 조회수"),
                        fieldWithPath("views.yesterday").description("어제 조회수"),
                        fieldWithPath("views.total").description("누적 조회수"),
                        fieldWithPath("visitors.today").description("오늘 방문자"),
                        fieldWithPath("visitors.yesterday").description("어제 방문자"),
                        fieldWithPath("visitors.total").description("누적 방문자"),
                        fieldWithPath("newCommentCount").description("새 댓글 수 (댓글 관리를 마지막으로 연 뒤 남이 쓴 것)"),
                        fieldWithPath("chart[]").description("최근 날짜별 그래프 (오래된 날부터)"),
                        fieldWithPath("chart[].date").description("날짜 (한국 시간 기준)"),
                        fieldWithPath("chart[].views").description("그날 조회수"),
                        fieldWithPath("chart[].visitors").description("그날 방문자"),
                        fieldWithPath("popularPosts[]").description("최근 7일 조회수가 많은 글"),
                        fieldWithPath("recentPosts[]").description("최근에 쓴 글"),
                        fieldWithPath("recentPosts[].postId").description("글 번호"),
                        fieldWithPath("recentPosts[].title").description("제목"),
                        fieldWithPath("recentPosts[].createdAt").description("쓴 시각"),
                        fieldWithPath("recentPosts[].visibility").description("공개 범위"))));
    }

    // ── 7. 공지사항 ──

    private Long insertNotice(Member writer, String kind, String title, String version, boolean pinned) {
        return jdbc.queryForObject("insert into notice (kind, title, content, release_version, pinned, users_id, created_at)"
                + " values (?, ?, ?, ?, ?, ?, now()) returning notice_id", Long.class,
                kind, title, "## 무엇이 바뀌었나\n\n- 공지사항 게시판이 생겼어요", version, pinned, writer.id());
    }

    @Test
    void 공지사항_쓰기() throws Exception {
        Member admin = members.register("docs");
        jdbc.update("update users set role = 'admin' where users_id = ?", admin.id());
        MockHttpSession session = login(admin);
        mvc.perform(withCsrf(RestDocumentationRequestBuilders.post("/api/v1/notices"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"kind":"update","title":"공지사항 게시판이 생겼어요","content":"## 무엇이 바뀌었나\\n\\n- 업데이트와 공지를 한곳에서",                                "releaseVersion":"0.6.0","pinned":true}"""))
                .andExpect(status().isCreated())
                .andDo(document("notice-create",
                        requestHeaders(csrfHeader()),
                        requestFields(
                                fieldWithPath("kind").description("`update`(업데이트) 또는 `notice`(공지)"),
                                fieldWithPath("title").description("제목 (1~100자)"),
                                fieldWithPath("content").description("본문 마크다운 (1~10,000자)"),
                                fieldWithPath("releaseVersion").description("릴리스 번호 `0.6.0` 모양. 업데이트에만 넣는다")
                                        .optional().type(JsonFieldType.STRING),
                                fieldWithPath("pinned").description("목록 맨 위에 고정할지")),
                        responseFields(fieldWithPath("noticeId").description("만든 공지사항 번호"))));
    }

    @Test
    void 공지사항_목록() throws Exception {
        Member admin = members.register("docs");
        jdbc.update("delete from notice");
        insertNotice(admin, "notice", "서버 점검 안내", null, false);
        insertNotice(admin, "update", "공지사항 게시판이 생겼어요", "0.6.0", true);
        mvc.perform(RestDocumentationRequestBuilders.get("/api/v1/notices").param("kind", "").param("page", "1"))
                .andExpect(status().isOk())
                .andDo(document("notice-list",
                        queryParameters(
                                parameterWithName("kind").description("탭: `update`, `notice`, 비우면 전체").optional(),
                                parameterWithName("page").description("쪽 번호 (1부터, 한 쪽 10개)").optional()),
                        responseFields(
                                fieldWithPath("items[].noticeId").description("번호"),
                                fieldWithPath("items[].kind").description("`update` 또는 `notice`"),
                                fieldWithPath("items[].title").description("제목"),
                                fieldWithPath("items[].releaseVersion").description("릴리스 번호 (업데이트만, 공지는 null)")
                                        .optional().type(JsonFieldType.STRING),
                                fieldWithPath("items[].pinned").description("고정 글인지 (고정 먼저, 그 안에서 새 글 먼저)"),
                                fieldWithPath("items[].createdAt").description("쓴 시각"),
                                fieldWithPath("page").description("지금 쪽"),
                                fieldWithPath("totalPages").description("마지막 쪽"),
                                fieldWithPath("totalCount").description("글 수"))));
    }

    @Test
    void 공지사항_읽기() throws Exception {
        Member admin = members.register("docs");
        Long noticeId = insertNotice(admin, "update", "공지사항 게시판이 생겼어요", "0.6.0", true);
        mvc.perform(RestDocumentationRequestBuilders.get("/api/v1/notices/{noticeId}", noticeId))
                .andExpect(status().isOk())
                .andDo(document("notice-read",
                        pathParameters(parameterWithName("noticeId").description("공지사항 번호")),
                        responseFields(
                                fieldWithPath("noticeId").description("번호"),
                                fieldWithPath("kind").description("`update` 또는 `notice`"),
                                fieldWithPath("title").description("제목"),
                                fieldWithPath("content").description("본문 마크다운"),
                                fieldWithPath("releaseVersion").description("릴리스 번호 (업데이트만)").optional()
                                        .type(JsonFieldType.STRING),
                                fieldWithPath("releaseUrl").description("그 번호의 GitHub 릴리스 주소 (업데이트만)").optional()
                                        .type(JsonFieldType.STRING),
                                fieldWithPath("pinned").description("고정 글인지"),
                                fieldWithPath("createdAt").description("쓴 시각"),
                                fieldWithPath("updatedAt").description("고친 시각. 고친 적이 없으면 null").optional()
                                        .type(JsonFieldType.STRING))));
    }

    @Test
    void 공지사항_최신_시각() throws Exception {
        insertNotice(members.register("docs"), "notice", "서버 점검 안내", null, false);
        mvc.perform(RestDocumentationRequestBuilders.get("/api/v1/notices/latest"))
                .andExpect(status().isOk())
                .andDo(document("notice-latest", responseFields(
                        fieldWithPath("latestAt").description("가장 새 공지사항을 쓴 시각. 글이 없으면 null")
                                .optional().type(JsonFieldType.STRING))));
    }

    // ── 7-2. 글쓰기 도구 (013) ──

    @Test
    void 글쓰기_잔디() throws Exception {
        Member member = members.register("docs");
        writePost(member, "오늘 쓴 글");
        mvc.perform(RestDocumentationRequestBuilders.get("/api/v1/blogs/{blogId}/activity", member.blogId()))
                .andExpect(status().isOk())
                .andDo(document("writing-activity",
                        pathParameters(parameterWithName("blogId").description("블로그 번호")),
                        responseFields(
                                fieldWithPath("blogId").description("블로그 번호"),
                                fieldWithPath("from").description("첫날 (한국 날짜, 오늘 포함 365일)"),
                                fieldWithPath("to").description("오늘 (한국 날짜)"),
                                fieldWithPath("days[]").description("공개 글을 쓴 날만, 날짜 순"),
                                fieldWithPath("days[].date").description("날짜"),
                                fieldWithPath("days[].postCount").description("그날 쓴 공개 글 수"),
                                fieldWithPath("days[].sheets").description("그날 쓴 원고지 매수"),
                                fieldWithPath("totalPosts").description("기간의 공개 글 수"),
                                fieldWithPath("totalSheets").description("기간의 원고지 매수"))));
    }

    @Test
    void 연재_만들기() throws Exception {
        Member member = members.register("docs");
        MockHttpSession session = login(member);
        mvc.perform(withCsrf(RestDocumentationRequestBuilders.post("/api/v1/me/blog/series"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("name", "제주 한 달 살기", "description", "한 달 동안 걸은 길")))
                .andExpect(status().isCreated())
                .andDo(document("series-create",
                        requestHeaders(csrfHeader()),
                        requestFields(
                                fieldWithPath("name").description("연재 이름, 1~30자"),
                                fieldWithPath("description").description("소개, 0~200자")),
                        responseFields(seriesFields())));
    }

    @Test
    void 글의_연재_정보() throws Exception {
        Member member = members.register("docs");
        Long seriesId = jdbc.queryForObject("insert into series (blog_id, name, created_at) values (?, '제주 한 달 살기', now())"
                + " returning series_id", Long.class, member.blogId());
        Long first = writePost(member, "첫날");
        Long second = writePost(member, "둘째 날");
        jdbc.update("insert into series_post (series_id, post_id, sort_order) values (?, ?, 1), (?, ?, 2)",
                seriesId, first, seriesId, second);
        mvc.perform(RestDocumentationRequestBuilders.get("/api/v1/posts/{postId}/series", second))
                .andExpect(status().isOk())
                .andDo(document("post-series",
                        pathParameters(parameterWithName("postId").description("글 번호")),
                        responseFields(
                                fieldWithPath("series").description("연재. 연재에 없으면 null"),
                                fieldWithPath("series.seriesId").description("연재 번호"),
                                fieldWithPath("series.name").description("연재 이름"),
                                fieldWithPath("series.episode").description("이 글이 몇 화인지 (보는 사람에게 보이는 글로 센다)"),
                                fieldWithPath("series.total").description("보이는 글 수"),
                                fieldWithPath("series.previous").description("이전 화. 첫 화면 null").optional().type(JsonFieldType.OBJECT),
                                fieldWithPath("series.previous.postId").description("이전 화 글 번호").optional(),
                                fieldWithPath("series.previous.title").description("이전 화 제목").optional(),
                                fieldWithPath("series.next").description("다음 화. 끝 화면 null").optional().type(JsonFieldType.OBJECT),
                                fieldWithPath("series.posts[]").description("연재 전체 (화 순서)"),
                                fieldWithPath("series.posts[].postId").description("글 번호"),
                                fieldWithPath("series.posts[].title").description("제목"),
                                fieldWithPath("series.posts[].episode").description("화 번호"))));
    }

    @Test
    void 임시_저장_예약() throws Exception {
        Member member = members.register("docs");
        MockHttpSession session = login(member);
        String created = mvc.perform(withCsrf(RestDocumentationRequestBuilders.post("/api/v1/me/drafts"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("title", "쓰는 중", "content", "")))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        Long draftId = ((Number) JsonPath.read(created, "$.draftId")).longValue();
        String publishAt = Instant.now().plusSeconds(86_400).truncatedTo(java.time.temporal.ChronoUnit.MINUTES).toString();
        mvc.perform(withCsrf(RestDocumentationRequestBuilders.put("/api/v1/me/drafts/{draftId}/schedule", draftId), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postJson(member, "내일 아침 9시에 올릴 글", "## 미리 써 둔 글", "\"publishAt\":\"" + publishAt + "\"")))
                .andExpect(status().isOk())
                .andDo(document("draft-schedule",
                        pathParameters(parameterWithName("draftId").description("임시 저장 글 번호")),
                        requestHeaders(csrfHeader()),
                        requestFields(
                                fieldWithPath("title").description("제목 (보통 글쓰기와 같은 검사)"),
                                fieldWithPath("content").description("본문 (마크다운)"),
                                fieldWithPath("categoryId").description("분류 번호 (내 블로그의 것)"),
                                fieldWithPath("topicId").description("주제 번호"),
                                fieldWithPath("visibility").description("`public` 또는 `private`"),
                                fieldWithPath("publishAt").description("올릴 시각. 초 아래는 버린다. 지금부터 1분 뒤 ~ 30일 안")),
                        responseFields(
                                fieldWithPath("draftId").description("임시 저장 글 번호"),
                                fieldWithPath("updatedAt").description("마지막 저장 시각"),
                                fieldWithPath("publishAt").description("예약 시각. 비어 있으면 임시 저장"))));
    }

    private static FieldDescriptor[] seriesFields() {
        return new FieldDescriptor[] {
            fieldWithPath("seriesId").description("연재 번호"),
            fieldWithPath("blogId").description("블로그 번호"),
            fieldWithPath("name").description("연재 이름"),
            fieldWithPath("description").description("소개"),
            fieldWithPath("posts[]").description("보이는 글을 화 순서로 (새 연재는 비어 있다)")
        };
    }

    // ── 8. 오류 ──

    @Test
    void 오류_입력값() throws Exception {
        Member member = members.register("docs");
        MockHttpSession session = login(member);
        mvc.perform(withCsrf(RestDocumentationRequestBuilders.post("/api/v1/posts"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(postJson(member, "", "본문", null)))
                .andExpect(status().isBadRequest())
                .andDo(document("error-validation", responseFields(
                        fieldWithPath("code").description("오류 종류 (화면은 이 값으로 나눈다)"),
                        fieldWithPath("message").description("사람이 읽는 안내 문구"),
                        fieldWithPath("fieldErrors[]").description("칸마다 틀린 곳 (입력값 오류일 때만)"),
                        fieldWithPath("fieldErrors[].field").description("칸 이름"),
                        fieldWithPath("fieldErrors[].code").description("틀린 까닭"),
                        fieldWithPath("fieldErrors[].message").description("칸 옆에 보일 문구"))));
    }

    @Test
    void 오류_로그인_필요() throws Exception {
        mvc.perform(RestDocumentationRequestBuilders.get("/api/v1/manage/dashboard"))
                .andExpect(status().isUnauthorized())
                .andDo(document("error-unauthenticated", responseFields(errorFields())));
    }

    @Test
    void 오류_없는_글() throws Exception {
        mvc.perform(RestDocumentationRequestBuilders.get("/api/v1/posts/{postId}", 999_999_999))
                .andExpect(status().isNotFound())
                .andDo(document("error-not-found", responseFields(errorFields())));
    }

    // ── 칸 설명 묶음 ──

    private static FieldDescriptor[] memberFields() {
        return new FieldDescriptor[] {
            fieldWithPath("member.id").description("회원 번호"),
            fieldWithPath("member.nickname").description("닉네임 (2~10자, 한글·영문·숫자)"),
            fieldWithPath("member.admin").description("관리자인지. 관리자만 공지사항을 쓴다")};
    }

    private static FieldDescriptor[] categoryFields() {
        return new FieldDescriptor[] {
            fieldWithPath("categoryId").description("분류 번호"),
            fieldWithPath("name").description("분류 이름"),
            fieldWithPath("postCount").description("이 분류의 글 수 (보는 사람이 볼 수 있는 글만)"),
            fieldWithPath("isDefault").description("\"미분류\"인지 (지울 수 없다)"),
            fieldWithPath("visibility").description("`public` 또는 `private`"),
            fieldWithPath("colorIndex").description("화면에서 쓸 분류 색 번호 (0~5)")};
    }

    private static FieldDescriptor[] postFields() {
        return new FieldDescriptor[] {
            fieldWithPath("title").description("제목 (1~100자)"),
            fieldWithPath("content").description("본문 마크다운 (1~10,000자)"),
            fieldWithPath("categoryId").description("내 블로그의 분류 번호"),
            fieldWithPath("topicId").description("주제 번호 (여행·음식·취미·운동·개발)"),
            fieldWithPath("visibility").description("`public` 또는 `private`"),
            fieldWithPath("tags[]").description("태그 (0~5개). 보낸 목록이 새 전체 목록이다").optional()};
    }

    private static FieldDescriptor[] listPostFields() {
        return new FieldDescriptor[] {
            fieldWithPath("postId").description("글 번호"),
            fieldWithPath("title").description("제목"),
            fieldWithPath("categoryId").description("분류 번호"),
            fieldWithPath("categoryName").description("분류 이름"),
            fieldWithPath("createdAt").description("쓴 시각"),
            fieldWithPath("preview").description("본문 앞부분 (마크다운 기호를 뺀 글자)"),
            fieldWithPath("thumbnail").description("본문의 첫 이미지 주소. 없으면 null").optional().type(JsonFieldType.STRING)};
    }

    private static FieldDescriptor[] pageFields(FieldDescriptor... first) {
        FieldDescriptor[] page = {
            fieldWithPath("totalCount").description("전체 글 수"),
            fieldWithPath("page").description("지금 쪽 번호"),
            fieldWithPath("totalPages").description("전체 쪽 수"),
            fieldWithPath("pageSize").description("한 쪽의 글 수")};
        FieldDescriptor[] all = new FieldDescriptor[first.length + page.length];
        System.arraycopy(first, 0, all, 0, first.length);
        System.arraycopy(page, 0, all, first.length, page.length);
        return all;
    }

    private static FieldDescriptor[] commentFields() {
        return new FieldDescriptor[] {
            fieldWithPath("id").description("댓글 번호"),
            fieldWithPath("author.id").description("쓴 회원 번호"),
            fieldWithPath("author.nickname").description("쓴 회원 닉네임 (탈퇴했으면 \"탈퇴한 회원\")"),
            fieldWithPath("author.withdrawn").description("쓴 회원이 탈퇴했는지"),
            fieldWithPath("body").description("댓글"),
            fieldWithPath("createdAt").description("쓴 시각"),
            fieldWithPath("canDelete").description("보는 사람이 지울 수 있는지 (쓴 사람이나 블로그 주인). 지운 칸이면 거짓"),
            fieldWithPath("parentId").description("원댓글 번호 (답글이면). 원댓글이면 null").optional()
                    .type(JsonFieldType.NUMBER),
            fieldWithPath("secret").description("비밀 댓글(봉한 쪽지)인지"),
            fieldWithPath("sealed").description("보는 사람이 읽을 수 없는 비밀 댓글인지. 참이면 body는 빈 글자, author는 빈 값"),
            fieldWithPath("deleted").description("답글이 남아 줄만 남은 지운 칸인지. 참이면 body는 빈 글자, author는 빈 값"),
            fieldWithPath("canReply").description("보는 사람이 이 댓글(답글이면 그 원댓글)에 답글을 달 수 있는지")};
    }

    private static FieldDescriptor[] errorFields() {
        return new FieldDescriptor[] {
            fieldWithPath("code").description("오류 종류"),
            fieldWithPath("message").description("사람이 읽는 안내 문구")};
    }

    private static org.springframework.restdocs.headers.HeaderDescriptor csrfHeader() {
        return headerWithName(CSRF_HEADER).description("CSRF 토큰 (`GET /api/v1/auth/csrf`로 받은 값, 쿠키 `XSRF-TOKEN`과 함께 보낸다)");
    }

    // ── 준비 ──

    private record Csrf(Cookie cookie, String token) {
    }

    /** 화면처럼 CSRF 토큰을 받는다. 로그인하면 토큰이 바뀌므로 로그인한 세션으로 다시 받는다. */
    private Csrf csrf(MockHttpSession session) throws Exception {
        MockHttpServletRequestBuilder request = org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                .get("/api/v1/auth/csrf");
        if (session != null) {
            request.session(session);
        }
        var response = mvc.perform(request).andExpect(status().isOk()).andReturn().getResponse();
        String token = JsonPath.read(response.getContentAsString(), "$.token");
        return new Csrf(response.getCookie("XSRF-TOKEN"), token);
    }

    private MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder request, MockHttpSession session)
            throws Exception {
        Csrf csrf = csrf(session);
        return request.session(session).cookie(csrf.cookie()).header(CSRF_HEADER, csrf.token());
    }

    private MockHttpSession login(Member member) throws Exception {
        Csrf csrf = csrf(null);
        return (MockHttpSession) mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/api/v1/auth/login").cookie(csrf.cookie()).header(CSRF_HEADER, csrf.token())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("email", member.user().getEmail(), "password", TestMembers.PASSWORD)))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    /** 글을 표에 바로 넣는다 (문서에 나오지 않는 준비). */
    // ── 9. AI (specs/014) ──

    @Test
    void 빨간_펜() throws Exception {
        Member member = members.register("docs");
        MockHttpSession session = login(member);
        String content = "오늘은 할수있다고 믿었다. 몇일 동안 연습했다.";
        when(proofreader.available()).thenReturn(true);
        when(proofreader.mode()).thenReturn("ai");
        when(proofreader.proofread(anyString())).thenReturn(List.of(
                new Suggestion("spacing", "오늘은 ", "할수있다고", "할 수 있다고", "의존 명사 '수'는 띄어 씁니다"),
                new Suggestion("spelling", "믿었다. ", "몇일", "며칠", "'며칠'이 표준어입니다")));
        // 이 예시 본문의 앞 결과만 지워 예시가 늘 같게 한다 (다른 본문의 보관 결과는 건드리지 않는다)
        redis.delete("proofread:result:" + HexFormat.of().formatHex(
                MessageDigest.getInstance("SHA-256").digest(content.getBytes(StandardCharsets.UTF_8))));
        mvc.perform(withCsrf(RestDocumentationRequestBuilders.post("/api/v1/me/proofread"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"" + content + "\",\"aiConsent\":true}"))
                .andExpect(status().isOk())
                .andDo(document("proofread",
                        requestHeaders(csrfHeader()),
                        requestFields(fieldWithPath("content").description("교정받을 마크다운 본문 (1~10,000자)"),
                                fieldWithPath("aiConsent").description("본문이 바깥 AI로 보내진다는 안내에 동의했는지."
                                        + " 상태의 mode가 ai인데 참이 아니면 본문을 보내지 않고 409 `PROOFREAD_CONSENT_REQUIRED` (규칙 교정은 보지 않는다)")),
                        responseFields(
                                fieldWithPath("corrections[]").description("고칠 곳. 본문 앞에서부터, 겹치지 않는다"),
                                fieldWithPath("corrections[].start").description("본문에서 시작 위치 (UTF-16, 자바스크립트 문자열 위치)"),
                                fieldWithPath("corrections[].end").description("끝 위치 (이 위치는 빼고)"),
                                fieldWithPath("corrections[].kind").description("`spacing`(띄어쓰기) · `spelling`(맞춤법) · `style`(다듬기)"),
                                fieldWithPath("corrections[].original").description("본문의 원문 (start~end 그대로)"),
                                fieldWithPath("corrections[].replacement").description("고친 말"),
                                fieldWithPath("corrections[].reason").description("고친 까닭"),
                                fieldWithPath("remaining").description("오늘(한국 날짜) 남은 횟수. 규칙 교정(`rules`)은 횟수를 세지 않아 늘 하루 한도"),
                                fieldWithPath("cached").description("같은 본문의 앞 결과를 다시 썼는지 (횟수에 넣지 않음. 규칙 교정은 늘 false)"))));
    }

    @Test
    void 비슷한_글() throws Exception {
        Member member = members.register("docs");
        Long postId = writePost(member, "제주 렌터카 여행 후기");
        writePost(member, "제주 버스 여행 팁");
        mvc.perform(RestDocumentationRequestBuilders.get("/api/v1/posts/{postId}/related", postId))
                .andExpect(status().isOk())
                .andDo(document("related-posts",
                        pathParameters(parameterWithName("postId").description("글 번호")),
                        responseFields(
                                fieldWithPath("basis").description("`meaning`(뜻이 비슷한 글, 임베딩) 또는 `tags`(태그·주제가 겹치는 글로 대신)"),
                                subsectionWithPath("posts[]").description("비슷한 글 4개까지. 첫 화면 글 모음과 같은 카드 한 줄"))));
    }

    // ── 10. 발견 (specs/015) ──

    @Test
    void 인기_검색어() throws Exception {
        Member member = members.register("docs");
        writePost(member, "제주 렌터카 여행 후기");
        // 로그인한 회원 둘이 찾아야 순위에 보인다 (stats.search-trend.min-count)
        for (int i = 0; i < 2; i++) {
            mvc.perform(RestDocumentationRequestBuilders.get("/api/v1/search/posts").param("q", "렌터카")
                    .session(login(members.register("docs")))).andExpect(status().isOk());
        }
        mvc.perform(RestDocumentationRequestBuilders.get("/api/v1/search/trending"))
                .andExpect(status().isOk())
                .andDo(document("search-trending",
                        responseFields(
                                fieldWithPath("items[]").description("최근 1시간 순위, 10개까지. 모인 검색어가 없으면 빈 목록"),
                                fieldWithPath("items[].rank").description("순위 (1부터)"),
                                fieldWithPath("items[].keyword").description("검색어 (앞뒤 공백을 지우고 공백은 한 칸, 영문은 소문자)"),
                                fieldWithPath("items[].change").description("10분 전과 견준 변화: `up` · `down` · `new` · `same`"),
                                fieldWithPath("items[].delta").description("오르거나 내린 칸 수 (`new`·`same`이면 0)"))));
    }

    @Test
    void 오늘의_이슈() throws Exception {
        mvc.perform(RestDocumentationRequestBuilders.get("/api/v1/posts/today-issue"))
                .andExpect(status().isOk())
                .andDo(document("today-issue",
                        responseFields(
                                fieldWithPath("date").description("고른 날 (한국 날짜)"),
                                subsectionWithPath("posts[]").optional()
                                        .description("오늘과 어제 많이 읽힌 공개 글, 블로그마다 하나, 3개까지. 첫 화면 글 모음과 같은 카드 한 줄"))));
    }

    @Test
    void 문장_수집() throws Exception {
        Member member = members.register("docs");
        Long postId = writePost(member, "바닷가 산책");
        Member reader = members.register("docs");
        MockHttpSession session = login(reader);
        mvc.perform(withCsrf(RestDocumentationRequestBuilders.post("/api/v1/me/clips"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"postId\":" + postId + ",\"text\":\"바다를 보며 3시간 걸었다.\"}"))
                .andExpect(status().isCreated())
                .andDo(document("clip-collect",
                        requestHeaders(csrfHeader()),
                        requestFields(fieldWithPath("postId").description("문장을 고른 글 번호 (볼 수 없는 글이면 404)"),
                                fieldWithPath("text").description("고른 문장 1~300자. 본문에 실제로 있어야 한다"
                                        + " (마크다운 기호·링크 주소·띄어쓰기는 걷어 내고 견준다, 아니면 400 `CLIP_NOT_IN_POST`)")),
                        responseFields(
                                fieldWithPath("clipId").description("모은 문장 번호. 같은 글의 같은 문장을 다시 보내면 앞의 것(200)"),
                                fieldWithPath("text").description("옮겨 적은 문장 (공백을 한 칸으로)"),
                                fieldWithPath("createdAt").description("모은 시각"),
                                fieldWithPath("post.postId").description("원문 글 번호 (글이 지워지면 null)"),
                                fieldWithPath("post.title").description("모을 때의 글 제목"),
                                fieldWithPath("post.blogName").description("모을 때의 블로그 이름"),
                                fieldWithPath("post.available").description("지금 원문을 볼 수 있는지 (비공개가 되거나 지워지면 false)"))));
    }

    // ── 11. 구독·알림 (specs/016) ──

    @Test
    void 원고_받아_보기() throws Exception {
        Member writer = members.register("docs");
        Member reader = members.register("docs");
        MockHttpSession session = login(reader);
        mvc.perform(withCsrf(RestDocumentationRequestBuilders.put("/api/v1/blogs/{blogId}/subscription", writer.blogId()),
                        session).contentType(MediaType.APPLICATION_JSON).content("{\"emailEnabled\":true}"))
                .andExpect(status().isOk())
                .andDo(document("subscription-put",
                        pathParameters(parameterWithName("blogId").description("받아 볼 블로그 번호")),
                        requestHeaders(csrfHeader()),
                        requestFields(fieldWithPath("emailEnabled").description("새 원고를 메일로도 받기 (본문이 없으면 false)."
                                + " 이미 받아 보면 이것만 바꾼다")),
                        responseFields(
                                fieldWithPath("subscribed").description("받아 보는 중인가"),
                                fieldWithPath("emailEnabled").description("메일로도 받는가"),
                                fieldWithPath("subscriberCount").description("이 블로그를 받아 보는 사람 수"))));
    }

    @Test
    void 우편함() throws Exception {
        Member reader = members.register("docs");
        MockHttpSession session = login(reader);
        jdbc.update("insert into notification (users_id, kind, post_id, post_title, actor_name, created_at)"
                + " values (?, 'post', null, '바닷가 산책', '바다 일기', now())", reader.id());
        mvc.perform(RestDocumentationRequestBuilders.get("/api/v1/me/notifications").session(session))
                .andExpect(status().isOk())
                .andDo(document("notifications",
                        responseFields(
                                fieldWithPath("items[]").description("소식, 최근 순 20개씩 (30일 보관)"),
                                fieldWithPath("items[].notificationId").description("소식 번호"),
                                fieldWithPath("items[].kind").description("`post`(받아 보는 블로그의 새 원고) · `comment`(내 글에 달린 댓글)"
                                        + " · `board`(내 글마당 글에 달린 댓글) · `accepted`(내 글마당 댓글이 채택됨)"
                                        + " · `editorial`(편집실이 내 글을 내리거나 지움) · `reply`(내 댓글에 답글)"),
                                fieldWithPath("items[].postId").description("블로그 글 번호 (글마당 소식이거나 지금 볼 수 없는 글이면 null)").optional(),
                                fieldWithPath("items[].boardPostId").description("글마당 글 번호 (블로그 글 소식이거나 지금 볼 수 없는 글이면 null)").optional(),
                                fieldWithPath("items[].postTitle").description("그때의 글 제목 (지금 볼 수 없는 글이면 null)").optional(),
                                fieldWithPath("items[].actorName").description("새 원고면 블로그 이름, 댓글이면 쓴 사람 닉네임, 편집실 소식이면 \"편집실\""),
                                fieldWithPath("items[].note").description("편집실 소식의 사유 (다른 소식은 null)").optional(),
                                fieldWithPath("items[].available").description("지금 그 글을 볼 수 있는가"),
                                fieldWithPath("items[].read").description("읽었는가"),
                                fieldWithPath("items[].createdAt").description("소식이 온 시각"),
                                fieldWithPath("page").description("쪽 번호"),
                                fieldWithPath("totalPages").description("모든 쪽 수"),
                                fieldWithPath("unreadCount").description("안 읽은 소식 수"))));
    }

    // ── 12. 글마당 (specs/017) ──

    @Test
    void 글마당_목록() throws Exception {
        Member writer = members.register("docs");
        jdbc.update("insert into board_post (users_id, topic_id, kind, title, content, created_at, updated_at)"
                + " values (?, (select min(topic_id) from topic), 'question', '제주 렌터카 vs 대중교통', '어느 쪽이 나을까요?', now(), now())",
                writer.id());
        mvc.perform(RestDocumentationRequestBuilders.get("/api/v1/board/posts").param("kind", "question").param("page", "1"))
                .andExpect(status().isOk())
                .andDo(document("board-list",
                        queryParameters(
                                parameterWithName("topic").description("주제 이름 (없으면 모두, 없는 주제는 400)").optional(),
                                parameterWithName("kind").description("`talk`(이야기) · `question`(물음), 없으면 모두").optional(),
                                parameterWithName("unsolved").description("`true`면 채택 전인 물음만").optional(),
                                parameterWithName("q").description("검색어. 단어마다 제목·본문·주제·쓴 사람 가운데 어딘가에 있는 글 (50자까지)").optional(),
                                parameterWithName("page").description("쪽 번호 (1부터, 넘으면 마지막 쪽)").optional()),
                        responseFields(
                                fieldWithPath("topics[]").description("주제 탭에 쓸 이름 (정해진 순서)"),
                                fieldWithPath("topic").description("고른 주제 (없으면 null)").optional(),
                                fieldWithPath("kind").description("고른 종류 (없으면 null)").optional(),
                                fieldWithPath("unsolved").description("채택 전 물음만 골랐는가"),
                                fieldWithPath("keyword").description("실제로 찾은 검색어 (없으면 null)").optional(),
                                fieldWithPath("totalCount").description("고른 글 수"),
                                fieldWithPath("page").description("쪽 번호"),
                                fieldWithPath("totalPages").description("모든 쪽 수"),
                                fieldWithPath("pageSize").description("한 쪽 글 수 (`board.page-size`)"),
                                fieldWithPath("posts[]").description("최신순"),
                                fieldWithPath("posts[].boardPostId").description("글 번호"),
                                fieldWithPath("posts[].topic").description("주제 이름"),
                                fieldWithPath("posts[].kind").description("`talk` · `question`"),
                                fieldWithPath("posts[].title").description("제목"),
                                subsectionWithPath("posts[].writer").description("쓴 사람 `{ id, nickname, withdrawn }` (탈퇴했으면 id·nickname이 null)"),
                                fieldWithPath("posts[].createdAt").description("쓴 시각"),
                                fieldWithPath("posts[].views").description("조회 수 (같은 사람 30분에 한 번)"),
                                fieldWithPath("posts[].commentCount").description("댓글 수"),
                                fieldWithPath("posts[].solved").description("채택한 댓글이 있는 물음인가"))));
    }

    @Test
    void 글마당_글쓰기와_채택() throws Exception {
        Member writer = members.register("docs");
        Member answerer = members.register("docs");
        MockHttpSession session = login(writer);
        Long topicId = jdbc.queryForObject("select min(topic_id) from topic", Long.class);
        String topic = jdbc.queryForObject("select topic_name from topic where topic_id = ?", String.class, topicId);
        mvc.perform(withCsrf(RestDocumentationRequestBuilders.post("/api/v1/board/posts"), session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("topic", topic, "kind", "question", "title", "NPE가 나요", "content", "어디서 **null**이 오나요?")))
                .andExpect(status().isCreated())
                .andDo(document("board-write",
                        requestHeaders(csrfHeader()),
                        requestFields(
                                fieldWithPath("topic").description("주제 이름"),
                                fieldWithPath("kind").description("`talk`(이야기) · `question`(물음)"),
                                fieldWithPath("title").description("제목 1~100자 (앞뒤 공백은 지운다)"),
                                fieldWithPath("content").description("마크다운 본문 1~10,000자")),
                        responseFields(fieldWithPath("boardPostId").description("새 글 번호"))));
        Long postId = jdbc.queryForObject("select max(board_post_id) from board_post where users_id = ?", Long.class,
                writer.id());
        Long commentId = jdbc.queryForObject("insert into board_comment (board_post_id, users_id, content, created_at)"
                + " values (?, ?, 'null 확인을 하세요', now()) returning board_comment_id", Long.class, postId, answerer.id());
        mvc.perform(withCsrf(RestDocumentationRequestBuilders.put("/api/v1/board/posts/{boardPostId}/accepted", postId),
                        session).contentType(MediaType.APPLICATION_JSON).content("{\"commentId\":" + commentId + "}"))
                .andExpect(status().isOk())
                .andDo(document("board-accept",
                        pathParameters(parameterWithName("boardPostId").description("물음 글 번호")),
                        requestHeaders(csrfHeader()),
                        requestFields(fieldWithPath("commentId").description("채택할 댓글 번호 (이 물음에 다른 사람이 단 댓글)")),
                        responseFields(fieldWithPath("acceptedCommentId").description("지금 채택한 댓글 번호"))));
    }

    // ── 13. 편집실 (specs/018) ──

    @Test
    void 편집실_내림() throws Exception {
        Member writer = members.register("docs");
        Member reporter = members.register("docs");
        Member admin = members.register("docs");
        jdbc.update("update users set role = 'admin' where users_id = ?", admin.id());
        Long postId = writePost(writer, "광고만 있는 글");
        jdbc.update("insert into post_report (users_id, post_id, reason, detail, created_at) values (?, ?, 'SPAM', null, now())",
                reporter.id(), postId);
        MockHttpSession session = login(admin);
        mvc.perform(RestDocumentationRequestBuilders.get("/api/v1/editorial/reports/posts/{postId}", postId).session(session))
                .andExpect(status().isOk())
                .andDo(document("editorial-review",
                        pathParameters(parameterWithName("postId").description("신고된 글 번호")),
                        responseFields(
                                fieldWithPath("postId").description("글 번호"),
                                fieldWithPath("blogName").description("블로그 이름"),
                                fieldWithPath("title").description("제목"),
                                fieldWithPath("excerpt").description("본문 앞부분 (마크다운을 걷어 낸 글자)"),
                                fieldWithPath("visibility").description("지금 공개 범위"),
                                fieldWithPath("locked").description("이미 내린(잠긴) 글인가"),
                                fieldWithPath("lockReason").description("내림 사유 (잠기지 않았으면 null)").optional(),
                                fieldWithPath("reports[]").description("대기 신고, 오래된 순 (신고한 사람은 없음)"),
                                fieldWithPath("reports[].reason").description("`SPAM` · `ABUSE` · `ADULT` · `OTHER`"),
                                fieldWithPath("reports[].detail").description("기타 설명 (기타가 아니면 null)").optional(),
                                fieldWithPath("reports[].createdAt").description("신고 시각"))));
        mvc.perform(withCsrf(RestDocumentationRequestBuilders.post("/api/v1/editorial/posts/{postId}/take-down", postId),
                        session).contentType(MediaType.APPLICATION_JSON).content(json("reason", "광고 링크만 있는 글입니다")))
                .andExpect(status().isNoContent())
                .andDo(document("editorial-take-down",
                        pathParameters(parameterWithName("postId").description("내릴 글 번호")),
                        requestHeaders(csrfHeader()),
                        requestFields(fieldWithPath("reason").description("사유 1~200자 (글 주인에게 보인다)"))));
    }

    private Long writePost(Member member, String title) {
        return jdbc.queryForObject("insert into post (category_id, topic_id, title, content, visibility, created_at)"
                + " values (?, (select min(topic_id) from topic), ?, ?, 'public', ?) returning post_id", Long.class,
                member.defaultCategoryId(), title, "## 걷기 좋은 날\n\n바다를 보며 **3시간** 걸었다.",
                java.sql.Timestamp.from(Instant.now()));
    }

    private String postJson(Member member, String title, String content, String extra) {
        Long topicId = jdbc.queryForObject("select min(topic_id) from topic", Long.class);
        return "{\"title\":" + quote(title) + ",\"content\":" + quote(content) + ",\"categoryId\":" + member.defaultCategoryId()
                + ",\"topicId\":" + topicId + ",\"visibility\":\"public\"" + (extra == null ? "" : "," + extra) + "}";
    }

    private static String json(String... keyValues) {
        StringBuilder out = new StringBuilder("{");
        for (int i = 0; i < keyValues.length; i += 2) {
            out.append(i == 0 ? "" : ",").append(quote(keyValues[i])).append(':').append(quote(keyValues[i + 1]));
        }
        return out.append('}').toString();
    }

    private static String quote(String value) {
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }
}
