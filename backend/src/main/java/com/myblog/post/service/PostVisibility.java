package com.myblog.post.service;

import com.myblog.blog.BlogDirectory.CategoryInfo;
import com.myblog.common.Visibility;
import org.springframework.stereotype.Component;

/**
 * "누구에게 어떤 글이 보이나"를 한곳에 둔다 (specs/003 research B-3, FR-030, FR-031, FR-048, SC-002, SC-013).
 * <ul>
 *   <li>블로그 주인은 자기 블로그의 모든 글을 본다.</li>
 *   <li>그 밖의 사람(로그인하지 않은 사람 포함)은 글이 공개이고 <b>그 분류도 공개</b>일 때만 본다.</li>
 * </ul>
 * 글 상세, 이전·다음 글, 분류별 글 개수가 모두 이 조건을 쓴다.
 * 004의 목록·검색도 이 조건을 그대로 쓴다. 단 검색은 주인이어도 비공개 글을 넣지 않고(contracts 15),
 * 주인의 목록에는 글마다 visibility를 담아 "비공개" 표시를 붙인다(FR-032).
 */
@Component
public class PostVisibility {

    /** 이 사람(viewerId, 로그인하지 않았으면 null)이 이 분류의 이 글을 볼 수 있나. */
    public boolean canSee(String postVisibility, CategoryInfo category, Long viewerId) {
        return isOwner(category, viewerId) || isOpenToEveryone(postVisibility, category.visibility());
    }

    /** 주인이 아닌 사람에게 보이는 글: 글과 분류가 모두 공개. */
    public boolean isOpenToEveryone(String postVisibility, String categoryVisibility) {
        return Visibility.isPublic(postVisibility) && Visibility.isPublic(categoryVisibility);
    }

    public boolean isOwner(CategoryInfo category, Long viewerId) {
        return viewerId != null && viewerId.equals(category.ownerId());
    }
}
