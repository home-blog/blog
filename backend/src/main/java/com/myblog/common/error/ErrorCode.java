package com.myblog.common.error;

import org.springframework.http.HttpStatus;

/**
 * 화면이 상황을 알아보는 영문 이름(code)과, 그대로 보여 줘도 되는 한글 문구(message).
 * 문구는 specs/001 contracts/auth-api.md를 따른다. 서버 내부 정보는 넣지 않는다 (FR-035).
 */
public enum ErrorCode {

    // 공통
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "입력값을 다시 확인해 주세요"),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "※ 로그인이 필요합니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "※ 권한이 없습니다"),
    CSRF_TOKEN_INVALID(HttpStatus.FORBIDDEN, "※ 요청이 만료되었습니다. 새로고침한 뒤 다시 시도해 주세요"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "※ 찾을 수 없습니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "※ 지원하지 않는 요청입니다"),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "※ 잠시 뒤 다시 시도해 주세요"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "※ 잠시 뒤 다시 시도해 주세요"),

    // 칸별 입력 규칙 (VALIDATION_FAILED의 fieldErrors에 들어간다)
    INVALID_EMAIL(HttpStatus.BAD_REQUEST, "이메일 형식이 올바르지 않습니다"),
    INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "닉네임은 한글, 영문, 숫자로 2~10자여야 합니다"),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호는 영문, 숫자, 특수문자를 포함해 8~20자로 입력해 주세요"),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다"),
    REQUIRED(HttpStatus.BAD_REQUEST, "필수 입력 항목입니다"),

    // 이메일 인증, 가입 (specs/001)
    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "이미 가입된 이메일입니다"),
    NICKNAME_ALREADY_USED(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다"),
    RESEND_TOO_SOON(HttpStatus.TOO_MANY_REQUESTS, "잠시 뒤에 다시 요청해 주세요"),
    RESEND_DAILY_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "오늘은 더 이상 인증번호를 보낼 수 없습니다"),
    MAIL_SEND_FAILED(HttpStatus.BAD_GATEWAY, "메일을 보내지 못했습니다. 잠시 뒤 다시 시도해 주세요"),
    CODE_MISMATCH(HttpStatus.BAD_REQUEST, "인증번호가 올바르지 않습니다"),
    CODE_EXPIRED(HttpStatus.BAD_REQUEST, "인증번호가 만료되었습니다. 인증번호를 다시 받아 주세요"),
    CODE_ATTEMPTS_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "인증번호 입력 횟수를 초과했습니다. 인증번호를 다시 받아 주세요"),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "이메일 인증을 먼저 완료해 주세요"),
    VERIFICATION_EXPIRED(HttpStatus.FORBIDDEN, "인증 유효 시간이 지났습니다. 이메일 인증을 다시 해 주세요"),

    // 로그인 (specs/001)
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다"),
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "로그인 시도가 여러 번 실패해 잠겼습니다. 잠시 뒤에 다시 시도해 주세요");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}
