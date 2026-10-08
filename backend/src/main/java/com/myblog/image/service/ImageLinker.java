package com.myblog.image.service;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.common.error.ErrorResponse.FieldErrorItem;
import com.myblog.image.config.ImageProperties;
import com.myblog.image.domain.PostImage;
import com.myblog.image.repository.PostImageRepository;
import com.myblog.post.PostContentSavedEvent;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 글을 저장할 때 본문 속 이미지를 그 글에 연결한다 (specs/005 T057, D-3, D-4, FR-024, FR-026).
 * 글 저장과 <b>같은 트랜잭션 안에서</b> 듣는다.
 * <ul>
 *   <li>본문의 우리 서버 주소(/api/images/…) 마크다운 이미지만 본다. 이 글의 이미지와 글쓴이가 올린 연결 전 이미지만 연결한다
 *       (남이 올린 이미지 주소는 연결하지 않는다).</li>
 *   <li>그 수가 max-per-post(10)를 넘으면 content 칸에 IMAGE_LIMIT_EXCEEDED로 거절한다: 글 저장 전체가 취소된다.</li>
 *   <li>이 글에 있었는데 본문에서 빠진 이미지는 기록을 지우고, 파일은 커밋 뒤에 지운다 (D-5).</li>
 * </ul>
 */
@Component
public class ImageLinker {

    private final PostImageRepository images;
    private final ImageProperties properties;
    private final ApplicationEventPublisher events;

    public ImageLinker(PostImageRepository images, ImageProperties properties, ApplicationEventPublisher events) {
        this.images = images;
        this.properties = properties;
        this.events = events;
    }

    @EventListener
    public void on(PostContentSavedEvent event) {
        Set<String> keys = ImageUrls.fileNamesIn(event.content()).stream()
                .map(name -> PostImage.KEY_PREFIX + name)
                .collect(Collectors.toSet());
        List<PostImage> linkable = keys.isEmpty() ? List.of() : images.findLinkable(keys, event.postId(), event.ownerId());
        if (linkable.size() > properties.maxPerPost()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.message(), List.of(
                    new FieldErrorItem("content", ErrorCode.IMAGE_LIMIT_EXCEEDED.name(),
                            ImageErrorMessages.limit(properties.maxPerPost()))));
        }
        List<Long> toLink = linkable.stream().filter(image -> image.getPostId() == null).map(PostImage::getId).toList();
        if (!toLink.isEmpty()) {
            images.link(toLink, event.postId());
        }
        List<PostImage> dropped = images.findByPostId(event.postId()).stream()
                .filter(image -> !keys.contains(image.getStorageKey()))
                .toList();
        if (!dropped.isEmpty()) {
            images.deleteAllInBatch(dropped);
            events.publishEvent(new ImageFilesReleased(dropped.stream().map(PostImage::getStorageKey).toList()));
        }
    }
}
