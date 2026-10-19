package com.myblog.board;

/** 글마당 글의 조회 수(board_post.views)를 올리는 입구 (specs/017 D-3). 통계 모듈이 부르고 글마당 모듈이 자기 표에 쓴다. */
public interface BoardViewCounter {

    /** views = views + 1을 DB에서 바로 한다. 없는 글이면 아무 일도 없다. */
    void increment(Long boardPostId);
}
