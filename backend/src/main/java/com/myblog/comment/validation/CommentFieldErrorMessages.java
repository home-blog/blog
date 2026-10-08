package com.myblog.comment.validation;

import com.myblog.comment.config.CommentProperties;
import com.myblog.common.error.ErrorCode;
import com.myblog.common.error.FieldErrorMessages;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** 댓글 오류 문구의 글자 수를 설정값에 맞춘다 (specs/005 T017). */
@Component
public class CommentFieldErrorMessages implements FieldErrorMessages {

    private final CommentProperties properties;

    public CommentFieldErrorMessages(CommentProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<String> messageFor(ErrorCode code) {
        if (code == ErrorCode.COMMENT_TOO_LONG) {
            return Optional.of("※ 댓글은 %d자 이하로 입력해 주세요".formatted(properties.maxLength()));
        }
        return Optional.empty();
    }
}
