package com.myblog.post.service;

import com.myblog.blog.BlogDirectory.CategoryInfo;
import com.myblog.common.Visibility;
import java.util.List;
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
 * 006의 인기 글도 isOpenToEveryone으로 거른다: 비공개 분류에 든 공개 글은 넣지 않는다 (2026-10-08 정할 것 24번, T040).
 * 004 글 목록은 {@link #listScope}로 "어느 분류의 어떤 글을 읽나"를 정한다 (004 T009, D-6, D-7).
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

    /**
     * 한 블로그의 글 목록에서 읽을 범위 (004 T009). 글 표에는 블로그 칸이 없어서 분류를 거쳐 찾는다 (004 D-6).
     * 주인이면 모든 분류의 모든 글, 아니면 공개 분류의 공개 글만 (004 D-7의 B). 요청 값으로 바꿀 수 없다.
     *
     * @param blogCategories 그 블로그의 모든 분류 (BlogDirectory.categoriesOf)
     */
    public ListScope listScope(Long blogOwnerId, List<CategoryInfo> blogCategories, Long viewerId) {
        if (viewerId != null && viewerId.equals(blogOwnerId)) {
            return new ListScope(blogCategories, false);
        }
        return new ListScope(blogCategories.stream()
                .filter(category -> Visibility.isPublic(category.visibility()))
                .toList(), true);
    }

    /** 목록이 읽을 분류들과, 공개 글만 읽을지. */
    public record ListScope(List<CategoryInfo> categories, boolean publicPostsOnly) {

        public List<Long> categoryIds() {
            return categories.stream().map(CategoryInfo::categoryId).toList();
        }
    }
}
