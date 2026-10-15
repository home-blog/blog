package com.myblog.post.controller.dto;

import com.myblog.post.validation.ValidContent;
import com.myblog.post.validation.ValidTitle;
import com.myblog.post.validation.ValidVisibility;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;

/**
 * 임시 저장·발행·예약 요청 본문 (specs/013 contracts 3, 4).
 * 컨트롤러에서 @Valid로 검사하지 않는다: 남의 임시 저장 글이면 형식 오류보다 먼저 404로 답하도록
 * 서비스가 주인을 확인한 <b>뒤에</b> 검사한다 (글 수정과 같은 순서).
 */
public final class DraftRequests {

    private DraftRequests() {
    }

    /** 저장: 비어 있어도 된다. 글자 수 상한과 내 것인지만 본다. */
    public record Save(String title, String content, Long categoryId, Long topicId, Long seriesId, String visibility,
            List<String> tags) {
    }

    /** 발행: 보통 글쓰기와 같은 검사 + 넣을 연재. */
    public record Publish(
            @ValidTitle String title,
            @ValidContent String content,
            @NotNull(message = "CATEGORY_REQUIRED") Long categoryId,
            @NotNull(message = "TOPIC_REQUIRED") Long topicId,
            @ValidVisibility String visibility,
            List<String> tags,
            Long seriesId) {

        public Save toSave() {
            return new Save(title, content, categoryId, topicId, seriesId, visibility, tags);
        }
    }

    /** 예약: 발행과 같은 검사 + 올릴 시각 (초 아래는 버린다). */
    public record Schedule(
            @ValidTitle String title,
            @ValidContent String content,
            @NotNull(message = "CATEGORY_REQUIRED") Long categoryId,
            @NotNull(message = "TOPIC_REQUIRED") Long topicId,
            @ValidVisibility String visibility,
            List<String> tags,
            Long seriesId,
            Instant publishAt) {

        public Publish toPublish() {
            return new Publish(title, content, categoryId, topicId, visibility, tags, seriesId);
        }
    }
}
