package com.myblog.image.service;

import com.myblog.common.error.ErrorCode;
import com.myblog.common.error.FieldErrorMessages;
import com.myblog.image.config.ImageProperties;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * 이미지 오류 문구의 숫자·형식을 설정값(community.image)에 맞춘다.
 * 프레임워크가 먼저 거절한 너무 큰 파일(GlobalExceptionHandler)도 서비스와 같은 문구를 쓰게 한다 (specs/005 R-6).
 */
@Component
public class ImageErrorMessages implements FieldErrorMessages {

    private final ImageProperties properties;

    public ImageErrorMessages(ImageProperties properties) {
        this.properties = properties;
    }

    @Override
    public Optional<String> messageFor(ErrorCode code) {
        return switch (code) {
            case INVALID_IMAGE -> Optional.of(invalidImage(properties));
            case IMAGE_LIMIT_EXCEEDED -> Optional.of(limit(properties.maxPerPost()));
            default -> Optional.empty();
        };
    }

    static String invalidImage(ImageProperties properties) {
        return "이미지는 %dMB 이하의 %s만 올릴 수 있습니다".formatted(
                properties.maxSize().toMegabytes(), String.join(", ", properties.allowedTypes()));
    }

    static String limit(int maxPerPost) {
        return "※ 이미지는 글 하나에 %d장까지 올릴 수 있습니다".formatted(maxPerPost);
    }
}
