package com.myblog.notice.controller;

import com.myblog.notice.service.NoticeService;
import com.myblog.notice.service.NoticeService.NoticeDetail;
import com.myblog.notice.service.NoticeService.NoticeInput;
import com.myblog.notice.service.NoticeService.NoticePage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 공지사항 (specs/012 contracts). 읽기는 누구나, 쓰기·고치기·지우기는 관리자만(서비스가 확인). */
@Tag(name = "11. 공지사항")
@RestController
@RequestMapping("/api/{version}/notices")
public class NoticeController {

    private final NoticeService noticeService;

    public NoticeController(NoticeService noticeService) {
        this.noticeService = noticeService;
    }

    @Operation(summary = "공지사항 목록", description = "kind로 탭(update·notice, 없거나 모르는 값이면 전체)을 고른다. 고정 먼저, 그 안에서 새 글 먼저. 한 쪽 글 수는 `notice.page-size`.")
    @GetMapping
    public NoticePage list(@RequestParam(required = false) String kind, @RequestParam(required = false) String page) {
        return noticeService.list(kind, page);
    }

    @Operation(summary = "가장 새 공지사항 시각", description = "머리글의 새 글 표시용. 글이 없으면 latestAt이 null.")
    @GetMapping("/latest")
    public LatestResponse latest() {
        return new LatestResponse(noticeService.latestAt().orElse(null));
    }

    @Operation(summary = "공지사항 읽기", description = "업데이트 글에는 그 번호의 GitHub 릴리스 주소(releaseUrl)가 있다. 없으면 404.")
    @GetMapping("/{noticeId:\\d+}")
    public NoticeDetail read(@PathVariable Long noticeId) {
        return noticeService.read(noticeId);
    }

    @Operation(summary = "공지사항 쓰기 (관리자)", description = "업데이트에는 릴리스 번호(0.6.0 모양)가 꼭 있고, 공지에는 넣지 않는다. 관리자가 아니면 403.")
    @PostMapping
    public ResponseEntity<IdResponse> write(@RequestBody NoticeInput input, @Parameter(hidden = true) Authentication authentication) {
        return ResponseEntity.status(HttpStatus.CREATED).body(new IdResponse(noticeService.write(authentication, input)));
    }

    @Operation(summary = "공지사항 고치기 (관리자)", description = "칸은 쓰기와 같다.")
    @PutMapping("/{noticeId:\\d+}")
    public IdResponse change(@PathVariable Long noticeId, @RequestBody NoticeInput input,
            @Parameter(hidden = true) Authentication authentication) {
        return new IdResponse(noticeService.change(authentication, noticeId, input));
    }

    @Operation(summary = "공지사항 지우기 (관리자)", description = "지운 글은 다시 볼 수 없다.")
    @DeleteMapping("/{noticeId:\\d+}")
    public ResponseEntity<Void> delete(@PathVariable Long noticeId, @Parameter(hidden = true) Authentication authentication) {
        noticeService.delete(authentication, noticeId);
        return ResponseEntity.noContent().build();
    }

    public record LatestResponse(Instant latestAt) {
    }

    public record IdResponse(Long noticeId) {
    }
}
