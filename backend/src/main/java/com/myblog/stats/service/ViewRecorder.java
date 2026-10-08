package com.myblog.stats.service;

import com.myblog.post.PostViewCounter;
import com.myblog.stats.repository.BlogDailyStatRepository;
import com.myblog.stats.repository.PostDailyStatRepository;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * 숫자를 쓰는 입구 하나 (specs/006 T054, D-3 A: 바로 DB, research B-3, R-3, FR-033 ~ FR-035, FR-037).
 * 글 읽기와 따로 된 트랜잭션에서 쓴다: 실패해도 글 읽기를 망치지 않는다.
 * 일별 줄은 "없으면 만들고 있으면 +1"을 한 쿼리로 한다 (동시에 와도 빠지지 않음). 조회수와 방문자는 다른 칸이다 (BM-06-5).
 * 느려지면(R-7) Redis에 모았다가 @Scheduled로 옮기는 방식을 이 입구 뒤에 더할 수 있다.
 */
@Service
public class ViewRecorder {

    private final PostViewCounter postViewCounter;
    private final BlogDailyStatRepository blogStats;
    private final PostDailyStatRepository postStats;

    public ViewRecorder(PostViewCounter postViewCounter, BlogDailyStatRepository blogStats, PostDailyStatRepository postStats) {
        this.postViewCounter = postViewCounter;
        this.blogStats = blogStats;
        this.postStats = postStats;
    }

    /** 조회 하나: 글 누적 +1, 그날 블로그 조회수 +1, 그날 글 조회수 +1. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void view(Long postId, Long blogId, LocalDate date) {
        postViewCounter.increment(postId);
        blogStats.addView(blogId, date);
        postStats.addView(postId, date);
    }

    /** 방문자 하나: 그날 블로그 방문자 +1. */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void visit(Long blogId, LocalDate date) {
        blogStats.addVisitor(blogId, date);
    }
}
