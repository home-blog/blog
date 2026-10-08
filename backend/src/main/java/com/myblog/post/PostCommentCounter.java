package com.myblog.post;

import java.util.Collection;
import java.util.Map;

/**
 * 글 상세·글 관리의 댓글 수를 묻는 틀 (specs/005 T010, FR-003, specs/006 T008). 글 모듈은 댓글 모듈을 부르지 못하므로(post ← comment)
 * 글 모듈이 틀만 두고 댓글 모듈이 채운다 (003의 CategoryPostCounter와 같은 방식). 채우는 쪽이 없으면 0으로 본다.
 */
public interface PostCommentCounter {

    long count(Long postId);

    /**
     * 여러 글의 댓글 수를 한 번에 (specs/006 T008, 글 관리의 한 쪽, FR-013). 결과에는 받은 모든 글 번호가 들어 있다.
     * 채우는 쪽이 없으면 부르는 쪽이 모두 0으로 본다.
     */
    Map<Long, Long> countByPostIds(Collection<Long> postIds);
}
