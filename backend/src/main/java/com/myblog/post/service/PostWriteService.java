package com.myblog.post.service;

import com.myblog.blog.BlogDirectory;
import com.myblog.blog.BlogDirectory.BlogInfo;
import com.myblog.blog.BlogDirectory.CategoryInfo;
import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.post.domain.Post;
import com.myblog.post.repository.PostRepository;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 글 쓰기 (specs/003 contracts 10, FR-008 ~ FR-018, FR-034, FR-046).
 * <ol>
 *   <li>글이 들어갈 블로그는 세션의 회원으로 정한다. 요청에서 블로그 번호를 받지 않는다.</li>
 *   <li>분류가 내 블로그의 것이 아니면 INVALID_CATEGORY.</li>
 *   <li>주제가 topic 표에 없으면 topicId "주제를 골라 주세요".</li>
 *   <li>같은 requestKey의 글이 이미 있으면 새로 만들지 않고 그 번호를 돌려준다 (D-6).
 *       거의 동시에 같은 키가 오면 request_key 중복 불가로 DB가 하나만 받고, 나머지는 다시 찾아 같은 번호를 돌려준다.</li>
 * </ol>
 * 중복으로 저장이 거절되면 그 트랜잭션은 쓸 수 없으므로, 저장과 다시 찾기를 각각 따로 된 트랜잭션에서 한다.
 */
@Service
public class PostWriteService {

    private final BlogDirectory blogDirectory;
    private final PostRepository posts;
    private final PostInputChecks checks;
    private final TransactionTemplate transaction;

    public PostWriteService(BlogDirectory blogDirectory, PostRepository posts, PostInputChecks checks,
            PlatformTransactionManager transactionManager) {
        this.blogDirectory = blogDirectory;
        this.posts = posts;
        this.checks = checks;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /** created가 거짓이면 같은 requestKey로 이미 만든 글이다. */
    public record Created(Long postId, boolean created) {
    }

    public Created create(Long memberId, Long categoryId, Long topicId, String title, String content, String visibility,
            String requestKey) {
        BlogInfo blog = blogDirectory.myBlog(memberId).orElseThrow(() -> new ApiException(ErrorCode.BLOG_NOT_FOUND));
        if (requestKey != null) {
            Optional<Long> existing = transaction.execute(status -> findMine(requestKey, blog));
            if (existing.isPresent()) {
                return new Created(existing.get(), false);
            }
        }
        checks.checkCategory(categoryId, blog.blogId());
        checks.checkTopic(topicId);
        try {
            Long postId = transaction.execute(status ->
                    posts.saveAndFlush(Post.create(categoryId, topicId, title, content, visibility, requestKey)).getId());
            return new Created(postId, true);
        } catch (DataIntegrityViolationException e) {
            // 같은 키가 먼저 저장됐거나, 그사이 분류가 지워졌다 (외래 키)
            if (requestKey != null) {
                Optional<Long> existing = transaction.execute(status -> findMine(requestKey, blog));
                if (existing.isPresent()) {
                    return new Created(existing.get(), false);
                }
            }
            throw PostInputChecks.invalidCategory();
        }
    }

    /** 이 키의 글이 내 블로그의 것일 때만 돌려준다. 남의 글의 번호는 알려 주지 않는다. */
    private Optional<Long> findMine(String requestKey, BlogInfo blog) {
        Optional<Post> post = posts.findByRequestKey(requestKey);
        if (post.isEmpty()) {
            return Optional.empty();
        }
        Optional<CategoryInfo> category = blogDirectory.category(post.get().getCategoryId());
        if (category.isPresent() && category.get().blogId().equals(blog.blogId())) {
            return Optional.of(post.get().getId());
        }
        throw new ApiException(ErrorCode.VALIDATION_FAILED);
    }
}
