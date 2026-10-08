package com.myblog.community.service;

import com.myblog.community.repository.PostReportRepository;
import com.myblog.post.PostDeletingEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 글 신고 정리 (specs/005 T038, data-model 7의 4번).
 * <ul>
 *   <li>글이 지워지면(PostDeletingEvent) 그 글의 신고도 같은 트랜잭션에서 지운다 (D-11, 003 D-7). 신고가 있어도 글 삭제는 막히지 않는다.</li>
 *   <li>MemberWithdrawnEvent는 <b>듣지 않는다</b>: 탈퇴한 회원이 한 신고는 남긴다 (002 D-5).</li>
 * </ul>
 */
@Component
public class ReportCleaner {

    private final PostReportRepository reports;

    public ReportCleaner(PostReportRepository reports) {
        this.reports = reports;
    }

    @EventListener
    public void on(PostDeletingEvent event) {
        reports.deleteByPostId(event.postId());
    }
}
