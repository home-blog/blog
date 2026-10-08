package com.myblog.blog.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.myblog.support.TestJson;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * 관리 화면의 분류 관리가 더하는 것 (specs/006 US3, T022, T025, quickstart S-3의 4, S-3a의 1·5, FR-018 ~ FR-020, SC-004).
 * 추가·이름·공개·순서·삭제 규칙은 003의 CategoryManageTest가 본다. 색 개수는 설정값 category.color-count(6).
 */
@SpringBootTest
@Transactional
@Import(TestMembers.class)
class CategoryColorTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

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
    void 분류_목록_추가_고치기_응답에_색_번호가_있고_새_분류는_차례로_색을_받는다() throws Exception {
        add("여행").andExpect(status().isCreated()).andExpect(jsonPath("$.colorIndex").value(1));
        Long food = idOf(add("음식").andExpect(jsonPath("$.colorIndex").value(2)));
        mvc.perform(patch("/api/me/blog/categories/{id}", food).with(csrf()).session(session)
                        .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("name", "맛집")))
                .andExpect(jsonPath("$.colorIndex").value(2));
        mvc.perform(get("/api/blogs/{id}/categories", owner.blogId()).session(session))
                .andExpect(jsonPath("$.categories[0].colorIndex").value(0))
                .andExpect(jsonPath("$.categories[1].colorIndex").value(1))
                .andExpect(jsonPath("$.categories[2].colorIndex").value(2));
    }

    @Test
    void 색은_6개를_돌고_나면_처음부터_다시() throws Exception {
        for (int i = 1; i <= 5; i++) {
            add("분류" + i).andExpect(jsonPath("$.colorIndex").value(i));
        }
        add("분류6").andExpect(jsonPath("$.colorIndex").value(0));
    }

    @Test
    void S3_4_순서를_바꿔도_각_분류의_색은_그대로() throws Exception {
        Long travel = idOf(add("여행"));
        Long food = idOf(add("음식"));
        mvc.perform(put("/api/me/blog/categories/order").with(csrf()).session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"categoryIds\":[" + food + "," + owner.defaultCategoryId() + "," + travel + "]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[0].categoryId").value(food))
                .andExpect(jsonPath("$.categories[0].colorIndex").value(2))
                .andExpect(jsonPath("$.categories[1].colorIndex").value(0))
                .andExpect(jsonPath("$.categories[2].colorIndex").value(1));
    }

    @Test
    void S3a_1_주인이_보는_글_개수는_비공개_글까지() throws Exception {
        Long secret = members.addCategory(owner.blogId(), "비밀", "private", 2);
        members.addPost(secret, "비공개 글", "private");
        members.addPost(secret, "공개 글", "public");
        mvc.perform(get("/api/blogs/{id}/categories", owner.blogId()).session(session))
                .andExpect(jsonPath("$.categories[1].postCount").value(2))
                .andExpect(jsonPath("$.categories[1].visibility").value("private"));
    }

    @Test
    void S3a_5_회원_B의_요청은_A의_분류를_바꾸지_못하고_404() throws Exception {
        Long travel = idOf(add("여행"));
        Member b = members.register("b");
        MockHttpSession bSession = members.login(mvc, b);
        mvc.perform(patch("/api/me/blog/categories/{id}", travel).with(csrf()).session(bSession)
                        .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("visibility", "private")))
                .andExpect(status().isNotFound());
        mvc.perform(delete("/api/me/blog/categories/{id}", travel).with(csrf()).session(bSession))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/blogs/{id}/categories", owner.blogId()).session(session))
                .andExpect(jsonPath("$.categories[1].name").value("여행"))
                .andExpect(jsonPath("$.categories[1].visibility").value("public"));
    }

    private ResultActions add(String name) throws Exception {
        return mvc.perform(post("/api/me/blog/categories").with(csrf()).session(session)
                .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("name", name)));
    }

    private static Long idOf(ResultActions result) throws Exception {
        return ((Number) JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.categoryId")).longValue();
    }
}
