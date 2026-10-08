package com.myblog.image.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.myblog.image.storage.InMemoryImageStorage;
import com.myblog.support.TestComments;
import com.myblog.support.TestImages;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMultipartHttpServletRequestBuilder;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

/**
 * 이미지 올리기·보기 (specs/005 US4, T047, quickstart S-7, S-8, FR-022 ~ FR-026, FR-029, SC-004).
 * 저장소는 메모리 저장소(TestImages)를 쓴다. 5MB를 넘는 파일을 프레임워크가 먼저 막는 경로는 진짜 서버에서 확인한다
 * (MockMvc는 업로드 크기 설정을 거치지 않는다). 여기서는 서비스가 같은 규칙으로 다시 막는 것을 본다.
 */
@SpringBootTest
@Import({TestMembers.class, TestComments.class, TestImages.class})
class ImageUploadTest {

    private static final int MB = 1024 * 1024;
    private static final String INVALID = "이미지는 5MB 이하의 jpg, png, gif, webp만 올릴 수 있습니다";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private TestComments testComments;

    @Autowired
    private InMemoryImageStorage storage;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private final List<Member> created = new ArrayList<>();
    private Member writer;
    private MockHttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        storage.reset();
        writer = register("w");
        session = members.login(mvc, writer);
    }

    @AfterEach
    void cleanUp() {
        storage.reset();
        created.forEach(testComments::deleteMember);
    }

    @Test
    void S7_1_2_4_9MB와_정확히_5MB_png는_올라가고_새_이름과_주소를_받는다() throws Exception {
        for (int size : new int[] {(int) (4.9 * MB), 5 * MB}) {
            String url = JsonPath.read(upload(TestImages.file("photo.png", "image/png", TestImages.bytes(TestImages.PNG, size)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.imageId").isNumber())
                    .andReturn().getResponse().getContentAsString(), "$.url");
            assertThat(url).matches("/api/images/[0-9a-f-]{36}\\.png");
            String key = "posts/" + url.substring("/api/images/".length());
            assertThat(storage.has(key)).isTrue();
            assertThat(storage.get(key).bytes()).hasSize(size);
            assertThat(jdbc.queryForObject("select post_id is null from post_image where storage_key = ?", Boolean.class, key))
                    .isTrue();
        }
    }

    @Test
    void S7_3_5MB를_넘으면_INVALID_IMAGE() throws Exception {
        upload(TestImages.file("big.png", "image/png", TestImages.bytes(TestImages.PNG, (int) (5.1 * MB))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_IMAGE"))
                .andExpect(jsonPath("$.message").value(INVALID));
        assertThat(rows()).isZero();
    }

    @Test
    void S7_4_5_gif_webp_jpg는_되고_bmp와_빈_파일은_안_된다() throws Exception {
        upload(TestImages.file("a.gif", "image/gif", TestImages.bytes(TestImages.GIF, 100))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.endsWith(".gif")));
        upload(TestImages.file("a.webp", "image/webp", TestImages.bytes(TestImages.WEBP, 100))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.endsWith(".webp")));
        upload(TestImages.file("a.jpg", "image/jpeg", TestImages.bytes(TestImages.JPG, 100))).andExpect(status().isCreated())
                .andExpect(jsonPath("$.url").value(org.hamcrest.Matchers.endsWith(".jpg")));
        upload(TestImages.file("a.bmp", "image/bmp", TestImages.bytes(TestImages.BMP, 100)))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_IMAGE"));
        upload(TestImages.file("a.png", "image/png", new byte[0]))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_IMAGE"));
        mvc.perform(multipart("/api/images").with(csrf()).session(session))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_IMAGE"));
        assertThat(rows()).isEqualTo(3);
    }

    @Test
    void S8_1_2_3_형식은_이름이_아니라_파일_앞부분으로_정하고_사용자_파일_이름은_쓰지_않는다() throws Exception {
        upload(TestImages.file("page.png", "image/png", "<html><script>alert(1)</script></html>".getBytes()))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("INVALID_IMAGE"));
        upload(TestImages.file("<svg>.svg", "image/svg+xml", "<svg xmlns=\"http://www.w3.org/2000/svg\"/>".getBytes()))
                .andExpect(status().isBadRequest());
        String url = JsonPath.read(upload(TestImages.file("../../test.jpg", "image/jpeg", TestImages.bytes(TestImages.PNG, 80)))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString(), "$.url");
        assertThat(url).endsWith(".png").doesNotContain("test").doesNotContain("..");
        mvc.perform(get(url).session(session))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(header().string("X-Content-Type-Options", "nosniff"))
                .andExpect(header().string("Cache-Control", org.hamcrest.Matchers.containsString("private")));
    }

    @Test
    void S7_6_새_글은_연결_전_이미지가_10장이면_11번째는_409() throws Exception {
        for (int i = 0; i < 10; i++) {
            upload(TestImages.png()).andExpect(status().isCreated());
        }
        upload(TestImages.png())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IMAGE_LIMIT_EXCEEDED"))
                .andExpect(jsonPath("$.message").value("※ 이미지는 글 하나에 10장까지 올릴 수 있습니다"));
        assertThat(rows()).isEqualTo(10);
    }

    @Test
    void S7_7_수정_중인_글은_그_글의_이미지와_연결_전_이미지를_합쳐_10장까지() throws Exception {
        Long postId = members.addPost(writer.defaultCategoryId(), "내 글", "public");
        for (int i = 0; i < 9; i++) {
            jdbc.update("insert into post_image (post_id, users_id, storage_key, created_at) values (?, ?, ?, now())",
                    postId, writer.id(), "posts/00000000-0000-0000-0000-00000000000" + i + ".png");
        }
        uploadTo(postId, TestImages.png()).andExpect(status().isCreated());
        uploadTo(postId, TestImages.png()).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("IMAGE_LIMIT_EXCEEDED"));
    }

    @Test
    void 남의_글이나_없는_글_번호로_올리면_404이고_파일이_남지_않는다() throws Exception {
        Member other = register("x");
        Long othersPost = members.addPost(other.defaultCategoryId(), "남의 글", "public");
        int files = storage.list("posts/").size();
        uploadTo(othersPost, TestImages.png()).andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("POST_NOT_FOUND"));
        uploadTo(99_999_999L, TestImages.png()).andExpect(status().isNotFound());
        assertThat(rows()).isZero();
        assertThat(storage.list("posts/")).hasSize(files);
    }

    @Test
    void S7_10_로그인하지_않으면_401_CSRF가_없으면_거절() throws Exception {
        mvc.perform(multipart("/api/images").file(TestImages.png()).with(csrf())).andExpect(status().isUnauthorized());
        mvc.perform(multipart("/api/images").file(TestImages.png()).session(session)).andExpect(status().isForbidden());
        assertThat(rows()).isZero();
    }

    @Test
    void S8_5_6_연결_전_이미지는_올린_사람만_보고_비공개_글의_이미지는_주인만_본다() throws Exception {
        String url = uploadedUrl();
        Member other = register("v");
        MockHttpSession otherSession = members.login(mvc, other);
        mvc.perform(get(url)).andExpect(status().isNotFound());
        mvc.perform(get(url).session(otherSession)).andExpect(status().isNotFound());
        byte[] body = mvc.perform(get(url).session(session)).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        assertThat(body).startsWith(TestImages.PNG);

        Long hidden = members.addPost(writer.defaultCategoryId(), "비공개", "private");
        jdbc.update("update post_image set post_id = ? where storage_key = ?", hidden, "posts/" + url.substring(12));
        mvc.perform(get(url).session(otherSession)).andExpect(status().isNotFound());
        mvc.perform(get(url).session(session)).andExpect(status().isOk());
        jdbc.update("update post set visibility = 'public' where post_id = ?", hidden);
        mvc.perform(get(url)).andExpect(status().isOk());
        // 없는 이름, 모양이 다른 이름도 같은 404
        mvc.perform(get("/api/images/00000000-0000-0000-0000-000000000000.png")).andExpect(status().isNotFound());
        mvc.perform(get("/api/images/..%2F..%2Fetc%2Fpasswd")).andExpect(status().is4xxClientError());
    }

    @Test
    void S8_7_8_저장소가_꺼지면_503이고_오류에_경로나_예외_이름이_없다() throws Exception {
        storage.failAll = true;
        String body = upload(TestImages.png())
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("STORAGE_UNAVAILABLE"))
                .andExpect(jsonPath("$.message").value("※ 잠시 뒤 다시 시도해 주세요"))
                .andReturn().getResponse().getContentAsString();
        assertThat(body).doesNotContain("posts/").doesNotContain("Exception").doesNotContain("localhost");
        assertThat(rows()).isZero();
        storage.failAll = false;
        upload(TestImages.png()).andExpect(status().isCreated());
    }

    private String uploadedUrl() throws Exception {
        return JsonPath.read(upload(TestImages.png()).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.url");
    }

    private ResultActions upload(MockMultipartFile file) throws Exception {
        return mvc.perform(multipart("/api/images").file(file).with(csrf()).session(session));
    }

    private ResultActions uploadTo(Long postId, MockMultipartFile file) throws Exception {
        MockMultipartHttpServletRequestBuilder request = multipart("/api/images").file(file);
        request.param("postId", String.valueOf(postId));
        return mvc.perform(request.with(csrf()).session(session));
    }

    private long rows() {
        return jdbc.queryForObject("select count(*) from post_image where users_id = ?", Long.class, writer.id());
    }

    private Member register(String prefix) {
        Member member = members.register(prefix);
        created.add(member);
        return member;
    }
}
