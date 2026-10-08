package com.myblog.post.service;

import com.myblog.blog.BlogDirectory;
import com.myblog.blog.BlogDirectory.CategoryInfo;
import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.common.error.ErrorResponse.FieldErrorItem;
import com.myblog.post.repository.TopicRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** 글쓰기와 글 수정이 같이 쓰는 확인: 분류는 내 블로그의 것, 주제는 topic 표에 있는 것 (FR-008, FR-034, FR-046). */
@Component
public class PostInputChecks {

    private final BlogDirectory blogDirectory;
    private final TopicRepository topics;

    public PostInputChecks(BlogDirectory blogDirectory, TopicRepository topics) {
        this.blogDirectory = blogDirectory;
        this.topics = topics;
    }

    /** 분류가 이 블로그의 것이 아니거나 없으면 INVALID_CATEGORY. */
    public void checkCategory(Long categoryId, Long blogId) {
        Optional<CategoryInfo> category = categoryId == null ? Optional.empty() : blogDirectory.category(categoryId);
        if (category.isEmpty() || !category.get().blogId().equals(blogId)) {
            throw invalidCategory();
        }
    }

    /** 주제가 없거나 topic 표에 없으면 topicId "주제를 골라 주세요". */
    public void checkTopic(Long topicId) {
        if (topicId == null || !topics.existsById(topicId)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.message(),
                    List.of(new FieldErrorItem("topicId", ErrorCode.TOPIC_REQUIRED.name(), ErrorCode.TOPIC_REQUIRED.message())));
        }
    }

    public static ApiException invalidCategory() {
        return new ApiException(ErrorCode.INVALID_CATEGORY, ErrorCode.INVALID_CATEGORY.message(),
                List.of(new FieldErrorItem("categoryId", ErrorCode.INVALID_CATEGORY.name(), ErrorCode.INVALID_CATEGORY.message())));
    }
}
