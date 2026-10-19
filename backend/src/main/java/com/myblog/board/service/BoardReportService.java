package com.myblog.board.service;

import com.myblog.board.config.BoardProperties;
import com.myblog.board.domain.BoardPost;
import com.myblog.board.repository.BoardPostRepository;
import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.common.error.ErrorResponse.FieldErrorItem;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 글마당 글 신고 (specs/018 US2, S2-1). 블로그 글 신고와 같은 규칙: 검사 순서는 없는 글(404) → 자기 글(403) → 입력 → 이미 신고(409).
 * 사유는 SPAM·ABUSE·ADULT·OTHER, 설명은 기타일 때만 저장한다. 접수만 하고 글은 편집실이 처리한다.
 */
@Service
public class BoardReportService {

    private static final Set<String> REASONS = Set.of("SPAM", "ABUSE", "ADULT", "OTHER");

    private final BoardPostRepository posts;
    private final JdbcTemplate jdbc;
    private final BoardProperties properties;
    private final Clock clock;

    public BoardReportService(BoardPostRepository posts, JdbcTemplate jdbc, BoardProperties properties, Clock clock) {
        this.posts = posts;
        this.jdbc = jdbc;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public void report(Long memberId, Long boardPostId, String reason, String detail) {
        BoardPost post = posts.findById(boardPostId).orElseThrow(() -> new ApiException(ErrorCode.BOARD_POST_NOT_FOUND));
        if (post.isWrittenBy(memberId)) {
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
        int inserted = jdbc.update("insert into board_report (users_id, board_post_id, reason, detail, created_at)"
                + " values (?, ?, ?, ?, ?) on conflict (users_id, board_post_id) do nothing", memberId, post.getId(),
                reason, stored, Timestamp.from(Instant.now(clock)));
        if (inserted == 0) {
            throw new ApiException(ErrorCode.ALREADY_REPORTED);
        }
    }

    private static ApiException invalid(String field, ErrorCode code, String message) {
        return new ApiException(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.message(),
                List.of(new FieldErrorItem(field, code.name(), message)));
    }
}
