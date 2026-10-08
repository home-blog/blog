package com.myblog.community.service;

import com.myblog.community.repository.PostLikeRepository;
import com.myblog.post.PostDeletingEvent;
import com.myblog.user.MemberWithdrawnEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 좋아요 정리 (specs/005 T032, FR-028, SC-005, data-model 7). 둘 다 <b>같은 트랜잭션 안에서</b> 듣는다:
 * 하나라도 실패하면 글 삭제·탈퇴 전체가 취소된다.
 * <ul>
 *   <li>글이 지워지면(PostDeletingEvent) 그 글의 좋아요를 지운다. 탈퇴로 블로그가 닫힐 때도 글마다 이 이벤트가 온다.</li>
 *   <li>회원이 탈퇴하면(MemberWithdrawnEvent) <b>그 회원이 남의 글에 누른 좋아요</b>를 모두 지운다 (002 T035, D-5).</li>
 * </ul>
 */
@Component
public class LikeCleaner {

    private final PostLikeRepository likes;

    public LikeCleaner(PostLikeRepository likes) {
        this.likes = likes;
    }

    @EventListener
    public void on(PostDeletingEvent event) {
        likes.deleteByPostId(event.postId());
    }

    @EventListener
    public void on(MemberWithdrawnEvent event) {
        likes.deleteByMemberId(event.memberId());
    }
}
