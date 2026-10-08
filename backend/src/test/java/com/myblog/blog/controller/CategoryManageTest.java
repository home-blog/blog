package com.myblog.blog.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import com.myblog.support.TestJson;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * 분류 관리 (specs/003 US6, quickstart S-10, S-9a의 2·3·9, T041). 실제 PostgreSQL로 확인한다.
 * 동시 요청(S-10의 4·14)은 CategoryConcurrencyTest에서 본다.
 */
@SpringBootTest
@Transactional
@Import(TestMembers.class)
class CategoryManageTest {

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private JdbcTemplate jdbc;

    private MockMvc mvc;
    private Member owner;
    private MockHttpSession ownerSession;
    private Member other;
    private MockHttpSession otherSession;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        owner = members.register("a");
        ownerSession = members.login(mvc, owner);
        other = members.register("b");
        otherSession = members.login(mvc, other);
    }

    @Test
    void S10_1_추가하면_목록_맨_아래에_공개로_생긴다() throws Exception {
        Long daily = idOf(create(ownerSession, TestJson.of("name", "  일상  "))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("일상"))
                .andExpect(jsonPath("$.postCount").value(0))
                .andExpect(jsonPath("$.isDefault").value(false))
                .andExpect(jsonPath("$.visibility").value("public")));
        create(ownerSession, TestJson.of("name", "비밀", "visibility", "private"))
                .andExpect(jsonPath("$.visibility").value("private"));

        mvc.perform(get("/api/blogs/{id}/categories", owner.blogId()).session(ownerSession))
                .andExpect(jsonPath("$.categories.length()").value(3))
                .andExpect(jsonPath("$.categories[0].name").value("미분류"))
                .andExpect(jsonPath("$.categories[1].categoryId").value(daily))
                .andExpect(jsonPath("$.categories[2].name").value("비밀"));
    }

    @Test
    void S10_2_3_5_같은_블로그_안에서는_대소문자_공백을_무시하고_겹치면_안_되고_다른_블로그와는_된다() throws Exception {
        create(ownerSession, TestJson.of("name", "일상")).andExpect(status().isCreated());
        create(ownerSession, TestJson.of("name", " 일상 "))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_NAME_DUPLICATED"))
                .andExpect(jsonPath("$.message").value("이미 있는 분류입니다"));
        create(ownerSession, TestJson.of("name", "Daily")).andExpect(status().isCreated());
        create(ownerSession, TestJson.of("name", "daily")).andExpect(status().isConflict());
        create(ownerSession, TestJson.of("name", "미분류")).andExpect(status().isConflict());

        create(otherSession, TestJson.of("name", "일상")).andExpect(status().isCreated());
    }

    @Test
    void 이름_규칙과_공개_여부_값을_서버가_검사한다() throws Exception {
        expectFieldError(create(ownerSession, TestJson.of("name", "   ")), "name", "※ 분류 이름을 입력해 주세요");
        expectFieldError(create(ownerSession, TestJson.of("visibility", "public")), "name", "※ 분류 이름을 입력해 주세요");
        expectFieldError(create(ownerSession, TestJson.of("name", "가".repeat(21))), "name", "※ 분류 이름은 20자 이하로 입력해 주세요");
        create(ownerSession, TestJson.of("name", "😀".repeat(20))).andExpect(status().isCreated());
        expectFieldError(create(ownerSession, TestJson.of("name", "x", "visibility", "friends")), "visibility",
                "※ 공개 여부를 다시 골라 주세요");
    }

    @Test
    void S10_6_이름을_바꾸면_그_분류의_글과_블로그_화면에_바로_보이고_자기_이름과는_비교하지_않는다() throws Exception {
        Long daily = create(ownerSession, TestJson.of("name", "일상")).andReturn().getResponse().getContentAsString()
                .transform(json -> Long.valueOf(JsonPath.read(json, "$.categoryId").toString()));
        Long postId = members.addPost(daily, "글", "public");

        update(ownerSession, daily, TestJson.of("name", "여행"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("여행"))
                .andExpect(jsonPath("$.postCount").value(1));
        mvc.perform(get("/api/posts/{id}", postId)).andExpect(jsonPath("$.category.name").value("여행"));
        mvc.perform(get("/api/blogs/{id}/categories", owner.blogId())).andExpect(jsonPath("$.categories[1].name").value("여행"));

        update(ownerSession, daily, TestJson.of("name", "여행")).andExpect(status().isOk());
        update(ownerSession, daily, TestJson.of("name", "여행 ")).andExpect(status().isOk());
        create(ownerSession, TestJson.of("name", "Daily")).andExpect(status().isCreated());
        update(ownerSession, daily, TestJson.of("name", "daily")).andExpect(status().isConflict());
    }

    @Test
    void S9a_1_2_3_공개_여부만_바꾸면_이름은_그대로이고_방문자_목록에서_빠진다() throws Exception {
        Long diary = idOf(create(ownerSession, TestJson.of("name", "일기")));
        members.addPost(diary, "하나", "public");
        members.addPost(diary, "둘", "public");

        update(ownerSession, diary, TestJson.of("visibility", "private"))
                .andExpect(jsonPath("$.name").value("일기"))
                .andExpect(jsonPath("$.visibility").value("private"))
                .andExpect(jsonPath("$.postCount").value(2));
        assertThat(jdbc.queryForObject("select count(*) from post where category_id = ? and visibility = 'public'",
                Long.class, diary)).isEqualTo(2);
        mvc.perform(get("/api/blogs/{id}/categories", owner.blogId()).session(otherSession))
                .andExpect(jsonPath("$.categories.length()").value(1));
        update(ownerSession, owner.defaultCategoryId(), TestJson.of("visibility", "private"))
                .andExpect(jsonPath("$.visibility").value("private"));
    }

    @Test
    void S10_7_8_순서를_바꾸면_블로그_화면도_같은_순서이고_번호가_빠지거나_남의_번호면_거절() throws Exception {
        Long a = idOf(create(ownerSession, TestJson.of("name", "가")));
        Long b = idOf(create(ownerSession, TestJson.of("name", "나")));
        Long d = owner.defaultCategoryId();

        reorder(ownerSession, "[" + b + "," + d + "," + a + "]")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.categories[0].categoryId").value(b))
                .andExpect(jsonPath("$.categories[2].categoryId").value(a));
        mvc.perform(get("/api/blogs/{id}/categories", owner.blogId()))
                .andExpect(jsonPath("$.categories[0].categoryId").value(b))
                .andExpect(jsonPath("$.categories[1].categoryId").value(d))
                .andExpect(jsonPath("$.categories[2].categoryId").value(a));

        for (String bad : new String[] {"[" + b + "," + d + "]", "[" + b + "," + d + "," + a + "," + a + "]",
                "[" + b + "," + d + "," + other.defaultCategoryId() + "]", "[]"}) {
            reorder(ownerSession, bad)
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.code").value("INVALID_CATEGORY_ORDER"))
                    .andExpect(jsonPath("$.message").value("※ 분류 목록이 바뀌었습니다. 새로 고친 뒤 다시 시도해 주세요"));
        }
    }

    @Test
    void S10_9_10_글이_있는_분류는_비공개_글까지_세어_거절하고_옮긴_뒤에는_지운다() throws Exception {
        Long daily = idOf(create(ownerSession, TestJson.of("name", "일상")));
        Long p1 = members.addPost(daily, "공개", "public");
        Long p2 = members.addPost(daily, "비공개", "private");

        remove(ownerSession, daily)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CATEGORY_HAS_POSTS"))
                .andExpect(jsonPath("$.message").value("이 분류에 글이 2개 있어 삭제할 수 없습니다. 글을 다른 분류로 옮긴 뒤 삭제해 주세요"));

        jdbc.update("update post set category_id = ? where post_id in (?, ?)", owner.defaultCategoryId(), p1, p2);
        remove(ownerSession, daily).andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("select count(*) from category where category_id = ?", Long.class, daily)).isZero();
        mvc.perform(get("/api/blogs/{id}/categories", owner.blogId()).session(ownerSession))
                .andExpect(jsonPath("$.categories.length()").value(1))
                .andExpect(jsonPath("$.categories[0].postCount").value(2));
    }

    @Test
    void S10_11_미분류는_이름을_바꿔도_지울_수_없다() throws Exception {
        remove(ownerSession, owner.defaultCategoryId())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("DEFAULT_CATEGORY_NOT_DELETABLE"))
                .andExpect(jsonPath("$.message").value("※ 미분류는 삭제할 수 없습니다"));
        update(ownerSession, owner.defaultCategoryId(), TestJson.of("name", "기타"))
                .andExpect(jsonPath("$.name").value("기타"))
                .andExpect(jsonPath("$.isDefault").value(true));
        remove(ownerSession, owner.defaultCategoryId()).andExpect(jsonPath("$.code").value("DEFAULT_CATEGORY_NOT_DELETABLE"));
    }

    @Test
    void S10_13_S9a_9_남의_분류는_고치기_삭제_모두_404이고_형식이_틀려도_404() throws Exception {
        Long theirs = other.defaultCategoryId();
        update(ownerSession, theirs, TestJson.of("name", "가로채기"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CATEGORY_NOT_FOUND"))
                .andExpect(jsonPath("$.message").value("※ 존재하지 않는 분류입니다"));
        update(ownerSession, theirs, TestJson.of("visibility", "private")).andExpect(status().isNotFound());
        update(ownerSession, theirs, TestJson.of("name", "", "visibility", "x")).andExpect(status().isNotFound());
        remove(ownerSession, theirs).andExpect(status().isNotFound());
        update(ownerSession, 999999999L, TestJson.of("name", "x")).andExpect(status().isNotFound());

        assertThat(jdbc.queryForMap("select name, visibility from category where category_id = ?", theirs))
                .containsEntry("name", "미분류").containsEntry("visibility", "public");
    }

    @Test
    void 로그인하지_않으면_401이고_CSRF가_없으면_거절() throws Exception {
        mvc.perform(post("/api/me/blog/categories").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content(TestJson.of("name", "x")))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/me/blog/categories").session(ownerSession).contentType(MediaType.APPLICATION_JSON)
                        .content(TestJson.of("name", "x")))
                .andExpect(status().isForbidden());
    }

    @Test
    void S10_15_분류가_없는_글과_분류가_없는_블로그는_없다() {
        assertThat(jdbc.queryForObject("select count(*) from post p left join category c using (category_id)"
                + " where c.category_id is null", Long.class)).isZero();
        assertThat(jdbc.queryForObject("select count(*) from blog b where not exists"
                + " (select 1 from category c where c.blog_id = b.blog_id)", Long.class)).isZero();
    }

    private ResultActions create(MockHttpSession session, String json) throws Exception {
        return mvc.perform(post("/api/me/blog/categories").with(csrf()).session(session)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions update(MockHttpSession session, Long categoryId, String json) throws Exception {
        return mvc.perform(patch("/api/me/blog/categories/{id}", categoryId).with(csrf()).session(session)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    private ResultActions reorder(MockHttpSession session, String ids) throws Exception {
        return mvc.perform(put("/api/me/blog/categories/order").with(csrf()).session(session)
                .contentType(MediaType.APPLICATION_JSON).content("{\"categoryIds\":" + ids + "}"));
    }

    private ResultActions remove(MockHttpSession session, Long categoryId) throws Exception {
        return mvc.perform(delete("/api/me/blog/categories/{id}", categoryId).with(csrf()).session(session));
    }

    private Long idOf(ResultActions result) {
        try {
            return Long.valueOf(JsonPath.read(result.andReturn().getResponse().getContentAsString(), "$.categoryId").toString());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void expectFieldError(ResultActions result, String field, String message) throws Exception {
        result.andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.fieldErrors[0].field").value(field))
                .andExpect(jsonPath("$.fieldErrors[0].message").value(message));
    }
}
