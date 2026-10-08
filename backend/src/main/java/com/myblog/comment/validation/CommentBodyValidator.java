package com.myblog.comment.validation;

import com.myblog.comment.config.CommentProperties;
import com.myblog.comment.domain.Comment;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/** @ValidCommentBody 검사 (specs/005 T017). */
public class CommentBodyValidator implements ConstraintValidator<ValidCommentBody, String> {

    private final CommentProperties properties;

    public CommentBodyValidator(CommentProperties properties) {
        this.properties = properties;
    }

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        String body = Comment.normalizeBody(value);
        int length = body.codePointCount(0, body.length());
        if (length == 0 || length < properties.minLength()) {
            return reject(context, "COMMENT_EMPTY");
        }
        if (length > properties.maxLength()) {
            return reject(context, "COMMENT_TOO_LONG");
        }
        return true;
    }

    private static boolean reject(ConstraintValidatorContext context, String errorCode) {
        context.disableDefaultConstraintViolation();
        context.buildConstraintViolationWithTemplate(errorCode).addConstraintViolation();
        return false;
    }
}
