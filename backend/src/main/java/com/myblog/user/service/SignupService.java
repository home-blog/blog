package com.myblog.user.service;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.common.error.ErrorResponse;
import com.myblog.user.domain.User;
import com.myblog.user.repository.UserRepository;
import com.myblog.user.verification.EmailVerificationStore;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 가입하기 (specs/001 contracts 5, FR-001 ~ FR-012, FR-022, FR-023).
 * 칸 형식은 요청을 받을 때(@Valid) 이미 검사했다. 여기서는 인증됨 확인 → 중복 확인 → 저장 → 인증됨 표시 삭제.
 * 가입 뒤 자동 로그인은 하지 않는다 (CF-01-9).
 */
@Service
public class SignupService {

    private final UserRepository users;
    private final EmailVerificationStore store;
    private final PasswordEncoder passwordEncoder;
    private final MemberRegistration registration;

    public SignupService(UserRepository users, EmailVerificationStore store, PasswordEncoder passwordEncoder,
            MemberRegistration registration) {
        this.users = users;
        this.store = store;
        this.passwordEncoder = passwordEncoder;
        this.registration = registration;
    }

    public User signup(String rawNickname, String rawEmail, String password) {
        String email = User.normalizeEmail(rawEmail);
        String nickname = rawNickname.strip();

        // 인증됨 표시가 없으면 거절 (FR-002, FR-022). 시간이 지나 사라졌는지는 화면이 안다 (VERIFICATION_EXPIRED로 보여 줌)
        if (!store.isVerified(email)) {
            throw new ApiException(ErrorCode.EMAIL_NOT_VERIFIED);
        }
        // 인증 뒤 그사이에 누가 가입했는지 다시 확인 (FR-023)
        if (users.existsActiveByEmail(email)) {
            throw duplicate(ErrorCode.EMAIL_ALREADY_REGISTERED, "email");
        }
        if (users.existsActiveByNickname(nickname)) {
            throw duplicate(ErrorCode.NICKNAME_ALREADY_USED, "nickname");
        }

        User member;
        try {
            member = registration.register(email, passwordEncoder.encode(password), nickname);
        } catch (DataIntegrityViolationException e) {
            // 거의 동시에 온 두 요청 중 늦은 쪽: DB의 중복 불가가 막는다 (FR-012, SC-002)
            String detail = String.valueOf(e.getMostSpecificCause().getMessage());
            throw detail.contains("uq_users_nickname_active")
                    ? duplicate(ErrorCode.NICKNAME_ALREADY_USED, "nickname")
                    : duplicate(ErrorCode.EMAIL_ALREADY_REGISTERED, "email");
        }
        store.clearVerified(email);
        return member;
    }

    private static ApiException duplicate(ErrorCode code, String field) {
        return new ApiException(code, code.message(), List.of(new ErrorResponse.FieldErrorItem(field, code.name(), code.message())));
    }
}
