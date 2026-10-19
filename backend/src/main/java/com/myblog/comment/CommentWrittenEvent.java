package com.myblog.comment;

/**
 * 댓글을 썼다 (specs/016 US2). 댓글 모듈이 댓글을 저장한 같은 트랜잭션 안에서 낸다.
 * 듣는 쪽(subscription)은 트랜잭션이 끝난 뒤(AFTER_COMMIT) 글 주인의 우편함에 소식을 넣는다. 댓글 쓰기는 그 결과를 기다리지 않는다.
 *
 * @param parentWriterId 답글이면 원댓글 쓴 사람 (specs/019 D-4), 아니면 null
 */
public record CommentWrittenEvent(Long commentId, Long postId, Long writerId, Long parentWriterId) {
}
