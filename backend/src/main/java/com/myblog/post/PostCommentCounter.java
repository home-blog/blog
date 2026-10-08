package com.myblog.post;

/**
 * 글 상세의 댓글 수를 묻는 틀 (specs/005 T010, FR-003). 글 모듈은 댓글 모듈을 부르지 못하므로(post ← comment)
 * 글 모듈이 틀만 두고 댓글 모듈이 채운다 (003의 CategoryPostCounter와 같은 방식). 채우는 쪽이 없으면 0으로 본다.
 */
public interface PostCommentCounter {

    long count(Long postId);
}
