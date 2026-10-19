package com.myblog.subscription.service;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.common.paging.PageNumbers;
import com.myblog.common.paging.PageNumbers.PageSlice;
import com.myblog.post.PostLookup;
import com.myblog.subscription.config.SubscriptionProperties;
import com.myblog.subscription.domain.Notification;
import com.myblog.subscription.repository.NotificationRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 우편함 (specs/016 US2, S2-1, S2-4): 나만 보는 소식 목록, 읽음, 모두 읽음, 안 읽은 수. */
@Service
@Transactional
public class NotificationService {

    private final NotificationRepository notifications;
    private final PostLookup postLookup;
    private final SubscriptionProperties properties;
    private final Clock clock;

    public NotificationService(NotificationRepository notifications, PostLookup postLookup,
            SubscriptionProperties properties, Clock clock) {
        this.notifications = notifications;
        this.postLookup = postLookup;
        this.properties = properties;
        this.clock = clock;
    }

    /** available: 지금 그 글을 볼 수 있는가 (지워졌거나 비공개가 되면 false, 그때 postId·postTitle은 null). */
    public record NotificationView(Long notificationId, String kind, Long postId, String postTitle, String actorName,
            boolean available, boolean read, Instant createdAt) {
    }

    public record NotificationPage(List<NotificationView> items, int page, int totalPages, long unreadCount) {
    }

    @Transactional(readOnly = true)
    public NotificationPage list(Long memberId, String page) {
        long total = notifications.countByMemberId(memberId);
        PageSlice slice = PageNumbers.of(page, total, properties.notificationPageSize());
        List<NotificationView> items = notifications.findByMemberIdOrderByCreatedAtDescIdDesc(memberId,
                        PageRequest.of(slice.page() - 1, slice.pageSize()))
                .stream()
                .map(n -> view(n, n.getPostId() != null && postLookup.findVisible(n.getPostId(), memberId).isPresent()))
                .toList();
        return new NotificationPage(items, slice.page(), slice.totalPages(), unreadCount(memberId));
    }

    @Transactional(readOnly = true)
    public long unreadCount(Long memberId) {
        return notifications.countByMemberIdAndReadAtIsNull(memberId);
    }

    /** 이미 읽었으면 그대로. 남의 것·없는 것은 404. */
    public void read(Long memberId, Long notificationId) {
        if (!notifications.existsByIdAndMemberId(notificationId, memberId)) {
            throw new ApiException(ErrorCode.NOTIFICATION_NOT_FOUND);
        }
        notifications.markRead(notificationId, memberId, Instant.now(clock));
    }

    public void readAll(Long memberId) {
        notifications.markAllRead(memberId, Instant.now(clock));
    }

    private static NotificationView view(Notification n, boolean available) {
        // 볼 수 없게 된 글은 번호·제목도 주지 않는다 (비공개로 돌린 글의 제목이 우편함에 남지 않게)
        return new NotificationView(n.getId(), n.getKind(), available ? n.getPostId() : null,
                available ? n.getPostTitle() : null, n.getActorName(), available, n.getReadAt() != null, n.getCreatedAt());
    }
}
