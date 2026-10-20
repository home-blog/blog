package com.myblog.user.service;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.user.AccessKeyGrant;
import com.myblog.user.MemberWithdrawnEvent;
import com.myblog.user.config.AccessKeyProperties;
import com.myblog.user.domain.AccessKey;
import com.myblog.user.domain.User;
import com.myblog.user.repository.AccessKeyRepository;
import com.myblog.user.repository.UserRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 개인 열쇠 만들기·목록·끊기와, MCP 주소에서 열쇠 확인 (specs/020 US1, research D-2).
 * <ul>
 *   <li>열쇠는 {@code gk_} + 무작위 32바이트(base64url). 원문은 만들 때 한 번만 돌려주고 SHA-256 해시만 저장한다.</li>
 *   <li>만들 때 회원 줄을 FOR UPDATE로 잡아 개수 세기와 넣기가 한 줄로 서고, 탈퇴와 겹쳐도 열쇠가 남지 않는다.</li>
 *   <li>확인할 때 기한과 탈퇴 여부를 다시 본다. 회원이 탈퇴하면 열쇠를 모두 지운다.</li>
 * </ul>
 */
@Service
public class AccessKeyService {

    static final String PREFIX = "gk_";
    private static final int TOKEN_BYTES = 32;
    private static final int HINT_LENGTH = 8;
    private static final Set<String> SCOPES = Set.of(AccessKeyGrant.READ, AccessKeyGrant.DRAFT);

    private final AccessKeyRepository keys;
    private final UserRepository users;
    private final AccessKeyProperties properties;
    private final Clock clock;
    private final SecureRandom random = new SecureRandom();

    public AccessKeyService(AccessKeyRepository keys, UserRepository users, AccessKeyProperties properties, Clock clock) {
        this.keys = keys;
        this.users = users;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public KeyList list(Long memberId) {
        List<KeyView> items = keys.findByMemberIdOrderByCreatedAtDescIdDesc(memberId).stream().map(KeyView::of).toList();
        return new KeyList(items, properties.maxPerMember(), properties.allowedDays(), properties.nameMaxLength());
    }

    /** 새 열쇠. 원문(token)은 이 응답에만 실린다. */
    @Transactional
    public Issued create(Long memberId, String name, String scope, Integer days) {
        String trimmed = name == null ? "" : name.strip();
        int length = trimmed.codePointCount(0, trimmed.length());
        if (length < 1 || length > properties.nameMaxLength() || scope == null || !SCOPES.contains(scope) || days == null
                || !properties.allowedDays().contains(days)) {
            throw new ApiException(ErrorCode.ACCESS_KEY_INVALID,
                    ErrorCode.ACCESS_KEY_INVALID.message().formatted(properties.nameMaxLength()));
        }
        users.findActiveByIdForUpdate(memberId).orElseThrow(() -> new ApiException(ErrorCode.UNAUTHENTICATED));
        if (keys.countByMemberId(memberId) >= properties.maxPerMember()) {
            throw new ApiException(ErrorCode.ACCESS_KEY_LIMIT,
                    ErrorCode.ACCESS_KEY_LIMIT.message().formatted(properties.maxPerMember()));
        }
        byte[] bytes = new byte[TOKEN_BYTES];
        random.nextBytes(bytes);
        String token = PREFIX + Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = Instant.now(clock);
        AccessKey key = keys.saveAndFlush(AccessKey.issue(memberId, trimmed, sha256(token),
                token.substring(0, HINT_LENGTH), scope, now, now.plus(Duration.ofDays(days))));
        return new Issued(KeyView.of(key), token);
    }

    /** 열쇠 끊기(지우기). 남의 열쇠·없는 열쇠는 똑같이 404. */
    @Transactional
    public void revoke(Long memberId, Long keyId) {
        AccessKey key = keys.findByIdAndMemberId(keyId, memberId)
                .orElseThrow(() -> new ApiException(ErrorCode.ACCESS_KEY_NOT_FOUND));
        keys.delete(key);
    }

    /**
     * MCP 주소에서 열쇠 확인 (S1-2). 모양이 다르거나, 없거나, 기한이 지났거나, 회원이 탈퇴했으면 비어 있다.
     * 통과하면 마지막 사용 시각을 남긴다(touch-interval보다 자주 쓰지는 않는다).
     */
    @Transactional
    public Optional<Verified> verify(String token) {
        if (token == null || !token.startsWith(PREFIX) || token.length() > 200) {
            return Optional.empty();
        }
        Instant now = Instant.now(clock);
        return keys.findByTokenHash(sha256(token))
                .filter(key -> !key.isExpiredAt(now))
                .flatMap(key -> users.findActiveById(key.getMemberId()).map(user -> {
                    keys.touch(key.getId(), now, now.minus(properties.touchInterval()));
                    return new Verified(user, new AccessKeyGrant(key.getId(), key.getScope()));
                }));
    }

    /** 탈퇴하면 그 회원의 열쇠를 모두 지운다 (data-model). 탈퇴와 같은 트랜잭션에서 듣는다. */
    @EventListener
    public void on(MemberWithdrawnEvent event) {
        keys.deleteByMemberId(event.memberId());
    }

    /**
     * 그 회원의 열쇠를 모두 끊는다. 비밀번호를 바꾸면 부른다: 잠깐 세션을 얻은 사람이 만들어 둔 열쇠가
     * 비밀번호를 바꾼 뒤에도 남아 있으면 안 된다(다른 기기 로그아웃과 같은 뜻).
     */
    @Transactional
    public void revokeAll(Long memberId) {
        // 회원 줄을 잡고 지운다: 열쇠 만들기도 같은 줄을 잡으므로, 그 사이에 끼어 만들어진 열쇠도 여기서 함께 지워진다
        users.findActiveByIdForUpdate(memberId);
        keys.deleteByMemberId(memberId);
    }

    static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256을 쓸 수 없습니다", e);
        }
    }

    public record KeyList(List<KeyView> items, int maxCount, List<Integer> allowedDays, int nameMaxLength) {
    }

    /** 화면에 보이는 열쇠 한 줄. 원문·해시는 없다. */
    public record KeyView(Long keyId, String name, String hint, String scope, Instant createdAt, Instant expiresAt,
            Instant lastUsedAt) {

        static KeyView of(AccessKey key) {
            return new KeyView(key.getId(), key.getName(), key.getTokenHint(), key.getScope(), key.getCreatedAt(),
                    key.getExpiresAt(), key.getLastUsedAt());
        }
    }

    /** 만든 열쇠: 목록 한 줄과 원문(이때만). */
    public record Issued(KeyView key, String token) {
    }

    /** 확인된 열쇠의 회원과 권한. */
    public record Verified(User user, AccessKeyGrant grant) {
    }
}
