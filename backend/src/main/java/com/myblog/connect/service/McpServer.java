package com.myblog.connect.service;

import com.myblog.connect.service.McpTools.Caller;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 글결 MCP의 JSON-RPC 처리 (specs/020 research D-1). Streamable HTTP를 상태 없이(stateless) 쓴다: POST 하나에 메시지 하나,
 * 요청이면 JSON 응답 하나, 알림이면 빈 202. 세션을 두지 않아 앱 두 벌(a·b) 어느 쪽이 받아도 같다.
 * <ul>
 *   <li>메서드: initialize, ping, tools/list, tools/call. 그 밖은 -32601. 알림(notifications/*)과 클라이언트의 응답은 202.</li>
 *   <li>판(protocolVersion): 클라이언트가 고른 판이 우리가 아는 판이면 그대로, 아니면 기본 판.</li>
 * </ul>
 */
@Service
public class McpServer {

    static final List<String> PROTOCOL_VERSIONS = List.of("2025-06-18", "2025-03-26", "2025-11-25");
    static final String DEFAULT_VERSION = PROTOCOL_VERSIONS.get(0);

    static final int PARSE_ERROR = -32700;
    static final int INVALID_REQUEST = -32600;
    static final int METHOD_NOT_FOUND = -32601;
    static final int INVALID_PARAMS = -32602;

    private static final String INSTRUCTIONS = """
            글결(geulgyeol)은 원고지 콘셉트의 한국어 블로그 서비스입니다. 이 연결로 사용자의 원고와 글결의 공개 글을 찾고 읽을 수 있습니다.
            - 질문에 답할 근거가 필요하면 search_passages로 단락을 찾고, 답에 인용할 때는 [글 제목](주소)로 출처를 붙이세요.
            - 글 전체가 필요하면 read_post, 사용자의 글 목록과 통계는 list_my_posts·my_blog_stats를 쓰세요.
            - 글을 대신 올릴 수는 없습니다. save_draft(열쇠가 허락할 때)는 임시 저장까지만 하고, 발행은 사용자가 화면에서 합니다.
            - 사용자 자신의 비공개 글도 보이므로, 사용자가 원하지 않으면 남에게 옮기지 마세요.""";

    private final McpTools tools;
    private final String release;

    public McpServer(McpTools tools, @Value("${myblog.release}") String release) {
        this.tools = tools;
        this.release = release;
    }

    /** status는 HTTP 상태, body는 보낼 JSON(없으면 빈 몸). */
    public record Outcome(int status, Map<String, Object> body) {
    }

    /** 몸이 JSON이 아니었다 (HTTP 400, -32700). */
    public Outcome parseError() {
        return new Outcome(400, error(null, PARSE_ERROR, "JSON을 읽을 수 없습니다"));
    }

    public Outcome handle(Object message, Caller caller) {
        if (!(message instanceof Map<?, ?> raw) || !"2.0".equals(raw.get("jsonrpc"))) {
            // 여러 메시지 묶음(배열)은 2025-06-18 판에서 빠졌다
            return new Outcome(400, error(null, INVALID_REQUEST, "JSON-RPC 2.0 메시지 하나를 보내 주세요"));
        }
        @SuppressWarnings("unchecked")
        Map<String, Object> rpc = (Map<String, Object>) raw;
        Object id = rpc.get("id");
        Object method = rpc.get("method");
        if (method == null) {
            // 클라이언트가 보낸 응답(result/error): 받기만 한다
            return rpc.containsKey("result") || rpc.containsKey("error") ? new Outcome(202, null)
                    : new Outcome(400, error(id, INVALID_REQUEST, "method가 없습니다"));
        }
        if (!(method instanceof String name)) {
            return new Outcome(400, error(null, INVALID_REQUEST, "method 모양이 틀렸습니다"));
        }
        if (!rpc.containsKey("id")) {
            return new Outcome(202, null);  // 알림 (notifications/initialized 등)
        }
        // MCP: 요청 id는 글자나 정수여야 하고 null이면 안 된다
        if (!(id instanceof String || id instanceof Integer || id instanceof Long || id instanceof BigInteger)) {
            return new Outcome(400, error(null, INVALID_REQUEST, "id는 글자나 정수여야 합니다"));
        }
        Map<String, Object> params = rpc.get("params") instanceof Map<?, ?> p ? cast(p) : Map.of();
        return switch (name) {
            case "initialize" -> new Outcome(200, result(id, initialize(params)));
            case "ping" -> new Outcome(200, result(id, Map.of()));
            case "tools/list" -> new Outcome(200, result(id, McpTools.ordered("tools", tools.definitions(caller.grant()))));
            case "tools/call" -> new Outcome(200, callTool(id, params, caller));
            default -> new Outcome(200, error(id, METHOD_NOT_FOUND, "모르는 메서드입니다: " + name));
        };
    }

    private Map<String, Object> initialize(Map<String, Object> params) {
        Object asked = params.get("protocolVersion");
        String version = asked instanceof String v && PROTOCOL_VERSIONS.contains(v) ? v : DEFAULT_VERSION;
        return McpTools.ordered(
                "protocolVersion", version,
                "capabilities", McpTools.ordered("tools", McpTools.ordered("listChanged", false)),
                "serverInfo", McpTools.ordered("name", "geulgyeol", "title", "글결", "version", release),
                "instructions", INSTRUCTIONS);
    }

    private Map<String, Object> callTool(Object id, Map<String, Object> params, Caller caller) {
        if (!(params.get("name") instanceof String toolName)) {
            return error(id, INVALID_PARAMS, "도구 이름(name)이 필요합니다");
        }
        Map<String, Object> args = params.get("arguments") instanceof Map<?, ?> a ? cast(a) : Map.of();
        Optional<Map<String, Object>> called = tools.call(toolName, args, caller);
        return called.map(value -> result(id, value))
                .orElseGet(() -> error(id, INVALID_PARAMS, "모르는 도구입니다: " + toolName));
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> cast(Map<?, ?> map) {
        return (Map<String, Object>) map;
    }

    private static Map<String, Object> result(Object id, Map<String, Object> result) {
        return McpTools.ordered("jsonrpc", "2.0", "id", id, "result", result);
    }

    static Map<String, Object> error(Object id, int code, String message) {
        return McpTools.ordered("jsonrpc", "2.0", "id", id, "error", McpTools.ordered("code", code, "message", message));
    }
}
