package com.myblog.editorial.service;

import com.myblog.blog.BlogDirectory;
import com.myblog.blog.BlogDirectory.BlogInfo;
import com.myblog.comment.CommentReports;
import com.myblog.comment.CommentReports.CommentBrief;
import com.myblog.comment.CommentReports.PendingComment;
import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.common.paging.PageNumbers;
import com.myblog.common.paging.PageNumbers.PageSlice;
import com.myblog.editorial.EditorialNoticeEvent;
import com.myblog.editorial.config.EditorialProperties;
import com.myblog.editorial.domain.EditorialAction;
import com.myblog.editorial.repository.EditorialActionRepository;
import com.myblog.editorial.service.EditorialService.Pending;
import com.myblog.editorial.service.EditorialService.ReportView;
import com.myblog.post.PostModeration;
import com.myblog.post.PostModeration.PostBrief;
import com.myblog.user.MemberModeration;
import com.myblog.user.MemberModeration.MemberRow;
import com.myblog.user.MemberModeration.Suspension;
import com.myblog.user.MemberModeration.Unsuspension;
import com.myblog.user.MemberNames;
import com.myblog.user.MemberNames.MemberName;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 편집실 운영 강화 (specs/022): 댓글 신고 처리(US1)와 회원 정지(US2). 관리자 확인은 컨트롤러가 먼저 한다.
 * <ul>
 *   <li>댓글 처리는 댓글 줄을 잠그고 차례로 한다(신고의 자동 가림과도). 대기 신고를 닫는 UPDATE가 0이면 409.</li>
 *   <li>가리기는 쓴 사람의 우편함에 편집실 소식을 이벤트로 알린다. 제목은 "‘글 제목’에 남긴 댓글" (research D-8).</li>
 *   <li>반려는 저절로 가린 댓글만 되돌린다. 관리자가 가린 댓글은 처리 기록의 되돌리기로만 푼다.</li>
 *   <li>회원 정지·풀기는 회원 모듈(MemberModeration)이 하고, 여기서는 사유를 검사하고 기록한다.</li>
 * </ul>
 */
@Service
public class ModerationDeskService {

    private final CommentReports commentReports;
    private final PostModeration posts;
    private final MemberNames memberNames;
    private final MemberModeration members;
    private final BlogDirectory blogs;
    private final EditorialService editorial;
    private final EditorialActionRepository actions;
    private final EditorialProperties properties;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public ModerationDeskService(CommentReports commentReports, PostModeration posts, MemberNames memberNames,
            MemberModeration members, BlogDirectory blogs, EditorialService editorial, EditorialActionRepository actions,
            EditorialProperties properties, ApplicationEventPublisher events, Clock clock) {
        this.commentReports = commentReports;
        this.posts = posts;
        this.memberNames = memberNames;
        this.members = members;
        this.blogs = blogs;
        this.editorial = editorial;
        this.actions = actions;
        this.properties = properties;
        this.events = events;
        this.clock = clock;
    }

    /** 댓글 검토 대기 한 줄. 댓글·글이 사라졌으면 내용이 비어 있다(드물다: 지우면 신고도 지워진다). */
    public record PendingCommentRow(Long commentId, Long postId, String postTitle, String excerpt, Long writerId,
            String writerNickname, boolean hidden, String hiddenBy, long reportCount, long spam, long abuse,
            long adult, long other, Instant firstAt, Instant lastAt) {
    }

    public record CommentReview(Long commentId, Long postId, String postTitle, String body, String writerNickname,
            boolean hidden, String hiddenBy, List<ReportView> reports) {
    }

    /** 회원 한 줄. suspended는 지금 정지 중인가. blogId·blogName은 블로그가 없으면 비어 있다. */
    public record MemberLine(Long memberId, String nickname, Long blogId, String blogName, String role,
            Instant joinedAt, boolean suspended, Instant suspendedUntil, String suspendReason) {
    }

    public record MemberPage(long totalCount, int page, int totalPages, int pageSize, List<MemberLine> items,
            List<Integer> suspendDays) {
    }

    /* US1 댓글 */

    @Transactional(readOnly = true)
    public Pending<PendingCommentRow> pendingComments(String page) {
        long total = commentReports.pendingCommentCount();
        PageSlice slice = PageNumbers.of(page, total, properties.pageSize());
        List<PendingComment> pending = commentReports.pendingComments(slice.offset(), slice.pageSize());
        List<CommentBrief> briefs = pending.stream().map(p -> commentReports.brief(p.commentId()).orElse(null)).toList();
        Map<Long, MemberName> names = memberNames.namesOf(briefs.stream().filter(b -> b != null)
                .map(CommentBrief::writerId).distinct().toList());
        List<PendingCommentRow> rows = new ArrayList<>(pending.size());
        for (int i = 0; i < pending.size(); i++) {
            PendingComment p = pending.get(i);
            CommentBrief brief = briefs.get(i);
            String title = brief == null ? null : posts.brief(brief.postId()).map(PostBrief::title).orElse(null);
            rows.add(new PendingCommentRow(p.commentId(), brief == null ? null : brief.postId(), title,
                    brief == null ? null : brief.excerpt(), brief == null ? null : brief.writerId(),
                    brief == null ? null : nickname(names, brief.writerId()), brief != null && brief.hidden(),
                    brief == null ? null : brief.hiddenBy(), p.reportCount(), p.spam(), p.abuse(), p.adult(),
                    p.other(), p.firstAt(), p.lastAt()));
        }
        return new Pending<>(total, slice.page(), slice.totalPages(), slice.pageSize(), rows);
    }

    @Transactional(readOnly = true)
    public CommentReview reviewComment(Long commentId) {
        CommentBrief brief = commentReports.brief(commentId).orElseThrow(ModerationDeskService::commentNotFound);
        String title = posts.brief(brief.postId()).map(PostBrief::title).orElse(null);
        Map<Long, MemberName> names = memberNames.namesOf(List.of(brief.writerId()));
        List<ReportView> reports = commentReports.pendingOf(commentId).stream()
                .map(line -> new ReportView(line.reason(), line.detail(), line.createdAt())).toList();
        return new CommentReview(brief.commentId(), brief.postId(), title, brief.body(),
                nickname(names, brief.writerId()), brief.hidden(), brief.hiddenBy(), reports);
    }

    /** 가리기 (US1-5): 대기 신고를 닫고 가린다(이미 가렸으면 그대로). 쓴 사람에게 알린다. */
    @Transactional
    public void hideComment(Long adminId, Long commentId, String rawReason) {
        String reason = editorial.checkReason(rawReason);
        CommentBrief brief = lockedBrief(commentId);
        Instant now = Instant.now(clock);
        int closed = commentReports.resolve(commentId, "hidden", now);
        if (closed == 0) {
            throw new ApiException(ErrorCode.REPORT_ALREADY_RESOLVED);
        }
        commentReports.hide(commentId, now);
        String title = postTitle(brief);
        actions.save(EditorialAction.onComment(commentId, title, "hidden", reason, closed, adminId, now));
        events.publishEvent(new EditorialNoticeEvent(brief.writerId(), brief.postId(), "‘" + title + "’에 남긴 댓글",
                reason));
    }

    /** 반려 (US1-6): 대기 신고를 닫는다. 저절로 가린 댓글이면 되돌린다. */
    @Transactional
    public void dismissComment(Long adminId, Long commentId) {
        CommentBrief brief = lockedBrief(commentId);
        Instant now = Instant.now(clock);
        int closed = commentReports.resolve(commentId, "dismissed", now);
        if (closed == 0) {
            throw new ApiException(ErrorCode.REPORT_ALREADY_RESOLVED);
        }
        if ("auto".equals(brief.hiddenBy())) {
            commentReports.restore(commentId);
        }
        actions.save(EditorialAction.onComment(commentId, postTitle(brief), "dismissed", null, closed, adminId, now));
    }

    /** 되돌리기 (US1-7): 가려져 있을 때만 풀고 기록한다. @return 이번에 되돌렸는가 */
    @Transactional
    public boolean restoreComment(Long adminId, Long commentId) {
        CommentBrief brief = lockedBrief(commentId);
        if (!commentReports.restore(commentId)) {
            return false;
        }
        actions.save(EditorialAction.onComment(commentId, postTitle(brief), "restored", null, 0, adminId,
                Instant.now(clock)));
        return true;
    }

    /* US2 회원 */

    @Transactional(readOnly = true)
    public MemberPage searchMembers(String query, String page) {
        long total = members.count(query);
        PageSlice slice = PageNumbers.of(page, total, properties.pageSize());
        List<MemberLine> items = members.search(query, slice.offset(), slice.pageSize()).stream().map(this::line)
                .toList();
        return new MemberPage(total, slice.page(), slice.totalPages(), slice.pageSize(), items, members.suspendDays());
    }

    @Transactional
    public Suspension suspend(Long adminId, Long memberId, Integer days, String rawReason) {
        String reason = editorial.checkReason(rawReason);
        Suspension done = members.suspend(adminId, memberId, days == null ? 0 : days, reason);
        actions.save(EditorialAction.onMember(memberId, done.nickname(), "suspended", reason, adminId,
                Instant.now(clock)));
        return done;
    }

    /** @return 이번에 풀었는가 (정지 중이 아니었으면 거짓, 기록하지 않음) */
    @Transactional
    public boolean unsuspend(Long adminId, Long memberId) {
        Unsuspension done = members.unsuspend(memberId);
        if (done.unsuspended()) {
            actions.save(EditorialAction.onMember(memberId, done.nickname(), "unsuspended", null, adminId,
                    Instant.now(clock)));
        }
        return done.unsuspended();
    }

    private MemberLine line(MemberRow row) {
        BlogInfo blog = blogs.myBlog(row.memberId()).orElse(null);
        return new MemberLine(row.memberId(), row.nickname(), blog == null ? null : blog.blogId(),
                blog == null ? null : blog.name(), row.role(), row.joinedAt(), row.suspendedUntil() != null,
                row.suspendedUntil(), row.suspendReason());
    }

    private CommentBrief lockedBrief(Long commentId) {
        if (!commentReports.lock(commentId)) {
            throw commentNotFound();
        }
        return commentReports.brief(commentId).orElseThrow(ModerationDeskService::commentNotFound);
    }

    private String postTitle(CommentBrief brief) {
        return posts.brief(brief.postId()).map(PostBrief::title).orElse("");
    }

    private static String nickname(Map<Long, MemberName> names, Long writerId) {
        MemberName name = names.get(writerId);
        return name == null || name.withdrawn() ? null : name.nickname();
    }

    private static ApiException commentNotFound() {
        return new ApiException(ErrorCode.COMMENT_NOT_FOUND);
    }
}
