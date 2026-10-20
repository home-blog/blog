package com.myblog.guestbook;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.myblog.support.TestClock;
import com.myblog.support.TestComments;
import com.myblog.support.TestJson;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import java.time.Duration;
import java.time.Instant;
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
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 방명록 (specs/021 US1 S1-1 ~ S1-6, US2 S2-1 ~ S2-3). 요청마다 커밋되는 것(소식은 커밋 뒤에 만든다)을 보므로
 * 클래스 전체 @Transactional을 쓰지 않고 끝에서 지운다. 30초 간격·하루 한도는 시계를 옮겨 확인한다.
 */
@SpringBootTest
@Import({TestMembers.class, TestComments.class, TestClock.Config.class})
class GuestbookTest {

    private static final long WAIT_MS = 5000;
    /** 한국 날짜로 낮 12시 (하루 한도의 날짜 경계를 피한다). 먼 미래라 다른 테스트의 엽서와 섞이지 않는다 */
    private static final Instant NOON = Instant.parse("2031-05-14T03:00:00Z");

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private TestComments testComments;

    @Autowired
    private TestClock clock;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private final List<Member> created = new ArrayList<>();
    private Member owner;
    private Member guest;
    private MockHttpSession ownerSession;
    private MockHttpSession guestSession;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        clock.set(NOON);
        owner = register("go");
        guest = register("gg");
        ownerSession = members.login(mvc, owner);
        guestSession = members.login(mvc, guest);
    }

    @AfterEach
    void cleanUp() {
        clock.reset();
        created.forEach(testComments::deleteMember);
    }

    private Member register(String prefix) {
        Member member = members.register(prefix);
        created.add(member);
        return member;
    }

    private ResultActions write(MockHttpSession session, Long blogId, String body, String stamp, Boolean secret)
            throws Exception {
        return mvc.perform(post("/api/v1/blogs/{id}/guestbook", blogId).with(csrf()).session(session)
                .contentType(MediaType.APPLICATION_JSON)
                .content(TestJson.of("body", body, "stamp", stamp, "secret", secret)));
    }

    private Long writeOk(MockHttpSession session, Long blogId, String body, boolean secret) throws Exception {
        String json = write(session, blogId, body, "pine", secret).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        clock.set(clock.instant().plus(Duration.ofMinutes(1)));
        return ((Number) JsonPath.read(json, "$.entryId")).longValue();
    }

    private ResultActions reply(MockHttpSession session, Long entryId, String body) throws Exception {
        return mvc.perform(put("/api/v1/guestbook/{id}/reply", entryId).with(csrf()).session(session)
                .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("body", body)));
    }

    private long entries(Long blogId) {
        return jdbc.queryForObject("select count(*) from guestbook_entry where blog_id = ?", Long.class, blogId);
    }

    private long notices(Member member, String kind) {
        return jdbc.queryForObject("select count(*) from notification where users_id = ? and kind = ?", Long.class,
                member.id(), kind);
    }

    private void awaitNotices(Member member, String kind, long expected) throws InterruptedException {
        long until = System.currentTimeMillis() + WAIT_MS;
        while (notices(member, kind) < expected && System.currentTimeMillis() < until) {
            Thread.sleep(50);
        }
        assertThat(notices(member, kind)).isEqualTo(expected);
    }

    // US1 엽서 남기기와 읽기

    @Test
    void S1_엽서를_남기면_최근_순으로_꽂히고_내용_우표_쓴_사람이_보인다() throws Exception {
        write(guestSession, owner.blogId(), "  들렀다 갑니다.\r\n사진이 좋아요  ", "moon", null)
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.entryId").isNumber())
                .andExpect(jsonPath("$.author.id").value(guest.id()))
                .andExpect(jsonPath("$.author.nickname").value(guest.user().getNickname()))
                .andExpect(jsonPath("$.body").value("들렀다 갑니다.\n사진이 좋아요"))
                .andExpect(jsonPath("$.stamp").value("moon"))
                .andExpect(jsonPath("$.secret").value(false))
                .andExpect(jsonPath("$.sealed").value(false))
                .andExpect(jsonPath("$.reply").doesNotExist())
                .andExpect(jsonPath("$.canDelete").value(true))
                .andExpect(jsonPath("$.canReply").value(false));
        clock.set(NOON.plus(Duration.ofMinutes(5)));
        Member other = register("gt");
        write(members.login(mvc, other), owner.blogId(), "두 번째 엽서", "crane", false).andExpect(status().isCreated());

        mvc.perform(get("/api/v1/blogs/{id}/guestbook", owner.blogId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.totalPages").value(1))
                .andExpect(jsonPath("$.pageSize").value(12))
                .andExpect(jsonPath("$.canWrite").value(false))
                .andExpect(jsonPath("$.bodyMaxLength").value(300))
                .andExpect(jsonPath("$.stamps.length()").value(5))
                .andExpect(jsonPath("$.items[0].body").value("두 번째 엽서"))
                .andExpect(jsonPath("$.items[1].body").value("들렀다 갑니다.\n사진이 좋아요"))
                .andExpect(jsonPath("$.items[0].canDelete").value(false));
        // 주인은 쓰지 못하고 모두 지우고 답할 수 있다, 손님은 쓸 수 있다
        mvc.perform(get("/api/v1/blogs/{id}/guestbook", owner.blogId()).session(ownerSession))
                .andExpect(jsonPath("$.canWrite").value(false))
                .andExpect(jsonPath("$.items[0].canDelete").value(true))
                .andExpect(jsonPath("$.items[0].canReply").value(true));
        mvc.perform(get("/api/v1/blogs/{id}/guestbook", owner.blogId()).session(guestSession))
                .andExpect(jsonPath("$.canWrite").value(true));
    }

    @Test
    void S1_5_주인에게_새_엽서_소식이_간다() throws Exception {
        writeOk(guestSession, owner.blogId(), "소식 가나요", true);
        awaitNotices(owner, "guestbook", 1);
        Long blogId = jdbc.queryForObject("select blog_id from notification where users_id = ? and kind = 'guestbook'",
                Long.class, owner.id());
        assertThat(blogId).isEqualTo(owner.blogId());
        mvc.perform(get("/api/v1/me/notifications").session(ownerSession))
                .andExpect(jsonPath("$.items[0].kind").value("guestbook"))
                .andExpect(jsonPath("$.items[0].blogId").value(owner.blogId()))
                .andExpect(jsonPath("$.items[0].actorName").value(guest.user().getNickname()))
                .andExpect(jsonPath("$.items[0].available").value(true));
    }

    @Test
    void S1_1_없는_블로그는_404이고_로그인하지_않으면_쓰기는_401() throws Exception {
        mvc.perform(get("/api/v1/blogs/{id}/guestbook", 999_999_999L))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BLOG_NOT_FOUND"));
        write(guestSession, 999_999_999L, "어디로", "pine", false)
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("BLOG_NOT_FOUND"));
        mvc.perform(post("/api/v1/blogs/{id}/guestbook", owner.blogId()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(TestJson.of("body", "몰래", "stamp", "pine")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));
        mvc.perform(get("/api/v1/blogs/{id}/guestbook", owner.blogId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.canWrite").value(false))
                .andExpect(jsonPath("$.totalCount").value(0));
        assertThat(entries(owner.blogId())).isZero();
    }

    @Test
    void S1_2_내용은_1자에서_300자까지이고_우표는_다섯_가지만() throws Exception {
        write(guestSession, owner.blogId(), "  \n ", "pine", false)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GUESTBOOK_BODY_EMPTY"));
        write(guestSession, owner.blogId(), "가".repeat(301), "pine", false)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GUESTBOOK_BODY_TOO_LONG"))
                .andExpect(jsonPath("$.message").value("※ 엽서는 300자 이하로 적어 주세요"));
        write(guestSession, owner.blogId(), "우표 없이", null, false)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GUESTBOOK_STAMP_INVALID"));
        write(guestSession, owner.blogId(), "이상한 우표", "rose", false)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GUESTBOOK_STAMP_INVALID"));
        assertThat(entries(owner.blogId())).isZero();
        // 경계: 300자(이모지는 한 글자로 센다)
        write(guestSession, owner.blogId(), "😀".repeat(300), "plum", false).andExpect(status().isCreated());
        assertThat(entries(owner.blogId())).isEqualTo(1);
    }

    @Test
    void S1_3_주인은_자기_방명록에_쓰지_못한다() throws Exception {
        write(ownerSession, owner.blogId(), "내 방명록", "pine", false)
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("GUESTBOOK_OWN_BLOG"));
        assertThat(entries(owner.blogId())).isZero();
    }

    @Test
    void S1_4_30초_안에_다시_쓰면_429이고_지나면_된다() throws Exception {
        Member second = register("gs");
        write(guestSession, owner.blogId(), "첫 엽서", "pine", false).andExpect(status().isCreated());
        clock.set(NOON.plus(Duration.ofSeconds(29)));
        // 다른 블로그여도 간격은 회원마다 센다
        write(guestSession, second.blogId(), "다른 블로그", "wave", false)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("GUESTBOOK_TOO_FREQUENT"));
        clock.set(NOON.plus(Duration.ofSeconds(30)));
        write(guestSession, second.blogId(), "다른 블로그", "wave", false).andExpect(status().isCreated());
    }

    @Test
    void S1_4_한_블로그에_하루_3장까지이고_다음_날은_다시_된다() throws Exception {
        for (int i = 0; i < 3; i++) {
            writeOk(guestSession, owner.blogId(), "엽서 " + i, false);
        }
        write(guestSession, owner.blogId(), "네 번째", "pine", false)
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("GUESTBOOK_DAILY_LIMIT"));
        // 다른 블로그는 따로 센다
        Member second = register("gs");
        write(guestSession, second.blogId(), "다른 블로그", "pine", false).andExpect(status().isCreated());
        // 한국 날짜로 다음 날 0시
        clock.set(Instant.parse("2031-05-14T15:00:00Z"));
        write(guestSession, owner.blogId(), "다음 날", "pine", false).andExpect(status().isCreated());
        assertThat(entries(owner.blogId())).isEqualTo(4);
    }

    @Test
    void S1_4_거의_동시에_온_요청도_한_장만_꽂힌다() throws Exception {
        int threads = 6;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Integer>> results = new ArrayList<>();
        for (int i = 0; i < threads; i++) {
            int n = i;
            Callable<Integer> task = () -> {
                start.await();
                return write(guestSession, owner.blogId(), "동시 " + n, "pine", false).andReturn().getResponse()
                        .getStatus();
            };
            results.add(pool.submit(task));
        }
        start.countDown();
        int createdCount = 0;
        for (Future<Integer> result : results) {
            int code = result.get();
            assertThat(code).isIn(201, 429);
            if (code == 201) {
                createdCount++;
            }
        }
        pool.shutdown();
        assertThat(createdCount).isEqualTo(1);
        assertThat(entries(owner.blogId())).isEqualTo(1);
    }

    @Test
    void S1_5_비밀_엽서는_쓴_사람과_주인만_읽고_남에게는_봉한_엽서다() throws Exception {
        writeOk(guestSession, owner.blogId(), "주인만 보세요", true);
        Member stranger = register("gx");
        MockHttpSession strangerSession = members.login(mvc, stranger);

        for (MockHttpSession session : new MockHttpSession[] {null, strangerSession}) {
            var request = get("/api/v1/blogs/{id}/guestbook", owner.blogId());
            mvc.perform(session == null ? request : request.session(session))
                    .andExpect(jsonPath("$.items[0].secret").value(true))
                    .andExpect(jsonPath("$.items[0].sealed").value(true))
                    .andExpect(jsonPath("$.items[0].body").value(""))
                    .andExpect(jsonPath("$.items[0].author.id").doesNotExist())
                    .andExpect(jsonPath("$.items[0].author.nickname").doesNotExist())
                    .andExpect(jsonPath("$.items[0].stamp").value("pine"))
                    .andExpect(jsonPath("$.items[0].canDelete").value(false));
        }
        for (MockHttpSession session : new MockHttpSession[] {guestSession, ownerSession}) {
            mvc.perform(get("/api/v1/blogs/{id}/guestbook", owner.blogId()).session(session))
                    .andExpect(jsonPath("$.items[0].sealed").value(false))
                    .andExpect(jsonPath("$.items[0].body").value("주인만 보세요"))
                    .andExpect(jsonPath("$.items[0].author.id").value(guest.id()));
        }
        // 비밀 엽서의 답장도 남에게는 보이지 않는다
        Long entryId = jdbc.queryForObject("select guestbook_entry_id from guestbook_entry where blog_id = ?",
                Long.class, owner.blogId());
        reply(ownerSession, entryId, "고마워요").andExpect(status().isOk());
        mvc.perform(get("/api/v1/blogs/{id}/guestbook", owner.blogId()).session(strangerSession))
                .andExpect(jsonPath("$.items[0].reply").doesNotExist())
                .andExpect(jsonPath("$.items[0].repliedAt").doesNotExist());
        mvc.perform(get("/api/v1/blogs/{id}/guestbook", owner.blogId()).session(guestSession))
                .andExpect(jsonPath("$.items[0].reply").value("고마워요"));
    }

    @Test
    void S1_6_쓴_사람이_탈퇴하면_탈퇴한_회원으로_보인다() throws Exception {
        writeOk(guestSession, owner.blogId(), "곧 떠나요", false);
        jdbc.update("update users set deleted_at = now() where users_id = ?", guest.id());
        mvc.perform(get("/api/v1/blogs/{id}/guestbook", owner.blogId()))
                .andExpect(jsonPath("$.items[0].author.withdrawn").value(true))
                .andExpect(jsonPath("$.items[0].author.id").doesNotExist())
                .andExpect(jsonPath("$.items[0].body").value("곧 떠나요"));
    }

    @Test
    void 쪽이_넘으면_다음_쪽에_꽂힌다() throws Exception {
        for (int i = 0; i < 13; i++) {
            jdbc.update("insert into guestbook_entry (blog_id, users_id, body, stamp, created_at)"
                    + " values (?, ?, ?, 'wave', now() - make_interval(mins => ?))", owner.blogId(), guest.id(),
                    "엽서 " + i, i);
        }
        mvc.perform(get("/api/v1/blogs/{id}/guestbook", owner.blogId()).param("page", "2"))
                .andExpect(jsonPath("$.totalCount").value(13))
                .andExpect(jsonPath("$.totalPages").value(2))
                .andExpect(jsonPath("$.page").value(2))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].body").value("엽서 12"));
        mvc.perform(get("/api/v1/blogs/{id}/guestbook", owner.blogId()).param("page", "abc"))
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.items[0].body").value("엽서 0"));
    }

    // US2 답장과 정리

    @Test
    void S2_주인이_답장하면_쓴_사람에게_소식이_한_번_가고_고쳐도_다시_가지_않는다() throws Exception {
        Long entryId = writeOk(guestSession, owner.blogId(), "안녕하세요", false);
        reply(ownerSession, entryId, "  반가워요\r\n또 오세요 ")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("반가워요\n또 오세요"))
                .andExpect(jsonPath("$.repliedAt").isString());
        awaitNotices(guest, "gb_reply", 1);
        reply(ownerSession, entryId, "고친 답장").andExpect(status().isOk())
                .andExpect(jsonPath("$.reply").value("고친 답장"));
        Thread.sleep(300);
        assertThat(notices(guest, "gb_reply")).isEqualTo(1);
        mvc.perform(get("/api/v1/blogs/{id}/guestbook", owner.blogId()))
                .andExpect(jsonPath("$.items[0].reply").value("고친 답장"));

        mvc.perform(delete("/api/v1/guestbook/{id}/reply", entryId).with(csrf()).session(ownerSession))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/blogs/{id}/guestbook", owner.blogId()))
                .andExpect(jsonPath("$.items[0].reply").doesNotExist())
                .andExpect(jsonPath("$.items[0].repliedAt").doesNotExist());

        // 지웠다 다시 써도 소식은 엽서마다 한 번만 (우편함을 채우지 않는다)
        reply(ownerSession, entryId, "다시 쓴 답장").andExpect(status().isOk());
        Thread.sleep(300);
        assertThat(notices(guest, "gb_reply")).isEqualTo(1);
    }

    @Test
    void S2_1_주인이_아니면_답장도_답장_지우기도_404() throws Exception {
        Long entryId = writeOk(guestSession, owner.blogId(), "안녕하세요", false);
        reply(guestSession, entryId, "내가 답장")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GUESTBOOK_ENTRY_NOT_FOUND"));
        mvc.perform(delete("/api/v1/guestbook/{id}/reply", entryId).with(csrf()).session(guestSession))
                .andExpect(status().isNotFound());
        reply(ownerSession, entryId, " ").andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GUESTBOOK_BODY_EMPTY"));
        reply(ownerSession, entryId, "가".repeat(301)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("GUESTBOOK_BODY_TOO_LONG"));
        reply(ownerSession, entryId, "가".repeat(300)).andExpect(status().isOk());
    }

    @Test
    void S2_1_지우기는_쓴_사람과_주인만_하고_남에게는_404() throws Exception {
        Long mine = writeOk(guestSession, owner.blogId(), "내가 지울 엽서", false);
        Long byOwner = writeOk(guestSession, owner.blogId(), "주인이 지울 엽서", false);
        Member stranger = register("gx");
        mvc.perform(delete("/api/v1/guestbook/{id}", mine).with(csrf()).session(members.login(mvc, stranger)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GUESTBOOK_ENTRY_NOT_FOUND"));
        mvc.perform(delete("/api/v1/guestbook/{id}", mine).with(csrf()))
                .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/v1/guestbook/{id}", mine).with(csrf()).session(guestSession))
                .andExpect(status().isNoContent());
        reply(ownerSession, byOwner, "답장").andExpect(status().isOk());
        mvc.perform(delete("/api/v1/guestbook/{id}", byOwner).with(csrf()).session(ownerSession))
                .andExpect(status().isNoContent());
        assertThat(entries(owner.blogId())).isZero();
        // 엽서를 지우면 그 엽서의 소식도 지워진다
        awaitNotices(owner, "guestbook", 0);
        assertThat(notices(guest, "gb_reply")).isZero();
    }

    @Test
    void S2_3_지운_엽서에_답장하면_404() throws Exception {
        Long entryId = writeOk(guestSession, owner.blogId(), "곧 지워요", false);
        mvc.perform(delete("/api/v1/guestbook/{id}", entryId).with(csrf()).session(guestSession))
                .andExpect(status().isNoContent());
        reply(ownerSession, entryId, "늦은 답장")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("GUESTBOOK_ENTRY_NOT_FOUND"));
        mvc.perform(delete("/api/v1/guestbook/{id}", entryId).with(csrf()).session(guestSession))
                .andExpect(status().isNotFound());
    }
}
