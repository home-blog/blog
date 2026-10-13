package com.myblog.user.repository;

import com.myblog.user.domain.SocialAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** 소셜 계정 연결 조회 (specs/010). 값은 파라미터로만 넘긴다. */
public interface SocialAccountRepository extends JpaRepository<SocialAccount, Long> {

    Optional<SocialAccount> findByProviderAndProviderUserId(String provider, String providerUserId);

    boolean existsByUserIdAndProvider(Long userId, String provider);

    Optional<SocialAccount> findByUserIdAndProvider(Long userId, String provider);

    List<SocialAccount> findByUserIdOrderByLinkedAtAsc(Long userId);

    long countByUserId(Long userId);

    /** 탈퇴: 이 회원의 연결을 모두 지운다 (US5). */
    @Modifying(flushAutomatically = true)
    @Query("delete from SocialAccount s where s.userId = :userId")
    int deleteAllByUserId(@Param("userId") Long userId);
}
