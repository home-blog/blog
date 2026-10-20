package com.myblog.post.service;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.post.PostLibrary;
import com.myblog.post.config.ExploreProperties;
import com.myblog.post.controller.dto.DraftRequests;
import com.myblog.post.repository.PostLibraryRepository;
import com.myblog.post.repository.PostLibraryRepository.Found;
import com.myblog.post.service.ManagePostQueryService.ManagePostPage;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * PostLibrary를 채운다 (specs/020 T005). 보는 조건은 PostLibraryRepository(= PostVisibility), 검색어 길이는 글 검색과 같다.
 * 내 글 목록은 블로그 관리 글 목록을, 임시 저장은 글쓰기 화면의 임시 저장을 그대로 쓴다(검사·한도가 같다, S3-1).
 */
@Service
public class PostLibraryService implements PostLibrary {

    private static final String DRAFT_EDIT_URL = "/write?draft=";

    private final PostLibraryRepository repository;
    private final PostPreview preview;
    private final PostTagService tags;
    private final ManagePostQueryService managePosts;
    private final DraftService drafts;
    private final int keywordMinLength;
    private final int keywordMaxLength;

    public PostLibraryService(PostLibraryRepository repository, PostPreview preview, PostTagService tags,
            ManagePostQueryService managePosts, DraftService drafts, ExploreProperties properties) {
        this.repository = repository;
        this.preview = preview;
        this.tags = tags;
        this.managePosts = managePosts;
        this.drafts = drafts;
        this.keywordMinLength = properties.search().keywordMinLength();
        this.keywordMaxLength = properties.search().keywordMaxLength();
    }

    @Override
    @Transactional(readOnly = true)
    public List<PostHit> search(Long viewerId, String query, boolean mineOnly, int limit) {
        SearchKeyword keyword = checked(query);
        return repository.findAll(viewerId, mineOnly, keyword.containsPatterns(), Math.max(1, limit)).stream()
                .map(found -> new PostHit(found.postId(), found.title(), found.blogName(), found.createdAt(),
                        found.visibility(), preview.of(found.content()), url(found.postId())))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Passage> passages(Long viewerId, String query, boolean mineOnly, PassageOptions options) {
        SearchKeyword keyword = checked(query);
        List<Found> candidates = repository.findAny(viewerId, mineOnly, keyword.containsPatterns(),
                Math.max(1, options.candidates()));
        return PassageFinder.find(candidates, keyword.words(), options);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<PostDocument> read(Long viewerId, Long postId) {
        return repository.findReadable(viewerId, postId).map(found -> new PostDocument(found.postId(), found.title(),
                found.blogName(), found.content(), found.visibility(), found.createdAt(), tags.tagsOf(found.postId()),
                url(found.postId())));
    }

    @Override
    @Transactional(readOnly = true)
    public MyPostPage myPosts(Long memberId, int page) {
        ManagePostPage result = managePosts.list(memberId, null, null, Math.max(1, page));
        List<MyPost> posts = result.items().stream()
                .map(row -> new MyPost(row.postId(), row.title(), row.category().name(), row.visibility(),
                        row.createdAt(), row.views(), row.commentCount(), url(row.postId())))
                .toList();
        return new MyPostPage(posts, result.page(), result.pageSize(), result.totalCount(),
                drafts.list(memberId).count());
    }

    @Override
    public SavedDraft saveDraft(Long memberId, String title, String content) {
        DraftService.Saved saved = drafts.create(memberId,
                new DraftRequests.Save(title, content, null, null, null, null, null));
        return new SavedDraft(saved.draftId(), title == null ? "" : title.strip(), DRAFT_EDIT_URL + saved.draftId());
    }

    /** 검색어를 다듬어 길이를 본다 (글 검색과 같은 규칙, 004 FR-010). */
    private SearchKeyword checked(String query) {
        SearchKeyword keyword = SearchKeyword.of(query);
        int length = keyword.length();
        if (length < keywordMinLength) {
            throw new ApiException(ErrorCode.SEARCH_KEYWORD_TOO_SHORT,
                    ErrorCode.SEARCH_KEYWORD_TOO_SHORT.message().formatted(keywordMinLength));
        }
        if (length > keywordMaxLength) {
            throw new ApiException(ErrorCode.SEARCH_KEYWORD_TOO_LONG,
                    ErrorCode.SEARCH_KEYWORD_TOO_LONG.message().formatted(keywordMaxLength));
        }
        return keyword;
    }

    private static String url(Long postId) {
        return "/posts/" + postId;
    }
}
