package com.myblog.user.controller;

import com.myblog.user.controller.dto.AccountRequests;
import com.myblog.user.domain.User;
import com.myblog.user.security.MemberPrincipal;
import com.myblog.user.service.CurrentMemberService;
import com.myblog.user.service.ProfileService;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 마이페이지 (specs/002 contracts). 모든 주소는 로그인해야 부를 수 있다 (SecurityConfig의 anyRequest().authenticated()).
 * 회원은 세션(@AuthenticationPrincipal) → CurrentMemberService로만 정한다. 주소·본문에서 회원 번호를 받지 않는다 (FR-002, FR-029).
 */
@RestController
@RequestMapping("/api/account")
public class AccountController {

    private final CurrentMemberService currentMember;
    private final ProfileService profileService;

    public AccountController(CurrentMemberService currentMember, ProfileService profileService) {
        this.currentMember = currentMember;
        this.profileService = profileService;
    }

    /** 내 정보 보기 (contracts 1). 비밀번호(해시)는 넣지 않는다. */
    @GetMapping
    public AccountResponse view(@AuthenticationPrincipal MemberPrincipal principal) {
        ProfileService.Profile profile = profileService.view(currentMember.get(principal));
        return new AccountResponse(profile.email(), profile.nickname(), profile.intro(), profile.joinedAt(),
                profile.blogId() == null ? null : new BlogRef(profile.blogId()));
    }

    /** 내 정보 저장 (contracts 2). */
    @PatchMapping("/profile")
    public ProfileUpdatedResponse updateProfile(@AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody AccountRequests.UpdateProfile request) {
        User member = currentMember.get(principal);
        User updated = profileService.update(member.getId(), request.nickname(), request.intro());
        return new ProfileUpdatedResponse("저장했습니다", updated.getNickname(), ProfileService.introOf(updated));
    }

    public record AccountResponse(String email, String nickname, String intro, Instant joinedAt, BlogRef blog) {
    }

    public record BlogRef(Long id) {
    }

    public record ProfileUpdatedResponse(String message, String nickname, String intro) {
    }
}
