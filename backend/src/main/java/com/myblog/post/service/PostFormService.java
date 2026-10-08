package com.myblog.post.service;

import com.myblog.blog.BlogDirectory;
import com.myblog.blog.BlogDirectory.BlogInfo;
import com.myblog.blog.BlogDirectory.CategoryInfo;
import com.myblog.common.Visibility;
import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.post.config.PostProperties;
import com.myblog.post.domain.Post;
import com.myblog.post.repository.PostRepository;
import com.myblog.post.repository.TopicRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 글쓰기 화면을 열 때 필요한 것 (specs/003 contracts 9, FR-012, FR-013, FR-046, research B-6):
 * 내 블로그의 분류, 주제 목록, 기본 분류·주제(내 블로그에서 가장 늦게 쓴 글의 것), 글자 수 제한.
 */
@Service
@Transactional(readOnly = true)
public class PostFormService {

    private final BlogDirectory blogDirectory;
    private final PostRepository posts;
    private final TopicRepository topics;
    private final PostProperties properties;

    public PostFormService(BlogDirectory blogDirectory, PostRepository posts, TopicRepository topics,
            PostProperties properties) {
        this.blogDirectory = blogDirectory;
        this.posts = posts;
        this.topics = topics;
        this.properties = properties;
    }

    public PostForm form(Long memberId) {
        BlogInfo blog = blogDirectory.myBlog(memberId).orElseThrow(() -> new ApiException(ErrorCode.BLOG_NOT_FOUND));
        List<CategoryInfo> categories = blogDirectory.categoriesOf(blog.blogId());
        Optional<Post> latest = posts.findFirstByCategoryIdInOrderByCreatedAtDescIdDesc(
                categories.stream().map(CategoryInfo::categoryId).toList());
        Long defaultCategoryId = latest.map(Post::getCategoryId).orElseGet(() -> categories.stream()
                .filter(CategoryInfo::isDefault).map(CategoryInfo::categoryId).findFirst().orElse(null));
        Long defaultTopicId = latest.map(Post::getTopicId).orElse(null);
        return new PostForm(
                categories.stream().map(c -> new CategoryOption(c.categoryId(), c.name())).toList(),
                topicOptions(),
                defaultCategoryId,
                defaultTopicId,
                Visibility.PUBLIC.value(),
                new Limits(properties.title().maxLength(), properties.content().maxLength()));
    }

    /** 주제 목록 (sort_order 순서). 글 수정 화면도 쓴다. */
    public List<TopicOption> topicOptions() {
        return topics.findAllByOrderBySortOrderAscIdAsc().stream().map(t -> new TopicOption(t.getId(), t.getName())).toList();
    }

    public List<CategoryOption> categoryOptions(Long blogId) {
        return blogDirectory.categoriesOf(blogId).stream().map(c -> new CategoryOption(c.categoryId(), c.name())).toList();
    }

    public record PostForm(List<CategoryOption> categories, List<TopicOption> topics, Long defaultCategoryId,
            Long defaultTopicId, String defaultVisibility, Limits limits) {
    }

    public record CategoryOption(Long categoryId, String name) {
    }

    public record TopicOption(Long topicId, String name) {
    }

    public record Limits(int titleMaxLength, int contentMaxLength) {
    }
}
