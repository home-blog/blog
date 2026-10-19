package com.myblog.ai;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.myblog.ai.service.Proofreader;
import com.myblog.ai.service.Proofreader.Suggestion;
import com.myblog.support.TestClock;
import com.myblog.support.TestMembers;
import com.myblog.support.TestMembers.Member;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

/**
 * 빨간 펜 교정 API (specs/014 US1, S1-1 ~ S1-6). AI는 가짜(Mockito)로 바꿔 끼운다.
 * 하루 횟수는 3번으로 줄여 경계를 본다. 횟수·결과는 Redis에 있으므로 본문마다 다른 글자를 넣어 앞 실행과 섞이지 않게 한다.
 */
@SpringBootTest
@Transactional
@Import({TestMembers.class, TestClock.Config.class})
@TestPropertySource(properties = {"proofread.daily-limit=3", "proofread.max-length=100"})
class ProofreadApiTest {

    private static final String PROOFREAD = "/api/v1/me/proofread";

    @Autowired
    private WebApplicationContext context;

    @Autowired
    private TestMembers members;

    @Autowired
    private TestClock clock;

    @MockitoBean
    private Proofreader proofreader;

    @MockitoSpyBean
    private StringRedisTemplate redis;

    private MockMvc mvc;
    private Member member;
    private MockHttpSession session;

    @BeforeEach
    void setUp() throws Exception {
        mvc = MockMvcBuilders.webAppContextSetup(context).apply(springSecurity()).build();
        member = members.register("pen");
        session = members.login(mvc, member);
        when(proofreader.available()).thenReturn(true);
        when(proofreader.mode()).thenReturn("ai");
    }

    @AfterEach
    void tearDown() {
        clock.reset();
    }

    private static String unique(String text) {
        return text + " " + UUID.randomUUID();
    }

    private ResultActions proofread(String content) throws Exception {
        return mvc.perform(post(PROOFREAD).session(session).with(csrf()).contentType(MediaType.APPLICATION_JSON)
                .content("{\"content\":\"" + content + "\"}"));
    }

    @Test
    void S1_1_로그인하지_않으면_401() throws Exception {
        mvc.perform(post(PROOFREAD).with(csrf()).contentType(MediaType.APPLICATION_JSON).content("{\"content\":\"글\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get(PROOFREAD)).andExpect(status().isUnauthorized());
    }

    @Test
    void S1_1_빈_본문과_한도를_넘는_본문은_400() throws Exception {
        proofread("   ").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("PROOFREAD_EMPTY"));
        proofread("가".repeat(101)).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("PROOFREAD_TOO_LONG"));
        verify(proofreader, never()).proofread(anyString());
    }

    @Test
    void S1_1_한도와_같은_길이는_된다() throws Exception {
        when(proofreader.proofread(anyString())).thenReturn(List.of());
        String content = "가".repeat(64) + UUID.randomUUID(); // 64 + 36 = 100자
        proofread(content).andExpect(status().isOk()).andExpect(jsonPath("$.corrections").isEmpty());
    }

    @Test
    void S1_2_AI가_준_고칠_곳을_본문에서_찾아_위치와_함께_돌려준다() throws Exception {
        String content = unique("오늘은 할수있다");
        when(proofreader.proofread(content)).thenReturn(List.of(
                new Suggestion("spacing", "오늘은 ", "할수있다", "할 수 있다", "의존 명사는 띄어 씁니다"),
                new Suggestion("spelling", "", "없는 말", "있는 말", "본문에 없다")));

        proofread(content).andExpect(status().isOk())
                .andExpect(jsonPath("$.corrections.length()").value(1))
                .andExpect(jsonPath("$.corrections[0].start").value(4))
                .andExpect(jsonPath("$.corrections[0].end").value(8))
                .andExpect(jsonPath("$.corrections[0].kind").value("spacing"))
                .andExpect(jsonPath("$.corrections[0].original").value("할수있다"))
                .andExpect(jsonPath("$.corrections[0].replacement").value("할 수 있다"))
                .andExpect(jsonPath("$.corrections[0].reason").value("의존 명사는 띄어 씁니다"))
                .andExpect(jsonPath("$.cached").value(false))
                .andExpect(jsonPath("$.remaining").value(2));
    }

    @Test
    void S1_4_하루_횟수를_넘으면_429() throws Exception {
        when(proofreader.proofread(anyString())).thenReturn(List.of());
        for (int i = 0; i < 3; i++) {
            proofread(unique("글 " + i)).andExpect(status().isOk()).andExpect(jsonPath("$.remaining").value(2 - i));
        }
        proofread(unique("넷째 글")).andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.code").value("PROOFREAD_LIMIT_EXCEEDED"));
        verify(proofreader, times(3)).proofread(anyString());
        mvc.perform(get(PROOFREAD).session(session))
                .andExpect(jsonPath("$.remaining").value(0))
                .andExpect(jsonPath("$.dailyLimit").value(3));
    }

    @Test
    void S1_4_같은_본문은_AI를_다시_부르지_않고_횟수도_그대로다() throws Exception {
        String content = unique("됬다");
        when(proofreader.proofread(content)).thenReturn(List.of(new Suggestion("spelling", "", "됬다", "됐다", "맞춤법")));

        proofread(content).andExpect(jsonPath("$.cached").value(false)).andExpect(jsonPath("$.remaining").value(2));
        proofread(content).andExpect(status().isOk())
                .andExpect(jsonPath("$.cached").value(true))
                .andExpect(jsonPath("$.remaining").value(2))
                .andExpect(jsonPath("$.corrections[0].replacement").value("됐다"));
        verify(proofreader, times(1)).proofread(content);
    }

    @Test
    void S1_4_횟수는_한국_날짜로_센다() throws Exception {
        when(proofreader.proofread(anyString())).thenReturn(List.of());
        clock.set(Instant.parse("2031-03-04T14:59:00Z")); // 한국 23:59
        session = members.login(mvc, member); // 세션 시각도 이 시계를 따르므로 옮긴 뒤 다시 로그인한다
        for (int i = 0; i < 3; i++) {
            proofread(unique("어제 글 " + i)).andExpect(status().isOk());
        }
        proofread(unique("어제 넷째")).andExpect(status().isTooManyRequests());

        clock.set(Instant.parse("2031-03-04T15:01:00Z")); // 한국 다음 날 0:01
        proofread(unique("오늘 글")).andExpect(status().isOk()).andExpect(jsonPath("$.remaining").value(2));
    }

    @Test
    void S1_5_AI가_실패하면_503이고_횟수는_그대로다() throws Exception {
        when(proofreader.proofread(anyString())).thenThrow(new Proofreader.UnavailableException("시간 초과"));

        proofread(unique("실패할 글")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("PROOFREAD_UNAVAILABLE"));
        mvc.perform(get(PROOFREAD).session(session)).andExpect(jsonPath("$.remaining").value(3));
    }

    @Test
    void S1_6_AI가_꺼져_있으면_설정에_꺼짐으로_오고_교정은_503() throws Exception {
        when(proofreader.available()).thenReturn(false);

        mvc.perform(get(PROOFREAD).session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
        proofread(unique("글")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("PROOFREAD_UNAVAILABLE"));
        verify(proofreader, never()).proofread(anyString());
    }

    @Test
    void 설정에는_켜짐과_남은_횟수가_있다() throws Exception {
        mvc.perform(get(PROOFREAD).session(session)).andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(true))
                .andExpect(jsonPath("$.mode").value("ai"))
                .andExpect(jsonPath("$.dailyLimit").value(3))
                .andExpect(jsonPath("$.remaining").value(3))
                .andExpect(jsonPath("$.maxLength").value(100));
    }

    /** Redis 값 연산을 엿보는 대역. 정한 연산만 실패하게 한다. */
    private ValueOperations<String, String> spyValueOps() {
        ValueOperations<String, String> ops = spy(redis.opsForValue());
        doReturn(ops).when(redis).opsForValue();
        return ops;
    }

    @Test
    void 결과를_보관하지_못해도_받은_교정은_돌려주고_횟수는_하나_쓴다() throws Exception {
        String content = unique("됬다");
        when(proofreader.proofread(content)).thenReturn(List.of(new Suggestion("spelling", "", "됬다", "됐다", "맞춤법")));
        ValueOperations<String, String> ops = spyValueOps();
        doThrow(new RedisConnectionFailureException("Redis 멈춤")).when(ops).set(anyString(), anyString(), any(Duration.class));

        proofread(content).andExpect(status().isOk())
                .andExpect(jsonPath("$.corrections[0].replacement").value("됐다"))
                .andExpect(jsonPath("$.remaining").value(2));
    }

    @Test
    void 횟수를_셀_수_없으면_AI를_부르지_않고_503() throws Exception {
        ValueOperations<String, String> ops = spyValueOps();
        doThrow(new RedisConnectionFailureException("Redis 멈춤")).when(ops).increment(anyString());

        proofread(unique("글")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("PROOFREAD_UNAVAILABLE"));
        verify(proofreader, never()).proofread(anyString());
    }

    @Test
    void 횟수_키에_수명을_주지_못하면_올린_것을_되돌리고_503() throws Exception {
        doThrow(new RedisConnectionFailureException("Redis 멈춤")).when(redis).expire(anyString(), any(Duration.class));

        proofread(unique("글")).andExpect(status().isServiceUnavailable());
        verify(proofreader, never()).proofread(anyString());
        reset(redis);
        mvc.perform(get(PROOFREAD).session(session)).andExpect(jsonPath("$.remaining").value(3));
    }

    @Test
    void 횟수를_되돌리지_못하면_500이_아니라_503() throws Exception {
        when(proofreader.proofread(anyString())).thenThrow(new Proofreader.UnavailableException("시간 초과"));
        ValueOperations<String, String> ops = spyValueOps();
        doThrow(new RedisConnectionFailureException("Redis 멈춤")).when(ops).decrement(anyString());

        proofread(unique("글")).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("PROOFREAD_UNAVAILABLE"));
    }

    @Test
    void 남은_횟수_값이_숫자가_아니면_503() throws Exception {
        String today = java.time.LocalDate.ofInstant(clock.instant(), java.time.ZoneId.of("Asia/Seoul")).toString();
        redis.opsForValue().set("proofread:count:" + member.user().getId() + ":" + today, "망가짐", Duration.ofMinutes(5));

        mvc.perform(get(PROOFREAD).session(session)).andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.code").value("PROOFREAD_UNAVAILABLE"));
    }

    @Test
    void 규칙_교정은_돈이_들지_않으므로_횟수를_세지_않고_보관하지도_않는다() throws Exception {
        when(proofreader.mode()).thenReturn("rules");
        when(proofreader.proofread(anyString())).thenReturn(List.of());
        String content = unique("규칙 교정");
        for (int i = 0; i < 5; i++) { // 하루 한도 3을 넘어도 된다
            proofread(content).andExpect(status().isOk())
                    .andExpect(jsonPath("$.remaining").value(3))
                    .andExpect(jsonPath("$.cached").value(false));
        }
        verify(proofreader, times(5)).proofread(content);
        mvc.perform(get(PROOFREAD).session(session))
                .andExpect(jsonPath("$.mode").value("rules"))
                .andExpect(jsonPath("$.remaining").value(3));
    }
}
