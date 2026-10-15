package com.myblog.notice;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * 공지사항 (specs/012). 받아들이는 기준(S1-1 ~ S5-2)을 정상·경계·예외값으로 확인한다.
 * 관리자 확인은 요청마다 DB를 본다(D-2): 관리자에서 빼거나 탈퇴하면 다시 로그인하지 않아도 바로 막힌다.
 */
@SpringBootTest
@Transactional
@Import(TestMembers.class)
class NoticeApiTest {

    private static final String NOTICES = "/api/v1/notices";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private Member admin;
    private Member member;
    private MockHttpSession adminSession;
    private MockHttpSession memberSession;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        jdbc.update("delete from notice");
        admin = members.register("adm");
        member = members.register("mem");
        jdbc.update("update users set role = 'admin' where users_id = ?", admin.id());
        adminSession = members.login(mvc, admin);
        memberSession = members.login(mvc, member);
    }

    private static String body(String kind, String title, String content, String version, boolean pinned) {
        return """
                {"kind":%s,"title":%s,"content":%s,"releaseVersion":%s,"pinned":%s}"""
                .formatted(json(kind), json(title), json(content), json(version), pinned);
    }

    private static String json(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n") + "\"";
    }

    private MockHttpServletRequestBuilder write(String body, MockHttpSession session) {
        return post(NOTICES).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON).content(body);
    }

    private Long insert(String kind, String title, String version, boolean pinned, Instant createdAt) {
        return jdbc.queryForObject("insert into notice (kind, title, content, release_version, pinned, users_id, created_at)"
                + " values (?, ?, '본문', ?, ?, ?, ?) returning notice_id", Long.class,
                kind, title, version, pinned, admin.id(), Timestamp.from(createdAt));
    }

    @Nested
    @DisplayName("관리자")
    class AdminOnly {

        @Test
        void S5_1_auth_me가_관리자인지_알려_준다() throws Exception {
            mvc.perform(get("/api/v1/auth/me").session(adminSession))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.member.admin").value(true));
            mvc.perform(get("/api/v1/auth/me").session(memberSession))
                    .andExpect(jsonPath("$.member.admin").value(false));
        }

        @Test
        void S4_5_로그인하지_않으면_401() throws Exception {
            mvc.perform(post(NOTICES).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                            .content(body("notice", "제목", "본문", null, false)))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void S4_5_일반_회원은_쓰기_고치기_지우기가_403() throws Exception {
            Long id = insert("notice", "있는 글", null, false, Instant.now());
            mvc.perform(write(body("notice", "제목", "본문", null, false), memberSession))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("FORBIDDEN"));
            mvc.perform(put(NOTICES + "/" + id).session(memberSession).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(body("notice", "바꿈", "본문", null, false)))
                    .andExpect(status().isForbidden());
            mvc.perform(delete(NOTICES + "/" + id).session(memberSession).with(csrf()))
                    .andExpect(status().isForbidden());
            mvc.perform(get(NOTICES + "/" + id)).andExpect(jsonPath("$.title").value("있는 글"));
        }

        @Test
        void S5_2_관리자에서_빼면_다시_로그인하지_않아도_403() throws Exception {
            jdbc.update("update users set role = 'member' where users_id = ?", admin.id());
            mvc.perform(write(body("notice", "제목", "본문", null, false), adminSession))
                    .andExpect(status().isForbidden());
        }

        @Test
        void S4_7_탈퇴한_관리자는_401() throws Exception {
            jdbc.update("update users set deleted_at = now() where users_id = ?", admin.id());
            mvc.perform(write(body("notice", "제목", "본문", null, false), adminSession))
                    .andExpect(status().isUnauthorized());
        }

        @Test
        void CSRF_토큰이_없으면_403() throws Exception {
            mvc.perform(post(NOTICES).session(adminSession).contentType(MediaType.APPLICATION_JSON)
                            .content(body("notice", "제목", "본문", null, false)))
                    .andExpect(status().isForbidden())
                    .andExpect(jsonPath("$.code").value("CSRF_TOKEN_INVALID"));
        }
    }

    @Nested
    @DisplayName("쓰기")
    class Writing {

        @Test
        void S4_1_업데이트_글을_고정해_쓰면_목록_맨_위에_보인다() throws Exception {
            insert("notice", "예전 글", null, false, Instant.now().minus(1, ChronoUnit.DAYS));
            mvc.perform(write(body("update", "공지사항이 생겼어요", "본문", "0.6.0", true), adminSession))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.noticeId").isNumber());
            mvc.perform(get(NOTICES))
                    .andExpect(jsonPath("$.items[0].title").value("공지사항이 생겼어요"))
                    .andExpect(jsonPath("$.items[0].kind").value("update"))
                    .andExpect(jsonPath("$.items[0].releaseVersion").value("0.6.0"))
                    .andExpect(jsonPath("$.items[0].pinned").value(true));
        }

        @Test
        void S4_2_제목은_1자와_100자가_되고_이모지는_한_글자() throws Exception {
            mvc.perform(write(body("notice", "가", "본문", null, false), adminSession)).andExpect(status().isCreated());
            mvc.perform(write(body("notice", "가".repeat(99) + "😀", "본문", null, false), adminSession))
                    .andExpect(status().isCreated());
        }

        @Test
        void S4_2_제목이_비었거나_101자면_400() throws Exception {
            mvc.perform(write(body("notice", "   ", "본문", null, false), adminSession))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("title"))
                    .andExpect(jsonPath("$.fieldErrors[0].code").value("TITLE_REQUIRED"));
            mvc.perform(write(body("notice", "가".repeat(101), "본문", null, false), adminSession))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].code").value("TITLE_TOO_LONG"));
        }

        @Test
        void 본문은_10000자까지_되고_10001자와_빈_본문은_400() throws Exception {
            mvc.perform(write(body("notice", "제목", "가".repeat(10_000), null, false), adminSession))
                    .andExpect(status().isCreated());
            mvc.perform(write(body("notice", "제목", "가".repeat(10_001), null, false), adminSession))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].code").value("CONTENT_TOO_LONG"));
            mvc.perform(write(body("notice", "제목", "\n  ", null, false), adminSession))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].code").value("CONTENT_REQUIRED"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"0.6.0", "0.10.12", "1234.0.0"})
        void S4_3_릴리스_번호_정상(String version) throws Exception {
            mvc.perform(write(body("update", "제목", "본문", version, false), adminSession)).andExpect(status().isCreated());
        }

        @ParameterizedTest
        @ValueSource(strings = {"v0.6.0", "0.6", "0.6.0.1", "abc", "12345.0.0", "0.6.0 ", ""})
        void S4_3_릴리스_번호_모양이_틀리면_400(String version) throws Exception {
            mvc.perform(write(body("update", "제목", "본문", version, false), adminSession))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("releaseVersion"));
        }

        @Test
        void S4_3_업데이트인데_번호가_없으면_400() throws Exception {
            mvc.perform(write(body("update", "제목", "본문", null, false), adminSession))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].code").value("RELEASE_VERSION_REQUIRED"));
        }

        @Test
        void S4_4_공지에_번호를_넣으면_400() throws Exception {
            mvc.perform(write(body("notice", "제목", "본문", "0.6.0", false), adminSession))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].code").value("RELEASE_VERSION_NOT_ALLOWED"));
        }

        @ParameterizedTest
        @ValueSource(strings = {"event", "UPDATE", ""})
        void 모르는_종류는_400(String kind) throws Exception {
            mvc.perform(write(body(kind, "제목", "본문", null, false), adminSession))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].code").value("NOTICE_KIND_INVALID"));
        }

        @Test
        void 고치면_내용과_고친_시각이_바뀐다() throws Exception {
            Long id = insert("notice", "처음 제목", null, false, Instant.now());
            mvc.perform(put(NOTICES + "/" + id).session(adminSession).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                            .content(body("update", "고친 제목", "고친 본문", "0.6.1", true)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.noticeId").value(id));
            mvc.perform(get(NOTICES + "/" + id))
                    .andExpect(jsonPath("$.title").value("고친 제목"))
                    .andExpect(jsonPath("$.content").value("고친 본문"))
                    .andExpect(jsonPath("$.kind").value("update"))
                    .andExpect(jsonPath("$.releaseVersion").value("0.6.1"))
                    .andExpect(jsonPath("$.pinned").value(true))
                    .andExpect(jsonPath("$.updatedAt").isNotEmpty());
        }

        @Test
        void S4_6_지우면_읽기가_404() throws Exception {
            Long id = insert("notice", "지울 글", null, false, Instant.now());
            mvc.perform(delete(NOTICES + "/" + id).session(adminSession).with(csrf())).andExpect(status().isNoContent());
            mvc.perform(get(NOTICES + "/" + id))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.code").value("NOTICE_NOT_FOUND"));
        }

        @Test
        void 없는_글을_고치거나_지우면_404() throws Exception {
            mvc.perform(put(NOTICES + "/999999999").session(adminSession).with(csrf())
                            .contentType(MediaType.APPLICATION_JSON).content(body("notice", "제목", "본문", null, false)))
                    .andExpect(status().isNotFound());
            mvc.perform(delete(NOTICES + "/999999999").session(adminSession).with(csrf()))
                    .andExpect(status().isNotFound());
        }
    }

    @Nested
    @DisplayName("보기")
    class Reading {

        @Test
        void S1_1_로그인하지_않아도_목록과_읽기가_보인다() throws Exception {
            Long id = insert("notice", "점검 안내", null, false, Instant.now());
            mvc.perform(get(NOTICES)).andExpect(status().isOk()).andExpect(jsonPath("$.items", hasSize(1)));
            mvc.perform(get(NOTICES + "/" + id)).andExpect(status().isOk()).andExpect(jsonPath("$.content").value("본문"));
        }

        @Test
        void S1_2_탭은_종류별로_거른다() throws Exception {
            Instant now = Instant.now();
            insert("update", "업데이트", "0.6.0", false, now);
            insert("notice", "공지", null, false, now.minusSeconds(1));
            mvc.perform(get(NOTICES).param("kind", "update")).andExpect(jsonPath("$.items[*].title", contains("업데이트")));
            mvc.perform(get(NOTICES).param("kind", "notice")).andExpect(jsonPath("$.items[*].title", contains("공지")));
            mvc.perform(get(NOTICES)).andExpect(jsonPath("$.items[*].title", contains("업데이트", "공지")));
        }

        @Test
        void S1_5_모르는_종류는_전체로_본다() throws Exception {
            insert("update", "업데이트", "0.6.0", false, Instant.now());
            insert("notice", "공지", null, false, Instant.now().minusSeconds(1));
            mvc.perform(get(NOTICES).param("kind", "xyz")).andExpect(jsonPath("$.items", hasSize(2)));
        }

        @Test
        void S1_3_고정_먼저_그_안에서_새_글_먼저() throws Exception {
            Instant now = Instant.now();
            insert("notice", "보통-옛날", null, false, now.minusSeconds(50));
            insert("notice", "고정-옛날", null, true, now.minusSeconds(40));
            insert("notice", "보통-새", null, false, now.minusSeconds(10));
            insert("update", "고정-새", "0.6.0", true, now.minusSeconds(20));
            insert("notice", "보통-가운데", null, false, now.minusSeconds(30));
            mvc.perform(get(NOTICES)).andExpect(jsonPath("$.items[*].title",
                    contains("고정-새", "고정-옛날", "보통-새", "보통-가운데", "보통-옛날")));
        }

        @Test
        void S1_4_11개면_1쪽에_10개_2쪽에_1개_넘는_쪽은_마지막_쪽() throws Exception {
            Instant now = Instant.now();
            for (int i = 1; i <= 11; i++) {
                insert("notice", "글" + i, null, false, now.minusSeconds(100 - i));
            }
            mvc.perform(get(NOTICES))
                    .andExpect(jsonPath("$.items", hasSize(10)))
                    .andExpect(jsonPath("$.page").value(1))
                    .andExpect(jsonPath("$.totalPages").value(2))
                    .andExpect(jsonPath("$.totalCount").value(11));
            mvc.perform(get(NOTICES).param("page", "2")).andExpect(jsonPath("$.items[*].title", contains("글1")));
            mvc.perform(get(NOTICES).param("page", "99")).andExpect(jsonPath("$.page").value(2));
            mvc.perform(get(NOTICES).param("page", "abc")).andExpect(jsonPath("$.page").value(1));
        }

        @Test
        void S2_1_업데이트_글에는_릴리스_링크가_있다() throws Exception {
            Long id = insert("update", "새 기능", "0.6.0", false, Instant.now());
            mvc.perform(get(NOTICES + "/" + id))
                    .andExpect(jsonPath("$.releaseVersion").value("0.6.0"))
                    .andExpect(jsonPath("$.releaseUrl").value("https://github.com/home-blog/blog/releases/tag/v0.6.0"));
        }

        @Test
        void S2_2_공지_글에는_릴리스_링크가_없다() throws Exception {
            Long id = insert("notice", "점검", null, false, Instant.now());
            mvc.perform(get(NOTICES + "/" + id))
                    .andExpect(jsonPath("$.releaseVersion").value(nullValue()))
                    .andExpect(jsonPath("$.releaseUrl").value(nullValue()));
        }

        @Test
        void S2_3_없는_번호는_404() throws Exception {
            mvc.perform(get(NOTICES + "/999999999")).andExpect(status().isNotFound());
        }

        @Test
        void 최신_글_시각은_글이_없으면_null이고_있으면_가장_새_글() throws Exception {
            mvc.perform(get(NOTICES + "/latest")).andExpect(jsonPath("$.latestAt").value(nullValue()));
            Instant newest = Instant.parse("2026-10-15T09:00:00Z");
            insert("notice", "옛날", null, true, newest.minusSeconds(3600));
            insert("notice", "새", null, false, newest);
            mvc.perform(get(NOTICES + "/latest")).andExpect(jsonPath("$.latestAt").value("2026-10-15T09:00:00Z"));
        }
    }
}
