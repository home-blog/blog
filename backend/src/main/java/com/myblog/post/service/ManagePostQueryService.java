package com.myblog.post.service;

import com.myblog.blog.BlogDirectory;
import com.myblog.blog.BlogDirectory.BlogInfo;
import com.myblog.blog.BlogDirectory.CategoryInfo;
import com.myblog.common.Visibility;
import com.myblog.common.config.ManageProperties;
import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.post.PostCommentCounter;
import com.myblog.post.domain.Post;
import com.myblog.post.repository.PostRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 블로그 관리의 글 목록 (specs/006 contracts 3-1, T018, research B-2, FR-012 ~ FR-014, FR-017, FR-037).
 * <ul>
 *   <li>내 블로그는 세션의 회원으로만 찾는다. 내 블로그의 모든 분류에 든 글을 <b>비공개 포함</b> 읽는다.</li>
 *   <li>공개 여부는 정해진 값(all, public, private)만. 분류는 내 블로그 것이어야 하고, 아니면 없는 분류와 같은 404.</li>
 *   <li>(작성 시각, 글 번호) 최신순, 한 쪽 manage.page-size개. 댓글 수는 그 쪽의 글 번호로 한 번에 묻는다.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class ManagePostQueryService {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));
    private static final String ALL = "all";

    private final BlogDirectory blogDirectory;
    private final PostRepository posts;
    private final ObjectProvider<PostCommentCounter> commentCounter;
    private final int pageSize;

    public ManagePostQueryService(BlogDirectory blogDirectory, PostRepository posts,
            ObjectProvider<PostCommentCounter> commentCounter, ManageProperties properties) {
        this.blogDirectory = blogDirectory;
        this.posts = posts;
        this.commentCounter = commentCounter;
        this.pageSize = properties.pageSize();
    }

    /**
     * @param visibility "all", "public", "private" 중 하나 (그 밖은 400)
     * @param categoryId 고른 분류 (없으면 모든 분류)
     * @param page       1부터
     */
    public ManagePostPage list(Long memberId, String visibility, Long categoryId, int page) {
        if (page < 1) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED);
        }
        Optional<Visibility> only = visibilityFilter(visibility);
        BlogInfo blog = blogDirectory.myBlog(memberId).orElseThrow(() -> new ApiException(ErrorCode.BLOG_NOT_FOUND));
        List<CategoryInfo> mine = blogDirectory.categoriesOf(blog.blogId());
        Map<Long, CategoryInfo> categoryById = mine.stream()
                .collect(Collectors.toMap(CategoryInfo::categoryId, Function.identity()));
        List<Long> allIds = List.copyOf(categoryById.keySet());
        List<Long> scope = allIds;
        if (categoryId != null) {
            if (!categoryById.containsKey(categoryId)) {
                throw new ApiException(ErrorCode.CATEGORY_NOT_FOUND);
            }
            scope = List.of(categoryId);
        }

        boolean hasAnyPost = !allIds.isEmpty() && posts.countByCategoryIdIn(allIds) > 0;
        if (!hasAnyPost) {
            return new ManagePostPage(List.of(), page, pageSize, 0, false);
        }
        Pageable pageable = PageRequest.of(page - 1, pageSize, NEWEST_FIRST);
        long totalCount = only.isPresent()
                ? posts.countByCategoryIdInAndVisibility(scope, only.get().value())
                : posts.countByCategoryIdIn(scope);
        List<Post> found = only.isPresent()
                ? posts.findByCategoryIdInAndVisibility(scope, only.get().value(), pageable)
                : posts.findByCategoryIdIn(scope, pageable);
        Map<Long, Long> commentCounts = commentCounts(found.stream().map(Post::getId).toList());
        List<ManagePostRow> items = found.stream()
                .map(post -> ManagePostRow.of(post, categoryById.get(post.getCategoryId()),
                        commentCounts.getOrDefault(post.getId(), 0L)))
                .toList();
        return new ManagePostPage(items, page, pageSize, totalCount, true);
    }

    private static Optional<Visibility> visibilityFilter(String value) {
        if (value == null || ALL.equals(value)) {
            return Optional.empty();
        }
        return Optional.of(Visibility.from(value).orElseThrow(() -> new ApiException(ErrorCode.VALIDATION_FAILED)));
    }

    private Map<Long, Long> commentCounts(List<Long> postIds) {
        PostCommentCounter counter = commentCounter.getIfAvailable();
        return counter == null || postIds.isEmpty() ? Map.of() : counter.countByPostIds(postIds);
    }

    /** hasAnyPost: 거르기 전 내 글이 하나라도 있는지 ("아직 쓴 글이 없습니다"와 "글이 없습니다"를 나눈다, FR-017). */
    public record ManagePostPage(List<ManagePostRow> items, int page, int pageSize, long totalCount, boolean hasAnyPost) {
    }

    public record ManagePostRow(Long postId, String title, CategoryRef category, Instant createdAt, String visibility,
            int views, long commentCount) {

        static ManagePostRow of(Post post, CategoryInfo category, long commentCount) {
            return new ManagePostRow(post.getId(), post.getTitle(), new CategoryRef(category.categoryId(), category.name()),
                    post.getCreatedAt(), post.getVisibility(), post.getViews(), commentCount);
        }
    }

    public record CategoryRef(Long categoryId, String name) {
    }
}
