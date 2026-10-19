package com.myblog.ai.config;

import static org.assertj.core.api.Assertions.assertThat;

import com.myblog.post.config.RelatedProperties;
import java.io.IOException;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.env.YamlPropertySourceLoader;
import org.springframework.core.env.StandardEnvironment;
import org.springframework.core.io.ClassPathResource;

/**
 * 배포(prod) 설정만으로 AI 설정이 묶이는지 본다 (specs/014 D-15).
 * 다른 테스트는 기본 프로필 dev로 돌아 without-key가 fake로 덮이므로, application.yml의 기본값을 따로 확인한다.
 * YAML의 따옴표 없는 off는 참·거짓(false)으로 읽혀 "false"가 되고, 서버가 켜지지 않는다.
 */
class ProdSettingsBindingTest {

    private static Binder prodBinder() throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        // 이 컴퓨터의 환경 변수(열쇠 등)에 기대지 않게 시스템 값을 뺀다
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        for (String file : List.of("application-prod.yml", "application.yml")) { // 앞의 것이 이긴다
            loader.load(file, new ClassPathResource(file)).forEach(environment.getPropertySources()::addLast);
        }
        return Binder.get(environment);
    }

    @Test
    void 배포에서_열쇠가_없으면_빨간_펜은_꺼지고_비슷한_글은_태그로_대신한다() throws IOException {
        Binder binder = prodBinder();

        ProofreadProperties proofread = binder.bind("proofread", ProofreadProperties.class).get();
        RelatedProperties related = binder.bind("related", RelatedProperties.class).get();

        assertThat(proofread.withoutKey()).isEqualTo("off");
        assertThat(proofread.resolvedProvider()).isEqualTo("off");
        assertThat(proofread.maxRetries()).isZero();
        assertThat(related.withoutKey()).isEqualTo("off");
        assertThat(related.resolvedProvider()).isEqualTo("off");
    }
}
