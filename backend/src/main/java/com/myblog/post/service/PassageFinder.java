package com.myblog.post.service;

import com.myblog.post.PostLibrary.Passage;
import com.myblog.post.PostLibrary.PassageOptions;
import com.myblog.post.repository.PostLibraryRepository.Found;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 단락 찾기 (RAG의 "찾기", specs/020 research D-3). 서버는 맞는 단락을 출처와 함께 고르기만 하고, 답은 사용자의 AI가 쓴다.
 * <ul>
 *   <li>단락: 마크다운을 빈 줄로 나눈 덩어리. 코드 블록(```, ~~~)은 한 덩어리, 이미지만 있는 줄은 뺀다.</li>
 *   <li>점수: 낱말마다 그 단락에 나온 횟수(최대 3) × 드문 정도(후보 글 중 몇 개에 나오나의 역수 로그),
 *       제목에 있는 낱말이면 그 글의 단락마다 조금 더한다.</li>
 *   <li>글마다 perPost개, 전체 limit개. 길면 처음 맞은 곳 둘레로 maxLength자만 자른다.</li>
 * </ul>
 * 상태가 없는 계산이라 단위 테스트로 본다.
 */
final class PassageFinder {

    private static final Pattern IMAGE_LINE = Pattern.compile("^\\s*!\\[[^\\]]*]\\([^)]*\\)\\s*$");
    private static final int MAX_COUNT = 3;
    private static final double TITLE_BONUS = 0.5;
    private static final String ELLIPSIS = "…";

    private PassageFinder() {
    }

    static List<Passage> find(List<Found> candidates, List<String> words, PassageOptions options) {
        List<String> lowered = words.stream().map(word -> word.toLowerCase(Locale.ROOT)).distinct().toList();
        Map<String, Double> idf = idf(candidates, lowered);
        List<Passage> scored = new ArrayList<>();
        for (Found post : candidates) {
            String title = post.title().toLowerCase(Locale.ROOT);
            double titleBonus = lowered.stream().filter(title::contains).mapToDouble(word -> TITLE_BONUS * idf.get(word))
                    .sum();
            for (Block block : blocks(post.content())) {
                String text = block.text().toLowerCase(Locale.ROOT);
                double score = 0;
                for (String word : lowered) {
                    score += Math.min(count(text, word), MAX_COUNT) * idf.get(word);
                }
                if (score > 0) {
                    scored.add(new Passage(post.postId(), post.title(), post.blogName(), "/posts/" + post.postId(),
                            block.index(), clip(block.text(), lowered, options.maxLength()), score + titleBonus));
                }
            }
        }
        // 점수 높은 순, 같으면 후보 순서(최근 글 먼저)와 글 안의 앞 단락 먼저
        Map<Long, Integer> order = new HashMap<>();
        for (int i = 0; i < candidates.size(); i++) {
            order.put(candidates.get(i).postId(), i);
        }
        scored.sort(Comparator.comparingDouble(Passage::score).reversed()
                .thenComparing(passage -> order.get(passage.postId()))
                .thenComparingInt(Passage::paragraph));
        Map<Long, Integer> perPost = new HashMap<>();
        List<Passage> picked = new ArrayList<>();
        for (Passage passage : scored) {
            if (picked.size() >= options.limit()) {
                break;
            }
            int taken = perPost.merge(passage.postId(), 1, Integer::sum);
            if (taken <= options.perPost()) {
                picked.add(passage);
            }
        }
        return picked;
    }

    /** 낱말마다 드문 정도: ln(1 + (n + 1) / (df + 0.5)). 모든 후보에 나오는 낱말도 0보다 크다. */
    private static Map<String, Double> idf(List<Found> candidates, List<String> words) {
        Map<String, Double> idf = new HashMap<>();
        int n = candidates.size();
        for (String word : words) {
            long df = candidates.stream()
                    .filter(post -> (post.title() + "\n" + post.content()).toLowerCase(Locale.ROOT).contains(word))
                    .count();
            idf.put(word, Math.log(1 + (n + 1) / (df + 0.5)));
        }
        return idf;
    }

    /** 마크다운을 단락으로 나눈다. index는 1부터, 빈 덩어리·이미지만 있는 덩어리는 세지 않는다. */
    static List<Block> blocks(String markdown) {
        List<Block> blocks = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        String fence = null;
        for (String line : markdown.replace("\r\n", "\n").replace('\r', '\n').split("\n", -1)) {
            String trimmed = line.strip();
            if (fence != null) {
                current.append(line).append('\n');
                if (trimmed.startsWith(fence)) {
                    fence = null;
                    flush(blocks, current);
                }
                continue;
            }
            if (trimmed.startsWith("```") || trimmed.startsWith("~~~")) {
                flush(blocks, current);
                fence = trimmed.substring(0, 3);
                current.append(line).append('\n');
                continue;
            }
            if (trimmed.isEmpty()) {
                flush(blocks, current);
            } else if (!IMAGE_LINE.matcher(line).matches()) {
                current.append(line).append('\n');
            }
        }
        flush(blocks, current);
        return blocks;
    }

    private static void flush(List<Block> blocks, StringBuilder current) {
        String text = current.toString().strip();
        current.setLength(0);
        if (!text.isEmpty()) {
            blocks.add(new Block(blocks.size() + 1, text));
        }
    }

    private static int count(String text, String word) {
        int count = 0;
        for (int at = text.indexOf(word); at >= 0 && count < MAX_COUNT; at = text.indexOf(word, at + word.length())) {
            count++;
        }
        return count;
    }

    /** 길면 처음 맞은 낱말 둘레로 maxLength자(코드 포인트)만 남기고 잘린 쪽에 …를 붙인다. */
    static String clip(String text, List<String> lowered, int maxLength) {
        int length = text.codePointCount(0, text.length());
        if (length <= maxLength) {
            return text;
        }
        String lower = text.toLowerCase(Locale.ROOT);
        int hit = lowered.stream().mapToInt(lower::indexOf).filter(at -> at >= 0).min().orElse(0);
        int hitPoint = text.codePointCount(0, Math.min(hit, text.length()));
        int startPoint = Math.max(0, Math.min(hitPoint - maxLength / 3, length - maxLength));
        int start = text.offsetByCodePoints(0, startPoint);
        int end = text.offsetByCodePoints(start, maxLength);
        return (start > 0 ? ELLIPSIS : "") + text.substring(start, end).strip() + (end < text.length() ? ELLIPSIS : "");
    }

    record Block(int index, String text) {
    }
}
