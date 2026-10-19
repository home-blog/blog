package com.myblog.post.service;

import com.myblog.blog.BlogDirectory;
import com.myblog.blog.BlogDirectory.CategoryInfo;
import com.myblog.post.config.RelatedProperties;
import com.myblog.post.domain.Post;
import com.myblog.post.related.Embedder;
import com.myblog.post.related.Vectors;
import com.myblog.post.repository.PostEmbeddingRepository;
import com.myblog.post.repository.PostEmbeddingRepository.Candidate;
import com.myblog.post.repository.PostEmbeddingRepository.Neighbor;
import com.myblog.post.repository.PostRepository;
import com.myblog.post.service.PostFeedService.FeedRow;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 이 글과 비슷한 글 (specs/014 US2, contracts 3).
 *
 * <ul>
 *   <li>뜻 정보가 있으면 뜻이 가까운 순(코사인 유사도, basis meaning). 없거나 AI가 꺼져 있으면 겹치는 태그 수 → 같은 주제 → 최신 순(basis tags)</li>
 *   <li>그 글 자신과 볼 수 없는 글은 뺀다. 한 블로그의 글은 per-blog개까지 섞는다(S2-2, S2-3)</li>
 *   <li>그 글을 볼 수 없으면 글 읽기와 같은 404(S2-1)</li>
 *   <li>고른 글 번호는 Redis에 cache-ttl 동안 둔다(요청마다 후보 벡터를 다 읽지 않게). 그 글의 뜻 정보가 바뀌면 지운다.
 *       번호만 두고 카드는 매번 공개 규칙으로 다시 읽으므로, 그사이 비공개가 된 글은 나오지 않는다.
 *       Redis가 멈춰도 보관만 건너뛰고 DB에서 골라 돌려준다(잃어도 되는 값, 헌법 1.1.0)</li>
 * </ul>
 */
@Service
public class RelatedPostService {

    private static final Logger log = LoggerFactory.getLogger(RelatedPostService.class);
    private static final Set<String> BASES = Set.of("meaning", "tags");

    private final PostRepository posts;
    private final BlogDirectory blogDirectory;
    private final PostVisibility visibility;
    private final PostEmbeddingRepository embeddings;
    private final Embedder embedder;
    private final PostFeedService feedService;
    private final RelatedProperties properties;
    private final StringRedisTemplate redis;

    public RelatedPostService(PostRepository posts, BlogDirectory blogDirectory, PostVisibility visibility,
            PostEmbeddingRepository embeddings, Embedder embedder, PostFeedService feedService, RelatedProperties properties,
            StringRedisTemplate redis) {
        this.posts = posts;
        this.blogDirectory = blogDirectory;
        this.visibility = visibility;
        this.embeddings = embeddings;
        this.embedder = embedder;
        this.feedService = feedService;
        this.properties = properties;
        this.redis = redis;
    }

    /** 이 글의 비슷한 글 번호를 두는 Redis 키. 값은 "basis|번호,번호". */
    public static String cacheKey(long postId) {
        return "related:" + postId;
    }

    /** basis: meaning(뜻) 또는 tags(태그·주제 대체). */
    public record RelatedView(String basis, List<FeedRow> posts) {
    }

    private record Pick(long postId, long blogId) {
    }

    @Transactional(readOnly = true)
    public RelatedView related(Long postId, Long viewerId) {
        Post post = posts.findById(postId).orElseThrow(PostReadService::notFound);
        CategoryInfo category = blogDirectory.category(post.getCategoryId()).orElseThrow(PostReadService::notFound);
        if (!visibility.canSee(post.getVisibility(), category, viewerId)) {
            throw PostReadService.notFound();
        }
        RelatedView cached = cached(postId);
        if (cached != null) {
            return cached;
        }
        List<Long> chosen = byMeaning(postId);
        String basis = "meaning";
        if (chosen.isEmpty()) {
            chosen = byTags(postId);
            basis = "tags";
        }
        keep(postId, basis, chosen);
        return new RelatedView(basis, feedService.rowsOf(chosen));
    }

    /**
     * 보관한 번호로 카드를 읽는다. 없거나, Redis를 읽지 못하거나, 값이 올바르지 않거나,
     * 그사이 볼 수 없게 된 글이 있으면 null(새로 고른다. 새로 고른 것으로 보관을 덮어쓴다).
     */
    private RelatedView cached(Long postId) {
        String value;
        try {
            value = redis.opsForValue().get(cacheKey(postId));
        } catch (RuntimeException e) {
            log.warn("비슷한 글 보관을 읽지 못했습니다 (글 {}): {}", postId, e.toString());
            return null;
        }
        if (value == null || !value.contains("|")) {
            return null;
        }
        String[] parts = value.split("\\|", 2);
        if (!BASES.contains(parts[0])) {
            log.warn("비슷한 글 보관 값이 올바르지 않습니다 (글 {}): {}", postId, value);
            return null;
        }
        List<Long> ids;
        try {
            ids = parts[1].isEmpty() ? List.of() : Arrays.stream(parts[1].split(",")).map(Long::valueOf).toList();
        } catch (NumberFormatException e) {
            log.warn("비슷한 글 보관 값이 올바르지 않습니다 (글 {}): {}", postId, value);
            return null;
        }
        List<FeedRow> rows = feedService.rowsOf(ids);
        return rows.size() == ids.size() ? new RelatedView(parts[0], rows) : null;
    }

    private void keep(Long postId, String basis, List<Long> chosen) {
        try {
            redis.opsForValue().set(cacheKey(postId),
                    basis + "|" + String.join(",", chosen.stream().map(String::valueOf).toList()), properties.cacheTtl());
        } catch (RuntimeException e) {
            log.warn("비슷한 글 보관을 쓰지 못했습니다 (글 {}): {}", postId, e.toString());
        }
    }

    private List<Long> byMeaning(Long postId) {
        if (!embedder.available()) {
            return List.of();
        }
        float[] source = embeddings.vectorOf(postId, embedder.model()).orElse(null);
        if (source == null) {
            return List.of();
        }
        List<Candidate> candidates = new ArrayList<>(
                embeddings.openCandidates(embedder.model(), postId, properties.maxCandidates()));
        Map<Long, Float> scores = new HashMap<>();
        candidates.forEach(candidate -> scores.put(candidate.postId(), Vectors.dot(source, candidate.embedding())));
        candidates.sort(Comparator.comparing((Candidate candidate) -> scores.get(candidate.postId())).reversed());
        return pick(candidates.stream().map(candidate -> new Pick(candidate.postId(), candidate.blogId())).toList());
    }

    private List<Long> byTags(Long postId) {
        int pool = properties.count() * properties.perBlog() * properties.tagPoolFactor();
        List<Neighbor> neighbors = embeddings.tagNeighbors(postId, pool);
        return pick(neighbors.stream()
                .filter(neighbor -> neighbor.shared() > 0 || neighbor.sameTopic())
                .map(neighbor -> new Pick(neighbor.postId(), neighbor.blogId()))
                .toList());
    }

    /** 앞에서부터 count개, 한 블로그는 per-blog개까지. */
    private List<Long> pick(List<Pick> ordered) {
        Map<Long, Integer> perBlog = new HashMap<>();
        List<Long> chosen = new ArrayList<>();
        for (Pick pick : ordered) {
            if (chosen.size() >= properties.count()) {
                break;
            }
            int taken = perBlog.getOrDefault(pick.blogId(), 0);
            if (taken >= properties.perBlog()) {
                continue;
            }
            perBlog.put(pick.blogId(), taken + 1);
            chosen.add(pick.postId());
        }
        return chosen;
    }
}
