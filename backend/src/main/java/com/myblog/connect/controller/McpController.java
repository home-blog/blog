package com.myblog.connect.controller;

import com.myblog.common.error.ErrorCode;
import com.myblog.connect.service.McpRateLimiter;
import com.myblog.connect.service.McpServer;
import com.myblog.connect.service.McpServer.Outcome;
import com.myblog.connect.service.McpTools.Caller;
import com.myblog.user.AccessKeyGrant;
import com.myblog.user.LoggedInMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

/**
 * 글결 MCP 주소 (specs/020 contracts 2). 개인 열쇠로만 들어온다(회원 모듈의 MCP 보안 사슬).
 * POST만 받는다: 서버가 먼저 보내는 흐름(GET)과 세션 끊기(DELETE)는 쓰지 않아 405.
 */
@Tag(name = "17. AI 비서 연결 (MCP)")
@RestController
public class McpController {

    private final McpServer server;
    private final McpRateLimiter rateLimiter;
    private final LoggedInMember loggedInMember;
    private final JsonMapper jsonMapper;

    public McpController(McpServer server, McpRateLimiter rateLimiter, LoggedInMember loggedInMember,
            JsonMapper jsonMapper) {
        this.server = server;
        this.rateLimiter = rateLimiter;
        this.loggedInMember = loggedInMember;
        this.jsonMapper = jsonMapper;
    }

    @Operation(summary = "MCP 메시지 (JSON-RPC 2.0)",
            description = "Authorization: Bearer <개인 열쇠>. initialize·ping·tools/list·tools/call. 알림은 202.")
    @PostMapping(path = "/api/{version}/mcp", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> message(@RequestBody(required = false) String body, Authentication authentication) {
        Long memberId = loggedInMember.requireIdOf(authentication);
        AccessKeyGrant grant = AccessKeyGrant.of(authentication).orElseThrow();
        if (!rateLimiter.allow(grant.keyId())) {
            return json(new Outcome(HttpStatus.TOO_MANY_REQUESTS.value(), Map.of("jsonrpc", "2.0",
                    "error", Map.of("code", -32000, "message", ErrorCode.MCP_TOO_MANY_REQUESTS.message()))));
        }
        Object message;
        try {
            message = body == null || body.isBlank() ? null : jsonMapper.readValue(body, Object.class);
        } catch (JacksonException e) {
            return json(server.parseError());
        }
        String baseUrl = ServletUriComponentsBuilder.fromCurrentContextPath().build().toUriString();
        return json(server.handle(message, new Caller(memberId, grant, baseUrl)));
    }

    @GetMapping("/api/{version}/mcp")
    public ResponseEntity<Void> stream() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).header(HttpHeaders.ALLOW, "POST").build();
    }

    @DeleteMapping("/api/{version}/mcp")
    public ResponseEntity<Void> endSession() {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED).header(HttpHeaders.ALLOW, "POST").build();
    }

    private ResponseEntity<String> json(Outcome outcome) {
        if (outcome.body() == null) {
            return ResponseEntity.status(outcome.status()).build();
        }
        return ResponseEntity.status(outcome.status()).contentType(MediaType.APPLICATION_JSON)
                .body(jsonMapper.writeValueAsString(outcome.body()));
    }
}
