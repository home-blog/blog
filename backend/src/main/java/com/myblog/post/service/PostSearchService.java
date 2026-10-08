package com.myblog.post.service;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.post.config.ExploreProperties;
import com.myblog.post.repository.PostSearchRepository;
import com.myblog.post.repository.PostSearchRepository.Match;
import com.myblog.post.service.PageNumbers.PageSlice;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 여러 블로그의 공개 글 검색 (specs/004 contracts 2, FR-010 ~ FR-018).
 * 검색어를 다듬어 길이를 먼저 본 뒤에만 쿼리를 실행한다 (서버 검사가 마지막 방어선, 헌법 IV).
 * 로그인했든 안 했든 결과는 같다: 비공개 글과 비공개 분류의 글은 항상 빠진다 (FR-011, D-7). 검색 기록은 남기지 않는다 (R-6).
 */
@Service
@Transactional(readOnly = true)
public class PostSearchService {

    private final PostSearchRepository searchRepository;
    private final PostPreview preview;
    private final int pageSize;
    private final int keywordMinLength;
    private final int keywordMaxLength;

    public PostSearchService(PostSearchRepository searchRepository, PostPreview preview, ExploreProperties properties) {
        this.searchRepository = searchRepository;
        this.preview = preview;
        // 검색 결과도 글 목록과 같은 페이지 글 수·미리보기 길이를 쓴다 (D-3: A)
        this.pageSize = properties.list().pageSize();
        this.keywordMinLength = properties.search().keywordMinLength();
        this.keywordMaxLength = properties.search().keywordMaxLength();
    }

    /**
     * @param q    검색어 그대로 (없으면 null)
     * @param page 요청한 페이지 번호 글자 그대로 (없으면 null)
     */
    public SearchView search(String q, String page) {
        SearchKeyword keyword = SearchKeyword.of(q);
        check(keyword);
        List<String> patterns = keyword.containsPatterns();
        long totalCount = searchRepository.count(patterns);
        PageSlice slice = PageNumbers.of(page, totalCount, pageSize);
        List<SearchRow> rows = searchRepository.find(patterns, slice.offset(), slice.pageSize()).stream()
                .map(this::toRow)
                .toList();
        return new SearchView(keyword.keyword(), totalCount, slice.page(), slice.totalPages(), slice.pageSize(), rows);
    }

    /** 2자 미만(비어 있음, 공백만, 1자)이면 400, 50자를 넘으면 400 (FR-010, D-1: A). */
    private void check(SearchKeyword keyword) {
        int length = keyword.length();
        if (length < keywordMinLength) {
            throw new ApiException(ErrorCode.SEARCH_KEYWORD_TOO_SHORT,
                    ErrorCode.SEARCH_KEYWORD_TOO_SHORT.message().formatted(keywordMinLength));
        }
        if (length > keywordMaxLength) {
            throw new ApiException(ErrorCode.SEARCH_KEYWORD_TOO_LONG,
                    ErrorCode.SEARCH_KEYWORD_TOO_LONG.message().formatted(keywordMaxLength));
        }
    }

    private SearchRow toRow(Match match) {
        return new SearchRow(match.postId(), match.blogId(), match.blogName(), match.title(), match.categoryId(),
                match.categoryName(), match.createdAt(), preview.of(match.content()));
    }

    /** keyword는 실제로 찾은 값(앞뒤 공백을 지운 값)이다 (FR-017). */
    public record SearchView(String keyword, long totalCount, int page, int totalPages, int pageSize,
            List<SearchRow> results) {
    }

    /** 검색 결과 한 줄. 항상 공개 글이라 visibility는 없다 (contracts 2 동작 8). */
    public record SearchRow(Long postId, Long blogId, String blogName, String title, Long categoryId,
            String categoryName, Instant createdAt, String preview) {
    }
}
