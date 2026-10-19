package com.myblog.board.service;

import com.myblog.board.config.BoardProperties;
import com.myblog.board.domain.BoardKind;
import com.myblog.board.domain.BoardPost;
import com.myblog.board.repository.BoardListRepository;
import com.myblog.board.repository.BoardListRepository.Filter;
import com.myblog.board.repository.BoardListRepository.Row;
import com.myblog.board.repository.BoardPostRepository;
import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.common.error.ErrorResponse.FieldErrorItem;
import com.myblog.common.paging.PageNumbers;
import com.myblog.common.paging.PageNumbers.PageSlice;
import com.myblog.post.TopicCatalog;
import com.myblog.post.TopicCatalog.TopicRef;
import com.myblog.user.LoggedInMember;
import com.myblog.user.MemberNames;
import com.myblog.user.MemberNames.MemberName;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 글마당 글 (specs/017 US1, US2, US5). 읽기는 누구나, 쓰기는 로그인한 회원.
 * <ul>
 *   <li>고치기는 쓴 사람만, 지우기는 쓴 사람과 관리자 (S2-2). 관리자 여부는 요청마다 DB에서 읽는다.</li>
 *   <li>같은 회원의 글은 board.post-interval 간격으로만 받는다. 회원마다 트랜잭션 잠금으로 줄을 세워 거의 동시에 온 요청도
 *       하나만 통과한다 (D-6, 블로그 댓글과 같은 방식).</li>
 *   <li>입력 검사는 칸마다 하고 틀린 칸을 모두 알린다 (공지사항과 같은 모양).</li>
 * </ul>
 */
@Service
public class BoardService {

    private static final String MEMBER_LOCK_SQL = "select pg_advisory_xact_lock(hashtextextended(?, 0))";
    /** 고치기·채택·댓글 지우기가 같은 글 줄을 잠그고 차례로 한다(채택 칸을 옛 값으로 덮어쓰지 않게) */
    private static final String POST_LOCK_SQL = "select board_post_id from board_post where board_post_id = ? for update";
    private static final ResultSetExtractor<Void> IGNORE = rs -> null;
    private static final Pattern BLANKS = Pattern.compile("\\p{javaWhitespace}+");

    private final BoardPostRepository posts;
    private final BoardListRepository lists;
    private final TopicCatalog topics;
    private final MemberNames memberNames;
    private final LoggedInMember loggedInMember;
    private final BoardProperties properties;
    private final JdbcTemplate jdbc;
    private final Clock clock;

    public BoardService(BoardPostRepository posts, BoardListRepository lists, TopicCatalog topics, MemberNames memberNames,
            LoggedInMember loggedInMember, BoardProperties properties, JdbcTemplate jdbc, Clock clock) {
        this.posts = posts;
        this.lists = lists;
        this.topics = topics;
        this.memberNames = memberNames;
        this.loggedInMember = loggedInMember;
        this.properties = properties;
        this.jdbc = jdbc;
        this.clock = clock;
    }

    /** 쓰기·고치기 입력 (contracts 2). */
    public record BoardInput(String topic, String kind, String title, String content) {
    }

    /** 목록 한 줄. writer는 탈퇴했으면 { id: null, nickname: null, withdrawn: true }. */
    public record BoardRow(Long boardPostId, String topic, String kind, String title, MemberName writer,
            Instant createdAt, long views, int commentCount, boolean solved) {

        static BoardRow of(Row row) {
            MemberName writer = row.withdrawn() ? MemberName.withdrawnMember()
                    : new MemberName(row.writerId(), row.nickname(), false);
            return new BoardRow(row.boardPostId(), row.topic(), row.kind(), row.title(), writer, row.createdAt(),
                    row.views(), row.commentCount(), row.solved());
        }
    }

    public record BoardPage(List<String> topics, String topic, String kind, boolean unsolved, String keyword,
            long totalCount, int page, int totalPages, int pageSize, List<BoardRow> posts) {
    }

    public record BoardDetail(Long boardPostId, String topic, String kind, String title, String content,
            MemberName writer, Instant createdAt, Instant updatedAt, long views, int commentCount,
            Long acceptedCommentId, boolean mine, boolean canDelete) {
    }

    /**
     * 목록 (contracts 1). topic·kind는 비어 있으면 모두. 없는 주제·종류는 400 (S1-4). q는 단어마다 모두 들어 있는 글(D-8).
     */
    @Transactional(readOnly = true)
    public BoardPage list(String topic, String kind, boolean unsolved, String q, String page) {
        List<TopicRef> all = topics.all();
        TopicRef chosenTopic = null;
        if (topic != null && !topic.isBlank()) {
            chosenTopic = all.stream().filter(t -> t.name().equals(topic)).findFirst()
                    .orElseThrow(() -> new ApiException(ErrorCode.BOARD_TOPIC_INVALID));
        }
        BoardKind chosenKind = null;
        if (kind != null && !kind.isBlank()) {
            chosenKind = BoardKind.from(kind).orElseThrow(() -> new ApiException(ErrorCode.BOARD_KIND_INVALID));
        }
        String keyword = q == null ? "" : q.strip();
        if (keyword.codePointCount(0, keyword.length()) > properties.keywordMaxLength()) {
            throw new ApiException(ErrorCode.SEARCH_KEYWORD_TOO_LONG,
                    ErrorCode.SEARCH_KEYWORD_TOO_LONG.message().formatted(properties.keywordMaxLength()));
        }
        Filter filter = new Filter(chosenTopic == null ? null : chosenTopic.id(),
                chosenKind == null ? null : chosenKind.value(), unsolved, patternsOf(keyword));
        long total = lists.count(filter);
        PageSlice slice = PageNumbers.of(page, total, properties.pageSize());
        List<BoardRow> rows = lists.find(filter, slice.offset(), slice.pageSize()).stream().map(BoardRow::of).toList();
        return new BoardPage(all.stream().map(TopicRef::name).toList(), chosenTopic == null ? null : chosenTopic.name(),
                chosenKind == null ? null : chosenKind.value(), unsolved, keyword.isEmpty() ? null : keyword, total,
                slice.page(), slice.totalPages(), slice.pageSize(), rows);
    }

    /** 첫 화면 글마당 최근 글 (US5). */
    @Transactional(readOnly = true)
    public List<BoardRow> recent() {
        return lists.find(Filter.none(), 0, properties.recentSize()).stream().map(BoardRow::of).toList();
    }

    @Transactional(readOnly = true)
    public BoardDetail read(Long boardPostId, Long viewerId) {
        BoardPost post = find(boardPostId);
        MemberName writer = memberNames.namesOf(List.of(post.getWriterId()))
                .getOrDefault(post.getWriterId(), MemberName.withdrawnMember());
        if (writer.withdrawn()) {
            writer = MemberName.withdrawnMember();
        }
        boolean mine = post.isWrittenBy(viewerId);
        boolean canDelete = mine || loggedInMember.isAdmin(viewerId);
        return new BoardDetail(post.getId(), topicName(post.getTopicId()), post.getKind(), post.getTitle(),
                post.getContent(), writer, post.getCreatedAt(), post.getUpdatedAt(), post.getViews(),
                post.getCommentCount(), post.getAcceptedCommentId(), mine, canDelete);
    }

    /** 쓴 사람 번호 (조회 수 이벤트에서 쓴 사람 자신을 뺄 때). 없는 글이면 404. */
    @Transactional(readOnly = true)
    public Long writerOf(Long boardPostId) {
        return find(boardPostId).getWriterId();
    }

    @Transactional
    public Long write(Long memberId, BoardInput input) {
        Checked checked = check(input);
        jdbc.query(MEMBER_LOCK_SQL, IGNORE, "board-post:" + memberId);
        Instant now = Instant.now(clock);
        Optional<Instant> last = posts.findLastCreatedAt(memberId);
        if (last.isPresent() && now.isBefore(last.get().plus(properties.postInterval()))) {
            throw new ApiException(ErrorCode.BOARD_POST_TOO_FAST);
        }
        BoardPost post = BoardPost.write(memberId, checked.topic().id(), checked.kind(), checked.title(),
                checked.content(), now);
        return posts.saveAndFlush(post).getId();
    }

    /** 고치기 (S2-2, S2-5). 남의 글은 입력을 보기 전에 403. */
    @Transactional
    public Long change(Long memberId, Long boardPostId, BoardInput input) {
        jdbc.query(POST_LOCK_SQL, IGNORE, boardPostId);
        BoardPost post = find(boardPostId);
        if (!post.isWrittenBy(memberId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        Checked checked = check(input);
        if (post.getAcceptedCommentId() != null && checked.kind() != BoardKind.QUESTION) {
            throw new ApiException(ErrorCode.BOARD_ACCEPTED_KIND_LOCKED);
        }
        post.change(checked.topic().id(), checked.kind(), checked.title(), checked.content(), Instant.now(clock));
        posts.flush();
        return post.getId();
    }

    /** 지우기 (S2-2, S2-4). 쓴 사람과 관리자만. 댓글도 함께 지운다. */
    @Transactional
    public void delete(Long memberId, Long boardPostId) {
        BoardPost post = find(boardPostId);
        if (!post.isWrittenBy(memberId) && !loggedInMember.isAdmin(memberId)) {
            throw new ApiException(ErrorCode.FORBIDDEN);
        }
        posts.clearAccepted(post.getId());
        posts.deletePost(post.getId());
    }

    private BoardPost find(Long boardPostId) {
        return posts.findById(boardPostId).orElseThrow(() -> new ApiException(ErrorCode.BOARD_POST_NOT_FOUND));
    }

    private String topicName(Long topicId) {
        Map<Long, String> names = topics.all().stream().collect(Collectors.toMap(TopicRef::id, TopicRef::name));
        return names.getOrDefault(topicId, "");
    }

    /** 공백으로 나눈 단어마다 "포함" 모양 (%단어%, ESCAPE '\'). 같은 단어는 하나로. */
    static List<String> patternsOf(String keyword) {
        Set<String> words = new LinkedHashSet<>();
        for (String word : BLANKS.split(keyword)) {
            if (!word.isEmpty()) {
                words.add(word);
            }
        }
        return words.stream().map(word -> "%" + escapeLike(word) + "%").toList();
    }

    static String escapeLike(String word) {
        return word.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }

    private record Checked(TopicRef topic, BoardKind kind, String title, String content) {
    }

    /** 칸마다 검사해 틀린 칸을 모두 알린다. 제목은 앞뒤 공백을 지우고, 본문은 줄바꿈만 맞추고 그대로 센다. */
    private Checked check(BoardInput input) {
        List<FieldErrorItem> errors = new ArrayList<>();
        Optional<TopicRef> topic = topics.byName(input.topic());
        if (topic.isEmpty()) {
            errors.add(item("topic", ErrorCode.BOARD_TOPIC_INVALID, ErrorCode.BOARD_TOPIC_INVALID.message()));
        }
        Optional<BoardKind> kind = BoardKind.from(input.kind());
        if (kind.isEmpty()) {
            errors.add(item("kind", ErrorCode.BOARD_KIND_INVALID, ErrorCode.BOARD_KIND_INVALID.message()));
        }
        String title = input.title() == null ? "" : input.title().strip();
        int titleLength = title.codePointCount(0, title.length());
        if (titleLength == 0) {
            errors.add(item("title", ErrorCode.TITLE_REQUIRED, ErrorCode.TITLE_REQUIRED.message()));
        } else if (titleLength > properties.titleMaxLength()) {
            errors.add(item("title", ErrorCode.BOARD_TITLE_TOO_LONG,
                    ErrorCode.BOARD_TITLE_TOO_LONG.message().formatted(properties.titleMaxLength())));
        }
        String content = input.content() == null ? "" : input.content().replace("\r\n", "\n").replace('\r', '\n');
        if (content.isBlank()) {
            errors.add(item("content", ErrorCode.CONTENT_REQUIRED, ErrorCode.CONTENT_REQUIRED.message()));
        } else if (content.codePointCount(0, content.length()) > properties.contentMaxLength()) {
            errors.add(item("content", ErrorCode.BOARD_CONTENT_TOO_LONG,
                    ErrorCode.BOARD_CONTENT_TOO_LONG.message().formatted(properties.contentMaxLength())));
        }
        if (!errors.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.message(), errors);
        }
        return new Checked(topic.orElseThrow(), kind.orElseThrow(), title, content);
    }

    private static FieldErrorItem item(String field, ErrorCode code, String message) {
        return new FieldErrorItem(field, code.name(), message);
    }
}
