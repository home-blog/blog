package com.myblog.community.controller;

import com.myblog.community.controller.dto.ReportRequest;
import com.myblog.community.service.ReportService;
import com.myblog.user.LoggedInMember;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** 글 신고 (specs/005 contracts 6). 회원 번호는 LoggedInMember로만 얻는다. */
@RestController
public class ReportController {

    private final LoggedInMember loggedInMember;
    private final ReportService reportService;

    public ReportController(LoggedInMember loggedInMember, ReportService reportService) {
        this.loggedInMember = loggedInMember;
        this.reportService = reportService;
    }

    @PostMapping("/api/posts/{postId}/reports")
    @ResponseStatus(HttpStatus.CREATED)
    public Received report(@PathVariable Long postId, Authentication authentication, @RequestBody ReportRequest request) {
        reportService.report(loggedInMember.requireIdOf(authentication), postId, request);
        return new Received("신고가 접수되었습니다");
    }

    public record Received(String message) {
    }
}
