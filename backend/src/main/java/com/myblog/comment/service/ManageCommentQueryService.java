package com.myblog.comment.service;

import com.myblog.blog.BlogDirectory;
import com.myblog.blog.BlogDirectory.BlogInfo;
import com.myblog.blog.CommentReadMarks;
import com.myblog.blog.CommentReadMarks.ReadMark;
import com.myblog.comment.domain.Comment;
import com.myblog.comment.repository.CommentRepository;
import com.myblog.common.config.ManageProperties;
import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.post.PostSummaryQuery;
import com.myblog.user.MemberNames;
import com.myblog.user.MemberNames.MemberName;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 블로그 관리의 댓글 관리 (specs/006 contracts 5, T027, T034, research B-9, FR-023 ~ FR-029).
 * <ul>
 *   <li>내 블로그(세션의 회원)의 모든 글에 달린 댓글을 (작성 시각, 댓글 번호) 최신순, 한 쪽 manage.page-size개.</li>
 *   <li>앞부분은 서버가 코드 포인트 기준 manage.comment.preview-length자로 자른다. 화면은 글자 그대로 보여 준다.</li>
 *   <li>작성자는 MemberNames로 한 번에 (탈퇴했으면 번호·닉네임 없이 withdrawn).</li>
 *   <li>NEW 표시는 보여 주기에만 쓴다: newSince보다 늦고 주인이 쓰지 않은 댓글.</li>
 * </ul>
 */
@Service
@Transactional(readOnly = true)
public class ManageCommentQueryService {

    private final BlogDirectory blogDirectory;
    private final PostSummaryQuery postSummary;
    private final CommentReadMarks readMarks;
    private final CommentRepository comments;
    private final MemberNames memberNames;
    private final int pageSize;
    private final int previewLength;

    public ManageCommentQueryService(BlogDirectory blogDirectory, PostSummaryQuery postSummary, CommentReadMarks readMarks,
            CommentRepository comments, MemberNames memberNames, ManageProperties properties) {
        this.blogDirectory = blogDirectory;
        this.postSummary = postSummary;
        this.readMarks = readMarks;
        this.comments = comments;
        this.memberNames = memberNames;
        this.pageSize = properties.pageSize();
        this.previewLength = properties.comment().previewLength();
    }

    /**
     * @param page     1부터
     * @param newSince NEW 기준. {@link NewSince#NONE}이면 NEW 없음, {@link NewSince#NEVER}면 남이 쓴 모든 댓글이 NEW
     */
    public ManageCommentPage list(Long memberId, int page, NewSince newSince) {
        BlogInfo blog = myBlog(memberId);
        List<Long> postIds = postSummary.postIdsOf(blog.blogId());
        if (postIds.isEmpty()) {
            return new ManageCommentPage(List.of(), page, pageSize, 0);
        }
        long totalCount = comments.countByPostIdIn(postIds);
        List<Comment> found = comments.findByPostIdsNewestFirst(postIds, PageRequest.of(page - 1, pageSize));
        Map<Long, MemberName> names = memberNames.namesOf(found.stream().map(Comment::getMemberId).distinct().toList());
        Map<Long, String> titles = postSummary.titlesOf(found.stream().map(Comment::getPostId).distinct().toList());
        List<ManageCommentRow> items = found.stream().map(comment -> new ManageCommentRow(
                comment.getId(),
                author(names.getOrDefault(comment.getMemberId(), MemberName.withdrawnMember())),
                comment.getCreatedAt(),
                preview(comment.getBody()),
                new PostRef(comment.getPostId(), titles.get(comment.getPostId())),
                newSince.isNew(comment, blog.ownerId()))).toList();
        return new ManageCommentPage(items, page, pageSize, totalCount);
    }

    /** 댓글 관리를 열 때 먼저 부른다 (contracts 5-1). 읽음 처리 뒤에 달린 댓글은 다음번 새 댓글로 남는다 (R-4). */
    @Transactional
    public ReadMark markRead(Long memberId) {
        return readMarks.markRead(myBlog(memberId).blogId());
    }

    private BlogInfo myBlog(Long memberId) {
        return blogDirectory.myBlog(memberId).orElseThrow(() -> new ApiException(ErrorCode.BLOG_NOT_FOUND));
    }

    /** 앞 previewLength자 (코드 포인트, 이모지 하나 = 한 글자). */
    private String preview(String body) {
        if (body.codePointCount(0, body.length()) <= previewLength) {
            return body;
        }
        return body.substring(0, body.offsetByCodePoints(0, previewLength));
    }

    private static Author author(MemberName name) {
        return new Author(name.withdrawn() ? null : name.nickname(), name.withdrawn());
    }

    /** NEW 기준. 보여 주기에만 쓰고 권한과 상관없다. */
    public record NewSince(Instant since, boolean everything) {

        /** 기준 없음: NEW를 붙이지 않는다. */
        public static final NewSince NONE = new NewSince(null, false);

        /** 한 번도 연 적 없음: 남이 쓴 모든 댓글이 NEW. */
        public static final NewSince NEVER = new NewSince(null, true);

        public static NewSince after(Instant since) {
            return new NewSince(since, false);
        }

        boolean isNew(Comment comment, Long ownerId) {
            if (ownerId.equals(comment.getMemberId())) {
                return false;
            }
            return everything || since != null && comment.getCreatedAt().isAfter(since);
        }
    }

    public record ManageCommentPage(List<ManageCommentRow> items, int page, int pageSize, long totalCount) {
    }

    public record ManageCommentRow(Long commentId, Author author, Instant createdAt, String preview, PostRef post,
            boolean isNew) {
    }

    /** 탈퇴한 작성자는 nickname이 null이고 withdrawn이 참 (화면은 "탈퇴한 사용자"). */
    public record Author(String nickname, boolean withdrawn) {
    }

    public record PostRef(Long postId, String title) {
    }
}
