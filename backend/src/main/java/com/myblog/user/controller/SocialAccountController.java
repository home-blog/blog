package com.myblog.user.controller;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.user.config.SocialProperties;
import com.myblog.user.domain.User;
import com.myblog.user.security.MemberPrincipal;
import com.myblog.user.service.CurrentMemberService;
import com.myblog.user.service.SocialAccountService;
import com.myblog.user.social.SocialClientRegistrations;
import com.myblog.user.social.SocialProvider;
import com.myblog.user.social.SocialSessionState;
import jakarta.servlet.http.HttpSession;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 마이페이지 "연결된 계정" (specs/010 contracts 6 ~ 8). 로그인해야 부른다. */
@RestController
@RequestMapping("/api/{version}/account/social")
public class SocialAccountController {

    private final CurrentMemberService currentMember;
    private final SocialAccountService socialAccounts;
    private final SocialClientRegistrations registrations;
    private final SocialProperties properties;
    private final Clock clock;

    public SocialAccountController(CurrentMemberService currentMember, SocialAccountService socialAccounts,
            SocialClientRegistrations registrations, SocialProperties properties, Clock clock) {
        this.currentMember = currentMember;
        this.socialAccounts = socialAccounts;
        this.registrations = registrations;
        this.properties = properties;
        this.clock = clock;
    }

    @GetMapping
    public SocialAccountsResponse view(@AuthenticationPrincipal MemberPrincipal principal) {
        User member = currentMember.get(principal);
        List<Linked> linked = socialAccounts.linkedOf(member.getId()).stream()
                .map(account -> new Linked(account.getProvider(), account.getLinkedAt()))
                .toList();
        return new SocialAccountsResponse(member.getPasswordHash() != null,
                registrations.enabled().stream().map(SocialProvider::id).toList(), linked);
    }

    /** 연결하기 시작: 세션에 "이 회원에 연결"을 적는다. 화면은 이어서 /oauth2/authorization/{provider}로 간다. */
    @PostMapping("/{provider}/link")
    public ResponseEntity<Void> startLink(@AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable("provider") String provider, HttpSession session) {
        User member = currentMember.get(principal);
        SocialProvider target = enabledProvider(provider);
        session.setAttribute(SocialSessionState.LINK,
                new SocialSessionState.LinkIntent(member.getId(), target, Instant.now(clock).plus(properties.pendingTtl())));
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{provider}")
    public ResponseEntity<Void> unlink(@AuthenticationPrincipal MemberPrincipal principal,
            @PathVariable("provider") String provider) {
        User member = currentMember.get(principal);
        SocialProvider target = SocialProvider.of(provider).orElseThrow(() -> new ApiException(ErrorCode.SOCIAL_NOT_LINKED));
        socialAccounts.unlink(member.getId(), target);
        return ResponseEntity.noContent().build();
    }

    private SocialProvider enabledProvider(String id) {
        return SocialProvider.of(id).filter(registrations::isEnabled)
                .orElseThrow(() -> new ApiException(ErrorCode.SOCIAL_PROVIDER_UNKNOWN));
    }

    public record SocialAccountsResponse(boolean hasPassword, List<String> providers, List<Linked> linked) {
    }

    public record Linked(String provider, Instant linkedAt) {
    }
}
