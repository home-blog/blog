package com.myblog.board.service;

import com.myblog.board.BoardReports;
import com.myblog.board.config.BoardProperties;
import com.myblog.board.repository.BoardPostRepository;
import com.myblog.post.TopicCatalog;
import com.myblog.post.TopicCatalog.TopicRef;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** BoardReports를 board_report·board_post 표로 채운다 (specs/018 D-5). 고정 쿼리에 값만 넣는다. */
@Component
public class BoardReportsAdapter implements BoardReports {

    private static final String PENDING_GROUPS = """
            select board_post_id, count(*) as total,
                   count(*) filter (where reason = 'SPAM') as spam,
                   count(*) filter (where reason = 'ABUSE') as abuse,
                   count(*) filter (where reason = 'ADULT') as adult,
                   count(*) filter (where reason = 'OTHER') as other,
                   min(created_at) as first_at, max(created_at) as last_at
            from board_report
            where resolved_at is null
            group by board_post_id
            order by total desc, first_at asc, board_post_id asc
            offset ? limit ?
            """;

    private final JdbcTemplate jdbc;
    private final BoardPostRepository posts;
    private final TopicCatalog topics;
    private final int excerptLength;

    public BoardReportsAdapter(JdbcTemplate jdbc, BoardPostRepository posts, TopicCatalog topics,
            BoardProperties properties) {
        this.jdbc = jdbc;
        this.posts = posts;
        this.topics = topics;
        this.excerptLength = properties.excerptLength();
    }

    @Override
    @Transactional(readOnly = true)
    public long pendingPostCount() {
        Long count = jdbc.queryForObject(
                "select count(distinct board_post_id) from board_report where resolved_at is null", Long.class);
        return count == null ? 0 : count;
    }

    @Override
    @Transactional(readOnly = true)
    public long pendingReportCount() {
        Long count = jdbc.queryForObject("select count(*) from board_report where resolved_at is null", Long.class);
        return count == null ? 0 : count;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PendingBoardPost> pendingPosts(long offset, int limit) {
        return jdbc.query(PENDING_GROUPS, (rs, rowNum) -> new PendingBoardPost(rs.getLong("board_post_id"),
                rs.getLong("total"), rs.getLong("spam"), rs.getLong("abuse"), rs.getLong("adult"), rs.getLong("other"),
                instant(rs, "first_at"), instant(rs, "last_at")), offset, limit);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BoardReportLine> pendingOf(Long boardPostId) {
        return jdbc.query("select reason, detail, created_at from board_report where board_post_id = ? and resolved_at is null"
                        + " order by created_at, board_report_id",
                (rs, rowNum) -> new BoardReportLine(rs.getString("reason"), rs.getString("detail"),
                        instant(rs, "created_at")), boardPostId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<BoardBrief> brief(Long boardPostId) {
        return posts.findById(boardPostId).map(post -> new BoardBrief(post.getId(), post.getWriterId(),
                topics.all().stream().filter(t -> t.id().equals(post.getTopicId())).map(TopicRef::name).findFirst()
                        .orElse(""), post.getTitle(), excerpt(post.getContent())));
    }

    @Override
    @Transactional
    public int dismiss(Long boardPostId, Instant now) {
        return jdbc.update("update board_report set resolved_at = ?, resolution = 'dismissed'"
                + " where board_post_id = ? and resolved_at is null", Timestamp.from(now), boardPostId);
    }

    @Override
    @Transactional
    public int remove(Long boardPostId) {
        posts.clearAccepted(boardPostId);
        return posts.deletePost(boardPostId);
    }

    /** 본문 앞부분: 줄바꿈을 빈칸 하나로 모으고 정한 길이에서 자른다(마크다운 기호는 그대로, 편집실만 본다). */
    private String excerpt(String content) {
        String flat = content.replaceAll("\\s+", " ").strip();
        return flat.codePointCount(0, flat.length()) <= excerptLength ? flat
                : flat.substring(0, flat.offsetByCodePoints(0, excerptLength)) + "…";
    }

    private static Instant instant(ResultSet rs, String column) throws SQLException {
        return rs.getTimestamp(column).toInstant();
    }
}
