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
 * 다른 테스트는 기본 프로필 dev로 돌아 배포와 설정이 다를 수 있으므로, application.yml의 기본값을 따로 확인한다.
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
    void 배포에서_열쇠가_없으면_빨간_펜은_규칙_교정이고_비슷한_글은_태그로_대신한다() throws IOException {
        Binder binder = prodBinder();

        ProofreadProperties proofread = binder.bind("proofread", ProofreadProperties.class).get();
        RelatedProperties related = binder.bind("related", RelatedProperties.class).get();

        assertThat(proofread.withoutKey()).isEqualTo("rules");
        assertThat(proofread.resolvedProvider()).isEqualTo("rules");
        assertThat(proofread.maxRetries()).isZero();
        assertThat(related.withoutKey()).isEqualTo("off");
        assertThat(related.resolvedProvider()).isEqualTo("off");
        assertThat(related.ollamaUrl()).isEmpty();
    }

    @Test
    void 배포가_Ollama를_켜서_주소를_넣으면_Ollama를_쓴다() throws IOException {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_ENVIRONMENT_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().remove(StandardEnvironment.SYSTEM_PROPERTIES_PROPERTY_SOURCE_NAME);
        environment.getPropertySources().addFirst(new org.springframework.core.env.MapPropertySource("deploy",
                java.util.Map.of("RELATED_OLLAMA_URL", "http://ollama:11434")));
        YamlPropertySourceLoader loader = new YamlPropertySourceLoader();
        for (String file : List.of("application-prod.yml", "application.yml")) {
            loader.load(file, new ClassPathResource(file)).forEach(environment.getPropertySources()::addLast);
        }

        RelatedProperties related = Binder.get(environment).bind("related", RelatedProperties.class).get();

        assertThat(related.resolvedProvider()).isEqualTo("ollama");
        assertThat(related.ollamaModel()).isEqualTo("bge-m3");
        assertThat(related.dimensions()).isEqualTo(1024);
    }
}
