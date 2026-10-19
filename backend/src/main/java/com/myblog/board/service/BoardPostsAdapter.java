package com.myblog.board.service;

import com.myblog.board.BoardPosts;
import com.myblog.board.repository.BoardPostRepository;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** BoardPosts를 글마당 표로 채운다 (specs/017 D-9). */
@Component
public class BoardPostsAdapter implements BoardPosts {

    private final BoardPostRepository posts;

    public BoardPostsAdapter(BoardPostRepository posts) {
        this.posts = posts;
    }

    @Override
    @Transactional(readOnly = true)
    public Set<Long> existing(Collection<Long> boardPostIds) {
        if (boardPostIds.isEmpty()) {
            return Set.of();
        }
        return new HashSet<>(posts.findExistingIds(boardPostIds));
    }
}
