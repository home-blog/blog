package com.myblog.user.validation;

import com.myblog.common.error.ErrorCode;
import com.myblog.common.error.FieldErrorMessages;
import com.myblog.user.config.AccountProperties;
import java.util.Optional;
import org.springframework.stereotype.Component;

/** 소개 오류 문구의 글자 수를 설정값에 맞춘다 (specs/002 T013). */
@Component
public class AccountFieldErrorMessages implements FieldErrorMessages {

    private final AccountProperties properties;

    public AccountFieldErrorMessages(AccountProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<String> messageFor(ErrorCode code) {
        if (code == ErrorCode.INTRO_TOO_LONG) {
            return Optional.of("※ 소개는 %d자 이하로 입력해 주세요".formatted(properties.intro().maxLength()));
        }
        return Optional.empty();
    }
}
