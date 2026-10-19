package com.myblog.ai.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 규칙 교정기 (specs/014 D-16): 돈이 드는 AI 없이 서버 안에서 맞춤법·띄어쓰기·외래어 표기를 고친다. 글을 밖으로 보내지 않는다.
 *
 * <ul>
 *   <li>정확도를 먼저 본다: 틀린 것이 분명한 말만 규칙으로 둔다. 뜻에 따라 맞기도 한 말(바램, 안되다, 로서·로써)은 두지 않는다</li>
 *   <li>어절(띄어쓰기로 나뉜 덩어리)마다 규칙을 차례로 적용하고, 바뀌었으면 그 어절 하나를 고칠 곳으로 낸다</li>
 *   <li>위치는 믿지 않는 {@link ProofreadChecker}가 다시 찾으므로, 코드 블록·링크 주소는 거기서 걸러진다</li>
 * </ul>
 */
public class RuleProofreader implements Proofreader {

    private record Rule(String kind, Pattern pattern, String replacement, String reason) {

        static Rule of(String kind, String regex, String replacement, String reason) {
            return new Rule(kind, Pattern.compile(regex), replacement, reason);
        }
    }

    /** 받침이 ㄹ인 글자(할, 갈, 먹을 …). -(으)ㄹ 수 있다, -(으)ㄹ 것을 찾는 데 쓴다. */
    private static final String RIEUL = finalConsonant(8, "");
    /** 받침이 ㄹ이지만 '것'과 한 낱말이 되는 글자(별것, 날것, 탈것, 물것)는 뺀다. 들것은 낱말 맨 앞일 때만 뺀다. */
    private static final String RIEUL_FOR_GEOT = finalConsonant(8, "별날탈물들");
    /** 받침이 ㄴ인 글자(그런, 한, 먹은 …): -(으)ㄴ 것. */
    private static final String NIEUN = finalConsonant(4, "");
    /** -ㄹ게, -ㄹ 거, -ㄹ 때, -ㄹ 수도: 말끝으로 자주 쓰는 글자만 둔다(아들께, 말수도처럼 명사 뒤를 고치지 않게). */
    private static final String VERB_RIEUL = "[할갈볼줄올될을쓸살알럴일놀들잘]";
    /** -ㄹ게요: 높임 조사 '께'+'요'(아들께요, 여러분들께요, 딸께요)와 헷갈리지 않게 '들'·'딸'은 뺀다. 만들께요는 따로 둔다. */
    private static final String RIEUL_BEFORE_YO = finalConsonant(8, "들딸");
    /** -ㄹ게, -ㄹ걸: 높임 조사 '께'(아들께)와 헷갈리지 않게 '들'은 뺀다. */
    private static final String PROMISE_RIEUL = "[할갈볼줄올될을쓸살알럴놀잘]";

    private static final String SPELLING = "spelling";
    private static final String SPACING = "spacing";
    private static final String LOANWORD = "외래어 표기법에 맞는 표기입니다";

    private static final List<Rule> WORD_RULES = List.of(
            // 맞춤법: 틀린 것이 분명한 말
            Rule.of(SPELLING, "됬", "됐", "'되었'을 줄이면 '됐'입니다"),
            Rule.of(SPELLING, "몇일", "며칠", "'며칠'이 표준어입니다"),
            Rule.of(SPELLING, "어떻해", "어떡해", "'어떻게 해'를 줄이면 '어떡해'입니다"),
            Rule.of(SPELLING, "(?<![가-힣])금새", "금세", "'금시에'를 줄이면 '금세'입니다"),
            Rule.of(SPELLING, "오랫만", "오랜만", "'오래간만'을 줄이면 '오랜만'입니다"),
            Rule.of(SPELLING, "오랜동안", "오랫동안", "'오랫동안'이 맞는 표기입니다"),
            Rule.of(SPELLING, "역활", "역할", "'역할'이 맞는 표기입니다"),
            Rule.of(SPELLING, "왠만", "웬만", "'웬만하다'가 맞는 표기입니다"),
            Rule.of(SPELLING, "왠일", "웬일", "'웬일'이 맞는 표기입니다"),
            Rule.of(SPELLING, "웬지", "왠지", "'왜인지'를 줄이면 '왠지'입니다"),
            Rule.of(SPELLING, "되요", "돼요", "'되어요'를 줄이면 '돼요'입니다"),
            Rule.of(SPELLING, "뵈요", "봬요", "'뵈어요'를 줄이면 '봬요'입니다"),
            Rule.of(SPELLING, "설레임", "설렘", "'설레다'의 명사는 '설렘'입니다"),
            Rule.of(SPELLING, "설레이는", "설레는", "'설레다'가 기본형입니다"),
            Rule.of(SPELLING, "설레인다", "설렌다", "'설레다'가 기본형입니다"),
            Rule.of(SPELLING, "어의없", "어이없", "'어이없다'가 맞는 표기입니다"),
            Rule.of(SPELLING, "희안(?=[하한해했])", "희한", "'희한하다'가 맞는 표기입니다"), // 이름 속의 희안은 그대로
            Rule.of(SPELLING, "일일히", "일일이", "'-이'로 끝나는 부사입니다"),
            Rule.of(SPELLING, "깨끗히", "깨끗이", "'-이'로 끝나는 부사입니다"),
            Rule.of(SPELLING, "곰곰히", "곰곰이", "'-이'로 끝나는 부사입니다"),
            Rule.of(SPELLING, "틈틈히", "틈틈이", "'-이'로 끝나는 부사입니다"),
            Rule.of(SPELLING, "번번히", "번번이", "'-이'로 끝나는 부사입니다"),
            Rule.of(SPELLING, "귀뜸", "귀띔", "'귀띔'이 맞는 표기입니다"),
            Rule.of(SPELLING, "내노라", "내로라", "'내로라하다'가 맞는 표기입니다"),
            Rule.of(SPELLING, "어줍잖", "어쭙잖", "'어쭙잖다'가 맞는 표기입니다"),
            Rule.of(SPELLING, "궁시렁", "구시렁", "'구시렁거리다'가 맞는 표기입니다"),
            Rule.of(SPELLING, "설겆이", "설거지", "'설거지'가 표준어입니다"),
            Rule.of(SPELLING, "닥달", "닦달", "'닦달하다'가 맞는 표기입니다"),
            Rule.of(SPELLING, "가르키", "가리키", "방향을 보이는 말은 '가리키다'입니다"),
            Rule.of(SPELLING, "있슴", "있음", "'-음'으로 끝맺습니다"),
            Rule.of(SPELLING, "없슴", "없음", "'-음'으로 끝맺습니다"),
            Rule.of(SPELLING, "같애", "같아", "'같다'는 '같아'로 활용합니다"),
            Rule.of(SPELLING, "알맞는", "알맞은", "형용사 '알맞다'는 '알맞은'으로 활용합니다"),
            Rule.of(SPELLING, "걸맞는", "걸맞은", "형용사 '걸맞다'는 '걸맞은'으로 활용합니다"),
            Rule.of(SPELLING, "않되(?=[가-힣])", "안 되", "부정의 '안'은 띄어 씁니다('않'은 '-지 않다')"), // '-지 않되'(연결 어미)는 그대로
            Rule.of(SPELLING, "않돼", "안 돼", "부정의 '안'은 띄어 씁니다('않'은 '-지 않다')"),
            Rule.of(SPELLING, "않한", "안 한", "부정의 '안'은 띄어 씁니다('않'은 '-지 않다')"),
            Rule.of(SPELLING, "할려(고|면)", "하려$1", "'-려고'는 받침 없이 붙입니다"),
            Rule.of(SPELLING, "갈려(고|면)", "가려$1", "'-려고'는 받침 없이 붙입니다"),
            Rule.of(SPELLING, "볼려(고|면)", "보려$1", "'-려고'는 받침 없이 붙입니다"),
            Rule.of(SPELLING, "줄려(고|면)", "주려$1", "'-려고'는 받침 없이 붙입니다"),
            // '께요'는 받침 ㄹ 글자 대부분(보낼께요), 높임 조사로 자주 쓰는 '들께요'·'딸께요'만 빼고. 말끝의 '께'는 자주 쓰는 글자만
            Rule.of(SPELLING, "만들께(?=요|[^가-힣]|$)", "만들게", "약속을 나타내는 어미는 '-ㄹ게'입니다"),
            Rule.of(SPELLING, "(" + RIEUL_BEFORE_YO + ")께(?=요)", "$1게", "약속을 나타내는 어미는 '-ㄹ게'입니다"),
            Rule.of(SPELLING, "(" + PROMISE_RIEUL + ")께(?=[^가-힣]|$)", "$1게", "약속을 나타내는 어미는 '-ㄹ게'입니다"),
            Rule.of(SPELLING, "(" + RIEUL_BEFORE_YO + ")껄(?=요)", "$1걸", "'-ㄹ걸'이 맞는 표기입니다"),
            Rule.of(SPELLING, "(" + PROMISE_RIEUL + ")껄(?=[^가-힣]|$)", "$1걸", "'-ㄹ걸'이 맞는 표기입니다"),
            // 외래어 표기 (고친 말의 받침이 같은 것만: 뒤의 조사가 그대로 맞게)
            Rule.of(SPELLING, "메세지", "메시지", LOANWORD),
            Rule.of(SPELLING, "컨텐츠", "콘텐츠", LOANWORD),
            Rule.of(SPELLING, "어플리케이션", "애플리케이션", LOANWORD),
            Rule.of(SPELLING, "악세사리", "액세서리", LOANWORD),
            Rule.of(SPELLING, "리더쉽", "리더십", LOANWORD),
            Rule.of(SPELLING, "멤버쉽", "멤버십", LOANWORD),
            Rule.of(SPELLING, "데이타", "데이터", LOANWORD),
            Rule.of(SPELLING, "아키텍쳐", "아키텍처", LOANWORD),
            Rule.of(SPELLING, "스케쥴", "스케줄", LOANWORD),
            Rule.of(SPELLING, "레포트", "리포트", LOANWORD),
            Rule.of(SPELLING, "메뉴얼", "매뉴얼", LOANWORD),
            Rule.of(SPELLING, "비지니스", "비즈니스", LOANWORD),
            Rule.of(SPELLING, "엑세스", "액세스", LOANWORD),
            Rule.of(SPELLING, "라이센스", "라이선스", LOANWORD),
            Rule.of(SPELLING, "프리젠테이션", "프레젠테이션", LOANWORD),
            Rule.of(SPELLING, "화이팅", "파이팅", LOANWORD),
            Rule.of(SPELLING, "바베큐", "바비큐", LOANWORD),
            Rule.of(SPELLING, "렌트카", "렌터카", LOANWORD),
            Rule.of(SPELLING, "쥬스", "주스", LOANWORD),
            Rule.of(SPELLING, "까페", "카페", LOANWORD),
            Rule.of(SPELLING, "쇼파", "소파", LOANWORD),
            Rule.of(SPELLING, "다이나믹", "다이내믹", LOANWORD),
            Rule.of(SPELLING, "타겟", "타깃", LOANWORD),
            // 띄어쓰기: 의존 명사(수, 것, 거, 때)는 띄어 쓴다
            Rule.of(SPACING, "(" + RIEUL + ")수(있|없)", "$1 수 $2", "의존 명사 '수'는 띄어 씁니다"),
            Rule.of(SPACING, "(" + VERB_RIEUL + ")수도", "$1 수도", "의존 명사 '수'는 띄어 씁니다"),
            Rule.of(SPACING, "(" + VERB_RIEUL + ")꺼(?=[가-힣])", "$1 거", "의존 명사 '거'는 띄어 씁니다('꺼'가 아닙니다)"),
            Rule.of(SPACING, "(" + VERB_RIEUL + ")때", "$1 때", "의존 명사 '때'는 띄어 씁니다"),
            Rule.of(SPACING, "(?<=[가-힣])들것", "들 것", "의존 명사 '것'은 띄어 씁니다"),
            Rule.of(SPACING, "(" + RIEUL_FOR_GEOT + "|" + NIEUN + "|는|던)것", "$1 것", "의존 명사 '것'은 띄어 씁니다"),
            Rule.of(SPACING, "것같", "것 같", "'같다'는 앞말과 띄어 씁니다"),
            Rule.of(SPACING, "거같", "거 같", "'같다'는 앞말과 띄어 씁니다"),
            Rule.of(SPACING, "뿐만아니라", "뿐만 아니라", "'아니라'는 띄어 씁니다"));

    /** 띄어쓰기를 넘나드는 규칙: 본문 전체에서 찾는다. */
    private static final List<Rule> SPAN_RULES = List.of(
            // 뒤에는 조사·접미사(이, 간, 부터 …)만 허용한다: 몇 일본인, 몇 일자리는 그대로
            Rule.of(SPELLING, "(?<![가-힣])몇 일(?=(?:이|은|을|의|간|도|째|에|씩|부터|까지)(?:[^가-힣]|$)|[^가-힣]|$)", "며칠",
                    "'며칠'이 표준어입니다"),
            Rule.of(SPACING, "(?<![가-힣])수 밖에", "수밖에", "'밖에'는 조사라 앞말에 붙여 씁니다")); // 호수 밖에(명사)는 그대로

    private static final Pattern WORD = Pattern.compile("\\S+");

    /** 위치를 찾을 때 쓰는 앞 글자 수 (Claude 프롬프트의 context와 같은 뜻). */
    private static final int CONTEXT_LENGTH = 6;

    @Override
    public boolean available() {
        return true;
    }

    @Override
    public String mode() {
        return "rules";
    }

    @Override
    public List<Suggestion> proofread(String content) {
        record Found(int at, Suggestion suggestion) {
        }
        List<Found> found = new ArrayList<>();
        Matcher word = WORD.matcher(content);
        while (word.find()) {
            String original = word.group();
            String fixed = original;
            Rule first = null;
            boolean spelling = false;
            for (Rule rule : WORD_RULES) {
                String next = rule.pattern().matcher(fixed).replaceAll(rule.replacement());
                if (!next.equals(fixed)) {
                    first = first == null ? rule : first;
                    spelling |= SPELLING.equals(rule.kind());
                    fixed = next;
                }
            }
            if (first != null) {
                found.add(new Found(word.start(), suggestion(content, word.start(), spelling ? SPELLING : SPACING,
                        original, fixed, first.reason())));
            }
        }
        for (Rule rule : SPAN_RULES) {
            Matcher span = rule.pattern().matcher(content);
            while (span.find()) {
                found.add(new Found(span.start(), suggestion(content, span.start(), rule.kind(), span.group(),
                        rule.pattern().matcher(span.group()).replaceAll(rule.replacement()), rule.reason())));
            }
        }
        found.sort(Comparator.comparingInt(Found::at));
        return found.stream().map(Found::suggestion).toList();
    }

    private static Suggestion suggestion(String content, int at, String kind, String original, String replacement,
            String reason) {
        String context = content.substring(Math.max(0, at - CONTEXT_LENGTH), at);
        return new Suggestion(kind, context, original, replacement, reason);
    }

    /** 받침(종성 번호)이 같은 한글 글자를 모두 모은 정규식 글자 묶음. except에 든 글자는 뺀다. */
    private static String finalConsonant(int index, String except) {
        StringBuilder chars = new StringBuilder("[");
        for (char c = '가'; c <= '힣'; c++) {
            if ((c - '가') % 28 == index && except.indexOf(c) < 0) {
                chars.append(c);
            }
        }
        return chars.append(']').toString();
    }
}
