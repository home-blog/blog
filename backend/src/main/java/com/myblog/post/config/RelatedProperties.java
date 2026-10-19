package com.myblog.post.config;

import java.time.Duration;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 비슷한 글 설정 (application.yml의 related.*, specs/014 research D-9 ~ D-13, D-15).
 *
 * <ul>
 *   <li>provider: {@code auto}(Voyage 열쇠가 있으면 voyage, 없고 Ollama 주소가 있으면 ollama, 둘 다 없으면 without-key),
 *       {@code voyage}, {@code ollama}(bge-m3, 무료·내 서버), {@code local}(글자 조각, 무료·서버 안), {@code off}</li>
 *   <li>열쇠(api-key)는 환경 변수 VOYAGE_API_KEY로만 받는다. Ollama 주소(ollama-url)는 RELATED_OLLAMA_URL(배포가 켤 때만 넣는다)</li>
 *   <li>정리 작업 간격(related.backfill-interval)은 @Scheduled가 직접 읽는다</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "related")
public record RelatedProperties(String provider, String withoutKey, String apiKey, String baseUrl, String model,
        String ollamaUrl, String ollamaModel, Duration ollamaTimeout, int dimensions, int maxChars, Duration timeout, int batchSize, int count, int perBlog, int maxCandidates,
        int tagPoolFactor, int queueSize, Duration cacheTtl) {

    private static final Set<String> PROVIDERS = Set.of("auto", "voyage", "ollama", "local", "off");

    public RelatedProperties {
        if (!PROVIDERS.contains(provider)) {
            throw new IllegalStateException("related.provider는 auto, voyage, ollama, local, off 가운데 하나여야 합니다");
        }
        if (!Set.of("local", "off").contains(withoutKey)) {
            throw new IllegalStateException("related.without-key는 local 또는 off여야 합니다");
        }
        if (dimensions < 1 || maxChars < 1 || batchSize < 1 || count < 1 || perBlog < 1 || maxCandidates < 1
                || tagPoolFactor < 1 || queueSize < 1) {
            throw new IllegalStateException("related의 수 값은 1 이상이어야 합니다");
        }
        apiKey = apiKey == null ? "" : apiKey.strip();
        ollamaUrl = ollamaUrl == null ? "" : ollamaUrl.strip();
    }

    /** 실제로 쓸 임베딩: auto는 Voyage 열쇠 → Ollama 주소 → without-key 순서로 고른다. */
    public String resolvedProvider() {
        if (!"auto".equals(provider)) {
            return provider;
        }
        if (!apiKey.isEmpty()) {
            return "voyage";
        }
        return ollamaUrl.isEmpty() ? withoutKey : "ollama";
    }
}
