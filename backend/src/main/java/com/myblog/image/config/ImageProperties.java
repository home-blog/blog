package com.myblog.image.config;

import com.myblog.image.domain.ImageType;
import java.time.Duration;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.unit.DataSize;

/**
 * 이미지 규칙과 저장소 설정 (application.yml의 community.image.*, specs/005 plan.md 설정값 목록, 헌법 VI).
 * 접속 키는 환경 변수로만 넣는다 (공개 저장소). 파일 업로드 크기의 프레임워크 설정도 maxSize에서 값을 가져온다 (ImageConfig).
 *
 * @param maxSize         한 장의 최대 크기 (5MB, 정확히 5MB는 통과)
 * @param maxPerPost      글 하나의 이미지 수 (10)
 * @param allowedTypes    받는 형식 (jpg, png, gif, webp)
 * @param orphanTtl       글에 연결되지 않은 이미지를 이만큼 지나면 지운다 (24시간, 가안, D-3)
 * @param cleanupInterval 정리 작업을 도는 간격
 * @param storage         s3(개발 MinIO) 또는 disk(서버 디스크) (D-1)
 */
@ConfigurationProperties(prefix = "community.image")
public record ImageProperties(DataSize maxSize, int maxPerPost, List<String> allowedTypes, Duration orphanTtl,
        Duration cleanupInterval, String storage, S3 s3, Disk disk) {

    public ImageProperties {
        if (maxSize == null || maxSize.toBytes() <= 0) {
            throw new IllegalStateException("community.image.max-size는 0보다 커야 합니다");
        }
        if (maxPerPost < 1) {
            throw new IllegalStateException("community.image.max-per-post는 1 이상이어야 합니다");
        }
        if (allowedTypes == null || allowedTypes.isEmpty()
                || allowedTypes.stream().anyMatch(type -> ImageType.fromExtension(type).isEmpty())) {
            throw new IllegalStateException("community.image.allowed-types는 jpg, png, gif, webp 중에서 골라야 합니다");
        }
        if (orphanTtl == null || orphanTtl.isNegative() || orphanTtl.isZero()) {
            throw new IllegalStateException("community.image.orphan-ttl은 0보다 길어야 합니다");
        }
        if (cleanupInterval == null || cleanupInterval.isNegative() || cleanupInterval.isZero()) {
            throw new IllegalStateException("community.image.cleanup-interval은 0보다 길어야 합니다");
        }
    }

    /** 받는 형식 (설정의 확장자 목록). */
    public Set<ImageType> allowedImageTypes() {
        Set<ImageType> types = EnumSet.noneOf(ImageType.class);
        allowedTypes.forEach(type -> ImageType.fromExtension(type).ifPresent(types::add));
        return types;
    }

    /** S3 방식 저장소 (개발 MinIO). 키가 비어 있으면 서버는 켜지고, 이미지 요청만 STORAGE_UNAVAILABLE이 된다. */
    public record S3(String endpoint, String region, String bucket, String accessKey, String secretKey) {
    }

    /** 서버 디스크 저장소. */
    public record Disk(String directory) {
    }
}
