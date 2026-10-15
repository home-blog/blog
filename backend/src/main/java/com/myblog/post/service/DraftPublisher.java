package com.myblog.post.service;

import com.myblog.post.config.WritingProperties;
import com.myblog.post.repository.PostDraftRepository;
import com.myblog.post.service.DraftService.ScheduledOutcome;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 예약 발행 작업 (specs/013 US4, D-2 ~ D-4). writing.schedule.check-interval(1분)마다 시각이 지난 예약을 올린다.
 * <ul>
 *   <li>예약 하나마다 트랜잭션 하나: 줄을 FOR UPDATE SKIP LOCKED로 잠그고 올린 뒤 커밋한다. 앱 두 벌(a·b)이 같이 돌아도
 *       한 벌만 그 줄을 잡는다. 그래도 겹치면 글의 request_key(임시 저장 글의 요청 번호)가 둘째 글을 막는다 (S4-4).</li>
 *   <li>서버가 꺼져 있던 동안 밀린 예약도 다음 차례에 올린다. 한 번에 batch-size까지.</li>
 *   <li>글 만들기에서 예외가 나면 그 트랜잭션은 되돌리고, 새 트랜잭션에서 예약을 풀어 같은 실패를 되풀이하지 않는다 (S4-5).</li>
 * </ul>
 */
@Component
public class DraftPublisher {

    private static final Logger log = LoggerFactory.getLogger(DraftPublisher.class);

    private final PostDraftRepository drafts;
    private final DraftService draftService;
    private final TransactionTemplate transaction;
    private final int batchSize;
    private final Clock clock;

    public DraftPublisher(PostDraftRepository drafts, DraftService draftService, PlatformTransactionManager transactionManager,
            WritingProperties writing, Clock clock) {
        this.drafts = drafts;
        this.draftService = draftService;
        this.transaction = new TransactionTemplate(transactionManager);
        this.batchSize = writing.schedule().batchSize();
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${writing.schedule.check-interval}", initialDelayString = "${writing.schedule.check-interval}")
    public void run() {
        int published = publishDue();
        if (published > 0) {
            log.info("예약 글 {}개를 올렸습니다", published);
        }
    }

    /** 시각이 지난 예약을 batch-size까지 올리고, 올린 수를 돌려준다. */
    public int publishDue() {
        int published = 0;
        for (int i = 0; i < batchSize; i++) {
            Long[] picked = {null};
            ScheduledOutcome outcome;
            try {
                outcome = transaction.execute(status -> {
                    Optional<Long> due = drafts.lockNextDue(Instant.now(clock));
                    if (due.isEmpty()) {
                        return null;
                    }
                    picked[0] = due.get();
                    return draftService.publishScheduled(due.get());
                });
            } catch (RuntimeException e) {
                log.warn("예약 글 {}을 올리다 실패해 예약을 풉니다", picked[0], e);
                unschedule(picked[0]);
                continue;
            }
            if (outcome == null) {
                break;
            }
            if (outcome == ScheduledOutcome.PUBLISHED) {
                published++;
            }
        }
        return published;
    }

    private void unschedule(Long draftId) {
        if (draftId == null) {
            return;
        }
        try {
            transaction.executeWithoutResult(status -> drafts.unschedule(draftId));
        } catch (RuntimeException e) {
            log.warn("예약 글 {}의 예약을 풀지 못했습니다", draftId, e);
        }
    }
}
