package com.myblog.post;

/**
 * 글 상세의 좋아요 수와 "내가 눌렀나"를 묻는 틀 (specs/005 T010, FR-011). 좋아요·신고 모듈(community)이 채운다.
 * 채우는 쪽이 없으면 0, false로 본다.
 */
public interface PostLikeSummary {

    /** viewerId는 로그인하지 않았으면 null (그때 likedByMe는 거짓). */
    LikeSummary summary(Long postId, Long viewerId);

    record LikeSummary(long likeCount, boolean likedByMe) {

        public static final LikeSummary NONE = new LikeSummary(0, false);
    }
}
