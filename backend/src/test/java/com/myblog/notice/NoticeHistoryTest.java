package com.myblog.notice;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * Flyway V13이 처음 넣는 공지사항 (지금까지의 릴리스 소식과 이용 안내)과 릴리스마다 더하는 업데이트 글(V15~).
 * 다른 공지 테스트는 표를 비우고 시작하지만 모두 되돌리므로(@Transactional), 여기서는 V13이 넣은 그대로를 본다.
 * 개발 DB에는 관리자가 써 본 글이 있을 수 있어, 쓴 사람이 있는 글은 이 테스트 안에서만 지우고 본다.
 */
@SpringBootTest
@Transactional
class NoticeHistoryTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        jdbc.update("delete from notice where users_id is not null");
    }

    @Test
    void 릴리스마다_업데이트_글이_하나씩_있다() {
        List<String> versions = jdbc.queryForList(
                "select release_version from notice where kind = 'update' order by created_at", String.class);
        assertThat(versions).containsExactly("0.3.0", "0.4.0", "0.5.0", "0.5.1", "0.5.2", "0.6.0", "0.7.0", "0.8.0", "0.9.0", "0.10.0", "0.11.0", "0.12.0", "0.13.0");
    }

    @Test
    void 공지_글은_여섯_개이고_릴리스_번호가_없다() {
        Integer notices = jdbc.queryForObject(
                "select count(*) from notice where kind = 'notice' and release_version is null", Integer.class);
        assertThat(notices).isEqualTo(6);
    }

    @Test
    void 운영팀이_넣은_글이라_쓴_사람이_비어_있다() {
        Integer seeded = jdbc.queryForObject("select count(*) from notice where users_id is null", Integer.class);
        assertThat(seeded).isEqualTo(19);
    }

    @Test
    void 제목과_본문은_공지사항_글자_수_안에_있다() {
        // 관리자가 그대로 고쳐 저장할 수 있어야 한다 (notice.title 100자, notice.content 10,000자)
        Integer outOfRange = jdbc.queryForObject("""
                select count(*) from notice
                where char_length(title) not between 1 and 100
                   or char_length(content) not between 1 and 10000
                   or content <> btrim(content, E' \\n')""", Integer.class);
        assertThat(outOfRange).isZero();
    }

    @Test
    void 첫_쪽에는_고정한_이용_안내와_최신_업데이트가_맨_위에_있다() throws Exception {
        mvc.perform(get("/api/v1/notices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(19))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.items", hasSize(10)))
                .andExpect(jsonPath("$.items[0].releaseVersion").value("0.13.0"))
                .andExpect(jsonPath("$.items[0].pinned").value(true))
                .andExpect(jsonPath("$.items[1].title").value("글결 이용 안내"))
                .andExpect(jsonPath("$.items[1].pinned").value(true))
                .andExpect(jsonPath("$.items[2].pinned").value(false));
    }

    @Test
    void 업데이트_탭은_새_릴리스부터() throws Exception {
        // 한 쪽은 10개라 오래된 0.5.0, 0.4.0, 0.3.0은 둘째 쪽으로 넘어간다
        mvc.perform(get("/api/v1/notices").param("kind", "update"))
                .andExpect(jsonPath("$.items[*].releaseVersion",
                        contains("0.13.0", "0.12.0", "0.11.0", "0.10.0", "0.9.0", "0.8.0", "0.7.0", "0.6.0", "0.5.2", "0.5.1")));
        mvc.perform(get("/api/v1/notices").param("kind", "update").param("page", "2"))
                .andExpect(jsonPath("$.items[*].releaseVersion", contains("0.5.0", "0.4.0", "0.3.0")));
    }

    @Test
    void 새_업데이트_글이_오면_앞_업데이트의_고정을_푼다() {
        List<String> pinned = jdbc.queryForList(
                "select coalesce(release_version, title) from notice where pinned order by created_at", String.class);
        assertThat(pinned).containsExactly("글결 이용 안내", "0.13.0");
    }
}
