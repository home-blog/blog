package com.myblog.community.controller;

import com.myblog.community.controller.dto.ClipRequest;
import com.myblog.community.service.SentenceClipService;
import com.myblog.community.service.SentenceClipService.ClipPage;
import com.myblog.community.service.SentenceClipService.ClipView;
import com.myblog.community.service.SentenceClipService.Saved;
import com.myblog.user.LoggedInMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 문장 수집과 필사 노트 (specs/015 contracts 3). 모두 로그인이 필요하다. 회원 번호는 LoggedInMember로만 얻는다. */
@Tag(name = "7. 댓글·좋아요·신고")
@RestController
public class SentenceClipController {

    private final LoggedInMember loggedInMember;
    private final SentenceClipService clipService;

    public SentenceClipController(LoggedInMember loggedInMember, SentenceClipService clipService) {
        this.loggedInMember = loggedInMember;
        this.clipService = clipService;
    }

    @Operation(summary = "문장 수집", description = "글 본문에 있는 문장을 필사 노트에 옮겨 적는다. 새로 모으면 201, 이미 모은 문장이면 200.")
    @PostMapping("/api/{version}/me/clips")
    public ResponseEntity<ClipView> collect(@RequestBody ClipRequest request, Authentication authentication) {
        Saved saved = clipService.collect(loggedInMember.requireIdOf(authentication), request.postId(), request.text());
        return ResponseEntity.status(saved.created() ? HttpStatus.CREATED : HttpStatus.OK).body(saved.clip());
    }

    @Operation(summary = "필사 노트", description = "내가 모은 문장, 최근 순으로 한 쪽에 `community.clip.page-size`개.")
    @GetMapping("/api/{version}/me/clips")
    public ClipPage list(@RequestParam(required = false) String page, Authentication authentication) {
        return clipService.list(loggedInMember.requireIdOf(authentication), page);
    }

    @Operation(summary = "모은 문장 지우기")
    @DeleteMapping("/api/{version}/me/clips/{clipId}")
    public ResponseEntity<Void> remove(@PathVariable Long clipId, Authentication authentication) {
        clipService.remove(loggedInMember.requireIdOf(authentication), clipId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "이 글에서 문장을 모은 사람 수", description = "글 주인만 본다. 누가 무엇을 모았는지는 알려 주지 않는다.")
    @GetMapping("/api/{version}/posts/{postId}/clips/collectors")
    public Collectors collectors(@PathVariable Long postId, Authentication authentication) {
        return new Collectors(clipService.collectors(loggedInMember.requireIdOf(authentication), postId));
    }

    public record Collectors(long collectors) {
    }
}
