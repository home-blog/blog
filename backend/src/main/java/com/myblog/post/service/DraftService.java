package com.myblog.post.service;

import com.myblog.blog.BlogClosingEvent;
import com.myblog.blog.BlogDirectory;
import com.myblog.blog.BlogDirectory.BlogInfo;
import com.myblog.common.Visibility;
import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.common.error.ErrorResponse.FieldErrorItem;
import com.myblog.post.DraftContentSavedEvent;
import com.myblog.post.config.PostProperties;
import com.myblog.post.config.WritingProperties;
import com.myblog.post.controller.dto.DraftRequests;
import com.myblog.post.domain.Post;
import com.myblog.post.domain.PostDraft;
import com.myblog.post.repository.BlogWriteLock;
import com.myblog.post.repository.PostDraftRepository;
import com.myblog.post.repository.TopicRepository;
import com.myblog.post.tag.TagNormalizer;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 임시 저장·예약 발행 (specs/013 US3, US4, contracts 3, 4).
 * <ul>
 *   <li>임시 저장 글은 주인만 본다. 남의 것은 없는 것과 같은 404 (S3-1). 공개 글 쿼리와 다른 표라 어디에도 나오지 않는다 (D-1).</li>
 *   <li>저장은 비어 있어도 된다: 글자 수 상한과 내 것인지만 본다. 발행·예약은 보통 글쓰기와 같은 검사를 한다.</li>
 *   <li>발행은 보통 글쓰기(PostWriteService)를 그대로 부르고, 임시 저장 글의 요청 번호를 쓴다: 두 번 와도 글은 하나 (D-3).</li>
 *   <li>예약 시각은 초 아래를 버리고, 지금부터 min-lead 뒤 ~ max-days 안 (S4-1). 올리기는 DraftPublisher가 한다.</li>
 * </ul>
 */
@Service
public class DraftService {

    private static final Logger log = LoggerFactory.getLogger(DraftService.class);

    private final PostDraftRepository drafts;
    private final BlogDirectory blogDirectory;
    private final PostInputChecks checks;
    private final TopicRepository topics;
    private final TagNormalizer tagNormalizer;
    private final PostWriteService writeService;
    private final SeriesService seriesService;
    private final Validator validator;
    private final ApplicationEventPublisher events;
    private final PostProperties postProperties;
    private final WritingProperties writing;
    private final TransactionTemplate transaction;
    private final BlogWriteLock writeLock;
    private final Clock clock;

    public DraftService(PostDraftRepository drafts, BlogDirectory blogDirectory, PostInputChecks checks, TopicRepository topics,
            TagNormalizer tagNormalizer, PostWriteService writeService, SeriesService seriesService, Validator validator,
            ApplicationEventPublisher events, PostProperties postProperties, WritingProperties writing,
            PlatformTransactionManager transactionManager, BlogWriteLock writeLock, Clock clock) {
        this.drafts = drafts;
        this.blogDirectory = blogDirectory;
        this.checks = checks;
        this.topics = topics;
        this.tagNormalizer = tagNormalizer;
        this.writeService = writeService;
        this.seriesService = seriesService;
        this.validator = validator;
        this.events = events;
        this.postProperties = postProperties;
        this.writing = writing;
        this.transaction = new TransactionTemplate(transactionManager);
        this.writeLock = writeLock;
        this.clock = clock;
    }

    // ── 임시 저장 (US3) ─────────────────────────────────────────────

    public DraftList list(Long memberId) {
        BlogInfo blog = myBlog(memberId);
        List<DraftItem> items = drafts.findByBlogIdOrderByUpdatedAtDescIdDesc(blog.blogId()).stream()
                .map(draft -> new DraftItem(draft.getId(), draft.getTitle(), draft.getUpdatedAt(), draft.getPublishAt()))
                .toList();
        return new DraftList(items, items.size(), writing.draft().maxCount());
    }

    public DraftView get(Long memberId, Long draftId) {
        return DraftView.of(myDraft(memberId, draftId));
    }

    public Saved create(Long memberId, DraftRequests.Save request) {
        BlogInfo blog = myBlog(memberId);
        checkSave(request, blog.blogId());
        return transaction.execute(status -> {
            writeLock.drafts(blog.blogId()); // 세기와 만들기 사이에 다른 저장이 끼지 않게
            if (drafts.countByBlogId(blog.blogId()) >= writing.draft().maxCount()) {
                throw new ApiException(ErrorCode.DRAFT_LIMIT_EXCEEDED,
                        ErrorCode.DRAFT_LIMIT_EXCEEDED.message().formatted(writing.draft().maxCount()));
            }
            PostDraft draft = PostDraft.start(blog.blogId(), Instant.now(clock));
            fill(draft, request);
            drafts.saveAndFlush(draft);
            events.publishEvent(new DraftContentSavedEvent(draft.getId(), memberId, draft.getContent()));
            return Saved.of(draft);
        });
    }

    public Saved save(Long memberId, Long draftId, DraftRequests.Save request) {
        return transaction.execute(status -> {
            PostDraft draft = myDraft(memberId, draftId);
            checkSave(request, draft.getBlogId());
            fill(draft, request);
            drafts.flush();
            events.publishEvent(new DraftContentSavedEvent(draft.getId(), memberId, draft.getContent()));
            return Saved.of(draft);
        });
    }

    public void delete(Long memberId, Long draftId) {
        transaction.executeWithoutResult(status -> drafts.deleteDraft(myDraft(memberId, draftId).getId()));
    }

    /**
     * 발행 (contracts 3-1): 보통 글쓰기 검사 → 글 만들기(임시 저장 글의 요청 번호) → 연재 → 임시 저장 글 지우기를
     * 한 트랜잭션으로 한다. 뒤 단계가 실패하면 글도 남지 않는다. 임시 저장 글 줄을 먼저 잠가, 같은 글을 함께 발행한
     * 둘째 요청은 첫째가 끝난 뒤 글이 없어 404를 받는다(예약 발행 작업은 잠긴 줄을 건너뛴다).
     */
    public PostWriteService.Created publish(Long memberId, Long draftId, DraftRequests.Publish request) {
        BlogInfo blog = myBlog(memberId);
        return transaction.execute(status -> {
            drafts.lockMine(draftId, blog.blogId()).orElseThrow(() -> new ApiException(ErrorCode.DRAFT_NOT_FOUND));
            PostDraft draft = myDraft(memberId, draftId);
            validate(request);
            PostWriteService.Created created = writeService.create(memberId, request.categoryId(), request.topicId(),
                    request.title(), request.content(), request.visibility(), draft.getRequestKey(), request.tags());
            seriesService.attachIfPossible(draft.getBlogId(), request.seriesId(), created.postId());
            drafts.deleteDraft(draft.getId());
            return created;
        });
    }

    // ── 예약 (US4) ─────────────────────────────────────────────

    public Saved schedule(Long memberId, Long draftId, DraftRequests.Schedule request) {
        PostDraft owned = transaction.execute(status -> myDraft(memberId, draftId));
        DraftRequests.Publish publish = request.toPublish();
        validate(publish);
        checks.checkCategory(publish.categoryId(), owned.getBlogId());
        checks.checkTopic(publish.topicId());
        tagNormalizer.normalize(publish.tags());
        Instant publishAt = checkPublishAt(request.publishAt());
        DraftRequests.Save save = publish.toSave();
        checkSave(save, owned.getBlogId());
        return transaction.execute(status -> {
            PostDraft draft = myDraft(memberId, draftId);
            fill(draft, save);
            draft.schedule(publishAt);
            drafts.flush();
            events.publishEvent(new DraftContentSavedEvent(draft.getId(), memberId, draft.getContent()));
            return Saved.of(draft);
        });
    }

    public Saved unschedule(Long memberId, Long draftId) {
        return transaction.execute(status -> {
            PostDraft draft = myDraft(memberId, draftId);
            draft.unschedule();
            drafts.flush();
            return Saved.of(draft);
        });
    }

    /**
     * 시각이 된 예약 하나를 올린다 (DraftPublisher가 줄을 잠근 트랜잭션 안에서 부른다).
     * 검사에 걸리면(예약 뒤 저장으로 제목을 지움, 분류가 지워짐 등) 예약을 풀고 임시 저장 글로 남긴다 (D-4).
     * 검사는 예외를 밖으로 내지 않는다: 바깥 트랜잭션을 되돌림 표시로 만들지 않기 위해서다.
     * 글 만들기에서 난 예외는 그대로 내보내고, DraftPublisher가 새 트랜잭션에서 예약을 푼다.
     */
    public ScheduledOutcome publishScheduled(Long draftId) {
        PostDraft draft = drafts.findById(draftId).orElse(null);
        if (draft == null || draft.getPublishAt() == null) {
            return ScheduledOutcome.SKIPPED;
        }
        BlogInfo blog = blogDirectory.blog(draft.getBlogId()).orElse(null);
        DraftRequests.Publish request = new DraftRequests.Publish(draft.getTitle(), draft.getContent(), draft.getCategoryId(),
                draft.getTopicId(), draft.getVisibility(), draft.tagList(), draft.getSeriesId());
        try {
            if (blog == null) {
                throw new ApiException(ErrorCode.BLOG_NOT_FOUND);
            }
            validate(request);
            checks.checkCategory(request.categoryId(), blog.blogId());
            checks.checkTopic(request.topicId());
            tagNormalizer.normalize(request.tags());
        } catch (ApiException | ConstraintViolationException e) {
            log.warn("예약 글 {}을 올리지 못해 예약을 풉니다: {}", draftId, e.getMessage());
            draft.unschedule();
            drafts.flush();
            return ScheduledOutcome.UNSCHEDULED;
        }
        PostWriteService.Created created = writeService.create(blog.ownerId(), request.categoryId(), request.topicId(),
                request.title(), request.content(), request.visibility(), draft.getRequestKey(), request.tags());
        seriesService.attachIfPossible(blog.blogId(), request.seriesId(), created.postId());
        drafts.deleteDraft(draft.getId());
        return ScheduledOutcome.PUBLISHED;
    }

    /** 블로그가 닫히면(탈퇴) 임시 저장·예약 글도 지운다. */
    @EventListener
    public void on(BlogClosingEvent event) {
        drafts.deleteByBlogId(event.blogId());
    }

    // ── 검사 ─────────────────────────────────────────────

    private void validate(DraftRequests.Publish request) {
        Set<ConstraintViolation<DraftRequests.Publish>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private Instant checkPublishAt(Instant requested) {
        if (requested == null) {
            throw field("publishAt", ErrorCode.PUBLISH_AT_REQUIRED, ErrorCode.PUBLISH_AT_REQUIRED.message());
        }
        Instant publishAt = requested.truncatedTo(ChronoUnit.MINUTES);
        Instant now = Instant.now(clock);
        if (publishAt.isBefore(now.plus(writing.schedule().minLead()))) {
            throw field("publishAt", ErrorCode.PUBLISH_AT_TOO_SOON, ErrorCode.PUBLISH_AT_TOO_SOON.message());
        }
        int maxDays = writing.schedule().maxDays();
        if (publishAt.isAfter(now.plus(maxDays, ChronoUnit.DAYS))) {
            throw field("publishAt", ErrorCode.PUBLISH_AT_TOO_LATE, ErrorCode.PUBLISH_AT_TOO_LATE.message().formatted(maxDays));
        }
        return publishAt;
    }

    /** 저장 검사: 글자 수 상한, 공개 범위, 태그 칸 길이, 분류·주제·연재가 있고 내 것인지. 비어 있는 것은 된다. */
    private void checkSave(DraftRequests.Save request, Long blogId) {
        List<FieldErrorItem> errors = new ArrayList<>();
        String title = Post.normalizeTitle(request.title());
        if (title.codePointCount(0, title.length()) > postProperties.title().maxLength()) {
            errors.add(item("title", ErrorCode.TITLE_TOO_LONG));
        }
        String content = Post.normalizeContent(request.content());
        if (content.codePointCount(0, content.length()) > postProperties.content().maxLength()) {
            errors.add(item("content", ErrorCode.CONTENT_TOO_LONG));
        }
        if (request.visibility() != null && Visibility.from(request.visibility()).isEmpty()) {
            errors.add(item("visibility", ErrorCode.VISIBILITY_INVALID));
        }
        if (PostDraft.joinTags(request.tags()).length() > writing.draft().tagsMaxLength()) {
            errors.add(item("tags", ErrorCode.DRAFT_TAGS_TOO_LONG));
        }
        if (request.categoryId() != null && blogDirectory.category(request.categoryId())
                .filter(category -> category.blogId().equals(blogId)).isEmpty()) {
            errors.add(item("categoryId", ErrorCode.INVALID_CATEGORY));
        }
        if (request.topicId() != null && !topics.existsById(request.topicId())) {
            errors.add(item("topicId", ErrorCode.TOPIC_REQUIRED));
        }
        if (request.seriesId() != null && !seriesService.isMine(blogId, request.seriesId())) {
            errors.add(item("seriesId", ErrorCode.SERIES_NOT_FOUND));
        }
        if (!errors.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.message(), errors);
        }
    }

    private void fill(PostDraft draft, DraftRequests.Save request) {
        String visibility = request.visibility() == null ? Visibility.PUBLIC.value() : request.visibility();
        draft.fill(request.categoryId(), request.topicId(), request.seriesId(), request.title(), request.content(), visibility,
                request.tags(), Instant.now(clock));
    }

    private BlogInfo myBlog(Long memberId) {
        return blogDirectory.myBlog(memberId).orElseThrow(() -> new ApiException(ErrorCode.BLOG_NOT_FOUND));
    }

    private PostDraft myDraft(Long memberId, Long draftId) {
        BlogInfo blog = myBlog(memberId);
        return drafts.findByIdAndBlogId(draftId, blog.blogId()).orElseThrow(() -> new ApiException(ErrorCode.DRAFT_NOT_FOUND));
    }

    private static FieldErrorItem item(String field, ErrorCode code) {
        return new FieldErrorItem(field, code.name(), code.message());
    }

    private static ApiException field(String field, ErrorCode code, String message) {
        return new ApiException(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.message(),
                List.of(new FieldErrorItem(field, code.name(), message)));
    }

    public enum ScheduledOutcome {
        PUBLISHED, UNSCHEDULED, SKIPPED
    }

    public record DraftItem(Long draftId, String title, Instant updatedAt, Instant publishAt) {
    }

    public record DraftList(List<DraftItem> items, int count, int maxCount) {
    }

    public record Saved(Long draftId, Instant updatedAt, Instant publishAt) {

        static Saved of(PostDraft draft) {
            return new Saved(draft.getId(), draft.getUpdatedAt(), draft.getPublishAt());
        }
    }

    public record DraftView(Long draftId, String title, String content, Long categoryId, Long topicId, Long seriesId,
            String visibility, List<String> tags, Instant updatedAt, Instant publishAt) {

        static DraftView of(PostDraft draft) {
            return new DraftView(draft.getId(), draft.getTitle(), draft.getContent(), draft.getCategoryId(),
                    draft.getTopicId(), draft.getSeriesId(), draft.getVisibility(), draft.tagList(), draft.getUpdatedAt(),
                    draft.getPublishAt());
        }
    }
}
