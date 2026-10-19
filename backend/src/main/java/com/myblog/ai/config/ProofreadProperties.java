package com.myblog.ai.config;

import java.time.Duration;
import java.time.ZoneId;
import java.util.Set;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 빨간 펜 설정 (application.yml의 proofread.*, specs/014 research D-1 ~ D-7, D-15).
 *
 * <ul>
 *   <li>provider: {@code auto}(열쇠가 있으면 claude, 없으면 without-key), {@code claude}, {@code fake}(개발용 규칙 교정), {@code off}</li>
 *   <li>열쇠(api-key)는 환경 변수 ANTHROPIC_API_KEY로만 받는다. 저장소에 적지 않는다</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "proofread")
public record ProofreadProperties(String provider, String withoutKey, String apiKey, String model, String effort,
        long maxTokens, Duration timeout, int dailyLimit, int maxLength, int maxCorrections, int originalMaxLength,
        int reasonMaxLength, Duration cacheTtl, Duration countMargin, ZoneId zone) {

    private static final Set<String> PROVIDERS = Set.of("auto", "claude", "fake", "off");
    private static final Set<String> EFFORTS = Set.of("low", "medium", "high", "xhigh", "max");

    public ProofreadProperties {
        if (!PROVIDERS.contains(provider)) {
            throw new IllegalStateException("proofread.provider는 auto, claude, fake, off 가운데 하나여야 합니다");
        }
        if (!Set.of("fake", "off").contains(withoutKey)) {
            throw new IllegalStateException("proofread.without-key는 fake 또는 off여야 합니다");
        }
        if (!EFFORTS.contains(effort)) {
            throw new IllegalStateException("proofread.effort는 low, medium, high, xhigh, max 가운데 하나여야 합니다");
        }
        if (dailyLimit < 1 || maxLength < 1 || maxCorrections < 1 || maxTokens < 1 || originalMaxLength < 1
                || reasonMaxLength < 1) {
            throw new IllegalStateException("proofread의 횟수·길이 값은 1 이상이어야 합니다");
        }
        apiKey = apiKey == null ? "" : apiKey.strip();
    }

    /** 실제로 쓸 교정기: auto는 열쇠가 있으면 claude, 없으면 without-key. */
    public String resolvedProvider() {
        if (!"auto".equals(provider)) {
            return provider;
        }
        return apiKey.isEmpty() ? withoutKey : "claude";
    }
}
