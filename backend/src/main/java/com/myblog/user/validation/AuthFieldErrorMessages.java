package com.myblog.user.validation;

import com.myblog.common.error.ErrorCode;
import com.myblog.common.error.FieldErrorMessages;
import com.myblog.user.config.AuthProperties;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** 닉네임·비밀번호 오류 문구의 글자 수를 설정값에 맞춘다 (값을 바꾸면 문구도 같이 바뀜). */
@Component
public class AuthFieldErrorMessages implements FieldErrorMessages {

    private final AuthProperties properties;

    public AuthFieldErrorMessages(AuthProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<String> messageFor(ErrorCode code) {
        return switch (code) {
            case INVALID_NICKNAME -> Optional.of("닉네임은 한글, 영문, 숫자로 %d~%d자여야 합니다"
                    .formatted(properties.nickname().minLength(), properties.nickname().maxLength()));
            case INVALID_PASSWORD -> Optional.of("비밀번호는 영문, 숫자, 특수문자를 포함해 %d~%d자로 입력해 주세요"
                    .formatted(properties.password().minLength(), properties.password().maxLength()));
            default -> Optional.empty();
        };
    }
}
