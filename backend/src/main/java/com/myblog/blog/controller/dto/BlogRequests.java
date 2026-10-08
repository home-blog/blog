package com.myblog.blog.controller.dto;

import com.myblog.blog.validation.ValidBlogIntro;
import com.myblog.blog.validation.ValidBlogName;

/** 블로그 설정 요청 본문 (specs/003 contracts 4). 블로그 번호 칸은 없다: 내 블로그는 세션으로 정한다 (FR-006). */
public final class BlogRequests {

    private BlogRequests() {
    }

    public record UpdateProfile(@ValidBlogName String name, @ValidBlogIntro String intro) {
    }
}
