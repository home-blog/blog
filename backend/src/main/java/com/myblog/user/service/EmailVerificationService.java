package com.myblog.user.service;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.common.error.ErrorResponse;
import com.myblog.user.config.AuthProperties;
import com.myblog.user.domain.User;
import com.myblog.user.mail.MailSendFailedException;
import com.myblog.user.mail.VerificationMailSender;
import com.myblog.user.repository.UserRepository;
import com.myblog.user.validation.AuthFieldErrorMessages;
import com.myblog.user.validation.UserInputRules;
import com.myblog.user.verification.EmailVerificationStore;
import com.myblog.user.verification.VerificationCodeGenerator;
import java.time.Duration;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/** 이메일 인증 (specs/001 contracts 2 ~ 4, FR-013 ~ FR-021). */
@Service
public class EmailVerificationService {

    private static final Logger log = LoggerFactory.getLogger(EmailVerificationService.class);

    private final AuthProperties properties;
    private final UserInputRules rules;
    private final AuthFieldErrorMessages fieldMessages;
    private final UserRepository users;
    private final EmailVerificationStore store;
    private final VerificationCodeGenerator generator;
    private final VerificationMailSender mailSender;

    public EmailVerificationService(AuthProperties properties, UserInputRules rules, AuthFieldErrorMessages fieldMessages,
            UserRepository users, EmailVerificationStore store, VerificationCodeGenerator generator,
            VerificationMailSender mailSender) {
        this.properties = properties;
        this.rules = rules;
        this.fieldMessages = fieldMessages;
        this.users = users;
        this.store = store;
        this.generator = generator;
        this.mailSender = mailSender;
    }

    /** 인증번호를 보낸 결과: 번호의 유효 시간과, 이 사람만 가지는 인증 증표. */
    public record SentCode(Duration validFor, String verificationToken) {
    }

    /** 인증번호 받기. */
    public SentCode send(String rawNickname, String rawEmail) {
        AuthProperties.EmailVerification config = properties.emailVerification();

        // ① 형식
        if (!rules.isValidEmail(rawEmail)) {
            throw fieldError(ErrorCode.INVALID_EMAIL, "email");
        }
        String nickname = rawNickname == null ? null : rawNickname.strip();
        if (!rules.isValidNickname(nickname)) {
            throw fieldError(ErrorCode.INVALID_NICKNAME, "nickname");
        }
        String email = User.normalizeEmail(rawEmail);

        // ② 이미 가입된 이메일이면 메일을 보내지 않는다 (FR-004)  ③ 닉네임 중복
        if (users.existsActiveByEmail(email)) {
            throw fieldError(ErrorCode.EMAIL_ALREADY_REGISTERED, "email");
        }
        if (users.existsActiveByNickname(nickname)) {
            throw fieldError(ErrorCode.NICKNAME_ALREADY_USED, "nickname");
        }

        // ④ 1분에 1번, 하루 5번 (FR-018). 1분 막기는 확인과 동시에 건다 (동시에 여러 번 눌러도 하나만 통과)
        if (!store.tryStartCooldown(email, config.resendInterval())) {
            throw new ApiException(ErrorCode.RESEND_TOO_SOON);
        }
        if (store.sentCount(email) >= config.dailyLimit()) {
            store.releaseCooldown(email);
            throw new ApiException(ErrorCode.RESEND_DAILY_LIMIT);
        }

        // ⑤ 새 번호와 증표 저장 (이전 번호·증표 무효)  ⑥ 메일이 나갈 때까지 기다린다
        String code = generator.generate();
        String token = generator.newFlowToken();
        store.saveCode(email, code, config.codeTtl());
        store.saveFlowToken(email, token, config.codeTtl());
        try {
            mailSender.send(email, code, config.codeTtl());
        } catch (MailSendFailedException e) {
            // ⑦ 실패하면 번호를 지우고 횟수에 넣지 않는다 → 바로 다시 받을 수 있다 (FR-020)
            log.warn("인증번호 메일 발송 실패: {}", e.getMessage());
            store.clearFlow(email);
            store.releaseCooldown(email);
            throw new ApiException(ErrorCode.MAIL_SEND_FAILED);
        }
        store.recordSent(email);
        return new SentCode(config.codeTtl(), token);
    }

    /** 인증번호 확인. 맞으면 번호를 바로 지우고 인증됨 표시를 남긴다 (FR-015, FR-016, FR-019). */
    public void confirm(String rawEmail, String rawCode, String verificationToken) {
        AuthProperties.EmailVerification config = properties.emailVerification();
        String email = User.normalizeEmail(rawEmail);
        // 인증번호를 받은 그 사람의 증표가 아니면 번호가 없는 것과 같게 답한다
        if (email == null || email.isEmpty() || !store.isFlowToken(email, verificationToken)) {
            throw new ApiException(ErrorCode.CODE_EXPIRED);
        }
        String saved = store.findCode(email).orElseThrow(() -> new ApiException(ErrorCode.CODE_EXPIRED));
        String input = rawCode == null ? "" : rawCode.strip().toUpperCase(Locale.ROOT);

        if (saved.equals(input)) {
            store.deleteCode(email);
            store.markVerified(email, verificationToken, config.verifiedTtl());
            return;
        }
        long failures = store.incrementFailures(email, config.codeTtl());
        if (failures >= config.maxWrongAttempts()) {
            store.deleteCode(email);
            throw new ApiException(ErrorCode.CODE_ATTEMPTS_EXCEEDED);
        }
        throw new ApiException(ErrorCode.CODE_MISMATCH);
    }

    /**
     * 이메일 변경: 번호, 틀린 횟수, 증표, 인증됨 표시를 지운다. 1분·하루 횟수는 남긴다 (FR-021).
     * 그 인증을 시작한 사람(증표가 맞는 사람)만 지울 수 있다. 아니면 아무것도 하지 않는다.
     */
    public void cancel(String rawEmail, String verificationToken) {
        String email = User.normalizeEmail(rawEmail);
        if (email == null || email.isEmpty()) {
            return;
        }
        if (store.isFlowToken(email, verificationToken) || store.isVerified(email, verificationToken)) {
            store.clearFlow(email);
        }
    }

    private ApiException fieldError(ErrorCode code, String field) {
        String message = fieldMessages.messageFor(code).orElse(code.message());
        return new ApiException(code, message, List.of(new ErrorResponse.FieldErrorItem(field, code.name(), message)));
    }
}
