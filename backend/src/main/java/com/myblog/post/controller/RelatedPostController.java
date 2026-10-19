package com.myblog.post.controller;

import com.myblog.post.service.RelatedPostService;
import com.myblog.post.service.RelatedPostService.RelatedView;
import com.myblog.user.LoggedInMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** 이 글과 비슷한 글 (specs/014 contracts 3). 누구나 읽는다(SecurityConfig). */
@Tag(name = "13. AI")
@RestController
public class RelatedPostController {

    private final RelatedPostService relatedPostService;
    private final LoggedInMember loggedInMember;

    public RelatedPostController(RelatedPostService relatedPostService, LoggedInMember loggedInMember) {
        this.relatedPostService = relatedPostService;
        this.loggedInMember = loggedInMember;
    }

    @Operation(summary = "비슷한 글",
            description = "이 글과 뜻이 비슷한 공개 글을 4개까지 준다(한 블로그는 2개까지). 뜻 정보(임베딩)가 없거나 AI가 꺼져 있으면"
                    + " 태그·주제가 겹치는 글로 대신하고 basis가 tags다. 이 글을 볼 수 없으면 글 읽기와 같은 404.")
    @GetMapping("/api/{version}/posts/{postId}/related")
    public RelatedView related(@PathVariable Long postId, @Parameter(hidden = true) Authentication authentication) {
        return relatedPostService.related(postId, loggedInMember.idOf(authentication).orElse(null));
    }
}
