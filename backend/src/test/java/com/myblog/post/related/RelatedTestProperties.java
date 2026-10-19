package com.myblog.post.related;

import com.myblog.post.config.RelatedProperties;
import java.time.Duration;

/** 테스트용 관련 글 설정. */
final class RelatedTestProperties {

    private RelatedTestProperties() {
    }

    static RelatedProperties with(String provider, String apiKey, int dimensions) {
        return new RelatedProperties(provider, "off", apiKey, "https://api.voyageai.com/v1", "voyage-3.5", dimensions,
                4000, Duration.ofSeconds(20), 32, 4, 2, 3000);
    }
}
