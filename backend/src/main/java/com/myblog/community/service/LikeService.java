package com.myblog.community.service;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.community.repository.PostLikeRepository;
import com.myblog.post.PostLikeSummary.LikeSummary;
import com.myblog.post.PostLookup;
import com.myblog.post.PostLookup.PostRef;
import com.myblog.user.ActiveMemberLock;
import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 글 좋아요 누르기·취소 (specs/005 contracts 4, 5, FR-009 ~ FR-012).
 * 검사 순서: 볼 수 있는 글인가(아니면 없는 글과 같은 POST_NOT_FOUND) → 자기 글이면 SELF_LIKE_NOT_ALLOWED → 처리.
 * 두 요청 모두 같은 결과를 몇 번 보내도 같다: 이미 눌렀으면 누르기는 그대로, 누르지 않았으면 취소도 그대로 (research B-8).
 * "다시 누르면 취소"는 화면이 likedByMe를 보고 둘 중 하나를 고른다.
 * 누르기는 넣기 전에 회원 줄을 잠근다: 같은 회원의 탈퇴와 겹쳐도 탈퇴 정리(LikeCleaner) 뒤에 좋아요가 남지 않는다.
 */
@Service
@Transactional
public class LikeService {

    private final PostLikeRepository likes;
    private final PostLookup postLookup;
    private final ActiveMemberLock memberLock;
    private final Clock clock;

    public LikeService(PostLikeRepository likes, PostLookup postLookup, ActiveMemberLock memberLock, Clock clock) {
        this.likes = likes;
        this.postLookup = postLookup;
        this.memberLock = memberLock;
        this.clock = clock;
    }

    public LikeSummary like(Long memberId, Long postId) {
        PostRef post = visiblePost(postId, memberId);
        if (post.isOwnedBy(memberId)) {
            throw new ApiException(ErrorCode.SELF_LIKE_NOT_ALLOWED);
        }
        if (!memberLock.lockIfActive(memberId)) {
            // 이 요청이 기다리는 사이 탈퇴가 끝났다
            throw new ApiException(ErrorCode.UNAUTHENTICATED);
        }
        likes.insertIfAbsent(post.postId(), memberId, Instant.now(clock));
        return summary(post.postId(), memberId);
    }

    public LikeSummary unlike(Long memberId, Long postId) {
        PostRef post = visiblePost(postId, memberId);
        likes.deleteByPostIdAndMemberId(post.postId(), memberId);
        return summary(post.postId(), memberId);
    }

    /** 글 상세에도 쓰는 좋아요 수와 "내가 눌렀나". viewerId는 로그인하지 않았으면 null. */
    @Transactional(readOnly = true)
    public LikeSummary summary(Long postId, Long viewerId) {
        boolean likedByMe = viewerId != null && likes.existsByPostIdAndMemberId(postId, viewerId);
        return new LikeSummary(likes.countByPostId(postId), likedByMe);
    }

    private PostRef visiblePost(Long postId, Long viewerId) {
        return postLookup.findVisible(postId, viewerId).orElseThrow(() -> new ApiException(ErrorCode.POST_NOT_FOUND));
    }
}
