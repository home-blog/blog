package com.myblog.common.dev;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 개발용: 내 컴퓨터 DB에 예시 글(블로거 12명, 글 108개)이 없으면 서버를 켤 때 넣는다 (2026-10-12 사용자: 로컬 DB에 따로 넣기 번거롭다).
 * 배포 DB와 같은 deploy/seed/demo-content.sql을 쓰므로 로컬과 배포 화면이 같아 보인다. dev 설정에서만 켜진다(배포에는 없음).
 * 끄려면 .env에 DEMO_SEED=false. 지우려면 deploy/seed/demo-remove.sql (지운 뒤에는 서버를 켤 때 다시 들어간다).
 * 예시 글 사진(demo-photos.sql)과 글마당 예시 글(demo-board.sql)은 켤 때마다 맞춘다: 이미 예시 글이 있는 DB에도 붙는다
 * (몇 번 실행해도 같다).
 */
@Component
@Profile("dev")
public class DevDemoSeed implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DevDemoSeed.class);
    /** demo-content.sql이 예시 회원에 넣는 표식 (로그인할 수 없는 비밀번호 칸 값). */
    static final String DEMO_MARKER = "!demo-account-no-login";
    /** 저장소 맨 위 기준 파일 위치. 서버는 backend 폴더나 맨 위에서 실행한다 (.env와 같은 이유) */
    private static final List<Path> SEED_DIRS = List.of(Path.of("../deploy/seed"), Path.of("deploy/seed"));
    private static final String CONTENT_FILE = "demo-content.sql";
    private static final String PHOTOS_FILE = "demo-photos.sql";
    private static final String BOARD_FILE = "demo-board.sql";
    /** psql 전용 머리(\set, 이미 있으면 멈추는 블록)가 끝나는 줄 */
    private static final String HEADER_END = "end $$;";
    private static final String BODY_END = "commit;";

    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final boolean enabled;

    public DevDemoSeed(JdbcTemplate jdbc, TransactionTemplate transaction,
            @Value("${DEMO_SEED:true}") boolean enabled) {
        this.jdbc = jdbc;
        this.transaction = transaction;
        this.enabled = enabled;
    }

    @Override
    public void run(ApplicationArguments args) throws IOException {
        if (!enabled) {
            return;
        }
        if (!hasDemoData()) {
            Optional<List<String>> content = read(CONTENT_FILE);
            if (content.isEmpty()) {
                log.info("예시 글 파일(deploy/seed/{})을 찾지 못해 넣지 않았습니다", CONTENT_FILE);
                return;
            }
            transaction.executeWithoutResult(status -> content.get().forEach(jdbc::execute));
            log.info("개발 DB에 예시 글을 넣었습니다 ({}개 문장). 끄려면 .env에 DEMO_SEED=false", content.get().size());
        }
        Optional<List<String>> photos = read(PHOTOS_FILE);
        if (photos.isPresent()) {
            Integer changed = transaction.execute(status -> photos.get().stream().mapToInt(jdbc::update).sum());
            if (changed != null && changed > 0) {
                log.info("예시 글 {}개에 사진을 넣었습니다", changed);
            }
        }
        Optional<List<String>> board = read(BOARD_FILE);
        if (board.isPresent()) {
            transaction.executeWithoutResult(status -> board.get().forEach(jdbc::update));
        }
    }

    private static Optional<List<String>> read(String fileName) throws IOException {
        Optional<Path> file = SEED_DIRS.stream().map(dir -> dir.resolve(fileName)).filter(Files::isRegularFile).findFirst();
        return file.isEmpty() ? Optional.empty()
                : Optional.of(statements(Files.readString(file.get(), StandardCharsets.UTF_8)));
    }

    private boolean hasDemoData() {
        Integer count = jdbc.queryForObject(
                "select count(*) from users where password = ? and email like 'demo.%@example.com'", Integer.class,
                DEMO_MARKER);
        return count != null && count > 0;
    }

    /**
     * demo-content.sql·demo-photos.sql에서 psql 전용 줄을 빼고 SQL 문장만 나눈다. 머리(…end $$;)와 끝(commit; 이후)은 버리고
     * 그 사이의 insert 문장을 따옴표 안의 줄바꿈·세미콜론을 지켜 나눈다 (본문에 ;가 들어 있다).
     * 생성 파일은 따옴표 문자열만 쓰고 $$ 문자열은 머리에만 있어서 이 정도로 충분하다.
     */
    static List<String> statements(String script) {
        int start = script.indexOf(HEADER_END);
        int end = script.lastIndexOf(BODY_END);
        if (start < 0 || end < start) {
            throw new IllegalStateException("예시 데이터 SQL 모양이 예상과 다릅니다 (build_seed.py로 다시 만들어 주세요)");
        }
        String body = script.substring(start + HEADER_END.length(), end);
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuote = false;   // '…' 안 ('' 는 따옴표 글자: 두 번 바뀌어 그대로)
        boolean inComment = false; // -- 부터 줄 끝까지
        for (int i = 0; i < body.length(); i++) {
            char c = body.charAt(i);
            if (inComment) {
                inComment = c != '\n';
                continue;
            }
            if (!inQuote && c == '-' && i + 1 < body.length() && body.charAt(i + 1) == '-') {
                inComment = true;
                continue;
            }
            if (c == '\'') {
                inQuote = !inQuote;
            }
            if (!inQuote && c == ';') {
                add(statements, current);
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        add(statements, current);
        return statements;
    }

    private static void add(List<String> statements, StringBuilder sql) {
        String statement = sql.toString().strip();
        if (!statement.isEmpty()) {
            statements.add(statement);
        }
    }
}
