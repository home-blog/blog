package com.myblog.comment.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myblog.support.TestComments;
import com.myblog.support.TestJson;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
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
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 댓글 5초 간격 (specs/005 T014, quickstart S-3, FR-008, SC-007, D-2).
 * 동시 요청이 각자 커밋해야 하므로 클래스 전체 @Transactional을 쓰지 않는다. 간격이 지난 것은 시각을 뒤로 돌려 만든다.
 */
@SpringBootTest
@Import({TestMembers.class, TestComments.class})
class CommentIntervalTest {

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
    private Member writer;
    private MockHttpSession session;
    private Long postId;
    private Long otherPostId;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        Member owner = register("o");
        writer = register("w");
        session = members.login(mvc, writer);
        postId = members.addPost(owner.defaultCategoryId(), "글 하나", "public");
        otherPostId = members.addPost(owner.defaultCategoryId(), "글 둘", "public");
    }

    @AfterEach
    void cleanUp() {
        created.forEach(testComments::deleteMember);
    }

    @Test
    void S3_1_2_바로_다시_쓰면_429이고_간격이_지나면_된다() throws Exception {
        write(session, postId).andExpect(status().isCreated());
        write(session, postId)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("COMMENT_TOO_FREQUENT"))
                .andExpect(jsonPath("$.message").value("※ 잠시 뒤에 다시 등록해 주세요"));
        testComments.rewind(writer.id());
        write(session, postId).andExpect(status().isCreated());
        assertThat(countBy(writer)).isEqualTo(2);
    }

    @Test
    void S3_3_다른_글이어도_사람_기준으로_막는다() throws Exception {
        write(session, postId).andExpect(status().isCreated());
        write(session, otherPostId).andExpect(status().isTooManyRequests());
    }

    @Test
    void S3_4_다른_두_회원은_동시에_써도_둘_다_된다() throws Exception {
        Member second = register("s");
        MockHttpSession secondSession = members.login(mvc, second);
        List<Integer> statuses = together(List.of(request(session, postId), request(secondSession, postId)));
        assertThat(statuses).containsExactly(201, 201);
    }

    @Test
    void S3_5_같은_회원의_요청_5개를_동시에_보내면_한_건만_저장된다() throws Exception {
        List<RequestBuilder> requests = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            requests.add(request(session, i % 2 == 0 ? postId : otherPostId));
        }
        List<Integer> statuses = together(requests);
        assertThat(statuses).containsOnly(201, 429);
        assertThat(statuses).filteredOn(code -> code == 201).hasSize(1);
        assertThat(countBy(writer)).isOne();
    }

    private ResultActions write(MockHttpSession who, Long target) throws Exception {
        return mvc.perform(request(who, target));
    }

    private static RequestBuilder request(MockHttpSession who, Long target) {
        return post("/api/posts/{id}/comments", target).with(csrf()).session(who)
                .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("body", "댓글"));
    }

    private List<Integer> together(List<RequestBuilder> requests) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(requests.size());
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (RequestBuilder request : requests) {
                futures.add(pool.submit((Callable<Integer>) () -> {
                    start.await();
                    return mvc.perform(request).andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get());
            }
            return statuses;
        } finally {
            pool.shutdown();
        }
    }

    private long countBy(Member member) {
        return jdbc.queryForObject("select count(*) from comment where users_id = ?", Long.class, member.id());
    }

    private Member register(String prefix) {
        Member member = members.register(prefix);
        created.add(member);
        return member;
    }
}
