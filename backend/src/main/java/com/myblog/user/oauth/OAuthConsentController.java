package com.myblog.user.oauth;

import com.myblog.user.LoggedInMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 위임장(허락·거절)과 연결된 앱 (specs/023 contracts 3·5). 세션 로그인 + CSRF. */
@Tag(name = "19. AI 앱 연결(OAuth)")
@RestController
public class OAuthConsentController {

    private final OAuthService oauth;
    private final LoggedInMember loggedInMember;

    public OAuthConsentController(OAuthService oauth, LoggedInMember loggedInMember) {
        this.oauth = oauth;
        this.loggedInMember = loggedInMember;
    }

    public record ApproveRequest(String scope) {
    }

    public record Redirect(String redirectTo) {
    }

    @Operation(summary = "위임장에 보일 연결 요청")
    @GetMapping("/api/{version}/oauth/requests/{requestId}")
    public OAuthService.RequestView request(@PathVariable String requestId, Authentication authentication) {
        loggedInMember.requireIdOf(authentication);
        return oauth.describe(requestId);
    }

    @Operation(summary = "허락", description = "앱으로 돌아갈 주소(허락 코드·state·iss)를 돌려준다")
    @PostMapping("/api/{version}/oauth/requests/{requestId}/approve")
    public Redirect approve(@PathVariable String requestId, @RequestBody(required = false) ApproveRequest body,
            Authentication authentication) {
        Long memberId = loggedInMember.requireIdOf(authentication);
        return new Redirect(oauth.approve(memberId, requestId, body == null ? null : body.scope()));
    }

    @Operation(summary = "거절", description = "앱으로 돌아갈 주소(error=access_denied)를 돌려준다")
    @PostMapping("/api/{version}/oauth/requests/{requestId}/deny")
    public Redirect deny(@PathVariable String requestId, Authentication authentication) {
        loggedInMember.requireIdOf(authentication);
        return new Redirect(oauth.deny(requestId));
    }

    @Operation(summary = "연결된 앱")
    @GetMapping("/api/{version}/me/connected-apps")
    public OAuthService.ConnectedApps connectedApps(Authentication authentication) {
        return oauth.connectedApps(loggedInMember.requireIdOf(authentication));
    }

    @Operation(summary = "연결 끊기")
    @DeleteMapping("/api/{version}/me/connected-apps/{grantId}")
    public ResponseEntity<Void> disconnect(@PathVariable long grantId, Authentication authentication) {
        oauth.disconnect(loggedInMember.requireIdOf(authentication), grantId);
        return ResponseEntity.noContent().build();
    }
}
