package com.myblog.ai.config;

import com.myblog.ai.service.ClaudeProofreader;
import com.myblog.ai.service.FakeProofreader;
import com.myblog.ai.service.OffProofreader;
import com.myblog.ai.service.ProofreadChecker;
import com.myblog.ai.service.Proofreader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.json.JsonMapper;

/** 설정(proofread.provider)에 따라 교정기 하나를 띄운다 (specs/014 D-15). 열쇠 값은 로그에 남기지 않는다. */
@Configuration
public class ProofreadConfig {

    private static final Logger log = LoggerFactory.getLogger(ProofreadConfig.class);

    @Bean
    Proofreader proofreader(ProofreadProperties properties, JsonMapper jsonMapper) {
        String provider = properties.resolvedProvider();
        log.info("빨간 펜 교정기: {}", provider);
        return switch (provider) {
            case "claude" -> new ClaudeProofreader(properties, jsonMapper);
            case "fake" -> new FakeProofreader();
            default -> new OffProofreader();
        };
    }

    @Bean
    ProofreadChecker proofreadChecker(ProofreadProperties properties) {
        return new ProofreadChecker(properties.maxCorrections());
    }
}
