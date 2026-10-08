package com.myblog.user.security;

import com.myblog.user.domain.User;
import java.time.Instant;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Spring Security가 아는 "로그인한 회원". 세션에 저장된다.
 * - 이름(getUsername)은 회원 식별값(users_id)이다. Spring Session이 이 이름으로 회원의 세션을 찾는다 (data-model 4,
 *   002의 "비밀번호를 바꾸면 다른 기기 로그아웃"의 바탕).
 * - 비밀번호 해시는 로그인을 마치면 지워져(eraseCredentials) 세션에 남지 않는다 (FR-010).
 */
public class MemberPrincipal implements UserDetails, CredentialsContainer {

    /** 세션에 직렬화되어 저장된다 (UserDetails가 Serializable). 칸을 바꾸면 올린다 */
    private static final long serialVersionUID = 1L;

    private static final List<GrantedAuthority> MEMBER = List.of(new SimpleGrantedAuthority("ROLE_MEMBER"));

    private final Long id;
    private final String email;
    private final String nickname;
    private String passwordHash;
    private final boolean locked;

    private MemberPrincipal(Long id, String email, String nickname, String passwordHash, boolean locked) {
        this.id = id;
        this.email = email;
        this.nickname = nickname;
        this.passwordHash = passwordHash;
        this.locked = locked;
    }

    /** 로그인을 시도하는 순간의 회원 상태로 만든다. 잠금 시각이 지났으면 잠기지 않은 것으로 본다 (FR-028). */
    public static MemberPrincipal of(User user, Instant now) {
        return new MemberPrincipal(user.getId(), user.getEmail(), user.getNickname(), user.getPasswordHash(), user.isLocked(now));
    }

    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getNickname() {
        return nickname;
    }

    @Override
    public String getUsername() {
        return String.valueOf(id);
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return MEMBER;
    }

    /** 잠겨 있으면 Spring Security가 비밀번호를 비교하기 전에 LockedException으로 거절한다 (FR-027). */
    @Override
    public boolean isAccountNonLocked() {
        return !locked;
    }

    @Override
    public void eraseCredentials() {
        this.passwordHash = null;
    }
}
