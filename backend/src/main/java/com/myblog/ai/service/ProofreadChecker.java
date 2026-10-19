package com.myblog.ai.service;

import com.myblog.ai.service.Proofreader.Suggestion;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * AI가 준 고칠 곳을 믿지 않고 본문에서 다시 찾는다 (specs/014 S1-2, S1-3, D-3, D-4).
 *
 * <ul>
 *   <li>위치는 "앞 글자 + 원문"으로 찾고, 없으면 원문만으로 아직 쓰지 않은 첫 자리를 쓴다. 그래도 없으면 버린다</li>
 *   <li>원문이 비었거나, 고친 말과 같거나, 모르는 종류면 버린다</li>
 *   <li>이미 고른 곳과 겹치거나, 코드 블록·인라인 코드·링크 주소에 걸치면 그 자리는 쓰지 않는다</li>
 * </ul>
 * 위치는 자바 문자열 위치(UTF-16)라 화면의 자바스크립트 문자열 위치와 같다.
 */
public class ProofreadChecker {

    /** 화면에 보이는 이유의 최대 글자 수. 넘으면 자른다. */
    public static final int REASON_MAX = 120;

    /** 원문 하나의 최대 글자 수. 이보다 길면 문단을 통째로 바꾸자는 것이라 받지 않는다. */
    static final int ORIGINAL_MAX = 200;

    static final Set<String> KINDS = Set.of("spacing", "spelling", "style");

    private static final Pattern FENCE = Pattern.compile("^(`{3,}|~{3,})");
    private static final Pattern INLINE_CODE = Pattern.compile("`[^`\\n]+`");
    private static final Pattern LINK_TARGET = Pattern.compile("\\]\\([^)\\n]*\\)");
    private static final Pattern AUTOLINK = Pattern.compile("<https?://[^>\\s]+>");
    private static final Pattern BARE_URL = Pattern.compile("https?://[^\\s)>\\]]+");

    private final int maxCorrections;

    public ProofreadChecker(int maxCorrections) {
        this.maxCorrections = maxCorrections;
    }

    /** 고칠 곳 하나. start·end는 보낸 본문의 UTF-16 위치 [start, end). */
    public record Correction(int start, int end, String kind, String original, String replacement, String reason) {
    }

    public List<Correction> check(String text, List<Suggestion> suggestions) {
        List<int[]> locked = protectedRanges(text);
        List<Correction> accepted = new ArrayList<>();
        for (Suggestion suggestion : suggestions) {
            if (accepted.size() >= maxCorrections) {
                break;
            }
            if (!usable(suggestion)) {
                continue;
            }
            int start = locate(text, suggestion, accepted, locked);
            if (start < 0) {
                continue;
            }
            accepted.add(new Correction(start, start + suggestion.original().length(), suggestion.kind(),
                    suggestion.original(), suggestion.replacement(), reasonOf(suggestion.reason())));
        }
        accepted.sort(Comparator.comparingInt(Correction::start));
        return accepted;
    }

    private static boolean usable(Suggestion suggestion) {
        String original = suggestion.original();
        return suggestion.kind() != null && KINDS.contains(suggestion.kind())
                && original != null && !original.isEmpty() && original.length() <= ORIGINAL_MAX
                && suggestion.replacement() != null && !original.equals(suggestion.replacement());
    }

    private static int locate(String text, Suggestion suggestion, List<Correction> accepted, List<int[]> locked) {
        String original = suggestion.original();
        String context = suggestion.context() == null ? "" : suggestion.context();
        if (!context.isEmpty()) {
            String needle = context + original;
            for (int at = text.indexOf(needle); at >= 0; at = text.indexOf(needle, at + 1)) {
                int start = at + context.length();
                if (free(start, start + original.length(), accepted, locked)) {
                    return start;
                }
            }
        }
        for (int at = text.indexOf(original); at >= 0; at = text.indexOf(original, at + 1)) {
            if (free(at, at + original.length(), accepted, locked)) {
                return at;
            }
        }
        return -1;
    }

    private static boolean free(int start, int end, List<Correction> accepted, List<int[]> locked) {
        for (Correction taken : accepted) {
            if (start < taken.end() && taken.start() < end) {
                return false;
            }
        }
        for (int[] range : locked) {
            if (start < range[1] && range[0] < end) {
                return false;
            }
        }
        return true;
    }

    private static String reasonOf(String reason) {
        String trimmed = reason == null ? "" : reason.strip();
        return trimmed.length() > REASON_MAX ? trimmed.substring(0, REASON_MAX) : trimmed;
    }

    /** 고치지 않는 구간: 코드 블록(``` ~~~), 인라인 코드, 링크·이미지 주소 (…), 자동 링크 <…>, 맨 주소. */
    static List<int[]> protectedRanges(String text) {
        List<int[]> ranges = new ArrayList<>();
        int lineStart = 0;
        int fenceStart = -1;
        String fence = null;
        while (lineStart <= text.length()) {
            int lineEnd = text.indexOf('\n', lineStart);
            int end = lineEnd < 0 ? text.length() : lineEnd;
            String line = text.substring(lineStart, end).stripLeading();
            Matcher marker = FENCE.matcher(line);
            if (fence == null && marker.find()) {
                fence = marker.group(1);
                fenceStart = lineStart;
            } else if (fence != null && line.startsWith(fence)) {
                ranges.add(new int[] {fenceStart, end});
                fence = null;
            }
            if (lineEnd < 0) {
                break;
            }
            lineStart = lineEnd + 1;
        }
        if (fence != null) {
            ranges.add(new int[] {fenceStart, text.length()});
        }
        for (Pattern pattern : List.of(INLINE_CODE, LINK_TARGET, AUTOLINK, BARE_URL)) {
            Matcher matcher = pattern.matcher(text);
            while (matcher.find()) {
                ranges.add(new int[] {matcher.start(), matcher.end()});
            }
        }
        return ranges;
    }
}
