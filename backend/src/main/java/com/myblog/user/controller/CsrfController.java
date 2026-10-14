package com.myblog.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/v1/auth/csrf: 화면이 변경 요청에 실을 CSRF 토큰을 받는다 (contracts 1, NF-11).
 * 화면은 처음 열릴 때와 로그인 뒤에 다시 받는다.
 */
@Tag(name = "1. 로그인·가입")
@RestController
public class CsrfController {

    @Operation(summary = "CSRF 토큰 받기", description = "화면이 처음 열릴 때와 로그인 뒤에 받는다. 글쓰기처럼 바꾸는 요청에는 쿠키 XSRF-TOKEN 값을 머리글 X-XSRF-TOKEN에 실어 보낸다.")
    @GetMapping("/api/{version}/auth/csrf")
    public CsrfResponse csrf(@Parameter(hidden = true) CsrfToken csrfToken) {
        return new CsrfResponse(csrfToken.getHeaderName(), csrfToken.getToken());
    }

    public record CsrfResponse(String headerName, String token) {
    }
}
