package com.myblog.post.service;

import com.myblog.blog.BlogDirectory;
import com.myblog.blog.BlogDirectory.CategoryInfo;
import com.myblog.blog.CategoryPostCounter;
import com.myblog.common.Visibility;
import com.myblog.post.repository.PostRepository;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 블로그 모듈의 CategoryPostCounter를 글 표와 PostVisibility의 조건으로 채운다 (specs/003 T010). */
@Component
@Transactional(readOnly = true)
public class CategoryPostCounterAdapter implements CategoryPostCounter {

    private final PostRepository posts;
    private final BlogDirectory blogDirectory;
    private final PostVisibility visibility;

    public CategoryPostCounterAdapter(PostRepository posts, BlogDirectory blogDirectory, PostVisibility visibility) {
        this.posts = posts;
        this.blogDirectory = blogDirectory;
        this.visibility = visibility;
    }

    @Override
    public long countAll(Long categoryId) {
        return posts.countByCategoryId(categoryId);
    }

    @Override
    public Map<Long, Long> countVisible(Collection<Long> categoryIds) {
        Map<Long, Long> counts = new LinkedHashMap<>();
        categoryIds.forEach(id -> counts.put(id, 0L));
        // 비공개 분류의 글은 공개 글이어도 세지 않는다 (FR-048)
        List<Long> openCategoryIds = blogDirectory.categories(categoryIds).stream()
                .filter(category -> visibility.isOpenToEveryone(Visibility.PUBLIC.value(), category.visibility()))
                .map(CategoryInfo::categoryId)
                .toList();
        if (!openCategoryIds.isEmpty()) {
            posts.countPublicByCategoryIds(openCategoryIds)
                    .forEach(row -> counts.put(row.getCategoryId(), row.getPostCount()));
        }
        return counts;
    }
}
