package com.myblog.editorial.service;

import com.myblog.board.BoardReports;
import com.myblog.board.BoardReports.BoardBrief;
import com.myblog.board.BoardReports.BoardReportLine;
import com.myblog.board.BoardReports.PendingBoardPost;
import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.common.paging.PageNumbers;
import com.myblog.common.paging.PageNumbers.PageSlice;
import com.myblog.community.PostReports;
import com.myblog.community.PostReports.PendingPost;
import com.myblog.community.PostReports.ReportLine;
import com.myblog.editorial.EditorialNoticeEvent;
import com.myblog.editorial.config.EditorialProperties;
import com.myblog.editorial.domain.EditorialAction;
import com.myblog.editorial.repository.EditorialActionRepository;
import com.myblog.editorial.repository.EditorialSummaryRepository;
import com.myblog.post.PostModeration;
import com.myblog.post.PostModeration.PostBrief;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 편집실 (specs/018). 관리자 확인은 컨트롤러가 먼저 한다(LoggedInMember.requireAdminIdOf, S1-1).
 * <ul>
 *   <li>내림·반려·지우기는 대기 신고를 닫는 UPDATE(또는 지우기)가 먼저다: 두 관리자가 겹쳐도 하나만 닫고, 닫을 것이 없으면 409.</li>
 *   <li>내림은 글을 비공개로 돌리고 잠근다(PostModeration). 지우지 않는다 (D-2).</li>
 *   <li>쓴 사람에게 가는 우편함 소식은 이벤트로만 알린다. 듣는 쪽이 커밋 뒤에 따로 만든다 (S1-5).</li>
 * </ul>
 */
@Service
public class EditorialService {

    private final PostReports postReports;
    private final BoardReports boardReports;
    private final PostModeration moderation;
    private final EditorialActionRepository actions;
    private final EditorialSummaryRepository summary;
    private final EditorialProperties properties;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public EditorialService(PostReports postReports, BoardReports boardReports, PostModeration moderation,
            EditorialActionRepository actions, EditorialSummaryRepository summary, EditorialProperties properties,
            ApplicationEventPublisher events, Clock clock) {
        this.postReports = postReports;
        this.boardReports = boardReports;
        this.moderation = moderation;
        this.actions = actions;
        this.summary = summary;
        this.properties = properties;
        this.events = events;
        this.clock = clock;
    }

    public record Summary(long members, long membersToday, long publicPosts, long publicPostsToday, long boardPosts,
            long pendingReports) {
    }

    /** 블로그 글 검토 대기 한 줄. 글이 사라졌으면 title이 비어 있다(드물다: 글을 지우면 신고도 지워진다). */
    public record PendingPostRow(Long postId, Long blogId, String blogName, String title, String visibility,
            boolean locked, long reportCount, long spam, long abuse, long adult, long other, Instant firstAt,
            Instant lastAt) {
    }

    public record PendingBoardRow(Long boardPostId, String topic, String title, long reportCount, long spam,
            long abuse, long adult, long other, Instant firstAt, Instant lastAt) {
    }

    public record Pending<T>(long totalCount, int page, int totalPages, int pageSize, List<T> items) {
    }

    public record ReportView(String reason, String detail, Instant createdAt) {
    }

    public record PostReview(Long postId, String blogName, String title, String excerpt, String visibility,
            boolean locked, String lockReason, List<ReportView> reports) {
    }

    public record BoardReview(Long boardPostId, String topic, String title, String excerpt, List<ReportView> reports) {
    }

    public record ActionRow(Long actionId, String targetKind, Long postId, Long boardPostId, String title,
            String action, String reason, int reportCount, Instant createdAt) {
    }

    @Transactional(readOnly = true)
    public Summary summary() {
        Instant todayStart = LocalDate.ofInstant(Instant.now(clock), properties.dayZone()).atStartOfDay(properties.dayZone())
                .toInstant();
        return new Summary(summary.members(), summary.membersSince(todayStart), summary.publicPosts(),
                summary.publicPostsSince(todayStart), summary.boardPosts(),
                postReports.pendingReportCount() + boardReports.pendingReportCount());
    }

    @Transactional(readOnly = true)
    public Pending<PendingPostRow> pendingPosts(String page) {
        long total = postReports.pendingPostCount();
        PageSlice slice = PageNumbers.of(page, total, properties.pageSize());
        List<PendingPostRow> rows = postReports.pendingPosts(slice.offset(), slice.pageSize()).stream()
                .map(this::postRow).toList();
        return new Pending<>(total, slice.page(), slice.totalPages(), slice.pageSize(), rows);
    }

    @Transactional(readOnly = true)
    public Pending<PendingBoardRow> pendingBoardPosts(String page) {
        long total = boardReports.pendingPostCount();
        PageSlice slice = PageNumbers.of(page, total, properties.pageSize());
        List<PendingBoardRow> rows = boardReports.pendingPosts(slice.offset(), slice.pageSize()).stream()
                .map(this::boardRow).toList();
        return new Pending<>(total, slice.page(), slice.totalPages(), slice.pageSize(), rows);
    }

    @Transactional(readOnly = true)
    public PostReview reviewPost(Long postId) {
        PostBrief brief = moderation.brief(postId).orElseThrow(() -> new ApiException(ErrorCode.POST_NOT_FOUND));
        List<ReportView> reports = postReports.pendingOf(postId).stream().map(EditorialService::view).toList();
        return new PostReview(brief.postId(), brief.blogName(), brief.title(), brief.excerpt(), brief.visibility(),
                brief.locked(), brief.lockReason(), reports);
    }

    @Transactional(readOnly = true)
    public BoardReview reviewBoardPost(Long boardPostId) {
        BoardBrief brief = boardReports.brief(boardPostId)
                .orElseThrow(() -> new ApiException(ErrorCode.BOARD_POST_NOT_FOUND));
        List<ReportView> reports = boardReports.pendingOf(boardPostId).stream().map(EditorialService::view).toList();
        return new BoardReview(brief.boardPostId(), brief.topic(), brief.title(), brief.excerpt(), reports);
    }

    /** 내림 (US1-4): 대기 신고를 닫고, 비공개로 돌리고 잠그고, 기록을 남기고, 주인에게 알린다. */
    @Transactional
    public void takeDown(Long adminId, Long postId, String rawReason) {
        String reason = checkReason(rawReason);
        PostBrief brief = moderation.brief(postId).orElseThrow(() -> new ApiException(ErrorCode.POST_NOT_FOUND));
        Instant now = Instant.now(clock);
        int closed = postReports.resolve(postId, "taken_down", now);
        if (closed == 0) {
            throw new ApiException(ErrorCode.REPORT_ALREADY_RESOLVED);
        }
        moderation.lock(postId, reason, now);
        actions.save(EditorialAction.onPost(postId, brief.title(), "taken_down", reason, closed, adminId, now));
        events.publishEvent(new EditorialNoticeEvent(brief.ownerId(), postId, brief.title(), reason));
    }

    /** 반려 (US1-5): 대기 신고만 닫는다. */
    @Transactional
    public void dismissPost(Long adminId, Long postId) {
        PostBrief brief = moderation.brief(postId).orElseThrow(() -> new ApiException(ErrorCode.POST_NOT_FOUND));
        Instant now = Instant.now(clock);
        int closed = postReports.resolve(postId, "dismissed", now);
        if (closed == 0) {
            throw new ApiException(ErrorCode.REPORT_ALREADY_RESOLVED);
        }
        actions.save(EditorialAction.onPost(postId, brief.title(), "dismissed", null, closed, adminId, now));
    }

    /** 잠금 풀기 (US1-6). 공개로 되돌리지는 않는다. 잠기지 않은 글이면 아무 일도 없다. */
    @Transactional
    public void unlock(Long adminId, Long postId) {
        PostBrief brief = moderation.brief(postId).orElseThrow(() -> new ApiException(ErrorCode.POST_NOT_FOUND));
        if (!brief.locked()) {
            return;
        }
        moderation.unlock(postId);
        actions.save(EditorialAction.onPost(postId, brief.title(), "unlocked", null, 0, adminId, Instant.now(clock)));
    }

    /** 글마당 글 지우기 (US2-2): 대기 신고가 있어야 한다. 글·댓글·신고를 지우고 쓴 사람에게 알린다. */
    @Transactional
    public void removeBoardPost(Long adminId, Long boardPostId, String rawReason) {
        String reason = checkReason(rawReason);
        BoardBrief brief = boardReports.brief(boardPostId)
                .orElseThrow(() -> new ApiException(ErrorCode.BOARD_POST_NOT_FOUND));
        int pending = boardReports.pendingOf(boardPostId).size();
        if (pending == 0) {
            throw new ApiException(ErrorCode.REPORT_ALREADY_RESOLVED);
        }
        if (boardReports.remove(boardPostId) == 0) {
            throw new ApiException(ErrorCode.BOARD_POST_NOT_FOUND);
        }
        Instant now = Instant.now(clock);
        actions.save(EditorialAction.onBoard(null, brief.title(), "removed", reason, pending, adminId, now));
        events.publishEvent(new EditorialNoticeEvent(brief.writerId(), null, brief.title(), reason));
    }

    @Transactional
    public void dismissBoardPost(Long adminId, Long boardPostId) {
        BoardBrief brief = boardReports.brief(boardPostId)
                .orElseThrow(() -> new ApiException(ErrorCode.BOARD_POST_NOT_FOUND));
        Instant now = Instant.now(clock);
        int closed = boardReports.dismiss(boardPostId, now);
        if (closed == 0) {
            throw new ApiException(ErrorCode.REPORT_ALREADY_RESOLVED);
        }
        actions.save(EditorialAction.onBoard(boardPostId, brief.title(), "dismissed", null, closed, adminId, now));
    }

    @Transactional(readOnly = true)
    public Pending<ActionRow> history(String page) {
        long total = actions.count();
        PageSlice slice = PageNumbers.of(page, total, properties.pageSize());
        List<ActionRow> rows = actions.findAllByOrderByCreatedAtDescIdDesc(PageRequest.of(slice.page() - 1,
                        slice.pageSize())).stream()
                .map(a -> new ActionRow(a.getId(), a.getTargetKind(), a.getPostId(), a.getBoardPostId(),
                        a.getTargetTitle(), a.getAction(), a.getReason(), a.getReportCount(), a.getCreatedAt()))
                .toList();
        return new Pending<>(total, slice.page(), slice.totalPages(), slice.pageSize(), rows);
    }

    private PendingPostRow postRow(PendingPost p) {
        PostBrief brief = moderation.brief(p.postId()).orElse(null);
        return new PendingPostRow(p.postId(), brief == null ? null : brief.blogId(),
                brief == null ? null : brief.blogName(), brief == null ? null : brief.title(),
                brief == null ? null : brief.visibility(), brief != null && brief.locked(), p.reportCount(), p.spam(),
                p.abuse(), p.adult(), p.other(), p.firstAt(), p.lastAt());
    }

    private PendingBoardRow boardRow(PendingBoardPost p) {
        BoardBrief brief = boardReports.brief(p.boardPostId()).orElse(null);
        return new PendingBoardRow(p.boardPostId(), brief == null ? null : brief.topic(),
                brief == null ? null : brief.title(), p.reportCount(), p.spam(), p.abuse(), p.adult(), p.other(),
                p.firstAt(), p.lastAt());
    }

    private static ReportView view(ReportLine line) {
        return new ReportView(line.reason(), line.detail(), line.createdAt());
    }

    private static ReportView view(BoardReportLine line) {
        return new ReportView(line.reason(), line.detail(), line.createdAt());
    }

    /** 사유: 앞뒤 공백을 지우고 1~reasonMaxLength자. */
    private String checkReason(String raw) {
        String reason = raw == null ? "" : raw.replace("\r\n", "\n").strip();
        int length = reason.codePointCount(0, reason.length());
        if (length == 0 || length > properties.reasonMaxLength()) {
            throw new ApiException(ErrorCode.EDITORIAL_REASON_INVALID,
                    ErrorCode.EDITORIAL_REASON_INVALID.message().formatted(properties.reasonMaxLength()));
        }
        return reason;
    }
}
