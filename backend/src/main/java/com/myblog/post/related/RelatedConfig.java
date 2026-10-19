package com.myblog.post.related;

import com.myblog.post.config.RelatedProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/** 설정(related.provider)에 따라 임베딩 하나를 띄운다 (specs/014 D-15). 열쇠 값은 로그에 남기지 않는다. */
@Configuration
public class RelatedConfig {

    private static final Logger log = LoggerFactory.getLogger(RelatedConfig.class);

    @Bean
    Embedder embedder(RelatedProperties properties, JsonMapper jsonMapper) {
        String provider = properties.resolvedProvider();
        log.info("비슷한 글 임베딩: {}", provider);
        return switch (provider) {
            case "voyage" -> new VoyageEmbedder(properties, jsonMapper);
            case "fake" -> new FakeEmbedder(properties.dimensions());
            default -> new OffEmbedder();
        };
    }
}
