package com.myblog.board;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.myblog.support.TestClock;
import com.myblog.support.TestComments;
import com.myblog.support.TestJson;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
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
 * 글마당 (specs/017 US1 ~ US5, S1-1 ~ S5-2). 우편함 소식은 커밋 뒤 따로 만들어지므로 클래스 전체 @Transactional을 쓰지 않고
 * 끝에서 지운다(회원을 지우면 글마당 글·댓글도 함께 지워진다). 다른 테스트의 글과 섞이지 않게 제목마다 고유한 표시를 넣고
 * 그 표시로 찾는다. 너무 빨리 올리기 간격은 시계를 옮겨 넘긴다.
 */
@SpringBootTest
@Import({TestMembers.class, TestComments.class, TestClock.Config.class})
class BoardTest {

    private static final long WAIT_MS = 5000;

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private TestComments testComments;

    @Autowired
    private TestClock clock;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private final List<Member> created = new ArrayList<>();
    private Member writer;
    private Member reader;
    private MockHttpSession writerSession;
    private MockHttpSession readerSession;
    private String mark;
    private Instant now;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        now = LocalDate.of(2100, 1, 1).plusDays(ThreadLocalRandom.current().nextInt(1, 300_000)).atTime(3, 0)
                .toInstant(ZoneOffset.UTC);
        clock.set(now);
        writer = register("bw");
        reader = register("br");
        writerSession = members.login(mvc, writer);
        readerSession = members.login(mvc, reader);
        mark = "mk" + UUID.randomUUID().toString().substring(0, 8);
    }

    @AfterEach
    void cleanUp() {
        clock.reset();
        for (Member member : created) {
            jdbc.update("delete from notification where users_id = ?", member.id());
            testComments.deleteMember(member);
        }
    }

    private Member register(String prefix) {
        Member member = members.register(prefix);
        created.add(member);
        return member;
    }

    /** 간격 검사에 걸리지 않게 시계를 2분 옮긴다. */
    private void later() {
        now = now.plus(Duration.ofMinutes(2));
        clock.set(now);
    }

    private ResultActions writeRaw(MockHttpSession session, String topic, String kind, String title, String content)
            throws Exception {
        return mvc.perform(post("/api/v1/board/posts").with(csrf()).session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestJson.of("topic", topic, "kind", kind, "title", title, "content", content)));
    }

    private Long write(MockHttpSession session, String topic, String kind, String title) throws Exception {
        later();
        String body = writeRaw(session, topic, kind, title + " " + mark, "본문 " + title).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(body, "$.boardPostId")).longValue();
    }

    private Long comment(MockHttpSession session, Long postId, String content) throws Exception {
        later();
        String body = mvc.perform(post("/api/v1/board/posts/{id}/comments", postId).with(csrf()).session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("content", content)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(body, "$.boardCommentId")).longValue();
    }

    private ResultActions list(String query) throws Exception {
        return mvc.perform(get("/api/v1/board/posts?q=" + mark + (query.isEmpty() ? "" : "&" + query)));
    }

    private void makeAdmin(Member member) {
        jdbc.update("update users set role = 'admin' where users_id = ?", member.id());
    }

    private long notices(Member member, String kind, Long boardPostId) throws InterruptedException {
        long deadline = System.currentTimeMillis() + WAIT_MS;
        long count = 0;
        while (System.currentTimeMillis() < deadline) {
            count = jdbc.queryForObject("select count(*) from notification where users_id = ? and kind = ? and board_post_id = ?",
                    Long.class, member.id(), kind, boardPostId);
            if (count > 0) {
                return count;
            }
            Thread.sleep(50);
        }
        return count;
    }

    // US1 둘러보기

    @Test
    void S1_목록은_최신순이고_주제와_종류로_고르며_댓글_수와_쓴_사람이_보인다() throws Exception {
        Long travel = write(writerSession, "여행", "talk", "제주 렌터카");
        Long food = write(readerSession, "음식", "question", "김치찌개 비법");
        comment(readerSession, travel, "좋아요");

        list("").andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.pageSize").value(10))
                .andExpect(jsonPath("$.topics", hasSize(5)))
                .andExpect(jsonPath("$.posts[0].boardPostId").value(food))
                .andExpect(jsonPath("$.posts[0].kind").value("question"))
                .andExpect(jsonPath("$.posts[0].solved").value(false))
                .andExpect(jsonPath("$.posts[1].boardPostId").value(travel))
                .andExpect(jsonPath("$.posts[1].topic").value("여행"))
                .andExpect(jsonPath("$.posts[1].commentCount").value(1))
                .andExpect(jsonPath("$.posts[1].writer.nickname").value(writer.user().getNickname()));
        list("topic=여행").andExpect(jsonPath("$.totalCount").value(1)).andExpect(jsonPath("$.topic").value("여행"));
        list("kind=question").andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.posts[0].boardPostId").value(food));
        list("topic=운동").andExpect(jsonPath("$.totalCount").value(0)).andExpect(jsonPath("$.posts", hasSize(0)));
    }

    @Test
    void S1_4_없는_주제나_종류는_400() throws Exception {
        mvc.perform(get("/api/v1/board/posts?topic=없는주제")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BOARD_TOPIC_INVALID"));
        mvc.perform(get("/api/v1/board/posts?kind=poem")).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("BOARD_KIND_INVALID"));
    }

    @Test
    void S1_1_없는_글은_404이고_로그인_없이_읽는다() throws Exception {
        mvc.perform(get("/api/v1/board/posts/{id}", Long.MAX_VALUE)).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BOARD_POST_NOT_FOUND"));
        Long id = write(writerSession, "취미", "talk", "뜨개질");
        mvc.perform(get("/api/v1/board/posts/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("뜨개질 " + mark))
                .andExpect(jsonPath("$.content").value("본문 뜨개질"))
                .andExpect(jsonPath("$.mine").value(false))
                .andExpect(jsonPath("$.canDelete").value(false));
        mvc.perform(get("/api/v1/board/posts/{id}/comments", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(0));
    }

    @Test
    void S1_2_조회_수는_같은_사람이_다시_봐도_한_번이고_쓴_사람은_세지_않는다() throws Exception {
        Long id = write(writerSession, "운동", "talk", "러닝화");
        mvc.perform(get("/api/v1/board/posts/{id}", id).session(writerSession)).andExpect(jsonPath("$.mine").value(true));
        mvc.perform(get("/api/v1/board/posts/{id}", id).session(readerSession)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/board/posts/{id}", id).session(readerSession)).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select views from board_post where board_post_id = ?", Long.class, id)).isEqualTo(1);
    }

    @Test
    void S1_3_탈퇴한_회원의_글은_남고_쓴_사람이_탈퇴한_회원으로_보인다() throws Exception {
        Long id = write(writerSession, "개발", "talk", "자바 21");
        jdbc.update("update users set deleted_at = now() where users_id = ?", writer.id());
        mvc.perform(get("/api/v1/board/posts/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.writer.withdrawn").value(true))
                .andExpect(jsonPath("$.writer.nickname").value(nullValue()));
        list("").andExpect(jsonPath("$.posts[0].writer.withdrawn").value(true));
    }

    // US2 쓰기·고치기·지우기

    @Test
    void S2_1_제목과_본문이_비거나_길면_칸마다_알린다() throws Exception {
        writeRaw(writerSession, "없는주제", "poem", "  ", "").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors", hasSize(4)));
        writeRaw(writerSession, "여행", "talk", "가".repeat(101), "본문").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].code").value("BOARD_TITLE_TOO_LONG"));
        writeRaw(writerSession, "여행", "talk", "제목", "가".repeat(10_001)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].code").value("BOARD_CONTENT_TOO_LONG"));
        // 경계값: 딱 100자 제목은 된다
        writeRaw(writerSession, "여행", "talk", "가".repeat(100), "본문").andExpect(status().isCreated());
        mvc.perform(post("/api/v1/board/posts").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content(TestJson.of("topic", "여행", "kind", "talk", "title", "t", "content", "c")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void S2_2_남의_글은_고치지_못하고_관리자는_지울_수_있다() throws Exception {
        Long id = write(writerSession, "여행", "talk", "부산 여행");
        String change = TestJson.of("topic", "음식", "kind", "talk", "title", "부산 맛집 " + mark, "content", "고친 본문");
        mvc.perform(put("/api/v1/board/posts/{id}", id).with(csrf()).session(readerSession)
                .contentType(MediaType.APPLICATION_JSON).content(change)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/board/posts/{id}", id).with(csrf()).session(readerSession))
                .andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/board/posts/{id}", id).with(csrf()).session(writerSession)
                .contentType(MediaType.APPLICATION_JSON).content(change)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/board/posts/{id}", id)).andExpect(jsonPath("$.topic").value("음식"))
                .andExpect(jsonPath("$.title").value("부산 맛집 " + mark));

        makeAdmin(reader);
        mvc.perform(get("/api/v1/board/posts/{id}", id).session(readerSession))
                .andExpect(jsonPath("$.canDelete").value(true));
        mvc.perform(delete("/api/v1/board/posts/{id}", id).with(csrf()).session(readerSession))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/board/posts/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void S2_3_같은_회원은_정한_간격_안에_다시_올리지_못한다() throws Exception {
        write(writerSession, "여행", "talk", "첫 글");
        writeRaw(writerSession, "여행", "talk", "둘째 글", "본문").andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("BOARD_POST_TOO_FAST"));
        // 다른 사람은 된다
        writeRaw(readerSession, "여행", "talk", "다른 사람 글", "본문").andExpect(status().isCreated());
        clock.set(now.plus(Duration.ofMinutes(1)));
        writeRaw(writerSession, "여행", "talk", "둘째 글", "본문").andExpect(status().isCreated());
    }

    @Test
    void S2_4_글을_지우면_댓글도_사라지고_목록에서_빠진다() throws Exception {
        Long id = write(writerSession, "취미", "talk", "사진");
        comment(readerSession, id, "멋져요");
        mvc.perform(delete("/api/v1/board/posts/{id}", id).with(csrf()).session(writerSession))
                .andExpect(status().isNoContent());
        list("").andExpect(jsonPath("$.totalCount").value(0));
        assertThat(jdbc.queryForObject("select count(*) from board_comment where board_post_id = ?", Long.class, id))
                .isZero();
    }

    // US3 댓글

    @Test
    void S3_댓글은_오래된_순이고_지우면_수가_줄며_비었거나_빠르면_막는다() throws Exception {
        Long id = write(writerSession, "개발", "talk", "스프링");
        Long first = comment(readerSession, id, "첫째");
        Long second = comment(writerSession, id, "둘째");
        mvc.perform(get("/api/v1/board/posts/{id}/comments", id).session(readerSession))
                .andExpect(jsonPath("$.count").value(2))
                .andExpect(jsonPath("$.comments[0].boardCommentId").value(first))
                .andExpect(jsonPath("$.comments[0].mine").value(true))
                .andExpect(jsonPath("$.comments[0].canDelete").value(true))
                .andExpect(jsonPath("$.comments[1].canDelete").value(false));

        mvc.perform(post("/api/v1/board/posts/{id}/comments", id).with(csrf()).session(readerSession)
                        .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("content", "  \n ")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("COMMENT_EMPTY"));
        mvc.perform(post("/api/v1/board/posts/{id}/comments", id).with(csrf()).session(readerSession)
                        .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("content", "가".repeat(1001))))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BOARD_COMMENT_TOO_LONG"));
        mvc.perform(post("/api/v1/board/posts/{id}/comments", id).with(csrf()).session(writerSession)
                        .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("content", "바로 또")))
                .andExpect(status().isTooManyRequests()).andExpect(jsonPath("$.code").value("BOARD_COMMENT_TOO_FAST"));

        mvc.perform(delete("/api/v1/board/comments/{id}", second).with(csrf()).session(readerSession))
                .andExpect(status().isForbidden());
        mvc.perform(delete("/api/v1/board/comments/{id}", second).with(csrf()).session(writerSession))
                .andExpect(status().isNoContent());
        list("").andExpect(jsonPath("$.posts[0].commentCount").value(1));
    }

    @Test
    void S3_내_글에_남이_댓글을_달면_우편함에_소식이_오고_내가_단_것은_오지_않는다() throws Exception {
        Long id = write(writerSession, "음식", "talk", "라면");
        comment(writerSession, id, "내가 단 댓글");
        comment(readerSession, id, "남이 단 댓글");
        assertThat(notices(writer, "board", id)).isEqualTo(1);
        mvc.perform(get("/api/v1/me/notifications").session(writerSession))
                .andExpect(jsonPath("$.items[0].kind").value("board"))
                .andExpect(jsonPath("$.items[0].boardPostId").value(id))
                .andExpect(jsonPath("$.items[0].postId").value(nullValue()))
                .andExpect(jsonPath("$.items[0].postTitle").value("라면 " + mark))
                .andExpect(jsonPath("$.items[0].actorName").value(reader.user().getNickname()))
                .andExpect(jsonPath("$.items[0].available").value(true));

        mvc.perform(delete("/api/v1/board/posts/{id}", id).with(csrf()).session(writerSession))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/me/notifications").session(writerSession))
                .andExpect(jsonPath("$.items[0].available").value(false))
                .andExpect(jsonPath("$.items[0].postTitle").value(nullValue()));
    }

    // US4 물음과 채택

    @Test
    void S4_물음을_쓴_사람만_남의_댓글을_채택하고_채택한_댓글은_맨_위로_오며_소식이_간다() throws Exception {
        Long question = write(writerSession, "개발", "question", "NPE 질문");
        Long mine = comment(writerSession, question, "제가 더 해 볼게요");
        Long answer = comment(readerSession, question, "null 확인을 하세요");
        String body = TestJson.of("commentId", answer);

        mvc.perform(put("/api/v1/board/posts/{id}/accepted", question).with(csrf()).session(readerSession)
                .contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isForbidden());
        mvc.perform(put("/api/v1/board/posts/{id}/accepted", question).with(csrf()).session(writerSession)
                        .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("commentId", mine)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BOARD_ACCEPT_INVALID"));

        mvc.perform(put("/api/v1/board/posts/{id}/accepted", question).with(csrf()).session(writerSession)
                        .contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk()).andExpect(jsonPath("$.acceptedCommentId").value(answer));
        mvc.perform(get("/api/v1/board/posts/{id}/comments", question).session(writerSession))
                .andExpect(jsonPath("$.acceptedCommentId").value(answer))
                .andExpect(jsonPath("$.canAccept").value(true))
                .andExpect(jsonPath("$.comments[0].boardCommentId").value(answer))
                .andExpect(jsonPath("$.comments[0].accepted").value(true));
        assertThat(notices(reader, "accepted", question)).isEqualTo(1);
        list("").andExpect(jsonPath("$.posts[0].solved").value(true));
        list("unsolved=true").andExpect(jsonPath("$.totalCount").value(0));

        // S4-2 채택된 댓글은 쓴 사람이 지울 수 없다
        mvc.perform(get("/api/v1/board/posts/{id}/comments", question).session(readerSession))
                .andExpect(jsonPath("$.comments[0].canDelete").value(false));
        mvc.perform(delete("/api/v1/board/comments/{id}", answer).with(csrf()).session(readerSession))
                .andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("BOARD_COMMENT_ACCEPTED"));
        // S2-5 채택한 물음은 이야기로 바꿀 수 없다
        mvc.perform(put("/api/v1/board/posts/{id}", question).with(csrf()).session(writerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestJson.of("topic", "개발", "kind", "talk", "title", "NPE", "content", "본문")))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BOARD_ACCEPTED_KIND_LOCKED"));

        mvc.perform(delete("/api/v1/board/posts/{id}/accepted", question).with(csrf()).session(writerSession))
                .andExpect(status().isOk()).andExpect(jsonPath("$.acceptedCommentId").value(nullValue()));
        mvc.perform(delete("/api/v1/board/comments/{id}", answer).with(csrf()).session(readerSession))
                .andExpect(status().isNoContent());
    }

    @Test
    void S4_1_이야기_글이나_다른_글의_댓글은_채택할_수_없고_관리자가_채택된_댓글을_지우면_풀린다() throws Exception {
        Long talk = write(writerSession, "취미", "talk", "이야기 글");
        Long talkComment = comment(readerSession, talk, "댓글");
        mvc.perform(put("/api/v1/board/posts/{id}/accepted", talk).with(csrf()).session(writerSession)
                        .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("commentId", talkComment)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BOARD_ACCEPT_INVALID"));

        Long question = write(writerSession, "취미", "question", "물음 글");
        mvc.perform(put("/api/v1/board/posts/{id}/accepted", question).with(csrf()).session(writerSession)
                        .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("commentId", talkComment)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("BOARD_ACCEPT_INVALID"));

        Long answer = comment(readerSession, question, "답");
        mvc.perform(put("/api/v1/board/posts/{id}/accepted", question).with(csrf()).session(writerSession)
                .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("commentId", answer))).andExpect(status().isOk());
        Member admin = register("ba");
        makeAdmin(admin);
        mvc.perform(delete("/api/v1/board/comments/{id}", answer).with(csrf()).session(members.login(mvc, admin)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/board/posts/{id}", question))
                .andExpect(jsonPath("$.acceptedCommentId").value(nullValue()))
                .andExpect(jsonPath("$.commentCount").value(0));
    }

    // US5 첫 화면과 검색

    @Test
    void S5_검색은_모든_단어가_들어_있는_글만_찾고_최근_글을_준다() throws Exception {
        Long both = write(writerSession, "여행", "talk", "단풍 명소");
        write(writerSession, "여행", "talk", "단풍 사진");
        mvc.perform(get("/api/v1/board/posts").param("q", mark + " 명소"))
                .andExpect(jsonPath("$.totalCount").value(1))
                .andExpect(jsonPath("$.keyword").value(mark + " 명소"))
                .andExpect(jsonPath("$.posts[0].boardPostId").value(both));
        // 쓴 사람 닉네임·주제 이름으로도 찾는다
        mvc.perform(get("/api/v1/board/posts").param("q", mark + " " + writer.user().getNickname() + " 여행"))
                .andExpect(jsonPath("$.totalCount").value(2));
        mvc.perform(get("/api/v1/board/posts").param("q", "가".repeat(51))).andExpect(status().isBadRequest());
        mvc.perform(get("/api/v1/board/posts/recent")).andExpect(status().isOk())
                .andExpect(jsonPath("$.posts[0].title").exists());
    }
}
