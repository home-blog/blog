package com.myblog.stats.controller;

import com.myblog.blog.BlogDirectory;
import com.myblog.blog.BlogDirectory.BlogInfo;
import com.myblog.comment.NewCommentCounter;
import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.user.LoggedInMember;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 블로그 관리 화면의 머리 정보 (specs/006 contracts 1, T014, T035, FR-001 ~ FR-003, FR-005, FR-028).
 * 블로그는 주소로 받지 않는다: 세션의 회원으로 그 회원의 블로그를 찾는다 (research B-1). 그래서 남의 관리 화면 주소가 없다.
 * 이름은 요청마다 표에서 읽는다 (설정을 바꾸면 다음 요청부터 새 이름, FR-040).
 */
@RestController
public class ManageHeaderController {

    private final LoggedInMember loggedInMember;
    private final BlogDirectory blogDirectory;
    private final NewCommentCounter newCommentCounter;

    public ManageHeaderController(LoggedInMember loggedInMember, BlogDirectory blogDirectory,
            NewCommentCounter newCommentCounter) {
        this.loggedInMember = loggedInMember;
        this.blogDirectory = blogDirectory;
        this.newCommentCounter = newCommentCounter;
    }

    @GetMapping("/api/manage/blog")
    public ManageHeader header(Authentication authentication) {
        Long memberId = loggedInMember.requireIdOf(authentication);
        BlogInfo blog = blogDirectory.myBlog(memberId).orElseThrow(() -> new ApiException(ErrorCode.BLOG_NOT_FOUND));
        return new ManageHeader(blog.blogId(), blog.name(), blog.intro(), "/blog/" + blog.blogId(),
                newCommentCounter.countFor(memberId));
    }

    /** blogPath는 화면의 블로그 주소 ("내 블로그 보기"). newCommentCount는 메뉴 옆 숫자 (T035, new-count와 같은 계산). */
    public record ManageHeader(Long blogId, String name, String intro, String blogPath, long newCommentCount) {
    }
}
