package com.myblog.community.service;

import com.myblog.community.PostReports;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** PostReports를 post_report 표로 채운다 (specs/018 D-3). 고정 쿼리에 값만 넣는다. */
@Component
public class PostReportsAdapter implements PostReports {

    private static final String PENDING_GROUPS = """
            select post_id, count(*) as total,
                   count(*) filter (where reason = 'SPAM') as spam,
                   count(*) filter (where reason = 'ABUSE') as abuse,
                   count(*) filter (where reason = 'ADULT') as adult,
                   count(*) filter (where reason = 'OTHER') as other,
                   min(created_at) as first_at, max(created_at) as last_at
            from post_report
            where resolved_at is null
            group by post_id
            order by total desc, first_at asc, post_id asc
            offset ? limit ?
            """;

    private final JdbcTemplate jdbc;

    public PostReportsAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    @Transactional(readOnly = true)
    public long pendingPostCount() {
        Long count = jdbc.queryForObject("select count(distinct post_id) from post_report where resolved_at is null",
                Long.class);
        return count == null ? 0 : count;
    }

    @Override
    @Transactional(readOnly = true)
    public long pendingReportCount() {
        Long count = jdbc.queryForObject("select count(*) from post_report where resolved_at is null", Long.class);
        return count == null ? 0 : count;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingPost> pendingPosts(long offset, int limit) {
        return jdbc.query(PENDING_GROUPS, (rs, rowNum) -> new PendingPost(rs.getLong("post_id"), rs.getLong("total"),
                rs.getLong("spam"), rs.getLong("abuse"), rs.getLong("adult"), rs.getLong("other"),
                instant(rs, "first_at"), instant(rs, "last_at")), offset, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ReportLine> pendingOf(Long postId) {
        return jdbc.query("select reason, detail, created_at from post_report where post_id = ? and resolved_at is null"
                        + " order by created_at, post_report_id",
                (rs, rowNum) -> new ReportLine(rs.getString("reason"), rs.getString("detail"), instant(rs, "created_at")),
                postId);
    }

    @Override
    @Transactional
    public int resolve(Long postId, String resolution, Instant now) {
        return jdbc.update("update post_report set resolved_at = ?, resolution = ? where post_id = ? and resolved_at is null",
                Timestamp.from(now), resolution, postId);
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        return rs.getTimestamp(column).toInstant();
    }
}
