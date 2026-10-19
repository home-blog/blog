package com.myblog.subscription.controller;

import com.myblog.subscription.service.NotificationService;
import com.myblog.subscription.service.NotificationService.NotificationPage;
import com.myblog.user.LoggedInMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 우편함 (specs/016 contracts 2). 모두 로그인이 필요하다. */
@Tag(name = "14. 구독·알림")
@RestController
public class NotificationController {

    private final LoggedInMember loggedInMember;
    private final NotificationService notificationService;

    public NotificationController(LoggedInMember loggedInMember, NotificationService notificationService) {
        this.loggedInMember = loggedInMember;
        this.notificationService = notificationService;
    }

    @Operation(summary = "우편함", description = "받아 보는 블로그의 새 글, 내 글에 달린 댓글. 최근 순, 30일 보관.")
    @GetMapping("/api/{version}/me/notifications")
    public NotificationPage list(@RequestParam(required = false) String page, Authentication authentication) {
        return notificationService.list(loggedInMember.requireIdOf(authentication), page);
    }

    @Operation(summary = "안 읽은 소식 수", description = "머리글이 1분마다 묻는다.")
    @GetMapping("/api/{version}/me/notifications/unread-count")
    public UnreadCount unreadCount(Authentication authentication) {
        return new UnreadCount(notificationService.unreadCount(loggedInMember.requireIdOf(authentication)));
    }

    @Operation(summary = "소식 읽음")
    @PostMapping("/api/{version}/me/notifications/{notificationId}/read")
    public ResponseEntity<Void> read(@PathVariable Long notificationId, Authentication authentication) {
        notificationService.read(loggedInMember.requireIdOf(authentication), notificationId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "모두 읽음")
    @PostMapping("/api/{version}/me/notifications/read-all")
    public ResponseEntity<Void> readAll(Authentication authentication) {
        notificationService.readAll(loggedInMember.requireIdOf(authentication));
        return ResponseEntity.noContent().build();
    }

    public record UnreadCount(long unreadCount) {
    }
}
