package com.myblog.ai.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.myblog.ai.service.ProofreadChecker.Correction;
import com.myblog.ai.service.Proofreader.Suggestion;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 규칙 교정기 (specs/014 D-16): 돈이 드는 AI 없이, 틀린 것이 분명한 맞춤법·띄어쓰기·외래어 표기만 고친다.
 * 정확도를 먼저 본다: 맞게 쓴 말을 틀렸다고 하지 않는 것이 더 중요하다.
 */
class RuleProofreaderTest {

    private final RuleProofreader rules = new RuleProofreader();
    private final ProofreadChecker checker = new ProofreadChecker(100, 200, 120);

    /** 본문 하나를 교정해 "원문→고친 말" 목록으로 본다(검사기를 거친 실제 결과). */
    private List<String> fixes(String text) {
        List<Correction> found = checker.check(text, rules.proofread(text));
        return found.stream().map(c -> c.original() + "→" + c.replacement()).toList();
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            어제 일이 다 됬다.          | 됬다.→됐다.
            몇일 동안 비가 왔다          | 몇일→며칠
            이제 어떻해                  | 어떻해→어떡해
            정말 오랫만이야              | 오랫만이야→오랜만이야
            주인공의 역활이 크다         | 역활이→역할이
            왠만하면 가자                | 왠만하면→웬만하면
            그건 안 되요                 | 되요→돼요
            내일 뵈요                    | 뵈요→봬요
            설레임이 가득하다            | 설레임이→설렘이
            정말 어의없다                | 어의없다→어이없다
            희안한 일이다                | 희안한→희한한
            일일히 적었다                | 일일히→일일이
            깨끗히 닦았다                | 깨끗히→깨끗이
            곰곰히 생각했다              | 곰곰히→곰곰이
            귀뜸을 해 줬다               | 귀뜸을→귀띔을
            설겆이를 했다                | 설겆이를→설거지를
            시간이 있슴                  | 있슴→있음
            그건 않돼                    | 않돼→안 돼
            금새 끝났다                  | 금새→금세
            알맞는 답을 골라라           | 알맞는→알맞은
            """)
    void 틀린_것이_분명한_맞춤법을_고친다(String text, String expected) {
        assertThat(fixes(text)).containsExactly(expected);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            나는 할수있다                | 할수있다→할 수 있다
            그럴수도 있지                | 그럴수도→그럴 수도
            갈수없는 길                  | 갈수없는→갈 수 없는
            집에 갈때 연락해             | 갈때→갈 때
            그런것 같다                  | 그런것→그런 것
            내가 하는것이 맞다           | 하는것이→하는 것이
            비가 올것같다                | 올것같다→올 것 같다
            그런 거같아                  | 거같아→거 같아
            내가 할께요                  | 할께요→할게요
            내일 갈께                    | 갈께→갈게
            메시지를 보낼께요            | 보낼께요→보낼게요
            먼저 만들께요                | 만들께요→만들게요
            그럴꺼야                     | 그럴꺼야→그럴 거야
            뿐만아니라 덤도 있다         | 뿐만아니라→뿐만 아니라
            """)
    void 의존_명사와_어미를_띄어_쓰고_붙여_쓴다(String text, String expected) {
        assertThat(fixes(text)).containsExactly(expected);
    }

    @ParameterizedTest
    @CsvSource(delimiter = '|', textBlock = """
            메세지를 보냈다              | 메세지를→메시지를
            좋은 컨텐츠가 많다           | 컨텐츠가→콘텐츠가
            어플리케이션을 만든다        | 어플리케이션을→애플리케이션을
            리더쉽이 필요하다            | 리더쉽이→리더십이
            데이타를 모은다              | 데이타를→데이터를
            아키텍쳐 그림                | 아키텍쳐→아키텍처
            스케쥴을 잡았다              | 스케쥴을→스케줄을
            레포트를 냈다                | 레포트를→리포트를
            """)
    void 외래어_표기를_고친다(String text, String expected) {
        assertThat(fixes(text)).containsExactly(expected);
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "공부할수록 재미있다",           // -ㄹ수록은 붙여 쓴다
        "놀려고 나왔다",                 // 놀다 → 놀려고가 맞다
        "참 안됐다",                     // 안되다(가엾다)는 한 낱말이다
        "선생님께 드렸다",               // 높임의 조사 께
        "아들께 드렸다",                 // 받침 ㄹ 뒤의 높임 조사도 그대로
        "선물은 아들께요",               // 높임 조사 '께' + '요'
        "여러분들께요 감사드려요",       // '-들께요'도 높임 조사
        "딸께요",                        // '딸께요'도 높임 조사
        "홀수도 짝수도",                 // 홀수는 한 낱말이다
        "별것 아니다 날것 그대로",       // 별것·날것은 한 낱말이다
        "선거 결과",                     // '거'로 끝나는 낱말을 띄우지 않는다
        "할 수 있다 할 때 그런 것 같다",  // 이미 맞게 띄어 썼다
        "웬일이야 웬만하면",             // 맞는 표기
        "왠지 콘셉트가 좋다",            // 맞는 표기
        "만들려고 했다",                 // 만들다 → 만들려고가 맞다
        "지금 새로 시작한다",            // '금새'가 아니다
        "컨셉트라는 말"                  // 콘셉트로 바꾸면 '콘셉트트'가 되지 않게 그대로 둔다
    })
    void 맞게_쓴_말은_고치지_않는다(String text) {
        assertThat(fixes(text)).isEmpty();
    }

    @Test
    void 한_낱말에_틀린_곳이_둘이면_한_번에_고친다() {
        assertThat(fixes("비가 올것같애")).containsExactly("올것같애→올 것 같아");
    }

    @Test
    void 띄어_쓴_며칠과_수밖에도_고친다() {
        assertThat(fixes("몇 일 지나면 할 수 밖에 없다")).containsExactly("몇 일→며칠", "수 밖에→수밖에");
    }

    @Test
    void 코드_블록과_인라인_코드와_주소_안은_고치지_않는다() {
        String text = """
                됬다 `됬다`
                ```
                메세지 됬다
                ```
                https://example.com/됬다
                """;
        assertThat(fixes(text)).containsExactly("됬다→됐다");
    }

    @Test
    void 이유와_종류를_같이_준다() {
        List<Suggestion> found = rules.proofread("할수있다 됬다 메세지");
        assertThat(found).extracting(Suggestion::kind).containsExactly("spacing", "spelling", "spelling");
        assertThat(found).allSatisfy(s -> assertThat(s.reason()).isNotBlank());
    }

    @Test
    void 밖으로_보내지_않는_규칙_교정이다() {
        assertThat(rules.available()).isTrue();
        assertThat(rules.mode()).isEqualTo("rules");
    }
}
