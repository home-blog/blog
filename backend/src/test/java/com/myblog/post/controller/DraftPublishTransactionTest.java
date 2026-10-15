package com.myblog.post.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doThrow;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.post.controller.dto.DraftRequests;
import com.myblog.post.service.DraftService;
import com.myblog.post.service.SeriesService;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
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
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

/**
 * 임시 저장 글 발행(contracts 3-1)은 글 만들기·연재 넣기·임시 저장 글 지우기를 한 트랜잭션으로 한다 (#74 리뷰).
 * 뒤 단계가 실패하면 글도 남지 않고, 같은 글을 두 요청이 함께 발행해도 글은 하나다.
 * 요청마다 따로 커밋해야 하므로 @Transactional을 쓰지 않고 끝에서 지운다.
 */
@SpringBootTest
@Import(TestMembers.class)
class DraftPublishTransactionTest {

    @Autowired
    private TestMembers members;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private DraftService draftService;

    @MockitoSpyBean
    private SeriesService seriesService;

    private Member owner;
    private Long topicId;
    private Long draftId;

    @BeforeEach
    void setUp() {
        owner = members.register("pubtx");
        topicId = jdbc.queryForObject("select min(topic_id) from topic", Long.class);
        draftId = jdbc.queryForObject("""
                insert into post_draft (blog_id, category_id, topic_id, title, content, visibility, request_key,
                                        created_at, updated_at)
                values (?, ?, ?, '임시 저장 글', '본문', 'public', ?, now(), now()) returning post_draft_id
                """, Long.class, owner.blogId(), owner.defaultCategoryId(), topicId, UUID.randomUUID().toString());
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

    private DraftRequests.Publish request() {
        return new DraftRequests.Publish("임시 저장 글", "본문", owner.defaultCategoryId(), topicId, "public", List.of(), null);
    }

    private long posts() {
        return jdbc.queryForObject("select count(*) from post p join category c on c.category_id = p.category_id"
                + " where c.blog_id = ?", Long.class, owner.blogId());
    }

    private long drafts() {
        return jdbc.queryForObject("select count(*) from post_draft where blog_id = ?", Long.class, owner.blogId());
    }

    @Test
    void 글을_만든_뒤_단계가_실패하면_글도_남지_않고_임시_저장_글은_그대로다() {
        doThrow(new IllegalStateException("연재 넣기 실패")).when(seriesService).attachIfPossible(anyLong(), any(), anyLong());

        assertThatThrownBy(() -> draftService.publish(owner.id(), draftId, request()))
                .isInstanceOf(IllegalStateException.class);

        assertThat(posts()).isZero();
        assertThat(drafts()).isOne();
    }

    @Test
    void 같은_임시_저장_글을_두_요청이_함께_발행해도_글은_하나고_둘째는_404다() throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(2);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Long>> results = new ArrayList<>();
        try {
            for (int i = 0; i < 2; i++) {
                Callable<Long> publish = () -> {
                    start.await();
                    return draftService.publish(owner.id(), draftId, request()).postId();
                };
                results.add(pool.submit(publish));
            }
            start.countDown();
            int published = 0;
            int notFound = 0;
            for (Future<Long> result : results) {
                try {
                    result.get();
                    published++;
                } catch (ExecutionException e) {
                    assertThat(e.getCause()).isInstanceOf(ApiException.class);
                    assertThat(((ApiException) e.getCause()).errorCode()).isEqualTo(ErrorCode.DRAFT_NOT_FOUND);
                    notFound++;
                }
            }
            assertThat(published).isOne();
            assertThat(notFound).isOne();
        } finally {
            pool.shutdownNow();
        }
        assertThat(posts()).isOne();
        assertThat(drafts()).isZero();
    }
}
