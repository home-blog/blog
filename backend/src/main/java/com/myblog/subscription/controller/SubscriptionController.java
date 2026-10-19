package com.myblog.subscription.controller;

import com.myblog.subscription.service.SubscriptionService;
import com.myblog.subscription.service.SubscriptionService.MySubscription;
import com.myblog.subscription.service.SubscriptionService.State;
import com.myblog.user.LoggedInMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 원고 받아 보기 (specs/016 contracts 1). 모두 로그인이 필요하다. 회원 번호는 LoggedInMember로만 얻는다. */
@Tag(name = "14. 구독·알림")
@RestController
public class SubscriptionController {

    private final LoggedInMember loggedInMember;
    private final SubscriptionService subscriptionService;

    public SubscriptionController(LoggedInMember loggedInMember, SubscriptionService subscriptionService) {
        this.loggedInMember = loggedInMember;
        this.subscriptionService = subscriptionService;
    }

    /** emailEnabled: 새 원고를 메일로도 받기 (본문이 없으면 false). */
    public record SubscribeRequest(Boolean emailEnabled) {
    }

    @Operation(summary = "원고 받아 보기", description = "이미 받아 보면 메일 받기만 바꾼다. 내 블로그는 400.")
    @PutMapping("/api/{version}/blogs/{blogId}/subscription")
    public State subscribe(@PathVariable Long blogId, @RequestBody(required = false) SubscribeRequest request,
            Authentication authentication) {
        boolean email = request != null && Boolean.TRUE.equals(request.emailEnabled());
        return subscriptionService.subscribe(loggedInMember.requireIdOf(authentication), blogId, email);
    }

    @Operation(summary = "그만 받아 보기", description = "받아 보지 않아도 200.")
    @DeleteMapping("/api/{version}/blogs/{blogId}/subscription")
    public State unsubscribe(@PathVariable Long blogId, Authentication authentication) {
        return subscriptionService.unsubscribe(loggedInMember.requireIdOf(authentication), blogId);
    }

    @Operation(summary = "내가 받아 보는 블로그", description = "최근에 받기 시작한 순.")
    @GetMapping("/api/{version}/me/subscriptions")
    public MySubscriptions mine(Authentication authentication) {
        return new MySubscriptions(subscriptionService.mine(loggedInMember.requireIdOf(authentication)));
    }

    public record MySubscriptions(List<MySubscription> items) {
    }
}
