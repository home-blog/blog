package com.myblog.image.service;

import com.myblog.image.storage.ImageStorage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 기록이 지워진 이미지 파일을 <b>트랜잭션이 끝난 뒤</b> 지운다 (specs/005 T058, D-5 A).
 * 파일 삭제가 실패해도 글 삭제는 이미 끝났다: 로그만 남기고, 정리 작업(OrphanImageCleaner)이 기록 없는 파일을 다시 지운다.
 */
@Component
class ImageFileRemover {

    private static final Logger log = LoggerFactory.getLogger(ImageFileRemover.class);

    private final ImageStorage storage;

    ImageFileRemover(ImageStorage storage) {
        this.storage = storage;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    void on(ImageFilesReleased event) {
        removeQuietly(event.storageKeys());
    }

    void removeQuietly(Iterable<String> keys) {
        for (String key : keys) {
            try {
                storage.delete(key);
            } catch (RuntimeException e) {
                log.warn("이미지 파일을 지우지 못했습니다. 정리 작업이 다시 지웁니다: {}", key, e);
            }
        }
    }
}
