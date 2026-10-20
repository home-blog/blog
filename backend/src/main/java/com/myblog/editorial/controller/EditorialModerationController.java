package com.myblog.editorial.controller;

import com.myblog.editorial.service.EditorialService.Pending;
import com.myblog.editorial.service.ModerationDeskService;
import com.myblog.editorial.service.ModerationDeskService.CommentReview;
import com.myblog.editorial.service.ModerationDeskService.MemberPage;
import com.myblog.editorial.service.ModerationDeskService.PendingCommentRow;
import com.myblog.user.LoggedInMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 편집실 운영 강화 (specs/022 contracts 3, 4): 댓글 신고 처리와 회원 정지. 모든 주소가 관리자만(401·403),
 * 관리자 확인을 다른 검사보다 먼저 한다.
 */
@Tag(name = "16. 편집실")
@RestController
@RequestMapping("/api/{version}/editorial")
public class EditorialModerationController {

    private final LoggedInMember loggedInMember;
    private final ModerationDeskService desk;

    public EditorialModerationController(LoggedInMember loggedInMember, ModerationDeskService desk) {
        this.loggedInMember = loggedInMember;
        this.desk = desk;
    }

    @Operation(summary = "댓글 검토 대기", description = "대기 신고가 있는 댓글, 신고가 많은 댓글 먼저. hidden은 지금 가려져 있는가, "
            + "hiddenBy는 auto(신고가 모여 저절로)·editor(관리자).")
    @GetMapping("/reports/comments")
    public Pending<PendingCommentRow> pendingComments(@RequestParam(required = false) String page,
            @Parameter(hidden = true) Authentication authentication) {
        loggedInMember.requireAdminIdOf(authentication);
        return desk.pendingComments(page);
    }

    @Operation(summary = "댓글 신고 자세히", description = "댓글 내용과 대기 신고(신고한 사람은 없음).")
    @GetMapping("/reports/comments/{commentId:\\d+}")
    public CommentReview reviewComment(@PathVariable Long commentId,
            @Parameter(hidden = true) Authentication authentication) {
        loggedInMember.requireAdminIdOf(authentication);
        return desk.reviewComment(commentId);
    }

    @Operation(summary = "댓글 가리기", description = "대기 신고를 닫고 댓글을 가린다. 쓴 사람 우편함에 사유와 함께 소식. 닫을 신고가 없으면 409.")
    @PostMapping("/comments/{commentId:\\d+}/hide")
    public ResponseEntity<Void> hideComment(@PathVariable Long commentId,
            @RequestBody(required = false) EditorialController.ReasonInput input,
            @Parameter(hidden = true) Authentication authentication) {
        Long adminId = loggedInMember.requireAdminIdOf(authentication);
        desk.hideComment(adminId, commentId, input == null ? null : input.reason());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "댓글 신고 반려", description = "대기 신고를 닫는다. 저절로 가린 댓글이면 다시 보인다. 닫을 신고가 없으면 409.")
    @PostMapping("/comments/{commentId:\\d+}/dismiss")
    public ResponseEntity<Void> dismissComment(@PathVariable Long commentId,
            @Parameter(hidden = true) Authentication authentication) {
        desk.dismissComment(loggedInMember.requireAdminIdOf(authentication), commentId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "가린 댓글 되돌리기", description = "restored는 이번에 되돌렸는가(가려져 있지 않았으면 false, 기록하지 않음).")
    @PostMapping("/comments/{commentId:\\d+}/restore")
    public RestoreResponse restoreComment(@PathVariable Long commentId,
            @Parameter(hidden = true) Authentication authentication) {
        return new RestoreResponse(desk.restoreComment(loggedInMember.requireAdminIdOf(authentication), commentId));
    }

    @Operation(summary = "회원 찾기", description = "닉네임 일부로(대소문자 무시, 탈퇴 제외), 최근 가입 순. q가 비면 모든 회원. "
            + "suspendDays는 고를 수 있는 정지 기간(일).")
    @GetMapping("/members")
    public MemberPage members(@RequestParam(required = false) String q, @RequestParam(required = false) String page,
            @Parameter(hidden = true) Authentication authentication) {
        loggedInMember.requireAdminIdOf(authentication);
        return desk.searchMembers(q, page);
    }

    @Operation(summary = "회원 정지", description = "기간(일)과 사유. 그 회원의 로그인 세션을 모두 끝내고 AI 비서 열쇠를 끊는다. "
            + "다시 정지하면 기간을 새로 정한다. 관리자는 409.")
    @PostMapping("/members/{memberId:\\d+}/suspend")
    public SuspendResponse suspend(@PathVariable Long memberId, @RequestBody(required = false) SuspendInput input,
            @Parameter(hidden = true) Authentication authentication) {
        Long adminId = loggedInMember.requireAdminIdOf(authentication);
        return new SuspendResponse(desk.suspend(adminId, memberId, input == null ? null : input.days(),
                input == null ? null : input.reason()).suspendedUntil());
    }

    @Operation(summary = "정지 풀기", description = "unsuspended는 이번에 풀었는가(정지 중이 아니었으면 false, 기록하지 않음).")
    @PostMapping("/members/{memberId:\\d+}/unsuspend")
    public UnsuspendResponse unsuspend(@PathVariable Long memberId,
            @Parameter(hidden = true) Authentication authentication) {
        return new UnsuspendResponse(desk.unsuspend(loggedInMember.requireAdminIdOf(authentication), memberId));
    }

    public record RestoreResponse(boolean restored) {
    }

    public record SuspendInput(Integer days, String reason) {
    }

    public record SuspendResponse(Instant suspendedUntil) {
    }

    public record UnsuspendResponse(boolean unsuspended) {
    }
}
