package com.myblog.user.service;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.common.error.ErrorResponse;
import com.myblog.user.config.SocialProperties;
import com.myblog.user.domain.SocialAccount;
import com.myblog.user.domain.User;
import com.myblog.user.repository.SocialAccountRepository;
import com.myblog.user.repository.UserRepository;
import com.myblog.user.social.SocialProfile;
import com.myblog.user.social.SocialProvider;
import com.myblog.user.social.SocialSessionState;
import com.myblog.user.validation.UserInputRules;
import com.myblog.user.verification.EmailVerificationStore;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 소셜 계정과 회원을 잇는 규칙 (specs/010 US1 ~ US5).
 * 소셜 계정 하나는 회원 하나에만, 회원 하나는 회사마다 하나만 연결된다 (D-10, DB의 중복 불가 둘이 마지막으로 막는다).
 */
@Service
public class SocialAccountService {

    /** 인증 안 된 이메일이 기존 회원과 같을 때 (US3-2). */
    static final String EMAIL_TAKEN_MESSAGE = "이미 가입된 이메일입니다. 그 계정으로 로그인한 뒤 마이페이지에서 연결해 주세요";

    private final SocialAccountRepository accounts;
    private final UserRepository users;
    private final MemberRegistration registration;
    private final EmailVerificationStore verificationStore;
    private final UserInputRules rules;
    private final SocialProperties properties;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public SocialAccountService(SocialAccountRepository accounts, UserRepository users, MemberRegistration registration,
            EmailVerificationStore verificationStore, UserInputRules rules, SocialProperties properties,
            TransactionTemplate transaction, Clock clock) {
        this.accounts = accounts;
        this.users = users;
        this.registration = registration;
        this.verificationStore = verificationStore;
        this.rules = rules;
        this.properties = properties;
        this.transaction = transaction;
        this.clock = clock;
    }

    /** 회사 로그인을 마친 사람의 갈 곳: 로그인할 회원, 또는 가입 대기. */
    public sealed interface SignIn permits SignIn.Member, SignIn.Pending {

        record Member(User user) implements SignIn {
        }

        record Pending(SocialSessionState.Pending pending) implements SignIn {
        }
    }

    /**
     * US1·US3: ① 이미 연결된 회원 → 그 회원 ② 회사가 인증한 이메일과 같은 회원 → 연결하고 그 회원 (D-1)
     * ③ 그 밖 → 가입 대기 (닉네임 제안을 붙여서).
     */
    public SignIn signIn(SocialProfile profile) {
        Optional<User> linked = linkedMember(profile);
        if (linked.isPresent()) {
            return new SignIn.Member(linked.get());
        }
        if (profile.emailVerified() && profile.email() != null) {
            Optional<User> sameEmail = users.findActiveByEmail(User.normalizeEmail(profile.email()));
            if (sameEmail.isPresent()
                    && !accounts.existsByUserIdAndProvider(sameEmail.get().getId(), profile.provider().id())) {
                try {
                    link(sameEmail.get().getId(), profile);
                    return new SignIn.Member(sameEmail.get());
                } catch (ApiException e) {
                    // 거의 동시에 같은 계정이 다른 곳에 연결됐다: 연결된 회원을 다시 찾는다
                    Optional<User> raced = linkedMember(profile);
                    if (raced.isPresent()) {
                        return new SignIn.Member(raced.get());
                    }
                }
            }
        }
        Instant expiresAt = Instant.now(clock).plus(properties.pendingTtl());
        return new SignIn.Pending(new SocialSessionState.Pending(profile.provider(), profile.providerUserId(),
                profile.email() == null ? null : User.normalizeEmail(profile.email()), profile.emailVerified(),
                suggestNickname(profile.name()), expiresAt));
    }

    /** 이 소셜 계정에 연결된, 탈퇴하지 않은 회원. */
    public Optional<User> linkedMember(SocialProfile profile) {
        return accounts.findByProviderAndProviderUserId(profile.provider().id(), profile.providerUserId())
                .flatMap(account -> users.findActiveById(account.getUserId()));
    }

    /** US4 연결하기. 이미 내 것이면 그대로 둔다. */
    public void link(Long memberId, SocialProfile profile) {
        String provider = profile.provider().id();
        Optional<SocialAccount> existing = accounts.findByProviderAndProviderUserId(provider, profile.providerUserId());
        if (existing.isPresent()) {
            if (existing.get().getUserId().equals(memberId)) {
                return;
            }
            throw new ApiException(ErrorCode.SOCIAL_ACCOUNT_IN_USE);
        }
        if (accounts.existsByUserIdAndProvider(memberId, provider)) {
            throw new ApiException(ErrorCode.SOCIAL_ALREADY_LINKED);
        }
        try {
            transaction.executeWithoutResult(status -> accounts.saveAndFlush(
                    SocialAccount.link(memberId, provider, profile.providerUserId(), Instant.now(clock))));
        } catch (DataIntegrityViolationException e) {
            throw constraint(e);
        }
    }

    /**
     * US2 소셜 가입. 회사가 확인한 이메일이 있으면 그것을, 없으면 001 이메일 인증을 마친 이메일을 쓴다.
     * 회원·블로그(가입 이벤트)·연결을 한 트랜잭션으로 만든다. 비밀번호는 없다 (D-4).
     */
    public User signup(SocialSessionState.Pending pending, String rawNickname, String rawEmail, String verificationToken) {
        String nickname = rawNickname == null ? null : rawNickname.strip();
        if (!rules.isValidNickname(nickname)) {
            throw fieldError(ErrorCode.INVALID_NICKNAME, "nickname", ErrorCode.INVALID_NICKNAME.message());
        }
        boolean usesVerificationCode = !pending.hasVerifiedEmail();
        String email = usesVerificationCode ? User.normalizeEmail(rawEmail) : pending.email();
        if (usesVerificationCode) {
            if (!rules.isValidEmail(email)) {
                throw fieldError(ErrorCode.INVALID_EMAIL, "email", ErrorCode.INVALID_EMAIL.message());
            }
            if (!verificationStore.isVerified(email, verificationToken)) {
                throw new ApiException(ErrorCode.EMAIL_NOT_VERIFIED);
            }
        }
        if (users.existsActiveByEmail(email)) {
            throw fieldError(ErrorCode.EMAIL_ALREADY_REGISTERED, "email", EMAIL_TAKEN_MESSAGE);
        }
        if (users.existsActiveByNickname(nickname)) {
            throw fieldError(ErrorCode.NICKNAME_ALREADY_USED, "nickname", ErrorCode.NICKNAME_ALREADY_USED.message());
        }
        if (accounts.findByProviderAndProviderUserId(pending.provider().id(), pending.providerUserId()).isPresent()) {
            throw new ApiException(ErrorCode.SOCIAL_ACCOUNT_IN_USE);
        }

        User member;
        try {
            member = transaction.execute(status -> {
                User created = registration.register(email, null, nickname);
                accounts.saveAndFlush(SocialAccount.link(created.getId(), pending.provider().id(),
                        pending.providerUserId(), Instant.now(clock)));
                return created;
            });
        } catch (DataIntegrityViolationException e) {
            throw constraint(e);
        }
        if (usesVerificationCode) {
            verificationStore.clearVerified(email);
        }
        return member;
    }

    /** US4 연결 해제. 비밀번호도 없고 이 연결이 마지막이면 막는다 (D-6). */
    public void unlink(Long memberId, SocialProvider provider) {
        transaction.executeWithoutResult(status -> {
            User member = users.findActiveByIdForUpdate(memberId)
                    .orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
            SocialAccount account = accounts.findByUserIdAndProvider(memberId, provider.id())
                    .orElseThrow(() -> new ApiException(ErrorCode.SOCIAL_NOT_LINKED));
            if (member.getPasswordHash() == null && accounts.countByUserId(memberId) <= 1) {
                throw new ApiException(ErrorCode.LAST_LOGIN_METHOD);
            }
            accounts.delete(account);
        });
    }

    /** 내 연결 목록 (연결한 순서). */
    public List<SocialAccount> linkedOf(Long memberId) {
        return accounts.findByUserIdOrderByLinkedAtAsc(memberId);
    }

    /** 탈퇴 트랜잭션 안에서 부른다 (US5). */
    public void deleteAllOf(Long memberId) {
        accounts.deleteAllByUserId(memberId);
    }

    /** 회사가 알려 준 이름이 닉네임 규칙에 맞고 아직 쓰이지 않았으면 제안한다. 아니면 비워 둔다 (US2-1). */
    String suggestNickname(String name) {
        if (name == null) {
            return null;
        }
        String candidate = name.strip();
        if (!rules.isValidNickname(candidate) || users.existsActiveByNickname(candidate)) {
            return null;
        }
        return candidate;
    }

    /** 거의 동시에 온 요청 중 늦은 쪽: DB의 중복 불가 이름으로 무엇이 겹쳤는지 알린다. */
    private static ApiException constraint(DataIntegrityViolationException e) {
        String detail = String.valueOf(e.getMostSpecificCause().getMessage());
        if (detail.contains("uq_users_nickname_active")) {
            return fieldError(ErrorCode.NICKNAME_ALREADY_USED, "nickname", ErrorCode.NICKNAME_ALREADY_USED.message());
        }
        if (detail.contains("uq_users_email_active")) {
            return fieldError(ErrorCode.EMAIL_ALREADY_REGISTERED, "email", EMAIL_TAKEN_MESSAGE);
        }
        if (detail.contains("uq_user_social_account_users_provider")) {
            return new ApiException(ErrorCode.SOCIAL_ALREADY_LINKED);
        }
        return new ApiException(ErrorCode.SOCIAL_ACCOUNT_IN_USE);
    }

    private static ApiException fieldError(ErrorCode code, String field, String message) {
        return new ApiException(code, message, List.of(new ErrorResponse.FieldErrorItem(field, code.name(), message)));
    }
}
