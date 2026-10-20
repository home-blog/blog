package com.myblog.user.controller;

import com.myblog.user.LoggedInMember;
import com.myblog.user.service.AccessKeyService;
import com.myblog.user.service.AccessKeyService.Issued;
import com.myblog.user.service.AccessKeyService.KeyList;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 개인 열쇠 (specs/020 contracts 1). 세션으로 로그인한 회원만. 열쇠로는 이 주소를 쓸 수 없다(열쇠는 MCP 주소 전용).
 */
@Tag(name = "17. AI 비서 연결 (MCP)")
@RestController
public class AccessKeyController {

    private final LoggedInMember loggedInMember;
    private final AccessKeyService service;

    public AccessKeyController(LoggedInMember loggedInMember, AccessKeyService service) {
        this.loggedInMember = loggedInMember;
        this.service = service;
    }

    @Operation(summary = "내 개인 열쇠 목록", description = "원문은 다시 보여 주지 않는다. 앞 8글자(hint)만.")
    @GetMapping("/api/{version}/me/access-keys")
    public KeyList list(Authentication authentication) {
        return service.list(loggedInMember.requireIdOf(authentication));
    }

    @Operation(summary = "개인 열쇠 만들기", description = "token은 이 응답에만 실린다. 회원당 5개, 기한 30·90·365일.")
    @PostMapping("/api/{version}/me/access-keys")
    @ResponseStatus(HttpStatus.CREATED)
    public CreatedKey create(@RequestBody CreateKey request, Authentication authentication) {
        Issued issued = service.create(loggedInMember.requireIdOf(authentication), request.name(), request.scope(),
                request.days());
        var key = issued.key();
        return new CreatedKey(key.keyId(), key.name(), key.hint(), key.scope(), key.createdAt(), key.expiresAt(),
                issued.token());
    }

    @Operation(summary = "개인 열쇠 끊기", description = "끊은 열쇠는 바로 쓸 수 없다. 남의 열쇠는 404.")
    @DeleteMapping("/api/{version}/me/access-keys/{keyId}")
    public ResponseEntity<Void> revoke(@PathVariable Long keyId, Authentication authentication) {
        service.revoke(loggedInMember.requireIdOf(authentication), keyId);
        return ResponseEntity.noContent().build();
    }

    /** scope: read(찾기·읽기) · draft(+ 임시 저장). days: 30 · 90 · 365. */
    public record CreateKey(String name, String scope, Integer days) {
    }

    /** 만든 열쇠. token은 이때 한 번만. */
    public record CreatedKey(Long keyId, String name, String hint, String scope, Instant createdAt, Instant expiresAt,
            String token) {
    }
}
