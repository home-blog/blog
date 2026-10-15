package com.myblog.notice.config;

import java.util.Arrays;
import java.util.regex.Pattern;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 공지사항 설정 (application.yml의 notice.*, specs/012 research D-5, D-10).
 * 글자는 코드 포인트로 센다(이모지 하나 = 한 글자). 릴리스 링크는 번호만 저장하고 이 틀로 만든다.
 * GitHub 릴리스는 {@code releaseUrlFrom}(0.4.0)부터 있어서, 그 전 번호에는 링크를 붙이지 않는다.
 */
@ConfigurationProperties(prefix = "notice")
public record NoticeProperties(Title title, Content content, int pageSize, String releaseUrl, String releaseUrlFrom) {

    /** notice.title 칸의 길이 (V11__notice.sql의 VARCHAR(100)). */
    public static final int TITLE_COLUMN_LENGTH = 100;

    private static final Pattern VERSION = Pattern.compile("\\d{1,4}\\.\\d{1,4}\\.\\d{1,4}");

    public NoticeProperties {
        if (pageSize < 1) {
            throw new IllegalStateException("notice.page-size는 1 이상이어야 합니다");
        }
        if (releaseUrl == null || !releaseUrl.contains("{version}")) {
            throw new IllegalStateException("notice.release-url에는 {version}이 있어야 합니다");
        }
        if (releaseUrlFrom == null || !VERSION.matcher(releaseUrlFrom).matches()) {
            throw new IllegalStateException("notice.release-url-from은 0.4.0 모양이어야 합니다");
        }
    }

    public record Title(int maxLength) {

        public Title {
            if (maxLength < 1 || maxLength > TITLE_COLUMN_LENGTH) {
                throw new IllegalStateException("notice.title.max-length는 1~%d이어야 합니다".formatted(TITLE_COLUMN_LENGTH));
            }
        }
    }

    public record Content(int maxLength) {

        public Content {
            if (maxLength < 1) {
                throw new IllegalStateException("notice.content.max-length는 1 이상이어야 합니다");
            }
        }
    }

    /** 릴리스 번호(0.6.0)의 GitHub 릴리스 주소. 릴리스를 만들기 전 번호면 비운다. */
    public String releaseUrlOf(String version) {
        if (!VERSION.matcher(version).matches() || compare(version, releaseUrlFrom) < 0) {
            return null;
        }
        return releaseUrl.replace("{version}", version);
    }

    /** 릴리스 번호를 자리마다 숫자로 견준다 (0.10.0은 0.4.0보다 뒤). */
    private static int compare(String left, String right) {
        int[] a = Arrays.stream(left.split("\\.")).mapToInt(Integer::parseInt).toArray();
        int[] b = Arrays.stream(right.split("\\.")).mapToInt(Integer::parseInt).toArray();
        return Arrays.compare(a, b);
    }
}
