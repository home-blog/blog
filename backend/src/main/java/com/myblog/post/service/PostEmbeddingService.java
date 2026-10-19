package com.myblog.post.service;

import com.myblog.post.PostContentSavedEvent;
import com.myblog.post.config.RelatedProperties;
import com.myblog.post.related.Embedder;
import com.myblog.post.repository.PostEmbeddingRepository;
import com.myblog.post.repository.PostEmbeddingRepository.Source;
import com.myblog.post.repository.PostEmbeddingRepository.Stored;
import jakarta.annotation.PreDestroy;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 글의 뜻 정보(임베딩)를 만든다 (specs/014 US2, S2-5, S2-6, D-11, D-12).
 *
 * <ul>
 *   <li>글을 저장하면(쓰기·고치기·임시 저장 글 발행) 트랜잭션이 끝난 뒤 따로 도는 일꾼이 그 글을 다시 만든다. 실패해도 글 저장은 성공이다</li>
 *   <li>제목 + 본문 앞부분의 요약값(sha256)이 그대로면 AI를 부르지 않는다</li>
 *   <li>공개 글만 임베딩한다: 비공개 글은 밖(Voyage)으로 보내지 않는다</li>
 *   <li>정리 작업(related.backfill-interval)이 빠진 것·바뀐 것을 채운다. 두 벌 가운데 잠금을 잡은 한 벌만 돈다</li>
 * </ul>
 */
@Service
public class PostEmbeddingService {

    private static final Logger log = LoggerFactory.getLogger(PostEmbeddingService.class);

    private final Embedder embedder;
    private final PostEmbeddingRepository embeddings;
    private final RelatedProperties properties;
    private final TransactionTemplate transaction;
    private final StringRedisTemplate redis;
    private final Clock clock;
    /** 저장 뒤 임베딩하는 일꾼 한 명. 밀리면 버린다(정리 작업이 채운다). */
    private final ThreadPoolExecutor worker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.SECONDS,
            new ArrayBlockingQueue<>(200), runnable -> {
                Thread thread = new Thread(runnable, "post-embedding");
                thread.setDaemon(true);
                return thread;
            }, new ThreadPoolExecutor.DiscardPolicy());

    public PostEmbeddingService(Embedder embedder, PostEmbeddingRepository embeddings, RelatedProperties properties,
            PlatformTransactionManager transactionManager, StringRedisTemplate redis, Clock clock) {
        this.embedder = embedder;
        this.embeddings = embeddings;
        this.properties = properties;
        this.transaction = new TransactionTemplate(transactionManager);
        this.redis = redis;
        this.clock = clock;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onSaved(PostContentSavedEvent event) {
        if (embedder.available()) {
            worker.execute(() -> refresh(List.of(event.postId())));
        }
    }

    @Scheduled(fixedDelayString = "${related.backfill-interval}", initialDelayString = "${related.backfill-initial-delay}")
    public void scheduledBackfill() {
        int done = backfill();
        if (done > 0) {
            log.info("비슷한 글: 뜻 정보 {}개를 맞췄습니다", done);
        }
    }

    /** 빠진 것·바뀐 것을 batch-size개까지 맞추고, 맞춘 수(새로 만듦 + 그대로라 시각만 올림)를 돌려준다. 다른 벌이 돌고 있으면 0. */
    public int backfill() {
        if (!embedder.available()) {
            return 0;
        }
        Integer checked = transaction.execute(status -> {
            if (!embeddings.tryBackfillLock()) {
                return 0;
            }
            return refreshCounting(embeddings.staleOpen(embedder.model(), properties.batchSize()), true);
        });
        return checked == null ? 0 : checked;
    }

    /**
     * 정해진 글들의 뜻 정보를 다시 만들고, 새로 만든 수를 돌려준다. 공개 글만, 내용이 그대로면 건너뛴다.
     * 실패해도 예외를 내지 않는다(글 저장은 성공이다).
     */
    public int refresh(List<Long> postIds) {
        return refreshCounting(postIds, false);
    }

    /**
     * withTouched면 그대로라 시각만 올린 글도 센다(정리 작업이 앞으로 나아갔는지 보려고).
     * 묶음이 실패하면 한 글씩 다시 보내 실패한 글만 건너뛴다. 연달아 두 글이 실패하면 장애로 보고 멈춘다.
     */
    private int refreshCounting(List<Long> postIds, boolean withTouched) {
        if (!embedder.available() || postIds.isEmpty()) {
            return 0;
        }
        int done = 0;
        try {
            List<Source> todo = new ArrayList<>();
            List<String> texts = new ArrayList<>();
            List<String> hashes = new ArrayList<>();
            for (Source source : embeddings.openSources(postIds)) {
                String text = textOf(source);
                String hash = sha256(embedder.model() + "\n" + text);
                Stored stored = embeddings.stored(source.postId()).orElse(null);
                if (stored != null && stored.model().equals(embedder.model()) && stored.contentHash().equals(hash)) {
                    embeddings.touch(source.postId(), clock.instant());
                    done += withTouched ? 1 : 0;
                    continue;
                }
                todo.add(source);
                texts.add(text);
                hashes.add(hash);
            }
            for (int from = 0; from < todo.size(); from += properties.batchSize()) {
                int to = Math.min(todo.size(), from + properties.batchSize());
                List<float[]> vectors;
                try {
                    vectors = embedder.embed(texts.subList(from, to));
                } catch (Embedder.UnavailableException batchFailed) {
                    return done + oneByOne(todo, texts, hashes, from, to);
                }
                for (int i = from; i < to; i++) {
                    save(todo.get(i).postId(), hashes.get(i), vectors.get(i - from));
                    done++;
                }
            }
        } catch (RuntimeException e) {
            log.warn("비슷한 글: 뜻 정보를 만들지 못했습니다 ({})", e.getMessage());
        }
        return done;
    }

    /**
     * 묶음이 실패했을 때 [from, to)를 한 글씩. 거절된 글 하나는 건너뛰고, 연달아 두 글이 실패하면 장애로 보고 멈춘다
     * (장애 때 호출을 글 수만큼 늘리지 않게. 거절되는 글 하나가 뒤 글을 막지도 않게).
     */
    private int oneByOne(List<Source> todo, List<String> texts, List<String> hashes, int from, int to) {
        int saved = 0;
        int failedInRow = 0;
        for (int i = from; i < to && failedInRow < 2; i++) {
            try {
                save(todo.get(i).postId(), hashes.get(i), embedder.embed(List.of(texts.get(i))).get(0));
                saved++;
                failedInRow = 0;
            } catch (Embedder.UnavailableException e) {
                log.warn("비슷한 글: 글 {}의 뜻 정보를 만들지 못했습니다 ({})", todo.get(i).postId(), e.getMessage());
                failedInRow++;
            }
        }
        return saved;
    }

    /** 저장하고, 그 글의 비슷한 글 보관을 지운다(다음 요청이 새 뜻 정보로 고르게). Redis가 멈추면 보관은 cache-ttl 뒤 저절로 사라진다. */
    private void save(long postId, String hash, float[] vector) {
        embeddings.upsert(postId, embedder.model(), hash, vector, clock.instant());
        try {
            redis.delete(RelatedPostService.cacheKey(postId));
        } catch (RuntimeException e) {
            log.warn("비슷한 글: 글 {}의 보관을 지우지 못했습니다 ({})", postId, e.toString());
        }
    }

    /** 임베딩할 글: 제목 + 본문 앞 max-chars자 (D-12). */
    private String textOf(Source source) {
        String content = source.content() == null ? "" : source.content();
        String head = content.length() > properties.maxChars() ? content.substring(0, properties.maxChars()) : content;
        return source.title() + "\n\n" + head;
    }

    private static String sha256(String text) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @PreDestroy
    void stop() {
        worker.shutdown();
    }
}
