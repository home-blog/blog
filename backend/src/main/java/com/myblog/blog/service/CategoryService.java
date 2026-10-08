package com.myblog.blog.service;

import com.myblog.blog.CategoryPostCounter;
import com.myblog.blog.controller.dto.CategoryRequests;
import com.myblog.blog.domain.Blog;
import com.myblog.blog.domain.Category;
import com.myblog.blog.repository.BlogRepository;
import com.myblog.blog.repository.CategoryRepository;
import com.myblog.blog.service.BlogQueryService.CategoryView;
import com.myblog.common.error.ApiException;
import com.myblog.common.error.ErrorCode;
import com.myblog.common.error.ErrorResponse.FieldErrorItem;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * 분류 관리 (specs/003 contracts 5 ~ 8, FR-034 ~ FR-043, FR-047). 내 블로그는 세션의 회원으로만 정한다.
 * <ul>
 *   <li>이름은 블로그 안에서 소문자로 맞춰 겹치면 안 된다. 서버가 먼저 보고, 거의 동시에 오면 DB의 uq_category_blog_name이
 *       마지막 방어선이다 (SC-006). 자기 자신과는 비교하지 않는다.</li>
 *   <li>남의 분류 번호는 없는 분류와 같은 CATEGORY_NOT_FOUND. 고치기 본문은 내 분류인지 본 뒤에 검사한다.</li>
 *   <li>글이 있는 분류와 미분류는 지울 수 없다. 세고 지우는 사이에 글이 들어오면 외래 키가 막고, 다시 세어 같은 오류로 답한다 (R-3).</li>
 * </ul>
 * DB가 거절한 트랜잭션은 다시 쓸 수 없으므로, 저장과 다시 확인을 각각 따로 된 트랜잭션에서 한다.
 */
@Service
public class CategoryService {

    private final BlogRepository blogs;
    private final CategoryRepository categories;
    private final CategoryPostCounter postCounter;
    private final Validator validator;
    private final TransactionTemplate transaction;

    public CategoryService(BlogRepository blogs, CategoryRepository categories, CategoryPostCounter postCounter,
            Validator validator, PlatformTransactionManager transactionManager) {
        this.blogs = blogs;
        this.categories = categories;
        this.postCounter = postCounter;
        this.validator = validator;
        this.transaction = new TransactionTemplate(transactionManager);
    }

    /** 분류 추가: 목록 맨 아래 (FR-037). */
    public CategoryView create(Long memberId, CategoryRequests.Create request) {
        try {
            return transaction.execute(status -> {
                Blog blog = myBlog(memberId);
                checkDuplicate(blog.getId(), request.name(), null);
                Category category = categories.saveAndFlush(Category.create(blog.getId(), request.name(),
                        categories.maxSortOrder(blog.getId()) + 1, request.visibility()));
                return CategoryView.of(category, 0);
            });
        } catch (DataIntegrityViolationException e) {
            throw duplicated();
        }
    }

    /** 이름·공개 여부 바꾸기: 보낸 칸만 (contracts 6). */
    public CategoryView update(Long memberId, Long categoryId, CategoryRequests.Update request) {
        try {
            return transaction.execute(status -> {
                Category category = myCategory(memberId, categoryId);
                validate(request);
                if (request.name() != null) {
                    checkDuplicate(category.getBlogId(), request.name(), category.getId());
                    category.rename(request.name());
                }
                if (request.visibility() != null) {
                    category.changeVisibility(request.visibility());
                }
                categories.flush();
                return CategoryView.of(category, postCounter.countAll(category.getId()));
            });
        } catch (DataIntegrityViolationException e) {
            throw duplicated();
        }
    }

    /** 순서 바꾸기: 내 분류 번호가 빠짐없이 한 번씩 있어야 하고, 1, 2, 3 …으로 다시 매긴다 (FR-039). */
    public List<CategoryView> reorder(Long memberId, CategoryRequests.Order request) {
        return transaction.execute(status -> {
            Blog blog = myBlog(memberId);
            List<Category> mine = categories.findByBlogIdOrderBySortOrderAscIdAsc(blog.getId());
            List<Long> ids = request.categoryIds() == null ? List.of() : request.categoryIds();
            Set<Long> mineIds = mine.stream().map(Category::getId).collect(Collectors.toSet());
            if (ids.size() != mine.size() || !new HashSet<>(ids).equals(mineIds)) {
                throw new ApiException(ErrorCode.INVALID_CATEGORY_ORDER);
            }
            Map<Long, Category> byId = mine.stream().collect(Collectors.toMap(Category::getId, Function.identity()));
            for (int i = 0; i < ids.size(); i++) {
                byId.get(ids.get(i)).moveTo(i + 1);
            }
            categories.flush();
            Map<Long, Long> counts = postCounter.countAll(ids);
            return ids.stream().map(id -> CategoryView.of(byId.get(id), counts.getOrDefault(id, 0L))).toList();
        });
    }

    /** 분류 삭제 (contracts 8). */
    public void delete(Long memberId, Long categoryId) {
        try {
            transaction.executeWithoutResult(status -> {
                Category category = myCategory(memberId, categoryId);
                if (category.isDefaultCategory()) {
                    throw new ApiException(ErrorCode.DEFAULT_CATEGORY_NOT_DELETABLE);
                }
                long count = postCounter.countAll(category.getId());
                if (count > 0) {
                    throw hasPosts(count);
                }
                categories.delete(category);
                categories.flush();
            });
        } catch (DataIntegrityViolationException e) {
            // 센 뒤에 글이 들어와 외래 키가 막았다 (R-3)
            Long count = transaction.execute(status -> postCounter.countAll(categoryId));
            throw hasPosts(count == null ? 0 : count);
        }
    }

    private Blog myBlog(Long memberId) {
        return blogs.findByOwnerId(memberId).orElseThrow(() -> new ApiException(ErrorCode.BLOG_NOT_FOUND));
    }

    /** 내 블로그의 분류만. 남의 분류도 없는 분류와 같은 404 (FR-035). */
    private Category myCategory(Long memberId, Long categoryId) {
        Blog blog = myBlog(memberId);
        return categories.findById(categoryId)
                .filter(category -> category.getBlogId().equals(blog.getId()))
                .orElseThrow(() -> new ApiException(ErrorCode.CATEGORY_NOT_FOUND));
    }

    private void validate(CategoryRequests.Update request) {
        Set<ConstraintViolation<CategoryRequests.Update>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            throw new ConstraintViolationException(violations);
        }
    }

    private void checkDuplicate(Long blogId, String name, Long excludeId) {
        if (categories.existsSameName(blogId, name.strip(), excludeId)) {
            throw duplicated();
        }
    }

    private static ApiException duplicated() {
        return new ApiException(ErrorCode.CATEGORY_NAME_DUPLICATED, ErrorCode.CATEGORY_NAME_DUPLICATED.message(),
                List.of(new FieldErrorItem("name", ErrorCode.CATEGORY_NAME_DUPLICATED.name(),
                        ErrorCode.CATEGORY_NAME_DUPLICATED.message())));
    }

    private static ApiException hasPosts(long count) {
        return new ApiException(ErrorCode.CATEGORY_HAS_POSTS, ErrorCode.CATEGORY_HAS_POSTS.message().formatted(count));
    }
}
