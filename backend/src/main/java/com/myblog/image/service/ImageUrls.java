package com.myblog.image.service;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 이미지의 바깥 주소 (/api/images/{UUID}.{확장자}, contracts 10·11, D-4). 저장소가 MinIO든 디스크든 같다.
 * 본문에서는 <b>우리 서버 주소의 마크다운 이미지</b>(![설명](/api/images/…))만 찾는다. 다른 사이트 주소는 연결하지 않는다.
 */
public final class ImageUrls {

    public static final String PATH = "/api/images/";

    /** 서버가 만드는 파일 이름 모양. 이 모양이 아니면 없는 이미지로 본다. */
    private static final Pattern FILE_NAME = Pattern.compile(
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(?:jpg|png|gif|webp)");

    private static final Pattern MARKDOWN_IMAGE = Pattern.compile(
            "!\\[[^\\]\\n]*\\]\\(\\s*<?" + Pattern.quote(PATH) + "(" + FILE_NAME.pattern() + ")");

    private ImageUrls() {
    }

    public static String of(String fileName) {
        return PATH + fileName;
    }

    public static boolean isFileName(String value) {
        return value != null && FILE_NAME.matcher(value).matches();
    }

    /** 본문 속 우리 이미지의 파일 이름들 (처음 나온 순서, 겹치면 하나). */
    public static Set<String> fileNamesIn(String markdown) {
        Set<String> names = new LinkedHashSet<>();
        if (markdown == null) {
            return names;
        }
        Matcher matcher = MARKDOWN_IMAGE.matcher(markdown);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }
}
