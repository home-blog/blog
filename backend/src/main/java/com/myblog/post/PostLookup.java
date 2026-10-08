package com.myblog.post;

import java.util.Optional;

/**
 * "이 사람이 볼 수 있는 글인가, 누구의 글인가"를 다른 모듈(댓글·좋아요·신고·이미지)에 알려 주는 입구 (specs/005 T009, FR-029).
 * 볼 수 없는 글(남의 비공개 글, 비공개 분류의 글)은 없는 글처럼 비어 있다. 부르는 쪽은 없는 글과 같은 POST_NOT_FOUND로 답한다
 * (005 research B-1, 003 R-6). 보는 조건은 글 상세와 같다 (PostVisibility).
 */
public interface PostLookup {

    /** viewerId는 로그인하지 않았으면 null. */
    Optional<PostRef> findVisible(Long postId, Long viewerId);

    /** 글과 그 블로그, 블로그 주인(= 글 작성자). */
    record PostRef(Long postId, Long blogId, Long ownerId) {

        public boolean isOwnedBy(Long memberId) {
            return memberId != null && memberId.equals(ownerId);
        }
    }
}
