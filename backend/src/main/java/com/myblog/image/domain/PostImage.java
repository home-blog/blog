package com.myblog.image.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;

/**
 * 글 이미지 기록 (post_image 표, specs/005 data-model 5, 팀 ERD 요청 T-1, D-3).
 * 올리면 "아직 글 없음"(postId 비어 있음)으로 기록하고, 글을 저장할 때 본문에 남은 것만 그 글에 연결한다.
 * 형식은 storageKey의 확장자다 (올릴 때 확인한 형식).
 */
@Entity
@Table(name = "post_image")
public class PostImage {

    /** 저장소 안의 폴더. 바깥 주소는 /api/images/{파일 이름}이다. */
    public static final String KEY_PREFIX = "posts/";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "post_image_id")
    private Long id;

    @Column(name = "post_id")
    private Long postId;

    @Column(name = "users_id", nullable = false, updatable = false)
    private Long memberId;

    @Column(name = "storage_key", nullable = false, length = 500, unique = true, updatable = false)
    private String storageKey;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected PostImage() {
    }

    public static PostImage uploaded(Long memberId, String fileName, Instant now) {
        PostImage image = new PostImage();
        image.memberId = memberId;
        image.storageKey = KEY_PREFIX + fileName;
        image.createdAt = now;
        return image;
    }

    /** 바깥 주소의 파일 이름 ({UUID}.{확장자}). */
    public String fileName() {
        return storageKey.substring(KEY_PREFIX.length());
    }

    public ImageType type() {
        String name = fileName();
        return ImageType.fromExtension(name.substring(name.lastIndexOf('.') + 1)).orElseThrow();
    }

    public Long getId() {
        return id;
    }

    public Long getPostId() {
        return postId;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getStorageKey() {
        return storageKey;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
