package com.myblog.board.service;

import com.myblog.board.BoardAnswerAcceptedEvent;
import com.myblog.board.BoardCommentWrittenEvent;
import com.myblog.board.config.BoardProperties;
import com.myblog.board.domain.BoardComment;
import com.myblog.board.domain.BoardPost;
import com.myblog.board.repository.BoardCommentRepository;
import com.myblog.board.repository.BoardPostRepository;
import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.user.LoggedInMember;
import com.myblog.user.MemberNames;
import com.myblog.user.MemberNames.MemberName;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 글마당 댓글과 채택 도장 (specs/017 US3, US4).
 * <ul>
 *   <li>같은 회원의 댓글은 board.comment-interval 간격으로만 받는다 (S3-3, 회원마다 트랜잭션 잠금).</li>
 *   <li>댓글 수는 쓰기·지우기와 같은 트랜잭션에서 DB에서 바로 더하고 뺀다 (D-4).</li>
 *   <li>지우기는 쓴 사람과 관리자. 채택된 댓글은 쓴 사람이 지울 수 없고(409), 관리자가 지우면 채택도 풀린다 (S4-2).</li>
 *   <li>채택은 물음을 쓴 사람이 다른 사람의 댓글 하나에만. 글마다 한 줄로 잠가 두 채택이 겹치지 않게 한다.</li>
 *   <li>우편함 소식은 이벤트로만 알린다. 듣는 쪽(구독 모듈)이 커밋 뒤에 따로 만든다 (S3-4).</li>
 * </ul>
 */
@Service
public class BoardCommentService {

    private static final String MEMBER_LOCK_SQL = "select pg_advisory_xact_lock(hashtextextended(?, 0))";
    private static final String POST_LOCK_SQL = "select board_post_id from board_post where board_post_id = ? for update";
    private static final ResultSetExtractor<Void> IGNORE = rs -> null;

    private final BoardPostRepository posts;
    private final BoardCommentRepository comments;
    private final MemberNames memberNames;
    private final LoggedInMember loggedInMember;
    private final BoardProperties properties;
    private final JdbcTemplate jdbc;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    public BoardCommentService(BoardPostRepository posts, BoardCommentRepository comments, MemberNames memberNames,
            LoggedInMember loggedInMember, BoardProperties properties, JdbcTemplate jdbc, Clock clock,
            ApplicationEventPublisher events) {
        this.posts = posts;
        this.comments = comments;
        this.memberNames = memberNames;
        this.loggedInMember = loggedInMember;
        this.properties = properties;
        this.jdbc = jdbc;
        this.clock = clock;
        this.events = events;
    }

    /** writer는 탈퇴했으면 { id: null, nickname: null, withdrawn: true }. canDelete는 화면이 지우기 단추를 보일지만 정한다. */
    public record CommentView(Long boardCommentId, MemberName writer, String content, Instant createdAt, boolean accepted,
            boolean mine, boolean canDelete) {
    }

    public record CommentList(long count, Long acceptedCommentId, boolean canAccept, List<CommentView> comments) {
    }

    /** 채택한 댓글이 맨 위, 나머지는 오래된 순 (contracts 1). canAccept: 보는 사람이 이 물음을 쓴 사람인가. */
    @Transactional(readOnly = true)
    public CommentList list(Long boardPostId, Long viewerId) {
        BoardPost post = findPost(boardPostId);
        List<BoardComment> found = comments.findByBoardPostIdOrderByCreatedAtAscIdAsc(post.getId());
        Map<Long, MemberName> names = memberNames.namesOf(found.stream().map(BoardComment::getWriterId).distinct().toList());
        boolean admin = loggedInMember.isAdmin(viewerId);
        Long accepted = post.getAcceptedCommentId();
        List<CommentView> views = new ArrayList<>();
        found.stream().filter(c -> c.getId().equals(accepted)).forEach(c -> views.add(view(c, names, accepted, viewerId, admin)));
        found.stream().filter(c -> !c.getId().equals(accepted)).forEach(c -> views.add(view(c, names, accepted, viewerId, admin)));
        return new CommentList(views.size(), accepted, post.isQuestion() && post.isWrittenBy(viewerId), views);
    }

    @Transactional
    public CommentView write(Long memberId, Long boardPostId, String content) {
        BoardPost post = findPost(boardPostId);
        String body = content == null ? "" : content.replace("\r\n", "\n").replace('\r', '\n').strip();
        if (body.isEmpty()) {
            throw new ApiException(ErrorCode.COMMENT_EMPTY);
        }
        if (body.codePointCount(0, body.length()) > properties.commentMaxLength()) {
            throw new ApiException(ErrorCode.BOARD_COMMENT_TOO_LONG,
                    ErrorCode.BOARD_COMMENT_TOO_LONG.message().formatted(properties.commentMaxLength()));
        }
        jdbc.query(MEMBER_LOCK_SQL, IGNORE, "board-comment:" + memberId);
        Instant now = Instant.now(clock);
        Optional<Instant> last = comments.findLastCreatedAt(memberId);
        if (last.isPresent() && now.isBefore(last.get().plus(properties.commentInterval()))) {
            throw new ApiException(ErrorCode.BOARD_COMMENT_TOO_FAST);
        }
        BoardComment saved = comments.saveAndFlush(BoardComment.write(post.getId(), memberId, body, now));
        posts.adjustCommentCount(post.getId(), 1);
        Map<Long, MemberName> names = memberNames.namesOf(List.of(memberId));
        if (!post.isWrittenBy(memberId)) {
            events.publishEvent(new BoardCommentWrittenEvent(saved.getId(), post.getId(), post.getWriterId(),
                    post.getTitle(), nameOf(names.get(memberId))));
        }
        return view(saved, names, post.getAcceptedCommentId(), memberId, loggedInMember.isAdmin(memberId));
    }

    @Transactional
    public void delete(Long memberId, Long boardCommentId) {
        BoardComment comment = comments.findById(boardCommentId)
                .orElseThrow(() -> new ApiException(ErrorCode.BOARD_COMMENT_NOT_FOUND));
        boolean admin = loggedInMember.isAdmin(memberId);
        if (!comment.isWrittenBy(memberId) && !admin) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        jdbc.query(POST_LOCK_SQL, IGNORE, comment.getBoardPostId());
        BoardPost post = findPost(comment.getBoardPostId());
        if (comment.getId().equals(post.getAcceptedCommentId())) {
            if (!admin) {
                throw new ApiException(ErrorCode.BOARD_COMMENT_ACCEPTED);
            }
            post.accept(null);
            posts.flush();
        }
        comments.delete(comment);
        comments.flush();
        posts.adjustCommentCount(post.getId(), -1);
    }

    /**
     * 채택하기·바꾸기 (S4-1). commentId가 null이면 채택을 거둔다. 새로 채택했을 때만 댓글 쓴 사람에게 소식을 알린다.
     * @return 지금 채택한 댓글 번호 (거뒀으면 null)
     */
    @Transactional
    public Long accept(Long memberId, Long boardPostId, Long commentId) {
        jdbc.query(POST_LOCK_SQL, IGNORE, boardPostId);
        BoardPost post = findPost(boardPostId);
        if (!post.isWrittenBy(memberId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        if (!post.isQuestion()) {
            throw new ApiException(ErrorCode.BOARD_ACCEPT_INVALID);
        }
        if (commentId == null) {
            post.accept(null);
            posts.flush();
            return null;
        }
        BoardComment comment = comments.findById(commentId)
                .filter(c -> c.getBoardPostId().equals(post.getId()) && !c.isWrittenBy(memberId))
                .orElseThrow(() -> new ApiException(ErrorCode.BOARD_ACCEPT_INVALID));
        if (comment.getId().equals(post.getAcceptedCommentId())) {
            return comment.getId();
        }
        post.accept(comment.getId());
        posts.flush();
        MemberName accepter = memberNames.namesOf(List.of(memberId)).get(memberId);
        events.publishEvent(new BoardAnswerAcceptedEvent(post.getId(), comment.getWriterId(), post.getTitle(),
                nameOf(accepter)));
        return comment.getId();
    }

    private BoardPost findPost(Long boardPostId) {
        return posts.findById(boardPostId).orElseThrow(() -> new ApiException(ErrorCode.BOARD_POST_NOT_FOUND));
    }

    private static String nameOf(MemberName name) {
        return name == null || name.withdrawn() ? "탈퇴한 회원" : name.nickname();
    }

    private static CommentView view(BoardComment comment, Map<Long, MemberName> names, Long acceptedId, Long viewerId,
            boolean admin) {
        MemberName writer = names.getOrDefault(comment.getWriterId(), MemberName.withdrawnMember());
        if (writer.withdrawn()) {
            writer = MemberName.withdrawnMember();
        }
        boolean mine = comment.isWrittenBy(viewerId);
        boolean accepted = comment.getId().equals(acceptedId);
        // 채택된 댓글은 쓴 사람이 지울 수 없다(관리자는 된다, S4-2)
        return new CommentView(comment.getId(), writer, comment.getContent(), comment.getCreatedAt(), accepted, mine,
                admin || (mine && !accepted));
    }
}
