package com.myblog.stats.controller;

import com.myblog.stats.service.DashboardService;
import com.myblog.stats.service.DashboardService.Dashboard;
import com.myblog.user.LoggedInMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 대시보드 (specs/006 contracts 2, T047, FR-006 ~ FR-011). 내 블로그는 세션으로만 정한다. */
@Tag(name = "9. 블로그 관리")
@RestController
public class DashboardController {

    private final LoggedInMember loggedInMember;
    private final DashboardService dashboardService;

    public DashboardController(LoggedInMember loggedInMember, DashboardService dashboardService) {
        this.loggedInMember = loggedInMember;
        this.dashboardService = dashboardService;
    }

    @Operation(summary = "대시보드", description = "오늘·누적 방문자와 조회수, 인기 글, 최근 글, 새 댓글, 최근 그래프.")
    @GetMapping("/api/{version}/manage/dashboard")
    public Dashboard dashboard(Authentication authentication) {
        return dashboardService.dashboard(loggedInMember.requireIdOf(authentication));
    }
}
