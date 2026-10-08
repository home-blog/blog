package com.myblog.stats.service;

import com.myblog.blog.BlogVisitedEvent;
import com.myblog.post.PostViewedEvent;
import com.myblog.stats.time.ServiceDay;
import java.time.LocalDate;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 조회수·방문자 세기 (specs/006 T055, T056, contracts 9, research B-3, NF-09).
 * <ul>
 *   <li>글 상세(PostViewedEvent): 같은 사람·같은 글 30분에 한 번 조회수, 같은 사람·같은 블로그 하루에 한 번 방문자.</li>
 *   <li>블로그 첫 화면·분류별 목록(BlogVisitedEvent): 방문자만 (D-5 B).</li>
 *   <li>블로그 주인 본인은 조회수·방문자 모두 세지 않는다 (D-4 A).</li>
 * </ul>
 * 모든 예외를 잡아 기록만 하고 삼킨다: 세기가 실패해도 화면은 그대로 보인다. 기록에 사람을 알아볼 값은 넣지 않는다.
 */
@Component
public class ViewCountListener {

    private static final Logger log = LoggerFactory.getLogger(ViewCountListener.class);

    private final VisitorKeyResolver visitorKeys;
    private final ViewMarks marks;
    private final ViewRecorder recorder;
    private final ServiceDay serviceDay;

    public ViewCountListener(VisitorKeyResolver visitorKeys, ViewMarks marks, ViewRecorder recorder, ServiceDay serviceDay) {
        this.visitorKeys = visitorKeys;
        this.marks = marks;
        this.recorder = recorder;
        this.serviceDay = serviceDay;
    }

    @EventListener
    public void on(PostViewedEvent event) {
        if (isOwner(event.ownerId(), event.viewerId())) {
            return;
        }
        try {
            Optional<String> visitor = visitorKeys.resolve(event.viewerId());
            if (visitor.isEmpty()) {
                return;
            }
            LocalDate today = serviceDay.today();
            if (marks.firstView(event.postId(), visitor.get())) {
                recorder.view(event.postId(), event.blogId(), today);
            }
            countVisit(event.blogId(), today, visitor.get());
        } catch (RuntimeException e) {
            log.warn("조회수를 세지 못했습니다 (글 {}): {}", event.postId(), e.toString());
        }
    }

    @EventListener
    public void on(BlogVisitedEvent event) {
        if (isOwner(event.ownerId(), event.viewerId())) {
            return;
        }
        try {
            Optional<String> visitor = visitorKeys.resolve(event.viewerId());
            if (visitor.isPresent()) {
                countVisit(event.blogId(), serviceDay.today(), visitor.get());
            }
        } catch (RuntimeException e) {
            log.warn("방문자를 세지 못했습니다 (블로그 {}): {}", event.blogId(), e.toString());
        }
    }

    private void countVisit(Long blogId, LocalDate today, String visitor) {
        if (marks.firstVisit(blogId, today, visitor)) {
            recorder.visit(blogId, today);
        }
    }

    private static boolean isOwner(Long ownerId, Long viewerId) {
        return viewerId != null && viewerId.equals(ownerId);
    }
}
