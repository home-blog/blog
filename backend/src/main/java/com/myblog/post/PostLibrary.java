package com.myblog.post;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

/**
 * 글 모듈이 AI 비서 연결(connect 모듈, specs/020)에 여는 입구: 원고 찾기·단락 찾기(RAG의 "찾기")·읽기·내 글·임시 저장.
 * 보는 조건은 화면과 같다(PostVisibility): 공개 분류의 공개 글 + 내 블로그의 모든 글. 남의 비공개 글은 없는 글이다.
 * 검색어 규칙(2~50자)은 글 검색과 같고, 어기면 ApiException(SEARCH_KEYWORD_TOO_SHORT/LONG)을 던진다.
 */
public interface PostLibrary {

    /** 검색어의 모든 낱말이 제목이나 본문에 있는 글, 최근 순 limit개. mineOnly면 내 블로그 글만. */
    List<PostHit> search(Long viewerId, String query, boolean mineOnly, int limit);

    /** 검색어와 가장 잘 맞는 단락(출처 포함). 단어 점수만 쓴다 (research D-3). */
    List<Passage> passages(Long viewerId, String query, boolean mineOnly, PassageOptions options);

    /** 볼 수 있는 글의 원문. 없거나 볼 수 없으면 비어 있다. */
    Optional<PostDocument> read(Long viewerId, Long postId);

    /** 내 블로그 글, 최근 순 (비공개 포함). page는 1부터, 한 쪽 크기는 블로그 관리 글 목록과 같다. */
    MyPostPage myPosts(Long memberId, int page);

    /** 내 블로그에 임시 저장 글을 만든다(발행하지 않는다, research D-4). 한도·글자 수 검사는 화면 저장과 같다. */
    SavedDraft saveDraft(Long memberId, String title, String content);

    /** 글 한 줄. url은 사이트 안 주소(/posts/{id}). */
    record PostHit(Long postId, String title, String blogName, Instant createdAt, String visibility, String excerpt,
            String url) {
    }

    /**
     * 단락 하나. paragraph는 글 안에서 몇 번째 덩어리인지(1부터, 이미지 줄은 세지 않는다). score는 높을수록 잘 맞는다.
     */
    record Passage(Long postId, String title, String blogName, String url, int paragraph, String text, double score) {
    }

    /** candidates: 살펴볼 후보 글 수, maxLength: 단락 글자 상한, perPost·limit: 글마다·전체 상한. */
    record PassageOptions(int candidates, int maxLength, int perPost, int limit) {
    }

    record PostDocument(Long postId, String title, String blogName, String content, String visibility,
            Instant createdAt, List<String> tags, String url) {
    }

    record MyPost(Long postId, String title, String category, String visibility, Instant createdAt, int views,
            long commentCount, String url) {
    }

    record MyPostPage(List<MyPost> posts, int page, int pageSize, long totalCount, int draftCount) {
    }

    /** editUrl은 그 임시 저장 글을 여는 글쓰기 화면 주소. */
    record SavedDraft(Long draftId, String title, String editUrl) {
    }
}
