package com.myblog.common.error;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.QueryTimeoutException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 모든 예외를 공통 오류 모양(ErrorResponse)으로 바꾼다.
 * 응답에는 예외 이름, 쿼리, 경로를 넣지 않는다. 자세한 내용은 서버 로그에만 남긴다 (FR-035, NF-06).
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    private final List<FieldErrorMessages> fieldErrorMessages;

    public GlobalExceptionHandler(List<FieldErrorMessages> fieldErrorMessages) {
        this.fieldErrorMessages = fieldErrorMessages;
    }

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ErrorResponse> handleApi(ApiException e) {
        return ResponseEntity.status(e.errorCode().status()).body(e.toResponse());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException e) {
        List<ErrorResponse.FieldErrorItem> items = new ArrayList<>();
        for (FieldError fieldError : e.getBindingResult().getFieldErrors()) {
            items.add(toItem(fieldError.getField(), fieldError.getDefaultMessage()));
        }
        for (ObjectError globalError : e.getBindingResult().getGlobalErrors()) {
            items.add(toItem(globalError.getObjectName(), globalError.getDefaultMessage()));
        }
        return badRequest(ErrorResponse.of(ErrorCode.VALIDATION_FAILED, items));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleUnreadable(HttpMessageNotReadableException e) {
        return badRequest(ErrorResponse.of(ErrorCode.VALIDATION_FAILED));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NoResourceFoundException e) {
        return respond(ErrorCode.NOT_FOUND);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethod(HttpRequestMethodNotSupportedException e) {
        return respond(ErrorCode.METHOD_NOT_ALLOWED);
    }

    /** DB나 Redis에 연결할 수 없거나, 연결된 채로 응답이 없을 때(Redis가 도중에 꺼지면 시간 초과로 온다, quickstart S-10). */
    @ExceptionHandler({DataAccessResourceFailureException.class, QueryTimeoutException.class})
    public ResponseEntity<ErrorResponse> handleUnavailable(Exception e) {
        log.error("저장소에 연결할 수 없습니다", e);
        return respond(ErrorCode.SERVICE_UNAVAILABLE);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleUnexpected(Exception e) {
        log.error("처리하지 못한 오류", e);
        return respond(ErrorCode.INTERNAL_ERROR);
    }

    /** 검사 어노테이션의 message에는 ErrorCode 이름을 적는다. 이름이 아니면 VALIDATION_FAILED로 본다. */
    private ErrorResponse.FieldErrorItem toItem(String field, String codeName) {
        ErrorCode code = parse(codeName);
        return new ErrorResponse.FieldErrorItem(field, code.name(), messageFor(code));
    }

    private String messageFor(ErrorCode code) {
        for (FieldErrorMessages messages : fieldErrorMessages) {
            Optional<String> message = messages.messageFor(code);
            if (message.isPresent()) {
                return message.get();
            }
        }
        return code.message();
    }

    private static ErrorCode parse(String codeName) {
        if (codeName == null) {
            return ErrorCode.VALIDATION_FAILED;
        }
        try {
            return ErrorCode.valueOf(codeName);
        } catch (IllegalArgumentException e) {
            return ErrorCode.VALIDATION_FAILED;
        }
    }

    private static ResponseEntity<ErrorResponse> badRequest(ErrorResponse body) {
        return ResponseEntity.badRequest().body(body);
    }

    private static ResponseEntity<ErrorResponse> respond(ErrorCode code) {
        return ResponseEntity.status(code.status()).body(ErrorResponse.of(code));
    }
}
