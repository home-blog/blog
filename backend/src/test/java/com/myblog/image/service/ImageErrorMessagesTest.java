package com.myblog.image.service;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.util.unit.DataSize;

/** 이미지 용량 안내 문구가 설정값을 줄이지 않고 적는다. */
class ImageErrorMessagesTest {

    @Test
    void 딱_떨어지는_MB는_MB로() {
        assertThat(ImageErrorMessages.sizeText(DataSize.ofMegabytes(5))).isEqualTo("5MB");
    }

    @Test
    void 딱_떨어지지_않으면_KB로() {
        assertThat(ImageErrorMessages.sizeText(DataSize.ofKilobytes(1536))).isEqualTo("1536KB");
    }
}
