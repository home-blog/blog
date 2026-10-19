package com.myblog.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.myblog.subscription.mail.NewsletterMailer;
import com.myblog.subscription.service.NotificationJanitor;
import com.myblog.support.TestClock;
import com.myblog.support.TestComments;
import com.myblog.support.TestJson;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 우편함과 원고 편지 (specs/016 US2 S2-1 ~ S2-5, US3 S3-1 ~ S3-4). 소식은 글·댓글이 커밋된 뒤 따로 만들어지므로
 * 클래스 전체 @Transactional을 쓰지 않고(커밋되어야 한다) 끝에서 지운다. 메일 한도의 날짜 키가 다른 실행과 섞이지 않게
 * 시계를 먼 미래의 서로 다른 날로 옮긴다.
 */
@SpringBootTest
@Import({TestMembers.class, TestComments.class, TestClock.Config.class})
@TestPropertySource(properties = {"subscription.newsletter.site-url=https://example.test/",
        "subscription.newsletter.daily-limit=2"})
class NotificationTest {

    private static final long WAIT_MS = 5000;

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

    @Autowired
    private NotificationJanitor janitor;

    @MockitoBean
    private NewsletterMailer mailer;

    private MockMvc mvc;
    private final List<Member> created = new ArrayList<>();
    private Member writer;
    private Member reader;
    private MockHttpSession writerSession;
    private MockHttpSession readerSession;
    private Long topicId;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        clock.set(LocalDate.of(2100, 1, 1).plusDays(ThreadLocalRandom.current().nextInt(1, 300_000))
                .atTime(3, 0).toInstant(ZoneOffset.UTC));
        writer = register("nw");
        reader = register("nr");
        writerSession = members.login(mvc, writer);
        readerSession = members.login(mvc, reader);
        topicId = jdbc.queryForObject("select min(topic_id) from topic", Long.class);
    }

    @AfterEach
    void cleanUp() {
        clock.reset();
        for (Member member : created) {
            jdbc.update("delete from notification where users_id = ?", member.id());
            jdbc.update("delete from blog_subscription where users_id = ? or blog_id = ?", member.id(), member.blogId());
            testComments.deleteMember(member);
        }
    }

    private Member register(String prefix) {
        Member member = members.register(prefix);
        created.add(member);
        return member;
    }

    private void subscribe(MockHttpSession session, Member blogOwner, boolean email) throws Exception {
        mvc.perform(put("/api/v1/blogs/{id}/subscription", blogOwner.blogId()).with(csrf()).session(session)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"emailEnabled\":" + email + "}"))
                .andExpect(status().isOk());
    }

    private Long write(Long categoryId, String title, String visibility) throws Exception {
        String body = mvc.perform(post("/api/v1/posts").with(csrf()).session(writerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestJson.of("title", title, "content", "## 첫 문단\n\n**바다**를 보며 [걸었다](https://x.test).",
                                "categoryId", categoryId, "topicId", topicId, "visibility", visibility)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        return ((Number) JsonPath.read(body, "$.postId")).longValue();
    }

    private void edit(Long postId, String title, String visibility) throws Exception {
        mvc.perform(put("/api/v1/posts/{id}", postId).with(csrf()).session(writerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(TestJson.of("title", title, "content", "본문", "categoryId", writer.defaultCategoryId(),
                                "topicId", topicId, "visibility", visibility)))
                .andExpect(status().isOk());
    }

    private long notices(Member member, String kind) {
        return jdbc.queryForObject("select count(*) from notification where users_id = ? and kind = ?", Long.class,
                member.id(), kind);
    }

    /** 일꾼이 소식을 만들 때까지 기다린다. */
    private void awaitNotices(Member member, String kind, long expected) throws InterruptedException {
        long until = System.currentTimeMillis() + WAIT_MS;
        while (notices(member, kind) < expected && System.currentTimeMillis() < until) {
            Thread.sleep(50);
        }
        assertThat(notices(member, kind)).isEqualTo(expected);
    }

    /** 일꾼에 남은 일이 없을 때까지 잠깐 기다린다 (만들지 않아야 하는 소식을 확인할 때). */
    private void settle() throws Exception {
        Member marker = register("nm");
        MockHttpSession markerSession = members.login(mvc, marker);
        subscribe(markerSession, writer, false);
        Long probe = write(writer.defaultCategoryId(), "일꾼 확인용", "public");
        awaitNotices(marker, "post", 1);
        jdbc.update("delete from notification where post_id = ?", probe);
    }

    @Test
    void 받아_보는_블로그에_새_공개_글이_오르면_우편함에_소식이_온다() throws Exception {
        Member stranger = register("ns");
        subscribe(readerSession, writer, false);
        Long postId = write(writer.defaultCategoryId(), "바다 일기", "public");

        awaitNotices(reader, "post", 1);
        mvc.perform(get("/api/v1/me/notifications").session(readerSession)).andExpect(status().isOk())
                .andExpect(jsonPath("$.unreadCount").value(1))
                .andExpect(jsonPath("$.items[0].kind").value("post"))
                .andExpect(jsonPath("$.items[0].postId").value(postId))
                .andExpect(jsonPath("$.items[0].postTitle").value("바다 일기"))
                .andExpect(jsonPath("$.items[0].actorName").isNotEmpty())
                .andExpect(jsonPath("$.items[0].available").value(true))
                .andExpect(jsonPath("$.items[0].read").value(false));
        assertThat(notices(writer, "post")).isZero();
        assertThat(notices(stranger, "post")).isZero();
    }

    @Test
    void S2_2_비공개로_올린_글은_처음_공개될_때만_한_번_알린다() throws Exception {
        subscribe(readerSession, writer, false);
        Long hiddenCategory = members.addCategory(writer.blogId(), "숨긴 분류", "private", 9);
        write(hiddenCategory, "비공개 분류의 글", "public");
        Long postId = write(writer.defaultCategoryId(), "아직 비공개", "private");
        settle();
        assertThat(notices(reader, "post")).isZero();

        edit(postId, "이제 공개", "public");
        awaitNotices(reader, "post", 1);
        edit(postId, "다시 비공개", "private");
        edit(postId, "또 공개", "public");
        settle();
        assertThat(notices(reader, "post")).as("같은 글로 두 번 알리지 않는다").isOne();
    }

    @Test
    void 내_글에_남이_댓글을_달면_소식이_오고_내가_단_댓글은_오지_않는다() throws Exception {
        Long postId = write(writer.defaultCategoryId(), "댓글 받을 글", "public");
        mvc.perform(post("/api/v1/posts/{id}/comments", postId).with(csrf()).session(readerSession)
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"좋은 글이네요\"}")).andExpect(status().isCreated());
        awaitNotices(writer, "comment", 1);
        mvc.perform(get("/api/v1/me/notifications").session(writerSession))
                .andExpect(jsonPath("$.items[0].kind").value("comment"))
                .andExpect(jsonPath("$.items[0].postTitle").value("댓글 받을 글"))
                .andExpect(jsonPath("$.items[0].actorName").value(reader.user().getNickname()));

        clock.set(clock.instant().plusSeconds(60)); // 댓글 간격
        mvc.perform(post("/api/v1/posts/{id}/comments", postId).with(csrf()).session(writerSession)
                .contentType(MediaType.APPLICATION_JSON).content("{\"body\":\"고맙습니다\"}")).andExpect(status().isCreated());
        settle();
        assertThat(notices(writer, "comment")).isOne();
    }

    @Test
    void S2_1_읽음과_모두_읽음은_내_것만() throws Exception {
        subscribe(readerSession, writer, false);
        write(writer.defaultCategoryId(), "첫째", "public");
        write(writer.defaultCategoryId(), "둘째", "public");
        awaitNotices(reader, "post", 2);
        String body = mvc.perform(get("/api/v1/me/notifications").session(readerSession))
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        long first = ((Number) JsonPath.read(body, "$.items[0].notificationId")).longValue();

        mvc.perform(post("/api/v1/me/notifications/{id}/read", first).with(csrf()).session(writerSession))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("NOTIFICATION_NOT_FOUND"));
        mvc.perform(post("/api/v1/me/notifications/{id}/read", first).with(csrf()).session(readerSession))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/me/notifications/unread-count").session(readerSession))
                .andExpect(jsonPath("$.unreadCount").value(1));
        mvc.perform(post("/api/v1/me/notifications/read-all").with(csrf()).session(readerSession))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/me/notifications/unread-count").session(readerSession))
                .andExpect(jsonPath("$.unreadCount").value(0));
        mvc.perform(get("/api/v1/me/notifications/unread-count")).andExpect(status().isUnauthorized());
    }

    @Test
    void S2_4_지워진_글의_소식은_볼_수_없는_글로_남는다() throws Exception {
        subscribe(readerSession, writer, false);
        Long postId = write(writer.defaultCategoryId(), "곧 지울 글", "public");
        awaitNotices(reader, "post", 1);
        mvc.perform(delete("/api/v1/posts/{id}", postId).with(csrf()).session(writerSession))
                .andExpect(status().is2xxSuccessful());

        mvc.perform(get("/api/v1/me/notifications").session(readerSession))
                .andExpect(jsonPath("$.items[0].postTitle").value("곧 지울 글"))
                .andExpect(jsonPath("$.items[0].available").value(false));
    }

    @Test
    void 보관_기간이_지난_소식은_정리_작업이_지운다() throws Exception {
        jdbc.update("insert into notification (users_id, kind, post_id, post_title, actor_name, created_at)"
                + " values (?, 'post', null, '옛 소식', '블로그', ?)", reader.id(),
                java.sql.Timestamp.from(clock.instant().minusSeconds(31L * 24 * 3600)));
        jdbc.update("insert into notification (users_id, kind, post_id, post_title, actor_name, created_at)"
                + " values (?, 'post', null, '새 소식', '블로그', ?)", reader.id(),
                java.sql.Timestamp.from(clock.instant().minusSeconds(29L * 24 * 3600)));

        janitor.clean();

        assertThat(jdbc.queryForList("select post_title from notification where users_id = ?", String.class, reader.id()))
                .containsExactly("새 소식");
    }

    @Test
    void 원고_편지는_메일을_켠_사람에게만_가고_그만_받기_링크로_끈다() throws Exception {
        Member quiet = register("nq");
        MockHttpSession quietSession = members.login(mvc, quiet);
        subscribe(readerSession, writer, true);
        subscribe(quietSession, writer, false);
        Long postId = write(writer.defaultCategoryId(), "편지로 갈 글", "public");

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(mailer, timeout(WAIT_MS)).send(eq(reader.user().getEmail()), anyString(), body.capture());
        verify(mailer, never()).send(eq(quiet.user().getEmail()), anyString(), anyString());
        assertThat(body.getValue()).contains("편지로 갈 글", "바다를 보며 걸었다.", "https://example.test/posts/" + postId)
                .doesNotContain("**", "https://x.test");

        Matcher token = Pattern.compile("token=(\\S+)").matcher(body.getValue());
        assertThat(token.find()).isTrue();
        mvc.perform(post("/api/v1/newsletter/unsubscribe").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token.group(1) + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.blogName").isNotEmpty());
        assertThat(jdbc.queryForObject("select email_enabled from blog_subscription where users_id = ?", Boolean.class,
                reader.id())).isFalse();

        mvc.perform(post("/api/v1/newsletter/unsubscribe").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"" + token.group(1).substring(0, token.group(1).length() - 2) + "xx\"}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("NEWSLETTER_TOKEN_INVALID"));
    }

    @Test
    void S3_2_하루_한도를_넘으면_그날은_더_보내지_않고_우편함_소식은_간다() throws Exception {
        Member second = register("n2");
        Member third = register("n3");
        subscribe(readerSession, writer, true);
        subscribe(members.login(mvc, second), writer, true);
        subscribe(members.login(mvc, third), writer, true);

        write(writer.defaultCategoryId(), "한도 시험", "public");

        awaitNotices(third, "post", 1);
        settle();
        verify(mailer, timeout(WAIT_MS).times(2)).send(anyString(), anyString(), anyString());
        assertThat(org.mockito.Mockito.mockingDetails(mailer).getInvocations()).hasSize(2);
    }

    @Test
    void S3_4_탈퇴하면_우편함과_받아_보기를_지운다() throws Exception {
        subscribe(readerSession, writer, true);
        write(writer.defaultCategoryId(), "탈퇴 전 소식", "public");
        awaitNotices(reader, "post", 1);

        mvc.perform(post("/api/v1/account/withdrawal").with(csrf()).session(readerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"" + TestMembers.PASSWORD + "\",\"agreed\":true}"))
                .andExpect(status().isOk());

        assertThat(notices(reader, "post")).isZero();
        assertThat(jdbc.queryForObject("select count(*) from blog_subscription where users_id = ?", Long.class,
                reader.id())).isZero();
    }
}
