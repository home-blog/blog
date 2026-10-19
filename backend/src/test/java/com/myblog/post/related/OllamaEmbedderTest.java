package com.myblog.post.related;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withServerError;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import tools.jackson.databind.json.JsonMapper;

/** Ollama 임베딩(bge-m3) 요청과 답 읽기 (specs/014 D-17). 실제 Ollama 대신 가짜 HTTP 서버로 본다. */
class OllamaEmbedderTest {

    private final RestClient.Builder builder = RestClient.builder();
    private final MockRestServiceServer ollama = MockRestServiceServer.bindTo(builder).build();
    private final OllamaEmbedder embedder = new OllamaEmbedder(builder.build(),
            RelatedTestProperties.with("ollama", "", "http://ollama:11434", 3), JsonMapper.builder().build());

    @Test
    void 글을_한꺼번에_보내고_보낸_순서대로_길이_1로_맞춘_벡터를_받는다() {
        ollama.expect(requestTo("http://ollama:11434/api/embed"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("""
                        {"model":"bge-m3","input":["첫 글","둘째 글"],"truncate":true}
                        """))
                .andRespond(withSuccess("""
                        {"model":"bge-m3","embeddings":[[1.0,0.0,0.0],[0.0,3.0,4.0]]}
                        """, MediaType.APPLICATION_JSON));

        List<float[]> vectors = embedder.embed(List.of("첫 글", "둘째 글"));

        assertThat(vectors.get(0)).containsExactly(1f, 0f, 0f);
        assertThat(vectors.get(1)).containsExactly(0f, 0.6f, 0.8f);
        ollama.verify();
    }

    @Test
    void 모델을_아직_받지_못했거나_서버가_멈추면_쓸_수_없음으로_알린다() {
        ollama.expect(requestTo("http://ollama:11434/api/embed")).andRespond(withServerError());

        assertThatThrownBy(() -> embedder.embed(List.of("글"))).isInstanceOf(Embedder.UnavailableException.class);
    }

    @Test
    void 개수나_차원이_다른_답은_받지_않는다() {
        ollama.expect(requestTo("http://ollama:11434/api/embed")).andRespond(withSuccess("""
                {"embeddings":[[1.0,0.0]]}
                """, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> embedder.embed(List.of("글"))).isInstanceOf(Embedder.UnavailableException.class);

        ollama.reset();
        ollama.expect(requestTo("http://ollama:11434/api/embed")).andRespond(withSuccess("""
                {"embeddings":[]}
                """, MediaType.APPLICATION_JSON));
        assertThatThrownBy(() -> embedder.embed(List.of("글"))).isInstanceOf(Embedder.UnavailableException.class);
    }

    @Test
    void 빈_답이나_빈_숫자가_든_답도_쓸_수_없음으로_알린다() {
        for (String body : List.of("null", "{}", "{\"embeddings\":[null]}", "{\"embeddings\":[[1.0,null,0.0]]}")) {
            ollama.reset();
            ollama.expect(requestTo("http://ollama:11434/api/embed")).andRespond(withSuccess(body, MediaType.APPLICATION_JSON));

            assertThatThrownBy(() -> embedder.embed(List.of("글"))).as(body).isInstanceOf(Embedder.UnavailableException.class);
        }
    }

    @Test
    void 모델_이름에_ollama와_모델과_차원을_붙인다() {
        assertThat(embedder.model()).isEqualTo("ollama-bge-m3@3");
    }
}
