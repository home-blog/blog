package com.myblog.community.service;

import com.myblog.community.repository.SentenceClipRepository;
import com.myblog.post.PostDeletingEvent;
import com.myblog.user.MemberWithdrawnEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 필사 노트 정리 (specs/015 US3). LikeCleaner처럼 <b>같은 트랜잭션 안에서</b> 듣는다.
 * <ul>
 *   <li>글이 지워지면 문장은 남기고 원문 자리만 비운다(모을 때의 제목·블로그 이름은 남는다).</li>
 *   <li>회원이 탈퇴하면 그 회원의 필사 노트를 모두 지운다(S3-8).</li>
 * </ul>
 */
@Component
public class SentenceClipCleaner {

    private final SentenceClipRepository clips;

    public SentenceClipCleaner(SentenceClipRepository clips) {
        this.clips = clips;
    }

    @EventListener
    public void on(PostDeletingEvent event) {
        clips.clearPost(event.postId());
    }

    @EventListener
    public void on(MemberWithdrawnEvent event) {
        clips.deleteByMemberId(event.memberId());
    }
}
