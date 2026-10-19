package com.myblog.post.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.myblog.post.service.DraftPublisher;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
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
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

/**
 * 예약 발행 S4-4: 앱 두 벌이 같은 시각에 예약을 올려도 글은 하나씩만 생긴다 (D-3, SKIP LOCKED + 요청 번호).
 * 두 벌이 따로 커밋해야 하므로 @Transactional을 쓰지 않고 끝에서 지운다.
 * 서버의 예약 발행 작업(1분마다)이 테스트 도중에 끼어들면 그 작업이 올린 수만큼 total이 모자라므로,
 * 이 테스트에서는 그 작업의 첫 실행을 한참 뒤로 미룬다(두 벌은 테스트가 직접 흉내 낸다).
 */
@SpringBootTest
@Import(TestMembers.class)
@TestPropertySource(properties = "writing.schedule.check-interval=1h")
class DraftScheduleConcurrencyTest {

    private static final int DRAFTS = 20;

    @Autowired
    private TestMembers members;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DraftPublisher publisher;

    private Member owner;

    @BeforeEach
    void setUp() {
        owner = members.register("sched");
        Long topicId = jdbc.queryForObject("select min(topic_id) from topic", Long.class);
        for (int i = 0; i < DRAFTS; i++) {
            jdbc.update("""
                    insert into post_draft (blog_id, category_id, topic_id, title, content, visibility, publish_at,
                                            request_key, created_at, updated_at)
                    values (?, ?, ?, ?, '본문', 'public', now() - interval '1 minute', ?, now(), now())
                    """, owner.blogId(), owner.defaultCategoryId(), topicId, "예약 " + i, UUID.randomUUID().toString());
        }
    }

    @AfterEach
    void tearDown() {
        jdbc.update("delete from post_draft where blog_id = ?", owner.blogId());
        jdbc.update("delete from post_tag where post_id in (select p.post_id from post p join category c on c.category_id = p.category_id"
                + " where c.blog_id = ?)", owner.blogId());
        jdbc.update("delete from post where category_id in (select category_id from category where blog_id = ?)", owner.blogId());
        jdbc.update("delete from category where blog_id = ?", owner.blogId());
        jdbc.update("delete from blog where blog_id = ?", owner.blogId());
        jdbc.update("delete from users where users_id = ?", owner.id());
    }

    @Test
    void S4_4_두_벌이_동시에_올려도_예약마다_글은_하나다() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        try {
            List<Future<Integer>> results = new ArrayList<>();
            for (int i = 0; i < 2; i++) {
                Callable<Integer> slot = () -> {
                    start.await();
                    int published = 0;
                    // 한 번에 batch-size까지 올리므로 끝날 때까지 되풀이한다
                    for (int n; (n = publisher.publishDue()) > 0; ) {
                        published += n;
                    }
                    return published;
                };
                results.add(pool.submit(slot));
            }
            start.countDown();
            int total = 0;
            for (Future<Integer> result : results) {
                total += result.get();
            }
            // 올리는 작업은 DB 전체의 예약을 본다. 개발 DB에 시각이 지난 다른 예약이 있으면 그것도 함께 올리므로 "이상"으로 본다.
            // 예약마다 글이 하나인지는 아래에서 이 블로그의 글 수로 확인한다
            assertThat(total).isGreaterThanOrEqualTo(DRAFTS);
        } finally {
            pool.shutdownNow();
        }
        assertThat(jdbc.queryForObject("select count(*) from post p join category c on c.category_id = p.category_id"
                + " where c.blog_id = ?", Long.class, owner.blogId())).isEqualTo(DRAFTS);
        assertThat(jdbc.queryForObject("select count(*) from post_draft where blog_id = ?", Long.class, owner.blogId())).isZero();
    }
}
