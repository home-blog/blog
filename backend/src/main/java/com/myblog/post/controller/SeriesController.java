package com.myblog.post.controller;

import com.myblog.post.service.SeriesService;
import com.myblog.post.service.SeriesService.PostSeries;
import com.myblog.post.service.SeriesService.SeriesDetail;
import com.myblog.post.service.SeriesService.SeriesSummary;
import com.myblog.user.LoggedInMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 연재 (specs/013 US2, contracts 2). 읽기는 누구나(SecurityConfig), 쓰기는 로그인한 주인만. 남의 것은 404.
 * 이름·소개 검사는 서비스가 주인을 확인한 뒤에 한다 (남의 연재면 형식 오류보다 404가 먼저).
 */
@Tag(name = "12. 글쓰기 도구")
@RestController
public class SeriesController {

    private final LoggedInMember loggedInMember;
    private final SeriesService seriesService;

    public SeriesController(LoggedInMember loggedInMember, SeriesService seriesService) {
        this.loggedInMember = loggedInMember;
        this.seriesService = seriesService;
    }

    @Operation(summary = "블로그의 연재 목록", description = "만든 순서. postCount는 보는 사람에게 보이는 글 수.")
    @GetMapping("/api/{version}/blogs/{blogId}/series")
    public SeriesList list(@PathVariable Long blogId, @Parameter(hidden = true) Authentication authentication) {
        return new SeriesList(seriesService.list(blogId, viewer(authentication)));
    }

    @Operation(summary = "연재 한 개", description = "보이는 글만 화 순서로. 화 번호(episode)는 보이는 글로 센다. 주인에게는 visibility도 준다.")
    @GetMapping("/api/{version}/series/{seriesId}")
    public SeriesDetail detail(@PathVariable Long seriesId, @Parameter(hidden = true) Authentication authentication) {
        return seriesService.detail(seriesId, viewer(authentication));
    }

    @Operation(summary = "글의 연재 정보", description = "몇 화인지, 이전·다음 화, 연재 전체 목록. 연재에 없으면 series가 null. 볼 수 없는 글이면 404.")
    @GetMapping("/api/{version}/posts/{postId}/series")
    public PostSeriesResponse ofPost(@PathVariable Long postId, @Parameter(hidden = true) Authentication authentication) {
        return new PostSeriesResponse(seriesService.ofPost(postId, viewer(authentication)));
    }

    @Operation(summary = "연재 만들기", description = "이름 `series.name.*`(1~30자), 소개 `series.description.max-length`(200자). 블로그마다 `series.max-per-blog`개.")
    @PostMapping("/api/{version}/me/blog/series")
    @ResponseStatus(HttpStatus.CREATED)
    public SeriesDetail create(@RequestBody SeriesRequest request, @Parameter(hidden = true) Authentication authentication) {
        return seriesService.create(loggedInMember.requireIdOf(authentication), request.name(), request.description());
    }

    @Operation(summary = "연재 이름·소개 고치기")
    @PutMapping("/api/{version}/me/blog/series/{seriesId}")
    public SeriesDetail update(@PathVariable Long seriesId, @RequestBody SeriesRequest request, @Parameter(hidden = true) Authentication authentication) {
        return seriesService.update(loggedInMember.requireIdOf(authentication), seriesId, request.name(), request.description());
    }

    @Operation(summary = "연재 지우기", description = "연재만 지운다. 글은 남는다.")
    @DeleteMapping("/api/{version}/me/blog/series/{seriesId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long seriesId, @Parameter(hidden = true) Authentication authentication) {
        seriesService.delete(loggedInMember.requireIdOf(authentication), seriesId);
    }

    @Operation(summary = "연재에 글 넣기", description = "끝 화로 넣는다. 다른 연재에 있었으면 옮긴다. 연재마다 `series.max-posts`개.")
    @PutMapping("/api/{version}/me/blog/series/{seriesId}/posts/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void addPost(@PathVariable Long seriesId, @PathVariable Long postId, @Parameter(hidden = true) Authentication authentication) {
        seriesService.addPost(loggedInMember.requireIdOf(authentication), seriesId, postId);
    }

    @Operation(summary = "연재에서 글 빼기")
    @DeleteMapping("/api/{version}/me/blog/series/{seriesId}/posts/{postId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removePost(@PathVariable Long seriesId, @PathVariable Long postId, @Parameter(hidden = true) Authentication authentication) {
        seriesService.removePost(loggedInMember.requireIdOf(authentication), seriesId, postId);
    }

    @Operation(summary = "연재 글 순서 바꾸기", description = "그 연재의 글 번호를 모두, 한 번씩 보낸다. 아니면 400 SERIES_ORDER_MISMATCH.")
    @PutMapping("/api/{version}/me/blog/series/{seriesId}/order")
    public SeriesDetail reorder(@PathVariable Long seriesId, @RequestBody OrderRequest request, @Parameter(hidden = true) Authentication authentication) {
        return seriesService.reorder(loggedInMember.requireIdOf(authentication), seriesId, request.postIds());
    }

    private Long viewer(Authentication authentication) {
        return loggedInMember.idOf(authentication).orElse(null);
    }

    public record SeriesRequest(String name, String description) {
    }

    public record OrderRequest(List<Long> postIds) {
    }

    public record SeriesList(List<SeriesSummary> items) {
    }

    public record PostSeriesResponse(PostSeries series) {
    }
}
