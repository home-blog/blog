package com.myblog.post.service;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.myblog.blog.BlogClosingEvent;
import com.myblog.blog.BlogDirectory;
import com.myblog.blog.BlogDirectory.BlogInfo;
import com.myblog.blog.BlogDirectory.CategoryInfo;
import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.common.error.ErrorResponse.FieldErrorItem;
import com.myblog.post.PostDeletingEvent;
import com.myblog.post.config.SeriesProperties;
import com.myblog.post.domain.Post;
import com.myblog.post.domain.Series;
import com.myblog.post.repository.PostRepository;
import com.myblog.post.repository.SeriesPostRepository;
import com.myblog.post.repository.SeriesPostRepository.Entry;
import com.myblog.post.repository.SeriesPostRepository.SeriesEntry;
import com.myblog.post.repository.SeriesRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 연재 (specs/013 US2, contracts 2).
 * <ul>
 *   <li>주인만 만들고 고치고 글을 넣는다. 남의 연재·남의 글은 없는 것과 같은 404 (S2-5).</li>
 *   <li>화 번호는 저장하지 않고 <b>보는 사람에게 보이는 글</b>로 센다 (D-10): 공개 규칙은 PostVisibility 그대로.</li>
 *   <li>글을 지우면 연재에서도 빠지고(PostDeletingEvent), 블로그가 닫히면 연재도 지운다.</li>
 * </ul>
 */
@Service
@Transactional
public class SeriesService {

    private final SeriesRepository seriesRepository;
    private final SeriesPostRepository seriesPosts;
    private final PostRepository posts;
    private final BlogDirectory blogDirectory;
    private final PostVisibility visibility;
    private final SeriesProperties properties;
    private final Clock clock;

    public SeriesService(SeriesRepository seriesRepository, SeriesPostRepository seriesPosts, PostRepository posts,
            BlogDirectory blogDirectory, PostVisibility visibility, SeriesProperties properties, Clock clock) {
        this.seriesRepository = seriesRepository;
        this.seriesPosts = seriesPosts;
        this.posts = posts;
        this.blogDirectory = blogDirectory;
        this.visibility = visibility;
        this.properties = properties;
        this.clock = clock;
    }

    // ── 읽기 (누구나) ─────────────────────────────────────────────

    /** 블로그의 연재 목록 (contracts 2-1). postCount는 보는 사람에게 보이는 글 수. */
    @Transactional(readOnly = true)
    public List<SeriesSummary> list(Long blogId, Long viewerId) {
        blogDirectory.blog(blogId).orElseThrow(() -> new ApiException(ErrorCode.BLOG_NOT_FOUND));
        List<Series> list = seriesRepository.findByBlogIdOrderByCreatedAtAscIdAsc(blogId);
        List<SeriesEntry> entries = seriesPosts.entriesOf(list.stream().map(Series::getId).toList());
        Map<Long, CategoryInfo> categories = categoriesOf(entries.stream().map(SeriesEntry::categoryId).toList());
        Map<Long, Long> counts = entries.stream()
                .filter(entry -> canSee(entry.visibility(), categories.get(entry.categoryId()), viewerId))
                .collect(Collectors.groupingBy(SeriesEntry::seriesId, Collectors.counting()));
        return list.stream()
                .map(series -> new SeriesSummary(series.getId(), series.getName(), series.getDescription(),
                        counts.getOrDefault(series.getId(), 0L)))
                .toList();
    }

    /** 연재 한 개 (contracts 2-2): 보이는 글만 화 순서로. 주인에게는 공개 범위도 준다. */
    @Transactional(readOnly = true)
    public SeriesDetail detail(Long seriesId, Long viewerId) {
        Series series = seriesRepository.findById(seriesId).orElseThrow(SeriesService::seriesNotFound);
        boolean owner = isOwner(series, viewerId);
        List<Entry> visible = visibleEntries(series.getId(), viewerId);
        List<EpisodeItem> items = new ArrayList<>();
        for (int i = 0; i < visible.size(); i++) {
            Entry entry = visible.get(i);
            items.add(new EpisodeItem(entry.postId(), entry.title(), i + 1, entry.createdAt(),
                    owner ? entry.visibility() : null));
        }
        return new SeriesDetail(series.getId(), series.getBlogId(), series.getName(), series.getDescription(), items);
    }

    /** 글의 연재 정보 (contracts 2-3). 글을 볼 수 없으면 글 읽기와 같은 404. 연재에 없으면 null. */
    @Transactional(readOnly = true)
    public PostSeries ofPost(Long postId, Long viewerId) {
        Post post = posts.findById(postId).orElseThrow(PostReadService::notFound);
        CategoryInfo category = blogDirectory.category(post.getCategoryId()).orElseThrow(PostReadService::notFound);
        if (!visibility.canSee(post.getVisibility(), category, viewerId)) {
            throw PostReadService.notFound();
        }
        Long seriesId = seriesPosts.seriesOf(postId).orElse(null);
        if (seriesId == null) {
            return null;
        }
        Series series = seriesRepository.findById(seriesId).orElseThrow(SeriesService::seriesNotFound);
        List<Entry> visible = visibleEntries(seriesId, viewerId);
        int index = -1;
        List<EpisodeRef> refs = new ArrayList<>();
        for (int i = 0; i < visible.size(); i++) {
            Entry entry = visible.get(i);
            refs.add(new EpisodeRef(entry.postId(), entry.title(), i + 1));
            if (entry.postId().equals(postId)) {
                index = i;
            }
        }
        LinkRef previous = index > 0 ? link(visible.get(index - 1)) : null;
        LinkRef next = index >= 0 && index < visible.size() - 1 ? link(visible.get(index + 1)) : null;
        return new PostSeries(series.getId(), series.getName(), index + 1, visible.size(), previous, next, refs);
    }

    // ── 쓰기 (주인) ─────────────────────────────────────────────

    public SeriesDetail create(Long memberId, String name, String description) {
        BlogInfo blog = myBlog(memberId);
        Input input = validate(name, description);
        if (seriesRepository.countByBlogId(blog.blogId()) >= properties.maxPerBlog()) {
            throw new ApiException(ErrorCode.SERIES_LIMIT_EXCEEDED,
                    ErrorCode.SERIES_LIMIT_EXCEEDED.message().formatted(properties.maxPerBlog()));
        }
        Series series = seriesRepository.save(Series.create(blog.blogId(), input.name(), input.description(), Instant.now(clock)));
        return detail(series.getId(), memberId);
    }

    public SeriesDetail update(Long memberId, Long seriesId, String name, String description) {
        Series series = mySeries(memberId, seriesId);
        Input input = validate(name, description);
        series.change(input.name(), input.description());
        return detail(series.getId(), memberId);
    }

    public void delete(Long memberId, Long seriesId) {
        seriesRepository.delete(mySeries(memberId, seriesId));
    }

    /** 글을 연재의 끝 화로 넣는다(다른 연재에서는 빠진다). 이미 이 연재에 있으면 그대로 (contracts 2-5). */
    public void addPost(Long memberId, Long seriesId, Long postId) {
        Series series = mySeries(memberId, seriesId);
        myPost(series.getBlogId(), postId);
        if (seriesPosts.seriesOf(postId).filter(seriesId::equals).isPresent()) {
            return;
        }
        if (seriesPosts.count(seriesId) >= properties.maxPosts()) {
            throw new ApiException(ErrorCode.SERIES_POSTS_LIMIT_EXCEEDED,
                    ErrorCode.SERIES_POSTS_LIMIT_EXCEEDED.message().formatted(properties.maxPosts()));
        }
        seriesPosts.append(seriesId, postId);
    }

    public void removePost(Long memberId, Long seriesId, Long postId) {
        mySeries(memberId, seriesId);
        if (seriesPosts.remove(seriesId, postId) == 0) {
            throw PostReadService.notFound();
        }
    }

    /** 순서 바꾸기 (S2-7): 그 연재의 글 번호를 모두, 한 번씩. */
    public SeriesDetail reorder(Long memberId, Long seriesId, List<Long> postIds) {
        mySeries(memberId, seriesId);
        Set<Long> current = new HashSet<>(seriesPosts.postIds(seriesId));
        if (postIds == null || postIds.isEmpty() || postIds.size() != current.size()
                || !current.equals(new HashSet<>(postIds))) {
            throw new ApiException(ErrorCode.SERIES_ORDER_MISMATCH);
        }
        seriesPosts.reorder(seriesId, postIds);
        return detail(seriesId, memberId);
    }

    /**
     * 임시 저장 글을 발행할 때 고른 연재에 넣는다 (013 US3). 연재가 그새 지워졌거나 남의 것이면 넣지 않는다
     * (글 발행을 실패로 만들지 않는다). 한도를 넘으면 넣지 않는다.
     */
    public void attachIfPossible(Long blogId, Long seriesId, Long postId) {
        if (seriesId == null) {
            return;
        }
        seriesRepository.findById(seriesId)
                .filter(series -> series.getBlogId().equals(blogId))
                .filter(series -> seriesPosts.count(seriesId) < properties.maxPosts())
                .ifPresent(series -> seriesPosts.append(seriesId, postId));
    }

    /** 이 회원의 연재인가 (임시 저장 글의 연재 칸을 검사할 때). */
    @Transactional(readOnly = true)
    public boolean isMine(Long blogId, Long seriesId) {
        return seriesRepository.findById(seriesId).filter(series -> series.getBlogId().equals(blogId)).isPresent();
    }

    @EventListener
    public void on(PostDeletingEvent event) {
        seriesPosts.removePost(event.postId());
    }

    @EventListener
    public void on(BlogClosingEvent event) {
        seriesRepository.findByBlogIdOrderByCreatedAtAscIdAsc(event.blogId())
                .forEach(series -> seriesPosts.postIds(series.getId()).forEach(seriesPosts::removePost));
        seriesRepository.deleteByBlogId(event.blogId());
    }

    // ── 도우미 ─────────────────────────────────────────────

    private List<Entry> visibleEntries(Long seriesId, Long viewerId) {
        List<Entry> entries = seriesPosts.entries(seriesId);
        Map<Long, CategoryInfo> categories = categoriesOf(entries.stream().map(Entry::categoryId).toList());
        return entries.stream()
                .filter(entry -> canSee(entry.visibility(), categories.get(entry.categoryId()), viewerId))
                .toList();
    }

    private Map<Long, CategoryInfo> categoriesOf(List<Long> categoryIds) {
        if (categoryIds.isEmpty()) {
            return Map.of();
        }
        return blogDirectory.categories(new HashSet<>(categoryIds)).stream()
                .collect(Collectors.toMap(CategoryInfo::categoryId, Function.identity()));
    }

    private boolean canSee(String postVisibility, CategoryInfo category, Long viewerId) {
        return category != null && visibility.canSee(postVisibility, category, viewerId);
    }

    private boolean isOwner(Series series, Long viewerId) {
        return viewerId != null && blogDirectory.blog(series.getBlogId())
                .map(blog -> viewerId.equals(blog.ownerId())).orElse(false);
    }

    private BlogInfo myBlog(Long memberId) {
        return blogDirectory.myBlog(memberId).orElseThrow(() -> new ApiException(ErrorCode.BLOG_NOT_FOUND));
    }

    private Series mySeries(Long memberId, Long seriesId) {
        BlogInfo blog = myBlog(memberId);
        return seriesRepository.findById(seriesId)
                .filter(series -> series.getBlogId().equals(blog.blogId()))
                .orElseThrow(SeriesService::seriesNotFound);
    }

    private void myPost(Long blogId, Long postId) {
        Post post = posts.findById(postId).orElseThrow(PostReadService::notFound);
        blogDirectory.category(post.getCategoryId())
                .filter(category -> category.blogId().equals(blogId))
                .orElseThrow(PostReadService::notFound);
    }

    private Input validate(String name, String description) {
        String trimmedName = name == null ? "" : name.strip();
        String trimmedDescription = description == null ? "" : description.strip();
        List<FieldErrorItem> errors = new ArrayList<>();
        int nameLength = trimmedName.codePointCount(0, trimmedName.length());
        if (nameLength < properties.name().minLength()) {
            errors.add(item("name", ErrorCode.SERIES_NAME_REQUIRED, ErrorCode.SERIES_NAME_REQUIRED.message()));
        } else if (nameLength > properties.name().maxLength()) {
            errors.add(item("name", ErrorCode.SERIES_NAME_TOO_LONG,
                    ErrorCode.SERIES_NAME_TOO_LONG.message().formatted(properties.name().maxLength())));
        }
        if (trimmedDescription.codePointCount(0, trimmedDescription.length()) > properties.description().maxLength()) {
            errors.add(item("description", ErrorCode.SERIES_DESCRIPTION_TOO_LONG,
                    ErrorCode.SERIES_DESCRIPTION_TOO_LONG.message().formatted(properties.description().maxLength())));
        }
        if (!errors.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.message(), errors);
        }
        return new Input(trimmedName, trimmedDescription);
    }

    private static FieldErrorItem item(String field, ErrorCode code, String message) {
        return new FieldErrorItem(field, code.name(), message);
    }

    private static LinkRef link(Entry entry) {
        return new LinkRef(entry.postId(), entry.title());
    }

    static ApiException seriesNotFound() {
        return new ApiException(ErrorCode.SERIES_NOT_FOUND);
    }

    private record Input(String name, String description) {
    }

    public record SeriesSummary(Long seriesId, String name, String description, long postCount) {
    }

    /** visibility는 주인에게만 (방문자에게는 null이라 응답에서 빠진다). */
    public record EpisodeItem(Long postId, String title, int episode, Instant createdAt,
            @JsonInclude(JsonInclude.Include.NON_NULL) String visibility) {
    }

    public record SeriesDetail(Long seriesId, Long blogId, String name, String description, List<EpisodeItem> posts) {
    }

    public record EpisodeRef(Long postId, String title, int episode) {
    }

    public record LinkRef(Long postId, String title) {
    }

    public record PostSeries(Long seriesId, String name, int episode, int total, LinkRef previous, LinkRef next,
            List<EpisodeRef> posts) {
    }
}
