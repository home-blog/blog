package com.myblog.comment.service;

import com.myblog.blog.CommentReadMarks;
import com.myblog.comment.CommentWrittenEvent;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.context.ApplicationEventPublisher;
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
 *   <li>작성 시각은 그 블로그의 댓글 읽음 처리(specs/006)와 블로그 줄 잠금으로 줄을 선 뒤에 정한다 (R-4).</li>
 *   <li>삭제는 댓글 작성자와 그 글의 블로그 주인만. 그 밖의 사람에게는 없는 댓글과 같은 COMMENT_NOT_FOUND (contracts 3).</li>
 *   <li>답글·비밀 댓글 (specs/019): 답글은 한 단계(늘 원댓글을 가리킴), 비밀 댓글의 답글은 비밀. 읽을 수 없는 비밀 댓글은
 *       내용·쓴 사람을 빼고 보낸다(sealed). 답글이 남은 원댓글을 지우면 지운 칸으로 남고, 마지막 답글이 지워지면 함께 지운다.
 *       원댓글 줄을 잠가(답글 쓰기 FOR SHARE, 지우기 FOR UPDATE) 세기와 지우기가 엇갈리지 않게 한다 (research D-2).</li>
 * </ul>
 */
@Service
public class CommentService {

    /** 회원마다 다른 잠금 번호. 다른 기능의 잠금과 겹치지 않게 이름을 붙여 해시한다. */
    private static final String MEMBER_LOCK_SQL = "select pg_advisory_xact_lock(hashtextextended(?, 0))";
    private static final ResultSetExtractor<Void> IGNORE = rs -> null;
    /** 지운 칸·봉한 쪽지의 쓴 사람: 번호·닉네임 없이 (탈퇴 표시도 아님). */
    private static final MemberName HIDDEN = new MemberName(null, null, false);

    private final CommentRepository comments;
    private final PostLookup postLookup;
    private final MemberNames memberNames;
    private final CommentProperties properties;
    private final Validator validator;
    private final JdbcTemplate jdbc;
    private final CommentReadMarks readMarks;
    private final Clock clock;
    private final ApplicationEventPublisher events;

    public CommentService(CommentRepository comments, PostLookup postLookup, MemberNames memberNames,
            CommentProperties properties, Validator validator, JdbcTemplate jdbc, CommentReadMarks readMarks, Clock clock,
            ApplicationEventPublisher events) {
        this.comments = comments;
        this.postLookup = postLookup;
        this.memberNames = memberNames;
        this.properties = properties;
        this.validator = validator;
        this.jdbc = jdbc;
        this.readMarks = readMarks;
        this.clock = clock;
        this.events = events;
    }

    /**
     * 글의 댓글 (contracts 1, specs/019): 원댓글 오래된 순, 각 원댓글 바로 뒤에 그 답글 오래된 순.
     * viewerId는 로그인하지 않았으면 null. count는 지운 칸을 뺀다.
     */
    @Transactional(readOnly = true)
    public CommentList list(Long postId, Long viewerId) {
        PostRef post = visiblePost(postId, viewerId);
        List<Comment> found = comments.findByPostIdOldestFirst(post.postId());
        Map<Long, Comment> byId = new HashMap<>();
        Map<Long, List<Comment>> replies = new HashMap<>();
        for (Comment comment : found) {
            byId.put(comment.getId(), comment);
            if (comment.isReply()) {
                replies.computeIfAbsent(comment.getParentId(), id -> new ArrayList<>()).add(comment);
            }
        }
        List<Comment> ordered = new ArrayList<>(found.size());
        for (Comment comment : found) {
            if (!comment.isReply()) {
                ordered.add(comment);
                ordered.addAll(replies.getOrDefault(comment.getId(), List.of()));
            }
        }
        Map<Long, MemberName> names = memberNames.namesOf(found.stream().map(Comment::getMemberId).distinct().toList());
        List<CommentView> views = ordered.stream()
                .map(comment -> view(comment, comment.isReply() ? byId.get(comment.getParentId()) : null, names, post,
                        viewerId))
                .toList();
        long count = ordered.stream().filter(comment -> !comment.isDeleted()).count();
        return new CommentList(count, views);
    }

    /** 댓글·답글 쓰기 (contracts 2, specs/019 contracts 2). */
    @Transactional
    public CommentView write(Long memberId, Long postId, CommentRequests.Write request) {
        PostRef post = visiblePost(postId, memberId);
        Comment parent = request.parentId() == null ? null : replyTarget(request.parentId(), post, memberId);
        Set<ConstraintViolation<CommentRequests.Write>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
        jdbc.query(MEMBER_LOCK_SQL, IGNORE, "comment:" + memberId);
        // 확인한 뒤 잠그기 전에 원댓글이 지워졌거나 지운 칸이 되었거나, 가리킨 답글이 지워졌다.
        // 답글 지우기도 원댓글 줄을 FOR UPDATE로 잡으므로, 잠근 뒤 다시 보면 그사이 지워진 답글이 보인다
        if (parent != null && (comments.lockShared(parent.getId()).isEmpty()
                || !request.parentId().equals(parent.getId()) && !comments.existsById(request.parentId()))) {
            throw commentNotFound();
        }
        // 블로그 주인의 읽음 처리와 줄을 선 뒤에 작성 시각을 정한다: 새 댓글이 보지도 못하고 읽음이 되지 않게 (specs/006 R-4)
        readMarks.holdForComment(post.blogId());
        Instant now = Instant.now(clock);
        Optional<Instant> last = comments.findLastCreatedAt(memberId);
        if (last.isPresent() && now.isBefore(last.get().plus(properties.minInterval()))) {
            throw new ApiException(ErrorCode.COMMENT_TOO_FREQUENT);
        }
        boolean secret = request.wantsSecret() || parent != null && parent.isSecret();
        Comment saved = comments.saveAndFlush(Comment.write(post.postId(), memberId, request.body(),
                parent == null ? null : parent.getId(), secret, now));
        // 글 주인·원댓글 쓴 사람의 우편함에 소식 (specs/016 US2, specs/019 D-4). 듣는 쪽은 커밋 뒤에 따로 돈다
        events.publishEvent(new CommentWrittenEvent(saved.getId(), post.postId(), memberId,
                parent == null ? null : parent.getMemberId()));
        return view(saved, parent, memberNames.namesOf(List.of(memberId)), post, memberId);
    }

    /**
     * 답글을 달 원댓글 (specs/019 S1-1, S1-2, S2-2). 답글을 가리키면 그 원댓글로 바꾼다.
     * 없음·다른 글·지운 칸·읽을 수 없는 비밀 댓글은 모두 COMMENT_NOT_FOUND.
     */
    private Comment replyTarget(Long parentId, PostRef post, Long memberId) {
        Comment target = comments.findById(parentId).orElseThrow(CommentService::commentNotFound);
        Comment root = target.isReply()
                ? comments.findById(target.getParentId()).orElseThrow(CommentService::commentNotFound)
                : target;
        if (!root.getPostId().equals(post.postId()) || root.isDeleted()
                || !root.readableBy(memberId, post.isOwnedBy(memberId), null)
                || target.isReply() && !target.readableBy(memberId, post.isOwnedBy(memberId), root.getMemberId())) {
            throw commentNotFound();
        }
        return root;
    }

    /**
     * 댓글 삭제 (contracts 3, FR-004, research B-3, specs/019 D-2).
     * 그 글을 볼 수 없거나, 작성자도 블로그 주인도 아니면 댓글이 있다는 것도 알리지 않고 COMMENT_NOT_FOUND.
     * 원댓글 줄을 먼저 FOR UPDATE로 잠그고 다시 읽는다: 답글이 남았으면 지운 칸, 아니면 줄을 지운다.
     * 답글을 지워 원댓글이 빈 지운 칸이 되면 원댓글도 지운다.
     */
    @Transactional
    public void delete(Long memberId, Long commentId) {
        // 잠그기 전에는 엔티티를 읽지 않는다: 잠근 뒤 처음 읽어야 기다리는 사이 바뀐 내용을 본다
        Long rootId = comments.findRootIdOf(commentId).orElseThrow(CommentService::commentNotFound);
        comments.lockForUpdate(rootId).orElseThrow(CommentService::commentNotFound);
        Comment comment = comments.findById(commentId).filter(c -> !c.isDeleted())
                .orElseThrow(CommentService::commentNotFound);
        PostRef post = postLookup.findVisible(comment.getPostId(), memberId).orElseThrow(CommentService::commentNotFound);
        if (!memberId.equals(comment.getMemberId()) && !post.isOwnedBy(memberId)) {
            throw commentNotFound();
        }
        if (comment.isReply()) {
            comments.delete(comment);
            comments.flush();
            comments.findById(rootId)
                    .filter(root -> root.isDeleted() && comments.countByParentId(rootId) == 0)
                    .ifPresent(root -> {
                        comments.delete(root);
                        comments.flush();
                    });
        } else if (comments.countByParentId(comment.getId()) > 0) {
            comments.markDeleted(comment.getId(), Instant.now(clock));
        } else {
            comments.delete(comment);
            comments.flush();
        }
    }

    private static ApiException commentNotFound() {
        return new ApiException(ErrorCode.COMMENT_NOT_FOUND);
    }

    private PostRef visiblePost(Long postId, Long viewerId) {
        return postLookup.findVisible(postId, viewerId).orElseThrow(() -> new ApiException(ErrorCode.POST_NOT_FOUND));
    }

    /**
     * 화면에 보낼 댓글. 지운 칸과 읽을 수 없는 비밀 댓글은 내용·쓴 사람을 빼고 보낸다 (specs/019 S2-1).
     *
     * @param parent 답글이면 원댓글 (원댓글이면 null)
     */
    private static CommentView view(Comment comment, Comment parent, Map<Long, MemberName> names, PostRef post,
            Long viewerId) {
        boolean owner = viewerId != null && post.isOwnedBy(viewerId);
        Long parentWriterId = parent == null ? null : parent.getMemberId();
        boolean sealed = !comment.readableBy(viewerId, owner, parentWriterId);
        if (comment.isDeleted() || sealed) {
            // 지운 칸·봉한 쪽지에는 단추가 없다 (봉한 쪽지의 원댓글이 읽히면 답글은 원댓글의 단추로 단다)
            return new CommentView(comment.getId(), HIDDEN, "", comment.getCreatedAt(), false, comment.getParentId(),
                    comment.isSecret(), !comment.isDeleted(), comment.isDeleted(), false);
        }
        MemberName author = names.getOrDefault(comment.getMemberId(), MemberName.withdrawnMember());
        boolean canDelete = viewerId != null && (viewerId.equals(comment.getMemberId()) || owner);
        Comment root = parent == null ? comment : parent;
        return new CommentView(comment.getId(), author, comment.getBody(), comment.getCreatedAt(), canDelete,
                comment.getParentId(), comment.isSecret(), false, false, canReply(root, viewerId, owner));
    }

    /** 원댓글에 답글을 달 수 있나: 로그인했고, 지운 칸이 아니고, 읽을 수 있다. */
    private static boolean canReply(Comment root, Long viewerId, boolean owner) {
        return viewerId != null && !root.isDeleted() && root.readableBy(viewerId, owner, null);
    }

    public record CommentList(long count, List<CommentView> comments) {
    }

    /**
     * author는 탈퇴했으면 { id: null, nickname: null, withdrawn: true }. canDelete·canReply는 화면이 단추를 보일지만 정한다.
     * sealed(읽을 수 없는 비밀 댓글)와 deleted(지운 칸)이면 body는 빈 글자, author는 모두 빈 값이다 (specs/019 contracts 1).
     */
    public record CommentView(Long id, MemberName author, String body, Instant createdAt, boolean canDelete,
            Long parentId, boolean secret, boolean sealed, boolean deleted, boolean canReply) {
    }
}
