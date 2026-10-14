package com.myblog.user.controller;

import com.myblog.user.controller.dto.AuthRequests;
import com.myblog.user.domain.User;
import com.myblog.user.security.MemberPrincipal;
import com.myblog.user.service.CurrentMemberService;
import com.myblog.user.service.EmailVerificationService;
import com.myblog.user.service.LoginService;
import com.myblog.user.service.SignupService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
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
 * 로그아웃(POST /api/v1/auth/logout)은 Spring Security의 LogoutFilter가 처리한다 (SecurityConfig).
 */
@Tag(name = "1. 로그인·가입")
@RestController
@RequestMapping("/api/{version}/auth")
public class AuthController {

    private final EmailVerificationService verification;
    private final SignupService signupService;
    private final LoginService loginService;
    private final CurrentMemberService currentMember;

    public AuthController(EmailVerificationService verification, SignupService signupService, LoginService loginService,
            CurrentMemberService currentMember) {
        this.verification = verification;
        this.signupService = signupService;
        this.loginService = loginService;
        this.currentMember = currentMember;
    }

    @Operation(summary = "이메일로 로그인", description = "성공하면 서버 세션(쿠키)이 생긴다. 5번 틀리면 10분 잠긴다. 로그아웃은 POST /api/v1/auth/logout (Spring Security가 처리).")
    @PostMapping("/login")
    public MemberResponse login(@Valid @RequestBody AuthRequests.Login request, HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        MemberPrincipal member = loginService.login(request.email(), request.password(), httpRequest, httpResponse);
        return MemberResponse.of(member);
    }

    /**
     * 지금 로그인했는지 (contracts 8). 화면이 처음 열릴 때와 닉네임을 바꾼 뒤 부른다.
     * 세션에 남은 값이 아니라 DB의 닉네임을 돌려주고, 탈퇴한 회원이면 401이다 (specs/002 T005).
     */
    @Operation(summary = "지금 로그인한 회원", description = "로그인하지 않았거나 탈퇴한 회원이면 401.")
    @GetMapping("/me")
    public MemberResponse me(@AuthenticationPrincipal MemberPrincipal principal) {
        User member = currentMember.get(principal);
        return new MemberResponse(new Member(member.getId(), member.getNickname()));
    }

    @Operation(summary = "이메일 인증번호 보내기", description = "6자리 번호를 메일로 보낸다(10분 유효, 1분에 한 번, 하루 5번). 받은 verificationToken을 확인·가입 요청에 다시 보낸다.")
    @PostMapping("/email-verifications")
    public SendCodeResponse sendCode(@RequestBody AuthRequests.SendCode request) {
        EmailVerificationService.SentCode sent = verification.send(request.nickname(), request.email());
        Duration validFor = sent.validFor();
        return new SendCodeResponse(
                "인증번호를 보냈습니다. %d분 안에 입력해 주세요".formatted(validFor.toMinutes()),
                validFor.toSeconds(),
                sent.verificationToken());
    }

    @Operation(summary = "인증번호 확인", description = "5번 틀리면 다시 받아야 한다.")
    @PostMapping("/email-verifications/confirm")
    public MessageResponse confirmCode(@RequestBody AuthRequests.ConfirmCode request) {
        verification.confirm(request.email(), request.code(), request.verificationToken());
        return new MessageResponse("이메일 인증이 완료되었습니다");
    }

    @Operation(summary = "인증 그만두기", description = "이메일을 바꾸거나 가입 화면을 떠날 때.")
    @PostMapping("/email-verifications/cancel")
    public ResponseEntity<Void> cancel(@RequestBody AuthRequests.CancelVerification request) {
        verification.cancel(request.email(), request.verificationToken());
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "회원 가입", description = "인증을 마친 이메일로만 된다. 블로그와 \"미분류\" 분류가 함께 만들어진다.")
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
