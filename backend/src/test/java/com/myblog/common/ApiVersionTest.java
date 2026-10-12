package com.myblog.common;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/** API 주소 버전 (ApiVersionConfig): /api/v1/… 만 받고, 버전이 없거나 모르는 버전은 로그인과 상관없이 400. */
@SpringBootTest
class ApiVersionTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    void v1_주소로_답한다() throws Exception {
        mvc.perform(get("/api/v1/posts")).andExpect(status().isOk());
        mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk());
    }

    @Test
    void 버전이_없는_예전_주소는_400() throws Exception {
        mvc.perform(get("/api/posts"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("API_VERSION_UNSUPPORTED"))
                .andExpect(jsonPath("$.message").value("※ 지원하지 않는 API 버전입니다"));
    }

    @Test
    void 지원하지_않는_버전은_400() throws Exception {
        mvc.perform(get("/api/v9/posts"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("API_VERSION_UNSUPPORTED"));
    }

    @Test
    void 버전_칸은_v와_숫자만_받는다() throws Exception {
        for (String path : new String[] {"/api/1/posts", "/api/v1.0/posts", "/api/V1/posts", "/api/v01/posts",
                "/api/v2147483648/posts"}) {
            mvc.perform(get(path))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("API_VERSION_UNSUPPORTED"));
        }
    }

    @Test
    void 버전이_없는_주소로_보낸_변경_요청도_CSRF보다_버전_오류가_먼저() throws Exception {
        mvc.perform(post("/api/posts").contentType("application/json").content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("API_VERSION_UNSUPPORTED"));
    }

    @Test
    void v1_주소의_변경_요청은_그대로_CSRF를_검사한다() throws Exception {
        mvc.perform(post("/api/v1/posts").contentType("application/json").content("{}"))
                .andExpect(status().isForbidden());
    }

    @Test
    void 로그인이_필요한_v1_주소는_그대로_401() throws Exception {
        mvc.perform(get("/api/v1/manage/blog")).andExpect(status().isUnauthorized());
    }

    @Test
    void 버전이_없는_주소는_버전을_따지지_않는다() throws Exception {
        mvc.perform(get("/robots.txt")).andExpect(status().isOk());
        mvc.perform(get("/api/images/00000000-0000-0000-0000-000000000000.png")).andExpect(status().isNotFound());
    }
}
