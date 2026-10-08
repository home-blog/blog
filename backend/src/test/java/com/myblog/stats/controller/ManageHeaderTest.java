package com.myblog.stats.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * 관리 화면 머리 정보 (specs/006 US1, T013, quickstart S-1의 4·5, FR-002, FR-003, FR-005, SC-001).
 * 블로그는 세션의 회원으로만 찾는다: 주소에 블로그 번호를 넣을 곳이 없다.
 */
@SpringBootTest
@Transactional
@Import(TestMembers.class)
class ManageHeaderTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void S1_4_로그인하지_않으면_401() throws Exception {
        mvc.perform(get("/api/manage/blog"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
    }

    @Test
    void S1_5_회원은_자기_블로그의_이름과_주소만_받는다() throws Exception {
        Member a = members.register("a");
        Member b = members.register("b");
        MockHttpSession session = members.login(mvc, b);

        mvc.perform(get("/api/manage/blog").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.blogId").value(b.blogId()))
                .andExpect(jsonPath("$.name").value(b.user().getNickname() + "의 블로그"))
                .andExpect(jsonPath("$.intro").value(""))
                .andExpect(jsonPath("$.blogPath").value("/blog/" + b.blogId()));
        // 블로그 번호를 실어 보내도 무시한다 (주소로 블로그를 고르지 않는다)
        mvc.perform(get("/api/manage/blog").param("blogId", a.blogId().toString()).session(session))
                .andExpect(jsonPath("$.blogId").value(b.blogId()));
    }

    @Test
    void 탈퇴한_회원의_세션이면_401() throws Exception {
        Member member = members.register("w");
        MockHttpSession session = members.login(mvc, member);
        MockHttpSession kept = members.login(mvc, member);
        mvc.perform(post("/api/account/withdrawal").with(csrf()).session(session).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"" + TestMembers.PASSWORD + "\",\"agreed\":true}"))
                .andExpect(status().isOk());

        mvc.perform(get("/api/manage/blog").session(kept)).andExpect(status().isUnauthorized());
    }
}
