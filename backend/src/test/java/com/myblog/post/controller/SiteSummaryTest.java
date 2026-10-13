package com.myblog.post.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import com.myblog.support.TestPosts;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * 첫 화면 머리의 "원고지 표지" 분량 (v0.5.1): 공개 글을 쓴 블로그 수, 공개 글 수, 원고지 매수 합.
 * DB에 다른 글(예시 데이터)이 있어도 되게, 글을 넣기 전과 뒤의 차이로 본다.
 */
// 넣기 전후를 비교하므로 센 값을 다시 쓰지 않는다 (summary-ttl 0)
@SpringBootTest(properties = "explore.feed.summary-ttl=0s")
@Transactional
@Import({TestMembers.class, TestPosts.class})
class SiteSummaryTest {

    private static final Instant BASE = Instant.parse("2026-01-01T00:00:00Z");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private TestPosts posts;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void 공개_글만_세고_매수는_글마다_올림해_더한다() throws Exception {
        DocumentContext before = summary();
        Member writer = members.register("a");
        members.register("b"); // 글이 없는 블로그는 쓴 사람으로 세지 않는다
        posts.add(writer.defaultCategoryId(), "긴 글", "가".repeat(250), "public", BASE);
        posts.add(writer.defaultCategoryId(), "짧은 글", "짧다", "public", BASE.plusSeconds(60));
        posts.add(writer.defaultCategoryId(), "비공개 글", "나만 본다", "private", BASE.plusSeconds(120));

        DocumentContext after = summary();
        assertThat(number(after, "bloggerCount") - number(before, "bloggerCount")).isEqualTo(1);
        assertThat(number(after, "postCount") - number(before, "postCount")).isEqualTo(2);
        assertThat(number(after, "sheets") - number(before, "sheets")).isEqualTo(3);
    }

    private DocumentContext summary() throws Exception {
        String body = mvc.perform(get("/api/v1/posts/summary"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.parse(body);
    }

    private static long number(DocumentContext json, String field) {
        return json.read("$." + field, Long.class);
    }
}
