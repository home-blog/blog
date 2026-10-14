package com.myblog.stats.controller;

import com.myblog.common.config.ManageProperties;
import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.stats.service.StatsQueryService;
import com.myblog.stats.service.StatsQueryService.DailyStat;
import com.myblog.user.LoggedInMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 통계 (specs/006 contracts 7, T042, FR-031, FR-038). 기간은 manage.stats.period-options(7, 30)에 있는 값만, 없으면 기본(30).
 * 유입 경로·시간대·기기 정보는 없다 (BM-06-8 확인 필요, 원본대로 저장도 하지 않음).
 */
@Tag(name = "9. 블로그 관리")
@RestController
public class StatsController {

    private final LoggedInMember loggedInMember;
    private final StatsQueryService statsQuery;
    private final ManageProperties.Stats properties;

    public StatsController(LoggedInMember loggedInMember, StatsQueryService statsQuery, ManageProperties properties) {
        this.loggedInMember = loggedInMember;
        this.statsQuery = statsQuery;
        this.properties = properties.stats();
    }

    @Operation(summary = "통계", description = "고를 수 있는 기간(`manage.stats.period-options`) 중 days의 날짜별 방문자·조회수와 글별 조회수.")
    @GetMapping("/api/{version}/manage/stats")
    public StatsResponse stats(Authentication authentication, @RequestParam(required = false) Integer days) {
        Long memberId = loggedInMember.requireIdOf(authentication);
        int period = days == null ? properties.periodDefault() : days;
        if (!properties.periodOptions().contains(period)) {
            throw new ApiException(ErrorCode.VALIDATION_FAILED);
        }
        return new StatsResponse(period, statsQuery.daily(memberId, period));
    }

    public record StatsResponse(int days, List<DailyStat> daily) {
    }
}
