package com.myblog;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * MyBlog 서버 시작점.
 *
 * <p>단일 서버이고, 안은 기능별 모듈(user, blog, post, comment, community, image, stats, common)로 나눈다.
 * 모듈 사이의 부르는 방향은 docs 저장소의 docs/가이드/06-아키텍처-그림.md를 따른다.
 */
@SpringBootApplication
public class MyBlogApplication {

    public static void main(String[] args) {
        SpringApplication.run(MyBlogApplication.class, args);
    }
}
