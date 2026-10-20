package com.myblog.user.oauth;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * 인가 서버의 주소(issuer)와 보호 자원(MCP 주소) (specs/023 D-6). 설정 mcp.oauth.issuer가 있으면 그것,
 * 없으면 이 요청이 들어온 주소(배포에서는 Nginx가 넘긴 Host·X-Forwarded-Proto, forward-headers-strategy)로 정한다.
 */
@Component
public class OAuthIssuer {

    /** 보호 자원(글결 MCP)의 길. API 버전이 바뀌면 함께 바꾼다. */
    public static final String MCP_RESOURCE_PATH = "/api/v1/mcp";
    public static final String RESOURCE_METADATA_PATH = "/.well-known/oauth-protected-resource";

    private final String configured;

    public OAuthIssuer(OAuthProperties properties) {
        this.configured = properties.issuer();
    }

    /** 끝에 / 없는 사이트 주소, 예: https://example.com */
    public String current() {
        if (!configured.isEmpty()) {
            return configured;
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath().replacePath(null).replaceQuery(null).build()
                .toUriString().replaceAll("/+$", "");
    }

    /** 보안 필터처럼 요청을 손에 쥔 곳에서. */
    public String current(HttpServletRequest request) {
        if (!configured.isEmpty()) {
            return configured;
        }
        return ServletUriComponentsBuilder.fromContextPath(request).replacePath(null).replaceQuery(null).build()
                .toUriString().replaceAll("/+$", "");
    }

    public String resourceMetadata(HttpServletRequest request) {
        return current(request) + RESOURCE_METADATA_PATH + MCP_RESOURCE_PATH;
    }

    public String resource() {
        return current() + MCP_RESOURCE_PATH;
    }

    public String resourceMetadata() {
        return current() + RESOURCE_METADATA_PATH + MCP_RESOURCE_PATH;
    }
}
