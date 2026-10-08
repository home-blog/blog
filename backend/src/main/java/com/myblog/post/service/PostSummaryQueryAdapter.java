package com.myblog.post.service;

import com.myblog.blog.BlogDirectory;
import com.myblog.blog.BlogDirectory.CategoryInfo;
import com.myblog.post.PostSummaryQuery;
import com.myblog.post.domain.Post;
import com.myblog.post.repository.PostRepository;
import com.myblog.post.repository.PostRepository.PostTitle;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * PostSummaryQuery를 글 표와 BlogDirectory(블로그 → 분류)로 채운다 (specs/006 T008, T045). 글 표에는 블로그 칸이 없어 분류를 거친다.
 * "누구나 볼 수 있는 글"은 PostVisibility의 조건을 그대로 쓴다.
 */
@Component
@Transactional(readOnly = true)
public class PostSummaryQueryAdapter implements PostSummaryQuery {

    private static final Sort NEWEST_FIRST = Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"));

    private final PostRepository posts;
    private final BlogDirectory blogDirectory;
    private final PostVisibility visibility;

    public PostSummaryQueryAdapter(PostRepository posts, BlogDirectory blogDirectory, PostVisibility visibility) {
        this.posts = posts;
        this.blogDirectory = blogDirectory;
        this.visibility = visibility;
    }

    @Override
    public List<Long> postIdsOf(Long blogId) {
        List<Long> categoryIds = blogDirectory.categoriesOf(blogId).stream().map(CategoryInfo::categoryId).toList();
        return categoryIds.isEmpty() ? List.of() : posts.findIdsByCategoryIdIn(categoryIds);
    }

    @Override
    public Map<Long, String> titlesOf(Collection<Long> postIds) {
        if (postIds.isEmpty()) {
            return Map.of();
        }
        return posts.findTitlesByIdIn(postIds).stream().collect(Collectors.toMap(PostTitle::getId, PostTitle::getTitle));
    }

    @Override
    public List<PostSummary> recent(Long blogId, int size) {
        List<Long> categoryIds = blogDirectory.categoriesOf(blogId).stream().map(CategoryInfo::categoryId).toList();
        if (categoryIds.isEmpty()) {
            return List.of();
        }
        return posts.findByCategoryIdIn(categoryIds, PageRequest.of(0, size, NEWEST_FIRST)).stream()
                .map(PostSummaryQueryAdapter::toSummary).toList();
    }

    @Override
    public List<PostSummary> publicAmong(Collection<Long> postIds) {
        if (postIds.isEmpty()) {
            return List.of();
        }
        List<Post> found = posts.findAllById(postIds);
        Map<Long, CategoryInfo> categoryById = blogDirectory.categories(found.stream().map(Post::getCategoryId).distinct().toList())
                .stream().collect(Collectors.toMap(CategoryInfo::categoryId, Function.identity()));
        return found.stream()
                .filter(post -> {
                    CategoryInfo category = categoryById.get(post.getCategoryId());
                    return category != null && visibility.isOpenToEveryone(post.getVisibility(), category.visibility());
                })
                .map(PostSummaryQueryAdapter::toSummary).toList();
    }

    private static PostSummary toSummary(Post post) {
        return new PostSummary(post.getId(), post.getTitle(), post.getCreatedAt(), post.getVisibility());
    }
}
