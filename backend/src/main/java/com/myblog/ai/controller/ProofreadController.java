package com.myblog.ai.controller;

import com.myblog.ai.service.ProofreadService;
import com.myblog.ai.service.ProofreadService.Result;
import com.myblog.ai.service.ProofreadService.Status;
import com.myblog.user.LoggedInMember;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 빨간 펜 교정 (specs/014 contracts 1). 로그인한 회원만. */
@RestController
@Tag(name = "13. AI")
public class ProofreadController {

    private final ProofreadService proofreadService;
    private final LoggedInMember loggedInMember;

    public ProofreadController(ProofreadService proofreadService, LoggedInMember loggedInMember) {
        this.proofreadService = proofreadService;
        this.loggedInMember = loggedInMember;
    }

    public record ProofreadRequest(String content) {
    }

    @Operation(summary = "빨간 펜 상태",
            description = "글쓰기 화면이 단추를 그릴지 정한다. AI가 꺼져 있으면 enabled가 false. 오늘 남은 횟수(한국 날짜)와 본문 한도를 준다.")
    @GetMapping("/api/{version}/me/proofread")
    public Status status(@Parameter(hidden = true) Authentication authentication) {
        return proofreadService.status(loggedInMember.requireIdOf(authentication));
    }

    @Operation(summary = "빨간 펜 교정받기",
            description = "본문을 AI에게 교정받는다. 고칠 곳마다 본문의 위치(UTF-16, 자바스크립트 문자열 위치)와 종류(spacing·spelling·style),"
                    + " 고친 말, 이유를 준다. 서버가 위치를 본문에서 다시 찾아 검사하고 코드·링크 주소는 고치지 않는다."
                    + " 같은 본문은 앞 결과를 다시 쓰고 횟수에 넣지 않는다. 하루 횟수를 넘으면 429, AI가 실패하면 503.")
    @PostMapping("/api/{version}/me/proofread")
    public Result proofread(@RequestBody ProofreadRequest request, @Parameter(hidden = true) Authentication authentication) {
        return proofreadService.proofread(loggedInMember.requireIdOf(authentication), request.content());
    }
}
