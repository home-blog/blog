package com.myblog.post.tag;

import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.common.error.ErrorResponse.FieldErrorItem;
import com.myblog.post.config.TagProperties;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 글에 붙일 태그를 다듬고 검사한다 (specs/005 T043, research B-6, FR-013, FR-014).
 * <ol>
 *   <li>개수가 max-per-post(5)를 넘으면 tags 칸에 TAG_TOO_MANY.</li>
 *   <li>태그마다: 앞뒤 공백을 지우고 → 앞의 #을 모두 지우고 → 1~15자(코드 포인트)이고 공백·쉼표가 없어야 한다 (tags[n] TAG_INVALID).</li>
 *   <li><b>소문자로 바꾼다</b> (D-10 A). 그래서 Java와 java는 같은 태그다. 겹치면 조용히 합치지 않고 tags[n] TAG_DUPLICATED.</li>
 * </ol>
 * 칸별 오류를 모아 VALIDATION_FAILED로 던진다 (모양은 @Valid와 같다).
 */
@Component
public class TagNormalizer {

    private final TagProperties properties;

    public TagNormalizer(TagProperties properties) {
        this.properties = properties;
    }

    /** 저장할 태그 목록 (소문자, 보낸 순서). 없으면 빈 목록. */
    public List<String> normalize(List<String> tags) {
        if (tags == null || tags.isEmpty()) {
            return List.of();
        }
        if (tags.size() > properties.maxPerPost()) {
            throw invalid(List.of(new FieldErrorItem("tags", ErrorCode.TAG_TOO_MANY.name(), tooManyMessage())));
        }
        List<String> result = new ArrayList<>();
        List<FieldErrorItem> errors = new ArrayList<>();
        Set<String> seen = new HashSet<>();
        for (int i = 0; i < tags.size(); i++) {
            Optional<String> name = normalizeOne(tags.get(i));
            String field = "tags[" + i + "]";
            if (name.isEmpty()) {
                errors.add(new FieldErrorItem(field, ErrorCode.TAG_INVALID.name(), invalidMessage()));
            } else if (!seen.add(name.get())) {
                errors.add(new FieldErrorItem(field, ErrorCode.TAG_DUPLICATED.name(), ErrorCode.TAG_DUPLICATED.message()));
            } else {
                result.add(name.get());
            }
        }
        if (!errors.isEmpty()) {
            throw invalid(errors);
        }
        return List.copyOf(result);
    }

    /** 태그 하나를 다듬는다. 규칙에 맞지 않으면 비어 있다. 태그별 글 목록이 주소의 이름을 다듬을 때도 쓴다. */
    public Optional<String> normalizeOne(String tag) {
        if (tag == null) {
            return Optional.empty();
        }
        String name = tag.strip();
        int start = 0;
        while (start < name.length() && name.charAt(start) == '#') {
            start++;
        }
        name = name.substring(start);
        int length = name.codePointCount(0, name.length());
        if (length < properties.minLength() || length > properties.maxLength()) {
            return Optional.empty();
        }
        if (name.chars().anyMatch(c -> Character.isWhitespace(c) || Character.isSpaceChar(c) || c == ',')) {
            return Optional.empty();
        }
        return Optional.of(name.toLowerCase(Locale.ROOT));
    }

    private String tooManyMessage() {
        return "※ 태그는 %d개까지 붙일 수 있습니다".formatted(properties.maxPerPost());
    }

    private String invalidMessage() {
        return "※ 태그는 공백과 쉼표 없이 %d~%d자로 입력해 주세요".formatted(properties.minLength(), properties.maxLength());
    }

    private static ApiException invalid(List<FieldErrorItem> errors) {
        return new ApiException(ErrorCode.VALIDATION_FAILED, ErrorCode.VALIDATION_FAILED.message(), errors);
    }
}
