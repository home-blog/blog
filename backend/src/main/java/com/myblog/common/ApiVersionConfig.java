package com.myblog.common;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Arrays;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.server.RequestPath;
import org.springframework.web.servlet.config.annotation.ApiVersionConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * API 주소 버전 (2026-10-12 사용자: 릴리스를 API에도 반영). 모든 API는 /api/{version}/… 이고 지금은 v1 하나다.
 * Spring Framework 7의 API 버전 기능을 쓴다: 주소의 두 번째 칸(/api/<b>v1</b>/posts)을 버전으로 읽는다.
 * <ul>
 *   <li>버전은 릴리스(0.4.0 …)와 따로 간다. 칸을 더하는 것처럼 예전 화면이 깨지지 않는 변경은 v1 그대로 두고,
 *       칸을 빼거나 이름·뜻을 바꾸는 변경일 때만 v2를 연다. 그때 바뀌는 처리기에만 version = "2"를 붙이고
 *       예전 처리기에는 version = "1"을 붙여 한동안 같이 둔다 (버전을 적지 않은 처리기는 모든 버전에 답한다).</li>
 *   <li>지원하지 않는 버전(/api/v9/…)은 400 API_VERSION_UNSUPPORTED (GlobalExceptionHandler).</li>
 *   <li>이미지 파일 주소(/api/images/{파일})는 글 본문에 저장되므로 버전 없이 고정한다. 올리기는 /api/v1/images.</li>
 * </ul>
 */
@Configuration
public class ApiVersionConfig implements WebMvcConfigurer {

    /** 지금 쓰는 API 버전들 (새 버전을 열면 여기에 더한다). */
    public static final String[] SUPPORTED = {"1"};

    private static final String API_PREFIX = "/api/";
    /** image 모듈 ImageUrls.PATH와 같다 (common은 다른 모듈을 부르지 않는다) */
    private static final String IMAGE_FILES = "/api/images/";

    @Override
    public void configureApiVersioning(ApiVersionConfigurer configurer) {
        configurer.usePathSegment(1, ApiVersionConfig::isVersioned)
                .addSupportedVersions(SUPPORTED)
                // 버전 칸이 없는 주소(로봇용 /og·/rss.xml, 이미지 파일, /actuator)는 버전을 따지지 않는다
                .setDefaultVersion(SUPPORTED[0]);
    }

    static boolean isVersioned(RequestPath path) {
        return isVersionedPath(path.pathWithinApplication().value());
    }

    /**
     * 버전이 없거나(/api/posts) 지원하지 않는 버전(/api/v9/posts)의 API 주소인가. 보안 설정이 이런 요청은 로그인과 상관없이
     * 통과시켜 "로그인이 필요합니다"(401) 대신 400 API_VERSION_UNSUPPORTED로 답하게 한다. 이런 주소에 답하는 처리기는 없다.
     */
    public static boolean isUnsupportedApiRequest(HttpServletRequest request) {
        String path = request.getRequestURI().substring(request.getContextPath().length());
        if (!isVersionedPath(path)) {
            return false;
        }
        String segment = path.substring(API_PREFIX.length()).split("/", 2)[0];
        String version = segment.startsWith("v") || segment.startsWith("V") ? segment.substring(1) : segment;
        return !Arrays.asList(SUPPORTED).contains(version);
    }

    private static boolean isVersionedPath(String path) {
        return path.startsWith(API_PREFIX) && !path.startsWith(IMAGE_FILES);
    }
}
