package com.myblog.comment.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 댓글 신고·자동 가림의 숫자 (application.yml의 moderation.comment.*, specs/022 US1).
 * autoHideCount: 서로 다른 회원의 대기 신고가 이만큼 모이면 저절로 가린다. reportDetailMaxLength: 기타 설명 글자 수(코드 포인트).
 * excerptLength: 편집실 목록의 댓글 앞부분 글자 수.
 */
@ConfigurationProperties(prefix = "moderation.comment")
public record CommentModerationProperties(int autoHideCount, int reportDetailMaxLength, int excerptLength) {

    /** comment_report.detail VARCHAR(200). */
    public static final int DETAIL_COLUMN_LENGTH = 200;

    public CommentModerationProperties {
        if (autoHideCount < 1) {
            throw new IllegalStateException("moderation.comment.auto-hide-count는 1 이상이어야 합니다");
        }
        if (reportDetailMaxLength < 0 || reportDetailMaxLength > DETAIL_COLUMN_LENGTH) {
            throw new IllegalStateException("moderation.comment.report-detail-max-length는 0 이상 %d 이하여야 합니다"
                    .formatted(DETAIL_COLUMN_LENGTH));
        }
        if (excerptLength < 1) {
            throw new IllegalStateException("moderation.comment.excerpt-length는 1 이상이어야 합니다");
        }
    }
}
