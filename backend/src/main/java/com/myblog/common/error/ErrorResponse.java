package com.myblog.common.error;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/**
 * 모든 오류 응답의 모양 (contracts/auth-api.md `공통 약속`).
 * fieldErrors는 칸마다 어긴 규칙이 있을 때만, retryAfterSeconds는 "몇 초 뒤에 다시"가 있을 때만(예: 로그인 잠금) 들어간다.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ErrorResponse(String code, String message, List<FieldErrorItem> fieldErrors, Long retryAfterSeconds) {

    public ErrorResponse(String code, String message, List<FieldErrorItem> fieldErrors) {
        this(code, message, fieldErrors, null);
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return new ErrorResponse(errorCode.name(), errorCode.message(), List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(errorCode.name(), message, List.of());
    }

    public static ErrorResponse of(ErrorCode errorCode, List<FieldErrorItem> fieldErrors) {
        return new ErrorResponse(errorCode.name(), errorCode.message(), List.copyOf(fieldErrors));
    }

    /** 칸 하나의 오류. */
    public record FieldErrorItem(String field, String code, String message) {
    }
}
