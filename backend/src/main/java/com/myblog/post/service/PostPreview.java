package com.myblog.post.service;

import com.myblog.post.config.ExploreProperties;
import java.util.regex.Pattern;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 목록·검색 한 줄의 본문 앞부분 (specs/004 T008, research B-3, D-8의 B, FR-005).
 * <ol>
 *   <li>마크다운 기호를 걷어 낸다 (걷어 내는 규칙은 가안, research B-3).</li>
 *   <li>줄바꿈과 이어진 공백을 공백 하나로 바꾼다.</li>
 *   <li>코드 포인트로 세어 설정값(100자)까지 남기고, 넘으면 끝에 "…"를 붙인다.</li>
 * </ol>
 * 결과는 글자 그대로다. 화면이 HTML로 해석하지 않는다.
 */
@Component
public class PostPreview {

    static final String ELLIPSIS = "…";

    private static final Pattern FENCE = Pattern.compile("(?m)^\\s*(```|~~~).*$");
    private static final Pattern IMAGE = Pattern.compile("!\\[([^\\]]*)]\\([^)]*\\)");
    private static final Pattern LINK = Pattern.compile("\\[([^\\]]*)]\\([^)]*\\)");
    private static final Pattern HEADING = Pattern.compile("(?m)^\\s{0,3}#{1,6}\\s+");
    private static final Pattern QUOTE = Pattern.compile("(?m)^\\s{0,3}(>\\s?)+");
    private static final Pattern LIST_MARK = Pattern.compile("(?m)^\\s*([-*+]|\\d+[.)])\\s+(\\[[ xX]]\\s+)?");
    private static final Pattern RULE = Pattern.compile("(?m)^\\s*([-*_]\\s*){3,}$");
    private static final Pattern EMPHASIS = Pattern.compile("(\\*{1,3}|~~)(?=\\S)(.+?)(?<=\\S)\\1");
    /** 밑줄 강조는 단어 안(snake_case)에서는 강조가 아니다 (CommonMark). */
    private static final Pattern UNDERSCORE_EMPHASIS =
            Pattern.compile("(?<![\\p{L}\\p{N}])(_{1,3})(?=\\S)(.+?)(?<=\\S)\\1(?![\\p{L}\\p{N}])");
    private static final Pattern CODE = Pattern.compile("`+");
    private static final Pattern SPACES = Pattern.compile("\\s+");

    private final int maxLength;

    @Autowired
    public PostPreview(ExploreProperties properties) {
        this(properties.list().previewLength());
    }

    PostPreview(int maxLength) {
        this.maxLength = maxLength;
    }

    public String of(String markdown) {
        String text = plainText(markdown);
        int length = text.codePointCount(0, text.length());
        if (length <= maxLength) {
            return text;
        }
        return text.substring(0, text.offsetByCodePoints(0, maxLength)).stripTrailing() + ELLIPSIS;
    }

    /** 마크다운 기호를 걷어 내고 줄바꿈을 공백으로 바꾼 글자. */
    static String plainText(String markdown) {
        if (markdown == null) {
            return "";
        }
        String text = FENCE.matcher(markdown).replaceAll("");
        text = IMAGE.matcher(text).replaceAll("$1");
        text = LINK.matcher(text).replaceAll("$1");
        text = RULE.matcher(text).replaceAll("");
        text = HEADING.matcher(text).replaceAll("");
        text = QUOTE.matcher(text).replaceAll("");
        text = LIST_MARK.matcher(text).replaceAll("");
        text = EMPHASIS.matcher(text).replaceAll("$2");
        text = UNDERSCORE_EMPHASIS.matcher(text).replaceAll("$2");
        text = CODE.matcher(text).replaceAll("");
        return SPACES.matcher(text).replaceAll(" ").strip();
    }
}
