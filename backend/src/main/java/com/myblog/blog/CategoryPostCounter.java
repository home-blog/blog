package com.myblog.blog;

import java.util.Collection;
import java.util.Map;

/**
 * "분류의 글 개수"를 묻는 틀 (specs/003 T009, FR-040, FR-043).
 * 블로그 모듈은 글 모듈을 부르지 못하므로(blog ← post) 블로그 모듈이 틀만 두고 글 모듈이 채운다.
 */
public interface CategoryPostCounter {

    /** 분류의 모든 글 (비공개 포함). 주인이 보는 개수와 분류 삭제 거절에 쓴다. */
    long countAll(Long categoryId);

    /** 분류마다 모든 글 개수 (비공개 포함, 한 번에 센다). 결과에는 받은 모든 분류 번호가 들어 있다. */
    Map<Long, Long> countAll(Collection<Long> categoryIds);

    /**
     * 분류마다 주인이 아닌 사람에게 보이는 글 개수 (공개 분류의 공개 글만, FR-043, FR-048).
     * 비공개 분류는 0이다. 결과에는 받은 모든 분류 번호가 들어 있다.
     */
    Map<Long, Long> countVisible(Collection<Long> categoryIds);
}
