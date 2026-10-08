package com.myblog.post.controller;

import com.myblog.post.controller.dto.PostRequests;
import com.myblog.post.service.PostFormService;
import com.myblog.post.service.PostFormService.PostForm;
import com.myblog.post.service.PostWriteService;
import com.myblog.user.LoggedInMember;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 글 (specs/003 contracts 9 ~ 14). 회원 번호는 LoggedInMember로만 얻는다 (주소·본문에서 받지 않음).
 * 글 읽기(GET /api/posts/{숫자})만 로그인 없이 부를 수 있다 (SecurityConfig).
 */
@RestController
public class PostController {

    private final LoggedInMember loggedInMember;
    private final PostFormService formService;
    private final PostWriteService writeService;

    public PostController(LoggedInMember loggedInMember, PostFormService formService, PostWriteService writeService) {
        this.loggedInMember = loggedInMember;
        this.formService = formService;
        this.writeService = writeService;
    }

    /** 글쓰기 화면의 기본값 (contracts 9). */
    @GetMapping("/api/me/blog/post-form")
    public PostForm form(Authentication authentication) {
        return formService.form(loggedInMember.requireIdOf(authentication));
    }

    /** 글 쓰기 (contracts 10). 새 글이면 201, 같은 requestKey로 이미 만든 글이면 200. 둘 다 { postId }. */
    @PostMapping("/api/posts")
    public ResponseEntity<PostIdResponse> create(Authentication authentication,
            @Valid @RequestBody PostRequests.Write request) {
        Long memberId = loggedInMember.requireIdOf(authentication);
        PostWriteService.Created result = writeService.create(memberId, request.categoryId(), request.topicId(),
                request.title(), request.content(), request.visibility(), request.requestKey());
        return ResponseEntity.status(result.created() ? HttpStatus.CREATED : HttpStatus.OK)
                .body(new PostIdResponse(result.postId()));
    }

    public record PostIdResponse(Long postId) {
    }
}
