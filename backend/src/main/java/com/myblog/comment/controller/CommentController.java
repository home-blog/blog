package com.myblog.comment.controller;

import com.myblog.comment.controller.dto.CommentRequests;
import com.myblog.comment.service.CommentService;
import com.myblog.comment.service.CommentService.CommentList;
import com.myblog.comment.service.CommentService.CommentView;
import com.myblog.user.LoggedInMember;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 댓글 (specs/005 contracts 1, 2). 회원 번호는 LoggedInMember로만 얻는다 (주소·본문에서 받지 않음).
 * 목록은 누구나 부른다 (SecurityConfig). <b>댓글 수정 주소는 없다</b> (FR-005).
 */
@RestController
public class CommentController {

    private final LoggedInMember loggedInMember;
    private final CommentService commentService;

    public CommentController(LoggedInMember loggedInMember, CommentService commentService) {
        this.loggedInMember = loggedInMember;
        this.commentService = commentService;
    }

    @GetMapping("/api/posts/{postId}/comments")
    public CommentList list(@PathVariable Long postId, Authentication authentication) {
        return commentService.list(postId, loggedInMember.idOf(authentication).orElse(null));
    }

    @PostMapping("/api/posts/{postId}/comments")
    @ResponseStatus(HttpStatus.CREATED)
    public CommentView write(@PathVariable Long postId, Authentication authentication,
            @RequestBody CommentRequests.Write request) {
        return commentService.write(loggedInMember.requireIdOf(authentication), postId, request);
    }
}
