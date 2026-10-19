package com.myblog.stats.service;

import com.myblog.board.BoardPostViewedEvent;
import com.myblog.board.BoardViewCounter;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * 글마당 글의 조회 수 (specs/017 S1-2, D-3). 블로그 글과 같은 방문자 판단(회원 번호·방문자 쿠키)과 30분 창으로 한 번만 센다.
 * 쓴 사람이 자기 글을 본 것은 세지 않는다. 세기가 실패해도 화면은 그대로 보인다(기록만 남긴다).
 */
@Component
public class BoardViewCountListener {

    private static final Logger log = LoggerFactory.getLogger(BoardViewCountListener.class);

    private final VisitorKeyResolver visitorKeys;
    private final ViewMarks marks;
    private final BoardViewCounter counter;

    public BoardViewCountListener(VisitorKeyResolver visitorKeys, ViewMarks marks, BoardViewCounter counter) {
        this.visitorKeys = visitorKeys;
        this.marks = marks;
        this.counter = counter;
    }

    @EventListener
    public void on(BoardPostViewedEvent event) {
        if (event.byWriter()) {
            return;
        }
        try {
            Optional<String> visitor = visitorKeys.resolve(event.viewerId());
            if (visitor.isPresent() && marks.firstBoardView(event.boardPostId(), visitor.get())) {
                counter.increment(event.boardPostId());
            }
        } catch (RuntimeException e) {
            log.warn("글마당 조회 수를 세지 못했습니다 (글 {}): {}", event.boardPostId(), e.toString());
        }
    }
}
