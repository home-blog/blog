package com.myblog.user.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.web.http.CookieSerializer;
import org.springframework.session.web.http.DefaultCookieSerializer;

/**
 * 세션 쿠키 (specs/001 T027, FR-030, NF-10). Spring Session이 이 설정으로 쿠키를 쓴다.
 * - 이름 SESSION, 유효 7일(마지막 사용 기준, 쓸 때마다 SessionLifetimeFilter가 다시 내려 보냄)
 * - 화면의 스크립트가 못 읽게 HttpOnly, 다른 사이트 요청에는 안 실리게 SameSite=Lax, 배포(HTTPS)에서는 Secure
 */
@Configuration
public class SessionConfig {

    public static final String COOKIE_NAME = "SESSION";

    @Bean
    public CookieSerializer cookieSerializer(AuthProperties properties,
            @Value("${server.servlet.session.cookie.secure:false}") boolean secure) {
        DefaultCookieSerializer serializer = new DefaultCookieSerializer();
        serializer.setCookieName(COOKIE_NAME);
        serializer.setCookiePath("/");
        serializer.setCookieMaxAge((int) properties.session().idleTimeout().toSeconds());
        serializer.setUseHttpOnlyCookie(true);
        serializer.setSameSite("Lax");
        serializer.setUseSecureCookie(secure);
        return serializer;
    }
}
