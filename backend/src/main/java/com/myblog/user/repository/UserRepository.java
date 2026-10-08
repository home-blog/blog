package com.myblog.user.repository;

import com.myblog.user.domain.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 회원 조회. 모든 조회는 탈퇴하지 않은 회원(deleted_at IS NULL)만 대상으로 하고,
 * 이메일·닉네임은 소문자로 맞춰 비교한다 (DB의 부분 인덱스 uq_users_*_active와 같은 기준, E-2).
 * 값은 파라미터로만 넘긴다 (쿼리 문자열을 이어 붙이지 않음).
 */
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("select u from User u where lower(u.email) = lower(:email) and u.deletedAt is null")
    Optional<User> findActiveByEmail(@Param("email") String email);

    @Query("select count(u) > 0 from User u where lower(u.email) = lower(:email) and u.deletedAt is null")
    boolean existsActiveByEmail(@Param("email") String email);

    @Query("select count(u) > 0 from User u where lower(u.nickname) = lower(:nickname) and u.deletedAt is null")
    boolean existsActiveByNickname(@Param("nickname") String nickname);
}
