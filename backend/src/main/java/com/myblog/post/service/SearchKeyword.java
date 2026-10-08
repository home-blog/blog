package com.myblog.post.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * 검색어 다듬기 한곳 (specs/004 T027, research B-4, B-5, R-1, FR-010, FR-012, FR-018).
 * <ul>
 *   <li>앞뒤 공백(전각 공백 포함)을 지운 값이 실제로 찾는 검색어다. 길이는 지운 뒤 코드 포인트로 센다(가운데 공백 포함).</li>
 *   <li>공백 문자(스페이스, 탭, 전각 공백)로 단어를 나누고, 빈 조각은 버리고, 같은 단어는 하나로 합친다.</li>
 *   <li>단어마다 {@code \ % _}를 일반 글자로 바꾸고 앞뒤에 {@code %}를 붙인 "포함" 모양을 만든다 (ESCAPE '\').</li>
 * </ul>
 * 길이 검사(2자 이상, 50자 이하)는 서비스가 설정값으로 한다.
 */
public record SearchKeyword(String keyword, List<String> words) {

    private static final Pattern BLANKS = Pattern.compile("\\p{javaWhitespace}+");

    public static SearchKeyword of(String raw) {
        String keyword = raw == null ? "" : raw.strip();
        Set<String> words = new LinkedHashSet<>();
        for (String word : BLANKS.split(keyword)) {
            if (!word.isEmpty()) {
                words.add(word);
            }
        }
        return new SearchKeyword(keyword, List.copyOf(words));
    }

    /** 검색어 글자 수 (코드 포인트, 가운데 공백 포함). */
    public int length() {
        return keyword.codePointCount(0, keyword.length());
    }

    /** 단어마다 "제목이나 본문에 들어 있다"를 찾는 모양 (ILIKE … ESCAPE '\'에 값으로 넘긴다). */
    public List<String> containsPatterns() {
        return words.stream().map(word -> "%" + escapeLike(word) + "%").toList();
    }

    static String escapeLike(String word) {
        return word.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
