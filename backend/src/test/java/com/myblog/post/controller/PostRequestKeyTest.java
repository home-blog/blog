package com.myblog.post.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import com.jayway.jsonpath.JsonPath;
import com.myblog.support.TestJson;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 연속 저장: 같은 요청 번호(requestKey)로는 글이 하나만 생긴다 (specs/003 S-5의 2·3, D-6, FR-018, SC-008, T018).
 * 동시 요청이 각자 커밋해야 하므로 클래스 전체 @Transactional을 쓰지 않고, 끝에서 만든 것을 지운다.
 */
@SpringBootTest
@Import(TestMembers.class)
class PostRequestKeyTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private Member owner;
    private MockHttpSession session;
    private Long topicId;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = members.register("k");
        session = members.login(mvc, owner);
        topicId = jdbc.queryForObject("select min(topic_id) from topic", Long.class);
    }

    @AfterEach
    void cleanUp() {
        jdbc.update("delete from post where category_id in (select category_id from category where blog_id = ?)", owner.blogId());
        jdbc.update("delete from category where blog_id = ?", owner.blogId());
        jdbc.update("delete from blog where blog_id = ?", owner.blogId());
        jdbc.update("delete from users where users_id = ?", owner.id());
    }

    @Test
    void S5_2_같은_키로_동시에_세_번_보내면_글은_하나이고_세_응답의_번호가_같다() throws Exception {
        String key = UUID.randomUUID().toString();
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(3);
        try {
            List<Future<MockHttpServletResponse>> futures = new ArrayList<>();
            for (int i = 0; i < 3; i++) {
                futures.add(pool.submit((Callable<MockHttpServletResponse>) () -> {
                    start.await();
                    return send(key);
                }));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            List<String> postIds = new ArrayList<>();
            for (Future<MockHttpServletResponse> future : futures) {
                MockHttpServletResponse response = future.get();
                statuses.add(response.getStatus());
                postIds.add(JsonPath.read(response.getContentAsString(), "$.postId").toString());
            }
            assertThat(statuses).containsOnly(200, 201).filteredOn(s -> s == 201).hasSize(1);
            assertThat(postIds).containsOnly(postIds.get(0));
        } finally {
            pool.shutdown();
        }
        assertThat(postCount()).isEqualTo(1);
    }

    @Test
    void S5_2_같은_키로_다시_보내면_200과_처음_번호() throws Exception {
        String key = UUID.randomUUID().toString();
        MockHttpServletResponse first = send(key);
        MockHttpServletResponse second = send(key);
        assertThat(first.getStatus()).isEqualTo(201);
        assertThat(second.getStatus()).isEqualTo(200);
        assertThat(second.getContentAsString()).isEqualTo(first.getContentAsString());
        assertThat(postCount()).isEqualTo(1);
    }

    @Test
    void S5_3_다른_키면_같은_내용이어도_글_두_개() throws Exception {
        assertThat(send(UUID.randomUUID().toString()).getStatus()).isEqualTo(201);
        assertThat(send(UUID.randomUUID().toString()).getStatus()).isEqualTo(201);
        assertThat(postCount()).isEqualTo(2);
    }

    private MockHttpServletResponse send(String key) throws Exception {
        return mvc.perform(post("/api/posts").with(csrf()).session(session).contentType(MediaType.APPLICATION_JSON)
                        .content(TestJson.of("title", "연속", "content", "본문", "categoryId", owner.defaultCategoryId(),
                                "topicId", topicId, "requestKey", key)))
                .andReturn().getResponse();
    }

    private long postCount() {
        return jdbc.queryForObject("select count(*) from post p join category c using (category_id) where c.blog_id = ?",
                Long.class, owner.blogId());
    }
}
