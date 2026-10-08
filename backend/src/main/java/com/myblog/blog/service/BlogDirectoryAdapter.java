package com.myblog.blog.service;

import com.myblog.blog.BlogDirectory;
import com.myblog.blog.domain.Blog;
import com.myblog.blog.domain.Category;
import com.myblog.blog.repository.BlogRepository;
import com.myblog.blog.repository.CategoryRepository;
import com.myblog.common.Visibility;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** BlogDirectory를 블로그·분류 표로 채운다. */
@Component
@Transactional(readOnly = true)
public class BlogDirectoryAdapter implements BlogDirectory {

    private final BlogRepository blogs;
    private final CategoryRepository categories;

    public BlogDirectoryAdapter(BlogRepository blogs, CategoryRepository categories) {
        this.blogs = blogs;
        this.categories = categories;
    }

    @Override
    public Optional<BlogInfo> myBlog(Long memberId) {
        return blogs.findByOwnerId(memberId).map(blog -> new BlogInfo(blog.getId(), blog.getOwnerId(), blog.getName()));
    }

    @Override
    public Optional<CategoryInfo> category(Long categoryId) {
        return categories.findById(categoryId).flatMap(category ->
                blogs.findById(category.getBlogId()).map(blog -> toInfo(category, blog)));
    }

    @Override
    public List<CategoryInfo> categories(Collection<Long> categoryIds) {
        List<Category> found = categories.findAllById(categoryIds);
        Map<Long, Blog> blogById = blogs.findAllById(found.stream().map(Category::getBlogId).distinct().toList())
                .stream().collect(Collectors.toMap(Blog::getId, Function.identity()));
        return found.stream().map(category -> toInfo(category, blogById.get(category.getBlogId()))).toList();
    }

    @Override
    public List<CategoryInfo> categoriesOf(Long blogId) {
        return blogs.findById(blogId)
                .map(blog -> categories.findByBlogIdOrderBySortOrderAscIdAsc(blogId).stream()
                        .map(category -> toInfo(category, blog)).toList())
                .orElse(List.of());
    }

    @Override
    public List<Long> publicCategoryIds(Long blogId) {
        return categories.findByBlogIdOrderBySortOrderAscIdAsc(blogId).stream()
                .filter(category -> Visibility.isPublic(category.getVisibility()))
                .map(Category::getId)
                .toList();
    }

    private static CategoryInfo toInfo(Category category, Blog blog) {
        return new CategoryInfo(category.getId(), blog.getId(), blog.getOwnerId(), blog.getName(), category.getName(),
                category.getVisibility(), category.isDefaultCategory());
    }
}
