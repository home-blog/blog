package com.myblog.post.service;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 글 카드의 썸네일 (specs/008 US1, 2026-10-12 사용자: 티스토리처럼 이미지가 크게 보이게).
 * 본문의 마크다운 이미지 가운데 제목이 "대표"인 것(![설명](주소 "대표")), 없으면 첫 이미지를 쓴다.
 * 표 칸을 더하지 않고 본문만 본다: 글쓰기 화면의 "대표 이미지" 고르기는 그 이미지에 "대표"를 붙인다.
 * 주소는 이 사이트의 경로(/api/images/…)와 http(s) 주소만 쓴다 (javascript:, data:, //다른사이트 는 쓰지 않는다).
 * 코드 블록(```…```, ~~~…~~~)과 인라인 코드(`…`) 안의 이미지 문법은 그림으로 보이지 않으므로 건너뛴다.
 * 화면의 대표 이미지 고르기(frontend/src/post/cover.ts)와 같은 규칙이다.
 */
public final class PostThumbnail {

    /** 대표 이미지 표시. 마크다운 이미지의 제목 칸에 적는다. */
    public static final String COVER_TITLE = "대표";

    /** ![설명](주소) 또는 ![설명](<주소>), 뒤에 "제목"이 붙을 수 있다. */
    private static final Pattern IMAGE = Pattern.compile(
            "!\\[[^\\]\\n]*]\\(\\s*<?([^)\\s>]+)>?(?:\\s+\"([^\"\\n]*)\")?\\s*\\)");

    /** 코드 블록(닫히지 않았으면 글 끝까지)과 인라인 코드. 이 안의 글자는 마크다운으로 그려지지 않는다. */
    private static final Pattern CODE = Pattern.compile(
            "(?ms)^ {0,3}(`{3,}|~{3,}).*?(?:^ {0,3}\\1[^\\n]*$|\\z)|`[^`\\n]+`");

    private PostThumbnail() {
    }

    /** 썸네일 주소. 쓸 수 있는 이미지가 없으면 비어 있다. */
    public static Optional<String> of(String markdown) {
        if (markdown == null || markdown.indexOf("![") < 0) {
            return Optional.empty();
        }
        String first = null;
        Matcher matcher = IMAGE.matcher(CODE.matcher(markdown).replaceAll(" "));
        while (matcher.find()) {
            String src = matcher.group(1);
            if (!usable(src)) {
                continue;
            }
            if (COVER_TITLE.equals(matcher.group(2))) {
                return Optional.of(src);
            }
            if (first == null) {
                first = src;
            }
        }
        return Optional.ofNullable(first);
    }

    static boolean usable(String src) {
        return src.startsWith("https://") || src.startsWith("http://") || (src.startsWith("/") && !src.startsWith("//"));
    }
}
