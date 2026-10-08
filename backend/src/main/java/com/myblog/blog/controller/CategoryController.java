package com.myblog.blog.controller;

import com.myblog.blog.controller.dto.CategoryRequests;
import com.myblog.blog.service.BlogQueryService.CategoryView;
import com.myblog.blog.service.CategoryService;
import com.myblog.user.LoggedInMember;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 분류 관리 (specs/003 contracts 5 ~ 8). 모두 로그인해야 하고, 내 블로그는 세션으로만 정한다 (주소에 블로그 번호가 없다).
 * 분류 목록 보기는 BlogController의 GET /api/blogs/{blogId}/categories를 쓴다.
 */
@RestController
@RequestMapping("/api/me/blog/categories")
public class CategoryController {

    private final LoggedInMember loggedInMember;
    private final CategoryService categoryService;

    public CategoryController(LoggedInMember loggedInMember, CategoryService categoryService) {
        this.loggedInMember = loggedInMember;
        this.categoryService = categoryService;
    }

    /** 분류 추가 (contracts 5). */
    @PostMapping
    public ResponseEntity<CategoryView> create(Authentication authentication, @Valid @RequestBody CategoryRequests.Create request) {
        CategoryView created = categoryService.create(loggedInMember.requireIdOf(authentication), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    /** 이름·공개 여부 바꾸기 (contracts 6). 본문은 내 분류인지 본 뒤에 서비스가 검사한다 (@Valid 없음). */
    @PatchMapping("/{categoryId}")
    public CategoryView update(@PathVariable Long categoryId, Authentication authentication,
            @RequestBody CategoryRequests.Update request) {
        return categoryService.update(loggedInMember.requireIdOf(authentication), categoryId, request);
    }

    /** 순서 바꾸기 (contracts 7). 응답은 새 순서의 분류 목록 (주인 기준 개수). */
    @PutMapping("/order")
    public CategoriesResponse reorder(Authentication authentication, @RequestBody CategoryRequests.Order request) {
        return new CategoriesResponse(categoryService.reorder(loggedInMember.requireIdOf(authentication), request));
    }

    /** 분류 삭제 (contracts 8). */
    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> delete(@PathVariable Long categoryId, Authentication authentication) {
        categoryService.delete(loggedInMember.requireIdOf(authentication), categoryId);
        return ResponseEntity.noContent().build();
    }

    public record CategoriesResponse(List<CategoryView> categories) {
    }
}
