package com.myblog.stats;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import com.myblog.support.TestClock;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** 인기 검색어 최소 횟수 (specs/015 D-4, 기본 2): 한 사람의 한 번 검색은 순위에 드러나지 않는다. */
@SpringBootTest
@Transactional
@Import({TestMembers.class, TestClock.Config.class})
class SearchTrendMinCountTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private TestClock clock;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private String word;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        clock.set(TestClock.distinctFarFutureSlot());
        word = "드문말" + UUID.randomUUID().toString().substring(0, 6);
        Member writer = members.register("trendmin");
        jdbc.update("insert into post (category_id, topic_id, title, content, visibility, created_at)"
                + " values (?, (select min(topic_id) from topic), ?, '본문', 'public', now())",
                writer.defaultCategoryId(), word + " 이야기");
    }

    @AfterEach
    void tearDown() {
        clock.reset();
    }

    private void search(MockHttpSession visitor) throws Exception {
        mvc.perform(get("/api/v1/search/posts").param("q", word).session(visitor));
    }

    private MockHttpSession visitor() throws Exception {
        return members.login(mvc, members.register("tv"));
    }

    @Test
    void 한_사람이_한_번_찾은_말은_보이지_않고_두_번째부터_보인다() throws Exception {
        search(visitor());
        mvc.perform(get("/api/v1/search/trending")).andExpect(jsonPath("$.items[?(@.keyword == '" + word + "')]").isEmpty());

        search(visitor());
        mvc.perform(get("/api/v1/search/trending")).andExpect(jsonPath("$.items[?(@.keyword == '" + word + "')]").isNotEmpty());
    }
}
