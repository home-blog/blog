package com.myblog.blog.controller.dto;

import com.myblog.blog.validation.ValidBlogVisibility;
import com.myblog.blog.validation.ValidCategoryName;
import java.util.List;

/** 분류 관리 요청 본문 (specs/003 contracts 5 ~ 7). 블로그 번호 칸은 없다: 내 블로그는 세션으로 정한다 (FR-035). */
public final class CategoryRequests {

    private CategoryRequests() {
    }

    /** 분류 추가. 공개 여부가 없으면 공개. */
    public record Create(@ValidCategoryName String name, @ValidBlogVisibility String visibility) {
    }

    /**
     * 분류 고치기: 보낸 칸만 바꾼다. 컨트롤러에서 @Valid로 검사하지 않는다:
     * 남의 분류면 형식 오류보다 먼저 404로 답해야 하므로 서비스가 내 분류인지 본 <b>뒤에</b> 검사한다.
     */
    public record Update(@ValidCategoryName(optional = true) String name, @ValidBlogVisibility String visibility) {
    }

    /** 위에서부터 새 순서. 내 분류 번호가 빠짐없이 한 번씩 있어야 한다. */
    public record Order(List<Long> categoryIds) {
    }
}
