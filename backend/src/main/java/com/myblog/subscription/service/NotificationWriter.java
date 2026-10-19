package com.myblog.subscription.service;

import com.myblog.comment.CommentWrittenEvent;
import com.myblog.post.PostLookup;
import com.myblog.post.PostPublishedEvent;
import com.myblog.subscription.repository.NotificationRepository;
import com.myblog.user.MemberNames;
import com.myblog.user.MemberNames.MemberName;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 우편함 소식과 원고 편지를 만든다 (specs/016 D-3, S2-2, S2-3). 글·댓글을 저장한 트랜잭션이 끝난 뒤(AFTER_COMMIT) 일꾼 하나가 따로 한다:
 * 글 저장·댓글 쓰기는 소식 때문에 느려지거나 실패하지 않는다. 일이 밀려 줄이 차면 넘치는 것은 버리고 기록한다.
 */
@Component
public class NotificationWriter {

    private static final Logger log = LoggerFactory.getLogger(NotificationWriter.class);
    private static final int QUEUE_SIZE = 1000;

    private final NotificationRepository notifications;
    private final PostLookup postLookup;
    private final MemberNames memberNames;
    private final Newsletter newsletter;
    private final TransactionTemplate transaction;
    private final Clock clock;
    private final ExecutorService worker;

    public NotificationWriter(NotificationRepository notifications, PostLookup postLookup, MemberNames memberNames,
            Newsletter newsletter, PlatformTransactionManager transactionManager, Clock clock) {
        this.notifications = notifications;
        this.postLookup = postLookup;
        this.memberNames = memberNames;
        this.newsletter = newsletter;
        this.transaction = new TransactionTemplate(transactionManager);
        this.clock = clock;
        this.worker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(QUEUE_SIZE),
                runnable -> {
                    Thread thread = new Thread(runnable, "notification-writer");
                    thread.setDaemon(true);
                    return thread;
                }, (task, pool) -> log.warn("우편함: 일이 밀려 소식 하나를 만들지 못했습니다"));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(PostPublishedEvent event) {
        worker.execute(() -> {
            try {
                transaction.executeWithoutResult(status -> notifications.insertPostNotices(event.blogId(),
                        event.ownerId(), event.postId(), clip(event.title(), 100), clip(event.blogName(), 30),
                        Instant.now(clock)));
            } catch (RuntimeException e) {
                log.warn("우편함: 글 {}의 새 글 소식을 만들지 못했습니다: {}", event.postId(), e.toString());
            }
            try {
                newsletter.send(event);
            } catch (RuntimeException e) {
                log.warn("원고 편지: 글 {}의 메일을 보내지 못했습니다: {}", event.postId(), e.toString());
            }
        });
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(CommentWrittenEvent event) {
        worker.execute(() -> {
            try {
                transaction.executeWithoutResult(status -> postLookup.findVisibleText(event.postId(), event.writerId())
                        .filter(post -> !post.ownerId().equals(event.writerId()))
                        .ifPresent(post -> {
                            MemberName writer = memberNames.namesOf(List.of(event.writerId())).get(event.writerId());
                            String name = writer == null || writer.withdrawn() ? "탈퇴한 회원" : writer.nickname();
                            notifications.insertCommentNotice(post.ownerId(), post.postId(), clip(post.title(), 100),
                                    clip(name, 30), Instant.now(clock));
                        }));
            } catch (RuntimeException e) {
                log.warn("우편함: 댓글 {}의 소식을 만들지 못했습니다: {}", event.commentId(), e.toString());
            }
        });
    }

    private static String clip(String text, int max) {
        return text.codePointCount(0, text.length()) <= max ? text : text.substring(0, text.offsetByCodePoints(0, max));
    }
}
