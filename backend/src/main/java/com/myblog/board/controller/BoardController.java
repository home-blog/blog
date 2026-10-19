package com.myblog.board.controller;

import com.myblog.board.BoardPostViewedEvent;
import com.myblog.board.service.BoardCommentService;
import com.myblog.board.service.BoardCommentService.CommentList;
import com.myblog.board.service.BoardCommentService.CommentView;
import com.myblog.board.service.BoardService;
import com.myblog.board.service.BoardService.BoardDetail;
import com.myblog.board.service.BoardService.BoardInput;
import com.myblog.board.service.BoardService.BoardPage;
import com.myblog.board.service.BoardService.BoardRow;
import com.myblog.user.LoggedInMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 글마당 (specs/017 contracts). 읽기는 누구나(SecurityConfig), 쓰기는 로그인한 회원. 회원 번호는 LoggedInMember로만 얻는다.
 * 글을 읽으면 읽기 트랜잭션이 끝난 뒤 BoardPostViewedEvent를 낸다: 통계 모듈이 30분에 한 번만 조회 수를 올린다 (S1-2).
 */
@Tag(name = "15. 글마당")
@RestController
public class BoardController {

    private final LoggedInMember loggedInMember;
    private final BoardService boardService;
    private final BoardCommentService commentService;
    private final ApplicationEventPublisher events;

    public BoardController(LoggedInMember loggedInMember, BoardService boardService, BoardCommentService commentService,
            ApplicationEventPublisher events) {
        this.loggedInMember = loggedInMember;
        this.boardService = boardService;
        this.commentService = commentService;
        this.events = events;
    }

    @Operation(summary = "글마당 목록", description = "topic(주제 이름)·kind(talk·question)·unsolved(해결 전 물음만)·q(검색어, 단어마다 모두 들어 있는 글)로 고른다. 최신순, 한 쪽은 `board.page-size`. 없는 주제·종류는 400.")
    @GetMapping("/api/{version}/board/posts")
    public BoardPage list(@RequestParam(required = false) String topic, @RequestParam(required = false) String kind,
            @RequestParam(defaultValue = "false") boolean unsolved, @RequestParam(required = false) String q,
            @RequestParam(required = false) String page) {
        return boardService.list(topic, kind, unsolved, q, page);
    }

    @Operation(summary = "글마당 최근 글", description = "첫 화면용. `board.recent-size`개.")
    @GetMapping("/api/{version}/board/posts/recent")
    public RecentResponse recent() {
        return new RecentResponse(boardService.recent());
    }

    @Operation(summary = "글마당 글 읽기", description = "읽으면 조회 수를 센다(같은 사람 30분에 한 번, 쓴 사람은 빼고). 없으면 404.")
    @GetMapping("/api/{version}/board/posts/{boardPostId:\\d+}")
    public BoardDetail read(@PathVariable Long boardPostId, @Parameter(hidden = true) Authentication authentication) {
        Long viewerId = loggedInMember.idOf(authentication).orElse(null);
        BoardDetail detail = boardService.read(boardPostId, viewerId);
        events.publishEvent(new BoardPostViewedEvent(detail.boardPostId(), viewerId, detail.mine()));
        return detail;
    }

    @Operation(summary = "글마당 글 쓰기", description = "주제·종류(talk·question)·제목·본문. 같은 회원은 `board.post-interval` 간격으로만(429).")
    @PostMapping("/api/{version}/board/posts")
    public ResponseEntity<IdResponse> write(@RequestBody BoardInput input,
            @Parameter(hidden = true) Authentication authentication) {
        Long id = boardService.write(loggedInMember.requireIdOf(authentication), input);
        return ResponseEntity.status(HttpStatus.CREATED).body(new IdResponse(id));
    }

    @Operation(summary = "글마당 글 고치기", description = "쓴 사람만(403). 채택한 댓글이 있는 물음은 이야기로 바꿀 수 없다(400).")
    @PutMapping("/api/{version}/board/posts/{boardPostId:\\d+}")
    public IdResponse change(@PathVariable Long boardPostId, @RequestBody BoardInput input,
            @Parameter(hidden = true) Authentication authentication) {
        return new IdResponse(boardService.change(loggedInMember.requireIdOf(authentication), boardPostId, input));
    }

    @Operation(summary = "글마당 글 지우기", description = "쓴 사람과 관리자만. 댓글도 함께 지운다.")
    @DeleteMapping("/api/{version}/board/posts/{boardPostId:\\d+}")
    public ResponseEntity<Void> delete(@PathVariable Long boardPostId,
            @Parameter(hidden = true) Authentication authentication) {
        boardService.delete(loggedInMember.requireIdOf(authentication), boardPostId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "글마당 댓글 목록", description = "채택한 댓글이 맨 위, 나머지는 오래된 순. canAccept는 보는 사람이 이 물음을 쓴 사람인가.")
    @GetMapping("/api/{version}/board/posts/{boardPostId:\\d+}/comments")
    public CommentList comments(@PathVariable Long boardPostId, @Parameter(hidden = true) Authentication authentication) {
        return commentService.list(boardPostId, loggedInMember.idOf(authentication).orElse(null));
    }

    @Operation(summary = "글마당 댓글 쓰기", description = "1자 이상 `board.comment-max-length`자 이하. 같은 회원은 `board.comment-interval` 간격으로만(429).")
    @PostMapping("/api/{version}/board/posts/{boardPostId:\\d+}/comments")
    public ResponseEntity<CommentView> writeComment(@PathVariable Long boardPostId, @RequestBody CommentInput input,
            @Parameter(hidden = true) Authentication authentication) {
        CommentView view = commentService.write(loggedInMember.requireIdOf(authentication), boardPostId, input.content());
        return ResponseEntity.status(HttpStatus.CREATED).body(view);
    }

    @Operation(summary = "글마당 댓글 지우기", description = "쓴 사람과 관리자만. 채택된 댓글은 쓴 사람이 지울 수 없다(409, 관리자는 됨).")
    @DeleteMapping("/api/{version}/board/comments/{boardCommentId:\\d+}")
    public ResponseEntity<Void> deleteComment(@PathVariable Long boardCommentId,
            @Parameter(hidden = true) Authentication authentication) {
        commentService.delete(loggedInMember.requireIdOf(authentication), boardCommentId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "댓글 채택하기", description = "물음을 쓴 사람만(403). 이 물음에 다른 사람이 단 댓글만(400). 다른 댓글을 고르면 바뀐다.")
    @PutMapping("/api/{version}/board/posts/{boardPostId:\\d+}/accepted")
    public AcceptedResponse accept(@PathVariable Long boardPostId, @RequestBody AcceptInput input,
            @Parameter(hidden = true) Authentication authentication) {
        return new AcceptedResponse(commentService.accept(loggedInMember.requireIdOf(authentication), boardPostId,
                input.commentId()));
    }

    @Operation(summary = "채택 거두기", description = "물음을 쓴 사람만.")
    @DeleteMapping("/api/{version}/board/posts/{boardPostId:\\d+}/accepted")
    public AcceptedResponse unaccept(@PathVariable Long boardPostId,
            @Parameter(hidden = true) Authentication authentication) {
        return new AcceptedResponse(commentService.accept(loggedInMember.requireIdOf(authentication), boardPostId, null));
    }

    public record RecentResponse(List<BoardRow> posts) {
    }

    public record IdResponse(Long boardPostId) {
    }

    public record CommentInput(String content) {
    }

    /** commentId가 없으면 400 (거두기는 DELETE). */
    public record AcceptInput(Long commentId) {
    }

    public record AcceptedResponse(Long acceptedCommentId) {
    }
}
