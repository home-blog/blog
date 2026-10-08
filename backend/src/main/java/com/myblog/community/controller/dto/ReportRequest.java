package com.myblog.community.controller.dto;

import com.myblog.community.validation.ValidReportDetail;
import com.myblog.community.validation.ValidReportReason;

/**
 * 글 신고 요청 (specs/005 contracts 6). 신고한 사람·글 번호는 본문에서 받지 않는다.
 * 컨트롤러에서 @Valid로 검사하지 않는다: 볼 수 없는 글(404)과 자기 글(403)을 입력 오류보다 먼저 알려야 하므로
 * 서비스가 그 뒤에 검사한다 (research B-1).
 */
public record ReportRequest(@ValidReportReason String reason, @ValidReportDetail String detail) {
}
