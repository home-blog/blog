package com.myblog.comment.controller.dto;

import com.myblog.comment.validation.ValidCommentBody;

/** 댓글 요청 본문 (specs/005 contracts 2). 작성자·글 번호는 본문에서 받지 않는다 (세션과 주소로 정한다). */
public final class CommentRequests {

    private CommentRequests() {
    }

    /**
     * 댓글 쓰기. 컨트롤러에서 @Valid로 검사하지 않는다: 볼 수 없는 글이면 형식 오류보다 먼저 404로 답해야 하므로
     * 서비스가 글을 확인한 <b>뒤에</b> 검사한다 (contracts `요청 검사 순서`).
     */
    public record Write(@ValidCommentBody String body) {
    }
}
