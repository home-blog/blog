package com.myblog.common;

import java.util.Optional;

/**
 * 공개 여부 값 (specs/003 contracts `이 기능의 약속`). 글(post.visibility)과 분류(category.visibility)가 같은 값을 쓴다.
 * 표에는 소문자 글자("public", "private")로 저장한다 (ck_post_visibility, ck_category_visibility).
 */
public enum Visibility {

    PUBLIC("public"),
    PRIVATE("private");

    private final String value;

    Visibility(String value) {
        this.value = value;
    }

    /** 표와 응답에 쓰는 글자. */
    public String value() {
        return value;
    }

    /** 표나 요청의 글자를 읽는다. 두 값이 아니면 비운다. */
    public static Optional<Visibility> from(String value) {
        for (Visibility visibility : values()) {
            if (visibility.value.equals(value)) {
                return Optional.of(visibility);
            }
        }
        return Optional.empty();
    }

    public static boolean isPublic(String value) {
        return PUBLIC.value.equals(value);
    }
}
