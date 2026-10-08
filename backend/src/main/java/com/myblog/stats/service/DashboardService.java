package com.myblog.stats.service;

import com.myblog.blog.BlogDirectory;
import com.myblog.blog.BlogDirectory.BlogInfo;
import com.myblog.comment.NewCommentCounter;
import com.myblog.common.config.ManageProperties;
import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.post.PostSummaryQuery;
import com.myblog.post.PostSummaryQuery.PostSummary;
import com.myblog.stats.domain.BlogDailyStat;
import com.myblog.stats.repository.BlogDailyStatRepository;
import com.myblog.stats.repository.BlogDailyStatRepository.Totals;
import com.myblog.stats.repository.PostDailyStatRepository;
import com.myblog.stats.repository.PostDailyStatRepository.PostViews;
import com.myblog.stats.service.StatsQueryService.DailyCount;
import com.myblog.stats.time.ServiceDay;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 대시보드 (specs/006 contracts 2, T046, data-model 6 `숫자 계산 규칙`, FR-006 ~ FR-011).
 * <ul>
 *   <li>오늘·어제: 그 한국 날짜의 블로그 일별 통계 줄 (없으면 0). 누적: 모든 줄의 합 (D-7, 지운 글의 조회수도 남는다).</li>
 *   <li>인기 글: 내 글의 최근 popular-days일 글별 조회수 합이 큰 순 → 누구나 볼 수 있는 글만 → 같으면 늦게 쓴 글 먼저 →
 *       popular-size개. 숫자는 그 기간의 합 (D-6). 비공개 분류에 든 공개 글은 뺀다.</li>
 *   <li>최근 글: 비공개 포함 recent-size개. 새 댓글 수: NewCommentCounter (메뉴 옆과 같은 계산, SC-008).</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class DashboardService {

    /** 조회수가 많은 순, 같으면 늦게 쓴 글 먼저 (같은 시각이면 번호가 큰 글). */
    private static final Comparator<Candidate> MOST_VIEWED_FIRST = Comparator.comparingLong(Candidate::views).reversed()
            .thenComparing((Candidate c) -> c.post().createdAt(), Comparator.reverseOrder())
            .thenComparing((Candidate c) -> c.post().postId(), Comparator.reverseOrder());

    private final BlogDirectory blogDirectory;
    private final BlogDailyStatRepository blogStats;
    private final PostDailyStatRepository postStats;
    private final PostSummaryQuery postSummary;
    private final NewCommentCounter newCommentCounter;
    private final StatsQueryService statsQuery;
    private final ServiceDay serviceDay;
    private final ManageProperties.Dashboard properties;

    public DashboardService(BlogDirectory blogDirectory, BlogDailyStatRepository blogStats, PostDailyStatRepository postStats,
            PostSummaryQuery postSummary, NewCommentCounter newCommentCounter, StatsQueryService statsQuery,
            ServiceDay serviceDay, ManageProperties properties) {
        this.blogDirectory = blogDirectory;
        this.blogStats = blogStats;
        this.postStats = postStats;
        this.postSummary = postSummary;
        this.newCommentCounter = newCommentCounter;
        this.statsQuery = statsQuery;
        this.serviceDay = serviceDay;
        this.properties = properties.dashboard();
    }

    public Dashboard dashboard(Long memberId) {
        BlogInfo blog = blogDirectory.myBlog(memberId).orElseThrow(() -> new ApiException(ErrorCode.BLOG_NOT_FOUND));
        LocalDate today = serviceDay.today();
        Map<LocalDate, BlogDailyStat> recentRows = blogStats.findByBlogIdAndStatDateBetween(blog.blogId(), today.minusDays(1), today)
                .stream().collect(Collectors.toMap(BlogDailyStat::getStatDate, row -> row));
        Optional<BlogDailyStat> todayRow = Optional.ofNullable(recentRows.get(today));
        Optional<BlogDailyStat> yesterdayRow = Optional.ofNullable(recentRows.get(today.minusDays(1)));
        Totals totals = blogStats.totalsOf(blog.blogId());

        Count views = new Count(todayRow.map(BlogDailyStat::getViews).orElse(0),
                yesterdayRow.map(BlogDailyStat::getViews).orElse(0), totals.getViews());
        Count visitors = new Count(todayRow.map(BlogDailyStat::getVisitors).orElse(0),
                yesterdayRow.map(BlogDailyStat::getVisitors).orElse(0), totals.getVisitors());
        List<RecentPost> recent = postSummary.recent(blog.blogId(), properties.recentSize()).stream()
                .map(post -> new RecentPost(post.postId(), post.title(), post.createdAt(), post.visibility())).toList();
        return new Dashboard(views, visitors, newCommentCounter.countFor(memberId),
                statsQuery.chart(blog.blogId(), properties.chartDays()), popular(blog.blogId(), today), recent);
    }

    private List<PopularPost> popular(Long blogId, LocalDate today) {
        List<Long> postIds = postSummary.postIdsOf(blogId);
        if (postIds.isEmpty()) {
            return List.of();
        }
        LocalDate from = today.minusDays(properties.popularDays() - 1L);
        Map<Long, Long> viewsByPost = postStats.sumViewsSince(postIds, from).stream()
                .filter(row -> row.getViews() > 0)
                .collect(Collectors.toMap(PostViews::getPostId, PostViews::getViews));
        return postSummary.publicAmong(viewsByPost.keySet()).stream()
                .map(post -> new Candidate(post, viewsByPost.get(post.postId())))
                .sorted(MOST_VIEWED_FIRST)
                .limit(properties.popularSize())
                .map(c -> new PopularPost(c.post().postId(), c.post().title(), c.views()))
                .toList();
    }

    private record Candidate(PostSummary post, long views) {
    }

    public record Dashboard(Count views, Count visitors, long newCommentCount, List<DailyCount> chart,
            List<PopularPost> popularPosts, List<RecentPost> recentPosts) {
    }

    public record Count(long today, long yesterday, long total) {
    }

    /** views: 최근 popular-days일 조회수 (D-6). */
    public record PopularPost(Long postId, String title, long views) {
    }

    public record RecentPost(Long postId, String title, Instant createdAt, String visibility) {
    }
}
