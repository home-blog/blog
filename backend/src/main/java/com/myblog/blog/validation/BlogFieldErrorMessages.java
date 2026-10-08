package com.myblog.blog.validation;

import com.myblog.blog.config.BlogProperties;
import com.myblog.blog.config.CategoryProperties;
import com.myblog.common.error.ErrorCode;
import com.myblog.common.error.FieldErrorMessages;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** 분류·블로그 오류 문구의 글자 수를 설정값에 맞춘다 (specs/003 T042, T048). */
@Component
public class BlogFieldErrorMessages implements FieldErrorMessages {

    private final CategoryProperties categoryProperties;
    private final BlogProperties blogProperties;

    public BlogFieldErrorMessages(CategoryProperties categoryProperties, BlogProperties blogProperties) {
        this.categoryProperties = categoryProperties;
        this.blogProperties = blogProperties;
    }

    @Override
    public Optional<String> messageFor(ErrorCode code) {
        return switch (code) {
            case CATEGORY_NAME_TOO_LONG ->
                    Optional.of("※ 분류 이름은 %d자 이하로 입력해 주세요".formatted(categoryProperties.name().maxLength()));
            case BLOG_NAME_TOO_LONG ->
                    Optional.of("※ 블로그 이름은 %d자 이하로 입력해 주세요".formatted(blogProperties.name().maxLength()));
            case BLOG_INTRO_TOO_LONG ->
                    Optional.of("※ 소개는 %d자 이하로 입력해 주세요".formatted(blogProperties.intro().maxLength()));
            default -> Optional.empty();
        };
    }
}
