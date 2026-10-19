package com.myblog.community.service;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.common.paging.PageNumbers;
import com.myblog.common.paging.PageNumbers.PageSlice;
import com.myblog.community.config.ClipProperties;
import com.myblog.community.domain.SentenceClip;
import com.myblog.community.repository.SentenceClipRepository;
import com.myblog.post.PostLookup;
import com.myblog.post.PostLookup.PostText;
import com.myblog.user.ActiveMemberLock;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.regex.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 문장 수집과 필사 노트 (specs/015 US3, S3-1 ~ S3-8).
 * 검사 순서: 문장 길이(CLIP_TEXT_INVALID) → 볼 수 있는 글인가(아니면 POST_NOT_FOUND) → 그 글 본문에 있는가(CLIP_NOT_IN_POST)
 * → 회원 줄을 잠그고(탈퇴와 겹치지 않게, 한도를 동시에 넘지 않게) → 이미 모은 문장이면 그것을 → 한도(CLIP_LIMIT_EXCEEDED) → 넣기.
 * 본문과는 마크다운 기호·링크 주소·띄어쓰기를 걷어 내고 견준다: 화면에서 고른 글자에는 그런 것이 없다.
 */
@Service
@Transactional
public class SentenceClipService {

    private static final Pattern SPACES = Pattern.compile("\\p{javaWhitespace}+");
    /** [보이는 글자](주소), ![대체 글자](주소): 보이는 글자만 남긴다 */
    private static final Pattern LINK = Pattern.compile("!?\\[([^\\]]*)]\\([^)]*\\)");
    /** 견줄 때 빼는 것: 공백과 마크다운 기호 */
    private static final Pattern MARKS = Pattern.compile("[\\p{javaWhitespace}#*_`>~|\\[\\]()!\\\\-]+");

    private final SentenceClipRepository clips;
    private final PostLookup postLookup;
    private final ActiveMemberLock memberLock;
    private final ClipProperties properties;
    private final Clock clock;

    public SentenceClipService(SentenceClipRepository clips, PostLookup postLookup, ActiveMemberLock memberLock,
            ClipProperties properties, Clock clock) {
        this.clips = clips;
        this.postLookup = postLookup;
        this.memberLock = memberLock;
        this.properties = properties;
        this.clock = clock;
    }

    /** 원문 글. postId는 글이 지워졌으면 null, available은 지금 모은 사람이 그 글을 볼 수 있는가. */
    public record ClipPost(Long postId, String title, String blogName, boolean available) {
    }

    public record ClipView(Long clipId, String text, Instant createdAt, ClipPost post) {
    }

    /** created: 새로 모았으면 true, 이미 모은 문장이면 false. */
    public record Saved(ClipView clip, boolean created) {
    }

    public record ClipPage(List<ClipView> items, int page, int totalPages, long totalCount) {
    }

    public Saved collect(Long memberId, Long postId, String rawText) {
        String text = tidy(rawText);
        int length = text.codePointCount(0, text.length());
        if (length < 1 || length > properties.maxLength()) {
            throw new ApiException(ErrorCode.CLIP_TEXT_INVALID,
                    ErrorCode.CLIP_TEXT_INVALID.message().formatted(properties.maxLength()));
        }
        if (postId == null) {
            throw new ApiException(ErrorCode.POST_NOT_FOUND);
        }
        PostText post = postLookup.findVisibleText(postId, memberId)
                .orElseThrow(() -> new ApiException(ErrorCode.POST_NOT_FOUND));
        if (!appearsIn(text, post.content())) {
            throw new ApiException(ErrorCode.CLIP_NOT_IN_POST);
        }
        if (!memberLock.lockIfActive(memberId)) {
            // 이 요청이 기다리는 사이 탈퇴가 끝났다
            throw new ApiException(ErrorCode.UNAUTHENTICATED);
        }
        var existing = clips.findByMemberIdAndPostIdAndText(memberId, postId, text);
        if (existing.isPresent()) {
            return new Saved(view(existing.get(), true), false);
        }
        if (clips.countByMemberId(memberId) >= properties.maxPerMember()) {
            throw new ApiException(ErrorCode.CLIP_LIMIT_EXCEEDED,
                    ErrorCode.CLIP_LIMIT_EXCEEDED.message().formatted(properties.maxPerMember()));
        }
        clips.insertIfAbsent(memberId, postId, post.title(), post.blogName(), text, Instant.now(clock));
        SentenceClip saved = clips.findByMemberIdAndPostIdAndText(memberId, postId, text).orElseThrow();
        return new Saved(view(saved, true), true);
    }

    /** 내 필사 노트, 최근 순. page는 요청한 쪽 번호 글자 그대로 (범위를 넘으면 마지막 쪽). */
    @Transactional(readOnly = true)
    public ClipPage list(Long memberId, String page) {
        long total = clips.countByMemberId(memberId);
        PageSlice slice = PageNumbers.of(page, total, properties.pageSize());
        List<ClipView> items = clips.findByMemberIdOrderByCreatedAtDescIdDesc(memberId,
                        PageRequest.of(slice.page() - 1, slice.pageSize()))
                .stream()
                .map(clip -> view(clip, clip.getPostId() != null
                        && postLookup.findVisible(clip.getPostId(), memberId).isPresent()))
                .toList();
        return new ClipPage(items, slice.page(), slice.totalPages(), total);
    }

    public void remove(Long memberId, Long clipId) {
        if (clips.deleteByIdAndMemberId(clipId, memberId) == 0) {
            throw new ApiException(ErrorCode.CLIP_NOT_FOUND);
        }
    }

    /** 이 글에서 문장을 모은 사람 수. 글 주인만 본다(남이면 없는 글과 같은 404, S3-7). */
    @Transactional(readOnly = true)
    public long collectors(Long memberId, Long postId) {
        postLookup.findVisible(postId, memberId)
                .filter(post -> post.isOwnedBy(memberId))
                .orElseThrow(() -> new ApiException(ErrorCode.POST_NOT_FOUND));
        return clips.countCollectors(postId);
    }

    private static ClipView view(SentenceClip clip, boolean available) {
        return new ClipView(clip.getId(), clip.getText(), clip.getCreatedAt(),
                new ClipPost(clip.getPostId(), clip.getPostTitle(), clip.getBlogName(), available));
    }

    /** 앞뒤 공백을 지우고 줄바꿈·여러 칸 공백을 한 칸으로. */
    static String tidy(String text) {
        return text == null ? "" : SPACES.matcher(text.strip()).replaceAll(" ");
    }

    /** 고른 문장이 본문에 있는가: 둘 다 링크는 보이는 글자만 남기고, 공백과 마크다운 기호를 걷어 낸 뒤 견준다. */
    static boolean appearsIn(String text, String content) {
        String needle = bare(text);
        return !needle.isEmpty() && bare(LINK.matcher(content).replaceAll("$1")).contains(needle);
    }

    private static String bare(String text) {
        return MARKS.matcher(text).replaceAll("");
    }
}
