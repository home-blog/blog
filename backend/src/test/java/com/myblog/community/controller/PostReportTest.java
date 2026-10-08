package com.myblog.community.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myblog.support.TestComments;
import com.myblog.support.TestJson;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.RequestBuilder;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 글 신고 (specs/005 US6, T034, quickstart S-10, S-11의 신고 줄, FR-017 ~ FR-021, SC-001 ~ SC-003).
 * 동시 요청이 각자 커밋해야 하므로 클래스 전체 @Transactional을 쓰지 않고 끝에서 지운다.
 */
@SpringBootTest
@Import({TestMembers.class, TestComments.class})
class PostReportTest {

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
    private Member owner;
    private Member reader;
    private MockHttpSession ownerSession;
    private MockHttpSession readerSession;
    private Long postId;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = register("o");
        reader = register("r");
        ownerSession = members.login(mvc, owner);
        readerSession = members.login(mvc, reader);
        postId = members.addPost(owner.defaultCategoryId(), "신고할 글", "public");
    }

    @AfterEach
    void cleanUp() {
        created.forEach(testComments::deleteMember);
    }

    @Test
    void S10_1_2_스팸으로_신고하면_접수되고_다시_보내면_409() throws Exception {
        report(readerSession, postId, TestJson.of("reason", "SPAM"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("신고가 접수되었습니다"));
        report(readerSession, postId, TestJson.of("reason", "ABUSE"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_REPORTED"))
                .andExpect(jsonPath("$.message").value("이미 신고한 글입니다"));
        assertThat(reports()).isOne();
        assertThat(jdbc.queryForObject("select reason from post_report where post_id = ?", String.class, postId))
                .isEqualTo("SPAM");
    }

    @Test
    void S10_3_동시에_5번_보내도_한_건만_접수된다() throws Exception {
        List<RequestBuilder> requests = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            requests.add(post("/api/posts/{id}/reports", postId).with(csrf()).session(readerSession)
                    .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("reason", "SPAM")));
        }
        List<Integer> statuses = together(requests);
        assertThat(statuses).filteredOn(code -> code == 201).hasSize(1);
        assertThat(statuses).filteredOn(code -> code == 409).hasSize(4);
        assertThat(reports()).isOne();
    }

    @Test
    void S10_4_5_기타는_빈_설명으로도_되고_200자는_되고_201자는_안_된다() throws Exception {
        Member second = register("s");
        Member third = register("t");
        report(readerSession, postId, TestJson.of("reason", "OTHER")).andExpect(status().isCreated());
        report(members.login(mvc, second), postId, TestJson.of("reason", "OTHER", "detail", "가".repeat(201)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors[0].field").value("detail"))
                .andExpect(jsonPath("$.fieldErrors[0].code").value("REPORT_DETAIL_TOO_LONG"))
                .andExpect(jsonPath("$.fieldErrors[0].message").value("※ 신고 내용은 200자 이하로 입력해 주세요"));
        report(members.login(mvc, third), postId, TestJson.of("reason", "OTHER", "detail", " 광고 링크가\r\n반복됩니다" + "가".repeat(186) + " "))
                .andExpect(status().isCreated());
        assertThat(jdbc.queryForList("select detail from post_report where post_id = ? order by post_report_id", String.class, postId))
                .containsExactly(null, "광고 링크가\n반복됩니다" + "가".repeat(186));
    }

    @Test
    void S10_6_욕설_혐오에_보낸_설명은_저장하지_않는다() throws Exception {
        report(readerSession, postId, TestJson.of("reason", "ABUSE", "detail", "설명")).andExpect(status().isCreated());
        assertThat(jdbc.queryForObject("select detail from post_report where post_id = ?", String.class, postId)).isNull();
    }

    @Test
    void S10_7_자기_글은_403이고_접수되지_않는다() throws Exception {
        report(ownerSession, postId, TestJson.of("reason", "SPAM"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("SELF_REPORT_NOT_ALLOWED"))
                .andExpect(jsonPath("$.message").value("※ 자기 글은 신고할 수 없습니다"));
        // 자기 글이면 형식이 틀려도 403이 먼저다 (research B-1)
        report(ownerSession, postId, "{}").andExpect(status().isForbidden());
        assertThat(reports()).isZero();
    }

    @Test
    void S10_10_사유가_없거나_모르는_값이면_REPORT_REASON_REQUIRED() throws Exception {
        for (String body : new String[] {"{}", TestJson.of("reason", "spam"), TestJson.of("reason", "HATE"),
                TestJson.of("reason", null)}) {
            report(readerSession, postId, body)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.fieldErrors[0].field").value("reason"))
                    .andExpect(jsonPath("$.fieldErrors[0].code").value("REPORT_REASON_REQUIRED"))
                    .andExpect(jsonPath("$.fieldErrors[0].message").value("※ 신고 사유를 골라 주세요"));
        }
        assertThat(reports()).isZero();
    }

    @Test
    void 로그인하지_않으면_401_CSRF가_없으면_거절_남의_비공개_글은_404() throws Exception {
        mvc.perform(post("/api/posts/{id}/reports", postId).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(TestJson.of("reason", "SPAM")))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/posts/{id}/reports", postId).session(readerSession).contentType(MediaType.APPLICATION_JSON)
                        .content(TestJson.of("reason", "SPAM")))
                .andExpect(status().isForbidden());
        Long hidden = members.addPost(owner.defaultCategoryId(), "비공개", "private");
        String missing = report(readerSession, 99_999_999L, TestJson.of("reason", "SPAM"))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        String others = report(readerSession, hidden, TestJson.of("reason", "SPAM"))
                .andExpect(status().isNotFound()).andReturn().getResponse().getContentAsString();
        assertThat(others).isEqualTo(missing).contains("POST_NOT_FOUND");
        assertThat(jdbc.queryForObject("select count(*) from post_report where users_id = ?", Long.class, reader.id())).isZero();
    }

    @Test
    void FR020_신고해도_글은_그대로_보인다() throws Exception {
        report(readerSession, postId, TestJson.of("reason", "ADULT")).andExpect(status().isCreated());
        mvc.perform(get("/api/posts/{id}", postId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("신고할 글"));
    }

    @Test
    void S11_글을_지우면_그_글의_신고도_지워지고_신고가_있어도_삭제는_막히지_않는다() throws Exception {
        report(readerSession, postId, TestJson.of("reason", "SPAM")).andExpect(status().isCreated());
        mvc.perform(delete("/api/posts/{id}", postId).with(csrf()).session(ownerSession)).andExpect(status().isNoContent());
        assertThat(reports()).isZero();
    }

    @Test
    void D5_탈퇴해도_그_회원이_한_신고는_남는다() throws Exception {
        report(readerSession, postId, TestJson.of("reason", "SPAM")).andExpect(status().isCreated());
        mvc.perform(post("/api/account/withdrawal").with(csrf()).session(readerSession).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"" + TestMembers.PASSWORD + "\",\"agreed\":true}"))
                .andExpect(status().isOk());
        assertThat(reports()).isOne();
    }

    private ResultActions report(MockHttpSession session, Long target, String body) throws Exception {
        return mvc.perform(post("/api/posts/{id}/reports", target).with(csrf()).session(session)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }

    private List<Integer> together(List<RequestBuilder> requests) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(requests.size());
        try {
            List<Future<Integer>> futures = new ArrayList<>();
            for (RequestBuilder request : requests) {
                futures.add(pool.submit((Callable<Integer>) () -> {
                    start.await();
                    return mvc.perform(request).andReturn().getResponse().getStatus();
                }));
            }
            start.countDown();
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get());
            }
            return statuses;
        } finally {
            pool.shutdown();
        }
    }

    private long reports() {
        return jdbc.queryForObject("select count(*) from post_report where post_id = ?", Long.class, postId);
    }

    private Member register(String prefix) {
        Member member = members.register(prefix);
        created.add(member);
        return member;
    }
}
