package com.myblog.post.service;

import com.myblog.post.config.TagProperties;
import com.myblog.post.repository.TagPostRepository;
import com.myblog.post.service.PageNumbers.PageSlice;
import com.myblog.post.service.PostSearchService.SearchRow;
import com.myblog.post.tag.TagNormalizer;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 같은 태그의 공개 글 목록 (specs/005 T045, contracts 9, FR-015, SC-008).
 * 주소의 이름은 글에 붙일 때와 같은 방법으로 다듬는다(#·대소문자 무시). 규칙에 맞지 않거나 없는 태그는 빈 목록이다.
 * 최신순, community.tag.page-size개씩, 없는 페이지면 마지막 페이지(004 PageNumbers). 한 줄 모양은 004 검색 결과와 같다.
 */
@Service
@Transactional(readOnly = true)
public class TagPostService {

    private final TagPostRepository tagPosts;
    private final TagNormalizer tagNormalizer;
    private final PostPreview preview;
    private final int pageSize;

    public TagPostService(TagPostRepository tagPosts, TagNormalizer tagNormalizer, PostPreview preview,
            TagProperties properties) {
        this.tagPosts = tagPosts;
        this.tagNormalizer = tagNormalizer;
        this.preview = preview;
        this.pageSize = properties.pageSize();
    }

    /** page는 요청한 페이지 번호 글자 그대로 (없으면 null). */
    public TagPostsView list(String tagName, String page) {
        Optional<String> tag = tagNormalizer.normalizeOne(tagName);
        long totalCount = tag.map(tagPosts::count).orElse(0L);
        PageSlice slice = PageNumbers.of(page, totalCount, pageSize);
        List<SearchRow> rows = tag.isEmpty() || totalCount == 0 ? List.of()
                : tagPosts.find(tag.get(), slice.offset(), slice.pageSize()).stream()
                        .map(match -> new SearchRow(match.postId(), match.blogId(), match.blogName(), match.title(),
                                match.categoryId(), match.categoryName(), match.createdAt(), preview.of(match.content())))
                        .toList();
        return new TagPostsView(tag.orElse(tagName == null ? "" : tagName.strip()), totalCount, slice.page(),
                slice.totalPages(), slice.pageSize(), rows);
    }

    /** tag는 다듬은 이름(소문자). */
    public record TagPostsView(String tag, long totalCount, int page, int totalPages, int pageSize, List<SearchRow> posts) {
    }
}
