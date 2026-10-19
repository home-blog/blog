package com.myblog.post;

import java.time.Instant;
import java.util.Optional;

/**
 * 편집실이 글을 살펴보고 내리는 입구 (specs/018 D-1, D-2). 편집실 모듈이 부르고 글 모듈이 자기 표에 쓴다.
 * 내림 = 비공개로 돌리고 잠근다. 잠긴 글은 주인이 다시 공개로 바꿀 수 없다(고치기는 된다). 글을 지우지 않는다.
 */
public interface PostModeration {

    /** 공개 범위와 상관없이 글 하나 (편집실은 신고된 비공개 글도 본다). 없으면 비운다. */
    Optional<PostBrief> brief(Long postId);

    /** 비공개로 돌리고 잠근다. 이미 잠겼으면 사유만 바꾼다. */
    void lock(Long postId, String reason, Instant now);

    /** 잠금을 푼다. 공개로 되돌리지는 않는다(주인이 고른다). 잠겨 있지 않았으면 거짓. */
    boolean unlock(Long postId);

    /** excerpt는 마크다운을 걷어 낸 앞부분, locked는 지금 잠겨 있는가. */
    record PostBrief(Long postId, Long blogId, Long ownerId, String blogName, String title, String excerpt,
            String visibility, boolean locked, String lockReason) {
    }
}
