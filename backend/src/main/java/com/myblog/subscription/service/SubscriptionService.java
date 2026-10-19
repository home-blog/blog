package com.myblog.subscription.service;

import com.myblog.blog.BlogDirectory;
import com.myblog.blog.BlogDirectory.BlogInfo;
import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.subscription.config.SubscriptionProperties;
import com.myblog.subscription.domain.BlogSubscription;
import com.myblog.subscription.repository.BlogSubscriptionRepository;
import com.myblog.user.ActiveMemberLock;
import com.myblog.user.MemberNames;
import com.myblog.user.MemberNames.MemberName;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 원고 받아 보기 (specs/016 US1, S1-1 ~ S1-3). 받기·그만 받기 모두 같은 요청을 몇 번 보내도 결과가 같다.
 * 검사 순서: 있는 블로그인가(아니면 BLOG_NOT_FOUND) → 내 블로그면 SELF_SUBSCRIBE_NOT_ALLOWED → 회원 줄을 잠그고
 * (탈퇴·한도와 겹치지 않게) → 새로 받는 것이면 한도(SUBSCRIPTION_LIMIT_EXCEEDED) → 넣거나 메일 받기만 바꾼다.
 */
@Service
@Transactional
public class SubscriptionService {

    private final BlogSubscriptionRepository subscriptions;
    private final BlogDirectory blogDirectory;
    private final MemberNames memberNames;
    private final ActiveMemberLock memberLock;
    private final SubscriptionProperties properties;
    private final Clock clock;

    public SubscriptionService(BlogSubscriptionRepository subscriptions, BlogDirectory blogDirectory,
            MemberNames memberNames, ActiveMemberLock memberLock, SubscriptionProperties properties, Clock clock) {
        this.subscriptions = subscriptions;
        this.blogDirectory = blogDirectory;
        this.memberNames = memberNames;
        this.memberLock = memberLock;
        this.properties = properties;
        this.clock = clock;
    }

    public record State(boolean subscribed, boolean emailEnabled, long subscriberCount) {
    }

    public record MySubscription(Long blogId, String blogName, String nickname, boolean emailEnabled,
            Instant createdAt) {
    }

    public State subscribe(Long memberId, Long blogId, boolean emailEnabled) {
        BlogInfo blog = blogDirectory.blog(blogId).orElseThrow(() -> new ApiException(ErrorCode.BLOG_NOT_FOUND));
        if (memberId.equals(blog.ownerId())) {
            throw new ApiException(ErrorCode.SELF_SUBSCRIBE_NOT_ALLOWED);
        }
        if (!memberLock.lockIfActive(memberId)) {
            // 이 요청이 기다리는 사이 탈퇴가 끝났다
            throw new ApiException(ErrorCode.UNAUTHENTICATED);
        }
        if (!subscriptions.existsByMemberIdAndBlogId(memberId, blogId)
                && subscriptions.countByMemberId(memberId) >= properties.maxPerMember()) {
            throw new ApiException(ErrorCode.SUBSCRIPTION_LIMIT_EXCEEDED,
                    ErrorCode.SUBSCRIPTION_LIMIT_EXCEEDED.message().formatted(properties.maxPerMember()));
        }
        subscriptions.upsert(memberId, blogId, emailEnabled, Instant.now(clock));
        return new State(true, emailEnabled, subscriptions.countByBlogId(blogId));
    }

    public State unsubscribe(Long memberId, Long blogId) {
        blogDirectory.blog(blogId).orElseThrow(() -> new ApiException(ErrorCode.BLOG_NOT_FOUND));
        subscriptions.deleteByMemberIdAndBlogId(memberId, blogId);
        return new State(false, false, subscriptions.countByBlogId(blogId));
    }

    /** 내가 받아 보는 블로그, 최근에 받기 시작한 순. 그사이 닫힌 블로그는 뺀다. */
    @Transactional(readOnly = true)
    public List<MySubscription> mine(Long memberId) {
        List<BlogSubscription> list = subscriptions.findByMemberIdOrderByCreatedAtDescIdDesc(memberId);
        List<Optional<BlogInfo>> blogs = list.stream().map(s -> blogDirectory.blog(s.getBlogId())).toList();
        Map<Long, MemberName> names = memberNames.namesOf(
                blogs.stream().flatMap(Optional::stream).map(BlogInfo::ownerId).toList());
        return java.util.stream.IntStream.range(0, list.size())
                .filter(i -> blogs.get(i).isPresent())
                .mapToObj(i -> {
                    BlogInfo blog = blogs.get(i).get();
                    MemberName owner = names.get(blog.ownerId());
                    return new MySubscription(blog.blogId(), blog.name(), owner == null ? "" : owner.nickname(),
                            list.get(i).isEmailEnabled(), list.get(i).getCreatedAt());
                })
                .toList();
    }
}
