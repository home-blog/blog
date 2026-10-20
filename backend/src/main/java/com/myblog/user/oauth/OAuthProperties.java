package com.myblog.user.oauth;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/** Claude 앱 커스텀 커넥터(OAuth)의 숫자 (application.yml의 mcp.oauth.*, specs/023 data-model). */
@ConfigurationProperties(prefix = "mcp.oauth")
public record OAuthProperties(String issuer, Duration accessTokenTtl, Duration refreshTokenTtl, Duration requestTtl,
        Duration codeTtl, int maxGrantsPerMember, int registerPerHour, int clientNameMaxLength, int maxRedirectUris,
        int unusedClientDays, String cleanupCron) {

    /** oauth_client.client_name 칸 길이 (V33). 설정을 이보다 길게 정하면 서버가 켜지지 않는다. */
    static final int NAME_COLUMN_LENGTH = 100;

    public OAuthProperties {
        issuer = issuer == null ? "" : issuer.strip().replaceAll("/+$", "");
        if (accessTokenTtl == null || refreshTokenTtl == null || requestTtl == null || codeTtl == null
                || accessTokenTtl.isNegative() || accessTokenTtl.isZero() || refreshTokenTtl.compareTo(accessTokenTtl) < 0
                || requestTtl.isNegative() || requestTtl.isZero() || codeTtl.isNegative() || codeTtl.isZero()) {
            throw new IllegalStateException("mcp.oauth: 토큰·요청·코드 수명은 0보다 길고, 갱신 토큰은 출입증보다 길어야 합니다");
        }
        if (maxGrantsPerMember < 1 || registerPerHour < 1 || maxRedirectUris < 1 || unusedClientDays < 1) {
            throw new IllegalStateException("mcp.oauth: 연결·등록·돌아갈 주소·정리 날 수는 1 이상이어야 합니다");
        }
        if (clientNameMaxLength < 1 || clientNameMaxLength > NAME_COLUMN_LENGTH) {
            throw new IllegalStateException(
                    "mcp.oauth.client-name-max-length는 1 이상 %d 이하여야 합니다".formatted(NAME_COLUMN_LENGTH));
        }
    }
}
