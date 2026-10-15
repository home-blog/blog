package com.myblog.post.controller;

import com.myblog.post.controller.dto.DraftRequests;
import com.myblog.post.service.DraftService;
import com.myblog.post.service.DraftService.DraftList;
import com.myblog.post.service.DraftService.DraftView;
import com.myblog.post.service.DraftService.Saved;
import com.myblog.user.LoggedInMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 임시 저장·예약 발행 (specs/013 US3, US4, contracts 3, 4). 모두 로그인한 주인만, 남의 것은 404.
 * 검사는 서비스가 주인을 확인한 뒤에 한다(남의 임시 저장 글이면 형식 오류보다 404가 먼저).
 */
@Tag(name = "12. 글쓰기 도구")
@RestController
public class DraftController {

    private final LoggedInMember loggedInMember;
    private final DraftService draftService;

    public DraftController(LoggedInMember loggedInMember, DraftService draftService) {
        this.loggedInMember = loggedInMember;
        this.draftService = draftService;
    }

    @Operation(summary = "내 임시 저장 글 목록", description = "마지막 저장 순. 예약된 글은 publishAt이 있다. maxCount는 `writing.draft.max-count`.")
    @GetMapping("/api/{version}/me/drafts")
    public DraftList list(@Parameter(hidden = true) Authentication authentication) {
        return draftService.list(loggedInMember.requireIdOf(authentication));
    }

    @Operation(summary = "임시 저장", description = "제목·본문이 비어도 된다. 글자 수 상한과 내 분류·주제·연재인지만 본다. 회원마다 `writing.draft.max-count`개.")
    @PostMapping("/api/{version}/me/drafts")
    @ResponseStatus(HttpStatus.CREATED)
    public Saved create(@RequestBody DraftRequests.Save request, @Parameter(hidden = true) Authentication authentication) {
        return draftService.create(loggedInMember.requireIdOf(authentication), request);
    }

    @Operation(summary = "임시 저장 글 하나")
    @GetMapping("/api/{version}/me/drafts/{draftId}")
    public DraftView get(@PathVariable Long draftId, @Parameter(hidden = true) Authentication authentication) {
        return draftService.get(loggedInMember.requireIdOf(authentication), draftId);
    }

    @Operation(summary = "임시 저장 글 다시 저장", description = "예약된 글이면 예약은 그대로 둔다.")
    @PutMapping("/api/{version}/me/drafts/{draftId}")
    public Saved save(@PathVariable Long draftId, @RequestBody DraftRequests.Save request,
            @Parameter(hidden = true) Authentication authentication) {
        return draftService.save(loggedInMember.requireIdOf(authentication), draftId, request);
    }

    @Operation(summary = "임시 저장 글 지우기")
    @DeleteMapping("/api/{version}/me/drafts/{draftId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long draftId, @Parameter(hidden = true) Authentication authentication) {
        draftService.delete(loggedInMember.requireIdOf(authentication), draftId);
    }

    @Operation(summary = "임시 저장 글 발행", description = "보통 글쓰기와 같은 검사. 글을 만들고 임시 저장 글은 지운다. seriesId를 주면 그 연재의 끝 화로 넣는다.")
    @PostMapping("/api/{version}/me/drafts/{draftId}/publish")
    @ResponseStatus(HttpStatus.CREATED)
    public PublishedResponse publish(@PathVariable Long draftId, @RequestBody DraftRequests.Publish request,
            @Parameter(hidden = true) Authentication authentication) {
        return new PublishedResponse(draftService.publish(loggedInMember.requireIdOf(authentication), draftId, request).postId());
    }

    @Operation(summary = "예약 발행", description = "보통 글쓰기 검사 + 시각 검사(초는 버림, `writing.schedule.min-lead` 뒤 ~ "
            + "`writing.schedule.max-days`일 안). 시각이 되면 1분 안에(`writing.schedule.check-interval`) 글로 올라간다.")
    @PutMapping("/api/{version}/me/drafts/{draftId}/schedule")
    public Saved schedule(@PathVariable Long draftId, @RequestBody DraftRequests.Schedule request,
            @Parameter(hidden = true) Authentication authentication) {
        return draftService.schedule(loggedInMember.requireIdOf(authentication), draftId, request);
    }

    @Operation(summary = "예약 취소", description = "임시 저장 글로 되돌린다.")
    @DeleteMapping("/api/{version}/me/drafts/{draftId}/schedule")
    public Saved unschedule(@PathVariable Long draftId, @Parameter(hidden = true) Authentication authentication) {
        return draftService.unschedule(loggedInMember.requireIdOf(authentication), draftId);
    }

    public record PublishedResponse(Long postId) {
    }
}
