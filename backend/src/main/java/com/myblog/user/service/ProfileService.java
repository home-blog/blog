package com.myblog.user.service;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.common.error.ErrorResponse;
import com.myblog.user.MemberBlogLookup;
import com.myblog.user.domain.User;
import com.myblog.user.repository.UserRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 마이페이지의 내 정보 보기·고치기 (specs/002 US1, contracts 1·2).
 * 칸 형식은 요청을 받을 때(@Valid) 이미 검사했다. 여기서는 중복 확인 → 저장.
 */
@Service
public class ProfileService {

    private final UserRepository users;
    private final MemberBlogLookup blogLookup;

    public ProfileService(UserRepository users, MemberBlogLookup blogLookup) {
        this.users = users;
        this.blogLookup = blogLookup;
    }

    /** 내 정보. 소개가 없으면 빈 문자열, 블로그가 없으면 blogId는 null (FR-001 ~ FR-004). */
    @Transactional(readOnly = true)
    public Profile view(User member) {
        Long blogId = blogLookup.blogIdOf(member.getId()).orElse(null);
        return new Profile(member.getEmail(), member.getNickname(), introOf(member), member.getCreatedAt(), blogId);
    }

    /**
     * 닉네임과 소개를 저장한다. 값이 지금과 같아도 오류 없이 저장한다 (FR-009).
     * 중복 닉네임은 미리 확인하고, 거의 동시에 온 요청은 DB의 중복 불가(uq_users_nickname_active)가 막는다 (FR-007, SC-002).
     */
    @Transactional
    public User update(Long memberId, String nickname, String intro) {
        User member = users.findActiveById(memberId).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
        if (users.existsActiveByNicknameExcept(nickname, memberId)) {
            throw nicknameTaken();
        }
        member.changeProfile(nickname, intro);
        try {
            users.flush();
        } catch (DataIntegrityViolationException e) {
            throw nicknameTaken();
        }
        return member;
    }

    public static String introOf(User member) {
        return member.getIntro() == null ? "" : member.getIntro();
    }

    /** 칸 아래에 보이도록 fieldErrors의 nickname 칸에도 넣는다 (FR-010). */
    private static ApiException nicknameTaken() {
        ErrorCode code = ErrorCode.NICKNAME_ALREADY_USED;
        return new ApiException(code, code.message(), List.of(new ErrorResponse.FieldErrorItem("nickname", code.name(), code.message())));
    }

    public record Profile(String email, String nickname, String intro, Instant joinedAt, Long blogId) {
    }
}
