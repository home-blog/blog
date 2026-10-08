package com.myblog.image.config;

import jakarta.servlet.MultipartConfigElement;
import org.springframework.boot.servlet.MultipartConfigFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.unit.DataSize;

/**
 * 파일 업로드 크기의 프레임워크 설정을 community.image.max-size 하나에서 정한다 (plan: 숫자를 두 곳에 쓰지 않음, R-6).
 * 파일 하나는 max-size까지(정확히 5MB 통과). 요청 전체는 파일에 다른 칸(postId)과 경계 글자가 더해지므로 64KB를 더 둔다.
 * 이 빈이 있으면 Spring Boot의 spring.servlet.multipart 자동 설정은 쓰이지 않는다.
 */
@Configuration
public class ImageConfig {

    static final DataSize REQUEST_OVERHEAD = DataSize.ofKilobytes(64);

    @Bean
    public MultipartConfigElement multipartConfigElement(ImageProperties properties) {
        MultipartConfigFactory factory = new MultipartConfigFactory();
        factory.setMaxFileSize(properties.maxSize());
        factory.setMaxRequestSize(DataSize.ofBytes(properties.maxSize().toBytes() + REQUEST_OVERHEAD.toBytes()));
        return factory.createMultipartConfig();
    }
}
