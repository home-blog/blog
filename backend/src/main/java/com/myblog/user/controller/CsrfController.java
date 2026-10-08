package com.myblog.user.controller;

import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * GET /api/auth/csrf: 화면이 변경 요청에 실을 CSRF 토큰을 받는다 (contracts 1, NF-11).
 * 화면은 처음 열릴 때와 로그인 뒤에 다시 받는다.
 */
@RestController
public class CsrfController {

    @GetMapping("/api/auth/csrf")
    public CsrfResponse csrf(CsrfToken csrfToken) {
        return new CsrfResponse(csrfToken.getHeaderName(), csrfToken.getToken());
    }

    public record CsrfResponse(String headerName, String token) {
    }
}
