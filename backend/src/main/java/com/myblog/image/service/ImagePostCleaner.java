package com.myblog.image.service;

import com.myblog.image.domain.PostImage;
import com.myblog.image.repository.PostImageRepository;
import com.myblog.post.PostDeletingEvent;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 글이 지워지면 그 글의 이미지 기록을 같은 트랜잭션에서 지우고, 파일은 <b>커밋 뒤에</b> 지운다 (specs/005 T058, FR-027, D-5 A).
 * 탈퇴로 블로그가 닫힐 때도 글마다 이 이벤트가 온다. 탈퇴한 회원의 연결 전 이미지는 정리 작업이 지운다 (가안).
 */
@Component
public class ImagePostCleaner {

    private final PostImageRepository images;
    private final ApplicationEventPublisher events;

    public ImagePostCleaner(PostImageRepository images, ApplicationEventPublisher events) {
        this.images = images;
        this.events = events;
    }

    @EventListener
    public void on(PostDeletingEvent event) {
        List<PostImage> found = images.findByPostId(event.postId());
        if (found.isEmpty()) {
            return;
        }
        images.deleteAllInBatch(found);
        images.flush();
        events.publishEvent(new ImageFilesReleased(found.stream().map(PostImage::getStorageKey).toList()));
    }
}
