package com.myblog.common.error;

import org.springframework.http.HttpStatus;

/**
 * 화면이 상황을 알아보는 영문 이름(code)과, 그대로 보여 줘도 되는 한글 문구(message).
 * 문구는 specs/001 contracts/auth-api.md를 따른다. 서버 내부 정보는 넣지 않는다 (FR-035).
 */
public enum ErrorCode {

    // 공통
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "입력값을 다시 확인해 주세요"),
    UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "※ 로그인이 필요합니다"),
    FORBIDDEN(HttpStatus.FORBIDDEN, "※ 권한이 없습니다"),
    CSRF_TOKEN_INVALID(HttpStatus.FORBIDDEN, "※ 요청이 만료되었습니다. 새로고침한 뒤 다시 시도해 주세요"),
    NOT_FOUND(HttpStatus.NOT_FOUND, "※ 찾을 수 없습니다"),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "※ 지원하지 않는 요청입니다"),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "※ 잠시 뒤 다시 시도해 주세요"),
    INTERNAL_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "※ 잠시 뒤 다시 시도해 주세요"),

    // 칸별 입력 규칙 (VALIDATION_FAILED의 fieldErrors에 들어간다)
    INVALID_EMAIL(HttpStatus.BAD_REQUEST, "이메일 형식이 올바르지 않습니다"),
    INVALID_NICKNAME(HttpStatus.BAD_REQUEST, "닉네임은 한글, 영문, 숫자로 2~10자여야 합니다"),
    INVALID_PASSWORD(HttpStatus.BAD_REQUEST, "비밀번호는 영문, 숫자, 특수문자를 포함해 8~20자로 입력해 주세요"),
    PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "비밀번호가 일치하지 않습니다"),
    REQUIRED(HttpStatus.BAD_REQUEST, "필수 입력 항목입니다"),

    // 이메일 인증, 가입 (specs/001)
    EMAIL_ALREADY_REGISTERED(HttpStatus.CONFLICT, "이미 가입된 이메일입니다"),
    NICKNAME_ALREADY_USED(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다"),
    RESEND_TOO_SOON(HttpStatus.TOO_MANY_REQUESTS, "잠시 뒤에 다시 요청해 주세요"),
    RESEND_DAILY_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "오늘은 더 이상 인증번호를 보낼 수 없습니다"),
    MAIL_SEND_FAILED(HttpStatus.BAD_GATEWAY, "메일을 보내지 못했습니다. 잠시 뒤 다시 시도해 주세요"),
    CODE_MISMATCH(HttpStatus.BAD_REQUEST, "인증번호가 올바르지 않습니다"),
    CODE_EXPIRED(HttpStatus.BAD_REQUEST, "인증번호가 만료되었습니다. 인증번호를 다시 받아 주세요"),
    CODE_ATTEMPTS_EXCEEDED(HttpStatus.TOO_MANY_REQUESTS, "인증번호 입력 횟수를 초과했습니다. 인증번호를 다시 받아 주세요"),
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "이메일 인증을 먼저 완료해 주세요"),
    VERIFICATION_EXPIRED(HttpStatus.FORBIDDEN, "인증 유효 시간이 지났습니다. 이메일 인증을 다시 해 주세요"),

    // 로그인 (specs/001)
    INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "이메일 또는 비밀번호가 올바르지 않습니다"),
    /** 문구의 횟수·분은 설정값에 따라 서비스가 채운다 (LoginService). */
    ACCOUNT_LOCKED(HttpStatus.LOCKED, "로그인 시도가 %d회 실패해 잠겼습니다. %d분 뒤에 다시 시도해 주세요"),

    // 블로그·분류·글 (specs/003 contracts). ※는 상세/03 안내 문구 표에 아직 없는 제안 문구다
    /** 없는 글, 남의 비공개 글, 남의 글의 수정·삭제가 모두 같은 응답이다 (FR-019, FR-026). */
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 글입니다"),
    /** 같은 요청 번호로 이미 저장된 글이 있는데 내용이 다르다 (응답을 못 받고 고쳐서 다시 보낸 경우, D-6). */
    POST_ALREADY_SAVED(HttpStatus.CONFLICT, "※ 이 글은 이미 저장되었습니다. 내 블로그에서 저장된 글을 확인한 뒤 고쳐 주세요"),
    BLOG_NOT_FOUND(HttpStatus.NOT_FOUND, "※ 존재하지 않는 블로그입니다"),
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "※ 존재하지 않는 분류입니다"),
    INVALID_CATEGORY(HttpStatus.BAD_REQUEST, "※ 분류를 다시 골라 주세요"),
    CATEGORY_NAME_DUPLICATED(HttpStatus.CONFLICT, "이미 있는 분류입니다"),
    DEFAULT_CATEGORY_NOT_DELETABLE(HttpStatus.CONFLICT, "※ 미분류는 삭제할 수 없습니다"),
    /** 문구의 글 개수는 서비스가 채운다. */
    CATEGORY_HAS_POSTS(HttpStatus.CONFLICT, "이 분류에 글이 %d개 있어 삭제할 수 없습니다. 글을 다른 분류로 옮긴 뒤 삭제해 주세요"),
    INVALID_CATEGORY_ORDER(HttpStatus.BAD_REQUEST, "※ 분류 목록이 바뀌었습니다. 새로 고친 뒤 다시 시도해 주세요"),
    /** 아래 칸별 문구의 숫자는 설정값에 따라 BlogFieldErrorMessages, PostFieldErrorMessages가 채운다. */
    TITLE_REQUIRED(HttpStatus.BAD_REQUEST, "제목을 입력해 주세요"),
    TITLE_TOO_LONG(HttpStatus.BAD_REQUEST, "※ 제목은 100자 이하로 입력해 주세요"),
    CONTENT_REQUIRED(HttpStatus.BAD_REQUEST, "본문을 입력해 주세요"),
    CONTENT_TOO_LONG(HttpStatus.BAD_REQUEST, "※ 본문은 10,000자 이하로 입력해 주세요"),
    CATEGORY_REQUIRED(HttpStatus.BAD_REQUEST, "※ 분류를 골라 주세요"),
    TOPIC_REQUIRED(HttpStatus.BAD_REQUEST, "주제를 골라 주세요"),
    VISIBILITY_INVALID(HttpStatus.BAD_REQUEST, "※ 공개 여부를 다시 골라 주세요"),
    BLOG_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "※ 블로그 이름을 입력해 주세요"),
    BLOG_NAME_TOO_LONG(HttpStatus.BAD_REQUEST, "※ 블로그 이름은 30자 이하로 입력해 주세요"),
    BLOG_INTRO_TOO_LONG(HttpStatus.BAD_REQUEST, "※ 소개는 200자 이하로 입력해 주세요"),
    CATEGORY_NAME_REQUIRED(HttpStatus.BAD_REQUEST, "※ 분류 이름을 입력해 주세요"),
    CATEGORY_NAME_TOO_LONG(HttpStatus.BAD_REQUEST, "※ 분류 이름은 20자 이하로 입력해 주세요"),

    // 글 탐색 (specs/004 contracts 2). 숫자는 설정값(explore.search.*)에 따라 PostSearchService가 채운다
    SEARCH_KEYWORD_TOO_SHORT(HttpStatus.BAD_REQUEST, "검색어를 %d자 이상 입력해 주세요"),
    SEARCH_KEYWORD_TOO_LONG(HttpStatus.BAD_REQUEST, "검색어는 %d자까지 입력할 수 있습니다"),

    // 댓글·좋아요·신고·태그·이미지 (specs/005 contracts). ※는 상세/05 안내 문구 표에 아직 없는 제안 문구다
    /** 칸별 문구. 글자 수는 설정값(community.comment.*)에 따라 CommentFieldErrorMessages가 채운다. */
    COMMENT_EMPTY(HttpStatus.BAD_REQUEST, "※ 댓글 내용을 입력해 주세요"),
    COMMENT_TOO_LONG(HttpStatus.BAD_REQUEST, "※ 댓글은 500자 이하로 입력해 주세요"),
    COMMENT_TOO_FREQUENT(HttpStatus.TOO_MANY_REQUESTS, "※ 잠시 뒤에 다시 등록해 주세요"),
    /** 없는 댓글, 볼 수 없는 글의 댓글, 지울 권한이 없는 남의 댓글이 모두 같은 응답이다 (2026-10-08 결정, 403을 쓰지 않음). */
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "※ 존재하지 않는 댓글입니다"),
    SELF_LIKE_NOT_ALLOWED(HttpStatus.FORBIDDEN, "※ 자기 글에는 좋아요를 누를 수 없습니다"),

    // 계정 관리 (specs/002 contracts 2 ~ 4)
    /** 401이 아니다. 401은 화면이 로그인 창을 띄우는 약속이다 (002 research B-2). */
    CURRENT_PASSWORD_MISMATCH(HttpStatus.BAD_REQUEST, "현재 비밀번호가 올바르지 않습니다"),
    PASSWORD_SAME_AS_CURRENT(HttpStatus.BAD_REQUEST, "현재 비밀번호와 다른 값을 입력해 주세요"),
    WITHDRAWAL_CONFLICT(HttpStatus.CONFLICT, "※ 잠시 뒤 다시 시도해 주세요"),
    /** 문구의 숫자는 설정값(account.intro.max-length)에 따라 AccountFieldErrorMessages가 채운다. */
    INTRO_TOO_LONG(HttpStatus.BAD_REQUEST, "※ 소개는 100자 이하로 입력해 주세요"),
    CURRENT_PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST, "※ 현재 비밀번호를 입력해 주세요"),
    PASSWORD_REQUIRED(HttpStatus.BAD_REQUEST, "※ 비밀번호를 입력해 주세요"),
    WITHDRAWAL_NOT_AGREED(HttpStatus.BAD_REQUEST, "※ 탈퇴 안내를 확인해 주세요");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus status() {
        return status;
    }

    public String message() {
        return message;
    }
}
