package com.myblog.stats.service;

import com.myblog.stats.config.StatsProperties;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.security.SecureRandom;
import java.util.HexFormat;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * "같은 사람"을 알아본다 (specs/006 T052, D-2 A, data-model 8·9, research R-2, FR-033, FR-034).
 * <ul>
 *   <li>로그인했으면 회원 번호: {@code m:{번호}}.</li>
 *   <li>아니면 무작위 방문자 쿠키: {@code g:{값}}. 쿠키가 없거나 모양이 틀리면 추측할 수 없는 새 값을 만들어 지금 응답에 내려 준다
 *       (HttpOnly, SameSite=Lax, 배포에서는 Secure, 유지 stats.visitor.cookie-max-age). IP 같은 개인 정보는 쓰지 않고, 서버에 저장하지 않는다.</li>
 * </ul>
 * 이벤트 리스너가 요청 안에서 돌므로 지금 요청·응답은 RequestContextHolder로 얻는다.
 */
@Component
public class VisitorKeyResolver {

    /** 쿠키 값: 16바이트 무작위 = 소문자 16진수 32자. */
    private static final int TOKEN_BYTES = 16;
    private static final String TOKEN_PATTERN = "[0-9a-f]{32}";

    private final SecureRandom random = new SecureRandom();
    private final StatsProperties.Visitor properties;
    private final boolean secure;

    public VisitorKeyResolver(StatsProperties properties,
            @Value("${server.servlet.session.cookie.secure:false}") boolean secure) {
        this.properties = properties.visitor();
        this.secure = secure;
    }

    /** 요청 밖(요청·응답이 없음)이면 비어 있다: 그때는 세지 않는다. */
    public Optional<String> resolve(Long memberId) {
        if (memberId != null) {
            return Optional.of("m:" + memberId);
        }
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)
                || attributes.getResponse() == null) {
            return Optional.empty();
        }
        return Optional.of("g:" + visitorToken(attributes.getRequest(), attributes.getResponse()));
    }

    private String visitorToken(HttpServletRequest request, HttpServletResponse response) {
        Optional<String> existing = readCookie(request);
        if (existing.isPresent()) {
            return existing.get();
        }
        String token = (String) request.getAttribute(VisitorKeyResolver.class.getName());
        if (token == null) {
            byte[] bytes = new byte[TOKEN_BYTES];
            random.nextBytes(bytes);
            token = HexFormat.of().formatHex(bytes);
            // 한 요청에서 두 번 불려도(글 상세 = 조회 + 방문) 같은 값, 쿠키는 한 번만
            request.setAttribute(VisitorKeyResolver.class.getName(), token);
            response.addHeader("Set-Cookie", ResponseCookie.from(properties.cookieName(), token)
                    .path("/")
                    .maxAge(properties.cookieMaxAge())
                    .httpOnly(true)
                    .secure(secure)
                    .sameSite("Lax")
                    .build().toString());
        }
        return token;
    }

    private Optional<String> readCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (properties.cookieName().equals(cookie.getName()) && cookie.getValue() != null
                    && cookie.getValue().matches(TOKEN_PATTERN)) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }
}
