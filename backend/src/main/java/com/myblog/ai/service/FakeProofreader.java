package com.myblog.ai.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 개발용 가짜 교정기 (specs/014 D-15): AI 열쇠 없이 화면을 돌려 보려고, 자주 틀리는 말 몇 개를 규칙으로 찾는다.
 * 배포에서는 쓰지 않는다(application-dev.yml의 proofread.without-key: fake).
 */
public class FakeProofreader implements Proofreader {

    private record Rule(String kind, String wrong, String right, String reason) {
    }

    private static final List<Rule> RULES = List.of(
            new Rule("spelling", "됬", "됐", "'되었'을 줄이면 '됐'입니다"),
            new Rule("spelling", "몇일", "며칠", "'며칠'이 표준어입니다"),
            new Rule("spelling", "어떻해", "어떡해", "'어떻게 해'를 줄이면 '어떡해'입니다"),
            new Rule("spelling", "금새", "금세", "'금시에'를 줄이면 '금세'입니다"),
            new Rule("spelling", "오랫만", "오랜만", "'오래간만'을 줄이면 '오랜만'입니다"),
            new Rule("spelling", "역활", "역할", "'역할'이 맞는 표기입니다"),
            new Rule("spelling", "왠만", "웬만", "'웬만하다'가 맞는 표기입니다"),
            new Rule("spelling", "되요", "돼요", "'되어요'를 줄이면 '돼요'입니다"),
            new Rule("spacing", "할수있", "할 수 있", "의존 명사 '수'는 띄어 씁니다"),
            new Rule("spacing", "할수없", "할 수 없", "의존 명사 '수'는 띄어 씁니다"),
            new Rule("spacing", "안되", "안 되", "부정의 '안'은 띄어 씁니다"),
            new Rule("style", "하는 것이 가능하다", "할 수 있다", "더 짧고 자연스러운 표현입니다"));

    /** 위치를 찾을 때 쓰는 앞 글자 수 (Claude 프롬프트의 context와 같은 뜻). */
    private static final int CONTEXT_LENGTH = 6;

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public List<Suggestion> proofread(String content) {
        record Found(int at, Suggestion suggestion) {
        }
        List<Found> found = new ArrayList<>();
        for (Rule rule : RULES) {
            for (int at = content.indexOf(rule.wrong()); at >= 0; at = content.indexOf(rule.wrong(), at + 1)) {
                String context = content.substring(Math.max(0, at - CONTEXT_LENGTH), at);
                found.add(new Found(at, new Suggestion(rule.kind(), context, rule.wrong(), rule.right(), rule.reason())));
            }
        }
        found.sort(Comparator.comparingInt(Found::at));
        return found.stream().map(Found::suggestion).toList();
    }
}
