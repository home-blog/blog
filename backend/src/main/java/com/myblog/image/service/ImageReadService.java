package com.myblog.image.service;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.image.domain.ImageType;
import com.myblog.image.domain.PostImage;
import com.myblog.image.repository.PostImageRepository;
import com.myblog.image.storage.ImageStorage;
import com.myblog.image.storage.ImageStorage.StoredFile;
import com.myblog.post.PostLookup;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 이미지 보기 (specs/005 T056, contracts 11, research B-5, FR-026).
 * 글에 연결된 이미지는 그 글을 볼 수 있는 사람만(PostLookup), 연결 전 이미지는 올린 사람만. 아니면 없는 이미지와 같은 404.
 */
@Service
public class ImageReadService {

    private final PostImageRepository images;
    private final ImageStorage storage;
    private final PostLookup postLookup;
    private final TransactionTemplate readOnly;

    public ImageReadService(PostImageRepository images, ImageStorage storage, PostLookup postLookup,
            PlatformTransactionManager transactionManager) {
        this.images = images;
        this.storage = storage;
        this.postLookup = postLookup;
        this.readOnly = new TransactionTemplate(transactionManager);
        this.readOnly.setReadOnly(true);
    }

    /** viewerId는 로그인하지 않았으면 null. 받은 쪽이 스트림을 닫는다. */
    public ImageFile open(String fileName, Long viewerId) {
        if (!ImageUrls.isFileName(fileName)) {
            throw notFound();
        }
        PostImage image = readOnly.execute(status -> images.findByStorageKey(PostImage.KEY_PREFIX + fileName)
                .filter(found -> canSee(found, viewerId))
                .orElseThrow(ImageReadService::notFound));
        StoredFile file = storage.open(image.getStorageKey()).orElseThrow(ImageReadService::notFound);
        return new ImageFile(image.type(), file);
    }

    private boolean canSee(PostImage image, Long viewerId) {
        if (image.getPostId() == null) {
            return viewerId != null && viewerId.equals(image.getMemberId());
        }
        return postLookup.findVisible(image.getPostId(), viewerId).isPresent();
    }

    private static ApiException notFound() {
        return new ApiException(ErrorCode.NOT_FOUND);
    }

    public record ImageFile(ImageType type, StoredFile file) {
    }
}
