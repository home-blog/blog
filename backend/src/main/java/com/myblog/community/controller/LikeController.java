package com.myblog.community.controller;

import com.myblog.community.service.LikeService;
import com.myblog.post.PostLikeSummary.LikeSummary;
import com.myblog.user.LoggedInMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

/** 글 좋아요 (specs/005 contracts 4, 5). 둘 다 200 { likeCount, likedByMe }. 회원 번호는 LoggedInMember로만 얻는다. */
@Tag(name = "7. 댓글·좋아요·신고")
@RestController
public class LikeController {

    private final LoggedInMember loggedInMember;
    private final LikeService likeService;

    public LikeController(LoggedInMember loggedInMember, LikeService likeService) {
        this.loggedInMember = loggedInMember;
        this.likeService = likeService;
    }

    @Operation(summary = "좋아요", description = "여러 번 눌러도 한 번으로 센다.")
    @PutMapping("/api/{version}/posts/{postId}/like")
    public LikeSummary like(@PathVariable Long postId, Authentication authentication) {
        return likeService.like(loggedInMember.requireIdOf(authentication), postId);
    }

    @Operation(summary = "좋아요 취소")
    @DeleteMapping("/api/{version}/posts/{postId}/like")
    public LikeSummary unlike(@PathVariable Long postId, Authentication authentication) {
        return likeService.unlike(loggedInMember.requireIdOf(authentication), postId);
    }
}
