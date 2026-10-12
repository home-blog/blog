package com.myblog.common.dev;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

/** 개발용 예시 글 넣기: demo-content.sql에서 psql 전용 줄을 빼고 문장만 나눈다. */
class DevDemoSeedTest {

    @Test
    void 실제_예시_파일에서_insert_문장만_나온다() throws Exception {
        String script = Files.readString(Path.of("../deploy/seed/demo-content.sql"), StandardCharsets.UTF_8);
        List<String> statements = DevDemoSeed.statements(script);
        assertThat(statements).isNotEmpty().allSatisfy(sql -> assertThat(sql.strip()).startsWithIgnoringCase("insert into"));
        assertThat(statements).noneMatch(sql -> sql.contains("\\set") || sql.contains("\\echo") || sql.contains("raise exception"));
        // 예시 회원 12명, 글 108개
        assertThat(statements).filteredOn(sql -> sql.strip().startsWith("insert into users ")).hasSize(12);
        assertThat(statements).filteredOn(sql -> sql.strip().startsWith("insert into post ")).hasSize(108);
    }

    @Test
    void 예시_사진_파일은_글이_처음_모양일_때만_바꾸는_update_문장이다() throws Exception {
        String script = Files.readString(Path.of("../deploy/seed/demo-photos.sql"), StandardCharsets.UTF_8);
        List<String> statements = DevDemoSeed.statements(script);
        assertThat(statements).isNotEmpty().allSatisfy(sql -> assertThat(sql.strip())
                .startsWith("update post set content = ")
                .contains("](https://picsum.photos/id/")
                .contains(" and content = '"));
    }

    @Test
    void 따옴표_안의_세미콜론과_줄바꿈은_문장을_나누지_않는다() {
        String script = """
                \\set ON_ERROR_STOP on
                begin;
                do $$ begin
                  raise exception 'x';
                end $$;
                insert into post (title, content) values ('a; b', '첫 줄;
                둘째 줄');
                insert into tag (name) values ('c');
                commit;
                \\echo 끝
                """;
        List<String> statements = DevDemoSeed.statements(script);
        assertThat(statements).hasSize(2);
        assertThat(statements.get(0)).contains("'a; b'", "첫 줄;\n둘째 줄");
    }

    @Test
    void 모양이_다르면_알린다() {
        assertThatThrownBy(() -> DevDemoSeed.statements("select 1;")).isInstanceOf(IllegalStateException.class);
    }
}
