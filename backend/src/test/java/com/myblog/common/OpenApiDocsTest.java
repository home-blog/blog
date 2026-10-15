package com.myblog.common;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 자동 API 문서(Swagger, /v3/api-docs). 발표·안내에 쓰므로 모든 API에 요약과 묶음(태그)이 있어야 하고,
 * 문서의 "로그인 필요" 표시(세션 쿠키)가 실제 보안 설정(SecurityConfig)과 같아야 한다.
 */
@SpringBootTest
class OpenApiDocsTest {

    @Autowired
    private WebApplicationContext context;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
    }

    @Test
    @SuppressWarnings("unchecked")
    void 모든_API에_요약과_묶음이_있다() throws Exception {
        List<Map<String, Object>> operations = operations();
        assertThat(operations).hasSizeGreaterThanOrEqualTo(60);
        for (Map<String, Object> operation : operations) {
            assertThat(operation.get("summary")).as("%s 요약", operation.get("_path")).isNotNull();
            assertThat((List<?>) operation.get("tags")).as("%s 묶음", operation.get("_path")).hasSize(1);
            // 프레임워크가 넣어 주는 인자(CsrfToken 등)가 요청 값으로 보이면 안 된다: 요청 값은 글자·숫자뿐이다
            List<Map<String, Object>> parameters = (List<Map<String, Object>>) operation.getOrDefault("parameters", List.of());
            for (Map<String, Object> parameter : parameters) {
                assertThat(String.valueOf(parameter.get("schema"))).as("%s의 %s", operation.get("_path"), parameter.get("name"))
                        .doesNotContain("$ref");
            }
        }
    }

    @Test
    void 로그인_필요_표시가_실제_보안_설정과_같다() throws Exception {
        for (Map<String, Object> operation : operations()) {
            if (!"get".equals(operation.get("_method"))) {
                continue;   // GET이 아닌 요청은 CSRF 토큰 없이 403이라 로그인 여부를 볼 수 없다
            }
            String path = ((String) operation.get("_path")).replaceAll("\\{[^}]+}", "1");
            int status = mvc.perform(get(path).session(new MockHttpSession())).andReturn().getResponse().getStatus();
            boolean locked = String.valueOf(operation.get("security")).contains("세션 쿠키");
            if (locked) {
                assertThat(status).as("GET %s는 로그인해야 한다", path).isEqualTo(401);
            } else {
                assertThat(status).as("GET %s는 로그인 없이 부른다", path).isNotEqualTo(401);
            }
        }
    }

    @Test
    void 기능마다_같은_이름의_자료_모양이_서로_덮지_않는다() throws Exception {
        String json = mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        Map<String, Object> schemas = JsonPath.read(json, "$.components.schemas");
        assertThat(schemas).containsKeys("PostRequests.Write", "CommentRequests.Write",
                "PostRequests.Update", "CategoryRequests.Update");
        String postWrite = JsonPath.read(json,
                "$.paths['/api/v1/posts'].post.requestBody.content['application/json'].schema['$ref']");
        assertThat(postWrite).endsWith("/PostRequests.Write");
    }

    /** 문서의 모든 동작. 주소와 방법을 _path, _method로 붙여 돌려준다. */
    private List<Map<String, Object>> operations() throws Exception {
        String json = mvc.perform(get("/v3/api-docs")).andReturn().getResponse().getContentAsString();
        Map<String, Map<String, Map<String, Object>>> paths = JsonPath.read(json, "$.paths");
        List<Map<String, Object>> operations = new ArrayList<>();
        paths.forEach((path, methods) -> methods.forEach((method, operation) -> {
            operation.put("_path", path);
            operation.put("_method", method);
            operations.add(operation);
        }));
        return operations;
    }
}
