package com.myblog.stats;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

/**
 * 내 블로그 통계를 읽기만 하는 입구 (specs/020 my_blog_stats). 블로그 관리 대시보드와 같은 숫자다.
 * 블로그가 없으면 비어 있다.
 */
public interface BlogStatsReader {

    Optional<BlogStats> of(Long memberId);

    record BlogStats(Count views, Count visitors, long newCommentCount, List<Day> lastDays, List<Popular> popularPosts) {
    }

    record Count(long today, long yesterday, long total) {
    }

    record Day(LocalDate date, long views, long visitors) {
    }

    /** views: 최근 며칠(대시보드 설정) 조회 수. */
    record Popular(Long postId, String title, long views) {
    }
}
