package com.myblog.post.config;

import java.time.Duration;
import java.time.ZoneId;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 글쓰기 도구 설정 (application.yml의 writing.*, specs/013 research D-5, D-6, D-12, 가안).
 * 임시 저장 개수·자동 저장 간격, 예약 범위·확인 간격, 잔디 기간·시간대.
 */
@ConfigurationProperties(prefix = "writing")
public record WritingProperties(Draft draft, Schedule schedule, Activity activity) {

    /** post_draft.tags 칸의 길이 (V12__writing_tools.sql의 VARCHAR(200)). */
    public static final int TAGS_COLUMN_LENGTH = 200;

    public record Draft(int maxCount, int autosaveSeconds, int tagsMaxLength) {

        public Draft {
            if (maxCount < 1 || autosaveSeconds < 1) {
                throw new IllegalStateException("writing.draft.max-count, autosave-seconds는 1 이상이어야 합니다");
            }
            if (tagsMaxLength < 1 || tagsMaxLength > TAGS_COLUMN_LENGTH) {
                throw new IllegalStateException("writing.draft.tags-max-length는 1~%d이어야 합니다".formatted(TAGS_COLUMN_LENGTH));
            }
        }
    }

    public record Schedule(int maxDays, Duration minLead, Duration checkInterval, int batchSize) {

        public Schedule {
            if (maxDays < 1 || batchSize < 1 || minLead == null || checkInterval == null || minLead.isNegative()) {
                throw new IllegalStateException("writing.schedule 값을 확인해 주세요");
            }
        }
    }

    public record Activity(int days, ZoneId zone) {

        public Activity {
            if (days < 1 || zone == null) {
                throw new IllegalStateException("writing.activity.days는 1 이상, zone은 있어야 합니다");
            }
        }
    }
}
