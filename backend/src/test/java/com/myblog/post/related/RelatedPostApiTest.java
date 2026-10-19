package com.myblog.post.related;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.myblog.post.service.PostEmbeddingService;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import java.util.List;
import java.util.UUID;
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
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * 비슷한 글 (specs/014 US2, S2-1 ~ S2-6). 뜻 정보는 가짜 임베딩(글자 두 개씩 묶어 센 벡터)으로 만든다.
 * 개발 DB에는 다른 글과 그 임베딩이 있을 수 있어, 이 테스트의 글에는 다른 글에 없는 낱말을 넣어 서로 가장 가깝게 한다.
 */
@SpringBootTest
@Transactional
@Import(TestMembers.class)
@TestPropertySource(properties = "related.provider=fake")
class RelatedPostApiTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private PostEmbeddingService embeddings;

    @MockitoSpyBean
    private Embedder embedder;

    @MockitoSpyBean
    private StringRedisTemplate redis;

    private MockMvc mvc;
    private Long topicId;
    private String word;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        topicId = jdbc.queryForObject("select min(topic_id) from topic", Long.class);
        // 이 테스트에만 있는 낱말 (가짜 임베딩에서 이 낱말을 나눠 가진 글끼리 가깝다)
        word = "쿼르탄" + UUID.randomUUID().toString().substring(0, 6);
    }

    private Long post(Member writer, String title, String content, String visibility) {
        return jdbc.queryForObject("""
                insert into post (category_id, topic_id, title, content, visibility, created_at)
                values (?, ?, ?, ?, ?, now()) returning post_id
                """, Long.class, writer.defaultCategoryId(), topicId, title, content, visibility);
    }

    private String repeat(String text) {
        return (word + " " + text + " ").repeat(6);
    }

    @Test
    void S2_2_S2_4_뜻이_가까운_순이고_자신과_볼_수_없는_글은_없다() throws Exception {
        Member a = members.register("rel");
        Member b = members.register("rel");
        Member c = members.register("rel");
        Long source = post(a, "제주 렌터카 여행 후기", repeat("제주 렌터카 여행 바다"), "public");
        Long near = post(b, "제주 버스 여행 팁", repeat("제주 버스 여행 바다"), "public");
        Long far = post(c, "자바 트랜잭션", word + " 스프링 트랜잭션 격리 수준 잠금", "public");
        Long hidden = post(b, "제주 렌터카 여행 비밀", repeat("제주 렌터카 여행 바다"), "private");
        Long privateCategory = jdbc.queryForObject("""
                insert into category (blog_id, name, visibility, is_default, sort_order, created_at)
                values (?, '숨긴 분류', 'private', false, 9, now()) returning category_id
                """, Long.class, c.blogId());
        Long hiddenByCategory = jdbc.queryForObject("""
                insert into post (category_id, topic_id, title, content, visibility, created_at)
                values (?, ?, '비공개 분류의 글', ?, 'public', now()) returning post_id
                """, Long.class, privateCategory, topicId, repeat("제주 렌터카 여행 바다"));
        embeddings.refresh(List.of(source, near, far, hidden, hiddenByCategory));

        String body = mvc.perform(get("/api/v1/posts/" + source + "/related"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basis").value("meaning"))
                .andExpect(jsonPath("$.posts.length()").value(lessThanOrEqualTo(4)))
                .andExpect(jsonPath("$.posts[0].postId").value(near))
                .andExpect(jsonPath("$.posts[0].title").value("제주 버스 여행 팁"))
                .andReturn().getResponse().getContentAsString();

        List<Number> ids = JsonPath.read(body, "$.posts[*].postId");
        // 뜻이 먼 글(far)은 맨 앞에 오지 않는다(맨 앞은 near). 볼 수 없는 글과 자신은 아예 없다
        assertThat(ids).extracting(Number::longValue).doesNotContain(source, hidden, hiddenByCategory);
        assertThat(ids.get(0).longValue()).isNotEqualTo(far);
    }

    @Test
    void S2_3_한_블로그의_글은_두_개까지() throws Exception {
        Member a = members.register("rel");
        Member b = members.register("rel");
        Long source = post(a, "가을 산행", repeat("가을 산행 단풍 능선"), "public");
        Long b1 = post(b, "가을 산행 하나", repeat("가을 산행 단풍 능선 하나"), "public");
        Long b2 = post(b, "가을 산행 둘", repeat("가을 산행 단풍 능선 둘"), "public");
        Long b3 = post(b, "가을 산행 셋", repeat("가을 산행 단풍 능선 셋"), "public");
        embeddings.refresh(List.of(source, b1, b2, b3));

        String body = mvc.perform(get("/api/v1/posts/" + source + "/related"))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();

        List<Number> blogIds = JsonPath.read(body, "$.posts[*].blogId");
        assertThat(blogIds.stream().filter(id -> id.longValue() == b.blogId())).hasSize(2);
    }

    @Test
    void S2_1_볼_수_없는_글의_비슷한_글은_404() throws Exception {
        Member a = members.register("rel");
        Long hidden = post(a, "비밀 글", "비밀", "private");

        mvc.perform(get("/api/v1/posts/" + hidden + "/related")).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
        mvc.perform(get("/api/v1/posts/987654321/related")).andExpect(status().isNotFound());
    }

    @Test
    void S2_4_뜻_정보가_없으면_태그가_겹치는_글로_대신한다() throws Exception {
        Member a = members.register("rel");
        Member b = members.register("rel");
        Long source = post(a, "태그 글", "본문", "public");
        Long two = post(b, "태그 둘 겹침", "본문", "public");
        Long one = post(b, "태그 하나 겹침", "본문", "public");
        String t1 = word + "가";
        String t2 = word + "나";
        Long tag1 = jdbc.queryForObject("insert into tag (name) values (?) returning tag_id", Long.class, t1);
        Long tag2 = jdbc.queryForObject("insert into tag (name) values (?) returning tag_id", Long.class, t2);
        for (Object[] link : List.of(new Object[] {source, tag1}, new Object[] {source, tag2}, new Object[] {two, tag1},
                new Object[] {two, tag2}, new Object[] {one, tag1})) {
            jdbc.update("insert into post_tag (post_id, tag_id) values (?, ?)", link);
        }

        mvc.perform(get("/api/v1/posts/" + source + "/related"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basis").value("tags"))
                .andExpect(jsonPath("$.posts[0].postId").value(two))
                .andExpect(jsonPath("$.posts[1].postId").value(one));
    }

    @Test
    void S2_5_본문이_그대로면_다시_만들지_않고_바뀌면_다시_만든다() {
        Member a = members.register("rel");
        Long id = post(a, "제목", "처음 본문 " + word, "public");

        embeddings.refresh(List.of(id));
        embeddings.refresh(List.of(id));
        verify(embedder, times(1)).embed(anyList());

        jdbc.update("update post set content = ?, updated_at = now() where post_id = ?", "고친 본문 " + word, id);
        embeddings.refresh(List.of(id));
        verify(embedder, times(2)).embed(anyList());
        assertThat(jdbc.queryForObject("select count(*) from post_embedding where post_id = ?", Integer.class, id)).isOne();
    }

    @Test
    void S2_5_비공개_글은_임베딩하지_않는다() {
        Member a = members.register("rel");
        Long hidden = post(a, "비밀", "비밀 본문", "private");

        embeddings.refresh(List.of(hidden));

        verify(embedder, never()).embed(anyList());
        assertThat(jdbc.queryForObject("select count(*) from post_embedding where post_id = ?", Integer.class, hidden)).isZero();
    }

    @Test
    void S2_5_임베딩이_실패해도_예외를_내지_않는다() {
        Member a = members.register("rel");
        Long id = post(a, "제목", "본문 " + word, "public");
        doThrow(new Embedder.UnavailableException("시간 초과")).when(embedder).embed(anyList());

        embeddings.refresh(List.of(id));

        assertThat(jdbc.queryForObject("select count(*) from post_embedding where post_id = ?", Integer.class, id)).isZero();
    }

    @Test
    void S2_6_정리_작업은_뜻_정보가_없는_공개_글을_채운다() {
        Member a = members.register("rel");
        Long open = post(a, "공개", "공개 본문 " + word, "public");
        Long hidden = post(a, "비공개", "비공개 본문 " + word, "private");
        clearInvocations(embedder);

        // 개발 DB의 다른 글도 같이 채울 수 있으므로 이 글이 채워질 때까지 되풀이한다
        for (int i = 0; i < 200 && count(open) == 0; i++) {
            if (embeddings.backfill() == 0) {
                break;
            }
        }

        assertThat(count(open)).isOne();
        assertThat(count(hidden)).isZero();
    }

    private int count(Long postId) {
        return jdbc.queryForObject("select count(*) from post_embedding where post_id = ?", Integer.class, postId);
    }

    @Test
    void 묶음이_실패하면_한_글씩_다시_보내_실패한_글만_건너뛴다() {
        Member a = members.register("rel");
        Long bad = post(a, "거절될 글", "본문 " + word, "public");
        Long ok = post(a, "괜찮은 글", "본문 " + word, "public");
        doAnswer(call -> {
            List<String> texts = call.getArgument(0);
            if (texts.size() > 1 || texts.get(0).startsWith("거절될 글")) { // 거절되는 글이 앞에 와도 뒤 글은 저장된다
                throw new Embedder.UnavailableException("이 글은 받지 않음");
            }
            return call.callRealMethod();
        }).when(embedder).embed(anyList());

        int saved = embeddings.refresh(List.of(bad, ok));

        assertThat(saved).isOne();
        assertThat(count(ok)).isOne();
        assertThat(count(bad)).isZero();
    }

    @Test
    void 한_글씩_보낸_글이_연달아_두_번_실패하면_장애로_보고_멈춘다() {
        Member a = members.register("rel");
        List<Long> ids = List.of(post(a, "하나", "본문 " + word, "public"), post(a, "둘", "본문 " + word, "public"),
                post(a, "셋", "본문 " + word, "public"));
        clearInvocations(embedder);
        doThrow(new Embedder.UnavailableException("장애")).when(embedder).embed(anyList());

        assertThat(embeddings.refresh(ids)).isZero();
        // 묶음 한 번 + 한 글씩 두 번 (세 번째 글은 보내지 않는다)
        verify(embedder, times(3)).embed(anyList());
    }

    @Test
    void 비슷한_글은_잠깐_보관하고_그_글의_뜻_정보가_바뀌면_지운다() throws Exception {
        Member a = members.register("rel");
        Long source = post(a, "보관할 글", repeat("보관 시험"), "public");
        embeddings.refresh(List.of(source));
        String key = "related:" + source;

        mvc.perform(get("/api/v1/posts/" + source + "/related")).andExpect(status().isOk());
        assertThat(redis.hasKey(key)).isTrue();

        jdbc.update("update post set content = ?, updated_at = now() where post_id = ?", repeat("바뀐 본문"), source);
        embeddings.refresh(List.of(source));
        assertThat(redis.hasKey(key)).isFalse();
    }

    @Test
    void Redis가_멈춰도_보관만_건너뛰고_비슷한_글을_돌려준다() throws Exception {
        Member a = members.register("rel");
        Member b = members.register("rel");
        Long source = post(a, "장마철 빨래", repeat("장마 빨래 제습"), "public");
        Long near = post(b, "장마철 빨래 말리기", repeat("장마 빨래 제습 건조"), "public");
        doThrow(new RedisConnectionFailureException("Redis 멈춤")).when(redis).opsForValue();
        doThrow(new RedisConnectionFailureException("Redis 멈춤")).when(redis).delete(anyString());

        // 뜻 정보를 저장한 뒤 보관 지우기가 실패해도 저장은 남는다
        assertThat(embeddings.refresh(List.of(source, near))).isEqualTo(2);

        mvc.perform(get("/api/v1/posts/" + source + "/related"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.basis").value("meaning"))
                .andExpect(jsonPath("$.posts[0].postId").value(near));
    }

    @Test
    void 보관_값이_깨져_있으면_새로_고른다() throws Exception {
        Member a = members.register("rel");
        Long source = post(a, "깨진 보관", repeat("깨진 보관 시험"), "public");
        embeddings.refresh(List.of(source));
        redis.opsForValue().set("related:" + source, "meaning|1,x");

        mvc.perform(get("/api/v1/posts/" + source + "/related")).andExpect(status().isOk());
        assertThat(redis.opsForValue().get("related:" + source)).doesNotContain("x");
    }
}
