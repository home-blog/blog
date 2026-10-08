package com.myblog.blog.service;

import com.myblog.blog.CommentReadMarks;
import com.myblog.blog.domain.Blog;
import com.myblog.blog.repository.BlogRepository;
import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** CommentReadMarks를 blog 표의 comments_read_at과 블로그 줄 잠금으로 채운다 (specs/006 T032). */
@Component
public class CommentReadMarksAdapter implements CommentReadMarks {

    private final BlogRepository blogs;
    private final Clock clock;

    public CommentReadMarksAdapter(BlogRepository blogs, Clock clock) {
        this.blogs = blogs;
        this.clock = clock;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Instant> readAt(Long blogId) {
        return blogs.findById(blogId).map(Blog::getCommentsReadAt);
    }

    /** 블로그 줄을 쓰기 잠금으로 잡고 지금 시각을 적는다. 잠금 안에서 시각을 정하므로 두 요청이 겹쳐도 뒤로 가지 않는다. */
    @Override
    @Transactional
    public ReadMark markRead(Long blogId) {
        Blog blog = blogs.findForUpdateById(blogId).orElseThrow(() -> new ApiException(ErrorCode.BLOG_NOT_FOUND));
        Instant previous = blog.getCommentsReadAt();
        Instant readAt = blog.markCommentsRead(Instant.now(clock));
        blogs.flush();
        return new ReadMark(previous, readAt);
    }

    @Override
    @Transactional
    public void holdForComment(Long blogId) {
        blogs.lockForShareById(blogId);
    }
}
