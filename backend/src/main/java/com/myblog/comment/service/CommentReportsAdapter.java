package com.myblog.comment.service;

import com.myblog.comment.CommentReports;
import com.myblog.comment.config.CommentModerationProperties;
import com.myblog.comment.repository.CommentRepository;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** CommentReports를 comment_report·comment 표로 채운다 (specs/022 D-1). 고정 쿼리에 값만 넣는다. */
@Component
public class CommentReportsAdapter implements CommentReports {

    private static final String PENDING_GROUPS = """
            select r.comment_id, count(*) as total,
                   count(*) filter (where r.reason = 'SPAM') as spam,
                   count(*) filter (where r.reason = 'ABUSE') as abuse,
                   count(*) filter (where r.reason = 'ADULT') as adult,
                   count(*) filter (where r.reason = 'OTHER') as other,
                   min(r.created_at) as first_at, max(r.created_at) as last_at
            from comment_report r
            join comment c on c.comment_id = r.comment_id
            where r.resolved_at is null and c.deleted_at is null
            group by r.comment_id
            order by total desc, first_at asc, r.comment_id asc
            offset ? limit ?
            """;

    private final JdbcTemplate jdbc;
    private final CommentRepository comments;
    private final int excerptLength;

    public CommentReportsAdapter(JdbcTemplate jdbc, CommentRepository comments, CommentModerationProperties properties) {
        this.jdbc = jdbc;
        this.comments = comments;
        this.excerptLength = properties.excerptLength();
    }

    @Override
    @Transactional(readOnly = true)
    public long pendingCommentCount() {
        return count("select count(distinct r.comment_id) from comment_report r join comment c on c.comment_id = r.comment_id"
                + " where r.resolved_at is null and c.deleted_at is null");
    }

    @Override
    @Transactional(readOnly = true)
    public long pendingReportCount() {
        return count("select count(*) from comment_report r join comment c on c.comment_id = r.comment_id"
                + " where r.resolved_at is null and c.deleted_at is null");
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingComment> pendingComments(long offset, int limit) {
        return jdbc.query(PENDING_GROUPS, (rs, rowNum) -> new PendingComment(rs.getLong("comment_id"),
                rs.getLong("total"), rs.getLong("spam"), rs.getLong("abuse"), rs.getLong("adult"), rs.getLong("other"),
                instant(rs, "first_at"), instant(rs, "last_at")), offset, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentReportLine> pendingOf(Long commentId) {
        return jdbc.query("select reason, detail, created_at from comment_report where comment_id = ?"
                        + " and resolved_at is null order by created_at, comment_report_id",
                (rs, rowNum) -> new CommentReportLine(rs.getString("reason"), rs.getString("detail"),
                        instant(rs, "created_at")), commentId);
    }

    @Override
    @Transactional
    public boolean lock(Long commentId) {
        return comments.lockForUpdate(commentId).isPresent();
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<CommentBrief> brief(Long commentId) {
        return comments.findById(commentId).filter(comment -> !comment.isDeleted())
                .map(comment -> new CommentBrief(comment.getId(), comment.getPostId(), comment.getMemberId(),
                        comment.getBody(), excerpt(comment.getBody()), comment.isHidden(), comment.getHiddenBy()));
    }

    @Override
    @Transactional
    public int resolve(Long commentId, String resolution, Instant now) {
        return jdbc.update("update comment_report set resolved_at = ?, resolution = ?"
                + " where comment_id = ? and resolved_at is null", Timestamp.from(now), resolution, commentId);
    }

    @Override
    @Transactional
    public boolean hide(Long commentId, Instant now) {
        return jdbc.update("update comment set hidden_at = ?, hidden_by = 'editor'"
                + " where comment_id = ? and hidden_at is null", Timestamp.from(now), commentId) > 0;
    }

    @Override
    @Transactional
    public boolean restore(Long commentId) {
        return jdbc.update("update comment set hidden_at = null, hidden_by = null"
                + " where comment_id = ? and hidden_at is not null", commentId) > 0;
    }

    /** 앞부분: 줄바꿈을 빈칸 하나로 모으고 정한 길이에서 자른다(편집실만 본다). */
    private String excerpt(String body) {
        String flat = body.replaceAll("\\s+", " ").strip();
        return flat.codePointCount(0, flat.length()) <= excerptLength ? flat
                : flat.substring(0, flat.offsetByCodePoints(0, excerptLength)) + "…";
    }

    private long count(String sql) {
        Long value = jdbc.queryForObject(sql, Long.class);
        return value == null ? 0 : value;
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        return rs.getTimestamp(column).toInstant();
    }
}
