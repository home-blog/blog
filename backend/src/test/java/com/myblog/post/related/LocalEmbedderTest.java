package com.myblog.post.related;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.assertj.core.data.Offset;
import org.junit.jupiter.api.Test;

/**
 * 글자 조각 임베딩 (specs/014 D-17): 돈이 드는 AI 없이 서버 안에서 만든다. 뜻은 모르지만,
 * 한국어는 낱말 안의 글자 두 개 조각(제주, 여행, 렌터…)을 많이 나눠 가진 글이 같은 이야기일 때가 많다.
 */
class LocalEmbedderTest {

    private final LocalEmbedder local = new LocalEmbedder(256);

    private float[] vector(String text) {
        return local.embed(List.of(text)).get(0);
    }

    private float similarity(String a, String b) {
        return Vectors.dot(vector(a), vector(b));
    }

    @Test
    void 같은_글은_늘_같은_벡터이고_길이는_1이다() {
        float[] jeju = vector("제주 렌터카 여행 후기\n\n바다를 따라 달렸다");

        assertThat(vector("제주 렌터카 여행 후기\n\n바다를 따라 달렸다")).containsExactly(jeju);
        assertThat(Vectors.dot(jeju, jeju)).isCloseTo(1f, Offset.offset(1e-4f));
    }

    @Test
    void 같은_이야기를_하는_글이_다른_이야기보다_가깝다() {
        String jeju = "제주 렌터카 여행 후기\n\n제주도에서 렌터카로 해안 도로를 달렸다. 여행 둘째 날은 비가 왔다.";
        String bus = "제주 버스 여행 팁\n\n제주도는 버스로도 여행할 수 있다. 해안 도로를 따라 가는 노선이 좋다.";
        String java = "스프링 트랜잭션 격리 수준\n\n자바 스프링에서 트랜잭션 격리 수준과 잠금을 정리했다.";

        assertThat(similarity(jeju, bus)).isGreaterThan(similarity(jeju, java));
        assertThat(similarity(bus, jeju)).isGreaterThan(similarity(bus, java));
    }

    @Test
    void 조사가_달라도_같은_낱말이면_가깝다() {
        // 여행을·여행이·여행은 모두 '여행' 조각을 나눠 가진다
        assertThat(similarity("여행을 떠났다", "여행이 좋다")).isGreaterThan(similarity("여행을 떠났다", "코드를 짰다"));
    }

    @Test
    void 코드_블록과_주소는_세지_않는다() {
        String withCode = "제주 여행\n\n```java\nclass Transaction {}\n```\nhttps://example.com/spring/transaction";

        assertThat(similarity(withCode, "제주 여행")).isCloseTo(1f, Offset.offset(1e-4f));
    }

    @Test
    void 빈_글은_0_벡터다() {
        assertThat(vector("   ")).containsOnly(0f);
    }

    @Test
    void 모델_이름에_방식과_차원을_붙인다() {
        assertThat(local.model()).isEqualTo("local-v1@256");
        assertThat(local.available()).isTrue();
    }
}
