package com.myblog.blog;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * 다른 모듈(글)이 블로그·분류에 대해 묻는 입구 (specs/003 T009, research B-2).
 * 글 모듈은 블로그·분류 표를 직접 건드리지 않고 이것으로만 묻는다. 이름은 요청마다 표에서 읽는다 (캐시 없음, SC-010).
 */
public interface BlogDirectory {

    /** 이 회원의 블로그 (회원 한 명에 하나). */
    Optional<BlogInfo> myBlog(Long memberId);

    /** 분류와 그 분류가 속한 블로그·주인. */
    Optional<CategoryInfo> category(Long categoryId);

    /** 여러 분류를 한 번에. 없는 번호는 빠진다. */
    List<CategoryInfo> categories(Collection<Long> categoryIds);

    /** 블로그의 모든 분류 (비공개 포함), 주인이 정한 순서. */
    List<CategoryInfo> categoriesOf(Long blogId);

    /** 블로그의 공개 분류 번호. 이전·다음 글을 찾을 때 쓴다 (FR-024, FR-048). */
    List<Long> publicCategoryIds(Long blogId);

    record BlogInfo(Long blogId, Long ownerId, String name) {
    }

    /** visibility는 "public" / "private" (common.Visibility의 글자). */
    record CategoryInfo(Long categoryId, Long blogId, Long ownerId, String blogName, String name, String visibility,
            boolean isDefault) {
    }
}
