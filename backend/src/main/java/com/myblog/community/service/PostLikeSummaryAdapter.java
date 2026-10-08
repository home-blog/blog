package com.myblog.community.service;

import com.myblog.post.PostLikeSummary;
import org.springframework.stereotype.Component;

/** 글 모듈의 PostLikeSummary를 채운다 → 글 상세의 likeCount, likedByMe (specs/005 T031). */
@Component
public class PostLikeSummaryAdapter implements PostLikeSummary {

    private final LikeService likeService;

    public PostLikeSummaryAdapter(LikeService likeService) {
        this.likeService = likeService;
    }

    @Override
    public LikeSummary summary(Long postId, Long viewerId) {
        return likeService.summary(postId, viewerId);
    }
}
