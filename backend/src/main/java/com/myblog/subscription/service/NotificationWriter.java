package com.myblog.subscription.service;

import com.myblog.board.BoardAnswerAcceptedEvent;
import com.myblog.board.BoardCommentWrittenEvent;
import com.myblog.comment.CommentWrittenEvent;
import com.myblog.editorial.EditorialNoticeEvent;
import com.myblog.guestbook.GuestbookRepliedEvent;
import com.myblog.guestbook.GuestbookWrittenEvent;
import com.myblog.post.PostLookup;
import com.myblog.post.PostPublishedEvent;
import com.myblog.subscription.config.SubscriptionProperties;
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

    private final NotificationRepository notifications;
    private final PostLookup postLookup;
    private final MemberNames memberNames;
    private final Newsletter newsletter;
    private final TransactionTemplate transaction;
    private final Clock clock;
    private final ExecutorService worker;
    private final ExecutorService mailWorker;
    private final Duration shutdownWait;

    public NotificationWriter(NotificationRepository notifications, PostLookup postLookup, MemberNames memberNames,
            Newsletter newsletter, PlatformTransactionManager transactionManager, Clock clock,
            SubscriptionProperties properties) {
        this.notifications = notifications;
        this.postLookup = postLookup;
        this.memberNames = memberNames;
        this.newsletter = newsletter;
        this.transaction = new TransactionTemplate(transactionManager);
        this.clock = clock;
        int queueSize = properties.worker().queueSize();
        this.shutdownWait = properties.worker().shutdownWait();
        this.worker = singleWorker("notification-writer", queueSize, "우편함: 일이 밀려 소식 하나를 만들지 못했습니다");
        this.mailWorker = singleWorker("newsletter-sender", queueSize, "원고 편지: 일이 밀려 글 하나의 메일을 보내지 못했습니다");
    }

    private static ExecutorService singleWorker(String name, int queueSize, String dropped) {
        return new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS, new ArrayBlockingQueue<>(queueSize), runnable -> {
            Thread thread = new Thread(runnable, name);
            thread.setDaemon(true);
            return thread;
        }, (task, pool) -> log.warn(dropped));
    }

    /** 끌 때 줄에 남은 일을 일꾼마다 잠깐(subscription.worker.shutdown-wait) 마친다. 그 뒤에 남은 것은 버린다(잃어도 되는 알림). */
    @PreDestroy
    void shutdown() throws InterruptedException {
        worker.shutdown();
        mailWorker.shutdown();
        // 두 일꾼을 따로 기다린다: 앞 일꾼이 늦어도 메일 일꾼을 기다리지 않고 넘어가지 않게
        boolean workerDone = worker.awaitTermination(shutdownWait.toMillis(), TimeUnit.MILLISECONDS);
        boolean mailDone = mailWorker.awaitTermination(shutdownWait.toMillis(), TimeUnit.MILLISECONDS);
        if (!workerDone || !mailDone) {
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

    /**
     * 댓글 소식은 글 주인에게, 답글 소식은 원댓글 쓴 사람에게 (specs/016 US2, specs/019 D-4). 자기에게는 보내지 않는다.
     * 글 주인의 댓글에 단 답글이면 글 주인은 댓글 소식 대신 답글 소식 하나만 받는다.
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(CommentWrittenEvent event) {
        worker.execute(() -> {
            try {
                transaction.executeWithoutResult(status -> postLookup.findVisibleText(event.postId(), event.writerId())
                        .ifPresent(post -> {
                            Long parentWriterId = event.parentWriterId();
                            String name = writerName(event.writerId());
                            String title = clip(post.title(), 100);
                            Instant now = Instant.now(clock);
                            if (!post.ownerId().equals(event.writerId())) {
                                if (post.ownerId().equals(parentWriterId)) {
                                    notifications.insertReplyNotice(post.ownerId(), post.postId(), title, name, now);
                                } else {
                                    notifications.insertCommentNotice(post.ownerId(), post.postId(), title, name, now);
                                }
                            }
                            if (parentWriterId != null && !parentWriterId.equals(event.writerId())
                                    && !parentWriterId.equals(post.ownerId())
                                    && postLookup.findVisibleText(post.postId(), parentWriterId).isPresent()) {
                                notifications.insertReplyNotice(parentWriterId, post.postId(), title, name, now);
                            }
                        }));
            } catch (RuntimeException e) {
                log.warn("우편함: 댓글 {}의 소식을 만들지 못했습니다: {}", event.commentId(), e.toString());
            }
        });
    }

    private String writerName(Long writerId) {
        MemberName writer = memberNames.namesOf(List.of(writerId)).get(writerId);
        return clip(writer == null || writer.withdrawn() ? "탈퇴한 회원" : writer.nickname(), 30);
    }

    /** 글마당 글에 댓글 (specs/017 US3). 글 쓴 사람이 단 댓글은 글마당 모듈이 이벤트를 내지 않는다. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(BoardCommentWrittenEvent event) {
        boardNotice(event.postWriterId(), "board", event.boardPostId(), event.postTitle(), event.commenterName());
    }

    /** 내 글마당 댓글이 채택됨 (specs/017 US4). */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(BoardAnswerAcceptedEvent event) {
        boardNotice(event.commentWriterId(), "accepted", event.boardPostId(), event.postTitle(), event.accepterName());
    }

    /** 편집실이 내 글을 내리거나 지웠다 (specs/018). 그사이 블로그 글이 지워졌으면 외래 키가 맞지 않아 기록만 남는다. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(EditorialNoticeEvent event) {
        worker.execute(() -> {
            try {
                transaction.executeWithoutResult(status -> notifications.insertEditorialNotice(event.memberId(),
                        event.postId(), clip(event.title(), 100), clip(event.reason(), 200), Instant.now(clock)));
            } catch (RuntimeException e) {
                log.warn("우편함: 편집실 소식을 만들지 못했습니다: {}", e.toString());
            }
        });
    }

    /** 내 방명록에 새 엽서 (specs/021 US1-5). 비밀 엽서여도 내용은 싣지 않으니 그대로 알린다. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(GuestbookWrittenEvent event) {
        guestbookNotice(event.ownerId(), "guestbook", event.entryId(), event.blogName(), event.writerId());
    }

    /** 내 엽서에 주인이 처음 답장함 (specs/021 US2-2). 보낸 이름은 블로그 주인의 닉네임. */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void on(GuestbookRepliedEvent event) {
        guestbookNotice(event.writerId(), "gb_reply", event.entryId(), event.blogName(), event.ownerId());
    }

    private void guestbookNotice(Long memberId, String kind, Long entryId, String blogName, Long actorId) {
        worker.execute(() -> {
            try {
                transaction.executeWithoutResult(status -> notifications.insertGuestbookNotice(memberId, kind, entryId,
                        clip(blogName, 100), writerName(actorId), Instant.now(clock)));
            } catch (RuntimeException e) {
                log.warn("우편함: 엽서 {}의 소식({})을 만들지 못했습니다: {}", entryId, kind, e.toString());
            }
        });
    }

    /** 그사이 글이 지워졌으면 글 번호 FK가 맞지 않아 넣지 못하고 기록만 남는다. */
    private void boardNotice(Long memberId, String kind, Long boardPostId, String title, String actorName) {
        worker.execute(() -> {
            try {
                transaction.executeWithoutResult(status -> notifications.insertBoardNotice(memberId, kind, boardPostId,
                        clip(title, 100), clip(actorName, 30), Instant.now(clock)));
            } catch (RuntimeException e) {
                log.warn("우편함: 글마당 글 {}의 소식({})을 만들지 못했습니다: {}", boardPostId, kind, e.toString());
            }
        });
    }

    private static String clip(String text, int max) {
        return text.codePointCount(0, text.length()) <= max ? text : text.substring(0, text.offsetByCodePoints(0, max));
    }
}
