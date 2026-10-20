package com.myblog.connect.service;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorResponse;
import com.myblog.connect.config.McpProperties;
import com.myblog.post.PostLibrary;
import com.myblog.post.PostLibrary.MyPostPage;
import com.myblog.post.PostLibrary.Passage;
import com.myblog.post.PostLibrary.PassageOptions;
import com.myblog.post.PostLibrary.PostDocument;
import com.myblog.post.PostLibrary.PostHit;
import com.myblog.post.PostLibrary.SavedDraft;
import com.myblog.stats.BlogStatsReader;
import com.myblog.stats.BlogStatsReader.BlogStats;
import com.myblog.user.AccessKeyGrant;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * 글결 MCP 도구 여섯 개 (specs/020 contracts 2). 결과는 사람이 읽을 글(content)과 같은 내용의 JSON(structuredContent)을 함께 준다.
 * 도구 안의 오류(없는 글, 검색어 길이, 임시 저장 한도)는 MCP 규칙대로 {@code isError: true} 결과로 돌려준다.
 * 주소는 절대 주소(https://…/posts/12)로 줘서 AI가 출처로 그대로 붙일 수 있게 한다.
 */
@Component
public class McpTools {

    private static final Logger log = LoggerFactory.getLogger(McpTools.class);
    private static final String STRING = "string";

    private final PostLibrary library;
    private final BlogStatsReader stats;
    private final McpProperties.Search search;
    private final ZoneId zone;

    public McpTools(PostLibrary library, BlogStatsReader stats, McpProperties properties,
            @Value("${stats.day-zone}") String dayZone) {
        this.library = library;
        this.stats = stats;
        this.search = properties.search();
        this.zone = ZoneId.of(dayZone);
    }

    /** 이번 요청의 회원·열쇠·사이트 주소. */
    public record Caller(Long memberId, AccessKeyGrant grant, String baseUrl) {
    }

    /** 이 열쇠로 쓸 수 있는 도구들 (읽기만 열쇠에는 save_draft가 없다, US3). */
    public List<Map<String, Object>> definitions(AccessKeyGrant grant) {
        List<Map<String, Object>> tools = new ArrayList<>();
        tools.add(tool("search_posts", "원고 찾기",
                "글결에서 글을 찾는다. 검색어의 모든 낱말이 제목이나 본문에 있는 글을 최근 순으로 돌려준다. "
                        + "공개 글과 사용자 자신의 글(비공개 포함)만 찾는다.",
                schema(Map.of(
                        "query", prop(STRING, "검색어 (2~50자, 띄어 쓴 낱말은 모두 들어 있어야 한다)"),
                        "mineOnly", prop("boolean", "참이면 사용자 자신의 블로그 글만"),
                        "limit", range("돌려받을 글 수", 1, search.maxPosts())), List.of("query")),
                true));
        tools.add(tool("search_passages", "단락 찾기 (RAG)",
                "질문에 답할 근거를 찾을 때 쓴다. 검색어와 가장 잘 맞는 단락을 글마다 최대 "
                        + search.passagesPerPost() + "개씩, 출처(글 제목·주소·몇 번째 단락)와 함께 돌려준다. "
                        + "답을 쓸 때는 돌려받은 단락만 근거로 삼고 출처 주소를 붙인다.",
                schema(Map.of(
                        "query", prop(STRING, "찾을 낱말들 (2~50자). 질문의 핵심 낱말을 띄어 써서 넣는다"),
                        "mineOnly", prop("boolean", "참이면 사용자 자신의 블로그 글에서만"),
                        "limit", range("돌려받을 단락 수", 1, search.maxPassages())), List.of("query")),
                true));
        tools.add(tool("read_post", "원고 읽기", "글 하나의 마크다운 원문과 제목·블로그·공개 범위·태그를 읽는다.",
                schema(Map.of("postId", range("글 번호 (search_posts·search_passages가 돌려준 postId)", 1, null)),
                        List.of("postId")),
                true));
        tools.add(tool("list_my_posts", "내 글 목록",
                "사용자 블로그의 글을 최근 순으로 본다. 비공개 글과 조회 수·댓글 수, 임시 저장 글 수가 함께 나온다.",
                schema(Map.of("page", range("쪽 번호 (1부터)", 1, null)), List.of()), true));
        tools.add(tool("my_blog_stats", "내 블로그 통계",
                "사용자 블로그의 오늘·어제·전체 조회 수와 방문자 수, 새 댓글 수, 최근 날짜별 흐름, 인기 글을 본다.",
                schema(Map.of(), List.of()), true));
        if (grant.canSaveDrafts()) {
            tools.add(tool("save_draft", "임시 저장",
                    "제목과 마크다운 본문을 사용자 블로그의 임시 저장 글로 넣는다. 발행하지 않는다: 사용자가 돌려받은 주소의 "
                            + "글쓰기 화면에서 읽고 고쳐서 직접 올린다.",
                    schema(Map.of(
                            "title", prop(STRING, "제목 (100자 이하)"),
                            "content", prop(STRING, "마크다운 본문")), List.of("title", "content")),
                    false));
        }
        return tools;
    }

    /** 도구가 없거나 이 열쇠로 쓸 수 없는 이름이면 비어 있다(-32602는 부르는 쪽이 정한다). */
    public Optional<Map<String, Object>> call(String name, Map<String, Object> args, Caller caller) {
        Function<Map<String, Object>, Map<String, Object>> tool = switch (name) {
            case "search_posts" -> a -> searchPosts(a, caller);
            case "search_passages" -> a -> searchPassages(a, caller);
            case "read_post" -> a -> readPost(a, caller);
            case "list_my_posts" -> a -> listMyPosts(a, caller);
            case "my_blog_stats" -> a -> myBlogStats(caller);
            case "save_draft" -> a -> saveDraft(a, caller);
            default -> null;
        };
        if (tool == null) {
            return Optional.empty();
        }
        if ("save_draft".equals(name) && !caller.grant().canSaveDrafts()) {
            return Optional.of(error("이 열쇠는 읽기만 할 수 있습니다. 임시 저장을 하려면 '읽기 + 임시 저장' 열쇠를 만들어 주세요."));
        }
        try {
            return Optional.of(tool.apply(args));
        } catch (BadArgument e) {
            return Optional.of(error(e.getMessage()));
        } catch (ApiException e) {
            return Optional.of(error(message(e.toResponse())));
        } catch (RuntimeException e) {
            log.warn("MCP 도구 {}를 실행하지 못했습니다: {}", name, e.toString());
            return Optional.of(error("※ 잠시 뒤 다시 시도해 주세요"));
        }
    }

    private Map<String, Object> searchPosts(Map<String, Object> args, Caller caller) {
        String query = requiredString(args, "query");
        int limit = intArg(args, "limit", 5, 1, search.maxPosts());
        List<PostHit> hits = library.search(caller.memberId(), query, boolArg(args, "mineOnly"), limit);
        List<Map<String, Object>> posts = new ArrayList<>();
        StringBuilder text = new StringBuilder(hits.isEmpty()
                ? "'" + query + "'에 맞는 글이 없습니다."
                : "'" + query + "'에 맞는 글 " + hits.size() + "개:\n");
        for (int i = 0; i < hits.size(); i++) {
            PostHit hit = hits.get(i);
            String url = caller.baseUrl() + hit.url();
            posts.add(ordered("postId", hit.postId(), "title", hit.title(), "blogName", hit.blogName(),
                    "createdAt", hit.createdAt().toString(), "visibility", hit.visibility(), "excerpt", hit.excerpt(),
                    "url", url));
            text.append('\n').append(i + 1).append(". ").append(hit.title()).append(" — ").append(hit.blogName())
                    .append(" (").append(day(hit.createdAt())).append(visibilityNote(hit.visibility())).append(")\n")
                    .append("   글 번호 ").append(hit.postId()).append(" · ").append(url).append('\n')
                    .append("   ").append(hit.excerpt()).append('\n');
        }
        return ok(text.toString().strip(), ordered("query", query, "posts", posts));
    }

    private Map<String, Object> searchPassages(Map<String, Object> args, Caller caller) {
        String query = requiredString(args, "query");
        int limit = intArg(args, "limit", 5, 1, search.maxPassages());
        List<Passage> found = library.passages(caller.memberId(), query, boolArg(args, "mineOnly"),
                new PassageOptions(search.candidates(), search.passageLength(), search.passagesPerPost(), limit));
        List<Map<String, Object>> passages = new ArrayList<>();
        StringBuilder text = new StringBuilder(found.isEmpty()
                ? "'" + query + "'와 맞는 단락이 없습니다. 다른 낱말로 찾아보세요."
                : "'" + query + "'와 맞는 단락 " + found.size() + "개입니다. 답에 인용할 때는 [제목](주소)를 출처로 붙이세요.\n");
        for (int i = 0; i < found.size(); i++) {
            Passage passage = found.get(i);
            String url = caller.baseUrl() + passage.url();
            passages.add(ordered("postId", passage.postId(), "title", passage.title(), "blogName", passage.blogName(),
                    "url", url, "paragraph", passage.paragraph(), "text", passage.text(),
                    "score", Math.round(passage.score() * 1000) / 1000.0));
            text.append("\n[").append(i + 1).append("] ").append(passage.title()).append(" — ").append(passage.blogName())
                    .append(", ").append(passage.paragraph()).append("번째 단락\n").append(url).append('\n')
                    .append("> ").append(passage.text().replace("\n", "\n> ")).append('\n');
        }
        return ok(text.toString().strip(), ordered("query", query, "passages", passages));
    }

    private Map<String, Object> readPost(Map<String, Object> args, Caller caller) {
        long postId = requiredLong(args, "postId");
        Optional<PostDocument> found = library.read(caller.memberId(), postId);
        if (found.isEmpty()) {
            return error("글 " + postId + "번을 찾을 수 없습니다 (없거나 볼 수 없는 글).");
        }
        PostDocument post = found.get();
        String url = caller.baseUrl() + post.url();
        String text = "# " + post.title() + "\n" + post.blogName() + " · " + day(post.createdAt())
                + visibilityNote(post.visibility()) + (post.tags().isEmpty() ? "" : " · 태그 " + String.join(", ", post.tags()))
                + "\n" + url + "\n\n---\n\n" + post.content();
        return ok(text, ordered("postId", post.postId(), "title", post.title(), "blogName", post.blogName(),
                "visibility", post.visibility(), "createdAt", post.createdAt().toString(), "tags", post.tags(),
                "url", url, "content", post.content()));
    }

    private Map<String, Object> listMyPosts(Map<String, Object> args, Caller caller) {
        int page = intArg(args, "page", 1, 1, Integer.MAX_VALUE / 100);
        MyPostPage result = library.myPosts(caller.memberId(), page);
        List<Map<String, Object>> posts = new ArrayList<>();
        StringBuilder text = new StringBuilder("내 글 " + result.totalCount() + "개 중 " + result.page() + "쪽, 임시 저장 "
                + result.draftCount() + "개\n");
        for (var post : result.posts()) {
            String url = caller.baseUrl() + post.url();
            posts.add(ordered("postId", post.postId(), "title", post.title(), "category", post.category(),
                    "visibility", post.visibility(), "createdAt", post.createdAt().toString(), "views", post.views(),
                    "commentCount", post.commentCount(), "url", url));
            text.append("\n- ").append(post.title()).append(" (").append(day(post.createdAt()))
                    .append(visibilityNote(post.visibility())).append(", 조회 ").append(post.views()).append(", 댓글 ")
                    .append(post.commentCount()).append(") 글 번호 ").append(post.postId());
        }
        return ok(text.toString().strip(), ordered("page", result.page(), "pageSize", result.pageSize(),
                "totalCount", result.totalCount(), "draftCount", result.draftCount(), "posts", posts));
    }

    private Map<String, Object> myBlogStats(Caller caller) {
        Optional<BlogStats> found = stats.of(caller.memberId());
        if (found.isEmpty()) {
            return error("블로그가 없습니다.");
        }
        BlogStats s = found.get();
        StringBuilder text = new StringBuilder()
                .append("조회 수: 오늘 ").append(s.views().today()).append(", 어제 ").append(s.views().yesterday())
                .append(", 전체 ").append(s.views().total()).append('\n')
                .append("방문자: 오늘 ").append(s.visitors().today()).append(", 어제 ").append(s.visitors().yesterday())
                .append(", 전체 ").append(s.visitors().total()).append('\n')
                .append("새 댓글: ").append(s.newCommentCount());
        if (!s.popularPosts().isEmpty()) {
            text.append("\n인기 글:");
            s.popularPosts().forEach(post -> text.append("\n- ").append(post.title()).append(" (조회 ")
                    .append(post.views()).append(") ").append(caller.baseUrl()).append("/posts/").append(post.postId()));
        }
        List<Map<String, Object>> days = s.lastDays().stream()
                .map(d -> ordered("date", d.date().toString(), "views", d.views(), "visitors", d.visitors())).toList();
        List<Map<String, Object>> popular = s.popularPosts().stream()
                .map(p -> ordered("postId", p.postId(), "title", p.title(), "views", p.views(),
                        "url", caller.baseUrl() + "/posts/" + p.postId()))
                .toList();
        return ok(text.toString(), ordered(
                "views", ordered("today", s.views().today(), "yesterday", s.views().yesterday(), "total", s.views().total()),
                "visitors", ordered("today", s.visitors().today(), "yesterday", s.visitors().yesterday(),
                        "total", s.visitors().total()),
                "newCommentCount", s.newCommentCount(), "lastDays", days, "popularPosts", popular));
    }

    private Map<String, Object> saveDraft(Map<String, Object> args, Caller caller) {
        String title = requiredString(args, "title");
        String content = requiredString(args, "content");
        SavedDraft saved = library.saveDraft(caller.memberId(), title, content);
        String url = caller.baseUrl() + saved.editUrl();
        return ok("임시 저장했습니다: '" + saved.title() + "'. 아직 발행하지 않았습니다. 사용자가 이 주소의 글쓰기 화면에서 "
                + "읽고 고친 뒤 직접 올립니다: " + url,
                ordered("draftId", saved.draftId(), "title", saved.title(), "editUrl", url));
    }

    private String day(Instant instant) {
        return instant.atZone(zone).toLocalDate().toString();
    }

    private static String visibilityNote(String visibility) {
        return "public".equals(visibility) ? "" : ", 비공개";
    }

    private static String message(ErrorResponse response) {
        StringBuilder text = new StringBuilder(response.message());
        if (response.fieldErrors() != null) {
            response.fieldErrors().forEach(item -> text.append(" / ").append(item.field()).append(": ").append(item.message()));
        }
        return text.toString();
    }

    /* 입력 꺼내기: 모양이 틀리면 도구 오류(isError)로 돌려준다 */

    private static String requiredString(Map<String, Object> args, String name) {
        if (!(args.get(name) instanceof String value)) {
            throw new BadArgument("'" + name + "' 값(글자)이 필요합니다.");
        }
        return value;
    }

    private static long requiredLong(Map<String, Object> args, String name) {
        if (!(args.get(name) instanceof Number value) || value.longValue() < 1) {
            throw new BadArgument("'" + name + "' 값(1 이상의 수)이 필요합니다.");
        }
        return value.longValue();
    }

    private static int intArg(Map<String, Object> args, String name, int fallback, int min, int max) {
        Object value = args.get(name);
        if (value == null) {
            return fallback;
        }
        if (!(value instanceof Number number) || number.longValue() < min || number.longValue() > max) {
            throw new BadArgument("'" + name + "'는 " + min + "~" + max + " 사이의 수여야 합니다.");
        }
        return number.intValue();
    }

    private static boolean boolArg(Map<String, Object> args, String name) {
        return Boolean.TRUE.equals(args.get(name));
    }

    /* 결과·정의 모양 */

    static Map<String, Object> ok(String text, Map<String, Object> structured) {
        return ordered("content", List.of(ordered("type", "text", "text", text)), "structuredContent", structured,
                "isError", false);
    }

    static Map<String, Object> error(String text) {
        return ordered("content", List.of(ordered("type", "text", "text", text)), "isError", true);
    }

    private static Map<String, Object> tool(String name, String title, String description, Map<String, Object> schema,
            boolean readOnly) {
        return ordered("name", name, "title", title, "description", description, "inputSchema", schema,
                "annotations", ordered("title", title, "readOnlyHint", readOnly, "destructiveHint", false,
                        "idempotentHint", readOnly, "openWorldHint", false));
    }

    private static Map<String, Object> schema(Map<String, Object> properties, List<String> required) {
        return ordered("type", "object", "properties", new LinkedHashMap<>(properties), "required", required,
                "additionalProperties", false);
    }

    private static Map<String, Object> prop(String type, String description) {
        return ordered("type", type, "description", description);
    }

    private static Map<String, Object> range(String description, int min, Integer max) {
        Map<String, Object> property = ordered("type", "integer", "description", description, "minimum", min);
        if (max != null) {
            property.put("maximum", max);
        }
        return property;
    }

    /** 순서를 지키는 Map (응답 JSON을 읽기 좋게). 키·값을 번갈아 넣는다. */
    static Map<String, Object> ordered(Object... keyValues) {
        Map<String, Object> map = new LinkedHashMap<>();
        for (int i = 0; i < keyValues.length; i += 2) {
            map.put((String) keyValues[i], keyValues[i + 1]);
        }
        return map;
    }

    /** 도구 입력이 틀렸다. */
    static final class BadArgument extends RuntimeException {

        BadArgument(String message) {
            super(message);
        }
    }
}
