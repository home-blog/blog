package com.myblog.blog.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myblog.support.TestJson;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/** 블로그 이름과 소개 고치기 (specs/003 US7, quickstart S-11, T047). 실제 PostgreSQL로 확인한다. */
@SpringBootTest
@Transactional
@Import(TestMembers.class)
class BlogSettingsTest {

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
        owner = members.register("a");
        session = members.login(mvc, owner);
    }

    @Test
    void S11_1_2_바꾼_이름과_소개가_블로그와_글_상세에_바로_보이고_앞뒤_공백은_지운다() throws Exception {
        Long postId = members.addPost(owner.defaultCategoryId(), "글", "public");
        save(TestJson.of("name", "  철수의 여행 일기  ", "intro", " 주말마다 떠납니다 "))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blogId").value(owner.blogId()))
                .andExpect(jsonPath("$.name").value("철수의 여행 일기"))
                .andExpect(jsonPath("$.intro").value("주말마다 떠납니다"));

        mvc.perform(get("/api/blogs/{id}", owner.blogId()))
                .andExpect(jsonPath("$.name").value("철수의 여행 일기"))
                .andExpect(jsonPath("$.intro").value("주말마다 떠납니다"));
        mvc.perform(get("/api/posts/{id}", postId)).andExpect(jsonPath("$.blogName").value("철수의 여행 일기"));
    }

    @Test
    void S11_3_4_빈_이름_공백_이름_31자_201자는_거절() throws Exception {
        expectFieldError(save(TestJson.of("name", "", "intro", "")), "name", "※ 블로그 이름을 입력해 주세요");
        expectFieldError(save(TestJson.of("name", "   ", "intro", "")), "name", "※ 블로그 이름을 입력해 주세요");
        expectFieldError(save(TestJson.of("intro", "")), "name", "※ 블로그 이름을 입력해 주세요");
        expectFieldError(save(TestJson.of("name", "가".repeat(31), "intro", "")), "name", "※ 블로그 이름은 30자 이하로 입력해 주세요");
        expectFieldError(save(TestJson.of("name", "이름", "intro", "가".repeat(201))), "intro", "※ 소개는 200자 이하로 입력해 주세요");

        save(TestJson.of("name", "😀".repeat(30), "intro", "가".repeat(200))).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select name from blog where blog_id = ?", String.class, owner.blogId()))
                .isEqualTo("😀".repeat(30));
    }

    @Test
    void S11_5_소개는_비워도_되고_비면_빈_글자로_준다() throws Exception {
        save(TestJson.of("name", "이름", "intro", "잠깐")).andExpect(status().isOk());
        save(TestJson.of("name", "이름", "intro", "   ")).andExpect(jsonPath("$.intro").value(""));
        save(TestJson.of("name", "이름")).andExpect(jsonPath("$.intro").value(""));
        assertThat(jdbc.queryForObject("select intro from blog where blog_id = ?", String.class, owner.blogId())).isNull();
        mvc.perform(get("/api/me/blog").session(session)).andExpect(jsonPath("$.intro").value(""));
    }

    @Test
    void S11_6_요청에_남의_블로그_번호를_넣어도_내_블로그만_바뀐다() throws Exception {
        Member other = members.register("b");
        String before = jdbc.queryForObject("select name from blog where blog_id = ?", String.class, other.blogId());
        save(TestJson.of("name", "바뀐 이름", "intro", "", "blogId", other.blogId())).andExpect(status().isOk());
        assertThat(jdbc.queryForObject("select name from blog where blog_id = ?", String.class, other.blogId())).isEqualTo(before);
        assertThat(jdbc.queryForObject("select name from blog where blog_id = ?", String.class, owner.blogId())).isEqualTo("바뀐 이름");
    }

    @Test
    void S11_7_블로그를_지우는_주소는_없다() throws Exception {
        mvc.perform(delete("/api/me/blog").with(csrf()).session(session)).andExpect(status().isMethodNotAllowed());
        assertThat(jdbc.queryForObject("select count(*) from blog where blog_id = ?", Long.class, owner.blogId())).isOne();
    }

    @Test
    void 로그인하지_않으면_401이고_CSRF가_없으면_거절() throws Exception {
        mvc.perform(put("/api/me/blog").with(csrf()).contentType(MediaType.APPLICATION_JSON).content(TestJson.of("name", "x")))
                .andExpect(status().isUnauthorized());
        mvc.perform(put("/api/me/blog").session(session).contentType(MediaType.APPLICATION_JSON).content(TestJson.of("name", "x")))
                .andExpect(status().isForbidden());
    }

    @Test
    void S10_2_소개_200자는_통과하고_저장_뒤_관리_화면_머리_이름도_새_값이다() throws Exception {
        String intro = "가".repeat(199) + "😀";
        save(TestJson.of("name", "새 이름", "intro", intro))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.intro").value(intro));
        // specs/006 T037, FR-040: 관리 화면 위쪽 이름은 다음 요청부터 새 값
        mvc.perform(get("/api/manage/blog").session(session))
                .andExpect(jsonPath("$.name").value("새 이름"))
                .andExpect(jsonPath("$.intro").value(intro));
    }

    private ResultActions save(String json) throws Exception {
        return mvc.perform(put("/api/me/blog").with(csrf()).session(session).contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private static void expectFieldError(ResultActions result, String field, String message) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors.length()").value(1))
                .andExpect(jsonPath("$.fieldErrors[0].field").value(field))
                .andExpect(jsonPath("$.fieldErrors[0].message").value(message));
    }
}
