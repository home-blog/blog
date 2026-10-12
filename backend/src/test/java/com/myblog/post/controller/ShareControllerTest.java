package com.myblog.post.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import com.myblog.support.TestPosts;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import javax.xml.parsers.DocumentBuilderFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;
import org.w3c.dom.Document;

/**
 * 로봇이 읽는 주소 (specs/007 US4·US5·US6): 링크 미리보기 카드, RSS, 사이트 지도, 검색엔진 안내.
 * 모두 로그인 없이 열리고, 비공개 글은 내용을 드러내지 않는다. 글은 다른 데이터보다 최신이 되게 먼 미래 시각으로 만든다.
 */
@SpringBootTest
@Transactional
@Import({TestMembers.class, TestPosts.class})
class ShareControllerTest {

    private static final Instant FUTURE = Instant.parse("2100-01-01T00:00:00Z");
    private static final String BASE = "http://localhost";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private TestPosts posts;

    private MockMvc mvc;
    private Member writer;
    private Long open;
    private Long secret;

    @BeforeEach
    void setUp() {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        writer = members.register("share");
        open = posts.add(writer.defaultCategoryId(), "R&D <노트> \"첫\" 글",
                "## 소제목\n\n![표지](/api/images/7) 본문 **첫 줄**입니다.", "public", FUTURE.plus(2, ChronoUnit.HOURS));
        secret = posts.add(writer.defaultCategoryId(), "숨긴 글 제목", "숨긴 본문", "private", FUTURE.plus(3, ChronoUnit.HOURS));
    }

    @Test
    void 공개_글_카드에는_제목_앞부분_블로그_이름_첫_그림이_이스케이프되어_들어간다() throws Exception {
        String html = body(mvc.perform(get("/og/posts/{id}", open))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/html"))
                .andExpect(header().string("Cache-Control", "max-age=600, public")));
        assertThat(html)
                .contains("<meta property=\"og:title\" content=\"R&amp;D &lt;노트&gt; &quot;첫&quot; 글\">")
                .contains("<meta property=\"og:type\" content=\"article\">")
                .contains("<meta property=\"og:url\" content=\"" + BASE + "/posts/" + open + "\">")
                .contains("<meta property=\"og:image\" content=\"" + BASE + "/api/images/7\">")
                .contains(writer.user().getNickname() + "의 블로그 · 소제목 표지 본문 첫 줄입니다.")
                .doesNotContain("<노트>");
    }

    @Test
    void 비공개_글과_없는_글은_기본_카드만() throws Exception {
        for (Long id : new Long[] {secret, 987_654_321L}) {
            String html = body(mvc.perform(get("/og/posts/{id}", id)).andExpect(status().isOk()));
            assertThat(html)
                    .contains("<meta property=\"og:title\" content=\"글결\">")
                    .contains("<meta property=\"og:image\" content=\"" + BASE + "/og-default.png\">")
                    .doesNotContain("숨긴");
        }
    }

    @Test
    void 블로그_카드와_첫_화면_카드() throws Exception {
        assertThat(body(mvc.perform(get("/og/blog/{id}", writer.blogId())).andExpect(status().isOk())))
                .contains("content=\"" + writer.user().getNickname() + "의 블로그 · 글결\"")
                .contains("<meta property=\"og:url\" content=\"" + BASE + "/blog/" + writer.blogId() + "\">");
        assertThat(body(mvc.perform(get("/og/")).andExpect(status().isOk())))
                .contains("<meta property=\"og:url\" content=\"" + BASE + "/\">");
    }

    @Test
    void RSS는_XML로_읽히고_공개_글만_최신순() throws Exception {
        String site = body(mvc.perform(get("/rss.xml"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("application/rss+xml")));
        Document doc = parse(site);
        assertThat(doc.getElementsByTagName("item").getLength()).isPositive();
        assertThat(doc.getElementsByTagName("item").item(0).getTextContent()).contains("R&D <노트>");
        assertThat(site).doesNotContain("숨긴 글 제목").contains(BASE + "/posts/" + open);

        String blog = body(mvc.perform(get("/blog/{id}/rss.xml", writer.blogId())).andExpect(status().isOk()));
        assertThat(parse(blog).getElementsByTagName("item").getLength()).isEqualTo(1);
        mvc.perform(get("/blog/{id}/rss.xml", 987_654_321L)).andExpect(status().isNotFound());
    }

    @Test
    void 사이트_지도에는_공개_글과_블로그() throws Exception {
        String xml = body(mvc.perform(get("/sitemap.xml")).andExpect(status().isOk()));
        parse(xml);
        assertThat(xml)
                .contains("<loc>" + BASE + "/posts/" + open + "</loc>")
                .contains("<loc>" + BASE + "/blog/" + writer.blogId() + "</loc>")
                .doesNotContain("/posts/" + secret + "<");
    }

    @Test
    void 검색엔진_안내는_회원_화면을_빼고_사이트_지도를_알린다() throws Exception {
        assertThat(body(mvc.perform(get("/robots.txt")).andExpect(status().isOk())))
                .contains("Disallow: /api/", "Disallow: /manage", "Sitemap: " + BASE + "/sitemap.xml");
    }

    private static String body(org.springframework.test.web.servlet.ResultActions result) throws Exception {
        return result.andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
    }

    private static Document parse(String xml) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
        return factory.newDocumentBuilder().parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
    }
}
