package com.myblog.comment.controller.dto;

import com.myblog.comment.validation.ValidCommentBody;

/** 댓글 요청 본문 (specs/005 contracts 2). 작성자·글 번호는 본문에서 받지 않는다 (세션과 주소로 정한다). */
public final class CommentRequests {

    private CommentRequests() {
    }

    /**
     * 댓글 쓰기. 컨트롤러에서 @Valid로 검사하지 않는다: 볼 수 없는 글이면 형식 오류보다 먼저 404로 답해야 하므로
     * 서비스가 글을 확인한 <b>뒤에</b> 검사한다 (contracts `요청 검사 순서`).
     *
     * @param parentId 답글이면 그 댓글 번호 (specs/019). 답글의 답글이면 서버가 원댓글로 바꾼다
     * @param secret   비밀 댓글(봉한 쪽지). 빼면 거짓. 비밀 댓글에 단 답글은 이 값과 상관없이 비밀
     */
    public record Write(@ValidCommentBody String body, Long parentId, Boolean secret) {

        public Write(String body) {
            this(body, null, null);
        }

        public boolean wantsSecret() {
            return Boolean.TRUE.equals(secret);
        }
    }
}
