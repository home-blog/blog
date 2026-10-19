package com.myblog.community.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** 문장 수집 설정 (specs/015 US3): 칸보다 긴 문장 길이, 0개 한도는 서버가 켜지지 않게 막는다. */
class ClipPropertiesTest {

    @Test
    void 정상값과_경계값() {
        assertThatCode(() -> new ClipProperties(300, 1000, 20)).doesNotThrowAnyException();
        assertThatCode(() -> new ClipProperties(1, 1, 1)).doesNotThrowAnyException();
    }

    @Test
    void 잘못된_값은_서버가_켜지지_않는다() {
        assertThatThrownBy(() -> new ClipProperties(0, 1000, 20)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ClipProperties(301, 1000, 20)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ClipProperties(300, 0, 20)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new ClipProperties(300, 1000, 0)).isInstanceOf(IllegalStateException.class);
    }
}
