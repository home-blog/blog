package com.myblog.post.config;

import java.time.Duration;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 비슷한 글 설정 (application.yml의 related.*, specs/014 research D-9 ~ D-13, D-15).
 *
 * <ul>
 *   <li>provider: {@code auto}(열쇠가 있으면 voyage, 없으면 without-key), {@code voyage}, {@code fake}(개발용), {@code off}</li>
 *   <li>열쇠(api-key)는 환경 변수 VOYAGE_API_KEY로만 받는다</li>
 *   <li>정리 작업 간격(related.backfill-interval)은 @Scheduled가 직접 읽는다</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "related")
public record RelatedProperties(String provider, String withoutKey, String apiKey, String baseUrl, String model,
        int dimensions, int maxChars, Duration timeout, int batchSize, int count, int perBlog, int maxCandidates,
        Duration cacheTtl) {

    private static final Set<String> PROVIDERS = Set.of("auto", "voyage", "fake", "off");

    public RelatedProperties {
        if (!PROVIDERS.contains(provider)) {
            throw new IllegalStateException("related.provider는 auto, voyage, fake, off 가운데 하나여야 합니다");
        }
        if (!Set.of("fake", "off").contains(withoutKey)) {
            throw new IllegalStateException("related.without-key는 fake 또는 off여야 합니다");
        }
        if (dimensions < 1 || maxChars < 1 || batchSize < 1 || count < 1 || perBlog < 1 || maxCandidates < 1) {
            throw new IllegalStateException("related의 수 값은 1 이상이어야 합니다");
        }
        apiKey = apiKey == null ? "" : apiKey.strip();
    }

    /** 실제로 쓸 임베딩: auto는 열쇠가 있으면 voyage, 없으면 without-key. */
    public String resolvedProvider() {
        if (!"auto".equals(provider)) {
            return provider;
        }
        return apiKey.isEmpty() ? withoutKey : "voyage";
    }
}
