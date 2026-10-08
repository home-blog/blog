package com.myblog.comment.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 댓글 내용 (community.comment.*, FR-002). 저장할 모양(Comment.normalizeBody: 줄바꿈 맞춤, 앞뒤 공백 지움)으로 센다.
 * 비면 COMMENT_EMPTY, 길면 COMMENT_TOO_LONG. 글자는 코드 포인트로 센다 (research B-2).
 */
@Documented
@Target({ElementType.FIELD, ElementType.PARAMETER, ElementType.RECORD_COMPONENT})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = CommentBodyValidator.class)
public @interface ValidCommentBody {

    String message() default "COMMENT_EMPTY";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
