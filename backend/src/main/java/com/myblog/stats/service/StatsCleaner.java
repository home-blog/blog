package com.myblog.stats.service;

import com.myblog.blog.BlogClosingEvent;
import com.myblog.post.PostDeletingEvent;
import com.myblog.stats.repository.BlogDailyStatRepository;
import com.myblog.stats.repository.PostDailyStatRepository;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 블로그를 닫거나 글을 지울 때 그 통계를 먼저 지운다 (specs/006 T010, D-6, data-model 4).
 * 통계 표가 블로그·글을 가리키므로(연쇄 삭제 없음) 블로그·글보다 먼저 지워야 한다.
 * 둘 다 {@code @EventListener}로 <b>같은 트랜잭션 안에서</b> 돈다: 실패하면 탈퇴·글 삭제 전체가 취소된다.
 * 탈퇴 때는 글 모듈이 글마다 PostDeletingEvent를 내므로(PostBlogClosingCleaner) 글별 통계도 함께 지워진다.
 * 지운 글의 조회수는 블로그 일별 통계에 남는다 (누적은 그 합, D-7).
 */
@Component
public class StatsCleaner {

    private final BlogDailyStatRepository blogStats;
    private final PostDailyStatRepository postStats;

    public StatsCleaner(BlogDailyStatRepository blogStats, PostDailyStatRepository postStats) {
        this.blogStats = blogStats;
        this.postStats = postStats;
    }

    @EventListener
    public void on(BlogClosingEvent event) {
        blogStats.deleteByBlogId(event.blogId());
    }

    @EventListener
    public void on(PostDeletingEvent event) {
        postStats.deleteByPostId(event.postId());
    }
}
