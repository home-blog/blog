package com.myblog.stats.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myblog.blog.BlogVisitedEvent;
import com.myblog.post.PostViewedEvent;
import com.myblog.support.TestClock;
import com.myblog.support.TestComments;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import com.myblog.support.TestStats;
import jakarta.servlet.http.Cookie;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.context.WebApplicationContext;

/**
 * 조회수·방문자 세기 (specs/006 US6, T049, quickstart S-6, S-7, FR-033 ~ FR-037, SC-005 ~ SC-007).
 * 결정: D-2 A(회원 번호, 비회원은 방문자 쿠키), D-3 A(바로 DB), D-4 A(주인 본인은 세지 않음), D-5 B(블로그 화면에서도 방문자).
 * 숫자는 글 읽기와 따로 된 트랜잭션에 쓰이므로 클래스 전체에 @Transactional을 걸지 않는다. 끝에서 지운다.
 * "30분"은 이 테스트에서 1초로 줄인다. 날짜 경계는 TestClock으로 만든다.
 */
@SpringBootTest(properties = "stats.view.dedupe-window=1s")
@Import({TestMembers.class, TestComments.class, TestStats.class, TestClock.Config.class, ViewCountingTest.TxProbe.class})
class ViewCountingTest {

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

    @Autowired
    private TxProbe txProbe;

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
        postId = members.addPost(owner.defaultCategoryId(), "읽을 글", "public");
    }

    @AfterEach
    void cleanUp() {
        clock.reset();
        created.forEach(testComments::deleteMember);
    }

    @Test
    void S6_1_2_3_회원이_열면_누적_오늘_블로그_오늘_글이_1씩_오르고_시간_안에는_그대로_지나면_다시() throws Exception {
        LocalDate today = today();
        open(postId, readerSession);
        assertThat(postViews(postId)).isEqualTo(1);
        assertThat(blogViews(today)).isEqualTo(1);
        assertThat(postDayViews(postId, today)).isEqualTo(1);
        assertThat(blogVisitors(today)).isEqualTo(1);

        open(postId, readerSession);
        assertThat(postViews(postId)).isEqualTo(1);

        Thread.sleep(1_200);
        open(postId, readerSession);
        assertThat(postViews(postId)).isEqualTo(2);
        assertThat(blogViews(today)).isEqualTo(2);
        assertThat(postDayViews(postId, today)).isEqualTo(2);
        assertThat(blogVisitors(today)).isEqualTo(1); // 방문자는 하루에 한 번
    }

    @Test
    void S6_5_같은_사람이_같은_글을_동시에_10번_열어도_정확히_1() throws Exception {
        List<MockHttpSession> sessions = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            sessions.add(members.login(mvc, reader));
        }
        ExecutorService pool = Executors.newFixedThreadPool(10);
        try {
            CountDownLatch start = new CountDownLatch(1);
            List<CompletableFuture<Void>> calls = new ArrayList<>();
            for (MockHttpSession session : sessions) {
                calls.add(CompletableFuture.runAsync(() -> {
                    try {
                        start.await();
                        open(postId, session);
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    }
                }, pool));
            }
            start.countDown();
            CompletableFuture.allOf(calls.toArray(CompletableFuture[]::new)).get();
        } finally {
            pool.shutdownNow();
        }
        assertThat(postViews(postId)).isEqualTo(1);
        assertThat(blogViews(today())).isEqualTo(1);
        assertThat(blogVisitors(today())).isEqualTo(1);
    }

    @Test
    void S6_7_없는_글과_남의_비공개_글은_세지_않는다() throws Exception {
        Long secret = members.addPost(owner.defaultCategoryId(), "비공개", "private");
        mvc.perform(get("/api/posts/{id}", secret).session(readerSession)).andExpect(status().isNotFound());
        mvc.perform(get("/api/posts/{id}", 99_999_999L).session(readerSession)).andExpect(status().isNotFound());
        assertThat(postViews(secret)).isZero();
        assertThat(blogViews(today())).isZero();
        assertThat(blogVisitors(today())).isZero();
    }

    @Test
    void S6_8_S7_1_글마다_따로_세고_같은_날_글_3개면_방문자_1_조회수_3() throws Exception {
        Long second = members.addPost(owner.defaultCategoryId(), "둘째", "public");
        Long third = members.addPost(owner.defaultCategoryId(), "셋째", "public");
        open(postId, readerSession);
        open(second, readerSession);
        open(third, readerSession);
        assertThat(postViews(postId)).isEqualTo(1);
        assertThat(postViews(second)).isEqualTo(1);
        assertThat(postViews(third)).isEqualTo(1);
        assertThat(blogViews(today())).isEqualTo(3);
        assertThat(blogVisitors(today())).isEqualTo(1);
    }

    @Test
    void S7_3_한국_23시59분과_다음_날_0시01분은_두_날짜에_방문자_1씩() throws Exception {
        Long second = members.addPost(owner.defaultCategoryId(), "둘째", "public");
        clock.set(Instant.parse("2026-10-08T14:59:00Z")); // 한국 10-08 23:59
        open(postId, readerSession);
        clock.set(Instant.parse("2026-10-08T15:01:00Z")); // 한국 10-09 00:01
        open(second, readerSession);
        assertThat(blogVisitors(LocalDate.of(2026, 10, 8))).isEqualTo(1);
        assertThat(blogVisitors(LocalDate.of(2026, 10, 9))).isEqualTo(1);
        assertThat(postDayViews(postId, LocalDate.of(2026, 10, 8))).isEqualTo(1);
        assertThat(postDayViews(second, LocalDate.of(2026, 10, 9))).isEqualTo(1);
    }

    @Test
    void S6_4_S7_5_비회원은_쿠키로_한_사람이고_다른_브라우저는_다른_사람() throws Exception {
        Long second = members.addPost(owner.defaultCategoryId(), "둘째", "public");
        MockHttpServletResponse first = mvc.perform(get("/api/posts/{id}", postId)).andExpect(status().isOk())
                .andReturn().getResponse();
        Cookie visitor = first.getCookie("MYBLOG_VISITOR");
        assertThat(visitor).isNotNull();
        assertThat(visitor.getValue()).matches("[0-9a-f]{32}");
        assertThat(visitor.isHttpOnly()).isTrue();
        assertThat(visitor.getMaxAge()).isEqualTo(365 * 24 * 3600);
        assertThat(first.getHeader("Set-Cookie")).contains("SameSite=Lax");

        // 같은 브라우저: 같은 글은 그대로, 다른 글은 조회수만 (방문자는 그대로), 쿠키를 다시 내려 주지 않는다
        MockHttpServletResponse again = mvc.perform(get("/api/posts/{id}", postId).cookie(visitor)).andReturn().getResponse();
        assertThat(again.getCookie("MYBLOG_VISITOR")).isNull();
        mvc.perform(get("/api/posts/{id}", second).cookie(visitor)).andExpect(status().isOk());
        assertThat(postViews(postId)).isEqualTo(1);
        assertThat(blogVisitors(today())).isEqualTo(1);

        // 다른 브라우저 (쿠키 없음): 다른 사람
        mvc.perform(get("/api/posts/{id}", postId)).andExpect(status().isOk());
        assertThat(postViews(postId)).isEqualTo(2);
        assertThat(blogVisitors(today())).isEqualTo(2);
    }

    @Test
    void S6_6_블로그_주인이_자기_글을_열면_조회수도_방문자도_그대로() throws Exception {
        MockHttpSession ownerSession = members.login(mvc, owner);
        open(postId, ownerSession);
        mvc.perform(get("/api/blogs/{id}", owner.blogId()).session(ownerSession)).andExpect(status().isOk());
        Long secret = members.addPost(owner.defaultCategoryId(), "비공개", "private");
        open(secret, ownerSession);
        assertThat(postViews(postId)).isZero();
        assertThat(postViews(secret)).isZero();
        assertThat(blogViews(today())).isZero();
        assertThat(blogVisitors(today())).isZero();
    }

    @Test
    void S7_2_블로그_첫_화면만_봐도_방문자_1이고_조회수는_그대로() throws Exception {
        mvc.perform(get("/api/blogs/{id}", owner.blogId()).session(readerSession)).andExpect(status().isOk());
        mvc.perform(get("/api/blogs/{id}", owner.blogId()).session(readerSession)).andExpect(status().isOk());
        assertThat(blogVisitors(today())).isEqualTo(1);
        assertThat(blogViews(today())).isZero();
        open(postId, readerSession);
        assertThat(blogVisitors(today())).isEqualTo(1);
        assertThat(blogViews(today())).isEqualTo(1);
    }

    @Test
    void 조회_방문_알림은_읽기_트랜잭션이_끝난_뒤에_온다_한_요청이_DB_연결_둘을_같이_쥐지_않는다() throws Exception {
        txProbe.seen.clear();
        open(postId, readerSession);
        mvc.perform(get("/api/blogs/{id}", owner.blogId()).session(readerSession)).andExpect(status().isOk());
        assertThat(txProbe.seen).containsExactly("post:false", "blog:false");
    }

    private void open(Long id, MockHttpSession session) throws Exception {
        mvc.perform(get("/api/posts/{id}", id).session(session)).andExpect(status().isOk());
    }

    private LocalDate today() {
        return clock.instant().atZone(java.time.ZoneId.of("Asia/Seoul")).toLocalDate();
    }

    private int postViews(Long id) {
        return jdbc.queryForObject("select views from post where post_id = ?", Integer.class, id);
    }

    private int postDayViews(Long id, LocalDate date) {
        return jdbc.query("select views from post_daily_stat where post_id = ? and stat_date = ?",
                rs -> rs.next() ? rs.getInt(1) : 0, id, date);
    }

    private int blogViews(LocalDate date) {
        return jdbc.query("select views from blog_daily_stat where blog_id = ? and stat_date = ?",
                rs -> rs.next() ? rs.getInt(1) : 0, owner.blogId(), date);
    }

    private int blogVisitors(LocalDate date) {
        return jdbc.query("select visitors from blog_daily_stat where blog_id = ? and stat_date = ?",
                rs -> rs.next() ? rs.getInt(1) : 0, owner.blogId(), date);
    }

    private Member register(String prefix) {
        Member member = members.register(prefix);
        created.add(member);
        return member;
    }

    /** 알림을 받을 때 트랜잭션이 열려 있는지 적어 둔다. */
    @TestComponent
    static class TxProbe {

        final List<String> seen = new CopyOnWriteArrayList<>();

        @EventListener
        void on(PostViewedEvent event) {
            seen.add("post:" + TransactionSynchronizationManager.isActualTransactionActive());
        }

        @EventListener
        void on(BlogVisitedEvent event) {
            seen.add("blog:" + TransactionSynchronizationManager.isActualTransactionActive());
        }
    }
}
