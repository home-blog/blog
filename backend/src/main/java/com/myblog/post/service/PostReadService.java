package com.myblog.post.service;

import com.myblog.blog.BlogDirectory;
import com.myblog.blog.BlogDirectory.CategoryInfo;
import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.post.PostCommentCounter;
import com.myblog.post.PostLikeSummary;
import com.myblog.post.PostLikeSummary.LikeSummary;
import com.myblog.post.domain.Post;
import com.myblog.post.domain.Topic;
import com.myblog.post.repository.PostRepository;
import com.myblog.post.repository.TopicRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 글 상세 (specs/003 contracts 11, FR-023 ~ FR-027, FR-031, FR-048).
 * <ul>
 *   <li>볼 수 없는 글(비공개 글, 비공개 분류의 글을 주인이 아닌 사람이 볼 때)은 없는 글과 똑같은 POST_NOT_FOUND (R-6).</li>
 *   <li>이전·다음 글은 같은 블로그의 공개 분류의 공개 글 중 (작성 시각, 글 번호) 바로 앞·뒤. 주인이 봐도 공개 글만 (research B-5).</li>
 *   <li>블로그·분류 이름은 요청마다 표에서 읽는다 (FR-038).</li>
 *   <li>댓글 수·좋아요 수는 위 모듈(comment, community)이 채우는 틀로 묻는다 (005 T010). 채우는 쪽이 없으면 0.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class PostReadService {

    private static final PageRequest FIRST = PageRequest.of(0, 1);

    private final PostRepository posts;
    private final TopicRepository topics;
    private final BlogDirectory blogDirectory;
    private final PostVisibility visibility;
    private final ObjectProvider<PostCommentCounter> commentCounter;
    private final ObjectProvider<PostLikeSummary> likeSummary;

    public PostReadService(PostRepository posts, TopicRepository topics, BlogDirectory blogDirectory,
            PostVisibility visibility, ObjectProvider<PostCommentCounter> commentCounter,
            ObjectProvider<PostLikeSummary> likeSummary) {
        this.posts = posts;
        this.topics = topics;
        this.blogDirectory = blogDirectory;
        this.visibility = visibility;
        this.commentCounter = commentCounter;
        this.likeSummary = likeSummary;
    }

    /** viewerId는 로그인하지 않았으면 null. */
    public PostDetail read(Long postId, Long viewerId) {
        Post post = posts.findById(postId).orElseThrow(PostReadService::notFound);
        CategoryInfo category = blogDirectory.category(post.getCategoryId()).orElseThrow(PostReadService::notFound);
        if (!visibility.canSee(post.getVisibility(), category, viewerId)) {
            throw notFound();
        }
        Topic topic = topics.findById(post.getTopicId()).orElseThrow(PostReadService::notFound);
        List<Long> openCategoryIds = blogDirectory.publicCategoryIds(category.blogId());
        Long prev = null;
        Long next = null;
        if (!openCategoryIds.isEmpty()) {
            prev = first(posts.findPreviousIds(openCategoryIds, post.getCreatedAt(), post.getId(), FIRST));
            next = first(posts.findNextIds(openCategoryIds, post.getCreatedAt(), post.getId(), FIRST));
        }
        long commentCount = commentCounter.getIfAvailable(() -> id -> 0L).count(post.getId());
        LikeSummary likes = likeSummary.getIfAvailable(() -> (id, viewer) -> LikeSummary.NONE).summary(post.getId(), viewerId);
        return new PostDetail(post.getId(), category.blogId(), category.blogName(),
                new CategoryRef(category.categoryId(), category.name(), category.visibility()),
                new TopicRef(topic.getId(), topic.getName()),
                post.getTitle(), post.getContent(), post.getVisibility(), post.getCreatedAt(), post.getUpdatedAt(),
                prev, next, visibility.isOwner(category, viewerId), commentCount, likes.likeCount(), likes.likedByMe());
    }

    /** 없는 글과 볼 수 없는 글은 상태 코드·본문이 같아야 한다 (FR-026). */
    static ApiException notFound() {
        return new ApiException(ErrorCode.POST_NOT_FOUND);
    }

    private static Long first(List<Long> ids) {
        return ids.isEmpty() ? null : ids.get(0);
    }

    public record PostDetail(Long postId, Long blogId, String blogName, CategoryRef category, TopicRef topic, String title,
            String content, String visibility, Instant createdAt, Instant updatedAt, Long prevPostId, Long nextPostId,
            boolean isOwner, long commentCount, long likeCount, boolean likedByMe) {
    }

    public record CategoryRef(Long categoryId, String name, String visibility) {
    }

    public record TopicRef(Long topicId, String name) {
    }
}
