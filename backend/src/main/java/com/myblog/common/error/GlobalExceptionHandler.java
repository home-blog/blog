package com.myblog.common.error;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import java.util.ArrayList;
import java.util.Comparator;
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
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
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

    /** 서비스가 직접 검사한 결과 (예: 주인 확인 뒤에 검사하는 글 수정, specs/003 contracts 13). 모양은 @Valid와 같다. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException e) {
        List<ErrorResponse.FieldErrorItem> items = new ArrayList<>();
        for (ConstraintViolation<?> violation : e.getConstraintViolations()) {
            String field = null;
            for (Path.Node node : violation.getPropertyPath()) {
                field = node.getName();
            }
            items.add(toItem(field, violation.getMessage()));
        }
        items.sort(Comparator.comparing(ErrorResponse.FieldErrorItem::field, Comparator.nullsLast(Comparator.naturalOrder())));
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

    /**
     * 숫자 자리에 글자가 온 경우. 주소 안의 번호(/api/blogs/abc)면 그런 주소가 없는 것이므로 404,
     * 물음표 뒤의 값(?page=abc)이면 입력값이 틀린 것이므로 400 VALIDATION_FAILED.
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        if (e.getParameter().hasParameterAnnotation(PathVariable.class)) {
            return respond(ErrorCode.NOT_FOUND);
        }
        return badRequest(ErrorResponse.of(ErrorCode.VALIDATION_FAILED));
    }

    /**
     * 파일이 업로드 크기 설정(community.image.max-size)을 넘으면 프레임워크가 우리 코드보다 먼저 거절한다.
     * 그때도 서비스가 직접 거절할 때와 같은 400 INVALID_IMAGE와 같은 문구(설정값에 맞춘 것)로 답한다 (specs/005 R-6).
     */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleTooLarge(MaxUploadSizeExceededException e) {
        ErrorCode code = ErrorCode.INVALID_IMAGE;
        return ResponseEntity.status(code.status()).body(ErrorResponse.of(code, messageFor(code)));
    }

    /** multipart 모양이 아닌 요청 등 파일을 읽을 수 없는 요청. */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<ErrorResponse> handleMultipart(MultipartException e) {
        return badRequest(ErrorResponse.of(ErrorCode.VALIDATION_FAILED));
    }

    /**
     * 꼭 있어야 하는 물음표 뒤의 값이 없는 경우 (specs/006 T007, NF-06).
     * 서버 오류(500)가 아니라 입력값이 틀린 것이다. 내부 정보 없이 400 VALIDATION_FAILED.
     */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleBadParameter(MissingServletRequestParameterException e) {
        return badRequest(ErrorResponse.of(ErrorCode.VALIDATION_FAILED));
    }

    /** 주소에 온 값이 값 검사(@Min 등)에 걸리면 400. 서버가 돌려줄 값이 검사에 걸린 것이면 서버 오류(500)다. */
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorResponse> handleMethodValidation(HandlerMethodValidationException e) {
        if (e.isForReturnValue()) {
            return handleUnexpected(e);
        }
        return badRequest(ErrorResponse.of(ErrorCode.VALIDATION_FAILED));
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
