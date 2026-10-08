package com.myblog.post.service;

import com.myblog.blog.BlogDirectory;
import com.myblog.blog.BlogDirectory.CategoryInfo;
import com.myblog.post.PostDeletingEvent;
import com.myblog.post.controller.dto.PostRequests;
import com.myblog.post.domain.Post;
import com.myblog.post.repository.PostRepository;
import com.myblog.post.service.PostFormService.CategoryOption;
import com.myblog.post.service.PostFormService.TopicOption;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 글 수정·삭제 (specs/003 contracts 12 ~ 14, FR-019 ~ FR-022, FR-041, FR-044).
 * 모두 <b>먼저 주인을 확인</b>하고 아니면 없는 글과 같은 POST_NOT_FOUND. 그래서 수정 요청 본문은 주인 확인 뒤에 검사한다:
 * 남의 글에는 형식이 틀린 본문을 보내도 400이 아니라 404다 (contracts `요청 검사 순서`).
 */
@Service
@Transactional
public class PostEditService {

    private final PostRepository posts;
    private final BlogDirectory blogDirectory;
    private final PostInputChecks checks;
    private final PostFormService formService;
    private final Validator validator;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public PostEditService(PostRepository posts, BlogDirectory blogDirectory, PostInputChecks checks,
            PostFormService formService, Validator validator, ApplicationEventPublisher events, Clock clock) {
        this.posts = posts;
        this.blogDirectory = blogDirectory;
        this.checks = checks;
        this.formService = formService;
        this.validator = validator;
        this.events = events;
        this.clock = clock;
    }

    /** 수정 화면을 채울 값 (contracts 12). 남의 글이면 공개 글이어도 404. */
    @Transactional(readOnly = true)
    public EditView editView(Long memberId, Long postId) {
        Owned owned = findMine(memberId, postId);
        Post post = owned.post();
        return new EditView(post.getId(), post.getTitle(), post.getContent(), post.getCategoryId(), post.getTopicId(),
                post.getVisibility(), formService.categoryOptions(owned.category().blogId()), formService.topicOptions());
    }

    /** 글 수정 (contracts 13). 바뀐 것이 없으면 아무것도 저장하지 않고 원래 수정 시각을 돌려준다 (SC-009). */
    public Updated update(Long memberId, Long postId, PostRequests.Update request) {
        Owned owned = findMine(memberId, postId);
        Set<ConstraintViolation<PostRequests.Update>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        checks.checkCategory(request.categoryId(), owned.category().blogId());
        checks.checkTopic(request.topicId());
        Post post = owned.post();
        boolean changed = post.update(request.categoryId(), request.topicId(), request.title(), request.content(),
                request.visibility(), Instant.now(clock));
        if (changed) {
            posts.flush();
        }
        return new Updated(post.getId(), changed, post.getUpdatedAt());
    }

    /**
     * 글 삭제 (contracts 14). 딸린 것(댓글·좋아요·태그 연결·이미지 기록·신고)을 가진 모듈이 PostDeletingEvent를 듣고
     * 같은 트랜잭션에서 먼저 지운 뒤 글을 지운다. 하나라도 실패하면 모두 취소된다 (FR-022, SC-007).
     */
    public void delete(Long memberId, Long postId) {
        Post post = findMine(memberId, postId).post();
        events.publishEvent(new PostDeletingEvent(post.getId()));
        posts.delete(post);
        posts.flush();
    }

    private Owned findMine(Long memberId, Long postId) {
        Post post = posts.findById(postId).orElseThrow(PostReadService::notFound);
        CategoryInfo category = blogDirectory.category(post.getCategoryId()).orElseThrow(PostReadService::notFound);
        if (!category.ownerId().equals(memberId)) {
            throw PostReadService.notFound();
        }
        return new Owned(post, category);
    }

    private record Owned(Post post, CategoryInfo category) {
    }

    public record EditView(Long postId, String title, String content, Long categoryId, Long topicId, String visibility,
            List<CategoryOption> categories, List<TopicOption> topics) {
    }

    /** updatedAt은 바뀌지 않았으면 원래 값(수정한 적 없으면 null). */
    public record Updated(Long postId, boolean changed, Instant updatedAt) {
    }
}
