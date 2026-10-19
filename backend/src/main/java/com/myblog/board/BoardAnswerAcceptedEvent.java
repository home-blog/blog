package com.myblog.board;

/**
 * 물음을 쓴 사람이 댓글을 채택했다 (specs/017 US4, D-9). 구독 모듈이 커밋 뒤 듣고 댓글 쓴 사람의 우편함에 "채택" 소식을 넣는다.
 * 채택을 거두거나 다시 같은 댓글을 고르면 내지 않는다.
 */
public record BoardAnswerAcceptedEvent(Long boardPostId, Long commentWriterId, String postTitle, String accepterName) {
}
