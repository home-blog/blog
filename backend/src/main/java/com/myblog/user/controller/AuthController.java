package com.myblog.user.controller;

import com.myblog.user.controller.dto.AuthRequests;
import com.myblog.user.service.EmailVerificationService;
import com.myblog.user.service.SignupService;
import jakarta.validation.Valid;
import java.time.Duration;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 이메일 인증과 가입 (specs/001 contracts 2 ~ 5). 문구는 상세/01 `안내 문구` 표를 따른다. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final EmailVerificationService verification;
    private final SignupService signupService;

    public AuthController(EmailVerificationService verification, SignupService signupService) {
        this.verification = verification;
        this.signupService = signupService;
    }

    @PostMapping("/email-verifications")
    public SendCodeResponse sendCode(@RequestBody AuthRequests.SendCode request) {
        Duration validFor = verification.send(request.nickname(), request.email());
        return new SendCodeResponse(
                "인증번호를 보냈습니다. %d분 안에 입력해 주세요".formatted(validFor.toMinutes()),
                validFor.toSeconds());
    }

    @PostMapping("/email-verifications/confirm")
    public MessageResponse confirmCode(@RequestBody AuthRequests.ConfirmCode request) {
        verification.confirm(request.email(), request.code());
        return new MessageResponse("이메일 인증이 완료되었습니다");
    }

    @PostMapping("/email-verifications/cancel")
    public ResponseEntity<Void> cancel(@RequestBody AuthRequests.CancelVerification request) {
        verification.cancel(request.email());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/signup")
    public ResponseEntity<MessageResponse> signup(@Valid @RequestBody AuthRequests.Signup request) {
        signupService.signup(request.nickname(), request.email(), request.password());
        return ResponseEntity.status(HttpStatus.CREATED).body(new MessageResponse("가입이 완료되었습니다. 로그인해 주세요"));
    }

    public record SendCodeResponse(String message, long expiresInSeconds) {
    }

    public record MessageResponse(String message) {
    }
}
