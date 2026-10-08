package com.myblog.comment.service;

import com.myblog.blog.BlogDirectory;
import com.myblog.blog.CommentReadMarks;
import com.myblog.comment.NewCommentCounter;
import com.myblog.comment.repository.CommentRepository;
import com.myblog.post.PostSummaryQuery;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 새 댓글 수 (specs/006 T033). 내 블로그 글 번호는 글 모듈에, 읽음 시각은 블로그 모듈에 묻는다. */
@Service
@Transactional(readOnly = true)
public class NewCommentCounterService implements NewCommentCounter {

    private final BlogDirectory blogDirectory;
    private final CommentReadMarks readMarks;
    private final PostSummaryQuery postSummary;
    private final CommentRepository comments;

    public NewCommentCounterService(BlogDirectory blogDirectory, CommentReadMarks readMarks, PostSummaryQuery postSummary,
            CommentRepository comments) {
        this.blogDirectory = blogDirectory;
        this.readMarks = readMarks;
        this.postSummary = postSummary;
        this.comments = comments;
    }

    @Override
    public long countFor(Long memberId) {
        return blogDirectory.myBlog(memberId).map(blog -> {
            List<Long> postIds = postSummary.postIdsOf(blog.blogId());
            if (postIds.isEmpty()) {
                return 0L;
            }
            Optional<Instant> readAt = readMarks.readAt(blog.blogId());
            return readAt.isPresent()
                    ? comments.countNewSince(postIds, blog.ownerId(), readAt.get())
                    : comments.countNotBy(postIds, blog.ownerId());
        }).orElse(0L);
    }
}
