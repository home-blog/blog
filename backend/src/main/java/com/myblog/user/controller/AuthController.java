package com.myblog.user.controller;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.user.controller.dto.AuthRequests;
import com.myblog.user.security.MemberPrincipal;
import com.myblog.user.service.EmailVerificationService;
import com.myblog.user.service.LoginService;
import com.myblog.user.service.SignupService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 이메일 인증, 가입, 로그인 (specs/001 contracts 2 ~ 8). 문구는 상세/01 `안내 문구` 표를 따른다.
 * 로그아웃(POST /api/auth/logout)은 Spring Security의 LogoutFilter가 처리한다 (SecurityConfig).
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final EmailVerificationService verification;
    private final SignupService signupService;
    private final LoginService loginService;

    public AuthController(EmailVerificationService verification, SignupService signupService, LoginService loginService) {
        this.verification = verification;
        this.signupService = signupService;
        this.loginService = loginService;
    }

    @PostMapping("/login")
    public MemberResponse login(@Valid @RequestBody AuthRequests.Login request, HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        MemberPrincipal member = loginService.login(request.email(), request.password(), httpRequest, httpResponse);
        return MemberResponse.of(member);
    }

    /** 지금 로그인했는지 (contracts 8). 화면이 처음 열릴 때 부른다. */
    @GetMapping("/me")
    public MemberResponse me(@AuthenticationPrincipal MemberPrincipal member) {
        if (member == null) {
            throw new ApiException(ErrorCode.UNAUTHENTICATED);
        }
        return MemberResponse.of(member);
    }

    @PostMapping("/email-verifications")
    public SendCodeResponse sendCode(@RequestBody AuthRequests.SendCode request) {
        EmailVerificationService.SentCode sent = verification.send(request.nickname(), request.email());
        Duration validFor = sent.validFor();
        return new SendCodeResponse(
                "인증번호를 보냈습니다. %d분 안에 입력해 주세요".formatted(validFor.toMinutes()),
                validFor.toSeconds(),
                sent.verificationToken());
    }

    @PostMapping("/email-verifications/confirm")
    public MessageResponse confirmCode(@RequestBody AuthRequests.ConfirmCode request) {
        verification.confirm(request.email(), request.code(), request.verificationToken());
        return new MessageResponse("이메일 인증이 완료되었습니다");
    }

    @PostMapping("/email-verifications/cancel")
    public ResponseEntity<Void> cancel(@RequestBody AuthRequests.CancelVerification request) {
        verification.cancel(request.email(), request.verificationToken());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/signup")
    public ResponseEntity<MessageResponse> signup(@Valid @RequestBody AuthRequests.Signup request) {
        signupService.signup(request.nickname(), request.email(), request.password(), request.verificationToken());
        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse("가입이 완료되었습니다. 로그인해 주세요"));
    }

    /** verificationToken은 확인·이메일 변경·가입 요청에 그대로 다시 보낸다. */
    public record SendCodeResponse(String message, long expiresInSeconds, String verificationToken) {
    }

    public record MessageResponse(String message) {
    }

    public record MemberResponse(Member member) {
        static MemberResponse of(MemberPrincipal principal) {
            return new MemberResponse(new Member(principal.getId(), principal.getNickname()));
        }
    }

    public record Member(Long id, String nickname) {
    }
}
