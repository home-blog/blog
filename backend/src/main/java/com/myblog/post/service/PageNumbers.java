package com.myblog.post.service;

/**
 * 페이지 번호 계산 한곳 (specs/004 T007, research B-2, FR-002, FR-004, FR-014, SC-005).
 * 화면과 서버는 1부터 센다. 요청 번호는 글자로 받아 여기서 읽는다 ({@code ?page=abc}가 서버 오류가 되지 않게).
 * <ul>
 *   <li>마지막 페이지 = ceil(전체 / 한 페이지 글 수), 0개면 1.</li>
 *   <li>마지막보다 큰 번호는 마지막 페이지로 바꾼다 (FR-004).</li>
 *   <li>1보다 작거나 숫자가 아닌 번호는 1페이지로 본다 (004 D-2: A, 2026-10-08). 글 목록과 검색 결과가 같다.</li>
 * </ul>
 */
public final class PageNumbers {

    /** 이보다 자릿수가 많은 숫자는 읽지 않고 "아주 큰 번호"로 본다 (int를 넘는 값). */
    private static final int MAX_DIGITS = 9;

    private PageNumbers() {
    }

    /** 실제로 보여 줄 페이지(1부터)와 마지막 페이지. */
    public record PageSlice(int page, int totalPages, int pageSize) {

        /** 건너뛸 글 수. */
        public long offset() {
            return (long) (page - 1) * pageSize;
        }
    }

    public static PageSlice of(String requested, long totalCount, int pageSize) {
        int totalPages = totalPages(totalCount, pageSize);
        return new PageSlice(clamp(requested, totalPages), totalPages, pageSize);
    }

    static int totalPages(long totalCount, int pageSize) {
        if (totalCount <= 0) {
            return 1;
        }
        return (int) Math.min(Integer.MAX_VALUE, (totalCount + pageSize - 1) / pageSize);
    }

    private static int clamp(String requested, int totalPages) {
        if (requested == null || requested.isEmpty()) {
            return 1;
        }
        if (!requested.chars().allMatch(c -> c >= '0' && c <= '9')) {
            return outOfRangeLow();
        }
        String digits = requested.replaceFirst("^0+", "");
        if (digits.isEmpty()) {
            return outOfRangeLow();
        }
        if (digits.length() > MAX_DIGITS) {
            return totalPages;
        }
        return Math.min(Integer.parseInt(digits), totalPages);
    }

    /** 1보다 작거나 숫자가 아닌 번호 (D-2: A, 1페이지). */
    private static int outOfRangeLow() {
        return 1;
    }
}
