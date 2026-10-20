package com.myblog.comment.controller;

import com.myblog.comment.controller.dto.CommentRequests;
import com.myblog.comment.service.CommentReportService;
import com.myblog.comment.service.CommentService;
import com.myblog.comment.service.CommentService.CommentList;
import com.myblog.comment.service.CommentService.CommentView;
import com.myblog.user.LoggedInMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 댓글 (specs/005 contracts 1 ~ 3). 회원 번호는 LoggedInMember로만 얻는다 (주소·본문에서 받지 않음).
 * 목록은 누구나 부른다 (SecurityConfig). <b>댓글 수정 주소는 없다</b> (FR-005).
 */
@Tag(name = "7. 댓글·좋아요·신고")
@RestController
public class CommentController {

    private final LoggedInMember loggedInMember;
    private final CommentService commentService;
    private final CommentReportService reportService;

    public CommentController(LoggedInMember loggedInMember, CommentService commentService,
            CommentReportService reportService) {
        this.loggedInMember = loggedInMember;
        this.commentService = commentService;
        this.reportService = reportService;
    }

    @Operation(summary = "댓글 목록", description = "누구나 부른다.")
    @GetMapping("/api/{version}/posts/{postId}/comments")
    public CommentList list(@PathVariable Long postId, Authentication authentication) {
        return commentService.list(postId, loggedInMember.idOf(authentication).orElse(null));
    }

    @Operation(summary = "댓글 쓰기", description = "댓글 수정은 없다(지우고 다시 쓴다).")
    @PostMapping("/api/{version}/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentView write(@PathVariable Long postId, Authentication authentication,
            @RequestBody CommentRequests.Write request) {
        return commentService.write(loggedInMember.requireIdOf(authentication), postId, request);
    }

    /** 댓글 삭제 (contracts 3). 작성자 또는 그 글의 블로그 주인만. 아니면 없는 댓글과 같은 404. */
    @Operation(summary = "댓글 삭제", description = "쓴 사람 또는 그 블로그 주인만.")
    @DeleteMapping("/api/{version}/comments/{commentId}")
    public ResponseEntity<Void> delete(@PathVariable Long commentId, Authentication authentication) {
        commentService.delete(loggedInMember.requireIdOf(authentication), commentId);
        return ResponseEntity.noContent().build();
    }

    /** 댓글 신고 (specs/022 contracts 1). 서로 다른 회원의 대기 신고가 기준에 닿으면 저절로 가린다(hidden). */
    @Operation(summary = "댓글 신고", description = "로그인, 읽을 수 있는 남의 댓글에 한 번. 대기 신고가 "
            + "`moderation.comment.auto-hide-count`건 모이면 저절로 가린다(hidden: true).")
    @PostMapping("/api/{version}/comments/{commentId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public ReportReceived report(@PathVariable Long commentId, Authentication authentication,
            @RequestBody(required = false) ReportInput input) {
        Long memberId = loggedInMember.requireIdOf(authentication);
        boolean hidden = reportService.report(memberId, commentId, input == null ? null : input.reason(),
                input == null ? null : input.detail());
        return new ReportReceived("신고가 접수되었습니다", hidden);
    }

    public record ReportInput(String reason, String detail) {
    }

    public record ReportReceived(String message, boolean hidden) {
    }
}
