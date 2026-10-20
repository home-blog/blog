package com.myblog.stats.service;

import com.myblog.blog.BlogDirectory;
import com.myblog.stats.BlogStatsReader;
import com.myblog.stats.service.DashboardService.Dashboard;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** BlogStatsReader를 대시보드 계산으로 채운다 (specs/020). 블로그가 없으면 대시보드를 부르지 않고 비운다. */
@Component
public class BlogStatsReaderAdapter implements BlogStatsReader {

    private final BlogDirectory blogDirectory;
    private final DashboardService dashboard;

    public BlogStatsReaderAdapter(BlogDirectory blogDirectory, DashboardService dashboard) {
        this.blogDirectory = blogDirectory;
        this.dashboard = dashboard;
    }

    @Override
    public Optional<BlogStats> of(Long memberId) {
        if (blogDirectory.myBlog(memberId).isEmpty()) {
            return Optional.empty();
        }
        Dashboard board = dashboard.dashboard(memberId);
        return Optional.of(new BlogStats(
                new Count(board.views().today(), board.views().yesterday(), board.views().total()),
                new Count(board.visitors().today(), board.visitors().yesterday(), board.visitors().total()),
                board.newCommentCount(),
                board.chart().stream().map(day -> new Day(day.date(), day.views(), day.visitors())).toList(),
                board.popularPosts().stream().map(post -> new Popular(post.postId(), post.title(), post.views()))
                        .toList()));
    }
}
