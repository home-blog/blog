package com.myblog.editorial.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 편집실 처리 기록 (editorial_action 표, specs/018 D-4). 대상이 지워져도 그때의 제목은 남고 대상 번호만 빈다.
 * targetKind는 post(블로그 글) · board(글마당 글) · comment(댓글) · member(회원, specs/022),
 * action은 taken_down · dismissed · removed · unlocked · hidden · restored · suspended · unsuspended.
 * 댓글이면 targetTitle은 그 글의 제목, 회원이면 그때의 닉네임이다.
 */
@Entity
@Table(name = "editorial_action")
public class EditorialAction {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "editorial_action_id")
    private Long id;

    @Column(name = "target_kind", nullable = false, length = 10, updatable = false)
    private String targetKind;

    @Column(name = "post_id")
    private Long postId;

    @Column(name = "board_post_id")
    private Long boardPostId;

    @Column(name = "comment_id")
    private Long commentId;

    @Column(name = "member_id")
    private Long memberId;

    @Column(name = "target_title", nullable = false, length = 100, updatable = false)
    private String targetTitle;

    @Column(name = "action", nullable = false, length = 20, updatable = false)
    private String action;

    @Column(name = "reason", length = 200, updatable = false)
    private String reason;

    @Column(name = "report_count", nullable = false, updatable = false)
    private int reportCount;

    @Column(name = "users_id")
    private Long adminId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected EditorialAction() {
    }

    public static EditorialAction onPost(Long postId, String title, String action, String reason, int reportCount,
            Long adminId, Instant now) {
        EditorialAction record = base("post", title, action, reason, reportCount, adminId, now);
        record.postId = postId;
        return record;
    }

    /** 지운 글마당 글은 번호를 남기지 않는다(지운 뒤라 외래 키가 맞지 않는다). */
    public static EditorialAction onBoard(Long boardPostId, String title, String action, String reason, int reportCount,
            Long adminId, Instant now) {
        EditorialAction record = base("board", title, action, reason, reportCount, adminId, now);
        record.boardPostId = boardPostId;
        return record;
    }

    /** 댓글 가리기·반려·되돌리기 (specs/022 US1). title은 댓글이 달린 글의 제목. */
    public static EditorialAction onComment(Long commentId, String postTitle, String action, String reason,
            int reportCount, Long adminId, Instant now) {
        EditorialAction record = base("comment", postTitle, action, reason, reportCount, adminId, now);
        record.commentId = commentId;
        return record;
    }

    /** 회원 정지·풀기 (specs/022 US2). title은 그때의 닉네임. */
    public static EditorialAction onMember(Long memberId, String nickname, String action, String reason, Long adminId,
            Instant now) {
        EditorialAction record = base("member", nickname, action, reason, 0, adminId, now);
        record.memberId = memberId;
        return record;
    }

    private static EditorialAction base(String kind, String title, String action, String reason, int reportCount,
            Long adminId, Instant now) {
        EditorialAction record = new EditorialAction();
        record.targetKind = kind;
        record.targetTitle = title;
        record.action = action;
        record.reason = reason;
        record.reportCount = reportCount;
        record.adminId = adminId;
        record.createdAt = now;
        return record;
    }

    public Long getId() {
        return id;
    }

    public String getTargetKind() {
        return targetKind;
    }

    public Long getPostId() {
        return postId;
    }

    public Long getBoardPostId() {
        return boardPostId;
    }

    public Long getCommentId() {
        return commentId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getTargetTitle() {
        return targetTitle;
    }

    public String getAction() {
        return action;
    }

    public String getReason() {
        return reason;
    }

    public int getReportCount() {
        return reportCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
