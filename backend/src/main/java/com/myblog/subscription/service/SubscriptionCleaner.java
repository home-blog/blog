package com.myblog.subscription.service;

import com.myblog.blog.BlogClosingEvent;
import com.myblog.subscription.repository.BlogSubscriptionRepository;
import com.myblog.subscription.repository.NotificationRepository;
import com.myblog.user.MemberWithdrawnEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 받아 보기·우편함 정리 (specs/016 S1-5). LikeCleaner처럼 <b>같은 트랜잭션 안에서</b> 듣는다.
 * <ul>
 *   <li>회원이 탈퇴하면 그 사람이 받아 보던 것과 우편함을 지운다. 그 사람의 블로그를 받아 보던 줄은 아래 블로그 닫기에서 지운다.</li>
 *   <li>블로그가 닫히면 그 블로그를 받아 보던 줄을 지운다(블로그 줄보다 먼저).</li>
 * </ul>
 */
@Component
public class SubscriptionCleaner {

    private final BlogSubscriptionRepository subscriptions;
    private final NotificationRepository notifications;

    public SubscriptionCleaner(BlogSubscriptionRepository subscriptions, NotificationRepository notifications) {
        this.subscriptions = subscriptions;
        this.notifications = notifications;
    }

    @EventListener
    public void on(MemberWithdrawnEvent event) {
        subscriptions.deleteByMemberId(event.memberId());
        notifications.deleteByMemberId(event.memberId());
    }

    @EventListener
    public void on(BlogClosingEvent event) {
        subscriptions.deleteByBlogId(event.blogId());
    }
}
