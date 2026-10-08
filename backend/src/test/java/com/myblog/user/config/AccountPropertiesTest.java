package com.myblog.user.config;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

/** 소개 글자 수 설정은 DB 칸(VARCHAR(100))을 넘을 수 없다. 잘못 정하면 서버가 켜지지 않는다. */
class AccountPropertiesTest {

    @Test
    void 기본값_0에서_100자는_된다() {
        assertThatCode(() -> new AccountProperties.Intro(0, 100)).doesNotThrowAnyException();
    }

    @Test
    void DB_칸보다_길거나_거꾸로_정하면_켜지지_않는다() {
        assertThatThrownBy(() -> new AccountProperties.Intro(0, 101)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new AccountProperties.Intro(-1, 100)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new AccountProperties.Intro(50, 10)).isInstanceOf(IllegalStateException.class);
    }
}
