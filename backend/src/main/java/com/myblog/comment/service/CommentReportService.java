package com.myblog.comment.service;

import com.myblog.comment.config.CommentModerationProperties;
import com.myblog.comment.domain.Comment;
import com.myblog.comment.repository.CommentRepository;
import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.common.error.ErrorResponse.FieldErrorItem;
import com.myblog.post.PostLookup;
import com.myblog.post.PostLookup.PostRef;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 댓글 신고와 자동 가림 (specs/022 US1, S1-1 ~ S1-2, D-2). 글 신고와 같은 검사 순서: 없는·볼 수 없는 댓글(404) → 내 댓글(403)
 * → 입력 → 이미 신고(409). 사유는 SPAM·ABUSE·ADULT·OTHER, 설명은 기타일 때만 저장한다.
 * 신고 대상 댓글 줄을 FOR UPDATE로 잠그고 넣은 뒤 대기 신고를 센다: 거의 동시에 온 신고도 한 줄로 서서, 기준에 닿은 한 번만 가린다.
 * 가린 뒤에 줄을 선 신고는 가린 댓글이라 404다.
 */
@Service
public class CommentReportService {

    private static final Set<String> REASONS = Set.of("SPAM", "ABUSE", "ADULT", "OTHER");

    private final CommentRepository comments;
    private final PostLookup postLookup;
    private final JdbcTemplate jdbc;
    private final CommentModerationProperties properties;
    private final Clock clock;

    public CommentReportService(CommentRepository comments, PostLookup postLookup, JdbcTemplate jdbc,
            CommentModerationProperties properties, Clock clock) {
        this.comments = comments;
        this.postLookup = postLookup;
        this.jdbc = jdbc;
        this.properties = properties;
        this.clock = clock;
    }

    /** @return 이 신고로 저절로 가려졌는가 */
    @Transactional
    public boolean report(Long memberId, Long commentId, String reason, String detail) {
        // 잠근 뒤 처음 읽는다: 기다리는 사이 가려졌거나 지워졌으면 그대로 보인다
        comments.lockForUpdate(commentId).orElseThrow(CommentReportService::notFound);
        Comment comment = comments.findById(commentId).filter(c -> !c.isDeleted() && !c.isHidden())
                .orElseThrow(CommentReportService::notFound);
        PostRef post = postLookup.findVisible(comment.getPostId(), memberId).orElseThrow(CommentReportService::notFound);
        Long parentWriterId = comment.isReply()
                ? comments.findById(comment.getParentId()).map(Comment::getMemberId).orElse(null)
                : null;
        if (!comment.readableBy(memberId, post.isOwnedBy(memberId), parentWriterId)) {
            throw notFound();
        }
        if (memberId.equals(comment.getMemberId())) {
            throw new ApiException(ErrorCode.SELF_REPORT_NOT_ALLOWED);
        }
        if (reason == null || !REASONS.contains(reason)) {
            throw invalid("reason", ErrorCode.REPORT_REASON_REQUIRED, ErrorCode.REPORT_REASON_REQUIRED.message());
        }
        String normalized = detail == null ? "" : detail.replace("\r\n", "\n").replace('\r', '\n').strip();
        if (normalized.codePointCount(0, normalized.length()) > properties.reportDetailMaxLength()) {
            throw invalid("detail", ErrorCode.REPORT_DETAIL_TOO_LONG,
                    "※ 신고 내용은 %d자 이하로 입력해 주세요".formatted(properties.reportDetailMaxLength()));
        }
        String stored = "OTHER".equals(reason) && !normalized.isEmpty() ? normalized : null;
        Instant now = Instant.now(clock);
        int inserted = jdbc.update("insert into comment_report (users_id, comment_id, reason, detail, created_at)"
                + " values (?, ?, ?, ?, ?) on conflict (users_id, comment_id) do nothing", memberId, commentId, reason,
                stored, Timestamp.from(now));
        if (inserted == 0) {
            throw new ApiException(ErrorCode.ALREADY_REPORTED, "※ 이미 신고한 댓글입니다");
        }
        Long pending = jdbc.queryForObject(
                "select count(*) from comment_report where comment_id = ? and resolved_at is null", Long.class,
                commentId);
        if (pending == null || pending < properties.autoHideCount()) {
            return false;
        }
        return jdbc.update("update comment set hidden_at = ?, hidden_by = 'auto' where comment_id = ? and hidden_at is null",
                Timestamp.from(now), commentId) > 0;
    }

    private static ApiException notFound() {
        return new ApiException(ErrorCode.COMMENT_NOT_FOUND);
    }

    private static ApiException invalid(String field, ErrorCode code, String message) {
        return new ApiException(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.message(),
                List.of(new FieldErrorItem(field, code.name(), message)));
    }
}
