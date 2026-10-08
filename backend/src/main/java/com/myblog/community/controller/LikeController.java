package com.myblog.community.controller;

import com.myblog.community.service.LikeService;
import com.myblog.post.PostLikeSummary.LikeSummary;
import com.myblog.user.LoggedInMember;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RestController;

/** 글 좋아요 (specs/005 contracts 4, 5). 둘 다 200 { likeCount, likedByMe }. 회원 번호는 LoggedInMember로만 얻는다. */
@RestController
public class LikeController {

    private final LoggedInMember loggedInMember;
    private final LikeService likeService;

    public LikeController(LoggedInMember loggedInMember, LikeService likeService) {
        this.loggedInMember = loggedInMember;
        this.likeService = likeService;
    }

    @PutMapping("/api/posts/{postId}/like")
    public LikeSummary like(@PathVariable Long postId, Authentication authentication) {
        return likeService.like(loggedInMember.requireIdOf(authentication), postId);
    }

    @DeleteMapping("/api/posts/{postId}/like")
    public LikeSummary unlike(@PathVariable Long postId, Authentication authentication) {
        return likeService.unlike(loggedInMember.requireIdOf(authentication), postId);
    }
}
