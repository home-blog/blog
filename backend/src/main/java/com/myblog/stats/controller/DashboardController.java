package com.myblog.stats.controller;

import com.myblog.stats.service.DashboardService;
import com.myblog.stats.service.DashboardService.Dashboard;
import com.myblog.user.LoggedInMember;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 대시보드 (specs/006 contracts 2, T047, FR-006 ~ FR-011). 내 블로그는 세션으로만 정한다. */
@RestController
public class DashboardController {

    private final LoggedInMember loggedInMember;
    private final DashboardService dashboardService;

    public DashboardController(LoggedInMember loggedInMember, DashboardService dashboardService) {
        this.loggedInMember = loggedInMember;
        this.dashboardService = dashboardService;
    }

    @GetMapping("/api/manage/dashboard")
    public Dashboard dashboard(Authentication authentication) {
        return dashboardService.dashboard(loggedInMember.requireIdOf(authentication));
    }
}
