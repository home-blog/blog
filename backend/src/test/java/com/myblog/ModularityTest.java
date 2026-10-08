package com.myblog;

import org.junit.jupiter.api.Test;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

/**
 * Spring Modulith: 기능별 모듈(user, blog, common …)의 경계를 검사한다.
 * - 다른 모듈의 안쪽(하위 패키지)을 직접 부르면 실패한다. 모듈끼리는 맨 위 패키지의 타입이나 이벤트로만 이어진다.
 * - 모듈끼리 서로 부르는 고리(순환)가 생기면 실패한다. (헌법: user ← blog ← post …)
 */
class ModularityTest {

    private final ApplicationModules modules = ApplicationModules.of(MyBlogApplication.class);

    @Test
    void 모듈_경계와_방향을_지킨다() {
        modules.verify();
    }

    @Test
    void 모듈_그림을_만든다() {
        // target/spring-modulith-docs 에 모듈 관계 그림(PlantUML)과 설명을 만든다
        new Documenter(modules).writeDocumentation();
    }
}
