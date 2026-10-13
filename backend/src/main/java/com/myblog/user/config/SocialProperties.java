package com.myblog.user.config;

import java.time.Duration;
import java.util.Map;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 소셜 로그인 설정 (application.yml의 social.*, specs/010).
 * clients의 키는 회사 이름(github, google, kakao, naver). 열쇠는 환경 변수로만 들어오고, 둘 중 하나라도 비면 그 회사는 쓰지 않는다 (D-9).
 */
@ConfigurationProperties(prefix = "social")
public record SocialProperties(Duration pendingTtl, Map<String, Client> clients) {

    public SocialProperties {
        clients = clients == null ? Map.of() : Map.copyOf(clients);
    }

    /** 회사에 앱을 등록하고 받은 값. */
    public record Client(String clientId, String clientSecret) {

        public boolean configured() {
            return clientId != null && !clientId.isBlank() && clientSecret != null && !clientSecret.isBlank();
        }
    }
}
