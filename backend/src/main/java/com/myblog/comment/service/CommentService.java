package com.myblog.comment.service;

import com.myblog.comment.config.CommentProperties;
import com.myblog.comment.controller.dto.CommentRequests;
import com.myblog.comment.domain.Comment;
import com.myblog.comment.repository.CommentRepository;
import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.post.PostLookup;
import com.myblog.post.PostLookup.PostRef;
import com.myblog.user.MemberNames;
import com.myblog.user.MemberNames.MemberName;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 댓글 쓰기·목록 (specs/005 contracts 1, 2, FR-001 ~ FR-003, FR-007, FR-008).
 * <ul>
 *   <li>볼 수 없는 글이면 없는 글과 같은 POST_NOT_FOUND (FR-029). 입력 검사는 그 <b>뒤에</b> 한다.</li>
 *   <li>같은 회원의 댓글은 min-interval(5초) 간격으로만 받는다. 마지막 댓글 시각을 DB에서 읽고, 같은 회원의 요청은
 *       트랜잭션 잠금(pg_advisory_xact_lock)으로 한 줄로 세워 거의 동시에 온 요청도 하나만 통과한다 (D-2, Redis 안 씀).</li>
 *   <li>작성자가 탈퇴했으면 번호·닉네임 없이 withdrawn만 참이다 (D-9).</li>
 * </ul>
 */
@Service
public class CommentService {

    /** 회원마다 다른 잠금 번호. 다른 기능의 잠금과 겹치지 않게 이름을 붙여 해시한다. */
    private static final String MEMBER_LOCK_SQL = "select pg_advisory_xact_lock(hashtextextended(?, 0))";
    private static final ResultSetExtractor<Void> IGNORE = rs -> null;

    private final CommentRepository comments;
    private final PostLookup postLookup;
    private final MemberNames memberNames;
    private final CommentProperties properties;
    private final Validator validator;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public CommentService(CommentRepository comments, PostLookup postLookup, MemberNames memberNames,
            CommentProperties properties, Validator validator, JdbcTemplate jdbc, Clock clock) {
        this.comments = comments;
        this.postLookup = postLookup;
        this.memberNames = memberNames;
        this.properties = properties;
        this.validator = validator;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** 글의 댓글, 오래된 순 (contracts 1). viewerId는 로그인하지 않았으면 null. */
    @Transactional(readOnly = true)
    public CommentList list(Long postId, Long viewerId) {
        PostRef post = visiblePost(postId, viewerId);
        List<Comment> found = comments.findByPostIdOldestFirst(post.postId());
        Map<Long, MemberName> names = memberNames.namesOf(found.stream().map(Comment::getMemberId).distinct().toList());
        List<CommentView> views = found.stream().map(comment -> view(comment, names, post, viewerId)).toList();
        return new CommentList(views.size(), views);
    }

    /** 댓글 쓰기 (contracts 2). */
    @Transactional
    public CommentView write(Long memberId, Long postId, CommentRequests.Write request) {
        PostRef post = visiblePost(postId, memberId);
        Set<ConstraintViolation<CommentRequests.Write>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        jdbc.query(MEMBER_LOCK_SQL, IGNORE, "comment:" + memberId);
        Instant now = Instant.now(clock);
        Optional<Instant> last = comments.findLastCreatedAt(memberId);
        if (last.isPresent() && now.isBefore(last.get().plus(properties.minInterval()))) {
            throw new ApiException(ErrorCode.COMMENT_TOO_FREQUENT);
        }
        Comment saved = comments.saveAndFlush(Comment.write(post.postId(), memberId, request.body(), now));
        return view(saved, memberNames.namesOf(List.of(memberId)), post, memberId);
    }

    private PostRef visiblePost(Long postId, Long viewerId) {
        return postLookup.findVisible(postId, viewerId).orElseThrow(() -> new ApiException(ErrorCode.POST_NOT_FOUND));
    }

    private static CommentView view(Comment comment, Map<Long, MemberName> names, PostRef post, Long viewerId) {
        MemberName author = names.getOrDefault(comment.getMemberId(), MemberName.withdrawnMember());
        boolean canDelete = viewerId != null && (viewerId.equals(comment.getMemberId()) || post.isOwnedBy(viewerId));
        return new CommentView(comment.getId(), author, comment.getBody(), comment.getCreatedAt(), canDelete);
    }

    public record CommentList(long count, List<CommentView> comments) {
    }

    /** author는 탈퇴했으면 { id: null, nickname: null, withdrawn: true }. canDelete는 화면이 삭제 버튼을 보일지만 정한다. */
    public record CommentView(Long id, MemberName author, String body, Instant createdAt, boolean canDelete) {
    }
}
