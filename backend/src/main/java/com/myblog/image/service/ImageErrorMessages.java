package com.myblog.image.service;

import com.myblog.common.error.ErrorCode;
import com.myblog.common.error.FieldErrorMessages;
import com.myblog.image.config.ImageProperties;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.unit.DataSize;

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
        return "이미지는 %s 이하의 %s만 올릴 수 있습니다".formatted(
                sizeText(properties.maxSize()), String.join(", ", properties.allowedTypes()));
    }

    /** 딱 떨어지는 MB면 MB로, 아니면 KB로 적는다 (1.5MB를 1MB로 줄여 안내하지 않게). */
    static String sizeText(DataSize size) {
        long megabyte = DataSize.ofMegabytes(1).toBytes();
        if (size.toBytes() % megabyte == 0) {
            return size.toMegabytes() + "MB";
        }
        return size.toKilobytes() + "KB";
    }

    static String limit(int maxPerPost) {
        return "※ 이미지는 글 하나에 %d장까지 올릴 수 있습니다".formatted(maxPerPost);
    }
}
