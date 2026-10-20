package com.myblog.user.repository;

import com.myblog.user.domain.User;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Pageable;
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

    /**
     * 로그인한 것으로 볼 회원 (specs/022 S2-3): 탈퇴하지 않았고 지금 정지 중이 아니다.
     * 정지 전에 받은 세션·열쇠로 와도 여기서 걸려 로그인하지 않은 것이 된다.
     */
    @Query("select u from User u where u.id = :id and u.deletedAt is null"
            + " and (u.suspendedUntil is null or u.suspendedUntil <= :now)")
    Optional<User> findLoggedInById(@Param("id") Long id, @Param("now") Instant now);

    /**
     * 편집실 회원 찾기 (specs/022 US2-1): 탈퇴하지 않은 회원 중 닉네임에 pattern이 들어 있는 회원, 최근 가입 순.
     * pattern은 부르는 쪽이 %·_·!를 !로 막은 뒤 앞뒤에 %를 붙인 값이다.
     */
    @Query("select u from User u where u.deletedAt is null and lower(u.nickname) like lower(:pattern) escape '!'"
            + " order by u.createdAt desc, u.id desc")
    List<User> searchActiveByNickname(@Param("pattern") String pattern, Pageable pageable);

    @Query("select count(u) from User u where u.deletedAt is null and lower(u.nickname) like lower(:pattern) escape '!'")
    long countActiveByNickname(@Param("pattern") String pattern);

    /** 지금 정지 중인 회원 수 (운영 현황). */
    @Query("select count(u) from User u where u.deletedAt is null and u.suspendedUntil > :now")
    long countSuspended(@Param("now") Instant now);

    /**
     * 회원 줄을 잠그고 읽는다. 같은 회원의 비밀번호 변경·탈퇴가 동시에 와도 하나씩 처리된다
     * (002 research R-3, PR #18 리뷰: 동시 비밀번호 변경).
     */
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
