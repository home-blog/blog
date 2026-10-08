package com.myblog.image.service;

import com.myblog.common.error.ApiException;
import com.myblog.image.config.ImageProperties;
import com.myblog.image.domain.PostImage;
import com.myblog.image.repository.PostImageRepository;
import com.myblog.image.storage.ImageStorage;
import com.myblog.image.storage.ImageStorage.StoredObject;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 이미지 정리 작업 (specs/005 T058, FR-027, research D-3, D-5). cleanup-interval마다 돈다.
 * <ol>
 *   <li>올린 지 orphan-ttl(24시간, 가안)이 지났는데 글에 연결되지 않은 이미지: 기록을 지우고 파일은 커밋 뒤에 지운다.
 *       글을 저장하지 않고 나간 경우, 탈퇴한 회원의 연결 전 이미지가 여기서 지워진다.</li>
 *   <li>저장소에는 있는데 기록이 없는 파일(글 삭제 뒤 파일 삭제가 실패한 경우 등, D-5 A): orphan-ttl보다 오래된 것만 지운다.
 *       방금 올리는 중인 파일(기록이 아직 커밋되지 않음)을 지우지 않기 위해서다.</li>
 * </ol>
 * 저장소에 연결할 수 없으면 로그만 남기고 다음 차례에 다시 한다.
 */
@Component
public class OrphanImageCleaner {

    private static final Logger log = LoggerFactory.getLogger(OrphanImageCleaner.class);
    private static final int BATCH = 500;

    private final PostImageRepository images;
    private final ImageStorage storage;
    private final ImageProperties properties;
    private final ApplicationEventPublisher events;
    private final TransactionTemplate transaction;
    private final Clock clock;

    public OrphanImageCleaner(PostImageRepository images, ImageStorage storage, ImageProperties properties,
            ApplicationEventPublisher events, PlatformTransactionManager transactionManager, Clock clock) {
        this.images = images;
        this.storage = storage;
        this.properties = properties;
        this.events = events;
        this.transaction = new TransactionTemplate(transactionManager);
        this.clock = clock;
    }

    @Scheduled(fixedDelayString = "${community.image.cleanup-interval}",
            initialDelayString = "${community.image.cleanup-interval}")
    public void cleanUp() {
        Instant before = Instant.now(clock).minus(properties.orphanTtl());
        transaction.executeWithoutResult(status -> {
            List<String> removed = images.deleteUnlinkedBefore(before);
            if (!removed.isEmpty()) {
                events.publishEvent(new ImageFilesReleased(removed));
                log.info("글에 연결되지 않은 이미지 {}개를 지웁니다", removed.size());
            }
        });
        try {
            removeFilesWithoutRecord(before);
        } catch (ApiException e) {
            log.warn("이미지 저장소에 연결할 수 없어 기록 없는 파일 정리를 다음에 합니다");
        }
    }

    private void removeFilesWithoutRecord(Instant before) {
        List<String> candidates = storage.list(PostImage.KEY_PREFIX).stream()
                .filter(object -> object.lastModified() != null && object.lastModified().isBefore(before))
                .map(StoredObject::key)
                .toList();
        for (int from = 0; from < candidates.size(); from += BATCH) {
            List<String> batch = candidates.subList(from, Math.min(from + BATCH, candidates.size()));
            Set<String> existing = new HashSet<>(images.findExistingKeys(batch));
            List<String> stray = new ArrayList<>(batch);
            stray.removeAll(existing);
            for (String key : stray) {
                try {
                    storage.delete(key);
                } catch (ApiException e) {
                    log.warn("기록 없는 이미지 파일을 지우지 못했습니다: {}", key);
                }
            }
        }
    }
}
