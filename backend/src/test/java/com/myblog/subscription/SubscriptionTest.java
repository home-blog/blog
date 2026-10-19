package com.myblog.subscription;

import static org.assertj.core.api.Assertions.assertThat;
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
import com.myblog.support.TestPosts;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * 원고 받아 보기 (specs/016 US1, S1-1 ~ S1-5). 한도는 시험하기 쉽게 2개로 줄인다.
 */
@SpringBootTest
@Transactional
@Import({TestMembers.class, TestPosts.class})
@TestPropertySource(properties = "subscription.max-per-member=2")
class SubscriptionTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private TestPosts posts;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private Member writer;
    private Member reader;
    private MockHttpSession writerSession;
    private MockHttpSession readerSession;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        writer = members.register("subw");
        reader = members.register("subr");
        writerSession = members.login(mvc, writer);
        readerSession = members.login(mvc, reader);
    }

    private String url(Member blogOwner) {
        return "/api/v1/blogs/" + blogOwner.blogId() + "/subscription";
    }

    @Test
    void 받아_보기를_켜고_끄면_블로그_머리의_수와_내_표시가_바뀐다() throws Exception {
        mvc.perform(put(url(writer)).with(csrf()).session(readerSession)).andExpect(status().isOk())
                .andExpect(jsonPath("$.subscribed").value(true))
                .andExpect(jsonPath("$.emailEnabled").value(false))
                .andExpect(jsonPath("$.subscriberCount").value(1));
        mvc.perform(get("/api/v1/blogs/{id}", writer.blogId()).session(readerSession))
                .andExpect(jsonPath("$.subscriberCount").value(1))
                .andExpect(jsonPath("$.subscribedByMe").value(true));
        mvc.perform(get("/api/v1/blogs/{id}", writer.blogId()))
                .andExpect(jsonPath("$.subscriberCount").value(1))
                .andExpect(jsonPath("$.subscribedByMe").value(false));

        mvc.perform(delete(url(writer)).with(csrf()).session(readerSession)).andExpect(status().isOk())
                .andExpect(jsonPath("$.subscribed").value(false))
                .andExpect(jsonPath("$.subscriberCount").value(0));
    }

    @Test
    void 메일도_받기를_고르고_다시_보내면_바꾼다() throws Exception {
        mvc.perform(put(url(writer)).with(csrf()).session(readerSession).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emailEnabled\":true}"))
                .andExpect(jsonPath("$.emailEnabled").value(true));
        mvc.perform(put(url(writer)).with(csrf()).session(readerSession).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"emailEnabled\":false}"))
                .andExpect(jsonPath("$.emailEnabled").value(false))
                .andExpect(jsonPath("$.subscriberCount").value(1));
    }

    @Test
    void S1_1_로그인하지_않으면_401_없는_블로그는_404_내_블로그는_400() throws Exception {
        mvc.perform(put(url(writer)).with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(put("/api/v1/blogs/999999999/subscription").with(csrf()).session(readerSession))
                .andExpect(status().isNotFound());
        mvc.perform(put(url(writer)).with(csrf()).session(writerSession)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("SELF_SUBSCRIBE_NOT_ALLOWED"));
    }

    @Test
    void S1_2_여러_번_보내도_한_줄이고_받지_않는_채_그만둬도_200() throws Exception {
        for (int i = 0; i < 3; i++) {
            mvc.perform(put(url(writer)).with(csrf()).session(readerSession)).andExpect(status().isOk());
        }
        assertThat(jdbc.queryForObject("select count(*) from blog_subscription where users_id = ?", Long.class,
                reader.id())).isOne();
        mvc.perform(delete(url(writer)).with(csrf()).session(readerSession)).andExpect(status().isOk());
        mvc.perform(delete(url(writer)).with(csrf()).session(readerSession)).andExpect(status().isOk())
                .andExpect(jsonPath("$.subscribed").value(false));
    }

    @Test
    void S1_3_받아_볼_수_있는_블로그는_정한_수까지() throws Exception {
        Member second = members.register("subs");
        Member third = members.register("subt");
        mvc.perform(put(url(writer)).with(csrf()).session(readerSession)).andExpect(status().isOk());
        mvc.perform(put(url(second)).with(csrf()).session(readerSession)).andExpect(status().isOk());
        mvc.perform(put(url(third)).with(csrf()).session(readerSession)).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("SUBSCRIPTION_LIMIT_EXCEEDED"));
        // 이미 받아 보는 블로그를 다시 보내는 것은 한도와 상관없다
        mvc.perform(put(url(writer)).with(csrf()).session(readerSession)).andExpect(status().isOk());
    }

    @Test
    void S1_4_받아_보는_글에는_받아_보는_블로그의_공개_글만_최신순으로() throws Exception {
        Member other = members.register("subo");
        Instant now = Instant.now();
        Long older = posts.add(writer.defaultCategoryId(), "먼저 쓴 글", "본문", "public", now.minus(2, ChronoUnit.HOURS));
        Long newer = posts.add(writer.defaultCategoryId(), "나중 쓴 글", "본문", "public", now.minus(1, ChronoUnit.HOURS));
        posts.add(writer.defaultCategoryId(), "비공개 글", "본문", "private", now);
        Long hiddenCategory = members.addCategory(writer.blogId(), "숨긴 분류", "private", 9);
        posts.add(hiddenCategory, "비공개 분류의 글", "본문", "public", now);
        posts.add(other.defaultCategoryId(), "받아 보지 않는 블로그의 글", "본문", "public", now);

        mvc.perform(get("/api/v1/me/subscriptions/feed").session(readerSession)).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(0));

        mvc.perform(put(url(writer)).with(csrf()).session(readerSession));
        mvc.perform(get("/api/v1/me/subscriptions/feed").session(readerSession))
                .andExpect(jsonPath("$.totalCount").value(2))
                .andExpect(jsonPath("$.posts[0].postId").value(newer))
                .andExpect(jsonPath("$.posts[1].postId").value(older))
                .andExpect(jsonPath("$.posts[0].blogName").isNotEmpty());
        mvc.perform(get("/api/v1/me/subscriptions/feed")).andExpect(status().isUnauthorized());
    }

    @Test
    void 받아_보는_블로그_목록() throws Exception {
        Member second = members.register("subs");
        mvc.perform(put(url(writer)).with(csrf()).session(readerSession));
        mvc.perform(put(url(second)).with(csrf()).session(readerSession).contentType(MediaType.APPLICATION_JSON)
                .content("{\"emailEnabled\":true}"));

        mvc.perform(get("/api/v1/me/subscriptions").session(readerSession)).andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(2))
                .andExpect(jsonPath("$.items[0].blogId").value(second.blogId()))
                .andExpect(jsonPath("$.items[0].emailEnabled").value(true))
                .andExpect(jsonPath("$.items[0].blogName").isNotEmpty())
                .andExpect(jsonPath("$.items[0].nickname").isNotEmpty());
    }

    @Test
    void S1_5_탈퇴하면_받아_보던_것과_내_블로그를_받아_보던_줄을_지운다() throws Exception {
        Member fan = members.register("subf");
        MockHttpSession fanSession = members.login(mvc, fan);
        mvc.perform(put(url(writer)).with(csrf()).session(readerSession));
        mvc.perform(put(url(reader)).with(csrf()).session(fanSession));

        mvc.perform(post("/api/v1/account/withdrawal").with(csrf()).session(readerSession)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"password\":\"" + TestMembers.PASSWORD + "\",\"agreed\":true}"))
                .andExpect(status().isOk());

        assertThat(jdbc.queryForObject("select count(*) from blog_subscription where users_id = ? or blog_id = ?",
                Long.class, reader.id(), reader.blogId())).isZero();
    }
}
