package com.myblog.post.service;

import com.myblog.common.error.ApiException;
import com.myblog.post.config.ShareProperties;
import com.myblog.post.repository.ShareRepository;
import com.myblog.post.repository.ShareRepository.PostStamp;
import com.myblog.post.repository.ShareRepository.SharedBlog;
import com.myblog.post.repository.ShareRepository.SharedPost;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.HtmlUtils;

/**
 * 로봇이 읽는 글결: 링크 미리보기 카드(OG), RSS, 사이트 지도, 검색엔진 안내 (specs/007 US4·US5·US6).
 * 사람이 보는 화면과 같은 공개 조건을 쓴다. 볼 수 없는 글·블로그는 내용 없이 기본 카드만 준다.
 * 주소는 모두 요청이 들어온 주소(base, 예: http://geulgyeol.java21.net)를 앞에 붙인 절대 주소다.
 */
@Service
@Transactional(readOnly = true)
public class ShareService {

    static final String SITE_NAME = "글결";
    static final String SITE_DESCRIPTION = "여러 사람이 쓰는 블로그를 한곳에서. 여행·음식·취미·운동·개발 이야기를 원고지에 담습니다.";
    static final String DEFAULT_IMAGE = "/og-default.png";

    /** 마크다운 본문의 첫 그림 주소: ![설명](주소) 또는 ![설명](<주소>) */
    private static final Pattern FIRST_IMAGE = Pattern.compile("!\\[[^\\]]*]\\(\\s*<?([^)\\s>]+)");
    private static final DateTimeFormatter RFC_1123 = DateTimeFormatter.RFC_1123_DATE_TIME;

    private final PostReadService postReadService;
    private final ShareRepository shareRepository;
    private final PostPreview preview;
    private final ShareProperties properties;

    public ShareService(PostReadService postReadService, ShareRepository shareRepository, PostPreview preview,
            ShareProperties properties) {
        this.postReadService = postReadService;
        this.shareRepository = shareRepository;
        this.preview = preview;
        this.properties = properties;
    }

    /** 첫 화면 카드. */
    public String homeCard(String base) {
        return card(new Card("website", SITE_NAME, SITE_DESCRIPTION, base + "/", base + DEFAULT_IMAGE, true));
    }

    /** 블로그 카드. 없거나 주인이 탈퇴한 블로그면 첫 화면 카드. 조회수·방문자는 세지 않는다. */
    public String blogCard(String base, long blogId) {
        return shareRepository.openBlog(blogId)
                .map(blog -> card(new Card("website", blog.name() + " · " + SITE_NAME, blogDescription(blog),
                        base + "/blog/" + blog.blogId(), base + DEFAULT_IMAGE, true)))
                .orElseGet(() -> homeCard(base));
    }

    /** 글 카드. 비공개 글·비공개 분류의 글·없는 글이면 첫 화면 카드 (글이 있다는 것도 드러내지 않는다). 조회수는 세지 않는다. */
    public String postCard(String base, long postId) {
        PostReadService.PostDetail post;
        try {
            post = postReadService.read(postId, null);
        } catch (ApiException e) {
            return homeCard(base);
        }
        String description = post.blogName() + " · " + preview.of(post.content());
        Optional<String> image = firstImage(base, post.content());
        return card(new Card("article", post.title(), description, base + "/posts/" + post.postId(),
                image.orElse(base + DEFAULT_IMAGE), image.isEmpty()));
    }

    /** 서비스 전체 RSS. */
    public String siteRss(String base) {
        return rss(base, SITE_NAME, base + "/", SITE_DESCRIPTION, base + "/rss.xml",
                shareRepository.latest(null, properties.rssSize()));
    }

    /** 블로그 RSS. 없거나 주인이 탈퇴한 블로그면 빈 값(404). */
    public Optional<String> blogRss(String base, long blogId) {
        return shareRepository.openBlog(blogId).map(blog -> rss(base, blog.name() + " · " + SITE_NAME,
                base + "/blog/" + blog.blogId(), blogDescription(blog), base + "/blog/" + blog.blogId() + "/rss.xml",
                shareRepository.latest(blog.blogId(), properties.rssSize())));
    }

    /** 사이트 지도: 첫 화면, 블로그, 최신 공개 글(고친 시각 포함). */
    public String sitemap(String base) {
        List<Long> blogIds = shareRepository.openBlogIds();
        List<PostStamp> posts = shareRepository.latestStamps(properties.sitemapMaxPosts());
        return xml(writer -> {
            writer.writeStartElement("urlset");
            writer.writeDefaultNamespace("http://www.sitemaps.org/schemas/sitemap/0.9");
            url(writer, base + "/", null);
            for (Long blogId : blogIds) {
                url(writer, base + "/blog/" + blogId, null);
            }
            for (PostStamp post : posts) {
                url(writer, base + "/posts/" + post.postId(), post.modifiedAt());
            }
            writer.writeEndElement();
        });
    }

    /** 검색엔진 안내: 회원 전용 화면과 서버 API는 빼고, 사이트 지도 주소를 알린다. */
    public String robots(String base) {
        return """
                User-agent: *
                Allow: /
                Disallow: /api/
                Disallow: /mypage
                Disallow: /manage
                Disallow: /me/
                Disallow: /write
                Disallow: /login
                Disallow: /signup
                Disallow: /search

                Sitemap: %s/sitemap.xml
                """.formatted(base);
    }

    private static String blogDescription(SharedBlog blog) {
        String intro = blog.intro() == null ? "" : blog.intro().strip();
        return intro.isEmpty() ? blog.name() + "의 글을 " + SITE_NAME + "에서 읽어 보세요." : intro;
    }

    /** 본문 첫 그림의 절대 주소. 이 사이트의 주소(/api/images/…)와 http(s) 주소만 쓴다. */
    static Optional<String> firstImage(String base, String markdown) {
        if (markdown == null) {
            return Optional.empty();
        }
        Matcher matcher = FIRST_IMAGE.matcher(markdown);
        if (!matcher.find()) {
            return Optional.empty();
        }
        String src = matcher.group(1);
        if (src.startsWith("https://") || src.startsWith("http://")) {
            return Optional.of(src);
        }
        if (src.startsWith("/") && !src.startsWith("//")) {
            return Optional.of(base + src);
        }
        return Optional.empty();
    }

    private static String card(Card card) {
        String title = escape(card.title());
        String description = escape(card.description());
        String url = escape(card.url());
        String image = escape(card.image());
        String imageSize = card.defaultImage()
                ? """
                  <meta property="og:image:width" content="1200">
                  <meta property="og:image:height" content="630">
                """
                : "";
        return """
                <!doctype html>
                <html lang="ko">
                <head>
                  <meta charset="utf-8">
                  <title>%1$s</title>
                  <meta name="description" content="%2$s">
                  <link rel="canonical" href="%3$s">
                  <meta property="og:type" content="%5$s">
                  <meta property="og:site_name" content="%6$s">
                  <meta property="og:locale" content="ko_KR">
                  <meta property="og:title" content="%1$s">
                  <meta property="og:description" content="%2$s">
                  <meta property="og:url" content="%3$s">
                  <meta property="og:image" content="%4$s">
                %7$s  <meta name="twitter:card" content="summary_large_image">
                  <meta name="twitter:title" content="%1$s">
                  <meta name="twitter:description" content="%2$s">
                  <meta name="twitter:image" content="%4$s">
                </head>
                <body>
                  <h1>%1$s</h1>
                  <p>%2$s</p>
                  <p><a href="%3$s">%3$s</a></p>
                </body>
                </html>
                """.formatted(title, description, url, image, card.type(), SITE_NAME, imageSize);
    }

    /** HTML 특수 문자(&amp; &lt; &gt; " ')만 바꾼다. 한글·가운뎃점 등은 UTF-8 글자 그대로 둔다. */
    private static String escape(String text) {
        return HtmlUtils.htmlEscape(text, StandardCharsets.UTF_8.name());
    }

    private String rss(String base, String title, String link, String description, String self, List<SharedPost> posts) {
        return xml(writer -> {
            writer.writeStartElement("rss");
            writer.writeAttribute("version", "2.0");
            writer.writeNamespace("atom", "http://www.w3.org/2005/Atom");
            writer.writeStartElement("channel");
            element(writer, "title", title);
            element(writer, "link", link);
            element(writer, "description", description);
            element(writer, "language", "ko");
            if (!posts.isEmpty()) {
                element(writer, "lastBuildDate", rfc1123(posts.get(0).createdAt()));
            }
            writer.writeEmptyElement("atom", "link", "http://www.w3.org/2005/Atom");
            writer.writeAttribute("href", self);
            writer.writeAttribute("rel", "self");
            writer.writeAttribute("type", "application/rss+xml");
            for (SharedPost post : posts) {
                String postLink = base + "/posts/" + post.postId();
                writer.writeStartElement("item");
                element(writer, "title", post.title());
                element(writer, "link", postLink);
                writer.writeStartElement("guid");
                writer.writeAttribute("isPermaLink", "true");
                writer.writeCharacters(postLink);
                writer.writeEndElement();
                element(writer, "pubDate", rfc1123(post.createdAt()));
                element(writer, "category", post.topicName());
                element(writer, "description", preview.of(post.content()));
                writer.writeEndElement();
            }
            writer.writeEndElement();
            writer.writeEndElement();
        });
    }

    private static void url(XMLStreamWriter writer, String loc, Instant modifiedAt) throws XMLStreamException {
        writer.writeStartElement("url");
        element(writer, "loc", loc);
        if (modifiedAt != null) {
            element(writer, "lastmod", DateTimeFormatter.ISO_OFFSET_DATE_TIME
                    .format(modifiedAt.truncatedTo(ChronoUnit.SECONDS).atOffset(ZoneOffset.UTC)));
        }
        writer.writeEndElement();
    }

    private static void element(XMLStreamWriter writer, String name, String text) throws XMLStreamException {
        writer.writeStartElement(name);
        writer.writeCharacters(text == null ? "" : text);
        writer.writeEndElement();
    }

    private static String rfc1123(Instant instant) {
        return RFC_1123.format(instant.atZone(ZoneOffset.UTC));
    }

    /** 글자를 XML 규칙대로 이스케이프하며 쓴다 (본문·제목에 &lt; &amp; 가 있어도 안전). */
    private static String xml(XmlBody body) {
        StringWriter out = new StringWriter();
        try {
            XMLStreamWriter writer = XMLOutputFactory.newFactory().createXMLStreamWriter(out);
            writer.writeStartDocument("UTF-8", "1.0");
            body.write(writer);
            writer.writeEndDocument();
            writer.close();
        } catch (XMLStreamException e) {
            throw new IllegalStateException("XML을 만들지 못했습니다", e);
        }
        return out.toString();
    }

    @FunctionalInterface
    private interface XmlBody {
        void write(XMLStreamWriter writer) throws XMLStreamException;
    }

    /** defaultImage는 글결 기본 이미지(크기를 알고 있음)를 쓰는지. */
    private record Card(String type, String title, String description, String url, String image, boolean defaultImage) {
    }
}
