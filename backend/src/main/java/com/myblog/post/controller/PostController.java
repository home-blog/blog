package com.myblog.post.controller;

import com.myblog.post.controller.dto.PostRequests;
import com.myblog.post.service.PostEditService;
import com.myblog.post.service.PostFormService;
import com.myblog.post.service.PostFormService.PostForm;
import com.myblog.post.service.PostReadService;
import com.myblog.post.service.PostWriteService;
import com.myblog.user.LoggedInMember;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
    private final PostReadService readService;
    private final PostEditService editService;

    public PostController(LoggedInMember loggedInMember, PostFormService formService, PostWriteService writeService,
            PostReadService readService, PostEditService editService) {
        this.loggedInMember = loggedInMember;
        this.formService = formService;
        this.writeService = writeService;
        this.readService = readService;
        this.editService = editService;
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

    /** 글 상세 (contracts 11). 누구나 부른다. 볼 수 없는 글은 없는 글과 같은 404. */
    @GetMapping("/api/posts/{postId}")
    public PostReadService.PostDetail read(@PathVariable Long postId, Authentication authentication) {
        return readService.read(postId, loggedInMember.idOf(authentication).orElse(null));
    }

    /** 수정 화면용 글 (contracts 12). 남의 글이면 404. */
    @GetMapping("/api/posts/{postId}/edit")
    public PostEditService.EditView editView(@PathVariable Long postId, Authentication authentication) {
        return editService.editView(loggedInMember.requireIdOf(authentication), postId);
    }

    /** 글 수정 (contracts 13). 본문은 주인 확인 뒤에 서비스가 검사한다 (@Valid 없음). */
    @PutMapping("/api/posts/{postId}")
    public PostEditService.Updated update(@PathVariable Long postId, Authentication authentication,
            @RequestBody PostRequests.Update request) {
        return editService.update(loggedInMember.requireIdOf(authentication), postId, request);
    }

    /** 글 삭제 (contracts 14). */
    @DeleteMapping("/api/posts/{postId}")
    public ResponseEntity<Void> delete(@PathVariable Long postId, Authentication authentication) {
        editService.delete(loggedInMember.requireIdOf(authentication), postId);
        return ResponseEntity.noContent().build();
    }

    public record PostIdResponse(Long postId) {
    }
}
