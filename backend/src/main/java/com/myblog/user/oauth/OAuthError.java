package com.myblog.user.oauth;

import org.springframework.http.HttpStatus;

/**
 * OAuth 끝점의 오류 (RFC 6749 5.2, RFC 7591 3.2.2). 응답은 {@code {"error", "error_description"}}.
 * error는 아래 정해진 낱말만 쓰고, 설명도 서버가 정한 글이라 사용자 입력이 섞이지 않는다.
 */
public class OAuthError extends RuntimeException {

    private final HttpStatus status;
    private final String error;

    public OAuthError(HttpStatus status, String error, String description) {
        super(description);
        this.status = status;
        this.error = error;
    }

    public static OAuthError badRequest(String error, String description) {
        return new OAuthError(HttpStatus.BAD_REQUEST, error, description);
    }

    public static OAuthError invalidGrant(String description) {
        return badRequest("invalid_grant", description);
    }

    public static OAuthError unavailable() {
        return new OAuthError(HttpStatus.SERVICE_UNAVAILABLE, "temporarily_unavailable", "잠시 뒤 다시 시도해 주세요");
    }

    public HttpStatus status() {
        return status;
    }

    public String error() {
        return error;
    }
}
