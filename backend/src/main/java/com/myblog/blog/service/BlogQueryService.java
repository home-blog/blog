package com.myblog.blog.service;

import com.myblog.blog.CategoryPostCounter;
import com.myblog.blog.domain.Blog;
import com.myblog.blog.domain.Category;
import com.myblog.blog.repository.BlogRepository;
import com.myblog.blog.repository.CategoryRepository;
import com.myblog.common.Visibility;
import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 블로그와 분류 목록 보기 (specs/003 contracts 1 ~ 3, FR-001, FR-006, FR-038, FR-039, FR-043, FR-048).
 * 이름은 요청마다 표에서 읽는다 (캐시 없음, SC-010).
 */
@Service
@Transactional(readOnly = true)
public class BlogQueryService {

    private final BlogRepository blogs;
    private final CategoryRepository categories;
    private final CategoryPostCounter postCounter;

    public BlogQueryService(BlogRepository blogs, CategoryRepository categories, CategoryPostCounter postCounter) {
        this.blogs = blogs;
        this.categories = categories;
        this.postCounter = postCounter;
    }

    /** 내 블로그 (회원 번호는 세션에서). */
    public BlogView myBlog(Long memberId) {
        Blog blog = blogs.findByOwnerId(memberId).orElseThrow(() -> new ApiException(ErrorCode.BLOG_NOT_FOUND));
        return BlogView.of(blog, true);
    }

    /** 누구나 보는 블로그 정보. viewerId는 로그인하지 않았으면 null. */
    public BlogView blog(Long blogId, Long viewerId) {
        Blog blog = find(blogId);
        return BlogView.of(blog, blog.getOwnerId().equals(viewerId));
    }

    /**
     * 분류 목록과 글 개수. 주인이 정한 순서(같으면 먼저 만든 것이 위).
     * 주인이 아니면 비공개 분류를 빼고, 개수는 공개 분류의 공개 글만 센다. 주인이면 비공개도 모두 센다.
     */
    public List<CategoryView> categories(Long blogId, Long viewerId) {
        Blog blog = find(blogId);
        List<Category> list = categories.findByBlogIdOrderBySortOrderAscIdAsc(blogId);
        if (blog.getOwnerId().equals(viewerId)) {
            Map<Long, Long> counts = postCounter.countAll(list.stream().map(Category::getId).toList());
            return list.stream().map(category -> CategoryView.of(category, counts.getOrDefault(category.getId(), 0L))).toList();
        }
        List<Category> open = list.stream().filter(category -> Visibility.isPublic(category.getVisibility())).toList();
        Map<Long, Long> counts = postCounter.countVisible(open.stream().map(Category::getId).toList());
        return open.stream().map(category -> CategoryView.of(category, counts.getOrDefault(category.getId(), 0L))).toList();
    }

    private Blog find(Long blogId) {
        return blogs.findById(blogId).orElseThrow(() -> new ApiException(ErrorCode.BLOG_NOT_FOUND));
    }

    /** 소개가 비었으면 ""로 준다 (data-model 1). */
    public record BlogView(Long blogId, String name, String intro, boolean isOwner) {

        static BlogView of(Blog blog, boolean isOwner) {
            return new BlogView(blog.getId(), blog.getName(), blog.getIntro() == null ? "" : blog.getIntro(), isOwner);
        }
    }

    public record CategoryView(Long categoryId, String name, long postCount, boolean isDefault, String visibility) {

        static CategoryView of(Category category, long postCount) {
            return new CategoryView(category.getId(), category.getName(), postCount, category.isDefaultCategory(),
                    category.getVisibility());
        }
    }
}
