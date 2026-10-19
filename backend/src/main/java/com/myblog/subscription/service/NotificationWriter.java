package com.myblog.subscription.service;

import com.myblog.comment.CommentWrittenEvent;
import com.myblog.post.PostLookup;
import com.myblog.post.PostPublishedEvent;
import com.myblog.subscription.repository.NotificationRepository;
import com.myblog.user.MemberNames;
import com.myblog.user.MemberNames.MemberName;
import jakarta.annotation.PreDestroy;
import java.time.Clock;
import java.time.Duration;
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
 * 우편함 소식과 원고 편지를 만든다 (specs/016 D-3, S2-2, S2-3). 글·댓글을 저장한 트랜잭션이 끝난 뒤(AFTER_COMMIT) 따로 한다:
 * 글 저장·댓글 쓰기는 소식 때문에 느려지거나 실패하지 않는다. 소식 일꾼과 메일 일꾼을 나눠, 메일을 여러 통 보내는 동안에도
 * 댓글 소식이 밀리지 않게 한다. 일이 밀려 줄이 차면 넘치는 것은 버리고 기록한다.
 * 만들 때 그 글을 아직 누구나 볼 수 있는지 다시 보고, 그사이 비공개로 돌리거나 지운 글은 알리지 않는다.
 * 서버를 끌 때(배포) 줄에 남은 일을 잠깐 마치고 끈다.
 */
@Component
public class NotificationWriter {

    private static final Logger log = LoggerFactory.getLogger(NotificationWriter.class);
    private static final int QUEUE_SIZE = 1000;
    private static final Duration SHUTDOWN_WAIT = Duration.ofSeconds(10);

    private final NotificationRepository notifications;
    private final PostLookup postLookup;
    private final MemberNames memberNames;
    private final Newsletter newsletter;
    private final TransactionTemplate transaction;
    private final Clock clock;
    private final ExecutorService worker;
    private final ExecutorService mailWorker;

    public NotificationWriter(NotificationRepository notifications, PostLookup postLookup, MemberNames memberNames,
            Newsletter newsletter, PlatformTransactionManager transactionManager, Clock clock) {
        this.notifications = notifications;
        this.postLookup = postLookup;
        this.memberNames = memberNames;
        this.newsletter = newsletter;
        this.transaction = new TransactionTemplate(transactionManager);
        this.clock = clock;
        this.worker = singleWorker("notification-writer", "우편함: 일이 밀려 소식 하나를 만들지 못했습니다");
        this.mailWorker = singleWorker("newsletter-sender", "원고 편지: 일이 밀려 글 하나의 메일을 보내지 못했습니다");
    }

    private static ExecutorService singleWorker(String name, String dropped) {
        return new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(QUEUE_SIZE), runnable -> {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            return thread;
        }, (task, pool) -> log.warn(dropped));
    }

    /** 끌 때 줄에 남은 일을 잠깐(SHUTDOWN_WAIT) 마친다. 그 뒤에 남은 것은 버린다(잃어도 되는 알림). */
    @PreDestroy
    void shutdown() throws InterruptedException {
        worker.shutdown();
        mailWorker.shutdown();
        if (!worker.awaitTermination(SHUTDOWN_WAIT.toSeconds(), TimeUnit.SECONDS)
                || !mailWorker.awaitTermination(SHUTDOWN_WAIT.toSeconds(), TimeUnit.SECONDS)) {
            log.warn("우편함: 끄는 동안 마치지 못한 소식·메일이 있습니다");
        }
    }

    /** 지금도 누구나 볼 수 있는 글인가 (그사이 비공개로 돌리거나 지웠으면 알리지 않는다). */
    private boolean stillOpen(Long postId) {
        return postLookup.findVisible(postId, null).isPresent();
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(PostPublishedEvent event) {
        worker.execute(() -> {
            try {
                transaction.executeWithoutResult(status -> {
                    if (stillOpen(event.postId())) {
                        notifications.insertPostNotices(event.blogId(), event.ownerId(), event.postId(),
                                clip(event.title(), 100), clip(event.blogName(), 30), Instant.now(clock));
                    }
                });
            } catch (RuntimeException e) {
                log.warn("우편함: 글 {}의 새 글 소식을 만들지 못했습니다: {}", event.postId(), e.toString());
            }
        });
        mailWorker.execute(() -> {
            try {
                if (Boolean.TRUE.equals(transaction.execute(status -> stillOpen(event.postId())))) {
                    newsletter.send(event);
                }
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
