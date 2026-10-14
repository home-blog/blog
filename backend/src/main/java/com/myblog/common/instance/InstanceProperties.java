package com.myblog.common.instance;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 이 서버가 앱 두 벌 중 어느 쪽인지 (application.yml의 instance.*, specs/011 무중단 배포).
 * 배포에서는 A(8410)·B(8411) 두 벌을 같이 띄우고 학원 Nginx가 번갈아 보낸다. 이름이 비어 있으면(로컬 개발) 아무것도 붙이지 않는다.
 */
@ConfigurationProperties(prefix = "instance")
public record InstanceProperties(String name) {

    public InstanceProperties {
        name = name == null ? "" : name.strip();
        if (!name.matches("[A-Za-z0-9_-]{0,20}")) {
            throw new IllegalStateException("instance.name은 영문·숫자·-·_ 20자 이내여야 합니다");
        }
    }

    public boolean named() {
        return !name.isEmpty();
    }
}
