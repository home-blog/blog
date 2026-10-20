package com.myblog.comment;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 블로그 글 댓글 신고를 편집실에 알려 주고 처리하는 입구 (specs/022 D-1). 편집실 모듈이 부르고 댓글 모듈이 자기 표에 쓴다.
 * 검토 대기 = 아직 닫지 않은 신고(resolved_at 없음). 신고한 사람은 알려 주지 않는다.
 */
public interface CommentReports {

    /** 대기 신고가 있는 댓글 수. */
    long pendingCommentCount();

    /** 대기 신고 수 (운영 현황). */
    long pendingReportCount();

    /** 대기 신고가 있는 댓글, 신고가 많은 댓글 먼저, 같으면 첫 신고가 오래된 댓글 먼저. */
    List<PendingComment> pendingComments(long offset, int limit);

    /** 한 댓글의 대기 신고, 오래된 순. */
    List<CommentReportLine> pendingOf(Long commentId);

    /** 댓글 줄을 잠근다(편집실 처리끼리, 그리고 신고와 차례로). 없는 댓글이면 거짓. */
    boolean lock(Long commentId);

    /** 댓글 하나(편집실은 내용을 본다). 없거나 지운 칸이면 비운다. */
    Optional<CommentBrief> brief(Long commentId);

    /** 대기 신고를 모두 닫는다. resolution은 hidden(가림)·dismissed(반려). 닫은 수를 돌려준다. */
    int resolve(Long commentId, String resolution, Instant now);

    /** 관리자가 가린다. 이미 가려져 있으면 그대로(저절로 가린 것도 그대로 둔다). 이번에 가렸으면 참. */
    boolean hide(Long commentId, Instant now);

    /** 가림을 거둔다. 가려져 있지 않았으면 거짓. */
    boolean restore(Long commentId);

    record PendingComment(Long commentId, long reportCount, long spam, long abuse, long adult, long other,
            Instant firstAt, Instant lastAt) {
    }

    /** reason은 SPAM·ABUSE·ADULT·OTHER, detail은 OTHER일 때만 있을 수 있다. */
    record CommentReportLine(String reason, String detail, Instant createdAt) {
    }

    /** hiddenBy는 auto·editor (가려져 있지 않으면 null). */
    record CommentBrief(Long commentId, Long postId, Long writerId, String body, String excerpt, boolean hidden,
            String hiddenBy) {
    }
}
