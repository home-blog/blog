package com.myblog.guestbook.service;

import com.myblog.blog.BlogDirectory;
import com.myblog.blog.BlogDirectory.BlogInfo;
import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.common.paging.PageNumbers;
import com.myblog.common.paging.PageNumbers.PageSlice;
import com.myblog.guestbook.GuestbookRepliedEvent;
import com.myblog.guestbook.GuestbookWrittenEvent;
import com.myblog.guestbook.config.GuestbookProperties;
import com.myblog.guestbook.controller.dto.GuestbookRequests;
import com.myblog.guestbook.domain.GuestbookEntry;
import com.myblog.guestbook.domain.Stamp;
import com.myblog.guestbook.repository.GuestbookEntryRepository;
import com.myblog.user.MemberNames;
import com.myblog.user.MemberNames.MemberName;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 방명록 (specs/021 contracts 1 ~ 4).
 * <ul>
 *   <li>검사 순서: 없는 블로그(BLOG_NOT_FOUND) → 주인(GUESTBOOK_OWN_BLOG) → 입력(400) → 간격·하루 한도(429).</li>
 *   <li>같은 회원의 엽서는 트랜잭션 잠금(pg_advisory_xact_lock)으로 한 줄로 세운 뒤 마지막 엽서 시각과 오늘 엽서 수를 DB에서 센다:
 *       거의 동시에 온 요청도 하나만 통과한다 (research D-4, 댓글과 같은 방법).</li>
 *   <li>비밀 엽서는 쓴 사람과 블로그 주인만 읽는다. 남에게는 우표·날짜만 보이는 봉한 엽서(sealed)로 보낸다 (S1-5).</li>
 *   <li>답장·지우기는 블로그 주인(지우기는 쓴 사람도)만. 그 밖의 사람에게는 엽서가 있다는 것도 알리지 않고 404 (S2-1).
 *       엽서 줄을 잠그고 한다: 두 번 답해도 소식은 처음 한 번 (S2-2).</li>
 *   <li>소식은 이벤트로만 알린다. 구독 모듈이 커밋 뒤에 따로 만든다.</li>
 * </ul>
 */
@Service
public class GuestbookService {

    /** 회원마다 다른 잠금 번호. 다른 기능의 잠금과 겹치지 않게 이름을 붙여 해시한다. */
    private static final String MEMBER_LOCK_SQL = "select pg_advisory_xact_lock(hashtextextended(?, 0))";
    private static final ResultSetExtractor<Void> IGNORE = rs -> null;
    /** 봉한 엽서의 쓴 사람: 번호·닉네임 없이 (탈퇴 표시도 아님). */
    private static final MemberName HIDDEN = new MemberName(null, null, false);

    private final GuestbookEntryRepository entries;
    private final BlogDirectory blogs;
    private final MemberNames memberNames;
    private final GuestbookProperties properties;
    private final JdbcTemplate jdbc;
    private final ApplicationEventPublisher events;
    private final Clock clock;

    public GuestbookService(GuestbookEntryRepository entries, BlogDirectory blogs, MemberNames memberNames,
            GuestbookProperties properties, JdbcTemplate jdbc, ApplicationEventPublisher events, Clock clock) {
        this.entries = entries;
        this.blogs = blogs;
        this.memberNames = memberNames;
        this.properties = properties;
        this.jdbc = jdbc;
        this.events = events;
        this.clock = clock;
    }

    /**
     * reply·repliedAt은 답장이 없거나 봉한 엽서면 null. canWrite: 로그인했고 주인이 아니다.
     */
    public record EntryView(Long entryId, MemberName author, String body, String stamp, boolean secret,
            boolean sealed, Instant createdAt, String reply, Instant repliedAt, boolean canDelete, boolean canReply) {
    }

    public record EntryPage(long totalCount, int page, int totalPages, int pageSize, boolean canWrite,
            int bodyMaxLength, List<String> stamps, List<EntryView> items) {
    }

    /** 엽서 목록 (contracts 1): 최근 순. viewerId는 로그인하지 않았으면 null. */
    @Transactional(readOnly = true)
    public EntryPage list(Long blogId, Long viewerId, String page) {
        BlogInfo blog = blog(blogId);
        long total = entries.countByBlogId(blogId);
        PageSlice slice = PageNumbers.of(page, total, properties.pageSize());
        List<GuestbookEntry> found = entries.findByBlogIdOrderByCreatedAtDescIdDesc(blogId,
                PageRequest.of(slice.page() - 1, slice.pageSize())).getContent();
        Map<Long, MemberName> names = memberNames.namesOf(
                found.stream().map(GuestbookEntry::getWriterId).distinct().toList());
        List<EntryView> items = found.stream().map(entry -> view(entry, blog.ownerId(), viewerId, names)).toList();
        boolean canWrite = viewerId != null && !viewerId.equals(blog.ownerId());
        return new EntryPage(total, slice.page(), slice.totalPages(), slice.pageSize(), canWrite,
                properties.bodyMaxLength(), Stamp.codes(), items);
    }

    /** 엽서 쓰기 (contracts 2). */
    @Transactional
    public EntryView write(Long memberId, Long blogId, GuestbookRequests.Write request) {
        BlogInfo blog = blog(blogId);
        if (blog.ownerId().equals(memberId)) {
            throw new ApiException(ErrorCode.GUESTBOOK_OWN_BLOG);
        }
        String body = checkBody(request.body());
        Stamp stamp = Stamp.from(request.stamp()).orElseThrow(() -> new ApiException(ErrorCode.GUESTBOOK_STAMP_INVALID));
        jdbc.query(MEMBER_LOCK_SQL, IGNORE, "guestbook:" + memberId);
        Instant now = Instant.now(clock);
        Optional<Instant> last = entries.findLastCreatedAt(memberId);
        if (last.isPresent() && now.isBefore(last.get().plus(properties.minInterval()))) {
            throw new ApiException(ErrorCode.GUESTBOOK_TOO_FREQUENT);
        }
        Instant todayStart = LocalDate.ofInstant(now, properties.dayZone()).atStartOfDay(properties.dayZone())
                .toInstant();
        if (entries.countByWriterIdAndBlogIdAndCreatedAtGreaterThanEqual(memberId, blogId, todayStart)
                >= properties.dailyPerBlog()) {
            throw new ApiException(ErrorCode.GUESTBOOK_DAILY_LIMIT,
                    ErrorCode.GUESTBOOK_DAILY_LIMIT.message().formatted(properties.dailyPerBlog()));
        }
        GuestbookEntry saved = entries.saveAndFlush(GuestbookEntry.write(blogId, memberId, body, stamp,
                request.wantsSecret(), now));
        events.publishEvent(new GuestbookWrittenEvent(saved.getId(), blogId, blog.name(), blog.ownerId(), memberId));
        return view(saved, blog.ownerId(), memberId, memberNames.namesOf(List.of(memberId)));
    }

    /** 엽서 지우기 (contracts 3): 쓴 사람과 블로그 주인. 답장도 함께, 소식은 외래 키로 함께 지워진다. */
    @Transactional
    public void delete(Long memberId, Long entryId) {
        GuestbookEntry entry = entries.findForUpdate(entryId).orElseThrow(GuestbookService::entryNotFound);
        Long ownerId = ownerOf(entry);
        if (!memberId.equals(entry.getWriterId()) && !memberId.equals(ownerId)) {
            throw entryNotFound();
        }
        entries.delete(entry);
        entries.flush();
    }

    /** 답장 쓰기·고치기 (contracts 4): 블로그 주인만. 처음 쓸 때만 쓴 사람에게 소식을 보낸다. */
    @Transactional
    public EntryView reply(Long memberId, Long entryId, GuestbookRequests.Reply request) {
        GuestbookEntry entry = entries.findForUpdate(entryId).orElseThrow(GuestbookService::entryNotFound);
        BlogInfo blog = blog(entry.getBlogId());
        if (!memberId.equals(blog.ownerId())) {
            throw entryNotFound();
        }
        String body = checkBody(request.body());
        boolean first = entry.reply(body, Instant.now(clock));
        entries.flush();
        if (first) {
            events.publishEvent(new GuestbookRepliedEvent(entry.getId(), blog.blogId(), blog.name(), blog.ownerId(),
                    entry.getWriterId()));
        }
        return view(entry, blog.ownerId(), memberId, memberNames.namesOf(List.of(entry.getWriterId())));
    }

    /** 답장 지우기: 블로그 주인만. 답장이 없어도 그대로 끝난다. */
    @Transactional
    public void deleteReply(Long memberId, Long entryId) {
        GuestbookEntry entry = entries.findForUpdate(entryId).orElseThrow(GuestbookService::entryNotFound);
        if (!memberId.equals(ownerOf(entry))) {
            throw entryNotFound();
        }
        entry.clearReply();
        entries.flush();
    }

    private BlogInfo blog(Long blogId) {
        return blogs.blog(blogId).orElseThrow(() -> new ApiException(ErrorCode.BLOG_NOT_FOUND));
    }

    /** 엽서의 블로그 주인. 블로그가 닫히면 엽서도 함께 지워지므로 늘 있다 (없으면 없는 엽서로 본다). */
    private Long ownerOf(GuestbookEntry entry) {
        return blogs.blog(entry.getBlogId()).map(BlogInfo::ownerId).orElseThrow(GuestbookService::entryNotFound);
    }

    /** 앞뒤 공백을 지우고 1~bodyMaxLength자 (코드 포인트로 센다). */
    private String checkBody(String raw) {
        String body = GuestbookEntry.normalize(raw);
        if (body.isEmpty()) {
            throw new ApiException(ErrorCode.GUESTBOOK_BODY_EMPTY);
        }
        if (body.codePointCount(0, body.length()) > properties.bodyMaxLength()) {
            throw new ApiException(ErrorCode.GUESTBOOK_BODY_TOO_LONG,
                    ErrorCode.GUESTBOOK_BODY_TOO_LONG.message().formatted(properties.bodyMaxLength()));
        }
        return body;
    }

    private static ApiException entryNotFound() {
        return new ApiException(ErrorCode.GUESTBOOK_ENTRY_NOT_FOUND);
    }

    /** 봉한 엽서는 내용·쓴 사람·답장을 빼고 보낸다 (S1-5). 탈퇴한 사람은 번호·닉네임 없이 withdrawn만 참 (S1-6). */
    private static EntryView view(GuestbookEntry entry, Long ownerId, Long viewerId, Map<Long, MemberName> names) {
        boolean owner = viewerId != null && viewerId.equals(ownerId);
        boolean canDelete = viewerId != null && (owner || viewerId.equals(entry.getWriterId()));
        if (!entry.readableBy(viewerId, ownerId)) {
            return new EntryView(entry.getId(), HIDDEN, "", entry.getStamp(), true, true, entry.getCreatedAt(), null,
                    null, false, false);
        }
        MemberName author = names.getOrDefault(entry.getWriterId(), MemberName.withdrawnMember());
        return new EntryView(entry.getId(), author, entry.getBody(), entry.getStamp(), entry.isSecret(), false,
                entry.getCreatedAt(), entry.getReply(), entry.getRepliedAt(), canDelete, owner);
    }
}
