package com.myblog.post.related;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 글자 조각 임베딩 (specs/014 D-17): 개발·시험에서 열쇠 없이 '뜻 정보' 경로를 돌려 보려고 서버 안에서 벡터를 만든다.
 * <b>배포에서는 쓰지 않는다</b>: 예시 글 154개로 재 보니 다른 블로그 가운데 가장 가까운 글이 같은 주제인 비율이 35%
 * (아무 글이나 고르면 22%)로, 태그·주제로 대신하는 것보다 못했다. 배포에서 뜻으로 고르려면 Ollama(bge-m3)를 켠다.
 *
 * <ul>
 *   <li>한국어 낱말은 조사·어미가 붙어 모양이 바뀌므로(여행을·여행이), 낱말 안의 글자 두 개 조각(여행, 행을)을 함께 센다</li>
 *   <li>첫 줄(제목)은 두 배로 센다. 코드 블록·인라인 코드·주소·링크 주소와 흔한 말(그리고, 있다 …)은 세지 않는다</li>
 *   <li>조각마다 해시로 칸을 정하고(부호도 해시로 정해 겹침의 치우침을 줄인다), 많이 나온 조각은 로그로 줄여 더한 뒤 길이 1로 맞춘다</li>
 * </ul>
 * 뜻은 모르므로 낱말이 겹치지 않는 비슷한 글(바다·해변)은 놓친다. 방식을 바꾸면 model()의 판 번호를 올려 모두 다시 만든다.
 */
public class LocalEmbedder implements Embedder {

    private static final Pattern FENCED_CODE = Pattern.compile("(?s)```.*?```|~~~.*?~~~");
    private static final Pattern INLINE_CODE = Pattern.compile("`[^`\\n]*`");
    private static final Pattern URL = Pattern.compile("https?://\\S+");
    private static final Pattern LINK_TARGET = Pattern.compile("\\]\\([^)\\n]*\\)");
    private static final Pattern WORD = Pattern.compile("[가-힣]+|[a-z0-9]+");

    private static final Set<String> COMMON = Set.of(
            "그리고", "하지만", "그래서", "그러나", "그런데", "또한", "있다", "없다", "했다", "한다", "합니다", "입니다",
            "있습니다", "없습니다", "것이다", "그런", "이런", "저런", "정말", "너무", "아주", "그냥", "오늘", "이번",
            "the", "and", "for", "with", "this", "that", "are", "was", "you", "not", "but");

    private static final double TITLE_WEIGHT = 2;

    private final int dimensions;

    public LocalEmbedder(int dimensions) {
        this.dimensions = dimensions;
    }

    @Override
    public String model() {
        return "local-v1@" + dimensions;
    }

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public List<float[]> embed(List<String> documents) {
        return documents.stream().map(this::embedOne).toList();
    }

    private float[] embedOne(String text) {
        int lineEnd = text.indexOf('\n');
        String title = lineEnd < 0 ? text : text.substring(0, lineEnd);
        String body = lineEnd < 0 ? "" : text.substring(lineEnd + 1);
        Map<String, Double> counts = new HashMap<>();
        count(counts, title, TITLE_WEIGHT);
        count(counts, body, 1);
        float[] vector = new float[dimensions];
        counts.forEach((feature, weight) -> {
            int hash = mix(feature.hashCode());
            float sign = (mix(hash ^ 0x9E3779B9) & 1) == 0 ? 1f : -1f;
            vector[Math.floorMod(hash, dimensions)] += sign * (float) (1 + Math.log(weight));
        });
        return Vectors.normalize(vector);
    }

    private static void count(Map<String, Double> counts, String text, double weight) {
        String plain = FENCED_CODE.matcher(text).replaceAll(" ");
        plain = INLINE_CODE.matcher(plain).replaceAll(" ");
        plain = LINK_TARGET.matcher(plain).replaceAll(" ");
        plain = URL.matcher(plain).replaceAll(" ").toLowerCase(Locale.ROOT);
        Matcher word = WORD.matcher(plain);
        while (word.find()) {
            String w = word.group();
            if (w.length() < 2 || COMMON.contains(w)) {
                continue;
            }
            counts.merge("w:" + w, weight, Double::sum);
            if (w.charAt(0) >= '가') { // 한글 낱말만 조각을 센다
                for (int i = 0; i + 2 <= w.length(); i++) {
                    counts.merge("g:" + w.substring(i, i + 2), weight, Double::sum);
                }
            }
        }
    }

    /** 해시를 고르게 섞는다 (MurmurHash3의 마무리 단계). */
    private static int mix(int h) {
        h ^= h >>> 16;
        h *= 0x85EBCA6B;
        h ^= h >>> 13;
        h *= 0xC2B2AE35;
        h ^= h >>> 16;
        return h;
    }
}
