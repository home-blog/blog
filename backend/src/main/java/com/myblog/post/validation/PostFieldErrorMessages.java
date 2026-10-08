package com.myblog.post.validation;

import com.myblog.common.error.ErrorCode;
import com.myblog.common.error.FieldErrorMessages;
import com.myblog.post.config.PostProperties;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** 제목·본문 오류 문구의 글자 수를 설정값에 맞춘다 (specs/003 T019). */
@Component
public class PostFieldErrorMessages implements FieldErrorMessages {

    private final PostProperties properties;

    public PostFieldErrorMessages(PostProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<String> messageFor(ErrorCode code) {
        return switch (code) {
            case TITLE_TOO_LONG -> Optional.of("※ 제목은 %d자 이하로 입력해 주세요".formatted(properties.title().maxLength()));
            case CONTENT_TOO_LONG -> Optional.of("※ 본문은 %,d자 이하로 입력해 주세요".formatted(properties.content().maxLength()));
            default -> Optional.empty();
        };
    }
}
