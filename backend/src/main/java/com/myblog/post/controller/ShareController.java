package com.myblog.post.controller;

import com.myblog.post.service.ShareService;
import java.time.Duration;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * 로봇이 읽는 주소 (specs/007 US4·US5·US6). 모두 로그인 없이 GET만 된다.
 * <ul>
 *   <li>/og/…: 링크 미리보기 로봇(카카오톡·디스코드 등)에게만 화면 컨테이너 Nginx가 넘기는 카드 HTML. 사람은 화면을 받는다</li>
 *   <li>/rss.xml, /blog/{id}/rss.xml, /sitemap.xml, /robots.txt</li>
 * </ul>
 */
@RestController
public class ShareController {

    private static final MediaType RSS = MediaType.parseMediaType("application/rss+xml;charset=UTF-8");
    private static final MediaType XML = MediaType.parseMediaType("application/xml;charset=UTF-8");
    private static final MediaType HTML = MediaType.parseMediaType("text/html;charset=UTF-8");
    private static final MediaType TEXT = MediaType.parseMediaType("text/plain;charset=UTF-8");
    /** 로봇이 자주 다시 가져가지 않게 10분 동안 캐시해도 된다고 알린다. */
    private static final CacheControl CACHE = CacheControl.maxAge(Duration.ofMinutes(10)).cachePublic();

    private final ShareService shareService;

    public ShareController(ShareService shareService) {
        this.shareService = shareService;
    }

    @GetMapping({"/og", "/og/"})
    public ResponseEntity<String> homeCard() {
        return ok(HTML, shareService.homeCard(base()));
    }

    @GetMapping("/og/blog/{blogId:\\d+}")
    public ResponseEntity<String> blogCard(@PathVariable long blogId) {
        return ok(HTML, shareService.blogCard(base(), blogId));
    }

    @GetMapping("/og/posts/{postId:\\d+}")
    public ResponseEntity<String> postCard(@PathVariable long postId) {
        return ok(HTML, shareService.postCard(base(), postId));
    }

    @GetMapping("/rss.xml")
    public ResponseEntity<String> siteRss() {
        return ok(RSS, shareService.siteRss(base()));
    }

    @GetMapping("/blog/{blogId:\\d+}/rss.xml")
    public ResponseEntity<String> blogRss(@PathVariable long blogId) {
        return shareService.blogRss(base(), blogId)
                .map(body -> ok(RSS, body))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/sitemap.xml")
    public ResponseEntity<String> sitemap() {
        return ok(XML, shareService.sitemap(base()));
    }

    @GetMapping("/robots.txt")
    public ResponseEntity<String> robots() {
        return ok(TEXT, shareService.robots(base()));
    }

    /** 요청이 들어온 주소 (배포에서는 바깥 Nginx가 넘긴 Host·X-Forwarded-Proto 기준, forward-headers-strategy). */
    private static String base() {
        return ServletUriComponentsBuilder.fromCurrentContextPath().replaceQuery(null).build().toUriString();
    }

    private static ResponseEntity<String> ok(MediaType type, String body) {
        return ResponseEntity.ok().contentType(type).cacheControl(CACHE).body(body);
    }
}
