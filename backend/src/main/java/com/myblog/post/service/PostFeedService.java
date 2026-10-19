package com.myblog.post.service;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.common.paging.PageNumbers;
import com.myblog.common.paging.PageNumbers.PageSlice;
import com.myblog.post.config.ExploreProperties;
import com.myblog.post.domain.Topic;
import com.myblog.post.repository.PostFeedRepository;
import com.myblog.post.repository.PostFeedRepository.FeedItem;
import com.myblog.post.repository.TopicRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 첫 화면 글 모음: 여러 블로그의 공개 글을 최신순으로, 주제별로 걸러 본다 (티스토리 첫 화면처럼).
 * 로그인했든 안 했든 결과는 같다 (검색과 같은 공개 조건). 숫자는 application.yml의 explore.feed.* 에서 읽는다.
 */
@Service
@Transactional(readOnly = true)
public class PostFeedService {

    private final PostFeedRepository feedRepository;
    private final TopicRepository topicRepository;
    private final PostPreview preview;
    private final ManuscriptMeasure manuscript;
    /** 마지막으로 센 첫 화면 머리 분량 (summary-ttl 동안 다시 쓴다). 여러 요청이 같이 읽으므로 volatile */
    private volatile CachedSummary cachedSummary;
    private final Object summaryLock = new Object();
    private final ExploreProperties.Feed settings;
    private final ExploreProperties.TodayIssue todayIssueSettings;
    /** 마지막으로 고른 오늘의 이슈 글 번호 (그날, today-issue.ttl 동안 다시 쓴다) */
    private volatile CachedIssue cachedIssue;
    private final Object issueLock = new Object();
    private final Clock clock;

    public PostFeedService(PostFeedRepository feedRepository, TopicRepository topicRepository, PostPreview preview,
            ManuscriptMeasure manuscript, ExploreProperties properties, Clock clock) {
        this.feedRepository = feedRepository;
        this.topicRepository = topicRepository;
        this.preview = preview;
        this.manuscript = manuscript;
        this.settings = properties.feed();
        this.todayIssueSettings = properties.todayIssue();
        this.clock = clock;
    }

    /**
     * @param topic 주제 이름 그대로 (없거나 비었으면 모든 주제). 정해진 주제가 아니면 400
     * @param page  요청한 페이지 번호 글자 그대로 (없으면 null)
     */
    public FeedView feed(String topic, String page) {
        List<Topic> topics = topicRepository.findAllByOrderBySortOrderAscIdAsc();
        Topic chosen = choose(topics, topic);
        Long topicId = chosen == null ? null : chosen.getId();
        long totalCount = feedRepository.count(topicId);
        PageSlice slice = PageNumbers.of(page, totalCount, settings.pageSize());
        List<FeedRow> rows = feedRepository.find(topicId, slice.offset(), slice.pageSize()).stream()
                .map(this::toRow)
                .toList();
        return new FeedView(chosen == null ? null : chosen.getName(), topics.stream().map(Topic::getName).toList(),
                totalCount, slice.page(), slice.totalPages(), slice.pageSize(), rows);
    }

    /**
     * 첫 화면 머리의 "원고지 표지" (v0.5.1): 공개 글을 쓴 블로그 수, 공개 글 수, 그 글들의 원고지 매수 합.
     * 글 모음과 같은 공개 규칙(공개 분류의 공개 글, 탈퇴하지 않은 회원)이고, 매수는 글마다 올림한 값을 더한다.
     */
    public SiteSummary summary() {
        // 세려면 공개 글 본문을 모두 읽으므로, summary-ttl 동안은 센 값을 다시 쓴다 (첫 화면마다 부른다)
        CachedSummary hit = freshCache();
        if (hit != null) {
            return hit.summary();
        }
        // 만료된 순간 여러 요청이 함께 와도 한 번만 센다: 잠근 뒤 다시 확인한다
        synchronized (summaryLock) {
            hit = freshCache();
            if (hit != null) {
                return hit.summary();
            }
            SiteSummary fresh = countSummary();
            cachedSummary = new CachedSummary(fresh, clock.instant());
            return fresh;
        }
    }

    /** 아직 summary-ttl 안이면 센 값, 아니면 null. */
    private CachedSummary freshCache() {
        CachedSummary hit = cachedSummary;
        return hit != null && hit.countedAt().plus(settings.summaryTtl()).isAfter(clock.instant()) ? hit : null;
    }

    private SiteSummary countSummary() {
        Set<Long> blogIds = new HashSet<>();
        long[] postCount = {0};
        long[] sheets = {0};
        feedRepository.forEachOpenPost((blogId, content) -> {
            blogIds.add(blogId);
            postCount[0]++;
            sheets[0] += manuscript.of(content).sheets();
        });
        return new SiteSummary(blogIds.size(), postCount[0], sheets[0]);
    }

    /** 첫 화면 머리의 분량: 쓴 사람(공개 글이 있는 블로그) 수, 공개 글 수, 원고지 매수 합. */
    public record SiteSummary(long bloggerCount, long postCount, long sheets) {
    }

    private record CachedSummary(SiteSummary summary, Instant countedAt) {
    }

    /**
     * 이번 주 인기 글: 최근 popular-days일(오늘 포함, 한국 날짜)의 조회수 합이 큰 공개 글 popular-size개.
     * 조회 기록이 없는 글끼리는 전체 조회수, 최신 순.
     */
    public PopularView popular(String topic) {
        Topic chosen = choose(topicRepository.findAllByOrderBySortOrderAscIdAsc(), topic);
        LocalDate today = LocalDate.now(clock.withZone(settings.dayZone()));
        LocalDate since = today.minusDays(settings.popularDays() - 1L);
        List<FeedRow> rows = feedRepository.findPopular(chosen == null ? null : chosen.getId(), since, today,
                settings.popularSize()).stream()
                .map(this::toRow)
                .toList();
        return new PopularView(chosen == null ? null : chosen.getName(), settings.popularDays(), rows);
    }

    /**
     * 오늘의 이슈 (specs/015 US2): 오늘을 포함한 최근 today-issue.days일(한국 날짜)의 조회수 합이 큰 공개 글, 블로그마다 하나.
     * 고른 글 번호는 그날 안에서 ttl 동안 다시 쓰고(S2-3), 카드는 요청마다 다시 읽어 볼 수 없게 된 글은 바로 뺀다(S2-1).
     */
    public TodayIssueView todayIssue() {
        Instant now = clock.instant();
        LocalDate today = LocalDate.ofInstant(now, settings.dayZone());
        return new TodayIssueView(today, rowsOf(todayIssueIds(today, now)));
    }

    private List<Long> todayIssueIds(LocalDate today, Instant now) {
        CachedIssue hit = cachedIssue;
        if (hit != null && hit.isFresh(today, now, todayIssueSettings.ttl())) {
            return hit.postIds();
        }
        synchronized (issueLock) {
            hit = cachedIssue;
            if (hit != null && hit.isFresh(today, now, todayIssueSettings.ttl())) {
                return hit.postIds();
            }
            List<Long> ids = List.copyOf(feedRepository.findTodayIssueIds(
                    today.minusDays(todayIssueSettings.days() - 1L), today, todayIssueSettings.size()));
            cachedIssue = new CachedIssue(today, now, ids);
            return ids;
        }
    }

    /** date는 고른 날(한국 날짜). posts는 많이 읽힌 순, 3개 이하. */
    public record TodayIssueView(LocalDate date, List<FeedRow> posts) {
    }

    private record CachedIssue(LocalDate day, Instant pickedAt, List<Long> postIds) {

        boolean isFresh(LocalDate today, Instant now, Duration ttl) {
            return day.equals(today) && pickedAt.plus(ttl).isAfter(now) && !now.isBefore(pickedAt);
        }
    }

    private static Topic choose(List<Topic> topics, String topic) {
        if (topic == null || topic.isBlank()) {
            return null;
        }
        String name = topic.strip();
        return topics.stream()
                .filter(t -> t.getName().equals(name))
                .findFirst()
                .orElseThrow(() -> new ApiException(ErrorCode.TOPIC_NOT_FOUND));
    }

    /** 정해진 글 번호들의 카드 한 줄, 번호 순서 그대로. 볼 수 없는 글은 빠진다 (비슷한 글, specs/014). */
    @Transactional(readOnly = true)
    public List<FeedRow> rowsOf(List<Long> postIds) {
        Map<Long, FeedItem> items = new HashMap<>();
        feedRepository.findOpenByIds(postIds).forEach(item -> items.put(item.postId(), item));
        return postIds.stream().map(items::get).filter(Objects::nonNull).map(this::toRow).toList();
    }

    private FeedRow toRow(FeedItem item) {
        ManuscriptMeasure.Measure measure = manuscript.of(item.content());
        return new FeedRow(item.postId(), item.blogId(), item.blogName(), item.nickname(), item.title(),
                item.topicName(), item.categoryId(), item.categoryName(), item.createdAt(), preview.of(item.content()),
                item.commentCount(), item.likeCount(), measure.sheets(), measure.readMinutes(),
                PostThumbnail.of(item.content()).orElse(null));
    }

    /** topic은 고른 주제 이름(모든 주제면 null), topics는 주제 탭에 쓸 다섯 개 이름(정해진 순서). */
    public record FeedView(String topic, List<String> topics, long totalCount, int page, int totalPages,
            int pageSize, List<FeedRow> posts) {
    }

    /** days는 인기 글을 고른 기간(일). */
    public record PopularView(String topic, int days, List<FeedRow> posts) {
    }

    /** 글 모음 한 줄. 항상 공개 글이다. thumbnail은 카드 썸네일 주소(없으면 null, {@link PostThumbnail}). */
    public record FeedRow(Long postId, Long blogId, String blogName, String nickname, String title, String topic,
            Long categoryId, String categoryName, Instant createdAt, String preview, long commentCount,
            long likeCount, int sheets, int readMinutes, String thumbnail) {
    }
}
