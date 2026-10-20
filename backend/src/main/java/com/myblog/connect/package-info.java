/**
 * AI 비서 연결 모듈 (specs/020): 글결 MCP 서버. 개인 열쇠로 붙은 AI 앱(MCP 클라이언트)이 내 블로그의 원고를 찾고(RAG의 "찾기")
 * 읽고, 내 통계를 보고, 임시 저장까지 한다. 맨 위 모듈이라 다른 모듈의 맨 위 타입(PostLibrary, BlogStatsReader,
 * LoggedInMember, AccessKeyGrant)만 부른다. 아래 모듈은 이 모듈을 모른다. 열쇠 확인은 회원 모듈의 MCP 보안 사슬이 한다.
 */
@org.springframework.modulith.ApplicationModule(displayName = "AI 비서 연결 (MCP)",
        allowedDependencies = {"post", "stats", "user", "common"})
package com.myblog.connect;
