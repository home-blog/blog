package com.myblog.common.instance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

/** 앱 두 벌(specs/011): 이름이 있으면 응답에 X-Instance를 붙이고, 비어 있으면(로컬) 붙이지 않는다. */
class InstanceHeaderFilterTest {

    @Test
    void 이름이_있으면_응답에_붙인다() throws Exception {
        MockHttpServletResponse response = run(new InstanceProperties("a"));
        assertThat(response.getHeader(InstanceHeaderFilter.HEADER)).isEqualTo("a");
    }

    @Test
    void 이름이_없으면_붙이지_않는다() throws Exception {
        assertThat(run(new InstanceProperties(null)).getHeader(InstanceHeaderFilter.HEADER)).isNull();
        assertThat(run(new InstanceProperties("  ")).getHeader(InstanceHeaderFilter.HEADER)).isNull();
    }

    @Test
    void 머리글에_넣을_수_없는_이름은_서버를_켜지_않는다() {
        assertThatThrownBy(() -> new InstanceProperties("a\r\nX-Evil: 1"))
                .isInstanceOf(IllegalStateException.class);
    }

    private static MockHttpServletResponse run(InstanceProperties instance) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/auth/csrf");
        MockHttpServletResponse response = new MockHttpServletResponse();
        new InstanceHeaderFilter(instance).doFilter(request, response, new MockFilterChain());
        return response;
    }
}
