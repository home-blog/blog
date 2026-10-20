package com.myblog.user.oauth;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 지난 출입증·연결과, 연결 없이 오래된 앱 등록을 매일 지운다 (specs/023 data-model). */
@Component
class OAuthCleanup {

    private final OAuthService oauth;

    OAuthCleanup(OAuthService oauth) {
        this.oauth = oauth;
    }

    @Scheduled(cron = "${mcp.oauth.cleanup-cron}")
    void run() {
        oauth.cleanup();
    }
}
