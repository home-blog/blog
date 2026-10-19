package com.myblog.board;

/**
 * 글마당 글에 댓글이 달렸다 (specs/017 US3, D-9). 구독 모듈이 커밋 뒤 듣고 글 쓴 사람의 우편함에 소식을 넣는다.
 * 받는 사람·제목·보낸 이름을 담아, 듣는 쪽이 글마당 표를 다시 읽지 않게 한다. 글 쓴 사람이 단 댓글이면 내지 않는다.
 */
public record BoardCommentWrittenEvent(Long boardCommentId, Long boardPostId, Long postWriterId, String postTitle,
        String commenterName) {
}
