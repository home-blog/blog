package com.myblog.post.controller.dto;

import com.myblog.post.service.PostListService.PostListView;
import com.myblog.post.service.PostListService.PostRow;
import java.util.List;

/** 글 목록·검색 응답 (specs/004 contracts 1, 2). 필드 이름은 계약 그대로다. */
public final class ExploreResponses {

    private ExploreResponses() {
    }

    public record PostListResponse(Long blogId, boolean isOwner, Long categoryId, long totalCount, int page,
            int totalPages, int pageSize, List<PostRow> posts) {

        public static PostListResponse of(PostListView view) {
            return new PostListResponse(view.blogId(), view.isOwner(), view.categoryId(), view.totalCount(),
                    view.page(), view.totalPages(), view.pageSize(), view.posts());
        }
    }
}
