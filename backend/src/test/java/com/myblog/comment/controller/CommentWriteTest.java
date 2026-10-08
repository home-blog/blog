package com.myblog.comment.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myblog.support.TestComments;
import com.myblog.support.TestJson;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 댓글 쓰고 읽기 (specs/005 US1, T013, quickstart S-1의 2 ~ 6, S-2, S-12의 1·2·4).
 * 요청마다 커밋되는 것을 보므로 클래스 전체 @Transactional을 쓰지 않고 끝에서 지운다.
 */
@SpringBootTest
@Import({TestMembers.class, TestComments.class})
class CommentWriteTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private TestComments testComments;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private final List<Member> created = new ArrayList<>();
    private Member owner;
    private Member reader;
    private MockHttpSession readerSession;
    private Long postId;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = register("o");
        reader = register("r");
        readerSession = members.login(mvc, reader);
        postId = members.addPost(owner.defaultCategoryId(), "댓글 달 글", "public");
    }

    @AfterEach
    void cleanUp() {
        created.forEach(testComments::deleteMember);
    }

    @Test
    void S1_2_로그인하지_않으면_401이고_댓글이_생기지_않는다() throws Exception {
        mvc.perform(post("/api/posts/{id}/comments", postId).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(TestJson.of("body", "안녕하세요")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        assertThat(commentCount()).isZero();
    }

    @Test
    void CSRF_토큰이_없으면_거절한다() throws Exception {
        mvc.perform(post("/api/posts/{id}/comments", postId).session(readerSession).contentType(MediaType.APPLICATION_JSON)
                        .content(TestJson.of("body", "안녕하세요")))
                .andExpect(status().isForbidden());
        assertThat(commentCount()).isZero();
    }

    @Test
    void S1_3_4_쓰면_닉네임_시각_내용이_오고_줄바꿈은_그대로_앞뒤_공백은_지운다() throws Exception {
        write(readerSession, "  잘 읽었습니다.\r\n사진이 예뻐요\n  ")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.author.id").value(reader.id()))
                .andExpect(jsonPath("$.author.nickname").value(reader.user().getNickname()))
                .andExpect(jsonPath("$.author.withdrawn").value(false))
                .andExpect(jsonPath("$.body").value("잘 읽었습니다.\n사진이 예뻐요"))
                .andExpect(jsonPath("$.createdAt").isString())
                .andExpect(jsonPath("$.canDelete").value(true));
        assertThat(jdbc.queryForObject("select parent_id is null and not is_secret from comment where post_id = ?",
                Boolean.class, postId)).isTrue();
    }

    @Test
    void S1_5_6_목록은_오래된_순이고_같은_회원이_한_글에_두_번째_댓글도_쓴다() throws Exception {
        write(readerSession, "첫 댓글").andExpect(status().isCreated());
        testComments.rewind(reader.id());
        write(readerSession, "두 번째 댓글").andExpect(status().isCreated());
        MockHttpSession ownerSession = members.login(mvc, owner);
        write(ownerSession, "주인의 답").andExpect(status().isCreated());

        mvc.perform(get("/api/posts/{id}/comments", postId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(3))
                .andExpect(jsonPath("$.comments[0].body").value("첫 댓글"))
                .andExpect(jsonPath("$.comments[1].body").value("두 번째 댓글"))
                .andExpect(jsonPath("$.comments[2].body").value("주인의 답"))
                .andExpect(jsonPath("$.comments[0].canDelete").value(false));
        // 작성자는 자기 댓글만, 블로그 주인은 모든 댓글을 지울 수 있다 (canDelete는 화면용 값)
        mvc.perform(get("/api/posts/{id}/comments", postId).session(readerSession))
                .andExpect(jsonPath("$.comments[0].canDelete").value(true))
                .andExpect(jsonPath("$.comments[2].canDelete").value(false));
        mvc.perform(get("/api/posts/{id}/comments", postId).session(ownerSession))
                .andExpect(jsonPath("$.comments[0].canDelete").value(true))
                .andExpect(jsonPath("$.comments[1].canDelete").value(true));
        // 글 상세의 댓글 수 (contracts 7)
        mvc.perform(get("/api/posts/{id}", postId))
                .andExpect(jsonPath("$.commentCount").value(3))
                .andExpect(jsonPath("$.likeCount").value(0))
                .andExpect(jsonPath("$.likedByMe").value(false));
    }

    @Test
    void S2_1_2_공백만이거나_줄바꿈만이면_COMMENT_EMPTY() throws Exception {
        for (String body : new String[] {"   ", "\n\n\r\n", ""}) {
            write(readerSession, body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("body"))
                    .andExpect(jsonPath("$.fieldErrors[0].code").value("COMMENT_EMPTY"))
                    .andExpect(jsonPath("$.fieldErrors[0].message").value("※ 댓글 내용을 입력해 주세요"));
        }
        mvc.perform(post("/api/posts/{id}/comments", postId).with(csrf()).session(readerSession)
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("COMMENT_EMPTY"));
        assertThat(commentCount()).isZero();
    }

    @Test
    void S2_3_4_500자는_통과하고_501자는_COMMENT_TOO_LONG() throws Exception {
        write(readerSession, "가".repeat(501))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].code").value("COMMENT_TOO_LONG"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("※ 댓글은 500자 이하로 입력해 주세요"));
        write(readerSession, "가".repeat(500)).andExpect(status().isCreated());
    }

    @Test
    void S2_5_이모지가_섞인_500자도_통과한다() throws Exception {
        // 이모지는 UTF-16으로 두 칸이지만 한 글자로 센다 (research B-2)
        write(readerSession, "😀".repeat(250) + "가".repeat(250)).andExpect(status().isCreated());
        assertThat(commentCount()).isOne();
    }

    @Test
    void S2_6_남의_비공개_글은_없는_글과_상태_코드와_본문이_같다() throws Exception {
        Long hidden = members.addPost(owner.defaultCategoryId(), "비공개", "private");
        String missingBody = write(readerSession, "안녕", 99_999_999L).andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();
        String hiddenBody = write(readerSession, "안녕", hidden).andExpect(status().isNotFound())
                .andReturn().getResponse().getContentAsString();
        assertThat(hiddenBody).isEqualTo(missingBody).contains("POST_NOT_FOUND");
        // 형식이 틀린 내용을 보내도 볼 수 없는 글이면 404가 먼저다 (요청 검사 순서)
        write(readerSession, " ", hidden).andExpect(status().isNotFound());
        mvc.perform(get("/api/posts/{id}/comments", hidden).session(readerSession)).andExpect(status().isNotFound());
        // 주인은 자기 비공개 글에 댓글을 쓰고 읽는다
        MockHttpSession ownerSession = members.login(mvc, owner);
        write(ownerSession, "메모", hidden).andExpect(status().isCreated());
        mvc.perform(get("/api/posts/{id}/comments", hidden).session(ownerSession)).andExpect(jsonPath("$.count").value(1));
    }

    @Test
    void S12_스크립트와_SQL_같은_글자도_그대로_저장하고_돌려준다() throws Exception {
        String[] bodies = {"<script>alert(1)</script>", "<img src=x onerror=alert(1)>", "' OR 1=1 --"};
        for (String body : bodies) {
            write(readerSession, body).andExpect(status().isCreated()).andExpect(jsonPath("$.body").value(body));
            testComments.rewind(reader.id());
        }
        mvc.perform(get("/api/posts/{id}/comments", postId))
                .andExpect(jsonPath("$.count").value(3))
                .andExpect(jsonPath("$.comments[2].body").value("' OR 1=1 --"))
                .andExpect(jsonPath("$.comments[2].author.id").value(reader.id()));
        assertThat(jdbc.queryForObject("select count(*) from post", Long.class)).isPositive();
    }

    @Test
    void 탈퇴한_작성자는_번호와_닉네임_없이_withdrawn() throws Exception {
        write(readerSession, "남는 댓글").andExpect(status().isCreated());
        jdbc.update("update users set deleted_at = now(), nickname = ? where users_id = ?", "탈퇴한사용자" + reader.id(), reader.id());
        mvc.perform(get("/api/posts/{id}/comments", postId))
                .andExpect(jsonPath("$.comments[0].author.withdrawn").value(true))
                .andExpect(jsonPath("$.comments[0].author.id").value(nullValue()))
                .andExpect(jsonPath("$.comments[0].author.nickname").value(nullValue()));
    }

    private ResultActions write(MockHttpSession session, String body) throws Exception {
        return write(session, body, postId);
    }

    private ResultActions write(MockHttpSession session, String body, Long target) throws Exception {
        return mvc.perform(post("/api/posts/{id}/comments", target).with(csrf()).session(session)
                .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("body", body)));
    }

    private long commentCount() {
        return jdbc.queryForObject("select count(*) from comment where post_id = ?", Long.class, postId);
    }

    private Member register(String prefix) {
        Member member = members.register(prefix);
        created.add(member);
        return member;
    }
}
