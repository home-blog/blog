package com.myblog.post.service;

import com.myblog.blog.BlogDirectory;
import com.myblog.blog.BlogDirectory.CategoryInfo;
import com.myblog.post.PostSummaryQuery;
import com.myblog.post.repository.PostRepository;
import com.myblog.post.repository.PostRepository.PostTitle;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** PostSummaryQuery를 글 표와 BlogDirectory(블로그 → 분류)로 채운다 (specs/006 T008). 글 표에는 블로그 칸이 없어 분류를 거친다. */
@Component
@Transactional(readOnly = true)
public class PostSummaryQueryAdapter implements PostSummaryQuery {

    private final PostRepository posts;
    private final BlogDirectory blogDirectory;

    public PostSummaryQueryAdapter(PostRepository posts, BlogDirectory blogDirectory) {
        this.posts = posts;
        this.blogDirectory = blogDirectory;
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
}
