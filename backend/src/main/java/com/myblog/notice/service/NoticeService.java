package com.myblog.notice.service;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.common.error.ErrorResponse.FieldErrorItem;
import com.myblog.common.paging.PageNumbers;
import com.myblog.common.paging.PageNumbers.PageSlice;
import com.myblog.notice.config.NoticeProperties;
import com.myblog.notice.domain.Notice;
import com.myblog.notice.domain.NoticeKind;
import com.myblog.notice.repository.NoticeRepository;
import com.myblog.user.LoggedInMember;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 공지사항 (specs/012). 읽기는 누구나, 쓰기·고치기·지우기는 관리자만.
 * 관리자 확인을 입력 검사보다 먼저 한다: 관리자가 아닌 사람에게는 입력 규칙을 알려 주지 않고 403만 준다.
 */
@Service
public class NoticeService {

    /** 0.6.0 모양, 자리마다 1~4자리 (D-6). 배포 작업이 받는 번호와 같다 */
    private static final Pattern RELEASE_VERSION = Pattern.compile("\\d{1,4}\\.\\d{1,4}\\.\\d{1,4}");

    private final NoticeRepository notices;
    private final LoggedInMember loggedInMember;
    private final NoticeProperties properties;
    private final Clock clock;

    public NoticeService(NoticeRepository notices, LoggedInMember loggedInMember, NoticeProperties properties, Clock clock) {
        this.notices = notices;
        this.loggedInMember = loggedInMember;
        this.properties = properties;
        this.clock = clock;
    }

    /** 쓰기·고치기 입력 (contracts 4). 비어 있을 수 있는 값은 검사에서 거른다. */
    public record NoticeInput(String kind, String title, String content, String releaseVersion, boolean pinned) {
    }

    public record NoticeRow(Long noticeId, String kind, String title, String releaseVersion, boolean pinned, Instant createdAt) {

        static NoticeRow of(Notice notice) {
            return new NoticeRow(notice.getId(), notice.getKind(), notice.getTitle(), notice.getReleaseVersion(),
                    notice.isPinned(), notice.getCreatedAt());
        }
    }

    public record NoticePage(List<NoticeRow> items, int page, int totalPages, long totalCount) {
    }

    public record NoticeDetail(Long noticeId, String kind, String title, String content, String releaseVersion,
            String releaseUrl, boolean pinned, Instant createdAt, Instant updatedAt) {
    }

    /** 탭(kind)과 쪽. 모르는 종류는 전체로 본다(S1-5). 쪽 번호 규칙은 글 목록과 같다(004 D-2). */
    @Transactional(readOnly = true)
    public NoticePage list(String kind, String page) {
        Optional<NoticeKind> tab = NoticeKind.from(kind);
        long total = tab.map(k -> notices.countByKind(k.value())).orElseGet(notices::count);
        PageSlice slice = PageNumbers.of(page, total, properties.pageSize());
        PageRequest request = PageRequest.of(slice.page() - 1, slice.pageSize());
        List<Notice> rows = tab.map(k -> notices.findPageByKind(k.value(), request)).orElseGet(() -> notices.findPage(request));
        return new NoticePage(rows.stream().map(NoticeRow::of).toList(), slice.page(), slice.totalPages(), total);
    }

    @Transactional(readOnly = true)
    public NoticeDetail read(Long noticeId) {
        Notice notice = find(noticeId);
        String version = notice.getReleaseVersion();
        return new NoticeDetail(notice.getId(), notice.getKind(), notice.getTitle(), notice.getContent(), version,
                version == null ? null : properties.releaseUrlOf(version), notice.isPinned(), notice.getCreatedAt(),
                notice.getUpdatedAt());
    }

    /** 머리글의 새 글 점(US3)용. 글이 없으면 비운다. */
    @Transactional(readOnly = true)
    public Optional<Instant> latestAt() {
        return notices.findFirstByOrderByCreatedAtDesc().map(Notice::getCreatedAt);
    }

    @Transactional
    public Long write(Authentication authentication, NoticeInput input) {
        Long adminId = loggedInMember.requireAdminIdOf(authentication);
        Checked checked = check(input);
        Notice notice = Notice.write(checked.kind(), checked.title(), checked.content(), checked.releaseVersion(),
                input.pinned(), adminId, Instant.now(clock));
        return notices.save(notice).getId();
    }

    @Transactional
    public Long change(Authentication authentication, Long noticeId, NoticeInput input) {
        loggedInMember.requireAdminIdOf(authentication);
        Notice notice = find(noticeId);
        Checked checked = check(input);
        notice.change(checked.kind(), checked.title(), checked.content(), checked.releaseVersion(), input.pinned(),
                Instant.now(clock));
        return notice.getId();
    }

    @Transactional
    public void delete(Authentication authentication, Long noticeId) {
        loggedInMember.requireAdminIdOf(authentication);
        notices.delete(find(noticeId));
    }

    private Notice find(Long noticeId) {
        return notices.findById(noticeId).orElseThrow(() -> new ApiException(ErrorCode.NOTICE_NOT_FOUND));
    }

    private record Checked(NoticeKind kind, String title, String content, String releaseVersion) {
    }

    /** 칸마다 검사해 틀린 칸을 모두 알린다 (contracts 4의 표). 제목은 앞뒤 공백을 지우고, 본문은 원문 그대로 센다. */
    private Checked check(NoticeInput input) {
        List<FieldErrorItem> errors = new ArrayList<>();
        Optional<NoticeKind> kind = NoticeKind.from(input.kind());
        if (kind.isEmpty()) {
            errors.add(item("kind", ErrorCode.NOTICE_KIND_INVALID, ErrorCode.NOTICE_KIND_INVALID.message()));
        }

        String title = input.title() == null ? "" : input.title().strip();
        int titleLength = title.codePointCount(0, title.length());
        if (titleLength == 0) {
            errors.add(item("title", ErrorCode.TITLE_REQUIRED, ErrorCode.TITLE_REQUIRED.message()));
        } else if (titleLength > properties.title().maxLength()) {
            errors.add(item("title", ErrorCode.TITLE_TOO_LONG,
                    "※ 제목은 %d자 이하로 입력해 주세요".formatted(properties.title().maxLength())));
        }

        String content = input.content() == null ? "" : input.content().replace("\r\n", "\n");
        if (content.isBlank()) {
            errors.add(item("content", ErrorCode.CONTENT_REQUIRED, ErrorCode.CONTENT_REQUIRED.message()));
        } else if (content.codePointCount(0, content.length()) > properties.content().maxLength()) {
            errors.add(item("content", ErrorCode.CONTENT_TOO_LONG,
                    "※ 본문은 %,d자 이하로 입력해 주세요".formatted(properties.content().maxLength())));
        }

        String version = input.releaseVersion();
        if (kind.orElse(null) == NoticeKind.UPDATE) {
            if (version == null) {
                errors.add(item("releaseVersion", ErrorCode.RELEASE_VERSION_REQUIRED,
                        ErrorCode.RELEASE_VERSION_REQUIRED.message()));
            } else if (!RELEASE_VERSION.matcher(version).matches()) {
                errors.add(item("releaseVersion", ErrorCode.RELEASE_VERSION_INVALID,
                        ErrorCode.RELEASE_VERSION_INVALID.message()));
            }
        } else if (kind.orElse(null) == NoticeKind.NOTICE && version != null) {
            errors.add(item("releaseVersion", ErrorCode.RELEASE_VERSION_NOT_ALLOWED,
                    ErrorCode.RELEASE_VERSION_NOT_ALLOWED.message()));
        }

        if (!errors.isEmpty()) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.message(), errors);
        }
        return new Checked(kind.orElseThrow(), title, content, version);
    }

    private static FieldErrorItem item(String field, ErrorCode code, String message) {
        return new FieldErrorItem(field, code.name(), message);
    }
}
