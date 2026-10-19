package com.myblog.subscription.service;

import com.myblog.blog.BlogDirectory;
import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.post.PostPublishedEvent;
import com.myblog.subscription.config.SubscriptionProperties;
import com.myblog.subscription.mail.NewsletterMailer;
import com.myblog.subscription.repository.BlogSubscriptionRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 원고 편지 (specs/016 US3, S3-1 ~ S3-4): 새 공개 글이 올라오면 메일 받기를 켠 사람(탈퇴하지 않은)에게 한 통씩.
 * 하루 한도(daily-limit)는 Redis 날짜 키로 센다. 넘으면 그날은 더 보내지 않고 다음 날로 미루지 않는다(오래된 새 글 소식은 뜻이 없다).
 * 보내다 실패하면 다시 보내지 않고 기록만 남긴다. 받는 주소는 기록에 남기지 않는다.
 */
@Service
public class Newsletter {

    private static final Logger log = LoggerFactory.getLogger(Newsletter.class);
    private static final Pattern FENCE = Pattern.compile("(?s)```.*?```");
    private static final Pattern LINK = Pattern.compile("!?\\[([^\\]]*)]\\([^)]*\\)");
    private static final Pattern MARKS = Pattern.compile("[#*_`>~|]+");
    private static final Pattern SPACES = Pattern.compile("\\s+");
    private static final String RECIPIENTS = """
            select s.blog_subscription_id, u.email from blog_subscription s
            join users u on u.users_id = s.users_id and u.deleted_at is null
            where s.blog_id = ? and s.email_enabled and s.users_id <> ?
            order by s.blog_subscription_id
            """;

    private final JdbcTemplate jdbc;
    private final StringRedisTemplate redis;
    private final NewsletterMailer mailer;
    private final NewsletterTokens tokens;
    private final BlogSubscriptionRepository subscriptions;
    private final BlogDirectory blogDirectory;
    private final SubscriptionProperties.Newsletter settings;
    private final ZoneId dayZone;
    private final Clock clock;

    public Newsletter(JdbcTemplate jdbc, StringRedisTemplate redis, NewsletterMailer mailer, NewsletterTokens tokens,
            BlogSubscriptionRepository subscriptions, BlogDirectory blogDirectory, SubscriptionProperties properties,
            @Value("${stats.day-zone}") ZoneId dayZone, Clock clock) {
        this.jdbc = jdbc;
        this.redis = redis;
        this.mailer = mailer;
        this.tokens = tokens;
        this.subscriptions = subscriptions;
        this.blogDirectory = blogDirectory;
        this.settings = properties.newsletter();
        this.dayZone = dayZone;
        this.clock = clock;
    }

    private record Recipient(long subscriptionId, String email) {
    }

    /** 보낸 수. 메일을 끄거나(enabled, site-url 없음) 오늘 한도를 다 쓰면 0. */
    public int send(PostPublishedEvent event) {
        if (!settings.canSend()) {
            return 0;
        }
        List<Recipient> recipients = jdbc.query(RECIPIENTS,
                (rs, n) -> new Recipient(rs.getLong(1), rs.getString(2)), event.blogId(), event.ownerId());
        String subject = "[글결] " + event.blogName() + "에 새 원고: " + event.title();
        int sent = 0;
        for (Recipient recipient : recipients) {
            if (!takeDailySlot()) {
                log.warn("원고 편지: 오늘 한도({}통)를 다 써서 글 {}의 나머지 {}통은 보내지 않습니다", settings.dailyLimit(),
                        event.postId(), recipients.size() - sent);
                break;
            }
            try {
                mailer.send(recipient.email(), subject, body(event, recipient.subscriptionId()));
                sent++;
            } catch (RuntimeException e) {
                log.warn("원고 편지를 보내지 못했습니다 (글 {}, 받아 보기 {}): {}", event.postId(), recipient.subscriptionId(),
                        e.getClass().getSimpleName());
            }
        }
        return sent;
    }

    /** 그만 받기 (S3-3): 서명이 맞으면 그 받아 보기의 메일만 끈다. 이미 꺼졌거나 받아 보기가 없어도 같은 답. 블로그 이름(없으면 null). */
    @Transactional
    public String unsubscribe(String token) {
        long id = tokens.verify(token).orElseThrow(() -> new ApiException(ErrorCode.NEWSLETTER_TOKEN_INVALID));
        return subscriptions.findById(id)
                .map(subscription -> {
                    subscriptions.disableEmail(subscription.getId());
                    return blogDirectory.blog(subscription.getBlogId()).map(BlogDirectory.BlogInfo::name).orElse(null);
                })
                .orElse(null);
    }

    String body(PostPublishedEvent event, long subscriptionId) {
        String site = settings.siteUrl();
        return """
                %s에 새 원고가 올라왔습니다.

                %s
                %s

                읽으러 가기: %s/posts/%d

                ──────────
                이 메일은 글결에서 "%s" 블로그의 새 원고를 메일로도 받기로 한 분께 보냅니다.
                이 블로그의 원고 편지를 그만 받으려면: %s/letters/stop?token=%s
                """.formatted(event.blogName(), event.title(), summary(event.content()), site, event.postId(),
                event.blogName(), site, tokens.sign(subscriptionId));
    }

    /** 마크다운 기호·코드·링크 주소를 걷어 낸 앞부분 summary-length자. */
    String summary(String content) {
        String text = content == null ? "" : content;
        text = FENCE.matcher(text).replaceAll(" ");
        text = LINK.matcher(text).replaceAll("$1");
        text = MARKS.matcher(text).replaceAll("");
        text = SPACES.matcher(text).replaceAll(" ").strip();
        int limit = settings.summaryLength();
        if (text.codePointCount(0, text.length()) <= limit) {
            return text;
        }
        return text.substring(0, text.offsetByCodePoints(0, limit)) + "…";
    }

    /** 오늘(한국 날짜) 한 통 더 보낼 수 있으면 센다. Redis가 멈추면 보내지 않는다(한도를 지키는 쪽). */
    private boolean takeDailySlot() {
        try {
            String key = "newsletter:sent:" + LocalDate.ofInstant(clock.instant(), dayZone);
            Long count = redis.opsForValue().increment(key);
            if (count != null && count == 1) {
                redis.expire(key, Duration.ofDays(2));
            }
            return count != null && count <= settings.dailyLimit();
        } catch (RuntimeException e) {
            log.warn("원고 편지 한도를 셀 수 없어 보내지 않습니다: {}", e.getClass().getSimpleName());
            return false;
        }
    }
}
