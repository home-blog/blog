package com.myblog.post.service;

import com.myblog.blog.BlogDirectory;
import com.myblog.blog.BlogDirectory.BlogInfo;
import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.post.config.WritingProperties;
import com.myblog.post.repository.PostFeedRepository;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 글쓰기 잔디 (specs/013 US1). 오늘(writing.activity.zone, 한국 시간) 포함 days일 동안 공개 글을 쓴 날을 센다.
 * 공개 분류의 공개 글만 세고 <b>주인이 봐도 같다</b> (D-13): 남에게 보이는 기록과 같아야 헷갈리지 않는다.
 * 매수는 글 읽기와 같은 계산(ManuscriptMeasure, 글마다 올림)이다. 진하기 단계는 화면이 정한다 (D-14).
 */
@Service
@Transactional(readOnly = true)
public class BlogActivityService {

    private final BlogDirectory blogDirectory;
    private final PostVisibility visibility;
    private final PostFeedRepository contents;
    private final ManuscriptMeasure manuscript;
    private final WritingProperties.Activity properties;
    private final Clock clock;

    public BlogActivityService(BlogDirectory blogDirectory, PostVisibility visibility, PostFeedRepository contents,
            ManuscriptMeasure manuscript, WritingProperties writing, Clock clock) {
        this.blogDirectory = blogDirectory;
        this.visibility = visibility;
        this.contents = contents;
        this.manuscript = manuscript;
        this.properties = writing.activity();
        this.clock = clock;
    }

    public Activity activity(Long blogId) {
        BlogInfo blog = blogDirectory.blog(blogId).orElseThrow(() -> new ApiException(ErrorCode.BLOG_NOT_FOUND));
        ZoneId zone = properties.zone();
        LocalDate to = LocalDate.ofInstant(clock.instant(), zone);
        LocalDate from = to.minusDays(properties.days() - 1L);
        // 방문자(null)의 범위 = 공개 분류. 주인이 봐도 같은 범위로 센다 (D-13)
        List<Long> categoryIds = visibility.listScope(blog.ownerId(), blogDirectory.categoriesOf(blogId), null).categoryIds();
        Map<LocalDate, long[]> byDate = new TreeMap<>();
        if (!categoryIds.isEmpty()) {
            contents.forEachPublicPostSince(categoryIds, from.atStartOfDay(zone).toInstant(), (createdAt, content) -> {
                LocalDate date = LocalDate.ofInstant(createdAt, zone);
                if (date.isAfter(to)) {
                    return; // 시계가 앞선 글(테스트·이전 데이터)은 세지 않는다
                }
                long[] counts = byDate.computeIfAbsent(date, d -> new long[2]);
                counts[0]++;
                counts[1] += manuscript.of(content).sheets();
            });
        }
        List<Day> days = byDate.entrySet().stream()
                .map(entry -> new Day(entry.getKey(), entry.getValue()[0], entry.getValue()[1]))
                .toList();
        long totalPosts = days.stream().mapToLong(Day::postCount).sum();
        long totalSheets = days.stream().mapToLong(Day::sheets).sum();
        return new Activity(blogId, from, to, days, totalPosts, totalSheets);
    }

    /** 잔디 응답 (contracts 1). 글이 있는 날만 날짜 순으로 담는다. */
    public record Activity(Long blogId, LocalDate from, LocalDate to, List<Day> days, long totalPosts, long totalSheets) {
    }

    public record Day(LocalDate date, long postCount, long sheets) {
    }
}
