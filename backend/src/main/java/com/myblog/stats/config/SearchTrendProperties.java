package com.myblog.stats.config;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 실시간 인기 검색어의 숫자 (application.yml의 stats.search-trend.*, specs/015 research D-1 ~ D-4).
 * <ul>
 *   <li>bucket: 한 칸의 길이(10분). windowBuckets 칸(6 = 1시간)을 합쳐 순위를 낸다. 변화는 한 칸 앞과 견준다</li>
 *   <li>dedupeWindow: 같은 사람·같은 검색어를 이 길이로 나눈 같은 칸 안에 다시 찾으면 세지 않는다(10분)</li>
 *   <li>size: 순위 수(10). banned: 들어 있으면 세지 않는 말(소문자로 맞춰 견준다)</li>
 * </ul>
 */
@ConfigurationProperties(prefix = "stats.search-trend")
public record SearchTrendProperties(Duration bucket, int windowBuckets, Duration dedupeWindow, int size,
        List<String> banned) {

    public SearchTrendProperties {
        if (bucket == null || bucket.toSeconds() < 1 || dedupeWindow == null
                || dedupeWindow.toSeconds() < 1) {
            throw new IllegalStateException("stats.search-trend.bucket·dedupe-window는 1초 이상이어야 합니다");
        }
        if (windowBuckets < 1 || size < 1) {
            throw new IllegalStateException("stats.search-trend.window-buckets·size는 1 이상이어야 합니다");
        }
        banned = banned == null ? List.of()
                : banned.stream().map(word -> word.strip().toLowerCase(Locale.ROOT)).filter(word -> !word.isEmpty()).toList();
    }
}
