package com.myblog.user.repository;

import com.myblog.user.domain.User;
import java.time.Instant;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * 회원 조회. 모든 조회는 탈퇴하지 않은 회원(deleted_at IS NULL)만 대상으로 하고,
 * 이메일·닉네임은 저장할 때처럼 앞뒤 공백을 지우고 소문자로 맞춰 비교한다 (DB의 부분 인덱스 uq_users_*_active와 같은 기준, E-2).
 * 값은 파라미터로만 넘긴다 (쿼리 문자열을 이어 붙이지 않음).
 */
public interface UserRepository extends JpaRepository<User, Long> {

    @Query("select u from User u where lower(u.email) = lower(trim(:email)) and u.deletedAt is null")
    Optional<User> findActiveByEmail(@Param("email") String email);

    @Query("select count(u) > 0 from User u where lower(u.email) = lower(trim(:email)) and u.deletedAt is null")
    boolean existsActiveByEmail(@Param("email") String email);

    @Query("select count(u) > 0 from User u where lower(u.nickname) = lower(trim(:nickname)) and u.deletedAt is null")
    boolean existsActiveByNickname(@Param("nickname") String nickname);

    // 계정 관리 (specs/002 T004)

    @Query("select u from User u where u.id = :id and u.deletedAt is null")
    Optional<User> findActiveById(@Param("id") Long id);

    /** 탈퇴하는 동안 같은 회원 줄을 잠근다 (002 research R-3). 두 번 눌러도 한 번만 처리된다. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.id = :id and u.deletedAt is null")
    Optional<User> findActiveByIdForUpdate(@Param("id") Long id);

    /** 나(id)를 빼고 같은 닉네임이 있는지. 내 닉네임 그대로나 대소문자만 바꾸는 것은 중복이 아니다 (002 FR-007). */
    @Query("select count(u) > 0 from User u where lower(u.nickname) = lower(trim(:nickname)) and u.id <> :id"
            + " and u.deletedAt is null")
    boolean existsActiveByNicknameExcept(@Param("nickname") String nickname, @Param("id") Long id);

    // 로그인 연속 실패 (specs/001 T031, data-model 1 `로그인 잠금의 상태 변화`)
    // 동시에 여러 번 틀려도 정확히 하나씩 오르도록, 읽고 쓰지 않고 DB에서 바로 +1 한다.

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update User u set u.failedLoginCount = u.failedLoginCount + 1 where u.id = :id")
    int incrementFailedLoginCount(@Param("id") Long id);

    @Query("select u.failedLoginCount from User u where u.id = :id")
    int findFailedLoginCount(@Param("id") Long id);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update User u set u.lockedUntil = :until where u.id = :id")
    int lockUntil(@Param("id") Long id, @Param("until") Instant until);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update User u set u.failedLoginCount = 0, u.lockedUntil = null where u.id = :id")
    int resetLoginFailures(@Param("id") Long id);
}
