package com.myblog.post.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 연재 설정 (application.yml의 series.*, specs/013 research D-11, 가안). 글자는 코드 포인트로 센다.
 */
@ConfigurationProperties(prefix = "series")
public record SeriesProperties(Name name, Description description, int maxPerBlog, int maxPosts) {

    /** series.name, series.description 칸의 길이 (V12__writing_tools.sql). */
    public static final int NAME_COLUMN_LENGTH = 30;
    public static final int DESCRIPTION_COLUMN_LENGTH = 200;

    public SeriesProperties {
        if (maxPerBlog < 1 || maxPosts < 1) {
            throw new IllegalStateException("series.max-per-blog, max-posts는 1 이상이어야 합니다");
        }
    }

    public record Name(int minLength, int maxLength) {

        public Name {
            if (minLength < 1 || maxLength < minLength || maxLength > NAME_COLUMN_LENGTH) {
                throw new IllegalStateException("series.name 길이는 1~%d이어야 합니다".formatted(NAME_COLUMN_LENGTH));
            }
        }
    }

    public record Description(int maxLength) {

        public Description {
            if (maxLength < 0 || maxLength > DESCRIPTION_COLUMN_LENGTH) {
                throw new IllegalStateException("series.description.max-length는 0~%d이어야 합니다".formatted(DESCRIPTION_COLUMN_LENGTH));
            }
        }
    }
}
