package com.myblog.community.domain;

import java.util.Arrays;
import java.util.Optional;

/** 신고 사유 (FR-018). DB의 ck_post_report_reason과 같은 네 값이다. */
public enum ReportReason {
    /** 스팸 */
    SPAM,
    /** 욕설·혐오 */
    ABUSE,
    /** 음란물 */
    ADULT,
    /** 기타. 이 사유일 때만 설명을 저장한다 (research B-10). */
    OTHER;

    /** 대소문자까지 정확히 같은 이름만 받는다. 없거나 모르는 값이면 비어 있다. */
    public static Optional<ReportReason> parse(String value) {
        return Arrays.stream(values()).filter(reason -> reason.name().equals(value)).findFirst();
    }
}
