package com.myblog.stats.service;

import com.myblog.blog.BlogDirectory;
import com.myblog.blog.BlogDirectory.BlogInfo;
import com.myblog.comment.CommentStatsQuery;
import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.post.PostSummaryQuery;
import com.myblog.stats.domain.BlogDailyStat;
import com.myblog.stats.repository.BlogDailyStatRepository;
import com.myblog.stats.time.ServiceDay;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 블로그의 일별 숫자 (specs/006 T041, research B-5, FR-031, FR-032, FR-036). 대시보드 그래프와 통계 화면이 함께 쓴다.
 * 오늘 포함 n일의 <b>모든 날짜</b>를 오래된 날부터, 기록이 없는 날은 0. 날짜는 한국 날짜.
 */
@Service
@Transactional(readOnly = true)
public class StatsQueryService {

    private final BlogDirectory blogDirectory;
    private final BlogDailyStatRepository blogStats;
    private final PostSummaryQuery postSummary;
    private final CommentStatsQuery commentStats;
    private final ServiceDay serviceDay;

    public StatsQueryService(BlogDirectory blogDirectory, BlogDailyStatRepository blogStats, PostSummaryQuery postSummary,
            CommentStatsQuery commentStats, ServiceDay serviceDay) {
        this.blogDirectory = blogDirectory;
        this.blogStats = blogStats;
        this.postSummary = postSummary;
        this.commentStats = commentStats;
        this.serviceDay = serviceDay;
    }

    /** 통계 화면: 조회수·방문자·댓글 수 (contracts 7). */
    public List<DailyStat> daily(Long memberId, int days) {
        BlogInfo blog = myBlog(memberId);
        List<LocalDate> dates = serviceDay.lastDays(days);
        LocalDate first = dates.getFirst();
        LocalDate last = dates.getLast();
        Map<LocalDate, BlogDailyStat> rows = rowsByDate(blog.blogId(), first, last);
        Map<LocalDate, Long> comments = commentStats.dailyCounts(postSummary.postIdsOf(blog.blogId()),
                serviceDay.startOf(first), serviceDay.startOf(last.plusDays(1)), serviceDay.zone());
        return dates.stream().map(date -> {
            BlogDailyStat row = rows.get(date);
            return new DailyStat(date, row == null ? 0 : row.getViews(), row == null ? 0 : row.getVisitors(),
                    comments.getOrDefault(date, 0L));
        }).toList();
    }

    /** 대시보드 그래프: 조회수·방문자 (contracts 2의 chart). */
    public List<DailyCount> chart(Long blogId, int days) {
        List<LocalDate> dates = serviceDay.lastDays(days);
        Map<LocalDate, BlogDailyStat> rows = rowsByDate(blogId, dates.getFirst(), dates.getLast());
        return dates.stream().map(date -> {
            BlogDailyStat row = rows.get(date);
            return new DailyCount(date, row == null ? 0 : row.getViews(), row == null ? 0 : row.getVisitors());
        }).toList();
    }

    private Map<LocalDate, BlogDailyStat> rowsByDate(Long blogId, LocalDate from, LocalDate to) {
        return blogStats.findByBlogIdAndStatDateBetween(blogId, from, to).stream()
                .collect(Collectors.toMap(BlogDailyStat::getStatDate, Function.identity()));
    }

    private BlogInfo myBlog(Long memberId) {
        return blogDirectory.myBlog(memberId).orElseThrow(() -> new ApiException(ErrorCode.BLOG_NOT_FOUND));
    }

    public record DailyStat(LocalDate date, long views, long visitors, long comments) {
    }

    public record DailyCount(LocalDate date, long views, long visitors) {
    }
}
