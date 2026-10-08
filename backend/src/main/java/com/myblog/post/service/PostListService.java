package com.myblog.post.service;

import com.myblog.blog.BlogDirectory;
import com.myblog.blog.BlogDirectory.BlogInfo;
import com.myblog.blog.BlogDirectory.CategoryInfo;
import com.myblog.common.Visibility;
import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.post.config.ExploreProperties;
import com.myblog.post.domain.Post;
import com.myblog.post.repository.PostRepository;
import com.myblog.post.service.PageNumbers.PageSlice;
import com.myblog.post.service.PostVisibility.ListScope;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 한 블로그의 글 목록 (specs/004 contracts 1, FR-001 ~ FR-006, FR-009).
 * 세기와 10개 읽기를 한 읽기 전용 트랜잭션에서 한다 (research R-4). 블로그·분류 이름은 요청마다 읽는다.
 */
@Service
@Transactional(readOnly = true)
public class PostListService {

    /** 작성 시각 내림차순, 같으면 나중에 만든 글(큰 번호)이 위 (FR-001, research B-2). */
    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final BlogDirectory blogDirectory;
    private final PostRepository posts;
    private final PostVisibility visibility;
    private final PostPreview preview;
    private final int pageSize;

    public PostListService(BlogDirectory blogDirectory, PostRepository posts, PostVisibility visibility,
            PostPreview preview, ExploreProperties properties) {
        this.blogDirectory = blogDirectory;
        this.posts = posts;
        this.visibility = visibility;
        this.preview = preview;
        this.pageSize = properties.list().pageSize();
    }

    /**
     * @param viewerId 로그인한 회원 번호. 로그인하지 않았거나 탈퇴했으면 null (방문자)
     * @param page       요청한 페이지 번호 글자 그대로 (없으면 null)
     * @param categoryId 고른 분류 번호 글자 그대로 (없거나 비었으면 모든 분류)
     */
    public PostListView list(Long blogId, Long viewerId, String page, String categoryId) {
        BlogInfo blog = blogDirectory.blog(blogId).orElseThrow(() -> new ApiException(ErrorCode.BLOG_NOT_FOUND));
        boolean isOwner = viewerId != null && viewerId.equals(blog.ownerId());
        ListScope scope = visibility.listScope(blog.ownerId(), blogDirectory.categoriesOf(blogId), viewerId);
        Long chosenCategoryId = null;
        if (categoryId != null && !categoryId.isEmpty()) {
            CategoryInfo chosen = chosenCategory(scope, categoryId);
            chosenCategoryId = chosen.categoryId();
            scope = new ListScope(List.of(chosen), scope.publicPostsOnly());
        }

        long totalCount = count(scope);
        PageSlice slice = PageNumbers.of(page, totalCount, pageSize);
        Map<Long, CategoryInfo> categoryById = scope.categories().stream()
                .collect(Collectors.toMap(CategoryInfo::categoryId, Function.identity()));
        List<PostRow> rows = read(scope, slice).stream()
                .map(post -> toRow(post, categoryById.get(post.getCategoryId())))
                .toList();
        return new PostListView(blogId, isOwner, chosenCategoryId, totalCount, slice.page(), slice.totalPages(), slice.pageSize(),
                rows);
    }

    /**
     * 고른 분류는 보이는 분류 묶음 안에 있을 때만 쓴다 (FR-007). 다른 블로그의 분류, 없는 번호, 숫자가 아닌 값,
     * 방문자가 보낸 비공개 분류 번호는 모두 똑같이 "존재하지 않는 분류"다: 비공개 분류가 있다는 것을 드러내지 않는다
     * (004 D-2: 가, D-7).
     */
    private static CategoryInfo chosenCategory(ListScope scope, String categoryId) {
        return scope.categories().stream()
                .filter(category -> category.categoryId().toString().equals(categoryId))
                .findFirst()
                .orElseThrow(PostListService::unknownCategory);
    }

    private static ApiException unknownCategory() {
        return new ApiException(ErrorCode.CATEGORY_NOT_FOUND);
    }

    private long count(ListScope scope) {
        List<Long> categoryIds = scope.categoryIds();
        if (categoryIds.isEmpty()) {
            return 0;
        }
        return scope.publicPostsOnly()
                ? posts.countByCategoryIdInAndVisibility(categoryIds, Visibility.PUBLIC.value())
                : posts.countByCategoryIdIn(categoryIds);
    }

    private List<Post> read(ListScope scope, PageSlice slice) {
        List<Long> categoryIds = scope.categoryIds();
        if (categoryIds.isEmpty()) {
            return List.of();
        }
        Pageable pageable = PageRequest.of(slice.page() - 1, slice.pageSize(), NEWEST_FIRST);
        return scope.publicPostsOnly()
                ? posts.findByCategoryIdInAndVisibility(categoryIds, Visibility.PUBLIC.value(), pageable)
                : posts.findByCategoryIdIn(categoryIds, pageable);
    }

    private PostRow toRow(Post post, CategoryInfo category) {
        return new PostRow(post.getId(), post.getTitle(), category.categoryId(), category.name(), post.getCreatedAt(),
                preview.of(post.getContent()), post.getVisibility());
    }

    /** 목록 한 페이지. categoryId는 실제로 적용한 분류 (고르지 않았으면 null). */
    public record PostListView(Long blogId, boolean isOwner, Long categoryId, long totalCount, int page,
            int totalPages, int pageSize, List<PostRow> posts) {
    }

    /** 목록 한 줄. visibility는 주인에게만 "private"가 올 수 있다 (FR-006). */
    public record PostRow(Long postId, String title, Long categoryId, String categoryName, Instant createdAt,
            String preview, String visibility) {
    }
}
