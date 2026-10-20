package com.myblog.guestbook.controller;

import com.myblog.guestbook.controller.dto.GuestbookRequests;
import com.myblog.guestbook.service.GuestbookService;
import com.myblog.guestbook.service.GuestbookService.EntryPage;
import com.myblog.guestbook.service.GuestbookService.EntryView;
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
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 방명록 (엽서함, specs/021 contracts 1 ~ 4). 회원 번호는 LoggedInMember로만 얻는다.
 * 목록은 누구나 부른다 (SecurityConfig의 GET /api/{버전}/blogs/**). 엽서 고치기 주소는 없다.
 */
@Tag(name = "18. 방명록")
@RestController
public class GuestbookController {

    private final LoggedInMember loggedInMember;
    private final GuestbookService guestbookService;

    public GuestbookController(LoggedInMember loggedInMember, GuestbookService guestbookService) {
        this.loggedInMember = loggedInMember;
        this.guestbookService = guestbookService;
    }

    @Operation(summary = "방명록 엽서 목록", description = "누구나 부른다. 최근 순, 한 쪽 12장. 남의 비밀 엽서는 봉한 엽서(sealed)로 온다.")
    @GetMapping("/api/{version}/blogs/{blogId}/guestbook")
    public EntryPage list(@PathVariable Long blogId, @RequestParam(required = false) String page,
            Authentication authentication) {
        return guestbookService.list(blogId, loggedInMember.idOf(authentication).orElse(null), page);
    }

    @Operation(summary = "엽서 남기기", description = "로그인한 회원(주인 말고). 1~300자, 우표 다섯 가지, 30초 간격, 한 블로그 하루 3장.")
    @PostMapping("/api/{version}/blogs/{blogId}/guestbook")
    @ResponseStatus(HttpStatus.CREATED)
    public EntryView write(@PathVariable Long blogId, Authentication authentication,
            @RequestBody GuestbookRequests.Write request) {
        return guestbookService.write(loggedInMember.requireIdOf(authentication), blogId, request);
    }

    @Operation(summary = "엽서 지우기", description = "쓴 사람 또는 블로그 주인. 답장도 함께 지운다.")
    @DeleteMapping("/api/{version}/guestbook/{entryId}")
    public ResponseEntity<Void> delete(@PathVariable Long entryId, Authentication authentication) {
        guestbookService.delete(loggedInMember.requireIdOf(authentication), entryId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "답장 쓰기·고치기", description = "블로그 주인만. 처음 쓸 때 엽서를 쓴 사람의 우편함에 소식이 간다.")
    @PutMapping("/api/{version}/guestbook/{entryId}/reply")
    public EntryView reply(@PathVariable Long entryId, Authentication authentication,
            @RequestBody GuestbookRequests.Reply request) {
        return guestbookService.reply(loggedInMember.requireIdOf(authentication), entryId, request);
    }

    @Operation(summary = "답장 지우기", description = "블로그 주인만.")
    @DeleteMapping("/api/{version}/guestbook/{entryId}/reply")
    public ResponseEntity<Void> deleteReply(@PathVariable Long entryId, Authentication authentication) {
        guestbookService.deleteReply(loggedInMember.requireIdOf(authentication), entryId);
        return ResponseEntity.noContent().build();
    }
}
