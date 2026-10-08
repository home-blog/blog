package com.myblog.post.service;

import com.myblog.blog.BlogDirectory;
import com.myblog.blog.BlogDirectory.BlogInfo;
import com.myblog.blog.BlogDirectory.CategoryInfo;
import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.post.PostContentSavedEvent;
import com.myblog.post.domain.Post;
import com.myblog.post.repository.PostRepository;
import com.myblog.post.tag.TagNormalizer;
import java.util.List;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
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
 *   <li>태그(005)는 TagNormalizer로 다듬고 검사해 글과 <b>같은 트랜잭션</b>에서 저장한다. 규칙에 어긋나면 글도 저장하지 않는다.</li>
 *   <li>저장한 뒤 같은 트랜잭션에서 PostContentSavedEvent를 낸다: 이미지 모듈이 본문 속 이미지를 이 글에 연결한다 (005 T057).</li>
 *   <li>같은 requestKey의 글이 이미 있으면 새로 만들지 않고 그 번호를 돌려준다 (D-6). 내용(태그 포함)이 다르면 POST_ALREADY_SAVED.
 *       거의 동시에 같은 키가 오면 request_key 중복 불가로 DB가 하나만 받고, 나머지는 다시 찾아 같은 번호를 돌려준다.</li>
 * </ol>
 * 중복으로 저장이 거절되면 그 트랜잭션은 쓸 수 없으므로, 저장과 다시 찾기를 각각 따로 된 트랜잭션에서 한다.
 */
@Service
public class PostWriteService {

    private final BlogDirectory blogDirectory;
    private final PostRepository posts;
    private final PostInputChecks checks;
    private final TagNormalizer tagNormalizer;
    private final PostTagService tagService;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate transaction;

    public PostWriteService(BlogDirectory blogDirectory, PostRepository posts, PostInputChecks checks,
            TagNormalizer tagNormalizer, PostTagService tagService, ApplicationEventPublisher events,
            PlatformTransactionManager transactionManager) {
        this.blogDirectory = blogDirectory;
        this.posts = posts;
        this.checks = checks;
        this.tagNormalizer = tagNormalizer;
        this.tagService = tagService;
        this.events = events;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /** created가 거짓이면 같은 requestKey로 이미 만든 글이다. */
    public record Created(Long postId, boolean created) {
    }

    public Created create(Long memberId, Long categoryId, Long topicId, String title, String content, String visibility,
            String requestKey, List<String> rawTags) {
        BlogInfo blog = blogDirectory.myBlog(memberId).orElseThrow(() -> new ApiException(ErrorCode.BLOG_NOT_FOUND));
        List<String> tags = tagNormalizer.normalize(rawTags);
        if (requestKey != null) {
            Optional<Long> existing = transaction.execute(status -> findMine(requestKey, blog, categoryId, topicId, title, content, visibility, tags));
            if (existing.isPresent()) {
                return new Created(existing.get(), false);
            }
        }
        checks.checkCategory(categoryId, blog.blogId());
        checks.checkTopic(topicId);
        try {
            Long postId = transaction.execute(status -> {
                Post post = posts.saveAndFlush(Post.create(categoryId, topicId, title, content, visibility, requestKey));
                tagService.replaceTags(post.getId(), tags);
                events.publishEvent(new PostContentSavedEvent(post.getId(), memberId, post.getContent()));
                return post.getId();
            });
            return new Created(postId, true);
        } catch (DataIntegrityViolationException e) {
            // 같은 키가 먼저 저장됐거나, 그사이 분류가 지워졌다 (외래 키)
            if (requestKey != null) {
                Optional<Long> existing = transaction.execute(status -> findMine(requestKey, blog, categoryId, topicId, title, content, visibility, tags));
                if (existing.isPresent()) {
                    return new Created(existing.get(), false);
                }
            }
            throw PostInputChecks.invalidCategory();
        }
    }

    /**
     * 이 키의 글이 내 블로그의 것일 때만 돌려준다. 남의 글의 번호는 알려 주지 않는다.
     * 같은 키인데 내용이 다르면(저장은 됐는데 응답을 못 받아 고친 뒤 다시 보낸 경우) 처음 글을 저장된 것처럼 답하지 않고
     * POST_ALREADY_SAVED로 알린다: 비공개로 바꿔 다시 보냈는데 공개 글이 남는 일을 막는다.
     */
    private Optional<Long> findMine(String requestKey, BlogInfo blog, Long categoryId, Long topicId, String title,
            String content, String visibility, List<String> tags) {
        Optional<Post> found = posts.findByRequestKey(requestKey);
        if (found.isEmpty()) {
            return Optional.empty();
        }
        Post post = found.get();
        Optional<CategoryInfo> category = blogDirectory.category(post.getCategoryId());
        if (category.isEmpty() || !category.get().blogId().equals(blog.blogId())) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED);
        }
        if (!post.sameAs(categoryId, topicId, title, content, visibility) || !tagService.sameTags(post.getId(), tags)) {
            throw new ApiException(ErrorCode.POST_ALREADY_SAVED);
        }
        return Optional.of(post.getId());
    }
}
