package com.myblog.post.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 로봇이 읽는 주소: RSS, 사이트 지도 (application.yml의 share.*, specs/007 US5·US6, 가안).
 *
 * @param rssSize         RSS 한 번에 주는 최신 공개 글 수
 * @param sitemapMaxPosts 사이트 지도에 넣는 공개 글 수 (최신순)
 */
@ConfigurationProperties(prefix = "share")
public record ShareProperties(int rssSize, int sitemapMaxPosts) {

    /** 사이트 지도 한 파일에 넣을 수 있는 주소 수 (sitemaps.org). 첫 화면·블로그 주소 몫을 남긴다. */
    static final int SITEMAP_URL_LIMIT = 50_000;

    public ShareProperties {
        if (rssSize < 1 || rssSize > 100) {
            throw new IllegalStateException("share.rss-size는 1~100이어야 합니다");
        }
        if (sitemapMaxPosts < 1 || sitemapMaxPosts > SITEMAP_URL_LIMIT / 2) {
            throw new IllegalStateException("share.sitemap-max-posts는 1~%d이어야 합니다".formatted(SITEMAP_URL_LIMIT / 2));
        }
    }
}
