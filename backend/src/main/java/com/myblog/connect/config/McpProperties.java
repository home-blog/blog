package com.myblog.connect.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** 글결 MCP의 숫자 (application.yml의 mcp.*, specs/020 data-model). */
@ConfigurationProperties(prefix = "mcp")
public record McpProperties(RateLimit rateLimit, Search search) {

    public McpProperties {
        if (rateLimit == null || rateLimit.perMinute() < 1) {
            throw new IllegalStateException("mcp.rate-limit.per-minute는 1 이상이어야 합니다");
        }
        if (search == null || search.candidates() < 1 || search.passageLength() < 50 || search.maxPassages() < 1
                || search.passagesPerPost() < 1 || search.maxPosts() < 1) {
            throw new IllegalStateException("mcp.search.*의 값을 확인해 주세요");
        }
    }

    /** perMinute: 열쇠마다 1분에 받는 요청. */
    public record RateLimit(int perMinute) {
    }

    /** candidates: 단락 찾기 후보 글, passageLength: 단락 글자 상한, maxPassages·passagesPerPost·maxPosts: 결과 상한. */
    public record Search(int candidates, int passageLength, int maxPassages, int passagesPerPost, int maxPosts) {
    }
}
