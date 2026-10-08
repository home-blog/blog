package com.myblog.blog.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 분류의 동시 요청 (specs/003 S-10의 4·14, SC-006, research R-3). 요청마다 따로 커밋해야 하므로 @Transactional을 쓰지 않고 끝에서 지운다.
 */
@SpringBootTest
@Import(TestMembers.class)
class CategoryConcurrencyTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private Member owner;
    private MockHttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = members.register("q");
        session = members.login(mvc, owner);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("delete from post where category_id in (select category_id from category where blog_id = ?)", owner.blogId());
        jdbc.update("delete from category where blog_id = ?", owner.blogId());
        jdbc.update("delete from blog where blog_id = ?", owner.blogId());
        jdbc.update("delete from users where users_id = ?", owner.id());
    }

    @Test
    void S10_4_같은_이름을_동시에_두_번_추가하면_하나만_생긴다() throws Exception {
        RequestBuilder add = post("/api/me/blog/categories").with(csrf()).session(session)
                .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("name", "daily"));
        List<Integer> statuses = together(add, add);
        assertThat(statuses).containsExactlyInAnyOrder(201, 409);
        assertThat(jdbc.queryForObject("select count(*) from category where blog_id = ? and lower(name) = 'daily'",
                Long.class, owner.blogId())).isOne();
    }

    @Test
    void S10_14_빈_분류의_삭제와_그_분류로_글쓰기를_동시에_보내면_분류_없는_글이_생기지_않는다() throws Exception {
        Long topicId = jdbc.queryForObject("select min(topic_id) from topic", Long.class);
        for (int round = 0; round < 5; round++) {
            Long categoryId = members.addCategory(owner.blogId(), "잠깐" + round, "public", 10 + round);
            RequestBuilder remove = delete("/api/me/blog/categories/{id}", categoryId).with(csrf()).session(session);
            RequestBuilder write = post("/api/posts").with(csrf()).session(session).contentType(MediaType.APPLICATION_JSON)
                    .content(TestJson.of("title", "글", "content", "본문", "categoryId", categoryId, "topicId", topicId));
            List<Integer> statuses = together(remove, write);
            // 둘 다 성공하는 일은 없다: 삭제가 먼저면 글쓰기는 INVALID_CATEGORY(400), 글이 먼저면 삭제는 CATEGORY_HAS_POSTS(409)
            assertThat(statuses).isIn(List.of(204, 400), List.of(409, 201));
        }
        assertThat(jdbc.queryForObject("select count(*) from post p left join category c using (category_id)"
                + " where c.category_id is null", Long.class)).isZero();
    }

    private List<Integer> together(RequestBuilder first, RequestBuilder second) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (RequestBuilder request : List.of(first, second)) {
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
}
