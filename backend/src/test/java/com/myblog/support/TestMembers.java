package com.myblog.support;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myblog.user.domain.User;
import com.myblog.user.service.MemberRegistration;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.UUID;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.test.web.servlet.MockMvc;

/**
 * 003 테스트가 같이 쓰는 도우미: 회원 가입(블로그와 미분류가 함께 생김), 로그인, 분류·글 바로 넣기.
 * 표에 바로 넣는 것은 "이미 있는 데이터"를 준비할 때만 쓴다. 확인하려는 동작은 주소로 부른다.
 */
@Component
public class TestMembers {

    public static final String PASSWORD = "abcd123!";

    private final MemberRegistration registration;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbc;

    public TestMembers(MemberRegistration registration, PasswordEncoder passwordEncoder, JdbcTemplate jdbc) {
        this.registration = registration;
        this.passwordEncoder = passwordEncoder;
        this.jdbc = jdbc;
    }

    /** 가입한 회원과 그 블로그·미분류 번호. */
    public record Member(User user, Long blogId, Long defaultCategoryId) {

        public Long id() {
            return user.getId();
        }
    }

    public Member register(String prefix) {
        String id = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        User user = registration.register(prefix + id + "@example.com", passwordEncoder.encode(PASSWORD),
                prefix + id.substring(0, 7));
        Long blogId = jdbc.queryForObject("select blog_id from blog where users_id = ?", Long.class, user.getId());
        Long categoryId = jdbc.queryForObject("select category_id from category where blog_id = ? and is_default",
                Long.class, blogId);
        return new Member(user, blogId, categoryId);
    }

    public MockHttpSession login(MockMvc mvc, Member member) throws Exception {
        return (MockHttpSession) mvc.perform(post("/api/auth/login").with(csrf()).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + member.user().getEmail() + "\",\"password\":\"" + PASSWORD + "\"}"))
                .andExpect(status().isOk())
                .andReturn().getRequest().getSession(false);
    }

    /** 분류를 표에 바로 넣는다 (분류 관리 주소가 생기기 전의 준비용). */
    public Long addCategory(Long blogId, String name, String visibility, int sortOrder) {
        return jdbc.queryForObject("insert into category (blog_id, name, visibility, is_default, sort_order, created_at)"
                + " values (?, ?, ?, false, ?, now()) returning category_id", Long.class, blogId, name, visibility, sortOrder);
    }

    /** 글을 표에 바로 넣는다 (준비용). 주제는 첫 번째 주제, 작성 시각은 지금. */
    public Long addPost(Long categoryId, String title, String visibility) {
        return addPost(categoryId, title, visibility, Instant.now());
    }

    /** 작성 시각을 정해 글을 넣는다 (이전·다음 글 순서 확인용). */
    public Long addPost(Long categoryId, String title, String visibility, Instant createdAt) {
        return jdbc.queryForObject("insert into post (category_id, topic_id, title, content, visibility, created_at)"
                + " values (?, (select min(topic_id) from topic), ?, '본문', ?, ?) returning post_id",
                Long.class, categoryId, title, visibility, Timestamp.from(createdAt));
    }
}
