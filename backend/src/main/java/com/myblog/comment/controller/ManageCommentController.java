package com.myblog.comment.controller;

import com.myblog.blog.CommentReadMarks.ReadMark;
import com.myblog.comment.NewCommentCounter;
import com.myblog.comment.service.ManageCommentQueryService;
import com.myblog.comment.service.ManageCommentQueryService.ManageCommentPage;
import com.myblog.comment.service.ManageCommentQueryService.NewSince;
import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.user.LoggedInMember;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 블로그 관리의 댓글 관리와 새 댓글 수 (specs/006 contracts 5-1, 5-2, 6, T028, T034). 모두 로그인해야 하고, 내 블로그는 세션으로만 정한다.
 * 댓글 삭제는 005의 DELETE /api/comments/{commentId}를 그대로 쓴다 (블로그 주인은 누구의 댓글이든 지운다).
 */
@RestController
public class ManageCommentController {

    /** newSince에 실어 "한 번도 연 적 없음"(읽음 처리의 previousReadAt이 null)을 알리는 값 (contracts 5-2, 가안). */
    static final String NEVER_READ = "never";

    private final LoggedInMember loggedInMember;
    private final ManageCommentQueryService queryService;
    private final NewCommentCounter newCommentCounter;

    public ManageCommentController(LoggedInMember loggedInMember, ManageCommentQueryService queryService,
            NewCommentCounter newCommentCounter) {
        this.loggedInMember = loggedInMember;
        this.queryService = queryService;
        this.newCommentCounter = newCommentCounter;
    }

    /** 읽음 처리 (contracts 5-1). 댓글 관리를 열 때 목록보다 먼저 부른다. */
    @PostMapping("/api/manage/comments/read")
    public ReadResponse markRead(Authentication authentication) {
        ReadMark mark = queryService.markRead(loggedInMember.requireIdOf(authentication));
        return new ReadResponse(mark.previous(), mark.readAt());
    }

    /**
     * 내 블로그 글의 모든 댓글 (contracts 5-2). newSince: 없으면 NEW 없음, "never"면 남이 쓴 모든 댓글이 NEW,
     * 시각(ISO 8601, 시간대 포함)이면 그보다 늦게 남이 쓴 댓글이 NEW. 형식이 틀리면 400.
     */
    @GetMapping("/api/manage/comments")
    public ManageCommentPage list(Authentication authentication,
            @RequestParam(defaultValue = "1") @Min(1) @Max(Integer.MAX_VALUE / 100) int page,
            @RequestParam(required = false) String newSince) {
        return queryService.list(loggedInMember.requireIdOf(authentication), page, parse(newSince));
    }

    /** 새 댓글 수 (contracts 6). 메뉴 옆·사용자 메뉴·대시보드와 같은 계산. */
    @GetMapping("/api/manage/comments/new-count")
    public CountResponse newCount(Authentication authentication) {
        return new CountResponse(newCommentCounter.countFor(loggedInMember.requireIdOf(authentication)));
    }

    private static NewSince parse(String value) {
        if (value == null) {
            return NewSince.NONE;
        }
        if (NEVER_READ.equals(value)) {
            return NewSince.NEVER;
        }
        try {
            return NewSince.after(OffsetDateTime.parse(value).toInstant());
        } catch (DateTimeParseException e) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED);
        }
    }

    public record ReadResponse(Instant previousReadAt, Instant readAt) {
    }

    public record CountResponse(long count) {
    }
}
