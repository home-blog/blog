package com.myblog.post;

/**
 * 글의 누적 조회수(post.views)를 올리는 입구 (specs/006 T051, FR-037). 통계 모듈이 부르고 글 모듈이 자기 표에 쓴다.
 */
public interface PostViewCounter {

    /** views = views + 1을 DB에서 바로 한다 (읽고 쓰기가 아니라 겹쳐도 빠지지 않음, research R-3). */
    void increment(Long postId);
}
