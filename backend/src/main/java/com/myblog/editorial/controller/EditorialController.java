package com.myblog.editorial.controller;

import com.myblog.editorial.service.EditorialService;
import com.myblog.editorial.service.EditorialService.ActionRow;
import com.myblog.editorial.service.EditorialService.BoardReview;
import com.myblog.editorial.service.EditorialService.Pending;
import com.myblog.editorial.service.EditorialService.PendingBoardRow;
import com.myblog.editorial.service.EditorialService.PendingPostRow;
import com.myblog.editorial.service.EditorialService.PostReview;
import com.myblog.editorial.service.EditorialService.Summary;
import com.myblog.user.LoggedInMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * 편집실 (specs/018 contracts). 모든 주소가 관리자만: 로그인하지 않았으면 401, 관리자가 아니면 403 (S1-1).
 * 관리자 확인을 다른 검사보다 먼저 한다: 관리자가 아닌 사람에게는 글이 있는지도 알리지 않는다.
 */
@Tag(name = "16. 편집실")
@RestController
@RequestMapping("/api/{version}/editorial")
public class EditorialController {

    private final LoggedInMember loggedInMember;
    private final EditorialService editorialService;

    public EditorialController(LoggedInMember loggedInMember, EditorialService editorialService) {
        this.loggedInMember = loggedInMember;
        this.editorialService = editorialService;
    }

    @Operation(summary = "운영 현황", description = "회원(탈퇴 제외)·오늘 가입·공개 글·오늘 공개 글·글마당 글·검토 대기 신고 수. 오늘은 `editorial.day-zone` 날짜.")
    @GetMapping("/summary")
    public Summary summary(@Parameter(hidden = true) Authentication authentication) {
        loggedInMember.requireAdminIdOf(authentication);
        return editorialService.summary();
    }

    @Operation(summary = "블로그 글 검토 대기", description = "대기 신고가 있는 글, 신고가 많은 글 먼저. 한 쪽은 `editorial.page-size`.")
    @GetMapping("/reports/posts")
    public Pending<PendingPostRow> pendingPosts(@RequestParam(required = false) String page,
            @Parameter(hidden = true) Authentication authentication) {
        loggedInMember.requireAdminIdOf(authentication);
        return editorialService.pendingPosts(page);
    }

    @Operation(summary = "블로그 글 신고 자세히", description = "대기 신고(사유·설명·시각, 신고한 사람은 없음)와 글 앞부분.")
    @GetMapping("/reports/posts/{postId:\\d+}")
    public PostReview reviewPost(@PathVariable Long postId, @Parameter(hidden = true) Authentication authentication) {
        loggedInMember.requireAdminIdOf(authentication);
        return editorialService.reviewPost(postId);
    }

    @Operation(summary = "내림", description = "대기 신고를 모두 닫고 글을 비공개로 돌리고 잠근다. 주인 우편함에 사유와 함께 소식. 닫을 신고가 없으면 409.")
    @PostMapping("/posts/{postId:\\d+}/take-down")
    public ResponseEntity<Void> takeDown(@PathVariable Long postId, @RequestBody ReasonInput input,
            @Parameter(hidden = true) Authentication authentication) {
        Long adminId = loggedInMember.requireAdminIdOf(authentication);
        editorialService.takeDown(adminId, postId, input == null ? null : input.reason());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "반려", description = "대기 신고를 모두 닫는다. 글은 그대로. 닫을 신고가 없으면 409.")
    @PostMapping("/posts/{postId:\\d+}/dismiss")
    public ResponseEntity<Void> dismissPost(@PathVariable Long postId,
            @Parameter(hidden = true) Authentication authentication) {
        editorialService.dismissPost(loggedInMember.requireAdminIdOf(authentication), postId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "잠금 풀기", description = "내린 글을 주인이 다시 공개할 수 있게 한다. 공개로 되돌리지는 않는다.")
    @PostMapping("/posts/{postId:\\d+}/unlock")
    public ResponseEntity<Void> unlock(@PathVariable Long postId,
            @Parameter(hidden = true) Authentication authentication) {
        editorialService.unlock(loggedInMember.requireAdminIdOf(authentication), postId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "글마당 검토 대기", description = "대기 신고가 있는 글마당 글, 신고가 많은 글 먼저.")
    @GetMapping("/reports/board")
    public Pending<PendingBoardRow> pendingBoard(@RequestParam(required = false) String page,
            @Parameter(hidden = true) Authentication authentication) {
        loggedInMember.requireAdminIdOf(authentication);
        return editorialService.pendingBoardPosts(page);
    }

    @Operation(summary = "글마당 신고 자세히")
    @GetMapping("/reports/board/{boardPostId:\\d+}")
    public BoardReview reviewBoard(@PathVariable Long boardPostId,
            @Parameter(hidden = true) Authentication authentication) {
        loggedInMember.requireAdminIdOf(authentication);
        return editorialService.reviewBoardPost(boardPostId);
    }

    @Operation(summary = "글마당 글 지우기", description = "대기 신고가 있는 글을 댓글과 함께 지운다. 쓴 사람 우편함에 사유와 함께 소식.")
    @PostMapping("/board/{boardPostId:\\d+}/remove")
    public ResponseEntity<Void> removeBoard(@PathVariable Long boardPostId, @RequestBody ReasonInput input,
            @Parameter(hidden = true) Authentication authentication) {
        Long adminId = loggedInMember.requireAdminIdOf(authentication);
        editorialService.removeBoardPost(adminId, boardPostId, input == null ? null : input.reason());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "글마당 신고 반려")
    @PostMapping("/board/{boardPostId:\\d+}/dismiss")
    public ResponseEntity<Void> dismissBoard(@PathVariable Long boardPostId,
            @Parameter(hidden = true) Authentication authentication) {
        editorialService.dismissBoardPost(loggedInMember.requireAdminIdOf(authentication), boardPostId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "처리 기록", description = "내림·반려·지우기·잠금 풀기, 최근 순.")
    @GetMapping("/actions")
    public Pending<ActionRow> history(@RequestParam(required = false) String page,
            @Parameter(hidden = true) Authentication authentication) {
        loggedInMember.requireAdminIdOf(authentication);
        return editorialService.history(page);
    }

    public record ReasonInput(String reason) {
    }
}
