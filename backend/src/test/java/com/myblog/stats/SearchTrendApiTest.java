package com.myblog.stats;

import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myblog.support.TestClock;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import jakarta.servlet.http.Cookie;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * 실시간 인기 검색어 (specs/015 US1, S1-1 ~ S1-6). 검색 칸(10분)은 Redis에 있으므로, 테스트마다 시계를 먼 미래의
 * 서로 다른 시각으로 옮겨 다른 실행의 검색과 섞이지 않게 한다. 방문자는 쿠키(MYBLOG_VISITOR)로 가린다.
 */
@SpringBootTest
@Transactional
@Import({TestMembers.class, TestClock.Config.class})
@TestPropertySource(properties = "stats.search-trend.banned=금지말")
class SearchTrendApiTest {

    private static final String TRENDING = "/api/v1/search/trending";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private TestClock clock;

    @Autowired
    private JdbcTemplate jdbc;

    @MockitoSpyBean
    private StringRedisTemplate redis;

    private MockMvc mvc;
    private String word;
    private Instant start;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        // 2040년대의 아무 10분 칸: 다른 실행과 겹치지 않는다
        start = Instant.parse("2040-01-01T00:00:00Z")
                .plus(Duration.ofMinutes(10L * ThreadLocalRandom.current().nextInt(1, 500_000)));
        clock.set(start);
        word = "검색" + UUID.randomUUID().toString().substring(0, 6);
        Member writer = members.register("trend");
        Long topicId = jdbc.queryForObject("select min(topic_id) from topic", Long.class);
        for (String suffix : new String[] {"가", "나", "다", "boot"}) {
            jdbc.update("""
                    insert into post (category_id, topic_id, title, content, visibility, created_at)
                    values (?, ?, ?, '본문', 'public', now())
                    """, writer.defaultCategoryId(), topicId, word + suffix + " Spring Boot 금지말");
        }
    }

    @AfterEach
    void tearDown() {
        clock.reset();
    }

    /** visitor: 같은 사람이면 같은 값. */
    private ResultActions search(String q, String visitor) throws Exception {
        return mvc.perform(get("/api/v1/search/posts").param("q", q).cookie(new Cookie("MYBLOG_VISITOR", visitor)));
    }

    private static String visitor() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    @Test
    void S1_1_결과가_있는_검색만_센다() throws Exception {
        search(word + "가", visitor()).andExpect(status().isOk());
        search(word + "없는말", visitor()).andExpect(status().isOk());

        mvc.perform(get(TRENDING)).andExpect(status().isOk())
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].rank").value(1))
                .andExpect(jsonPath("$.items[0].keyword").value(word + "가"));
    }

    @Test
    void S1_2_같은_사람이_10분_안에_다시_찾으면_한_번만_센다() throws Exception {
        String same = visitor();
        for (int i = 0; i < 3; i++) {
            search(word + "가", same); // 한 사람이 세 번: 1
        }
        search(word + "나", visitor());
        search(word + "나", visitor()); // 두 사람: 2

        mvc.perform(get(TRENDING))
                .andExpect(jsonPath("$.items[0].keyword").value(word + "나"))
                .andExpect(jsonPath("$.items[1].keyword").value(word + "가"));

        clock.set(start.plus(Duration.ofMinutes(11))); // 10분이 지나면 다시 센다
        search(word + "가", same);
        search(word + "가", same);
        mvc.perform(get(TRENDING)).andExpect(jsonPath("$.items[0].keyword").value(word + "가"));
    }

    @Test
    void S1_3_공백과_대소문자를_맞춰_센다() throws Exception {
        search("  " + word + "boot   Spring  BOOT ", visitor());
        search(word + "boot spring boot", visitor());

        mvc.perform(get(TRENDING))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].keyword").value(word + "boot spring boot"));
    }

    @Test
    void S1_4_금지어가_든_검색어는_순위에_넣지_않는다() throws Exception {
        search(word + "다 금지말", visitor()).andExpect(status().isOk());

        mvc.perform(get(TRENDING)).andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void 글자나_숫자_없이_기호뿐인_검색어는_세지_않는다() throws Exception {
        jdbc.update("update post set title = title || ' -- ((' where title like ?", word + "가%");
        search("--", visitor()).andExpect(status().isOk()).andExpect(jsonPath("$.totalCount").isNotEmpty());
        search("((", visitor()).andExpect(status().isOk());
        search(word + "가 --", visitor()).andExpect(status().isOk());

        mvc.perform(get(TRENDING))
                .andExpect(jsonPath("$.items.length()").value(1))
                .andExpect(jsonPath("$.items[0].keyword").value(word + "가 --"));
    }

    @Test
    void S1_5_순위와_10분_전과의_변화() throws Exception {
        for (int i = 0; i < 3; i++) {
            search(word + "나", visitor());
        }
        search(word + "가", visitor());
        // 10분 전: 나(3) 1위, 가(1) 2위

        clock.set(start.plus(Duration.ofMinutes(10)));
        for (int i = 0; i < 3; i++) {
            search(word + "가", visitor());
        }
        search(word + "다", visitor());
        // 지금: 가(4) 1위, 나(3) 2위, 다(1) 3위

        mvc.perform(get(TRENDING))
                .andExpect(jsonPath("$.items[0].keyword").value(word + "가"))
                .andExpect(jsonPath("$.items[0].change").value("up"))
                .andExpect(jsonPath("$.items[0].delta").value(1))
                .andExpect(jsonPath("$.items[1].keyword").value(word + "나"))
                .andExpect(jsonPath("$.items[1].change").value("down"))
                .andExpect(jsonPath("$.items[1].delta").value(1))
                .andExpect(jsonPath("$.items[2].keyword").value(word + "다"))
                .andExpect(jsonPath("$.items[2].change").value("new"));

        clock.set(start.plus(Duration.ofMinutes(75))); // 1시간이 넘게 지나면 빈다
        mvc.perform(get(TRENDING)).andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void S1_6_Redis가_멈춰도_검색은_되고_순위만_빈다() throws Exception {
        doThrow(new RedisConnectionFailureException("Redis 멈춤")).when(redis).opsForZSet();
        doThrow(new RedisConnectionFailureException("Redis 멈춤")).when(redis).opsForValue();

        search(word + "가", visitor()).andExpect(status().isOk())
                .andExpect(jsonPath("$.totalCount").value(1));
        mvc.perform(get(TRENDING)).andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());
    }

    @Test
    void 순위는_10개까지() throws Exception {
        for (int i = 0; i < 12; i++) {
            jdbc.update("update post set title = title || ? where title like ?", " 단어" + i, word + "가%");
        }
        for (int i = 0; i < 12; i++) {
            search(word + "가 단어" + i, visitor());
        }
        mvc.perform(get(TRENDING)).andExpect(jsonPath("$.items.length()").value(10));
    }

    @Test
    void 검색이_없으면_빈_목록이고_잘못된_검색어는_세지_않는다() throws Exception {
        mvc.perform(get(TRENDING)).andExpect(status().isOk()).andExpect(jsonPath("$.items").isEmpty());

        search("가", visitor()).andExpect(status().isBadRequest()); // 두 글자보다 짧다
        mvc.perform(get(TRENDING)).andExpect(jsonPath("$.items").isEmpty());
    }
}
