package com.myblog.stats.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myblog.support.TestComments;
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
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 숫자 세기가 실패해도 글은 보인다 (specs/006 T050, quickstart S-12, research B-3, NF-09).
 * Redis에 연결할 수 없는 것처럼 만들어도 글 상세는 200이고 숫자는 그대로, 관리 화면 주소도 정상이다.
 */
@SpringBootTest
@Import({TestMembers.class, TestComments.class})
class ViewCountingFailureTest {

    @MockitoBean
    private StringRedisTemplate redis;

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

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        when(redis.opsForValue()).thenThrow(new RedisConnectionFailureException("Redis에 연결할 수 없음 (테스트)"));
    }

    @AfterEach
    void cleanUp() {
        created.forEach(testComments::deleteMember);
    }

    @Test
    void S12_Redis가_없어도_글_상세는_200이고_숫자는_그대로_관리_화면도_정상() throws Exception {
        Member owner = register("o");
        Member reader = register("r");
        Long postId = members.addPost(owner.defaultCategoryId(), "글", "public");
        MockHttpSession readerSession = members.login(mvc, reader);

        mvc.perform(get("/api/posts/{id}", postId).session(readerSession))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("글"));
        mvc.perform(get("/api/posts/{id}", postId)).andExpect(status().isOk());
        mvc.perform(get("/api/blogs/{id}", owner.blogId())).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select views from post where post_id = ?", Integer.class, postId)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from blog_daily_stat where blog_id = ?", Long.class, owner.blogId()))
                .isZero();

        MockHttpSession ownerSession = members.login(mvc, owner);
        mvc.perform(get("/api/manage/dashboard").session(ownerSession)).andExpect(status().isOk());
        mvc.perform(get("/api/manage/stats").session(ownerSession)).andExpect(status().isOk());
        mvc.perform(get("/api/manage/posts").session(ownerSession)).andExpect(status().isOk());
    }

    private Member register(String prefix) {
        Member member = members.register(prefix);
        created.add(member);
        return member;
    }
}
