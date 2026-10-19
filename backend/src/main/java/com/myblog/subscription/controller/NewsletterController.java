package com.myblog.subscription.controller;

import com.myblog.subscription.service.Newsletter;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 원고 편지 그만 받기 (specs/016 contracts 3). 메일의 링크로 로그인 없이 부른다. */
@Tag(name = "14. 구독·알림")
@RestController
public class NewsletterController {

    private final Newsletter newsletter;

    public NewsletterController(Newsletter newsletter) {
        this.newsletter = newsletter;
    }

    public record UnsubscribeRequest(String token) {
    }

    /** blogName은 메일을 끈 블로그 이름 (받아 보기가 이미 없으면 null). */
    public record Unsubscribed(String blogName) {
    }

    @Operation(summary = "원고 편지 그만 받기", description = "메일 속 서명된 값으로, 로그인 없이 그 블로그의 메일만 끈다. 잘못된 값은 400.")
    @PostMapping("/api/{version}/newsletter/unsubscribe")
    public Unsubscribed unsubscribe(@RequestBody UnsubscribeRequest request) {
        return new Unsubscribed(newsletter.unsubscribe(request.token()));
    }
}
