package com.myblog.stats.service;

import com.myblog.stats.config.SearchTrendProperties;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ZSetOperations.TypedTuple;
import org.springframework.stereotype.Service;

/**
 * 실시간 인기 검색어 (specs/015 US1, research D-1 ~ D-4).
 *
 * <ul>
 *   <li>검색어는 앞뒤 공백을 지우고 여러 칸 공백을 한 칸으로, 영문은 소문자로 맞춘다(S1-3). 금지어가 들어 있으면 세지 않는다(S1-4)</li>
 *   <li>같은 사람·같은 검색어는 dedupe-window 칸(시계 기준으로 나눈 칸)에 한 번(S1-2). 이 표시의 키는 사람과 검색어를 해시로 묶어, Redis에 둘이 글자로 함께 남지 않는다</li>
 *   <li>10분 칸마다 정렬 집합 {@code search:count:{칸 번호}}에 센다. 순위는 최근 6칸(1시간)의 합, 변화는 한 칸 앞 6칸과 견준다(S1-5)</li>
 *   <li>Redis가 멈추면 세지 않고 순위는 빈 목록이다. 검색은 그대로 된다(S1-6, 잃어도 되는 값)</li>
 * </ul>
 */
@Service
public class SearchTrends {

    private static final Logger log = LoggerFactory.getLogger(SearchTrends.class);
    private static final Pattern SPACES = Pattern.compile("\\p{javaWhitespace}+");

    private final StringRedisTemplate redis;
    private final SearchTrendProperties properties;
    private final Clock clock;

    public SearchTrends(StringRedisTemplate redis, SearchTrendProperties properties, Clock clock) {
        this.redis = redis;
        this.properties = properties;
        this.clock = clock;
    }

    /** change: up(delta칸 올라감) · down(delta칸 내려감) · new · same. */
    public record Trend(int rank, String keyword, String change, int delta) {
    }

    /** 검색어를 순위에 넣을 모양으로. 빈 말이면 빈 문자열. */
    static String normalize(String keyword) {
        if (keyword == null) {
            return "";
        }
        return SPACES.matcher(keyword.strip()).replaceAll(" ").toLowerCase(Locale.ROOT);
    }

    /** visitor: 회원(m:번호)이나 방문자(g:쿠키)를 가리는 값. */
    public void record(String keyword, String visitor) {
        String normalized = normalize(keyword);
        if (normalized.isEmpty() || properties.banned().stream().anyMatch(normalized::contains)) {
            return;
        }
        try {
            // 시계로 정한 칸 번호를 키에 넣는다: 만료는 Redis 시간이라 TTL은 치우기만 한다
            long slot = clock.instant().getEpochSecond() / properties.dedupeWindow().toSeconds();
            String seen = "search:seen:" + sha256(visitor + "\n" + normalized + "\n" + slot);
            if (!Boolean.TRUE.equals(redis.opsForValue().setIfAbsent(seen, "1", properties.dedupeWindow()))) {
                return;
            }
            String key = countKey(currentBucket());
            redis.opsForZSet().incrementScore(key, normalized, 1);
            redis.expire(key, properties.bucket().multipliedBy(properties.windowBuckets() + 1L));
        } catch (RuntimeException e) {
            log.warn("인기 검색어를 세지 못했습니다: {}", e.toString());
        }
    }

    /** 최근 1시간의 순위(size개)와 10분 전과의 변화. */
    public List<Trend> trending() {
        try {
            long now = currentBucket();
            List<String> current = ranking(now);
            List<String> previous = ranking(now - 1);
            List<Trend> trends = new ArrayList<>();
            for (int i = 0; i < current.size(); i++) {
                int rank = i + 1;
                int before = previous.indexOf(current.get(i)) + 1;
                if (before == 0) {
                    trends.add(new Trend(rank, current.get(i), "new", 0));
                } else if (before > rank) {
                    trends.add(new Trend(rank, current.get(i), "up", before - rank));
                } else if (before < rank) {
                    trends.add(new Trend(rank, current.get(i), "down", rank - before));
                } else {
                    trends.add(new Trend(rank, current.get(i), "same", 0));
                }
            }
            return trends;
        } catch (RuntimeException e) {
            log.warn("인기 검색어를 읽지 못했습니다: {}", e.toString());
            return List.of();
        }
    }

    /** last 칸까지 window-buckets칸을 합쳐 많은 순(같으면 가나다순) size개. */
    private List<String> ranking(long last) {
        Map<String, Double> counts = new HashMap<>();
        for (long bucket = last - properties.windowBuckets() + 1; bucket <= last; bucket++) {
            Set<TypedTuple<String>> tuples = redis.opsForZSet().rangeWithScores(countKey(bucket), 0, -1);
            if (tuples == null) {
                continue;
            }
            for (TypedTuple<String> tuple : tuples) {
                if (tuple.getValue() != null && tuple.getScore() != null) {
                    counts.merge(tuple.getValue(), tuple.getScore(), Double::sum);
                }
            }
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue(Comparator.reverseOrder())
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(properties.size())
                .map(Map.Entry::getKey)
                .toList();
    }

    private long currentBucket() {
        return clock.instant().getEpochSecond() / properties.bucket().toSeconds();
    }

    private static String countKey(long bucket) {
        return "search:count:" + bucket;
    }

    private static String sha256(String text) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
