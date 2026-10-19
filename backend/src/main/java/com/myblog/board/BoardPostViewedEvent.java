package com.myblog.board;

/**
 * 글마당 글을 읽었다 (specs/017 S1-2, D-3). 통계 모듈이 듣고 같은 사람이 30분 안에 다시 본 것은 세지 않는다.
 * 읽기 트랜잭션이 끝난 뒤(컨트롤러에서) 낸다. viewerId는 로그인하지 않았으면 null, byWriter는 쓴 사람이 자기 글을 봤는가.
 */
public record BoardPostViewedEvent(Long boardPostId, Long viewerId, boolean byWriter) {
}
