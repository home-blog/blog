package com.myblog;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/** 서버가 PostgreSQL·Redis와 함께 문제없이 뜨는지 확인한다. (CI는 두 서비스를 띄운 뒤 실행) */
@SpringBootTest
class MyBlogApplicationTests {

    @Test
    void contextLoads() {
    }
}
