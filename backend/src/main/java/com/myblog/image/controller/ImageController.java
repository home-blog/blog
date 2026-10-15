package com.myblog.image.controller;

import com.myblog.image.service.ImageReadService;
import com.myblog.image.service.ImageReadService.ImageFile;
import com.myblog.image.service.ImageUploadService;
import com.myblog.image.service.ImageUploadService.Uploaded;
import com.myblog.image.service.ImageUrls;
import com.myblog.user.LoggedInMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Duration;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 이미지 올리기·보기 (specs/005 contracts 10, 11). 회원 번호는 LoggedInMember로만 얻는다.
 * 보기는 누구나 부르고(SecurityConfig의 GET /api/images/**), 볼 수 있는지는 서비스가 정한다.
 */
@Tag(name = "8. 이미지")
@RestController
public class ImageController {

    /** 볼 수 있는 사람이 바뀔 수 있어(글을 비공개로) 공용 캐시에는 남기지 않고 브라우저에만 잠깐 둔다 (가안). */
    private static final CacheControl CACHE = CacheControl.maxAge(Duration.ofMinutes(5)).cachePrivate();

    private final LoggedInMember loggedInMember;
    private final ImageUploadService uploadService;
    private final ImageReadService readService;

    public ImageController(LoggedInMember loggedInMember, ImageUploadService uploadService, ImageReadService readService) {
        this.loggedInMember = loggedInMember;
        this.uploadService = uploadService;
        this.readService = readService;
    }

    /** 이미지 한 장 올리기. postId는 수정 중인 글의 번호 (선택). */
    @Operation(summary = "이미지 올리기", description = "받은 주소를 본문 마크다운에 넣는다. 형식·한 장 크기(`community.image.allowed-types`, `max-size`)를 확인하고, 글에 연결되지 않은 이미지는 정한 시간(`orphan-ttl`) 뒤 지운다.")
    @PostMapping(path = "/api/{version}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    public Uploaded upload(Authentication authentication, @RequestParam(name = "file", required = false) MultipartFile file,
            @RequestParam(name = "postId", required = false) Long postId) {
        return uploadService.upload(loggedInMember.requireIdOf(authentication), file, postId);
    }

    /** 이미지 보기. Content-Type은 올릴 때 확인한 형식이고, 브라우저가 다른 형식으로 짐작하지 않게 nosniff를 붙인다. */
    @Operation(summary = "이미지 보기", description = "글에 연결된 이미지는 그 글을 볼 수 있는 사람만, 아직 글에 연결되지 않은 이미지는 올린 사람만 본다. 아니면 404.")
    @GetMapping(ImageUrls.PATH + "{fileName}")
    public ResponseEntity<InputStreamResource> view(@PathVariable String fileName, Authentication authentication) {
        ImageFile image = readService.open(fileName, loggedInMember.idOf(authentication).orElse(null));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(image.type().contentType()))
                .contentLength(image.file().size())
                .cacheControl(CACHE)
                .header("X-Content-Type-Options", "nosniff")
                .body(new InputStreamResource(image.file().content()));
    }
}
