package com.myblog.post.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 글 목록·검색의 숫자 (application.yml의 explore.*, specs/004 plan.md 설정값 목록, 헌법 VI).
 * 잘못 정하면 서버가 켜지지 않는다. 글자는 코드 포인트로 센다.
 * 검색 결과도 글 목록과 같은 페이지 글 수·미리보기 길이를 쓴다 (004 D-3: A, 같은 설정값).
 */
@ConfigurationProperties(prefix = "explore")
public record ExploreProperties(ListPage list, Search search) {

    /** 한 페이지 글 수(10)와 미리보기 길이(100자). */
    public record ListPage(int pageSize, int previewLength) {

        public ListPage {
            if (pageSize < 1 || previewLength < 1) {
                throw new IllegalStateException("explore.list의 page-size와 preview-length는 1 이상이어야 합니다");
            }
        }
    }

    /** 검색어 길이 2~50자 (앞뒤 공백을 지운 뒤, 004 FR-010, D-1). */
    public record Search(int keywordMinLength, int keywordMaxLength) {

        public Search {
            if (keywordMinLength < 1 || keywordMaxLength < keywordMinLength) {
                throw new IllegalStateException("explore.search는 1 <= keyword-min-length <= keyword-max-length이어야 합니다");
            }
        }
    }
}
