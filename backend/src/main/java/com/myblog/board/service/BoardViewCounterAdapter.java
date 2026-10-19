package com.myblog.board.service;

import com.myblog.board.BoardViewCounter;
import com.myblog.board.repository.BoardPostRepository;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** BoardViewCounter를 글마당 표로 채운다 (specs/017 D-3). */
@Component
public class BoardViewCounterAdapter implements BoardViewCounter {

    private final BoardPostRepository posts;

    public BoardViewCounterAdapter(BoardPostRepository posts) {
        this.posts = posts;
    }

    @Override
    @Transactional
    public void increment(Long boardPostId) {
        posts.incrementViews(boardPostId);
    }
}
