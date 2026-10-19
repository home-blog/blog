package com.myblog.subscription.service;

import com.myblog.blog.BlogSubscriptionSummary;
import com.myblog.post.SubscribedBlogs;
import com.myblog.subscription.domain.BlogSubscription;
import com.myblog.subscription.repository.BlogSubscriptionRepository;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** 아래 모듈의 틀을 채운다: 블로그 머리의 받아 보는 수(blog), 받아 보는 글의 블로그 번호(post) (specs/016 US1). */
@Component
@Transactional(readOnly = true)
public class SubscriptionAdapters implements BlogSubscriptionSummary, SubscribedBlogs {

    private final BlogSubscriptionRepository subscriptions;

    public SubscriptionAdapters(BlogSubscriptionRepository subscriptions) {
        this.subscriptions = subscriptions;
    }

    @Override
    public Summary summary(Long blogId, Long viewerId) {
        Optional<BlogSubscription> mine = viewerId == null ? Optional.empty()
                : subscriptions.findByMemberIdAndBlogId(viewerId, blogId);
        return new Summary(subscriptions.countByBlogId(blogId), mine.isPresent(),
                mine.map(BlogSubscription::isEmailEnabled).orElse(false));
    }

    @Override
    public List<Long> blogIdsOf(Long memberId) {
        return subscriptions.findBlogIdsByMemberId(memberId);
    }
}
