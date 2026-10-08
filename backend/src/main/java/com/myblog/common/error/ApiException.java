package com.myblog.common.error;

import java.util.List;

/**
 * 서비스가 "이 요청은 이 이유로 거절"을 알릴 때 던진다. GlobalExceptionHandler가 ErrorResponse로 바꾼다.
 */
public class ApiException extends RuntimeException {

    private final ErrorCode errorCode;
    private final String responseMessage;
    private final List<ErrorResponse.FieldErrorItem> fieldErrors;

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
    }

    public ErrorCode errorCode() {
        return errorCode;
    }

    public ErrorResponse toResponse() {
        return new ErrorResponse(errorCode.name(), responseMessage, fieldErrors);
    }
}
