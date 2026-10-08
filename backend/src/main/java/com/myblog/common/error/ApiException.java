package com.myblog.common.error;

import java.util.List;

/**
 * 서비스가 "이 요청은 이 이유로 거절"을 알릴 때 던진다. GlobalExceptionHandler가 ErrorResponse로 바꾼다.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String responseMessage;
    private final List<ErrorResponse.FieldErrorItem> fieldErrors;
    private final Long retryAfterSeconds;

    public ApiException(ErrorCode errorCode) {
        this(errorCode, errorCode.message(), List.of());
    }

    public ApiException(ErrorCode errorCode, String responseMessage) {
        this(errorCode, responseMessage, List.of());
    }

    public ApiException(ErrorCode errorCode, String responseMessage, List<ErrorResponse.FieldErrorItem> fieldErrors) {
        super(errorCode.name());
        this.errorCode = errorCode;
        this.responseMessage = responseMessage;
        this.fieldErrors = List.copyOf(fieldErrors);
        this.retryAfterSeconds = null;
    }

    /** "몇 초 뒤에 다시 시도"를 함께 알린다 (예: 로그인 잠금 ACCOUNT_LOCKED). */
    public ApiException(ErrorCode errorCode, String responseMessage, long retryAfterSeconds) {
        super(errorCode.name());
        this.errorCode = errorCode;
        this.responseMessage = responseMessage;
        this.fieldErrors = List.of();
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public ErrorResponse toResponse() {
        return new ErrorResponse(errorCode.name(), responseMessage, fieldErrors, retryAfterSeconds);
    }
}
