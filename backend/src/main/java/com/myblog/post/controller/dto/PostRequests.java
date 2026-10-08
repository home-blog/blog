package com.myblog.post.controller.dto;

import com.myblog.post.validation.ValidContent;
import com.myblog.post.validation.ValidTitle;
import com.myblog.post.validation.ValidVisibility;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 글 요청 본문 (specs/003 contracts 10, 13).
 * 작성 시각·블로그 번호·작성자 칸은 두지 않는다: 보내도 무시된다 (FR-008, FR-014). 블로그는 세션의 회원으로 정한다.
 */
public final class PostRequests {

    private PostRequests() {
    }

    /**
     * 글 쓰기. requestKey는 화면이 글쓰기 화면을 열 때 만든 1회용 번호다 (D-6). 같은 번호로 다시 오면 글을 새로 만들지 않는다.
     * 주제가 topic 표에 있는지는 서비스가 본다 (없으면 topicId "주제를 골라 주세요").
     */
    public record Write(
            @ValidTitle String title,
            @ValidContent String content,
            @NotNull(message = "CATEGORY_REQUIRED") Long categoryId,
            @NotNull(message = "TOPIC_REQUIRED") Long topicId,
            @ValidVisibility String visibility,
            @Size(max = 36, message = "VALIDATION_FAILED") String requestKey) {
    }

    /**
     * 글 수정 (contracts 13). 컨트롤러에서 @Valid로 검사하지 않는다: 남의 글이면 형식 오류보다 먼저 404로 답해야 하므로
     * 서비스가 주인을 확인한 <b>뒤에</b> 검사한다 (contracts `요청 검사 순서`).
     */
    public record Update(
            @ValidTitle String title,
            @ValidContent String content,
            @NotNull(message = "CATEGORY_REQUIRED") Long categoryId,
            @NotNull(message = "TOPIC_REQUIRED") Long topicId,
            @ValidVisibility String visibility) {
    }
}
