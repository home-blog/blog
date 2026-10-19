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

    /** aiConsent: 화면에서 "본문이 AI로 보내진다"에 동의했는지. 바깥 AI(mode ai)일 때 거짓이면 409 */
    public record ProofreadRequest(String content, Boolean aiConsent) {
    }

    @Operation(summary = "빨간 펜 상태",
            description = "글쓰기 화면이 단추를 그릴지 정한다. mode는 ai(바깥 AI, 보내기 동의·하루 횟수), rules(서버 안의 맞춤법 규칙, 무료·횟수 없음),"
                    + " off(enabled false). 오늘 남은 횟수(한국 날짜, rules는 늘 하루 한도)와 본문 한도를 준다.")
    @GetMapping("/api/{version}/me/proofread")
    public Status status(@Parameter(hidden = true) Authentication authentication) {
        return proofreadService.status(loggedInMember.requireIdOf(authentication));
    }

    @Operation(summary = "빨간 펜 교정받기",
            description = "본문을 교정받는다(AI 또는 규칙). 고칠 곳마다 본문의 위치(UTF-16, 자바스크립트 문자열 위치)와 종류(spacing·spelling·style),"
                    + " 고친 말, 이유를 준다. 서버가 위치를 본문에서 다시 찾아 검사하고 코드·링크 주소는 고치지 않는다."
                    + " AI는 같은 본문이면 앞 결과를 다시 쓰고 횟수에 넣지 않으며, 하루 횟수를 넘으면 429. 규칙 교정은 횟수를 세지 않는다. 바깥 AI인데 aiConsent가 참이 아니면 본문을 보내지 않고 409. 실패하면 503.")
    @PostMapping("/api/{version}/me/proofread")
    public Result proofread(@RequestBody ProofreadRequest request, @Parameter(hidden = true) Authentication authentication) {
        return proofreadService.proofread(loggedInMember.requireIdOf(authentication), request.content(),
                Boolean.TRUE.equals(request.aiConsent()));
    }
}
