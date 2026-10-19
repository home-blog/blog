package com.myblog.stats.controller;

import com.myblog.stats.service.SearchTrends;
import com.myblog.stats.service.SearchTrends.Trend;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** 실시간 인기 검색어 (specs/015 contracts 1). 로그인 없이 부른다(SecurityConfig의 GET /api/v1/search/**). */
@Tag(name = "6. 둘러보기·검색")
@RestController
public class SearchTrendController {

    private final SearchTrends trends;

    public SearchTrendController(SearchTrends trends) {
        this.trends = trends;
    }

    public record TrendingResponse(List<Trend> items) {
    }

    @Operation(summary = "실시간 인기 검색어",
            description = "최근 1시간 동안 결과가 있었던 검색어 10개. change는 10분 전과 견준 변화: up·down(delta칸), new, same.")
    @GetMapping("/api/{version}/search/trending")
    public TrendingResponse trending() {
        return new TrendingResponse(trends.trending());
    }
}
