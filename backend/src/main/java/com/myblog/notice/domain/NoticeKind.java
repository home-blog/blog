package com.myblog.notice.domain;

import java.util.Arrays;
import java.util.Optional;

/** 공지사항 종류 (specs/012 D-4). 표와 API에는 소문자 이름으로 쓴다. */
public enum NoticeKind {
    /** 서비스가 바뀐 소식. 릴리스 번호가 꼭 있다 */
    UPDATE("update"),
    /** 점검 안내, 이용 규칙 같은 일반 알림. 릴리스 번호가 없다 */
    NOTICE("notice");

    private final String value;

    NoticeKind(String value) {
        this.value = value;
    }

    public String value() {
        return value;
    }

    /** 대소문자까지 정확히 맞아야 한다. 모르는 값이면 비운다. */
    public static Optional<NoticeKind> from(String value) {
        return Arrays.stream(values()).filter(kind -> kind.value.equals(value)).findFirst();
    }
}
