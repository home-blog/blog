package com.myblog.post.related;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import com.myblog.post.config.RelatedProperties;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/** Voyage AI 임베딩 요청과 답 읽기 (specs/014 D-10). 실제 서버 대신 가짜 HTTP 서버로 본다. */
class VoyageEmbedderTest {

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer voyage = MockRestServiceServer.bindTo(builder).build();
    private final VoyageEmbedder embedder = new VoyageEmbedder(builder.build(), RelatedTestProperties.with("voyage", "vk-test", 3),
            JsonMapper.builder().build());

    @Test
    void 글을_문서로_보내고_번호_순서대로_길이_1로_맞춘_벡터를_받는다() {
        voyage.expect(requestTo("https://api.voyageai.com/v1/embeddings"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer vk-test"))
                .andExpect(content().json("""
                        {"input":["첫 글","둘째 글"],"model":"voyage-3.5","input_type":"document","output_dimension":3}
                        """))
                .andRespond(withSuccess("""
                        {"object":"list","data":[
                          {"object":"embedding","embedding":[0.0,3.0,4.0],"index":1},
                          {"object":"embedding","embedding":[1.0,0.0,0.0],"index":0}
                        ],"model":"voyage-3.5","usage":{"total_tokens":10}}
                        """, MediaType.APPLICATION_JSON));

        List<float[]> vectors = embedder.embed(List.of("첫 글", "둘째 글"));

        assertThat(vectors).hasSize(2);
        assertThat(vectors.get(0)).containsExactly(1f, 0f, 0f);
        assertThat(vectors.get(1)).containsExactly(0f, 0.6f, 0.8f);
        voyage.verify();
    }

    @Test
    void 서버_오류면_쓸_수_없음으로_알린다() {
        voyage.expect(requestTo("https://api.voyageai.com/v1/embeddings")).andRespond(withServerError());

        assertThatThrownBy(() -> embedder.embed(List.of("글"))).isInstanceOf(Embedder.UnavailableException.class);
    }

    @Test
    void float을_넘는_값이나_0_벡터는_받지_않는다() {
        for (String vector : List.of("[1.0e300,1.0,0.0]", "[0.0,0.0,0.0]")) {
            voyage.reset();
            voyage.expect(requestTo("https://api.voyageai.com/v1/embeddings")).andRespond(withSuccess(
                    "{\"data\":[{\"embedding\":" + vector + ",\"index\":0}]}", MediaType.APPLICATION_JSON));

            assertThatThrownBy(() -> embedder.embed(List.of("글"))).as(vector).isInstanceOf(Embedder.UnavailableException.class);
        }
    }

    @Test
    void 차원이_다른_답은_받지_않는다() {
        voyage.expect(requestTo("https://api.voyageai.com/v1/embeddings")).andRespond(withSuccess("""
                {"data":[{"embedding":[1.0,0.0],"index":0}]}
                """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> embedder.embed(List.of("글"))).isInstanceOf(Embedder.UnavailableException.class);
    }
}
