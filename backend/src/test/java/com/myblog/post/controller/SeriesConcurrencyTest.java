package com.myblog.post.controller;

import static org.assertj.core.api.Assertions.assertThat;

import com.myblog.post.service.SeriesService;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import com.myblog.support.TestPosts;
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
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 연재 동시 요청 (specs/013 US2, PR #72 리뷰): 한도 검사와 넣기가 겹쳐도 한도를 넘지 않고, 같은 글을 두 연재에
 * 동시에 넣어도 500이 나지 않는다. 요청마다 따로 커밋해야 하므로 @Transactional을 쓰지 않고 끝에서 지운다.
 */
@SpringBootTest
@Import({TestMembers.class, TestPosts.class})
class SeriesConcurrencyTest {

    private static final int THREADS = 8;

    @Autowired
    private TestMembers members;

    @Autowired
    private TestPosts posts;

    @Autowired
    private JdbcTemplate jdbc;

    @Autowired
    private SeriesService seriesService;

    private Member owner;

    @BeforeEach
    void setUp() {
        owner = members.register("seriesrace");
    }

    @AfterEach
    void tearDown() {
        jdbc.update("delete from series_post where series_id in (select series_id from series where blog_id = ?)", owner.blogId());
        jdbc.update("delete from series where blog_id = ?", owner.blogId());
        jdbc.update("delete from post where category_id in (select category_id from category where blog_id = ?)", owner.blogId());
        jdbc.update("delete from category where blog_id = ?", owner.blogId());
        jdbc.update("delete from blog where blog_id = ?", owner.blogId());
        jdbc.update("delete from users where users_id = ?", owner.id());
    }

    private List<Throwable> runTogether(List<Callable<Object>> tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.size());
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Object>> futures = new ArrayList<>();
        try {
            for (Callable<Object> task : tasks) {
                futures.add(pool.submit(() -> {
                    start.await();
                    return task.call();
                }));
            }
            start.countDown();
            List<Throwable> failures = new ArrayList<>();
            for (Future<Object> future : futures) {
                try {
                    future.get();
                } catch (java.util.concurrent.ExecutionException e) {
                    failures.add(e.getCause());
                }
            }
            return failures;
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    void 한도_바로_앞에서_동시에_넣어도_한도를_넘지_않는다() throws Exception {
        Long seriesId = seriesService.create(owner.id(), "긴 연재", "").seriesId();
        for (int i = 0; i < 199; i++) {
            Long postId = posts.add(owner.defaultCategoryId(), "글" + i, "본문", "public", Instant.now());
            jdbc.update("insert into series_post (series_id, post_id, sort_order) values (?, ?, ?)", seriesId, postId, i + 1);
        }
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            Long postId = posts.add(owner.defaultCategoryId(), "동시 " + i, "본문", "public", Instant.now());
            tasks.add(() -> {
                seriesService.addPost(owner.id(), seriesId, postId);
                return null;
            });
        }
        runTogether(tasks);
        assertThat(jdbc.queryForObject("select count(*) from series_post where series_id = ?", Long.class, seriesId))
                .isEqualTo(200);
        assertThat(jdbc.queryForObject("select count(distinct sort_order) from series_post where series_id = ?", Long.class,
                seriesId)).as("화 순서가 겹치지 않는다").isEqualTo(200);
    }

    @Test
    void 같은_글을_여러_연재에_동시에_넣어도_한_연재에만_들고_500이_나지_않는다() throws Exception {
        Long postId = posts.add(owner.defaultCategoryId(), "글", "본문", "public", Instant.now());
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            Long seriesId = seriesService.create(owner.id(), "연재 " + i, "").seriesId();
            tasks.add(() -> {
                seriesService.addPost(owner.id(), seriesId, postId);
                return null;
            });
        }
        List<Throwable> failures = runTogether(tasks);
        assertThat(failures).isEmpty();
        assertThat(jdbc.queryForObject("select count(*) from series_post where post_id = ?", Long.class, postId)).isOne();
    }

    @Test
    void 블로그_연재_한도_바로_앞에서_동시에_만들어도_한도를_넘지_않는다() throws Exception {
        for (int i = 0; i < 49; i++) {
            jdbc.update("insert into series (blog_id, name, created_at) values (?, ?, now())", owner.blogId(), "연재" + i);
        }
        List<Callable<Object>> tasks = new ArrayList<>();
        for (int i = 0; i < THREADS; i++) {
            tasks.add(() -> seriesService.create(owner.id(), "마지막", ""));
        }
        runTogether(tasks);
        assertThat(jdbc.queryForObject("select count(*) from series where blog_id = ?", Long.class, owner.blogId())).isEqualTo(50);
    }
}
