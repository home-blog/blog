package com.myblog.common;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Paths;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 자동 API 문서(springdoc)의 머리: API 주소 버전과 릴리스 번호를 같이 보여 준다 (2026-10-12 사용자: 릴리스를 API에도 반영).
 * 문서는 개발 설정에서만 켠다 (springdoc.*.enabled). 바뀐 것의 기록은 저장소 맨 위 CHANGELOG.md.
 */
@Configuration
public class OpenApiConfig {

    @Bean
    OpenAPI geulgyeolApi(@Value("${myblog.release}") String release) {
        return new OpenAPI().info(new Info()
                .title("글결 API")
                .version("v" + ApiVersionConfig.SUPPORTED[ApiVersionConfig.SUPPORTED.length - 1] + " · 릴리스 " + release)
                .description("""
                        모든 주소는 /api/v1/… 입니다 (이미지 파일 /api/images/{파일}만 버전 없이 고정).
                        API 버전은 릴리스와 따로 갑니다: 칸을 더하는 변경은 v1 그대로, 칸을 빼거나 이름·뜻을 바꾸는 변경만 v2를 엽니다.
                        릴리스마다 바뀐 것은 저장소의 CHANGELOG.md에 있습니다."""));
    }

    /** 처리기 주소의 {version} 칸을 지금 버전(v1)으로 채워 문서에서 바로 불러 볼 수 있게 한다. */
    @Bean
    OpenApiCustomizer currentVersionPaths() {
        String current = "/api/v" + ApiVersionConfig.SUPPORTED[ApiVersionConfig.SUPPORTED.length - 1] + "/";
        return openApi -> {
            Paths paths = new Paths();
            openApi.getPaths().forEach((path, item) -> paths.addPathItem(path.replace("/api/{version}/", current), item));
            openApi.setPaths(paths);
        };
    }
}
