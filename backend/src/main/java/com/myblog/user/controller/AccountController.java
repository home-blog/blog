package com.myblog.user.controller;

import com.myblog.user.controller.dto.AccountRequests;
import com.myblog.user.domain.User;
import com.myblog.user.security.MemberPrincipal;
import com.myblog.user.service.CurrentMemberService;
import com.myblog.user.service.PasswordChangeService;
import com.myblog.user.config.SessionConfig;
import com.myblog.user.service.ProfileService;
import com.myblog.user.service.WithdrawalService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import java.time.Instant;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.authentication.logout.CookieClearingLogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
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
    private final PasswordChangeService passwordChangeService;
    private final WithdrawalService withdrawalService;
    private final SecurityContextLogoutHandler contextLogout = new SecurityContextLogoutHandler();
    private final CookieClearingLogoutHandler cookieLogout = new CookieClearingLogoutHandler(SessionConfig.COOKIE_NAME);

    public AccountController(CurrentMemberService currentMember, ProfileService profileService,
            PasswordChangeService passwordChangeService, WithdrawalService withdrawalService) {
        this.currentMember = currentMember;
        this.profileService = profileService;
        this.passwordChangeService = passwordChangeService;
        this.withdrawalService = withdrawalService;
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

    /**
     * 비밀번호 변경 (contracts 3). 지금 기기(이 세션)는 로그인이 유지되고 다른 기기는 모두 끊긴다 (FR-020).
     * 세션은 Spring Session이 감싼 것이라 getId()가 세션 표의 ID다. 비밀번호는 응답·로그에 넣지 않는다 (NF-01).
     */
    @PostMapping("/password")
    public MessageResponse changePassword(@AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody AccountRequests.ChangePassword request, HttpSession session) {
        User member = currentMember.get(principal);
        passwordChangeService.change(member, request.currentPassword(), request.newPassword(), session.getId());
        return new MessageResponse("비밀번호를 변경했습니다");
    }

    /**
     * 회원 탈퇴 (contracts 4). 성공하면 이 기기도 로그아웃시킨다: 로그인 정보를 지우고 세션을 끝내고 쿠키를 만료시킨다 (FR-027).
     * 다른 기기의 세션은 서비스가 이미 모두 끝냈다.
     */
    @PostMapping("/withdrawal")
    public MessageResponse withdraw(@AuthenticationPrincipal MemberPrincipal principal,
            @Valid @RequestBody AccountRequests.Withdraw request, Authentication authentication,
            HttpServletRequest httpRequest, HttpServletResponse httpResponse) {
        User member = currentMember.get(principal);
        withdrawalService.withdraw(member, request.password());
        contextLogout.logout(httpRequest, httpResponse, authentication);
        cookieLogout.logout(httpRequest, httpResponse, authentication);
        return new MessageResponse("탈퇴가 완료되었습니다");
    }

    public record MessageResponse(String message) {
    }

    public record AccountResponse(String email, String nickname, String intro, Instant joinedAt, BlogRef blog) {
    }

    public record BlogRef(Long id) {
    }

    public record ProfileUpdatedResponse(String message, String nickname, String intro) {
    }
}
